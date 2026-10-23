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

/**
 * Pure decisions for opening and switching a chromatogram in the plant
 * shell. No SWT: the chart toolbar and the progress shell call these so a
 * fast local open does not restyle the plot on a timer.
 */
public final class BaijiuChromatogramOpenChrome {

	/**
	 * Eclipse {@code ProgressMonitorJobsDialog} message. A local file open
	 * finishes inside this window, so the shell stays unpainted.
	 */
	public static final int PROGRESS_QUIET_MS = 700;
	/**
	 * One pass after a chromatogram tab change. Not the 80/250 menu storm.
	 */
	public static final int EDITOR_CHROME_QUIET_MS = 120;
	public static final String PROGRESS_ZH = "正在打开…";
	public static final String CANCEL_ZH = "取消";

	private BaijiuChromatogramOpenChrome() {

	}

	/**
	 * File basename, without a ChemClipse supplier suffix such as
	 * {@code (OICD)} or a plant {@code [CSD]} marker.
	 */
	public static String plantChromatogramTabLabel(String raw) {

		if(raw == null || raw.isBlank()) {
			return "";
		}
		String text = stripSupplierSuffix(raw.trim());
		int slash = Math.max(text.lastIndexOf('/'), text.lastIndexOf('\\'));
		if(slash >= 0 && slash < text.length() - 1) {
			text = stripSupplierSuffix(text.substring(slash + 1).trim());
		}
		return text.isBlank() ? raw.trim() : text;
	}

	public static boolean isEnglishOperationInProgress(String text) {

		if(text == null) {
			return false;
		}
		return text.trim().startsWith("Operation in progress");
	}

	public static String localizeProgressText(String text) {

		if(text == null) {
			return "";
		}
		String trimmed = text.trim();
		if(isEnglishOperationInProgress(trimmed)) {
			return PROGRESS_ZH;
		}
		if("Cancel".equalsIgnoreCase(trimmed)) {
			return CANCEL_ZH;
		}
		return text;
	}

	/**
	 * @return {@code true} while a fast open should keep the progress shell hidden
	 */
	public static boolean suppressProgressDialog(long shownAtMs, long nowMs) {

		if(shownAtMs < 0 || nowMs < shownAtMs) {
			return false;
		}
		return nowMs - shownAtMs < PROGRESS_QUIET_MS;
	}

	/**
	 * A ready plant toolbar row must not be laid out again. That layout is
	 * what blanks the plot and snaps the axes when the operator switches tabs.
	 */
	public static boolean shouldRebuildChartToolbar(boolean ready, boolean needsWork) {

		return !ready || needsWork;
	}

	public static int editorChromeDelayMs() {

		return EDITOR_CHROME_QUIET_MS;
	}

	private static String stripSupplierSuffix(String text) {

		String current = text;
		for(int n = 0; n < 3; n++) {
			String next = stripOneSuffix(current);
			if(next.equals(current)) {
				return current;
			}
			current = next.trim();
		}
		return current;
	}

	private static String stripOneSuffix(String text) {

		if(text.length() < 5) {
			return text;
		}
		char tail = text.charAt(text.length() - 1);
		if(tail != ')' && tail != ']') {
			return text;
		}
		char open = tail == ')' ? '(' : '[';
		int at = text.lastIndexOf(open);
		if(at <= 0) {
			return text;
		}
		String token = text.substring(at + 1, text.length() - 1).trim();
		if(!isSupplierToken(token)) {
			return text;
		}
		return text.substring(0, at).trim();
	}

	private static boolean isSupplierToken(String token) {

		return "OICD".equalsIgnoreCase(token) //
				|| "CSD".equalsIgnoreCase(token) //
				|| "MSD".equalsIgnoreCase(token) //
				|| "WSD".equalsIgnoreCase(token) //
				|| "OCB".equalsIgnoreCase(token) //
				|| "ICD".equalsIgnoreCase(token);
	}
}
