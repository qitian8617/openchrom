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

/**
 * Modbus CRC-16 (polynomial 0xA001, init 0xFFFF).
 */
public final class GcCrc16 {

	private GcCrc16() {
	}

	public static int compute(byte[] data, int offset, int length) {

		int crc = 0xFFFF;
		for(int index = offset; index < offset + length; index++) {
			crc ^= data[index] & 0xFF;
			for(int bit = 0; bit < 8; bit++) {
				if((crc & 0x0001) != 0) {
					crc = (crc >>> 1) ^ 0xA001;
				} else {
					crc >>>= 1;
				}
			}
		}
		return crc & 0xFFFF;
	}
}
