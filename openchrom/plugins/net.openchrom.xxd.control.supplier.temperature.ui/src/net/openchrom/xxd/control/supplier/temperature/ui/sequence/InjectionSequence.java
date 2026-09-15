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
import java.util.List;

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
