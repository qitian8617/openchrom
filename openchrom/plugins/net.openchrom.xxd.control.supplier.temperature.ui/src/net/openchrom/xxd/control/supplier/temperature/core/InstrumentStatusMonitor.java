/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.core;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class InstrumentStatusMonitor {

	private static final Logger LOG = Logger.getLogger(InstrumentStatusMonitor.class.getName());
	private static final InstrumentStatusMonitor INSTANCE = new InstrumentStatusMonitor();

	private final InstrumentStatusParser parser = new InstrumentStatusParser();
	private final List<Consumer<InstrumentReadiness>> listeners = new CopyOnWriteArrayList<>();
	private final Object lock = new Object();
	private IInstrumentGateway gateway = DisconnectedGateway.INSTANCE;
	private InstrumentReadiness latest = InstrumentReadiness.disconnected();
	private ScheduledExecutorService executor;
	private ScheduledFuture<?> pollTask;
	private boolean acquiring;

	public static InstrumentStatusMonitor getInstance() {

		return INSTANCE;
	}

	private InstrumentStatusMonitor() {
	}

	public void start() {

		synchronized(lock) {
			if(executor == null || executor.isShutdown()) {
				executor = Executors.newSingleThreadScheduledExecutor(runnable -> {

					Thread thread = new Thread(runnable, "gcws-instrument-status");
					thread.setDaemon(true);
					return thread;
				});
			}
			if(pollTask == null || pollTask.isCancelled()) {
				pollTask = executor.scheduleAtFixedRate(this::safePoll, 0, 1, TimeUnit.SECONDS);
			}
		}
	}

	public void stop() {

		synchronized(lock) {
			if(pollTask != null) {
				pollTask.cancel(false);
				pollTask = null;
			}
			if(executor != null) {
				executor.shutdownNow();
				executor = null;
			}
			gateway.disconnect();
			gateway = DisconnectedGateway.INSTANCE;
			acquiring = false;
			publish(InstrumentReadiness.disconnected());
		}
	}

	public void addListener(Consumer<InstrumentReadiness> listener) {

		if(listener != null) {
			listeners.add(listener);
			listener.accept(snapshot());
		}
	}

	public void removeListener(Consumer<InstrumentReadiness> listener) {

		listeners.remove(listener);
	}

	public InstrumentReadiness snapshot() {

		synchronized(lock) {
			return latest;
		}
	}

	public boolean isAcquiring() {

		synchronized(lock) {
			return acquiring;
		}
	}

	public void setAcquiring(boolean acquiring) {

		synchronized(lock) {
			this.acquiring = acquiring;
		}
	}

	public void connectTcp(String host, int port, int timeoutMs) throws IOException {

		TcpInstrumentGateway next = new TcpInstrumentGateway(host, port, timeoutMs);
		try {
			next.connect();
		} catch(IOException ex) {
			publish(InstrumentReadiness.connectFailed(ex.getMessage()));
			throw ex;
		}
		synchronized(lock) {
			gateway.disconnect();
			gateway = next;
		}
		InstrumentPreferences.setHost(host);
		InstrumentPreferences.setPort(port);
		publish(poll());
	}

	public void disconnect() {

		synchronized(lock) {
			gateway.disconnect();
			gateway = DisconnectedGateway.INSTANCE;
			acquiring = false;
		}
		publish(InstrumentReadiness.disconnected());
	}

	/**
	 * Install a gateway for tests. Operator UI uses {@link #connectTcp}.
	 */
	public void installGateway(IInstrumentGateway next) {

		synchronized(lock) {
			if(gateway != null && gateway != next) {
				gateway.disconnect();
			}
			gateway = next == null ? DisconnectedGateway.INSTANCE : next;
		}
		publish(poll());
	}

	public String ignite() throws IOException {

		IInstrumentGateway use;
		synchronized(lock) {
			use = gateway;
		}
		if(!use.isConnected()) {
			throw new IOException("not connected");
		}
		String reply = use.query(InstrumentCommands.IGNITE);
		publish(poll());
		return reply;
	}

	public InstrumentReadiness poll() {

		IInstrumentGateway use;
		synchronized(lock) {
			use = gateway;
		}
		InstrumentReadiness readiness = evaluate(use);
		publish(readiness);
		return readiness;
	}

	public InstrumentReadiness evaluate(IInstrumentGateway use) {

		if(use == null || !use.isConnected()) {
			return InstrumentReadiness.disconnected();
		}
		try {
			String fidRaw = use.query(InstrumentCommands.READ_FID_STATUS);
			FidStatus fid = parser.parseFidStatus(fidRaw);
			String gasRaw = use.query(InstrumentCommands.READ_GAS_PRESSURE);
			GasPressure gas = parser.parseGasPressure(gasRaw);
			TemperatureSnapshot temps = parser.parseTemperatures(fidRaw, false);
			try {
				String tempRaw = use.query(InstrumentCommands.READ_TEMPERATURES);
				TemperatureSnapshot dedicated = parser.parseTemperatures(tempRaw, true);
				if(dedicated.isPresent()) {
					temps = dedicated;
				}
			} catch(IOException ex) {
				LOG.log(Level.FINE, "optional READ_TEMPERATURES skipped", ex);
			}
			return InstrumentReadiness.fromPoll(true, fid, gas, temps, null);
		} catch(IOException ex) {
			return InstrumentReadiness.fromPoll(true, null, null, TemperatureSnapshot.empty(), ex.getMessage());
		}
	}

	private void safePoll() {

		try {
			poll();
		} catch(RuntimeException ex) {
			LOG.log(Level.WARNING, "instrument status poll failed", ex);
		}
	}

	private void publish(InstrumentReadiness readiness) {

		synchronized(lock) {
			latest = readiness;
		}
		for(Consumer<InstrumentReadiness> listener : listeners) {
			try {
				listener.accept(readiness);
			} catch(RuntimeException ex) {
				LOG.log(Level.FINE, "status listener failed", ex);
			}
		}
	}
}
