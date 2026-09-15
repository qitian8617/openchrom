/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.sequence;

import java.util.UUID;

/**
 * One intended injection in the pilot queue.
 */
public final class InjectionSequenceEntry {

	private final String id;
	private InjectionType type;
	private String sampleId;
	private String sampleName;
	private String notes;
	private InjectionStatus status;
	private String chromatogramPath;
	private String parallelGroupId;

	public InjectionSequenceEntry(InjectionType type, String sampleId, String sampleName, String notes) {

		this(UUID.randomUUID().toString(), type, sampleId, sampleName, notes, InjectionStatus.PENDING, "", "");
	}

	public InjectionSequenceEntry(String id, InjectionType type, String sampleId, String sampleName, String notes, InjectionStatus status, String chromatogramPath) {

		this(id, type, sampleId, sampleName, notes, status, chromatogramPath, "");
	}

	public InjectionSequenceEntry(String id, InjectionType type, String sampleId, String sampleName, String notes, InjectionStatus status, String chromatogramPath, String parallelGroupId) {

		this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
		this.type = type == null ? InjectionType.SAMPLE : type;
		this.sampleId = sampleId == null ? "" : sampleId.trim();
		this.sampleName = sampleName == null ? "" : sampleName.trim();
		this.notes = notes == null ? "" : notes;
		this.status = status == null ? InjectionStatus.PENDING : status;
		this.chromatogramPath = chromatogramPath == null ? "" : chromatogramPath;
		this.parallelGroupId = parallelGroupId == null ? "" : parallelGroupId.trim();
		if(this.type != InjectionType.SAMPLE) {
			this.parallelGroupId = "";
		}
	}

	public static InjectionSequenceEntry ofType(InjectionType type, int ordinalOneBased) {

		InjectionType kind = type == null ? InjectionType.SAMPLE : type;
		return new InjectionSequenceEntry(kind, kind.defaultSampleId(ordinalOneBased), kind.defaultSampleName(true), "");
	}

	public String getId() {

		return id;
	}

	public InjectionType getType() {

		return type;
	}

	public void setType(InjectionType type) {

		this.type = type == null ? InjectionType.SAMPLE : type;
		if(this.type != InjectionType.SAMPLE) {
			this.parallelGroupId = "";
		}
	}

	public String getSampleId() {

		return sampleId;
	}

	public void setSampleId(String sampleId) {

		this.sampleId = sampleId == null ? "" : sampleId.trim();
	}

	public String getSampleName() {

		return sampleName;
	}

	public void setSampleName(String sampleName) {

		this.sampleName = sampleName == null ? "" : sampleName.trim();
	}

	public String getNotes() {

		return notes;
	}

	public void setNotes(String notes) {

		this.notes = notes == null ? "" : notes;
	}

	public InjectionStatus getStatus() {

		return status;
	}

	public void setStatus(InjectionStatus status) {

		this.status = status == null ? InjectionStatus.PENDING : status;
	}

	public String getChromatogramPath() {

		return chromatogramPath;
	}

	public void setChromatogramPath(String chromatogramPath) {

		this.chromatogramPath = chromatogramPath == null ? "" : chromatogramPath;
	}

	public String getParallelGroupId() {

		return parallelGroupId;
	}

	public void setParallelGroupId(String parallelGroupId) {

		this.parallelGroupId = parallelGroupId == null ? "" : parallelGroupId.trim();
		if(type != InjectionType.SAMPLE) {
			this.parallelGroupId = "";
		}
	}

	public boolean isParallelSample() {

		return type == InjectionType.SAMPLE && !parallelGroupId.isBlank();
	}

	public String displayLabel(boolean chinese) {

		String name = sampleName.isBlank() ? type.label(chinese) : sampleName;
		if(sampleId.isBlank()) {
			return name;
		}
		if(name.equals(sampleId)) {
			return sampleId;
		}
		return type.label(chinese) + " " + sampleId + " · " + name;
	}

	public InjectionSequenceEntry copy() {

		return new InjectionSequenceEntry(id, type, sampleId, sampleName, notes, status, chromatogramPath, parallelGroupId);
	}
}
