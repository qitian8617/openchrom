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

/**
 * One sequence-driven batch-results row: vial metadata plus optional quantify
 * result. Incomplete vials keep a skip reason instead of being dropped.
 */
public final class BaijiuSequenceResultRow {

	public static final String GB_NOT_APPLICABLE = "不适用";

	private final BaijiuSequenceVial vial;
	private final BaijiuBatchRow batchRow;
	private final String skipReason;
	private final String parallelNote;

	public BaijiuSequenceResultRow(BaijiuSequenceVial vial, BaijiuBatchRow batchRow, String skipReason, String parallelNote) {

		this.vial = vial;
		this.batchRow = batchRow;
		this.skipReason = skipReason == null ? "" : skipReason;
		this.parallelNote = parallelNote == null ? "" : parallelNote;
	}

	public BaijiuSequenceVial getVial() {

		return vial;
	}

	public BaijiuBatchRow getBatchRow() {

		return batchRow;
	}

	public String getSkipReason() {

		return skipReason;
	}

	public String getParallelNote() {

		return parallelNote;
	}

	public BaijiuSequenceResultRow withParallelNote(String note) {

		return new BaijiuSequenceResultRow(vial, batchRow, skipReason, note);
	}

	public boolean isQuantified() {

		return batchRow != null;
	}

	public boolean isSkipped() {

		return batchRow == null;
	}

	public boolean isSuccess() {

		return batchRow != null && batchRow.isSuccess();
	}

	public Double concentrationOf(String compoundId) {

		return batchRow == null ? null : batchRow.concentrationOf(compoundId);
	}

	public String getGbVerdict() {

		if(vial == null || !vial.isSample()) {
			return isQuantified() || !skipReason.isEmpty() ? GB_NOT_APPLICABLE : "";
		}
		if(batchRow == null) {
			return "";
		}
		return batchRow.getGbVerdict();
	}

	public String getChromatogramPath() {

		if(vial == null) {
			return "";
		}
		if(!vial.getChromatogramPath().isEmpty()) {
			return vial.getChromatogramPath();
		}
		if(batchRow != null && batchRow.getFile() != null) {
			return batchRow.getFile().getAbsolutePath();
		}
		return "";
	}

	public String getRemark() {

		StringBuilder text = new StringBuilder();
		append(text, oneLine(skipReason));
		if(batchRow != null) {
			append(text, oneLine(batchRow.getMessage()));
		}
		append(text, oneLine(parallelNote));
		if(vial != null) {
			append(text, oneLine(vial.getNotes()));
		}
		return text.toString();
	}

	private static void append(StringBuilder text, String part) {

		if(part == null || part.isBlank()) {
			return;
		}
		if(text.length() > 0) {
			text.append(' ');
		}
		text.append(part.trim());
	}

	private static String oneLine(String value) {

		if(value == null || value.isBlank()) {
			return "";
		}
		return value.replace('\n', ' ').replace('\r', ' ').trim();
	}
}
