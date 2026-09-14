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

public final class DisconnectedGateway implements IInstrumentGateway {

	public static final DisconnectedGateway INSTANCE = new DisconnectedGateway();

	private DisconnectedGateway() {
	}

	@Override
	public boolean isConnected() {

		return false;
	}

	@Override
	public void connect() {

		// no-op: remains disconnected until a live gateway is installed
	}

	@Override
	public void disconnect() {

		// already disconnected
	}

	@Override
	public String query(String command) throws IOException {

		throw new IOException("not connected");
	}
}
