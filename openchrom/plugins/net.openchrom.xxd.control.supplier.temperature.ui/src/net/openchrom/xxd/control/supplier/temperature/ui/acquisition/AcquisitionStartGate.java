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
 * Shared Start Analysis toggle for MainView and the dedicated-shell toolbar.
 * Same FID gate as the reverse-control Start button; no second protocol.
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
		if(!snapshot.canStartAnalysis()) {
			return Outcome.blocked(FidReadiness.startBlockedTitle(chinese), snapshot.operatorTip(chinese));
		}
		manager.startAcquisition();
		return Outcome.started();
	}
}
