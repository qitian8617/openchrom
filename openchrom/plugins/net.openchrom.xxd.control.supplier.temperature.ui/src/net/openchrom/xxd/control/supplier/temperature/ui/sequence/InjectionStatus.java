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

import java.util.Locale;

public enum InjectionStatus {

	PENDING, RUNNING, DONE, SKIPPED, FAILED;

	public String label(boolean chinese) {

		return switch(this) {
			case PENDING -> chinese ? "待进样" : "Pending";
			case RUNNING -> chinese ? "运行中" : "Running";
			case DONE -> chinese ? "已完成" : "Done";
			case SKIPPED -> chinese ? "已跳过" : "Skipped";
			case FAILED -> chinese ? "失败" : "Failed";
		};
	}

	public boolean isOpen() {

		return this == PENDING || this == RUNNING;
	}

	public static InjectionStatus parse(String raw) {

		if(raw == null || raw.isBlank()) {
			return PENDING;
		}
		try {
			return valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch(IllegalArgumentException e) {
			return PENDING;
		}
	}
}
