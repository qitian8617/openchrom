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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;
import org.eclipse.chemclipse.logging.core.Logger;

import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.AcquisitionPoint;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.AcquisitionSaveResult;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.IAcquisitionListener;
import net.openchrom.xxd.control.supplier.temperature.ui.acquisition.RealtimeAcquisitionManager;

/**
 * Session sequence: persist under {@code ~/OpenChrom/Sequences/} and advance
 * when Main Start Analysis starts / saves. Optional — an empty queue does not
 * block acquisition.
 */
public final class InjectionSequenceManager implements IAcquisitionListener {

	public interface Listener {

		void onSequenceChanged();
	}

	public static final String DIRECTORY_PROPERTY = "net.openchrom.gcws.sequence.dir";
	public static final String SEQUENCE_DIRECTORY = "Sequences";
	public static final String CURRENT_FILE = "current.json";
	public static final String FILE_EXTENSION = ".json";

	private static final Logger logger = Logger.getLogger(InjectionSequenceManager.class);
	private static volatile InjectionSequenceManager instance;

	private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
	private final InjectionSequence sequence;
	private final Path directory;
	private final Path autoSaveFile;

	private InjectionSequenceManager() {

		this(loadOrEmpty(resolveDirectory()), resolveDirectory(), true);
	}

	InjectionSequenceManager(InjectionSequence sequence, Path directory) {

		this(sequence, directory, false);
	}

	private InjectionSequenceManager(InjectionSequence sequence, Path directory, boolean hookAcquisition) {

		this.sequence = sequence == null ? new InjectionSequence() : sequence;
		this.directory = directory == null ? resolveDirectory() : directory;
		this.autoSaveFile = this.directory.resolve(CURRENT_FILE);
		if(hookAcquisition) {
			RealtimeAcquisitionManager.getInstance().addListener(this);
		}
	}

	public static InjectionSequenceManager getInstance() {

		InjectionSequenceManager local = instance;
		if(local == null) {
			synchronized(InjectionSequenceManager.class) {
				local = instance;
				if(local == null) {
					instance = local = new InjectionSequenceManager();
				}
			}
		}
		return local;
	}

	public static Path resolveDirectory() {

		String override = System.getProperty(DIRECTORY_PROPERTY);
		if(override != null && !override.isBlank()) {
			return Path.of(override.trim());
		}
		return Path.of(System.getProperty("user.home"), "OpenChrom", SEQUENCE_DIRECTORY);
	}

	public void addListener(Listener listener) {

		if(listener != null) {
			listeners.addIfAbsent(listener);
		}
	}

	public void removeListener(Listener listener) {

		listeners.remove(listener);
	}

	public synchronized InjectionSequence snapshot() {

		return sequence.copy();
	}

	public synchronized InjectionSequenceEntry current() {

		InjectionSequenceEntry entry = sequence.current();
		return entry == null ? null : entry.copy();
	}

	public Path getAutoSaveFile() {

		return autoSaveFile;
	}

	public Path getDirectory() {

		return directory;
	}

	public synchronized InjectionSequenceEntry add(InjectionType type) {

		InjectionSequenceEntry entry = sequence.add(type);
		persistAndFire();
		return entry.copy();
	}

	public synchronized boolean remove(int index) {

		boolean removed = sequence.remove(index);
		if(removed) {
			persistAndFire();
		}
		return removed;
	}

	public synchronized boolean moveUp(int index) {

		boolean moved = sequence.moveUp(index);
		if(moved) {
			persistAndFire();
		}
		return moved;
	}

	public synchronized boolean moveDown(int index) {

		boolean moved = sequence.moveDown(index);
		if(moved) {
			persistAndFire();
		}
		return moved;
	}

	public synchronized boolean setCurrent(int index) {

		boolean set = sequence.setCurrent(index);
		if(set) {
			persistAndFire();
		}
		return set;
	}

	public synchronized boolean updateEntry(int index, InjectionType type, String sampleId, String sampleName, String notes) {

		boolean updated = sequence.updateEntry(index, type, sampleId, sampleName, notes);
		if(updated) {
			persistAndFire();
		}
		return updated;
	}

	public synchronized void fillTypical(int sampleCount) {

		sequence.fillTypical(sampleCount);
		persistAndFire();
	}

	public synchronized boolean skip(int index) {

		boolean skipped = sequence.skip(index);
		if(skipped) {
			persistAndFire();
		}
		return skipped;
	}

	public synchronized boolean retry(int index) {

		boolean retried = sequence.retry(index);
		if(retried) {
			persistAndFire();
		}
		return retried;
	}

	public synchronized void saveTo(Path file) throws IOException {

		InjectionSequenceIO.save(sequence, file);
		if(file != null && !autoSaveFile.equals(file.toAbsolutePath().normalize()) && !autoSaveFile.equals(file)) {
			InjectionSequenceIO.save(sequence, autoSaveFile);
		}
	}

	public synchronized void loadFrom(Path file) throws IOException {

		sequence.replaceAll(InjectionSequenceIO.load(file));
		persistAndFire();
	}

	public synchronized String currentSummary(boolean chinese) {

		if(sequence.isEmpty()) {
			return chinese ? "进样序列：未编排（开始分析仍可用）" : "Sequence: none (Start Analysis still works)";
		}
		InjectionSequenceEntry entry = sequence.current();
		if(entry == null) {
			return chinese ? "进样序列：无当前行" : "Sequence: no current row";
		}
		int position = sequence.getCurrentIndex() + 1;
		String head = chinese ? "当前进样 " + position + "/" + sequence.size() + "：" : "Current injection " + position + "/" + sequence.size() + ": ";
		return head + entry.displayLabel(chinese) + " · " + entry.getStatus().label(chinese);
	}

	@Override
	public void onSampleAppended(IChromatogramCSD chromatogram, AcquisitionPoint point, int totalPoints) {

	}

	@Override
	public void onAcquisitionStarted(IChromatogramCSD chromatogram) {

		synchronized(this) {
			InjectionSequenceEntry entry = sequence.beginCurrent();
			if(entry != null && chromatogram != null) {
				if(!entry.getSampleId().isBlank()) {
					chromatogram.setSampleName(entry.getSampleId());
				}
				String dataName = entry.getSampleName().isBlank() ? entry.getType().label(true) : entry.getSampleName();
				if(!dataName.isBlank()) {
					chromatogram.setDataName(dataName);
				}
			}
			if(entry != null) {
				persistAndFire();
			}
		}
	}

	@Override
	public void onAcquisitionSaved(File file, IChromatogramCSD chromatogram, AcquisitionSaveResult result) {

		synchronized(this) {
			String path = file == null ? "" : file.getAbsolutePath();
			if(sequence.completeCurrent(path) != null) {
				persistAndFire();
			}
		}
	}

	@Override
	public void onAcquisitionFailed(String reason, Throwable throwable) {

		synchronized(this) {
			if(sequence.failCurrent() != null) {
				persistAndFire();
			}
		}
	}

	private void persistAndFire() {

		try {
			Files.createDirectories(directory);
			InjectionSequenceIO.save(sequence, autoSaveFile);
		} catch(IOException e) {
			logger.warn("Failed to auto-save injection sequence to " + autoSaveFile, e);
		}
		for(Listener listener : listeners) {
			listener.onSequenceChanged();
		}
	}

	private static InjectionSequence loadOrEmpty(Path directory) {

		Path file = directory.resolve(CURRENT_FILE);
		if(!Files.isRegularFile(file)) {
			return new InjectionSequence();
		}
		try {
			return InjectionSequenceIO.load(file);
		} catch(Exception e) {
			logger.warn("Failed to load injection sequence from " + file, e);
			return new InjectionSequence();
		}
	}
}
