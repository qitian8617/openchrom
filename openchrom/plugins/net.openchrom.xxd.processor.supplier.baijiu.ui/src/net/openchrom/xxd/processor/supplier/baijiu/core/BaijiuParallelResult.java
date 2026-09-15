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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BaijiuParallelResult {

	private final boolean success;
	private final String message;
	private final String sampleLabel;
	private final String needleALabel;
	private final String needleBLabel;
	private final List<BaijiuParallelCompoundStat> rows;

	public BaijiuParallelResult(boolean success, String message, String sampleLabel, String needleALabel, String needleBLabel, List<BaijiuParallelCompoundStat> rows) {

		this.success = success;
		this.message = message == null ? "" : message;
		this.sampleLabel = sampleLabel == null ? "" : sampleLabel;
		this.needleALabel = needleALabel == null ? "" : needleALabel;
		this.needleBLabel = needleBLabel == null ? "" : needleBLabel;
		this.rows = rows == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(rows));
	}

	public static BaijiuParallelResult failure(String message) {

		return new BaijiuParallelResult(false, message, "", "", "", List.of());
	}

	public boolean isSuccess() {

		return success;
	}

	public String getMessage() {

		return message;
	}

	public String getSampleLabel() {

		return sampleLabel;
	}

	public String getNeedleALabel() {

		return needleALabel;
	}

	public String getNeedleBLabel() {

		return needleBLabel;
	}

	public List<BaijiuParallelCompoundStat> getRows() {

		return rows;
	}

	public BaijiuParallelCompoundStat methanol() {

		for(BaijiuParallelCompoundStat row : rows) {
			if(row.isMethanol()) {
				return row;
			}
		}
		return null;
	}

	public int pairedCompoundCount() {

		int count = 0;
		for(BaijiuParallelCompoundStat row : rows) {
			if(row.hasBothNeedles()) {
				count++;
			}
		}
		return count;
	}
}
