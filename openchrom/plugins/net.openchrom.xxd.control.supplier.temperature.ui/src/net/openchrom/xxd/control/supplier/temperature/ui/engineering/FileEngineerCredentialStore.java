/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.engineering;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Salted PBKDF2 record plus lockout counters. The file never contains a
 * password. A malformed file is corrupt and is not treated as "no password".
 */
public final class FileEngineerCredentialStore {

	static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	static final int MIN_ITERATIONS = 210_000;
	static final int MAX_ITERATIONS = 2_000_000;
	static final int SALT_BYTES = 16;
	static final int HASH_BYTES = 32;

	private final Path path;

	public FileEngineerCredentialStore(Path path) {

		this.path = path;
	}

	public static FileEngineerCredentialStore inUserHome() {

		String home = System.getProperty("user.home", ".");
		return new FileEngineerCredentialStore(Path.of(home, ".openchrom", "gcws-engineer.auth"));
	}

	public synchronized CredentialRecord load() {

		if(!Files.isRegularFile(path)) {
			return CredentialRecord.none();
		}
		String text;
		try {
			text = Files.readString(path, StandardCharsets.UTF_8);
		} catch(IOException ex) {
			return CredentialRecord.damaged();
		}
		return parse(text);
	}

	public synchronized void store(CredentialRecord record) throws IOException {

		moveIntoPlace(record, true);
	}

	/**
	 * First enrollment. Fails if a credential file is already present so two
	 * creators cannot both believe they set the password.
	 */
	public synchronized void storeIfAbsent(CredentialRecord record) throws IOException {

		if(record == null || record.corrupt() || record.missing()) {
			throw new IOException("Refusing to persist an empty or corrupt engineer credential");
		}
		Path parent = path.getParent();
		if(parent != null) {
			Files.createDirectories(parent);
		}
		byte[] body = render(record).getBytes(StandardCharsets.UTF_8);
		try(FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
			channel.write(ByteBuffer.wrap(body));
			channel.force(true);
		}
		restrict(path);
	}

	private void moveIntoPlace(CredentialRecord record, boolean replace) throws IOException {

		if(record == null || record.corrupt() || record.missing()) {
			throw new IOException("Refusing to persist an empty or corrupt engineer credential");
		}
		Path parent = path.getParent();
		if(parent != null) {
			Files.createDirectories(parent);
		}
		Path temp = path.resolveSibling(path.getFileName().toString() + ".tmp");
		Files.writeString(temp, render(record), StandardCharsets.UTF_8);
		restrict(temp);
		try {
			if(replace) {
				try {
					Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				} catch(AtomicMoveNotSupportedException ex) {
					Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
				}
			} else {
				try {
					Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE);
				} catch(AtomicMoveNotSupportedException ex) {
					Files.move(temp, path);
				}
			}
		} catch(FileAlreadyExistsException ex) {
			Files.deleteIfExists(temp);
			throw ex;
		}
		restrict(path);
		Files.deleteIfExists(temp);
	}

	static CredentialRecord parse(String text) {

		if(text == null || text.isBlank()) {
			return CredentialRecord.damaged();
		}
		Map<String, String> values = new LinkedHashMap<>();
		for(String raw : text.split("\\R")) {
			String line = raw.trim();
			if(line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			int eq = line.indexOf('=');
			if(eq <= 0) {
				return CredentialRecord.damaged();
			}
			String key = line.substring(0, eq).trim();
			String value = line.substring(eq + 1).trim();
			if(key.toLowerCase().contains("password") || key.toLowerCase().contains("secret")) {
				return CredentialRecord.damaged();
			}
			values.put(key, value);
		}
		if(!"1".equals(values.get("version")) || !ALGORITHM.equals(values.get("algorithm"))) {
			return CredentialRecord.damaged();
		}
		int iterations;
		int failures;
		long lockoutUntil;
		try {
			iterations = Integer.parseInt(values.get("iterations"));
			failures = Integer.parseInt(values.getOrDefault("failures", "0"));
			lockoutUntil = Long.parseLong(values.getOrDefault("lockoutUntilEpochMs", "0"));
		} catch(RuntimeException ex) {
			return CredentialRecord.damaged();
		}
		if(iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS || failures < 0 || lockoutUntil < 0) {
			return CredentialRecord.damaged();
		}
		byte[] salt = decode(values.get("salt"), SALT_BYTES);
		byte[] hash = decode(values.get("hash"), HASH_BYTES);
		if(salt == null || hash == null) {
			return CredentialRecord.damaged();
		}
		return new CredentialRecord(false, false, iterations, salt, hash, failures, lockoutUntil);
	}

	static String render(CredentialRecord record) {

		Base64.Encoder encoder = Base64.getEncoder();
		return "version=1\n" //
				+ "algorithm=" + ALGORITHM + "\n" //
				+ "iterations=" + record.iterations() + "\n" //
				+ "salt=" + encoder.encodeToString(record.salt()) + "\n" //
				+ "hash=" + encoder.encodeToString(record.hash()) + "\n" //
				+ "failures=" + record.failures() + "\n" //
				+ "lockoutUntilEpochMs=" + record.lockoutUntilEpochMs() + "\n";
	}

	private static byte[] decode(String value, int expected) {

		if(value == null || value.isBlank()) {
			return null;
		}
		try {
			byte[] decoded = Base64.getDecoder().decode(value);
			if(decoded.length != expected) {
				return null;
			}
			return decoded;
		} catch(IllegalArgumentException ex) {
			return null;
		}
	}

	private static void restrict(Path file) {

		try {
			Set<PosixFilePermission> ownerOnly = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
			Files.setPosixFilePermissions(file, ownerOnly);
		} catch(UnsupportedOperationException | IOException ex) {
			/*
			 * Non-POSIX volumes keep the platform default. The record is still
			 * a salted hash, not a password.
			 */
		}
	}

	public static final class CredentialRecord {

		private final boolean missing;
		private final boolean corrupt;
		private final int iterations;
		private final byte[] salt;
		private final byte[] hash;
		private final int failures;
		private final long lockoutUntilEpochMs;

		private CredentialRecord(boolean missing, boolean corrupt, int iterations, byte[] salt, byte[] hash, int failures, long lockoutUntilEpochMs) {

			this.missing = missing;
			this.corrupt = corrupt;
			this.iterations = iterations;
			this.salt = salt;
			this.hash = hash;
			this.failures = failures;
			this.lockoutUntilEpochMs = lockoutUntilEpochMs;
		}

		static CredentialRecord none() {

			return new CredentialRecord(true, false, 0, null, null, 0, 0L);
		}

		static CredentialRecord damaged() {

			return new CredentialRecord(false, true, 0, null, null, 0, 0L);
		}

		public static CredentialRecord enrolled(int iterations, byte[] salt, byte[] hash) {

			return new CredentialRecord(false, false, iterations, salt.clone(), hash.clone(), 0, 0L);
		}

		public CredentialRecord withLockout(int failureCount, long lockoutUntil) {

			return new CredentialRecord(missing, corrupt, iterations, salt, hash, failureCount, lockoutUntil);
		}

		public CredentialRecord clearedLockout() {

			return withLockout(0, 0L);
		}

		public boolean missing() {

			return missing;
		}

		public boolean corrupt() {

			return corrupt;
		}

		public int iterations() {

			return iterations;
		}

		public byte[] salt() {

			return salt == null ? null : salt.clone();
		}

		public byte[] hash() {

			return hash == null ? null : hash.clone();
		}

		public int failures() {

			return failures;
		}

		public long lockoutUntilEpochMs() {

			return lockoutUntilEpochMs;
		}
	}
}
