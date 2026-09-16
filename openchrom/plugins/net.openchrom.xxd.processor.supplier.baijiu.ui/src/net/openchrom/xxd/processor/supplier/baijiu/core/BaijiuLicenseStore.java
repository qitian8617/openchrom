/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Offline license persistence: Eclipse instance preferences, then
 * {@code ~/OpenChrom/licenses/baijiu-fid.bjlic}. No network.
 */
public final class BaijiuLicenseStore {

	public static final String PREFERENCE_NODE = "net.openchrom.xxd.processor.supplier.baijiu.ui";
	public static final String PREFERENCE_KEY = "license.text";
	public static final String FILE_PROPERTY = "net.openchrom.baijiu.license.file";

	private BaijiuLicenseStore() {

	}

	public static String loadText() {

		String fromPrefs = readPrefs();
		if(fromPrefs != null && !fromPrefs.isBlank()) {
			return fromPrefs;
		}
		Path file = licenseFile();
		if(file != null && Files.isRegularFile(file)) {
			try {
				return Files.readString(file, StandardCharsets.UTF_8);
			} catch(IOException e) {
				return "";
			}
		}
		return "";
	}

	public static BaijiuLicense load() {

		return BaijiuLicense.parse(loadText());
	}

	public static void save(String text) {

		String value = text == null ? "" : text.trim();
		IEclipsePreferences prefs = prefs();
		if(prefs != null) {
			if(value.isEmpty()) {
				prefs.remove(PREFERENCE_KEY);
			} else {
				prefs.put(PREFERENCE_KEY, value);
			}
			flush(prefs);
		}
		Path file = licenseFile();
		if(file == null) {
			return;
		}
		try {
			Files.createDirectories(file.getParent());
			if(value.isEmpty()) {
				Files.deleteIfExists(file);
			} else {
				Files.writeString(file, value, StandardCharsets.UTF_8);
			}
		} catch(IOException e) {
			// preferences still hold the text
		}
	}

	public static void save(BaijiuLicense license) {

		save(license == null ? "" : license.toPropertiesText());
	}

	public static void clear() {

		save("");
	}

	public static Path defaultLicenseFile() {

		return Path.of(System.getProperty("user.home"), "OpenChrom", "licenses", BaijiuLicense.FILE_NAME);
	}

	public static Path licenseFile() {

		String override = System.getProperty(FILE_PROPERTY);
		if(override != null && !override.isBlank()) {
			return Path.of(override);
		}
		return defaultLicenseFile();
	}

	public static String readFile(Path file) throws IOException {

		Objects.requireNonNull(file);
		return Files.readString(file, StandardCharsets.UTF_8);
	}

	private static String readPrefs() {

		IEclipsePreferences prefs = prefs();
		if(prefs == null) {
			return "";
		}
		try {
			return prefs.get(PREFERENCE_KEY, "");
		} catch(RuntimeException e) {
			return "";
		}
	}

	private static IEclipsePreferences prefs() {

		try {
			return InstanceScope.INSTANCE.getNode(PREFERENCE_NODE);
		} catch(RuntimeException e) {
			return null;
		}
	}

	private static void flush(IEclipsePreferences prefs) {

		try {
			prefs.flush();
		} catch(BackingStoreException e) {
			// keep in-memory / file copy
		}
	}
}
