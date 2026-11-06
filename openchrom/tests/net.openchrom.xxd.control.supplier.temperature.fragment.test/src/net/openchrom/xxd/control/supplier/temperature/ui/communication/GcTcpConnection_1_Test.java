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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

public class GcTcpConnection_1_Test {

	@Test
	public void ackMustMatchCommandAndSuccessStatus() {

		assertDoesNotThrow(() -> GcTcpConnection.validateAck(new GcFrame(GcCommand.ACK, (byte)1, new byte[] {GcCommand.START_ACQ, 0}), GcCommand.START_ACQ));
		assertThrows(IOException.class, () -> GcTcpConnection.validateAck(new GcFrame(GcCommand.ACK, (byte)1, new byte[] {GcCommand.STOP_ACQ, 0}), GcCommand.START_ACQ));
		assertThrows(IOException.class, () -> GcTcpConnection.validateAck(new GcFrame(GcCommand.ACK, (byte)1, new byte[] {GcCommand.START_ACQ, 1}), GcCommand.START_ACQ));
		assertThrows(IOException.class, () -> GcTcpConnection.validateAck(new GcFrame(GcCommand.ACQ_DATA, (byte)1, new byte[0]), GcCommand.START_ACQ));
	}

	@Test
	public void dataFrameWithRequestSequenceDoesNotCompleteCommand() throws Exception {

		try(ServerSocket server = new ServerSocket(0, 2, InetAddress.getLoopbackAddress())) {
			CompletableFuture<Void> device = CompletableFuture.runAsync(() -> {
				try(Socket socket = server.accept()) {
					handshake(socket);
					GcFrame request = readFrame(socket);
					writeFrame(socket, GcFrameCodec.encode(GcCommand.ACQ_DATA, request.getSequence(), new byte[0]));
					Thread.sleep(500);
				} catch(IOException e) {
					throw new UncheckedIOException(e);
				} catch(InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
			GcTcpConnection connection = new GcTcpConnection();
			try {
				connection.connect(endpoint(server));
				IOException error = assertThrows(IOException.class, () -> connection.sendFrameAndWait(GcCommand.START_ACQ, new byte[] {100, 0}, 150));
				assertTrue(error.getMessage().contains("Response timeout"), error.getMessage());
			} finally {
				connection.disconnect();
			}
			assertDoesNotThrow(() -> device.get(5, TimeUnit.SECONDS));
		}
	}

	@Test
	public void reconnectDoesNotLetOldReaderCloseNewSocket() throws Exception {

		try(ServerSocket server = new ServerSocket(0, 2, InetAddress.getLoopbackAddress())) {
			CompletableFuture<Void> device = CompletableFuture.runAsync(() -> {
				try(Socket first = server.accept()) {
					handshake(first);
					try(Socket second = server.accept()) {
						handshake(second);
						GcFrame request = readFrame(second);
						writeFrame(second, GcFrameCodec.encodeAck(request.getSequence(), GcCommand.HEARTBEAT, (byte)0));
					}
				} catch(IOException e) {
					throw new UncheckedIOException(e);
				}
			});
			GcTcpConnection connection = new GcTcpConnection();
			try {
				connection.connect(endpoint(server));
				connection.connect(endpoint(server));
				assertTrue(connection.isConnected());
				GcFrame response = connection.sendFrameAndWait(GcCommand.HEARTBEAT, new byte[0], 2000);
				assertEquals(GcCommand.ACK, response.getCommand());
			} finally {
				connection.disconnect();
			}
			assertDoesNotThrow(() -> device.get(5, TimeUnit.SECONDS));
		}
	}

	private static GcDeviceEndpoint endpoint(ServerSocket server) {

		return new GcDeviceEndpoint(server.getInetAddress().getHostAddress(), server.getLocalPort());
	}

	private static void handshake(Socket socket) throws IOException {

		InputStream input = socket.getInputStream();
		StringBuilder line = new StringBuilder();
		while(true) {
			int value = input.read();
			if(value < 0) {
				throw new IOException("Client closed before handshake");
			}
			line.append((char)value);
			if(line.toString().endsWith("\n")) {
				break;
			}
		}
		assertTrue(line.toString().contains("GCWS\tHELLO\tPC"));
		OutputStream output = socket.getOutputStream();
		output.write("HELLO_OK\n".getBytes(StandardCharsets.US_ASCII));
		output.flush();
	}

	private static GcFrame readFrame(Socket socket) throws IOException {

		GcFrameParser parser = new GcFrameParser();
		InputStream input = socket.getInputStream();
		byte[] buffer = new byte[256];
		while(true) {
			int count = input.read(buffer);
			if(count < 0) {
				throw new IOException("Client closed before request");
			}
			parser.append(java.util.Arrays.copyOf(buffer, count));
			List<GcFrame> frames = parser.drainFrames();
			if(!frames.isEmpty()) {
				return frames.get(0);
			}
		}
	}

	private static void writeFrame(Socket socket, byte[] frame) throws IOException {

		OutputStream output = socket.getOutputStream();
		output.write(frame);
		output.flush();
	}
}
