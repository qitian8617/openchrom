/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.acquisition;

import java.io.File;

public final class AcquisitionSaveResult {

	public enum Format {
		OCB, XY, NONE
	}

	private final boolean success;
	private final File file;
	private final Format format;
	private final int points;
	private final String reason;
	private final File emergencyFile;

	private AcquisitionSaveResult(boolean success, File file, Format format, int points, String reason, File emergencyFile) {

		this.success = success;
		this.file = file;
		this.format = format == null ? Format.NONE : format;
		this.points = points;
		this.reason = reason;
		this.emergencyFile = emergencyFile;
	}

	public static AcquisitionSaveResult saved(File file, Format format, int points) {

		return new AcquisitionSaveResult(true, file, format, points, null, null);
	}

	public static AcquisitionSaveResult failed(String reason, int points, File emergencyFile) {

		return new AcquisitionSaveResult(false, null, Format.NONE, points, reason, emergencyFile);
	}

	public boolean isSuccess() {

		return success;
	}

	public File getFile() {

		return file;
	}

	public Format getFormat() {

		return format;
	}

	public int getPoints() {

		return points;
	}

	public String getReason() {

		return reason;
	}

	public File getEmergencyFile() {

		return emergencyFile;
	}

	public String absolutePath() {

		return file == null ? "" : file.getAbsolutePath();
	}

	public String emergencyPath() {

		return emergencyFile == null ? "" : emergencyFile.getAbsolutePath();
	}
}
