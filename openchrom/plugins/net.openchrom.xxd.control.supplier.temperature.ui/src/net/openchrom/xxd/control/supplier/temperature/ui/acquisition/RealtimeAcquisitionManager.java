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
import java.util.List;
import java.util.ConcurrentModificationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.chemclipse.chromatogram.csd.peak.detector.supplier.firstderivative.core.PeakDetectorCSD;
import org.eclipse.chemclipse.chromatogram.csd.peak.detector.supplier.firstderivative.settings.PeakDetectorSettingsCSD;
import org.eclipse.chemclipse.chromatogram.peak.detector.model.Threshold;
import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;
import org.eclipse.chemclipse.csd.model.core.selection.ChromatogramSelectionCSD;
import org.eclipse.chemclipse.csd.model.core.selection.IChromatogramSelectionCSD;
import org.eclipse.chemclipse.logging.core.Logger;
import org.eclipse.chemclipse.model.support.ChromatogramSupport;
import org.eclipse.chemclipse.processing.core.IProcessingMessage;
import org.eclipse.chemclipse.processing.core.IProcessingInfo;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Display;

import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqBatch;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqDoneInfo;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqPayloadCodec;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcAcqSample;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcCommand;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcConnectionManager;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcFrame;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.IGcFrameListener;

public final class RealtimeAcquisitionManager {

	private static final Logger logger = Logger.getLogger(RealtimeAcquisitionManager.class);
	private static final RealtimeAcquisitionManager INSTANCE = new RealtimeAcquisitionManager();
	private static final int DEFAULT_SAMPLE_INTERVAL_MS = 100;
	private static final int EDITOR_REFRESH_INTERVAL_MS = 66;
	private static final float AUTO_PEAK_MIN_SPAN = 5f;

	private final GcConnectionManager connectionManager = GcConnectionManager.getInstance();
	private final List<IAcquisitionListener> listeners = new CopyOnWriteArrayList<>();
	private final AtomicBoolean acquiring = new AtomicBoolean(false);
	private final IGcFrameListener frameListener = this::handleFrame;
	private final IGcFrameListener panelAcqListener = this::handlePanelAcqFrame;

	private IChromatogramCSD chromatogram;
	private int points;
	private int expectedBatchSequence;
	private boolean nativeEditorOpened;
	private volatile boolean editorRefreshActive;
	private volatile IChromatogramSelectionCSD editorSelection;
	private int editorRefreshTicks;

	private RealtimeAcquisitionManager() {

		connectionManager.addFrameListener(panelAcqListener);
	}

	public static RealtimeAcquisitionManager getInstance() {

		return INSTANCE;
	}

	public boolean isAcquiring() {

		return acquiring.get();
	}

	public IChromatogramCSD getChromatogram() {

		return chromatogram;
	}

	public void addListener(IAcquisitionListener listener) {

		listeners.add(listener);
	}

	public void removeListener(IAcquisitionListener listener) {

		listeners.remove(listener);
	}

	public synchronized void startAcquisition() {

		if(acquiring.get()) {
			return;
		}
		if(!connectionManager.isConnected()) {
			notifyFailed(AcquisitionMessages.startFailedNotConnected(true) + " / " + AcquisitionMessages.startFailedNotConnected(false), null);
			return;
		}
		chromatogram = AcquisitionChromatogramStore.createChromatogram();
		points = 0;
		expectedBatchSequence = 0;
		nativeEditorOpened = false;
		editorSelection = null;
		editorRefreshTicks = 0;
		acquiring.set(true);
		startEditorRefreshLoop();
		connectionManager.addFrameListener(frameListener);
		notifyStarted(chromatogram);
		Thread.ofVirtual().start(() -> {
			try {
				byte[] payload = GcAcqPayloadCodec.encodeStartRequest(DEFAULT_SAMPLE_INTERVAL_MS);
				connectionManager.sendFrameAndWait(GcCommand.START_ACQ, payload);
			} catch(Exception e) {
				logger.warn("Failed to send START_ACQ frame", e);
				stopAcquisitionQuietly();
				String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
				notifyFailed(AcquisitionMessages.startFailed(detail, true) + " / " + AcquisitionMessages.startFailed(detail, false), e);
			}
		});
	}

	public synchronized void stopAcquisition() {

		if(!acquiring.getAndSet(false)) {
			return;
		}
		stopEditorRefreshLoop();
		connectionManager.removeFrameListener(frameListener);
		Thread.ofVirtual().start(() -> {
			try {
				connectionManager.sendFrameAndWait(GcCommand.STOP_ACQ, new byte[0]);
			} catch(Exception e) {
				logger.warn("Failed to send STOP_ACQ frame", e);
			}
		});
		finishRun();
	}

	private synchronized void stopAcquisitionQuietly() {

		acquiring.set(false);
		stopEditorRefreshLoop();
		connectionManager.removeFrameListener(frameListener);
	}

	private void handleFrame(GcFrame frame) {

		if(!acquiring.get()) {
			return;
		}
		try {
			switch(frame.getCommand()) {
				case GcCommand.ACQ_DATA -> handleDataBatch(frame);
				case GcCommand.ACQ_DONE -> handleDone(frame);
				case GcCommand.ERROR -> handleDeviceError(frame);
				default -> {
					// ignore unrelated frames
				}
			}
		} catch(RuntimeException e) {
			logger.warn("Failed to handle GC frame " + frame, e);
		}
	}

	private void handlePanelAcqFrame(GcFrame frame) {

		if(frame.getCommand() != GcCommand.PANEL_ACQ) {
			return;
		}
		byte[] payload = frame.getPayload();
		boolean start = payload != null && payload.length > 0 && payload[0] != 0;
		Display display = Display.getDefault();
		if(display == null || display.isDisposed()) {
			applyPanelAcqRequest(start);
			return;
		}
		display.asyncExec(() -> {
			if(!display.isDisposed()) {
				applyPanelAcqRequest(start);
			}
		});
	}

	private void applyPanelAcqRequest(boolean start) {

		if(start) {
			startAcquisition();
		} else {
			stopAcquisition();
		}
	}

	private void handleDataBatch(GcFrame frame) {

		GcAcqBatch batch = GcAcqPayloadCodec.decodeDataBatch(frame.getPayload());
		if(batch.batchSequence() != expectedBatchSequence) {
			logger.warn("GC batch sequence gap: expected " + expectedBatchSequence + ", got " + batch.batchSequence());
		}
		expectedBatchSequence = batch.batchSequence() + 1;
		for(GcAcqSample sample : batch.samples()) {
			appendPoint(new AcquisitionPoint(sample.retentionTimeMs(), sample.signal()));
		}
	}

	private void handleDone(GcFrame frame) {

		try {
			GcAcqDoneInfo doneInfo = GcAcqPayloadCodec.decodeDone(frame.getPayload());
			logger.info("Acquisition done: totalSamples=" + doneInfo.totalSamples() + ", lastBatch=" + doneInfo.lastBatchSequence());
		} catch(IllegalArgumentException e) {
			logger.warn("ACQ_DONE payload invalid", e);
		}
		stopAcquisition();
	}

	private void handleDeviceError(GcFrame frame) {

		String reason = frame.getPayload().length == 0 ? "Device error" : new String(frame.getPayload());
		logger.warn("Device error frame: " + reason);
		stopAcquisition();
	}

	private synchronized void appendPoint(AcquisitionPoint point) {

		if(chromatogram == null) {
			return;
		}
		synchronized(chromatogram) {
			AcquisitionChromatogramStore.appendPoint(chromatogram, point);
			points++;
		}
		if(!nativeEditorOpened && points >= 2) {
			nativeEditorOpened = true;
			CsdNativeEditorSupport.openEditorAsync(chromatogram);
		}
		for(IAcquisitionListener listener : listeners) {
			listener.onSampleAppended(chromatogram, point, points);
		}
	}

	private void finishRun() {

		IChromatogramCSD current = chromatogram;
		if(current == null) {
			notifyFailed(AcquisitionMessages.noDataReason(true) + " / " + AcquisitionMessages.noDataReason(false), null);
			return;
		}
		if(points > 2) {
			autoProcess(current);
		}
		saveOpenAndComplete(current);
	}

	private void autoProcess(IChromatogramCSD current) {

		try {
			ChromatogramSupport.calculateScanIntervalAndDelay(current);
			float span = current.getMaxSignal() - current.getMinSignal();
			if(span < AUTO_PEAK_MIN_SPAN) {
				logger.info("Skip auto peak detection on baseline-only run (span=" + span + ")");
			} else {
				IChromatogramSelectionCSD selection = new ChromatogramSelectionCSD(current);
				PeakDetectorCSD peakDetector = new PeakDetectorCSD();
				PeakDetectorSettingsCSD settings = new PeakDetectorSettingsCSD();
				settings.setDetectorType(org.eclipse.chemclipse.chromatogram.xxd.peak.detector.supplier.firstderivative.model.DetectorType.CB);
				settings.setMinimumSignalToNoiseRatio(0);
				settings.setMovingAverageWindowSize(3);
				settings.setOptimizeBaseline(false);
				settings.setThreshold(Threshold.OFF);
				settings.setUseNoiseSegments(true);
				IProcessingInfo<?> info = peakDetector.detect(selection, settings, new NullProgressMonitor());
				logger.info("Auto peak detection finished: scans=" + current.getNumberOfScans() + ", peaks=" + current.getPeaks().size());
				if(info != null && info.hasErrorMessages()) {
					logProcessingMessages(info);
				}
			}
			ChromatogramEditorNotifier.publishFinalUpdate(current);
		} catch(Exception e) {
			logger.warn("Auto-processing failed; chromatogram will still be saved", e);
		}
	}

	private void logProcessingMessages(IProcessingInfo<?> info) {

		List<IProcessingMessage> messages = info.getMessages();
		if(messages.isEmpty()) {
			logger.warn("Auto-processing finished with errors, but no details were provided.");
			return;
		}
		for(IProcessingMessage message : messages) {
			if(isIgnorableAutoProcessingMessage(message)) {
				continue;
			}
			logger.warn("Auto-processing " + message.getMessageType() + ": " + message.getDescription() + " - " + message.getMessage());
		}
	}

	private boolean isIgnorableAutoProcessingMessage(IProcessingMessage message) {

		String text = message.getMessage();
		return text != null && text.contains("empty scans");
	}

	private void saveOpenAndComplete(IChromatogramCSD current) {

		AcquisitionSaveResult result = AcquisitionChromatogramStore.save(current);
		if(result.isSuccess()) {
			File file = result.getFile();
			logger.info("Saved acquisition data to " + file.getAbsolutePath() + " (" + result.getFormat() + ")");
			ChromatogramEditorNotifier.publishFinalUpdate(current);
			CsdNativeEditorSupport.replaceWithFileEditor(current, file);
			notifySaved(file, current, result);
			notifyCompleted(current);
			return;
		}
		ChromatogramEditorNotifier.publishFinalUpdate(current);
		if(points >= 2 && !nativeEditorOpened) {
			nativeEditorOpened = true;
			CsdNativeEditorSupport.openEditorAsync(current);
		}
		String reason = AcquisitionMessages.saveFailedDialog(result.getReason(), result.getPoints(), result.emergencyPath());
		logger.warn("Failed to save acquisition data: " + result.getReason());
		notifyFailed(reason, null);
	}

	private void notifyStarted(IChromatogramCSD csd) {

		for(IAcquisitionListener listener : listeners) {
			listener.onAcquisitionStarted(csd);
		}
	}

	private void notifyCompleted(IChromatogramCSD csd) {

		for(IAcquisitionListener listener : listeners) {
			listener.onAcquisitionCompleted(csd);
		}
	}

	private void notifySaved(File file, IChromatogramCSD csd, AcquisitionSaveResult result) {

		for(IAcquisitionListener listener : listeners) {
			listener.onAcquisitionSaved(file, csd, result);
		}
	}

	private void notifyFailed(String reason, Throwable throwable) {

		for(IAcquisitionListener listener : listeners) {
			listener.onAcquisitionFailed(reason, throwable);
		}
	}

	private void startEditorRefreshLoop() {

		Display display = Display.getDefault();
		if(display == null) {
			return;
		}
		editorRefreshActive = true;
		scheduleEditorRefresh(display);
	}

	private void stopEditorRefreshLoop() {

		editorRefreshActive = false;
	}

	private void scheduleEditorRefresh(Display display) {

		display.timerExec(EDITOR_REFRESH_INTERVAL_MS, () -> {
			if(!editorRefreshActive || display.isDisposed()) {
				return;
			}
			IChromatogramCSD current = chromatogram;
			if(current != null) {
				boolean hasEnoughScans;
				synchronized(current) {
					hasEnoughScans = current.getNumberOfScans() >= 2;
				}
				if(hasEnoughScans) {
					if(editorSelection == null) {
						editorSelection = ChromatogramEditorNotifier.findEditorSelection(current);
					}
					try {
						ChromatogramEditorNotifier.publishLiveUpdate(current, editorSelection);
					} catch(ConcurrentModificationException e) {
						logger.warn("Skipped live chromatogram refresh due to concurrent scan update", e);
					}
					editorRefreshTicks++;
				}
			}
			scheduleEditorRefresh(display);
		});
	}
}
