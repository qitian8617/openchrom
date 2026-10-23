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

import java.util.UUID;

/**
 * One needle in an {@link OptimizeSequenceRequest}. Independent of the
 * reverse-control queue type so a later solver can consume the same rows.
 */
public final class OptimizeSequenceRow {

	private final String id;
	private final String type;
	private final String concentrationBand;
	private final String sampleId;
	private final String sampleName;
	private final String notes;
	private final String status;
	private final String chromatogramPath;
	private final String parallelGroupId;
	private final boolean inserted;

	public OptimizeSequenceRow(String id, String type, String concentrationBand, String sampleId, String sampleName, String notes, String status, String chromatogramPath, String parallelGroupId, boolean inserted) {

		this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
		this.type = type == null || type.isBlank() ? "SAMPLE" : type.trim();
		this.concentrationBand = concentrationBand == null ? "" : concentrationBand.trim();
		this.sampleId = sampleId == null ? "" : sampleId.trim();
		this.sampleName = sampleName == null ? "" : sampleName.trim();
		this.notes = notes == null ? "" : notes;
		this.status = status == null || status.isBlank() ? "PENDING" : status.trim();
		this.chromatogramPath = chromatogramPath == null ? "" : chromatogramPath;
		this.parallelGroupId = parallelGroupId == null ? "" : parallelGroupId.trim();
		this.inserted = inserted;
	}

	public static OptimizeSequenceRow existing(String id, String type, String concentrationBand, String sampleId, String sampleName, String notes, String status, String chromatogramPath, String parallelGroupId) {

		return new OptimizeSequenceRow(id, type, concentrationBand, sampleId, sampleName, notes, status, chromatogramPath, parallelGroupId, false);
	}

	public static OptimizeSequenceRow of(String id, String type, String concentrationBand, String sampleId) {

		return existing(id, type, concentrationBand, sampleId, sampleId, "", "PENDING", "", "");
	}

	public static OptimizeSequenceRow insertedBlank(String id, String sampleId) {

		return new OptimizeSequenceRow(id, "BLANK", ConcentrationBand.UNKNOWN.name(), sampleId, "空白", "污染规避", "PENDING", "", "", true);
	}

	public String getId() {

		return id;
	}

	public String getType() {

		return type;
	}

	public String getConcentrationBand() {

		return concentrationBand;
	}

	public String getSampleId() {

		return sampleId;
	}

	public String getSampleName() {

		return sampleName;
	}

	public String getNotes() {

		return notes;
	}

	public String getStatus() {

		return status;
	}

	public String getChromatogramPath() {

		return chromatogramPath;
	}

	public String getParallelGroupId() {

		return parallelGroupId;
	}

	public boolean isInserted() {

		return inserted;
	}

	public String displayId() {

		return sampleId.isBlank() ? id : sampleId;
	}
}
