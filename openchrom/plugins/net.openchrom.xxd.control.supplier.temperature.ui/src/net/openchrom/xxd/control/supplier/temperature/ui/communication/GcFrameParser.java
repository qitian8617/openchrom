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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GcFrameParser {

	private static final Logger LOGGER = Logger.getLogger(GcFrameParser.class.getName());
	private static final int MAX_BUFFER_BYTES = 65536;

	private final List<Byte> buffer = new ArrayList<>();

	public synchronized void append(byte[] chunk) {

		if(chunk == null || chunk.length == 0) {
			return;
		}
		for(byte value : chunk) {
			buffer.add(value);
		}
		if(buffer.size() > MAX_BUFFER_BYTES) {
			int overflow = buffer.size() - MAX_BUFFER_BYTES;
			buffer.subList(0, overflow).clear();
			LOGGER.log(Level.WARNING, "GC frame parser buffer overflow, dropped {0} bytes", overflow);
		}
	}

	public synchronized List<GcFrame> drainFrames() {

		List<GcFrame> frames = new ArrayList<>();
		while(true) {
			int headerIndex = findHeader();
			if(headerIndex < 0) {
				if(buffer.size() > 1) {
					buffer.subList(0, buffer.size() - 1).clear();
				}
				break;
			}
			if(headerIndex > 0) {
				buffer.subList(0, headerIndex).clear();
			}
			if(buffer.size() < GcFrameCodec.MIN_FRAME_BYTES) {
				break;
			}
			int payloadLength = (buffer.get(4) & 0xFF) | ((buffer.get(5) & 0xFF) << 8);
			int frameLength = GcFrameCodec.FIXED_OVERHEAD + payloadLength;
			if(buffer.size() < frameLength) {
				break;
			}
			byte[] raw = new byte[frameLength];
			for(int index = 0; index < frameLength; index++) {
				raw[index] = buffer.get(index);
			}
			try {
				frames.add(GcFrameCodec.decode(raw));
				buffer.subList(0, frameLength).clear();
			} catch(IllegalArgumentException e) {
				LOGGER.log(Level.WARNING, "Discarded invalid GC frame: {0} [{1}]", new Object[] {e.getMessage(), GcFrameCodec.toHex(raw)});
				buffer.remove(0);
			}
		}
		return frames;
	}

	public synchronized void reset() {

		buffer.clear();
	}

	private int findHeader() {

		for(int index = 0; index < buffer.size() - 1; index++) {
			if(buffer.get(index) == GcFrameCodec.HEADER_0 && buffer.get(index + 1) == GcFrameCodec.HEADER_1) {
				return index;
			}
		}
		return -1;
	}
}
