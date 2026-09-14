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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

public class InstrumentStatusMonitor_1_Test {

	@Test
	public void disconnectedGatewayYieldsDisconnectedReadiness() {

		InstrumentReadiness readiness = InstrumentStatusMonitor.getInstance().evaluate(DisconnectedGateway.INSTANCE);
		assertEquals(InstrumentReadiness.Kind.DISCONNECTED, readiness.getKind());
	}

	@Test
	public void scriptedPollsUseExistingReadsOnly() throws IOException {

		ScriptedInstrumentGateway gateway = new ScriptedInstrumentGateway() //
				.reply(InstrumentCommands.READ_FID_STATUS, "online=1 flame=1 currentPa=9.1 state=LIT") //
				.reply(InstrumentCommands.READ_GAS_PRESSURE, "h2=0.22 air=0.31") //
				.reply(InstrumentCommands.READ_TEMPERATURES, "oven=79.5/80 inlet=220/220");
		gateway.connect();
		InstrumentReadiness readiness = InstrumentStatusMonitor.getInstance().evaluate(gateway);
		assertEquals(InstrumentReadiness.Kind.READY, readiness.getKind());
		assertEquals(0.22d, readiness.getGasPressure().getHydrogenMPa(), 0.0001d);
		assertTrue(readiness.getTemperatures().isPresent());
		assertTrue(readiness.getTemperatures().isFromDedicatedRead());
	}

	@Test
	public void queryFailureIsStatusReadFailed() throws IOException {

		ScriptedInstrumentGateway gateway = new ScriptedInstrumentGateway().failQuery(new IOException("link down"));
		gateway.connect();
		InstrumentReadiness readiness = InstrumentStatusMonitor.getInstance().evaluate(gateway);
		assertEquals(InstrumentReadiness.Kind.STATUS_READ_FAILED, readiness.getKind());
		assertTrue(readiness.operatorMessage(java.util.Locale.CHINA).contains("状态读取失败"));
	}

	@Test
	public void tcpLineProtocolRoundTrip() throws Exception {

		ExecutorService pool = Executors.newSingleThreadExecutor();
		try(ServerSocket server = new ServerSocket(0)) {
			int port = server.getLocalPort();
			pool.submit(() -> {

				try(Socket socket = server.accept(); BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII)); PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII), true)) {
					for(int i = 0; i < 3; i++) {
						String command = in.readLine();
						if(InstrumentCommands.READ_FID_STATUS.equals(command)) {
							out.print("online=1 flame=1 currentPa=4.5\r\n");
						} else if(InstrumentCommands.READ_GAS_PRESSURE.equals(command)) {
							out.print("h2=0.21 air=0.29\r\n");
						} else {
							out.print("unsupported\r\n");
						}
						out.flush();
					}
				} catch(IOException ex) {
					throw new RuntimeException(ex);
				}
				return null;
			});
			TcpInstrumentGateway gateway = new TcpInstrumentGateway("127.0.0.1", port, 2000);
			gateway.connect();
			try {
				InstrumentReadiness readiness = InstrumentStatusMonitor.getInstance().evaluate(gateway);
				assertEquals(InstrumentReadiness.Kind.READY, readiness.getKind());
				assertEquals(0.21d, readiness.getGasPressure().getHydrogenMPa(), 0.0001d);
				assertEquals(4.5d, readiness.getFidStatus().getCurrentPa(), 0.001d);
			} finally {
				gateway.disconnect();
			}
		} finally {
			pool.shutdownNow();
			pool.awaitTermination(2, TimeUnit.SECONDS);
		}
	}
}
