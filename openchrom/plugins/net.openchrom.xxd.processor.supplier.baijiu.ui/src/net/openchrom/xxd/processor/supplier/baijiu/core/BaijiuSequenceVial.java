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

import java.util.Locale;

import org.eclipse.chemclipse.model.core.IChromatogram;

/**
 * One injection-sequence row for the batch results table. Uses string type /
 * status codes so this package does not import reverse-control sequence types.
 */
public final class BaijiuSequenceVial {

	public static final String TYPE_BLANK = "BLANK";
	public static final String TYPE_MIX_STD = "MIX_STD";
	public static final String TYPE_QC = "QC";
	public static final String TYPE_SAMPLE = "SAMPLE";

	public static final String STATUS_PENDING = "PENDING";
	public static final String STATUS_RUNNING = "RUNNING";
	public static final String STATUS_DONE = "DONE";
	public static final String STATUS_SKIPPED = "SKIPPED";
	public static final String STATUS_FAILED = "FAILED";

	private final int ordinal;
	private final String typeCode;
	private final String typeLabel;
	private final String sampleId;
	private final String sampleName;
	private final String statusCode;
	private final String statusLabel;
	private final String chromatogramPath;
	private final String notes;
	private final String parallelGroupId;
	private final String parallelLabel;
	private final IChromatogram chromatogram;

	public BaijiuSequenceVial(int ordinal, String typeCode, String typeLabel, String sampleId, String sampleName, String statusCode, String statusLabel, String chromatogramPath, String notes, String parallelGroupId, String parallelLabel, IChromatogram chromatogram) {

		this.ordinal = Math.max(1, ordinal);
		this.typeCode = normalizeCode(typeCode, TYPE_SAMPLE);
		this.typeLabel = typeLabel == null || typeLabel.isBlank() ? typeLabel(this.typeCode, true) : typeLabel.trim();
		this.sampleId = sampleId == null ? "" : sampleId.trim();
		this.sampleName = sampleName == null ? "" : sampleName.trim();
		this.statusCode = normalizeCode(statusCode, STATUS_PENDING);
		this.statusLabel = statusLabel == null || statusLabel.isBlank() ? statusLabel(this.statusCode, true) : statusLabel.trim();
		this.chromatogramPath = chromatogramPath == null ? "" : chromatogramPath.trim();
		this.notes = notes == null ? "" : notes;
		this.parallelGroupId = parallelGroupId == null ? "" : parallelGroupId.trim();
		this.parallelLabel = parallelLabel == null ? "" : parallelLabel.trim();
		this.chromatogram = chromatogram;
	}

	public static BaijiuSequenceVial of(int ordinal, String typeCode, String sampleId, String sampleName, String statusCode, String chromatogramPath, IChromatogram chromatogram) {

		String type = normalizeCode(typeCode, TYPE_SAMPLE);
		String status = normalizeCode(statusCode, STATUS_PENDING);
		return new BaijiuSequenceVial(ordinal, type, typeLabel(type, true), sampleId, sampleName, status, statusLabel(status, true), chromatogramPath, "", "", "", chromatogram);
	}

	public int getOrdinal() {

		return ordinal;
	}

	public String getTypeCode() {

		return typeCode;
	}

	public String getTypeLabel() {

		return typeLabel;
	}

	public String getSampleId() {

		return sampleId;
	}

	public String getSampleName() {

		return sampleName;
	}

	public String getStatusCode() {

		return statusCode;
	}

	public String getStatusLabel() {

		return statusLabel;
	}

	public String getChromatogramPath() {

		return chromatogramPath;
	}

	public String getNotes() {

		return notes;
	}

	public String getParallelGroupId() {

		return parallelGroupId;
	}

	public String getParallelLabel() {

		return parallelLabel;
	}

	public IChromatogram getChromatogram() {

		return chromatogram;
	}

	public boolean isSample() {

		return TYPE_SAMPLE.equals(typeCode);
	}

	public boolean isDone() {

		return STATUS_DONE.equals(statusCode);
	}

	public static String typeLabel(String typeCode, boolean chinese) {

		return switch(normalizeCode(typeCode, TYPE_SAMPLE)) {
			case TYPE_BLANK -> chinese ? "空白" : "Blank";
			case TYPE_MIX_STD -> chinese ? "混标" : "Mix-standard";
			case TYPE_QC -> "QC";
			default -> chinese ? "样品" : "Sample";
		};
	}

	public static String statusLabel(String statusCode, boolean chinese) {

		return switch(normalizeCode(statusCode, STATUS_PENDING)) {
			case STATUS_RUNNING -> chinese ? "运行中" : "Running";
			case STATUS_DONE -> chinese ? "已完成" : "Done";
			case STATUS_SKIPPED -> chinese ? "已跳过" : "Skipped";
			case STATUS_FAILED -> chinese ? "失败" : "Failed";
			default -> chinese ? "待进样" : "Pending";
		};
	}

	private static String normalizeCode(String raw, String fallback) {

		if(raw == null || raw.isBlank()) {
			return fallback;
		}
		return raw.trim().toUpperCase(Locale.ROOT);
	}
}
