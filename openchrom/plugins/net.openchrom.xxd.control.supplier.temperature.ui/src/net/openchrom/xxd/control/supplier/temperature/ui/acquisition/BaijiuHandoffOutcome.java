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

/**
 * Result of resolving or running post-save Baijiu handoff. No SWT.
 */
public final class BaijiuHandoffOutcome {

	public enum Kind {
		READY, OPENED, SKIPPED, MISSING_PLUGIN, INVALID_FILE, FAILED
	}

	private final Kind kind;
	private final String path;
	private final String messageCn;
	private final String messageEn;

	private BaijiuHandoffOutcome(Kind kind, String path, String messageCn, String messageEn) {

		this.kind = kind;
		this.path = path == null ? "" : path;
		this.messageCn = messageCn == null ? "" : messageCn;
		this.messageEn = messageEn == null ? "" : messageEn;
	}

	public static BaijiuHandoffOutcome ready(String path) {

		return new BaijiuHandoffOutcome(Kind.READY, path, "", "");
	}

	public static BaijiuHandoffOutcome opened(String path) {

		return new BaijiuHandoffOutcome(Kind.OPENED, path, "", "");
	}

	public static BaijiuHandoffOutcome skipped() {

		return new BaijiuHandoffOutcome(Kind.SKIPPED, "", "", "");
	}

	public static BaijiuHandoffOutcome missingPlugin() {

		return new BaijiuHandoffOutcome(Kind.MISSING_PLUGIN, "", BaijiuHandoffMessages.pluginMissing(true), BaijiuHandoffMessages.pluginMissing(false));
	}

	public static BaijiuHandoffOutcome invalidFile(BaijiuHandoffRequest request) {

		if(request == null) {
			return new BaijiuHandoffOutcome(Kind.INVALID_FILE, "", BaijiuHandoffRequest.of(null).reason(true), BaijiuHandoffRequest.of(null).reason(false));
		}
		return new BaijiuHandoffOutcome(Kind.INVALID_FILE, request.getAbsolutePath(), request.reason(true), request.reason(false));
	}

	public static BaijiuHandoffOutcome failed(String detail) {

		String cn = detail == null || detail.isBlank() ? BaijiuHandoffMessages.failed(null, true) : detail;
		return new BaijiuHandoffOutcome(Kind.FAILED, "", cn, BaijiuHandoffMessages.failed(detail, false));
	}

	public Kind getKind() {

		return kind;
	}

	public String getPath() {

		return path;
	}

	public boolean isOpened() {

		return kind == Kind.OPENED;
	}

	public boolean canOpen() {

		return kind == Kind.READY || kind == Kind.OPENED;
	}

	public String message(boolean chinese) {

		if(kind == Kind.MISSING_PLUGIN) {
			return BaijiuHandoffMessages.pluginMissingBilingual();
		}
		return chinese ? messageCn : messageEn;
	}
}
