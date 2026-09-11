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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.chemclipse.model.core.IPeak;

public final class PeakMatchResult {

	private final Map<String, MatchedPeak> matched;
	private final List<IPeak> unmatched;

	public PeakMatchResult(Map<String, MatchedPeak> matched, List<IPeak> unmatched) {

		this.matched = matched == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(matched));
		this.unmatched = unmatched == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(unmatched));
	}

	public Map<String, MatchedPeak> getMatched() {

		return matched;
	}

	public List<IPeak> getUnmatched() {

		return unmatched;
	}

	public MatchedPeak get(String compoundId) {

		return matched.get(compoundId);
	}
}
