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

import java.util.Arrays;

public final class GcFrame {

	private final byte command;
	private final byte sequence;
	private final byte[] payload;

	public GcFrame(byte command, byte sequence, byte[] payload) {

		this.command = command;
		this.sequence = sequence;
		this.payload = payload == null ? new byte[0] : payload.clone();
	}

	public byte getCommand() {

		return command;
	}

	public byte getSequence() {

		return sequence;
	}

	public byte[] getPayload() {

		return payload.clone();
	}

	public int getPayloadLength() {

		return payload.length;
	}

	@Override
	public String toString() {

		return "GcFrame{command=" + GcCommand.toString(command) + ", sequence=" + (sequence & 0xFF) + ", payloadLength=" + payload.length + "}";
	}

	@Override
	public boolean equals(Object object) {

		if(this == object) {
			return true;
		}
		if(!(object instanceof GcFrame other)) {
			return false;
		}
		return command == other.command && sequence == other.sequence && Arrays.equals(payload, other.payload);
	}

	@Override
	public int hashCode() {

		int result = command;
		result = 31 * result + sequence;
		result = 31 * result + Arrays.hashCode(payload);
		return result;
	}
}
