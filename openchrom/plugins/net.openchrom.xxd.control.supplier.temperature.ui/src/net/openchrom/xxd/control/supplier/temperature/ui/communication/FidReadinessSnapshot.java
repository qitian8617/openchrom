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

import java.util.Objects;

/**
 * Last connection + {@code READ_FID_STATUS} + {@code READ_GAS_PRESSURE} sample.
 */
public final class FidReadinessSnapshot {

	public static final FidReadinessSnapshot DISCONNECTED = new FidReadinessSnapshot(false, null, null, null, null);

	private final boolean connected;
	private final GcTcpConnection.FidStatus fidStatus;
	private final GcTcpConnection.GasPressure pressure;
	private final String fidReadError;
	private final String pressureReadError;

	public FidReadinessSnapshot(boolean connected, GcTcpConnection.FidStatus fidStatus, GcTcpConnection.GasPressure pressure, String fidReadError, String pressureReadError) {

		this.connected = connected;
		this.fidStatus = fidStatus;
		this.pressure = pressure;
		this.fidReadError = blankToNull(fidReadError);
		this.pressureReadError = blankToNull(pressureReadError);
	}

	public boolean isConnected() {

		return connected;
	}

	public GcTcpConnection.FidStatus getFidStatus() {

		return fidStatus;
	}

	public GcTcpConnection.GasPressure getPressure() {

		return pressure;
	}

	public String getFidReadError() {

		return fidReadError;
	}

	public String getPressureReadError() {

		return pressureReadError;
	}

	public FidReadiness.Kind kind() {

		return FidReadiness.classify(connected, fidStatus, fidReadError);
	}

	public boolean canStartAnalysis() {

		return FidReadiness.canStartAnalysis(kind());
	}

	public String h2Text() {

		return FidReadiness.formatMpa(pressure == null ? null : Float.valueOf(pressure.getH2Mpa()));
	}

	public String airText() {

		return FidReadiness.formatMpa(pressure == null ? null : Float.valueOf(pressure.getAirMpa()));
	}

	public String fidPaText() {

		return FidReadiness.formatPa(fidStatus);
	}

	public String flameText(boolean chinese) {

		return FidReadiness.flameText(fidStatus, chinese);
	}

	public String connectionText(boolean chinese) {

		return FidReadiness.connectionText(connected, chinese);
	}

	public String operatorTip(boolean chinese) {

		return FidReadiness.operatorTip(kind(), chinese);
	}

	public String title(boolean chinese) {

		return FidReadiness.title(kind(), chinese);
	}

	@Override
	public boolean equals(Object obj) {

		if(this == obj) {
			return true;
		}
		if(!(obj instanceof FidReadinessSnapshot other)) {
			return false;
		}
		return connected == other.connected
				&& Objects.equals(fidReadError, other.fidReadError)
				&& Objects.equals(pressureReadError, other.pressureReadError)
				&& sameFid(fidStatus, other.fidStatus)
				&& samePressure(pressure, other.pressure);
	}

	@Override
	public int hashCode() {

		return Objects.hash(Boolean.valueOf(connected), fidReadError, pressureReadError, fidKey(fidStatus), pressure == null ? null : Float.valueOf(pressure.getH2Mpa()), pressure == null ? null : Float.valueOf(pressure.getAirMpa()));
	}

	private static String blankToNull(String value) {

		if(value == null || value.isBlank()) {
			return null;
		}
		return value;
	}

	private static boolean sameFid(GcTcpConnection.FidStatus a, GcTcpConnection.FidStatus b) {

		if(a == b) {
			return true;
		}
		if(a == null || b == null) {
			return false;
		}
		return a.isOnline() == b.isOnline()
				&& a.isAutoIgnite() == b.isAutoIgnite()
				&& a.isBusy() == b.isBusy()
				&& a.isFlame() == b.isFlame()
				&& a.isValve1() == b.isValve1()
				&& a.isValve2() == b.isValve2()
				&& a.getCurrentPa() == b.getCurrentPa()
				&& a.isDetectorValid() == b.isDetectorValid()
				&& Float.compare(a.getDetectorC(), b.getDetectorC()) == 0
				&& Objects.equals(a.getState(), b.getState());
	}

	private static boolean samePressure(GcTcpConnection.GasPressure a, GcTcpConnection.GasPressure b) {

		if(a == b) {
			return true;
		}
		if(a == null || b == null) {
			return false;
		}
		return Float.compare(a.getH2Mpa(), b.getH2Mpa()) == 0 && Float.compare(a.getAirMpa(), b.getAirMpa()) == 0;
	}

	private static String fidKey(GcTcpConnection.FidStatus status) {

		if(status == null) {
			return null;
		}
		return status.isOnline() + "/" + status.isFlame() + "/" + status.isBusy() + "/" + status.getState() + "/" + status.getCurrentPa();
	}
}
