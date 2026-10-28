/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.acquisition;

import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadiness;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadinessMonitor;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.FidReadinessSnapshot;

/**
 * Shared Start Analysis toggle for MainView, the plant toolbar, and the
 * Baijiu shell (reflection on {@code toggle}). Same gate everywhere.
 * <p>
 * 电流判据暂不可用，先不拦开始采集 — {@link FidReadiness#REQUIRE_FID_CURRENT_FOR_START}
 * is {@code false}, so flame / FID current does not block. Not connected still
 * blocks. Already acquiring toggles to stop. Current pA stays on the strip.
 */
public final class AcquisitionStartGate {

	public record Outcome(boolean ok, boolean acquiring, String title, String message) {

		public static Outcome started() {

			return new Outcome(true, true, "", "");
		}

		public static Outcome stopped() {

			return new Outcome(true, false, "", "");
		}

		public static Outcome blocked(String title, String message) {

			return new Outcome(false, false, title == null ? "" : title, message == null ? "" : message);
		}
	}

	private AcquisitionStartGate() {

	}

	public static Outcome toggle(boolean chinese) {

		RealtimeAcquisitionManager manager = RealtimeAcquisitionManager.getInstance();
		if(manager.isAcquiring()) {
			manager.stopAcquisition();
			return Outcome.stopped();
		}
		FidReadinessSnapshot snapshot = FidReadinessMonitor.getInstance().getSnapshot();
		// 电流判据暂不可用，先不拦开始采集. Disconnected and FID-board-offline still block.
		if(!snapshot.canStartAnalysis()) {
			return Outcome.blocked(FidReadiness.startBlockedTitle(chinese), snapshot.operatorTip(chinese));
		}
		manager.startAcquisition();
		return Outcome.started();
	}
}
