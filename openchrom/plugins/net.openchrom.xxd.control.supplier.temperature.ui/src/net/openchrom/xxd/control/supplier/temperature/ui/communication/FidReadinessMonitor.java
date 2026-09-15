/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.communication;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.chemclipse.logging.core.Logger;

/**
 * Single poll loop for {@code READ_FID_STATUS} / {@code READ_GAS_PRESSURE} so Main
 * and Detector share one snapshot instead of racing duplicate TCP reads.
 */
public final class FidReadinessMonitor {

	private static final FidReadinessMonitor INSTANCE = new FidReadinessMonitor();
	private static final Logger logger = Logger.getLogger(FidReadinessMonitor.class);
	private static final long POLL_MS = 1_000;
	private static final long IO_TIMEOUT_MS = 8_000;

	private final GcConnectionManager connectionManager = GcConnectionManager.getInstance();
	private final List<IFidReadinessListener> listeners = new CopyOnWriteArrayList<>();
	private final AtomicBoolean pollBusy = new AtomicBoolean(false);
	private final AtomicBoolean loopStarted = new AtomicBoolean(false);
	private final IGcConnectionListener connectionListener = this::onConnectionChanged;

	private volatile FidReadinessSnapshot snapshot = FidReadinessSnapshot.DISCONNECTED;
	private volatile boolean connectionHooked;

	private FidReadinessMonitor() {
	}

	public static FidReadinessMonitor getInstance() {

		return INSTANCE;
	}

	public FidReadinessSnapshot getSnapshot() {

		if(!connectionManager.isConnected() && snapshot.isConnected()) {
			return FidReadinessSnapshot.DISCONNECTED;
		}
		return snapshot;
	}

	public void addListener(IFidReadinessListener listener) {

		if(listener == null) {
			return;
		}
		ensureConnectionHook();
		listeners.add(listener);
		listener.onFidReadinessChanged(getSnapshot());
		ensurePollLoop();
		if(connectionManager.isConnected()) {
			requestPoll();
		}
	}

	public void removeListener(IFidReadinessListener listener) {

		listeners.remove(listener);
	}

	public void requestPoll() {

		if(!connectionManager.isConnected() || listeners.isEmpty()) {
			return;
		}
		pollOnce();
	}

	private void ensureConnectionHook() {

		if(connectionHooked) {
			return;
		}
		synchronized(this) {
			if(connectionHooked) {
				return;
			}
			connectionManager.addConnectionListener(connectionListener);
			connectionHooked = true;
			if(connectionManager.isConnected()) {
				publish(new FidReadinessSnapshot(true, null, null, null, null));
			}
		}
	}

	private void ensurePollLoop() {

		if(!loopStarted.compareAndSet(false, true)) {
			return;
		}
		Thread.ofVirtual().name("gc-fid-readiness").start(this::pollLoop);
	}

	private void pollLoop() {

		while(true) {
			try {
				if(!listeners.isEmpty() && connectionManager.isConnected()) {
					pollOnce();
				}
				Thread.sleep(POLL_MS);
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
				loopStarted.set(false);
				return;
			}
		}
	}

	private void onConnectionChanged(boolean connected, GcDeviceEndpoint endpoint) {

		if(!connected) {
			publish(FidReadinessSnapshot.DISCONNECTED);
			return;
		}
		publish(new FidReadinessSnapshot(true, null, null, null, null));
		requestPoll();
	}

	private void pollOnce() {

		if(!connectionManager.isConnected() || !pollBusy.compareAndSet(false, true)) {
			return;
		}
		Thread.ofVirtual().name("gc-fid-readiness-io").start(() -> {
			try {
				if(!connectionManager.isConnected()) {
					publish(FidReadinessSnapshot.DISCONNECTED);
					return;
				}
				GcTcpConnection.FidStatus status = null;
				GcTcpConnection.GasPressure pressure = null;
				String fidError = null;
				String pressureError = null;
				try {
					status = connectionManager.readFidStatus(IO_TIMEOUT_MS);
				} catch(Exception ex) {
					fidError = ex.getMessage();
					logger.warn("FID status poll failed", ex);
				}
				try {
					pressure = connectionManager.readGasPressure(IO_TIMEOUT_MS);
				} catch(Exception ex) {
					pressureError = ex.getMessage();
					logger.warn("Gas pressure poll failed", ex);
				}
				if(!connectionManager.isConnected()) {
					publish(FidReadinessSnapshot.DISCONNECTED);
					return;
				}
				publish(new FidReadinessSnapshot(true, status, pressure, fidError, pressureError));
			} finally {
				pollBusy.set(false);
			}
		});
	}

	private void publish(FidReadinessSnapshot next) {

		FidReadinessSnapshot previous = snapshot;
		if(next.equals(previous)) {
			return;
		}
		snapshot = next;
		for(IFidReadinessListener listener : listeners) {
			try {
				listener.onFidReadinessChanged(next);
			} catch(RuntimeException ex) {
				logger.warn("FID readiness listener failed", ex);
			}
		}
	}
}
