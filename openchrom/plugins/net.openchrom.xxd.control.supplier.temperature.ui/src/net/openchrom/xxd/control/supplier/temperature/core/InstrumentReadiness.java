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

import java.util.Locale;

public final class InstrumentReadiness {

	public enum Kind {
		DISCONNECTED, CONNECT_FAILED, STATUS_READ_FAILED, FID_OFFLINE, IGNITE_FAILED, FLAME_OFF, SIGNAL_ABSENT, READY
	}

	private final boolean connected;
	private final Kind kind;
	private final FidStatus fidStatus;
	private final GasPressure gasPressure;
	private final TemperatureSnapshot temperatures;
	private final String detail;
	private final long timestampMs;

	public InstrumentReadiness(boolean connected, Kind kind, FidStatus fidStatus, GasPressure gasPressure, TemperatureSnapshot temperatures, String detail, long timestampMs) {

		this.connected = connected;
		this.kind = kind;
		this.fidStatus = fidStatus;
		this.gasPressure = gasPressure;
		this.temperatures = temperatures == null ? TemperatureSnapshot.empty() : temperatures;
		this.detail = detail;
		this.timestampMs = timestampMs;
	}

	public static InstrumentReadiness disconnected() {

		return new InstrumentReadiness(false, Kind.DISCONNECTED, null, null, TemperatureSnapshot.empty(), null, System.currentTimeMillis());
	}

	public static InstrumentReadiness connectFailed(String detail) {

		return new InstrumentReadiness(false, Kind.CONNECT_FAILED, null, null, TemperatureSnapshot.empty(), detail, System.currentTimeMillis());
	}

	public static InstrumentReadiness fromPoll(boolean connected, FidStatus fid, GasPressure gas, TemperatureSnapshot temps, String readError) {

		long now = System.currentTimeMillis();
		if(!connected) {
			return new InstrumentReadiness(false, Kind.DISCONNECTED, fid, gas, temps, readError, now);
		}
		if(readError != null && !readError.isBlank()) {
			return new InstrumentReadiness(true, Kind.STATUS_READ_FAILED, fid, gas, temps, readError, now);
		}
		if(fid == null || !fid.isParsed()) {
			return new InstrumentReadiness(true, Kind.STATUS_READ_FAILED, fid, gas, temps, "READ_FID_STATUS", now);
		}
		if(gas == null || !gas.isParsed()) {
			return new InstrumentReadiness(true, Kind.STATUS_READ_FAILED, fid, gas, temps, "READ_GAS_PRESSURE", now);
		}
		if(!fid.isBoardOnline()) {
			return new InstrumentReadiness(true, Kind.FID_OFFLINE, fid, gas, temps, null, now);
		}
		if(fid.isIgniteFailed()) {
			return new InstrumentReadiness(true, Kind.IGNITE_FAILED, fid, gas, temps, fid.getState(), now);
		}
		if(Boolean.FALSE.equals(fid.getFlameOn())) {
			return new InstrumentReadiness(true, Kind.FLAME_OFF, fid, gas, temps, null, now);
		}
		if(Boolean.TRUE.equals(fid.getFlameOn()) && fid.getCurrentPa() != null && !fid.hasFidSignal()) {
			return new InstrumentReadiness(true, Kind.SIGNAL_ABSENT, fid, gas, temps, null, now);
		}
		return new InstrumentReadiness(true, Kind.READY, fid, gas, temps, null, now);
	}

	public boolean isConnected() {

		return connected;
	}

	public Kind getKind() {

		return kind;
	}

	public FidStatus getFidStatus() {

		return fidStatus;
	}

	public GasPressure getGasPressure() {

		return gasPressure;
	}

	public TemperatureSnapshot getTemperatures() {

		return temperatures;
	}

	public String getDetail() {

		return detail;
	}

	public long getTimestampMs() {

		return timestampMs;
	}

	public boolean allowsAcquisition() {

		return isConnected() //
				&& kind != Kind.STATUS_READ_FAILED //
				&& kind != Kind.FID_OFFLINE //
				&& kind != Kind.IGNITE_FAILED //
				&& fidStatus != null //
				&& fidStatus.isBoardOnline() //
				&& Boolean.TRUE.equals(fidStatus.getFlameOn()) //
				&& !fidStatus.isIgniteFailed();
	}

	public boolean isFailure() {

		return kind == Kind.DISCONNECTED || kind == Kind.CONNECT_FAILED || kind == Kind.STATUS_READ_FAILED || kind == Kind.FID_OFFLINE || kind == Kind.IGNITE_FAILED;
	}

	public String operatorMessage(Locale locale) {

		switch(kind) {
			case DISCONNECTED:
				return OperatorMessages.disconnected(locale);
			case CONNECT_FAILED:
				return OperatorMessages.connectFailed(locale, detail);
			case STATUS_READ_FAILED:
				return OperatorMessages.statusReadFailed(locale, detail);
			case FID_OFFLINE:
				return OperatorMessages.fidOffline(locale);
			case IGNITE_FAILED:
				return OperatorMessages.igniteFailed(locale);
			case FLAME_OFF:
				return OperatorMessages.flameOff(locale);
			case SIGNAL_ABSENT:
				return OperatorMessages.signalAbsent(locale);
			case READY:
			default:
				return OperatorMessages.ready(locale);
		}
	}
}
