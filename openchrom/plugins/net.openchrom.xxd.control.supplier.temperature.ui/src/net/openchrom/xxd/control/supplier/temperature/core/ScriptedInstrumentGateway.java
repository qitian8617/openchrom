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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Test / lab double. Not used by the operator UI unless installed explicitly.
 */
public final class ScriptedInstrumentGateway implements IInstrumentGateway {

	private final Map<String, String> replies = new LinkedHashMap<>();
	private boolean connected;
	private IOException connectError;
	private IOException queryError;

	public ScriptedInstrumentGateway reply(String command, String body) {

		replies.put(command, body);
		return this;
	}

	public ScriptedInstrumentGateway failConnect(IOException error) {

		this.connectError = error;
		return this;
	}

	public ScriptedInstrumentGateway failQuery(IOException error) {

		this.queryError = error;
		return this;
	}

	@Override
	public boolean isConnected() {

		return connected;
	}

	@Override
	public void connect() throws IOException {

		if(connectError != null) {
			throw connectError;
		}
		connected = true;
	}

	@Override
	public void disconnect() {

		connected = false;
	}

	@Override
	public String query(String command) throws IOException {

		if(!connected) {
			throw new IOException("not connected");
		}
		if(queryError != null) {
			throw queryError;
		}
		String reply = replies.get(command);
		if(reply == null) {
			throw new IOException("no scripted reply for " + command);
		}
		return reply;
	}
}
