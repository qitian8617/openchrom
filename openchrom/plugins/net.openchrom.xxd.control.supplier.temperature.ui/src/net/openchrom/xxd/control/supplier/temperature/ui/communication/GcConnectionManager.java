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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.chemclipse.logging.core.Logger;

public final class GcConnectionManager {

	private static final GcConnectionManager INSTANCE = new GcConnectionManager();
	private static final Logger logger = Logger.getLogger(GcConnectionManager.class);
	private static final long DEFAULT_RESPONSE_TIMEOUT_MS = 10_000;
	private static final long HEARTBEAT_MS = 2000;
	private static final long HEARTBEAT_IO_TIMEOUT_MS = 3000;

	private final GcTcpConnection connection = new GcTcpConnection();
	private final GcDeviceScanner scanner = new GcDeviceScanner();
	private final List<IGcConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
	private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
		Thread thread = new Thread(runnable, "gc-pc-heartbeat");
		thread.setDaemon(true);
		return thread;
	});
	private ScheduledFuture<?> heartbeatTask;

	private GcConnectionManager() {

		connection.setConnectionLostHandler(this::handleConnectionLost);
	}

	public static GcConnectionManager getInstance() {

		return INSTANCE;
	}

	public boolean isConnected() {

		return connection.isConnected();
	}

	public GcDeviceEndpoint getConnectedEndpoint() {

		return connection.getEndpoint();
	}

	public boolean isScanning() {

		return scanner.isScanning();
	}

	public CompletableFuture<List<GcDeviceEndpoint>> scanDevicesAsync(int preferredPort) {

		return scanner.scanAsync(preferredPort);
	}

	public void connect(GcDeviceEndpoint endpoint) throws IOException {

		try {
			connection.connect(endpoint);
		} catch(IOException e) {
			logger.warn("GC connect/handshake failed for " + endpoint + ": " + e.getMessage());
			throw e;
		}
		notifyConnectionChanged(true, endpoint);
		startHeartbeat();
		logger.info("GC device connected after handshake: " + endpoint);
	}

	public void disconnect() {

		stopHeartbeat();
		GcDeviceEndpoint endpoint = connection.getEndpoint();
		connection.disconnect();
		notifyConnectionChanged(false, endpoint);
	}

	/**
	 * Sends an ASCII protocol line (F407 {@code app_tcp_server.c}), e.g. oven program commands.
	 */
	public void sendProtocolLine(String lineWithoutCrLf) throws IOException {

		connection.sendProtocolLine(lineWithoutCrLf);
	}

	public ColumnOvenProgram readOvenProgram() throws IOException {

		return connection.readOvenProgram(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public ColumnOvenProgram readOvenProgram(long timeoutMs) throws IOException {

		return connection.readOvenProgram(timeoutMs);
	}

	public void writeOvenProgram(ColumnOvenProgram program) throws IOException {

		connection.writeOvenProgram(program, DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void writeOvenProgram(ColumnOvenProgram program, long timeoutMs) throws IOException {

		connection.writeOvenProgram(program, timeoutMs);
	}

	public GcTcpConnection.OvenPid readOvenPid() throws IOException {

		return connection.readOvenPid(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public GcTcpConnection.OvenPid readOvenPid(long timeoutMs) throws IOException {

		return connection.readOvenPid(timeoutMs);
	}

	public void writeOvenPid(GcTcpConnection.OvenPid pid) throws IOException {

		connection.writeOvenPid(pid, DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void writeOvenPid(GcTcpConnection.OvenPid pid, long timeoutMs) throws IOException {

		connection.writeOvenPid(pid, timeoutMs);
	}

	public GcTcpConnection.OvenPid readInletPid() throws IOException {

		return connection.readInletPid(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public GcTcpConnection.OvenPid readInletPid(long timeoutMs) throws IOException {

		return connection.readInletPid(timeoutMs);
	}

	public void writeInletPid(GcTcpConnection.OvenPid pid) throws IOException {

		connection.writeInletPid(pid, DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void writeInletPid(GcTcpConnection.OvenPid pid, long timeoutMs) throws IOException {

		connection.writeInletPid(pid, timeoutMs);
	}

	public GcTcpConnection.OvenPid readDetectorPid() throws IOException {

		return connection.readDetectorPid(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public GcTcpConnection.OvenPid readDetectorPid(long timeoutMs) throws IOException {

		return connection.readDetectorPid(timeoutMs);
	}

	public void writeDetectorPid(GcTcpConnection.OvenPid pid) throws IOException {

		connection.writeDetectorPid(pid, DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void writeDetectorPid(GcTcpConnection.OvenPid pid, long timeoutMs) throws IOException {

		connection.writeDetectorPid(pid, timeoutMs);
	}

	public GcTcpConnection.OvenLiveTemp readOvenTemp() throws IOException {

		return connection.readOvenTemp(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public GcTcpConnection.OvenLiveTemp readOvenTemp(long timeoutMs) throws IOException {

		return connection.readOvenTemp(timeoutMs);
	}

	public GcTcpConnection.AuxLiveTemp readInletTemp() throws IOException {

		return connection.readInletTemp(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public GcTcpConnection.AuxLiveTemp readInletTemp(long timeoutMs) throws IOException {

		return connection.readInletTemp(timeoutMs);
	}

	public void writeInletTemp(float setpointC) throws IOException {

		connection.writeInletTemp(setpointC, DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void writeInletTemp(float setpointC, long timeoutMs) throws IOException {

		connection.writeInletTemp(setpointC, timeoutMs);
	}

	public GcTcpConnection.AuxLiveTemp readDetectorTemp() throws IOException {

		return connection.readDetectorTemp(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public GcTcpConnection.AuxLiveTemp readDetectorTemp(long timeoutMs) throws IOException {

		return connection.readDetectorTemp(timeoutMs);
	}

	public void writeDetectorTemp(float setpointC) throws IOException {

		connection.writeDetectorTemp(setpointC, DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void writeDetectorTemp(float setpointC, long timeoutMs) throws IOException {

		connection.writeDetectorTemp(setpointC, timeoutMs);
	}

	public void writeZoneSelect(boolean inlet, boolean detector, boolean oven, long timeoutMs) throws IOException {

		connection.writeZoneSelect(inlet, detector, oven, timeoutMs);
	}

	public void startInletHeat() throws IOException {

		connection.startInletHeat(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void startInletHeat(long timeoutMs) throws IOException {

		connection.startInletHeat(timeoutMs);
	}

	public void stopInletHeat() throws IOException {

		connection.stopInletHeat(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void stopInletHeat(long timeoutMs) throws IOException {

		connection.stopInletHeat(timeoutMs);
	}

	public void startDetectorHeat() throws IOException {

		connection.startDetectorHeat(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void startDetectorHeat(long timeoutMs) throws IOException {

		connection.startDetectorHeat(timeoutMs);
	}

	public void stopDetectorHeat() throws IOException {

		connection.stopDetectorHeat(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void stopDetectorHeat(long timeoutMs) throws IOException {

		connection.stopDetectorHeat(timeoutMs);
	}

	public void startOvenControl() throws IOException {

		connection.startOvenControl(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void startOvenControl(long timeoutMs) throws IOException {

		connection.startOvenControl(timeoutMs);
	}

	public void stopOvenControl() throws IOException {

		connection.stopOvenControl(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void stopOvenControl(long timeoutMs) throws IOException {

		connection.stopOvenControl(timeoutMs);
	}

	public void startFidIgnite() throws IOException {

		connection.startFidIgnite(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void startFidIgnite(long timeoutMs) throws IOException {

		connection.startFidIgnite(timeoutMs);
	}

	public void stopFidValves() throws IOException {

		connection.stopFidValves(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void stopFidValves(long timeoutMs) throws IOException {

		connection.stopFidValves(timeoutMs);
	}

	public void startFidValves() throws IOException {

		connection.startFidValves(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void startFidValves(long timeoutMs) throws IOException {

		connection.startFidValves(timeoutMs);
	}

	public void setFidAutoIgnite(boolean enable) throws IOException {

		connection.setFidAutoIgnite(enable, DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void setFidAutoIgnite(boolean enable, long timeoutMs) throws IOException {

		connection.setFidAutoIgnite(enable, timeoutMs);
	}

	public GcTcpConnection.FidStatus readFidStatus() throws IOException {

		return connection.readFidStatus(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public GcTcpConnection.FidStatus readFidStatus(long timeoutMs) throws IOException {

		return connection.readFidStatus(timeoutMs);
	}

	public GcTcpConnection.GasPressure readGasPressure() throws IOException {

		return connection.readGasPressure(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public GcTcpConnection.GasPressure readGasPressure(long timeoutMs) throws IOException {

		return connection.readGasPressure(timeoutMs);
	}

	public GcTcpConnection.ControlStatus readControlStatus() throws IOException {

		return connection.readControlStatus(DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public GcTcpConnection.ControlStatus readControlStatus(long timeoutMs) throws IOException {

		return connection.readControlStatus(timeoutMs);
	}

	public void sendFrame(byte command, byte[] payload) throws IOException {

		connection.sendFrame(command, payload);
	}

	public GcFrame sendFrameAndWait(byte command, byte[] payload, long timeoutMs) throws IOException {

		return connection.sendFrameAndWait(command, payload, timeoutMs);
	}

	public GcFrame sendFrameAndWait(byte command, byte[] payload) throws IOException {

		return sendFrameAndWait(command, payload, DEFAULT_RESPONSE_TIMEOUT_MS);
	}

	public void addConnectionListener(IGcConnectionListener listener) {

		connectionListeners.add(listener);
	}

	public void removeConnectionListener(IGcConnectionListener listener) {

		connectionListeners.remove(listener);
	}

	public void addFrameListener(IGcFrameListener listener) {

		connection.addFrameListener(listener);
	}

	public void removeFrameListener(IGcFrameListener listener) {

		connection.removeFrameListener(listener);
	}

	public void shutdown() {

		stopHeartbeat();
		heartbeatExecutor.shutdownNow();
		disconnect();
		scanner.shutdown();
	}

	private void startHeartbeat() {

		stopHeartbeat();
		heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(this::heartbeatOnce, HEARTBEAT_MS, HEARTBEAT_MS, TimeUnit.MILLISECONDS);
	}

	private void stopHeartbeat() {

		ScheduledFuture<?> task = heartbeatTask;
		heartbeatTask = null;
		if(task != null) {
			task.cancel(false);
		}
	}

	private void heartbeatOnce() {

		if(!connection.isConnected()) {
			return;
		}
		try {
			connection.tryOwnerHeartbeat(HEARTBEAT_IO_TIMEOUT_MS);
		} catch(Exception ex) {
			logger.warn("PC heartbeat failed: " + ex.getMessage());
			handleConnectionLost();
		}
	}

	private void handleConnectionLost() {

		stopHeartbeat();
		GcDeviceEndpoint endpoint = connection.getEndpoint();
		connection.disconnect();
		notifyConnectionChanged(false, endpoint);
	}

	private void notifyConnectionChanged(boolean connected, GcDeviceEndpoint endpoint) {

		for(IGcConnectionListener listener : connectionListeners) {
			try {
				listener.connectionStateChanged(connected, endpoint);
			} catch(RuntimeException e) {
				logger.warn("Connection listener failed", e);
			}
		}
	}
}
