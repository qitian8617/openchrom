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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Line protocol: write COMMAND\\r\\n, read one ASCII response line. Used for the
 * existing READ_FID_STATUS / READ_GAS_PRESSURE polls (UART-TCP or Ethernet).
 */
public final class TcpInstrumentGateway implements IInstrumentGateway {

	private final String host;
	private final int port;
	private final int timeoutMs;
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;

	public TcpInstrumentGateway(String host, int port, int timeoutMs) {

		this.host = host;
		this.port = port;
		this.timeoutMs = timeoutMs;
	}

	@Override
	public synchronized boolean isConnected() {

		return socket != null && socket.isConnected() && !socket.isClosed();
	}

	@Override
	public synchronized void connect() throws IOException {

		disconnect();
		Socket next = new Socket();
		next.connect(new InetSocketAddress(host, port), timeoutMs);
		next.setSoTimeout(timeoutMs);
		this.socket = next;
		this.in = new BufferedReader(new InputStreamReader(next.getInputStream(), StandardCharsets.US_ASCII));
		this.out = new PrintWriter(new OutputStreamWriter(next.getOutputStream(), StandardCharsets.US_ASCII), true);
	}

	@Override
	public synchronized void disconnect() {

		if(out != null) {
			out.close();
			out = null;
		}
		if(in != null) {
			try {
				in.close();
			} catch(IOException ex) {
				// ignore
			}
			in = null;
		}
		if(socket != null) {
			try {
				socket.close();
			} catch(IOException ex) {
				// ignore
			}
			socket = null;
		}
	}

	@Override
	public synchronized String query(String command) throws IOException {

		if(!isConnected() || out == null || in == null) {
			throw new IOException("not connected");
		}
		out.print(command);
		out.print("\r\n");
		out.flush();
		if(out.checkError()) {
			throw new IOException("write failed");
		}
		String line = in.readLine();
		if(line == null) {
			throw new IOException("EOF");
		}
		return line.trim();
	}
}
