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
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import net.openchrom.xxd.control.supplier.temperature.ui.engineering.FileEngineerCredentialStore.CredentialRecord;

/**
 * Workstation engineer authorization. This is not a firmware or network
 * credential: another TCP client is unaffected. There is no built-in password.
 * Idle expiry and exit clear the in-memory grant only. They do not stop heat
 * or acquisition; callers must keep safety commands available.
 */
public final class EngineerSession {

	public static final int MIN_PASSWORD_LENGTH = 10;
	public static final int DEFAULT_FAILURES_BEFORE_LOCKOUT = 5;
	public static final long DEFAULT_LOCKOUT_MS = 30_000L;
	public static final long MAX_LOCKOUT_MS = 15L * 60L * 1000L;
	static final int PBKDF2_ITERATIONS = 600_000;

	private static final Logger LOGGER = Logger.getLogger(EngineerSession.class.getName());
	private static volatile EngineerSession active;

	private final FileEngineerCredentialStore store;
	private final SecureRandom random;
	private final long idleMs;
	private final int failuresBeforeLockout;
	private final long initialLockoutMs;
	private final List<String> audit = new ArrayList<>();

	private CredentialRecord record;
	private long unlockedUntilEpochMs;

	public EngineerSession(FileEngineerCredentialStore store, long idleMs) {

		this(store, idleMs, DEFAULT_FAILURES_BEFORE_LOCKOUT, DEFAULT_LOCKOUT_MS);
	}

	public EngineerSession(FileEngineerCredentialStore store, long idleMs, int failuresBeforeLockout, long initialLockoutMs) {

		this.store = store;
		this.random = new SecureRandom();
		this.idleMs = idleMs;
		this.failuresBeforeLockout = failuresBeforeLockout;
		this.initialLockoutMs = initialLockoutMs;
		this.record = store.load();
	}

	public static EngineerSession shared() {

		EngineerSession current = active;
		if(current != null) {
			return current;
		}
		synchronized(EngineerSession.class) {
			if(active == null) {
				active = new EngineerSession(FileEngineerCredentialStore.inUserHome(), idleMillisFromProperty());
			}
			return active;
		}
	}

	/** Test hook. Production UI uses {@link #shared()}. */
	public static void install(EngineerSession session) {

		active = session;
	}

	public static long idleMillisFromProperty() {

		String raw = System.getProperty("net.openchrom.gcws.engineerIdleMinutes", "15");
		int minutes;
		try {
			minutes = Integer.parseInt(raw.trim());
		} catch(RuntimeException ex) {
			minutes = 15;
		}
		if(minutes < 1) {
			minutes = 1;
		}
		if(minutes > 240) {
			minutes = 240;
		}
		return minutes * 60_000L;
	}

	public long idleMillis() {

		return idleMs;
	}

	public boolean needsEnrollment() {

		return record.missing() && !record.corrupt();
	}

	public boolean corrupt() {

		return record.corrupt();
	}

	public synchronized Attempt enroll(char[] password, char[] confirm, long nowEpochMs) {

		try {
			record = store.load();
			if(record.corrupt()) {
				audit("corrupt");
				return Attempt.CORRUPT;
			}
			if(!record.missing()) {
				audit("already-enrolled");
				return Attempt.ALREADY_ENROLLED;
			}
			if(password == null || confirm == null || password.length == 0 || confirm.length == 0) {
				return Attempt.EMPTY;
			}
			if(password.length < MIN_PASSWORD_LENGTH) {
				return Attempt.TOO_SHORT;
			}
			if(!Arrays.equals(password, confirm)) {
				return Attempt.MISMATCH;
			}
			byte[] salt = new byte[FileEngineerCredentialStore.SALT_BYTES];
			random.nextBytes(salt);
			byte[] hash = pbkdf2(password, salt, PBKDF2_ITERATIONS);
			CredentialRecord enrolled = CredentialRecord.enrolled(PBKDF2_ITERATIONS, salt, hash);
			try {
				store.storeIfAbsent(enrolled);
			} catch(java.nio.file.FileAlreadyExistsException ex) {
				record = store.load();
				if(!record.missing() && !record.corrupt()) {
					audit("already-enrolled");
					return Attempt.ALREADY_ENROLLED;
				}
				audit("io-failed");
				return Attempt.IO_FAILED;
			} catch(IOException ex) {
				audit("io-failed");
				record = store.load();
				return record.corrupt() ? Attempt.CORRUPT : Attempt.IO_FAILED;
			}
			record = enrolled;
			grant(nowEpochMs);
			audit("enroll");
			return Attempt.ENROLLED;
		} finally {
			wipe(password);
			wipe(confirm);
		}
	}

	public synchronized Attempt verify(char[] password, long nowEpochMs) {

		try {
			record = store.load();
			if(record.corrupt()) {
				audit("corrupt");
				return Attempt.CORRUPT;
			}
			if(record.missing()) {
				return Attempt.EMPTY;
			}
			if(nowEpochMs < record.lockoutUntilEpochMs()) {
				audit("locked");
				return Attempt.LOCKED;
			}
			if(password == null || password.length == 0) {
				return Attempt.EMPTY;
			}
			byte[] actual = pbkdf2(password, record.salt(), record.iterations());
			if(!java.security.MessageDigest.isEqual(actual, record.hash())) {
				return registerFailure(nowEpochMs);
			}
			record = record.clearedLockout();
			try {
				store.store(record);
			} catch(IOException ex) {
				audit("persist-failed");
				lock(nowEpochMs);
				return Attempt.CORRUPT;
			}
			grant(nowEpochMs);
			audit("unlock");
			return Attempt.VERIFIED;
		} finally {
			wipe(password);
		}
	}

	public synchronized void noteCancelled() {

		audit("cancelled");
	}

	synchronized void recordExternalAudit(String event) {

		if(event == null || event.isBlank()) {
			return;
		}
		audit(event);
	}

	public synchronized void lock(String reason, long nowEpochMs) {

		lock(nowEpochMs);
		audit(reason == null || reason.isBlank() ? "exit-lock" : reason);
	}

	public synchronized void touch(long nowEpochMs) {

		if(allowsEngineeringWrite(nowEpochMs)) {
			unlockedUntilEpochMs = nowEpochMs + idleMs;
		}
	}

	/**
	 * @return {@code true} only while an enrollment or verify is still inside
	 *         the idle window. Does not change heat or acquisition state.
	 */
	public synchronized boolean allowsEngineeringWrite(long nowEpochMs) {

		if(record.corrupt() || record.missing() || unlockedUntilEpochMs <= 0L) {
			return false;
		}
		if(nowEpochMs >= unlockedUntilEpochMs) {
			lock(nowEpochMs);
			audit("idle-lock");
			return false;
		}
		return true;
	}

	public long lockoutRemainingMs(long nowEpochMs) {

		long until = record.lockoutUntilEpochMs();
		if(until <= nowEpochMs) {
			return 0L;
		}
		return until - nowEpochMs;
	}

	public List<String> auditSnapshot() {

		synchronized(audit) {
			return List.copyOf(audit);
		}
	}

	private Attempt registerFailure(long nowEpochMs) {

		int failures = record.failures() + 1;
		long until = record.lockoutUntilEpochMs();
		if(failures >= failuresBeforeLockout) {
			int extra = failures - failuresBeforeLockout;
			long delay = initialLockoutMs;
			for(int i = 0; i < extra && delay < MAX_LOCKOUT_MS; i++) {
				delay = Math.min(MAX_LOCKOUT_MS, delay * 2L);
			}
			until = nowEpochMs + delay;
		}
		record = record.withLockout(failures, until);
		try {
			store.store(record);
		} catch(IOException ex) {
			audit("persist-failed");
			return Attempt.CORRUPT;
		}
		if(until > nowEpochMs) {
			audit("lockout");
			return Attempt.LOCKED;
		}
		audit("verify-failed");
		return Attempt.WRONG;
	}

	private void grant(long nowEpochMs) {

		unlockedUntilEpochMs = nowEpochMs + idleMs;
	}

	private void lock(long nowEpochMs) {

		unlockedUntilEpochMs = 0L;
		if(nowEpochMs < 0L) {
			unlockedUntilEpochMs = 0L;
		}
	}

	private void audit(String event) {

		synchronized(audit) {
			audit.add(event);
			if(audit.size() > 50) {
				audit.remove(0);
			}
		}
		LOGGER.log(Level.INFO, "workstation engineer auth: {0}", event);
	}

	private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {

		PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, FileEngineerCredentialStore.HASH_BYTES * 8);
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance(FileEngineerCredentialStore.ALGORITHM);
			return factory.generateSecret(spec).getEncoded();
		} catch(GeneralSecurityException ex) {
			throw new IllegalStateException("PBKDF2 is required for workstation engineer authorization", ex);
		} finally {
			spec.clearPassword();
		}
	}

	private static void wipe(char[] password) {

		if(password != null) {
			Arrays.fill(password, '\0');
		}
	}

	public enum Attempt {
		ENROLLED, VERIFIED, MISMATCH, TOO_SHORT, EMPTY, WRONG, LOCKED, CORRUPT, ALREADY_ENROLLED, IO_FAILED
	}
}
