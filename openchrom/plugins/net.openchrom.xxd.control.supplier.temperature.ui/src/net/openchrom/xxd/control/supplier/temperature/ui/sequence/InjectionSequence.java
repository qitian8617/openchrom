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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ordered injection queue: Blank / Mix-standard / QC / Sample×N.
 * Headless; no SWT and no hardware. Status advances when the acquire path
 * starts or saves, or when the operator skips / retries a row.
 */
public final class InjectionSequence {

	public static final int MAX_SAMPLES_IN_TEMPLATE = 20;

	private String name;
	private final List<InjectionSequenceEntry> entries;
	private int currentIndex;

	public InjectionSequence() {

		this("", new ArrayList<>(), -1);
	}

	public InjectionSequence(String name, List<InjectionSequenceEntry> entries, int currentIndex) {

		this.name = name == null ? "" : name.trim();
		this.entries = new ArrayList<>();
		if(entries != null) {
			for(InjectionSequenceEntry entry : entries) {
				if(entry != null) {
					this.entries.add(entry);
				}
			}
		}
		this.currentIndex = clampIndex(currentIndex);
	}

	public String getName() {

		return name;
	}

	public void setName(String name) {

		this.name = name == null ? "" : name.trim();
	}

	public int getCurrentIndex() {

		return currentIndex;
	}

	public List<InjectionSequenceEntry> entries() {

		return Collections.unmodifiableList(entries);
	}

	public int size() {

		return entries.size();
	}

	public boolean isEmpty() {

		return entries.isEmpty();
	}

	public InjectionSequenceEntry get(int index) {

		if(!inRange(index)) {
			return null;
		}
		return entries.get(index);
	}

	public InjectionSequenceEntry current() {

		return get(currentIndex);
	}

	public InjectionSequenceEntry add(InjectionType type) {

		InjectionSequenceEntry entry = InjectionSequenceEntry.ofType(type, nextOrdinal(type));
		entries.add(entry);
		if(currentIndex < 0) {
			currentIndex = 0;
		}
		return entry;
	}

	public InjectionSequenceEntry add(InjectionType type, String sampleId, String sampleName, String notes) {

		InjectionSequenceEntry entry = new InjectionSequenceEntry(type, sampleId, sampleName, notes);
		entries.add(entry);
		if(currentIndex < 0) {
			currentIndex = 0;
		}
		return entry;
	}

	public boolean remove(int index) {

		if(!inRange(index)) {
			return false;
		}
		entries.remove(index);
		if(entries.isEmpty()) {
			currentIndex = -1;
			return true;
		}
		if(index < currentIndex) {
			currentIndex--;
		} else if(currentIndex >= entries.size()) {
			currentIndex = entries.size() - 1;
		}
		return true;
	}

	public boolean moveUp(int index) {

		if(index <= 0 || !inRange(index)) {
			return false;
		}
		swap(index, index - 1);
		return true;
	}

	public boolean moveDown(int index) {

		if(!inRange(index) || index >= entries.size() - 1) {
			return false;
		}
		swap(index, index + 1);
		return true;
	}

	public boolean setCurrent(int index) {

		if(!inRange(index)) {
			return false;
		}
		currentIndex = index;
		return true;
	}

	public boolean updateEntry(int index, InjectionType type, String sampleId, String sampleName, String notes) {

		InjectionSequenceEntry entry = get(index);
		if(entry == null) {
			return false;
		}
		if(entry.getStatus() == InjectionStatus.RUNNING) {
			return false;
		}
		if(type != null && entry.getStatus() == InjectionStatus.PENDING) {
			entry.setType(type);
		}
		entry.setSampleId(sampleId);
		entry.setSampleName(sampleName);
		entry.setNotes(notes);
		return true;
	}

	/**
	 * Insert a second SAMPLE needle after {@code index}, sharing sample id/name
	 * and a {@code parallelGroupId}. Queue type stays SAMPLE so acquire/status
	 * semantics are unchanged. At most two members per group.
	 *
	 * @return the new needle, or {@code null} if the row cannot take a parallel
	 */
	public InjectionSequenceEntry addParallelOf(int index) {

		InjectionSequenceEntry source = get(index);
		if(source == null || source.getType() != InjectionType.SAMPLE) {
			return null;
		}
		if(source.getStatus() == InjectionStatus.RUNNING) {
			return null;
		}
		String groupId = source.getParallelGroupId();
		if(groupId.isBlank()) {
			groupId = UUID.randomUUID().toString();
			source.setParallelGroupId(groupId);
		} else if(countInGroup(groupId) >= 2) {
			return null;
		}
		if(source.getNotes().isBlank()) {
			source.setNotes("平行针 A");
		}
		InjectionSequenceEntry twin = new InjectionSequenceEntry(InjectionType.SAMPLE, source.getSampleId(), source.getSampleName(), "平行针 B");
		twin.setParallelGroupId(groupId);
		entries.add(index + 1, twin);
		if(currentIndex > index) {
			currentIndex++;
		}
		return twin;
	}

	public String parallelNeedleLabel(int index, boolean chinese) {

		InjectionSequenceEntry entry = get(index);
		if(entry == null || !entry.isParallelSample()) {
			return "";
		}
		int ordinal = 0;
		int total = 0;
		String groupId = entry.getParallelGroupId();
		for(int i = 0; i < entries.size(); i++) {
			InjectionSequenceEntry other = entries.get(i);
			if(groupId.equals(other.getParallelGroupId())) {
				total++;
				if(i == index) {
					ordinal = total;
				}
			}
		}
		if(ordinal == 1) {
			return chinese ? "平行针 A" : "Needle A";
		}
		if(ordinal == 2) {
			return chinese ? "平行针 B" : "Needle B";
		}
		return chinese ? "平行" : "Parallel";
	}

	/**
	 * Two-needle SAMPLE pairs: explicit {@code parallelGroupId} first, then
	 * leftover SAMPLE rows that share a sample id.
	 */
	public List<InjectionSequenceEntry[]> findParallelPairs() {

		List<InjectionSequenceEntry[]> pairs = new ArrayList<>();
		Map<String, Boolean> used = new LinkedHashMap<>();
		Map<String, List<InjectionSequenceEntry>> byGroup = new LinkedHashMap<>();
		for(InjectionSequenceEntry entry : entries) {
			if(entry.getType() != InjectionType.SAMPLE || entry.getParallelGroupId().isBlank()) {
				continue;
			}
			byGroup.computeIfAbsent(entry.getParallelGroupId(), key -> new ArrayList<>()).add(entry);
		}
		for(List<InjectionSequenceEntry> group : byGroup.values()) {
			if(group.size() < 2) {
				continue;
			}
			pairs.add(new InjectionSequenceEntry[]{group.get(0), group.get(1)});
			used.put(group.get(0).getId(), Boolean.TRUE);
			used.put(group.get(1).getId(), Boolean.TRUE);
		}
		Map<String, List<InjectionSequenceEntry>> bySample = new LinkedHashMap<>();
		for(InjectionSequenceEntry entry : entries) {
			if(entry.getType() != InjectionType.SAMPLE || used.containsKey(entry.getId()) || entry.getSampleId().isBlank()) {
				continue;
			}
			bySample.computeIfAbsent(entry.getSampleId(), key -> new ArrayList<>()).add(entry);
		}
		for(List<InjectionSequenceEntry> group : bySample.values()) {
			if(group.size() < 2) {
				continue;
			}
			pairs.add(new InjectionSequenceEntry[]{group.get(0), group.get(1)});
		}
		return pairs;
	}

	/**
	 * Replace the queue with a typical lab list: blank → 混标 → QC → sample×N.
	 */
	public void fillTypical(int sampleCount) {

		int samples = Math.max(1, Math.min(MAX_SAMPLES_IN_TEMPLATE, sampleCount));
		entries.clear();
		entries.add(InjectionSequenceEntry.ofType(InjectionType.BLANK, 1));
		entries.add(InjectionSequenceEntry.ofType(InjectionType.MIX_STD, 1));
		entries.add(InjectionSequenceEntry.ofType(InjectionType.QC, 1));
		for(int i = 1; i <= samples; i++) {
			entries.add(InjectionSequenceEntry.ofType(InjectionType.SAMPLE, i));
		}
		currentIndex = 0;
		name = "空白-混标-QC-样品×" + samples;
	}

	/**
	 * Mark the current pending row as running. If the pointer sits on a finished
	 * row, it first advances to the next pending item. Empty queue is a no-op so
	 * Start Analysis still works without a sequence.
	 *
	 * @return the running entry, or {@code null} if the queue has nothing pending
	 */
	public InjectionSequenceEntry beginCurrent() {

		ensureCurrentOpen();
		InjectionSequenceEntry entry = current();
		if(entry == null) {
			return null;
		}
		if(entry.getStatus() == InjectionStatus.RUNNING) {
			return entry;
		}
		if(entry.getStatus() != InjectionStatus.PENDING) {
			return null;
		}
		entry.setStatus(InjectionStatus.RUNNING);
		return entry;
	}

	/**
	 * After a successful save: attach the chromatogram path, mark done, advance.
	 */
	public InjectionSequenceEntry completeCurrent(String chromatogramPath) {

		InjectionSequenceEntry entry = current();
		if(entry == null || entry.getStatus() != InjectionStatus.RUNNING) {
			return null;
		}
		entry.setChromatogramPath(chromatogramPath);
		entry.setStatus(InjectionStatus.DONE);
		advanceToNextPending();
		return entry;
	}

	/**
	 * After a failed acquire/save. Stays on the row so the operator can retry or skip.
	 */
	public InjectionSequenceEntry failCurrent() {

		InjectionSequenceEntry entry = current();
		if(entry == null || entry.getStatus() != InjectionStatus.RUNNING) {
			return null;
		}
		entry.setStatus(InjectionStatus.FAILED);
		return entry;
	}

	public boolean skip(int index) {

		InjectionSequenceEntry entry = get(index);
		if(entry == null) {
			return false;
		}
		if(entry.getStatus() != InjectionStatus.PENDING && entry.getStatus() != InjectionStatus.FAILED) {
			return false;
		}
		entry.setStatus(InjectionStatus.SKIPPED);
		if(index == currentIndex) {
			advanceToNextPending();
		}
		return true;
	}

	public boolean retry(int index) {

		InjectionSequenceEntry entry = get(index);
		if(entry == null) {
			return false;
		}
		if(entry.getStatus() != InjectionStatus.FAILED && entry.getStatus() != InjectionStatus.SKIPPED) {
			return false;
		}
		entry.setStatus(InjectionStatus.PENDING);
		entry.setChromatogramPath("");
		currentIndex = index;
		return true;
	}

	public int pendingCount() {

		int count = 0;
		for(InjectionSequenceEntry entry : entries) {
			if(entry.getStatus() == InjectionStatus.PENDING) {
				count++;
			}
		}
		return count;
	}

	public boolean hasPending() {

		return pendingCount() > 0;
	}

	public InjectionSequence copy() {

		List<InjectionSequenceEntry> copies = new ArrayList<>();
		for(InjectionSequenceEntry entry : entries) {
			copies.add(entry.copy());
		}
		return new InjectionSequence(name, copies, currentIndex);
	}

	public void replaceAll(InjectionSequence other) {

		entries.clear();
		if(other == null) {
			currentIndex = -1;
			name = "";
			return;
		}
		name = other.name;
		for(InjectionSequenceEntry entry : other.entries) {
			entries.add(entry.copy());
		}
		currentIndex = clampIndex(other.currentIndex);
	}

	private void ensureCurrentOpen() {

		InjectionSequenceEntry entry = current();
		if(entry != null && entry.getStatus().isOpen()) {
			return;
		}
		advanceToNextPending();
	}

	private void advanceToNextPending() {

		int start = currentIndex < 0 ? 0 : currentIndex + 1;
		for(int i = start; i < entries.size(); i++) {
			if(entries.get(i).getStatus() == InjectionStatus.PENDING) {
				currentIndex = i;
				return;
			}
		}
		if(entries.isEmpty()) {
			currentIndex = -1;
		} else if(!inRange(currentIndex)) {
			currentIndex = entries.size() - 1;
		}
	}

	private int countInGroup(String groupId) {

		if(groupId == null || groupId.isBlank()) {
			return 0;
		}
		int count = 0;
		for(InjectionSequenceEntry entry : entries) {
			if(groupId.equals(entry.getParallelGroupId())) {
				count++;
			}
		}
		return count;
	}

	private int nextOrdinal(InjectionType type) {

		int count = 1;
		for(InjectionSequenceEntry entry : entries) {
			if(entry.getType() == type) {
				count++;
			}
		}
		return count;
	}

	private void swap(int left, int right) {

		InjectionSequenceEntry hold = entries.get(left);
		entries.set(left, entries.get(right));
		entries.set(right, hold);
		if(currentIndex == left) {
			currentIndex = right;
		} else if(currentIndex == right) {
			currentIndex = left;
		}
	}

	private boolean inRange(int index) {

		return index >= 0 && index < entries.size();
	}

	private int clampIndex(int index) {

		if(entries.isEmpty()) {
			return -1;
		}
		if(index < 0) {
			return 0;
		}
		if(index >= entries.size()) {
			return entries.size() - 1;
		}
		return index;
	}
}
