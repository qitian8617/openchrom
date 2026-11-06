/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.engineering;

/**
 * Connected / disconnected control permissions already verified on the
 * reverse-control panel. Call sites keep these predicates so a later
 * layout change cannot quietly redefine them.
 * <p>
 * Heat stop stays available while a temperature session is running.
 * Acquisition start stays in {@code FidReadiness}; this class does not
 * replace that gate. Engineer authorization is a separate check and is
 * not folded into these operator predicates.
 */
public final class ConsoleControlPolicy {

	private ConsoleControlPolicy() {

	}

	/**
	 * Main temperature toggle. Busy wins. While running, the button stays
	 * enabled so stop is still available, including after a fault. Idle
	 * start is blocked only by an oven fault; connection is checked when
	 * the command is sent.
	 */
	public static boolean mainHeatToggleEnabled(boolean busy, boolean running, boolean ovenFault) {

		if(busy) {
			return false;
		}
		if(running) {
			return true;
		}
		return !ovenFault;
	}

	public static boolean mainTrendStartEnabled(boolean recording, boolean connected, boolean tempControlRunning) {

		return !recording && connected && !tempControlRunning;
	}

	public static boolean mainTrendStopEnabled(boolean recording, boolean tempControlRunning) {

		return recording && !tempControlRunning;
	}

	public static boolean detectorIgniteEnabled(boolean connected, boolean busy, boolean ignitionLocked) {

		return connected && !busy && !ignitionLocked;
	}

	public static boolean detectorValveEnabled(boolean connected, boolean busy, boolean valveStateKnown) {

		return connected && !busy && valveStateKnown;
	}

	public static boolean detectorAutoIgniteEnabled(boolean connected, boolean busy) {

		return connected && !busy;
	}

	/** {@code CommunicationSettingsView#updateButtonStates}. */
	public static boolean communicationConnectEnabled(boolean connected, boolean scanning) {

		return !connected && !scanning;
	}

	public static boolean communicationDisconnectEnabled(boolean connected, boolean scanning) {

		return connected && !scanning;
	}

	public static boolean communicationRefreshEnabled(boolean scanning) {

		return !scanning;
	}

	/** {@code CommunicationSettingsView#setBusy}. */
	public static boolean communicationConnectEnabledWhenBusy(boolean busy, boolean connected) {

		return !busy && !connected;
	}

	public static boolean communicationDisconnectEnabledWhenBusy(boolean busy, boolean connected) {

		return !busy && connected;
	}

	public static boolean communicationRefreshEnabledWhenBusy(boolean busy) {

		return !busy;
	}

	public static boolean pidDebugStartEnabled(boolean connected, boolean busy, boolean heating, boolean recording, boolean pidDebugRunning) {

		return connected && !busy && !heating && !recording && !pidDebugRunning;
	}

	public static boolean pidDebugStopHeatEnabled(boolean connected, boolean busy, boolean heating, boolean pidDebugRunning) {

		return connected && (heating || pidDebugRunning) && !busy;
	}

	public static boolean columnProgramEditEnabled(boolean busy) {

		return !busy;
	}
}
