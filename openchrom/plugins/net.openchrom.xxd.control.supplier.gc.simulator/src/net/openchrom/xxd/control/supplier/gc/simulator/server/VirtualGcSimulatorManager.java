/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.gc.simulator.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.eclipse.chemclipse.logging.core.Logger;

public final class VirtualGcSimulatorManager {

	private static final VirtualGcSimulatorManager INSTANCE = new VirtualGcSimulatorManager();
	private static final Logger logger = Logger.getLogger(VirtualGcSimulatorManager.class);

	private final List<Consumer<String>> statusListeners = new CopyOnWriteArrayList<>();
	private final List<String> logEntries = new ArrayList<>();

	private VirtualGcTcpServer server;
	private volatile String lastStatus = "Stopped";

	private VirtualGcSimulatorManager() {
	}

	public static VirtualGcSimulatorManager getInstance() {

		return INSTANCE;
	}

	public synchronized boolean isRunning() {

		return server != null && server.isRunning();
	}

	public synchronized int getPort() {

		return server == null ? VirtualGcTcpServer.defaultPort() : server.getPort();
	}

	public synchronized void start(int port) throws IOException {

		if(isRunning()) {
			return;
		}
		server = new VirtualGcTcpServer(port, this::publishStatus, () -> lastStatus = "Stopped");
		server.start();
		lastStatus = "Listening on port " + port;
		notifyListeners(lastStatus);
	}

	public synchronized void stop() {

		if(server != null) {
			server.stop();
			server = null;
		}
		lastStatus = "Stopped";
		notifyListeners(lastStatus);
	}

	public void shutdown() {

		stop();
		statusListeners.clear();
	}

	public String getLastStatus() {

		return lastStatus;
	}

	public synchronized List<String> getLogEntries() {

		return new ArrayList<>(logEntries);
	}

	public void addStatusListener(Consumer<String> listener) {

		statusListeners.add(listener);
	}

	public void removeStatusListener(Consumer<String> listener) {

		statusListeners.remove(listener);
	}

	private synchronized void publishStatus(String message) {

		lastStatus = message;
		logEntries.add(message);
		if(logEntries.size() > 200) {
			logEntries.remove(0);
		}
		logger.info("[Virtual GC] " + message);
		notifyListeners(message);
	}

	private void notifyListeners(String message) {

		for(Consumer<String> listener : statusListeners) {
			try {
				listener.accept(message);
			} catch(RuntimeException e) {
				logger.warn("Status listener failed", e);
			}
		}
	}
}
