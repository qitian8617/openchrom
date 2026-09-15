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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.chemclipse.logging.core.Logger;

public class GcDeviceScanner {

	private static final Logger logger = Logger.getLogger(GcDeviceScanner.class);
	public static final int DEFAULT_GC_PORT = 8234;
	public static final int[] COMMON_GC_PORTS = {DEFAULT_GC_PORT, 5024, 8000, 9000};
	private static final int CONNECT_TIMEOUT_MS = 400;
	private static final int HANDSHAKE_PROBE_TIMEOUT_MS = 1200;

	private final ExecutorService scanCoordinator = Executors.newVirtualThreadPerTaskExecutor();
	private final AtomicBoolean scanning = new AtomicBoolean(false);
	private volatile Future<?> currentScan;

	public boolean isScanning() {

		return scanning.get();
	}

	public void cancel() {

		if(currentScan != null) {
			currentScan.cancel(true);
		}
	}

	public CompletableFuture<List<GcDeviceEndpoint>> scanAsync(int preferredPort) {

		cancel();
		scanning.set(true);
		CompletableFuture<List<GcDeviceEndpoint>> future = new CompletableFuture<>();
		currentScan = scanCoordinator.submit(() -> {
			try {
				if(!future.isCancelled()) {
					future.complete(scan(preferredPort));
				}
			} catch(Exception e) {
				future.completeExceptionally(e);
			} finally {
				scanning.set(false);
			}
		});
		future.whenComplete((result, error) -> {
			if(future.isCancelled()) {
				scanning.set(false);
			}
		});
		return future;
	}

	public List<GcDeviceEndpoint> scan(int preferredPort) {

		Set<GcDeviceEndpoint> found = Collections.synchronizedSet(new LinkedHashSet<>());
		int[] ports = resolvePorts(preferredPort);
		List<String> subnets = getLocalSubnetPrefixes();
		if(subnets.isEmpty()) {
			subnets = List.of("192.168.1", "192.168.0", "10.0.0");
		}

		List<Callable<Void>> tasks = new ArrayList<>();
		for(String prefix : subnets) {
			for(int host = 1; host <= 254; host++) {
				if(Thread.currentThread().isInterrupted()) {
					return new ArrayList<>(found);
				}
				String ip = prefix + "." + host;
				for(int port : ports) {
					tasks.add(() -> {
						if(probeEndpoint(ip, port)) {
							found.add(new GcDeviceEndpoint(ip, port));
						}
						return null;
					});
				}
			}
		}

		try(ExecutorService probePool = Executors.newVirtualThreadPerTaskExecutor()) {
			probePool.invokeAll(tasks, 30, TimeUnit.SECONDS);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return new ArrayList<>(found);
	}

	private static int[] resolvePorts(int preferredPort) {

		if(preferredPort > 0 && preferredPort <= 65535) {
			return new int[] {preferredPort};
		}
		return COMMON_GC_PORTS;
	}

	private static boolean probeEndpoint(String host, int port) {

		try(Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
			socket.setTcpNoDelay(true);
			socket.setSoTimeout(HANDSHAKE_PROBE_TIMEOUT_MS);
			/*
			 * Prefer F407 GCWS handshake so open ports that are not chromatographs are filtered out.
			 * Fall back to "port open" if the peer closes without answering (e.g. simulator).
			 */
			try {
				GcAsciiProtocol.performHandshake(socket);
				return true;
			} catch(IOException handshakeError) {
				logger.info("Handshake probe failed for " + host + ":" + port + " - " + handshakeError.getMessage());
				return false;
			}
		} catch(IOException e) {
			return false;
		}
	}

	static List<String> getLocalSubnetPrefixes() {

		List<String> prefixes = new CopyOnWriteArrayList<>();
		try {
			for(NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				if(!networkInterface.isUp() || networkInterface.isLoopback()) {
					continue;
				}
				for(InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
					if(address instanceof Inet4Address inet4 && !address.isLoopbackAddress()) {
						byte[] octets = inet4.getAddress();
						prefixes.add((octets[0] & 0xFF) + "." + (octets[1] & 0xFF) + "." + (octets[2] & 0xFF));
					}
				}
			}
		} catch(IOException e) {
			logger.warn("Failed to enumerate network interfaces", e);
		}
		return prefixes.stream().distinct().toList();
	}

	public void shutdown() {

		cancel();
		scanCoordinator.shutdownNow();
	}
}
