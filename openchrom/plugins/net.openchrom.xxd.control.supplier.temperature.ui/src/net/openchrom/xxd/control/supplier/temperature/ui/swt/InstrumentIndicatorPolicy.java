/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.swt;

/**
 * Display/debug settings may hide extra FID current and the live trend.
 * Connection, flame, and acquiring stay on screen in every console page.
 */
public final class InstrumentIndicatorPolicy {

	public static final String CONNECTION = "connection";
	public static final String FLAME = "flame";
	public static final String ACQUIRING = "acquiring";
	public static final String FID_CURRENT = "fidCurrent";
	public static final String TREND = "trend";

	private InstrumentIndicatorPolicy() {

	}

	/**
	 * @return {@code true} only for debug chrome. The three operator
	 *         indicators, and any unknown role, stay visible.
	 */
	public static boolean debugDisplayMayHide(String role) {

		return FID_CURRENT.equals(role) || TREND.equals(role);
	}
}
