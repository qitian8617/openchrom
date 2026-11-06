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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

import org.eclipse.chemclipse.logging.core.Logger;

import net.openchrom.xxd.control.supplier.temperature.ui.communication.GcDeviceScanner;

public final class VirtualGcTcpServer implements Runnable {

	private static final Logger logger = Logger.getLogger(VirtualGcTcpServer.class);

	private final int port;
	private final Consumer<String> statusListener;
	private final Runnable onStopped;

	private ServerSocket serverSocket;
	private VirtualGcClientSession activeSession;
	private Thread acceptThread;

	public VirtualGcTcpServer(int port, Consumer<String> statusListener, Runnable onStopped) {

		this.port = port;
		this.statusListener = statusListener;
		this.onStopped = onStopped;
	}

	public synchronized void start() throws IOException {

		if(serverSocket != null && !serverSocket.isClosed()) {
			return;
		}
		serverSocket = new ServerSocket();
		serverSocket.bind(new InetSocketAddress(port));
		acceptThread = Thread.ofVirtual().name("virtual-gc-server").start(this);
		notifyStatus("Listening on port " + port);
	}

	public synchronized void stop() {

		closeActiveSession();
		if(serverSocket != null) {
			try {
				serverSocket.close();
			} catch(IOException e) {
				logger.warn("Failed to close server socket", e);
			}
			serverSocket = null;
		}
		if(acceptThread != null) {
			acceptThread.interrupt();
			acceptThread = null;
		}
		onStopped.run();
	}

	public boolean isRunning() {

		return serverSocket != null && !serverSocket.isClosed();
	}

	public int getPort() {

		return port;
	}

	@Override
	public void run() {

		try {
			while(serverSocket != null && !serverSocket.isClosed()) {
				Socket clientSocket = serverSocket.accept();
				clientSocket.setTcpNoDelay(true);
				synchronized(this) {
					closeActiveSession();
					activeSession = new VirtualGcClientSession(clientSocket, statusListener, this::closeActiveSession);
					Thread.ofVirtual().name("virtual-gc-client").start(activeSession);
				}
			}
		} catch(IOException e) {
			if(serverSocket != null && !serverSocket.isClosed()) {
				logger.warn("Virtual GC server accept loop failed", e);
				notifyStatus("Server error: " + e.getMessage());
			}
		}
	}

	public static int defaultPort() {

		return GcDeviceScanner.DEFAULT_GC_PORT;
	}

	private synchronized void closeActiveSession() {

		if(activeSession != null) {
			activeSession.close();
			activeSession = null;
		}
	}

	private void notifyStatus(String message) {

		if(statusListener != null) {
			statusListener.accept(message);
		}
	}
}
