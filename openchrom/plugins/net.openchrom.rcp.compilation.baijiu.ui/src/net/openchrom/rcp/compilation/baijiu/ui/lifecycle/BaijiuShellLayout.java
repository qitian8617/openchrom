/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.lifecycle;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Phase-2 layout persistence. Default launches no longer pass
 * {@code -clearPersistedState}. A chrome-epoch bump or an operator reset
 * deletes {@code workbench.xmi} once, then the workspace remembers sash/size.
 */
public final class BaijiuShellLayout {

	public static final String RESET_PROPERTY = "net.openchrom.baijiu.clearLayout";
	public static final String RESET_PROGRAM_ARG = "-clearPersistedState";
	static final String EPOCH_FILE_NAME = "baijiu-shell-chrome.epoch";
	static final String RESET_MARKER_NAME = "baijiu-reset-layout";

	private BaijiuShellLayout() {

	}

	public static boolean shouldClearPersistedState(int storedEpoch, boolean resetRequested) {

		return resetRequested || storedEpoch < BaijiuShellChrome.CHROME_EPOCH;
	}

	public static void prepareWorkspace() {

		Path instance = instanceArea();
		if(instance == null) {
			return;
		}
		try {
			Path metadata = instance.resolve(".metadata");
			Files.createDirectories(metadata);
			int stored = readEpoch(metadata.resolve(EPOCH_FILE_NAME));
			boolean reset = Files.exists(metadata.resolve(RESET_MARKER_NAME)) || Boolean.getBoolean(RESET_PROPERTY);
			if(shouldClearPersistedState(stored, reset)) {
				deletePersistedWorkbench(instance);
				Files.deleteIfExists(metadata.resolve(RESET_MARKER_NAME));
			}
			Files.writeString(metadata.resolve(EPOCH_FILE_NAME), Integer.toString(BaijiuShellChrome.CHROME_EPOCH), StandardCharsets.UTF_8);
		} catch(IOException e) {
			// keep launching; chrome hide still runs after the model loads
		}
	}

	public static void requestResetOnNextLaunch() throws IOException {

		Path instance = instanceArea();
		if(instance == null) {
			throw new IOException("osgi.instance.area is not set");
		}
		Path metadata = instance.resolve(".metadata");
		Files.createDirectories(metadata);
		Files.writeString(metadata.resolve(RESET_MARKER_NAME), "reset", StandardCharsets.UTF_8);
	}

	static int readEpoch(Path epochFile) {

		if(epochFile == null || !Files.isRegularFile(epochFile)) {
			return 0;
		}
		try {
			String text = Files.readString(epochFile, StandardCharsets.UTF_8).trim();
			if(text.isEmpty()) {
				return 0;
			}
			return Integer.parseInt(text);
		} catch(IOException | NumberFormatException e) {
			return 0;
		}
	}

	static void deletePersistedWorkbench(Path instance) throws IOException {

		Path workbench = instance.resolve(".metadata").resolve(".plugins").resolve("org.eclipse.e4.workbench").resolve("workbench.xmi");
		Files.deleteIfExists(workbench);
		Path workbenchDir = workbench.getParent();
		if(workbenchDir != null && Files.isDirectory(workbenchDir)) {
			try(Stream<Path> children = Files.list(workbenchDir)) {
				children.filter(path -> path.getFileName().toString().startsWith("workbench.xmi")).forEach(path -> {
					try {
						Files.deleteIfExists(path);
					} catch(IOException e) {
						// ignore leftover snapshots
					}
				});
			}
		}
	}

	static Path instanceArea() {

		return resolveArea(firstNonBlank(System.getProperty("osgi.instance.area"), System.getProperty("osgi.instance.area.default")));
	}

	static Path resolveArea(String area) {

		if(area == null || area.isBlank()) {
			String home = System.getProperty("user.home");
			if(home == null || home.isBlank()) {
				return null;
			}
			return Path.of(home, "BaijiuFID");
		}
		String trimmed = area.trim();
		if(trimmed.startsWith("file:")) {
			try {
				URI uri = URI.create(trimmed);
				if(uri.getScheme() != null) {
					return Path.of(uri);
				}
			} catch(RuntimeException e) {
				// fall through to path parse
			}
			return Path.of(trimmed.substring("file:".length()));
		}
		if(trimmed.startsWith("@user.home")) {
			String home = System.getProperty("user.home", "");
			String rest = trimmed.substring("@user.home".length());
			if(rest.startsWith("/") || rest.startsWith("\\")) {
				rest = rest.substring(1);
			}
			return rest.isEmpty() ? Path.of(home) : Path.of(home, rest);
		}
		return Path.of(trimmed);
	}

	private static String firstNonBlank(String... values) {

		if(values == null) {
			return null;
		}
		for(String value : values) {
			if(value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}
}
