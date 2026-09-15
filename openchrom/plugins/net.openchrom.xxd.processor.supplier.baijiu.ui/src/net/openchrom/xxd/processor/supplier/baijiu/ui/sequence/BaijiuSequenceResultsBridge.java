/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.sequence;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionSequence;
import net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionSequenceEntry;
import net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionSequenceIO;
import net.openchrom.xxd.control.supplier.temperature.ui.sequence.InjectionSequenceManager;
import net.openchrom.xxd.processor.supplier.baijiu.core.BaijiuSequenceVial;

/**
 * Maps reverse-control {@link InjectionSequence} rows to Baijiu result vials.
 * Loaded only when {@code temperature.ui} is present.
 */
public final class BaijiuSequenceResultsBridge {

	private BaijiuSequenceResultsBridge() {

	}

	public static List<BaijiuSequenceVial> fromCurrent() {

		return fromSequence(InjectionSequenceManager.getInstance().snapshot());
	}

	public static List<BaijiuSequenceVial> fromJsonFile(Path file) throws IOException {

		return fromSequence(InjectionSequenceIO.load(file));
	}

	public static List<BaijiuSequenceVial> fromSequence(InjectionSequence sequence) {

		List<BaijiuSequenceVial> vials = new ArrayList<>();
		if(sequence == null) {
			return vials;
		}
		List<InjectionSequenceEntry> entries = sequence.entries();
		for(int i = 0; i < entries.size(); i++) {
			InjectionSequenceEntry entry = entries.get(i);
			vials.add(new BaijiuSequenceVial(i + 1, entry.getType().name(), entry.getType().label(true), entry.getSampleId(), entry.getSampleName(), entry.getStatus().name(), entry.getStatus().label(true), entry.getChromatogramPath(), entry.getNotes(), entry.getParallelGroupId(), sequence.parallelNeedleLabel(i, true), null));
		}
		return vials;
	}
}
