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

/**
 * File-path plumbing for post-save Baijiu handoff. No SWT.
 */
public final class BaijiuHandoffRequest {

	private final File file;
	private final String reasonCn;
	private final String reasonEn;

	private BaijiuHandoffRequest(File file, String reasonCn, String reasonEn) {

		this.file = file;
		this.reasonCn = reasonCn;
		this.reasonEn = reasonEn;
	}

	public static BaijiuHandoffRequest of(File file) {

		if(file == null) {
			return new BaijiuHandoffRequest(null, "没有色谱图文件，无法交白酒工作台。", "No chromatogram file; cannot open the Baijiu workbench.");
		}
		if(!file.isFile()) {
			String path = file.getAbsolutePath();
			return new BaijiuHandoffRequest(file, "色谱图文件不存在：" + path, "Chromatogram file is missing: " + path);
		}
		if(file.length() <= 0L) {
			String path = file.getAbsolutePath();
			return new BaijiuHandoffRequest(file, "色谱图文件为空：" + path, "Chromatogram file is empty: " + path);
		}
		return new BaijiuHandoffRequest(file, "", "");
	}

	public boolean isValid() {

		return file != null && reasonCn.isEmpty();
	}

	public File getFile() {

		return file;
	}

	public String getAbsolutePath() {

		return file == null ? "" : file.getAbsolutePath();
	}

	public String reason(boolean chinese) {

		return chinese ? reasonCn : reasonEn;
	}
}
