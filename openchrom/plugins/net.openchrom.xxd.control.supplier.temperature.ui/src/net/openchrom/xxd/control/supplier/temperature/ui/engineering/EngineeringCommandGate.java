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

import java.io.IOException;
import java.util.Set;

/**
 * Classifies ASCII commands before they are written to the socket, including
 * {@code sendProtocolLine}. Safety stops and operator commands stay available
 * when engineer mode is locked. PID writes need a live grant. Anything else
 * that looks like a command we do not implement is not sent.
 */
public final class EngineeringCommandGate {

	public enum Kind {
		READ_OR_HANDSHAKE, SAFETY_STOP, OPERATOR_WRITE, ENGINEERING_WRITE, UNSUPPORTED
	}

	private static final Set<String> READS = Set.of( //
			"HELLO", //
			"READ_OVEN_PROGRAM", //
			"READ_OVEN_PID", //
			"READ_INLET_PID", //
			"READ_DETECTOR_PID", //
			"READ_OVEN_TEMP", //
			"READ_INLET_TEMP", //
			"READ_DETECTOR_TEMP", //
			"READ_FID_STATUS", //
			"READ_GAS_PRESSURE", //
			"READ_CONTROL_STATUS");

	private static final Set<String> SAFETY = Set.of( //
			"STOP_INLET_HEAT", //
			"STOP_DETECTOR_HEAT", //
			"STOP_OVEN_CONTROL", //
			"STOP_FID_VALVES");

	private static final Set<String> OPERATOR = Set.of( //
			"WRITE_OVEN_PROGRAM", //
			"WRITE_INLET_TEMP", //
			"WRITE_DETECTOR_TEMP", //
			"WRITE_ZONE_SELECT", //
			"START_INLET_HEAT", //
			"START_DETECTOR_HEAT", //
			"START_OVEN_CONTROL", //
			"START_FID_IGNITE", //
			"START_FID_VALVES", //
			"FID_AUTO_ON", //
			"FID_AUTO_OFF");

	private static final Set<String> ENGINEERING = Set.of( //
			"WRITE_OVEN_PID", //
			"WRITE_INLET_PID", //
			"WRITE_DETECTOR_PID");

	private EngineeringCommandGate() {

	}

	public static Kind classify(String line) {

		String verb = verb(line);
		if(READS.contains(verb)) {
			return Kind.READ_OR_HANDSHAKE;
		}
		if(SAFETY.contains(verb)) {
			return Kind.SAFETY_STOP;
		}
		if(OPERATOR.contains(verb)) {
			return Kind.OPERATOR_WRITE;
		}
		if(ENGINEERING.contains(verb)) {
			return Kind.ENGINEERING_WRITE;
		}
		return Kind.UNSUPPORTED;
	}

	public static void check(String line, EngineerSession session, long nowEpochMs) throws IOException {

		Kind kind = classify(line);
		String verb = verb(line);
		if(kind == Kind.UNSUPPORTED) {
			audit(session, "denied-unsupported:" + verb);
			throw new IOException("Unsupported workstation command was not sent: " + verb);
		}
		if(kind == Kind.ENGINEERING_WRITE) {
			if(session == null || !session.allowsEngineeringWrite(nowEpochMs)) {
				audit(session, "denied-write:" + verb);
				throw new IOException("Engineering write denied; workstation engineer mode is locked");
			}
			session.touch(nowEpochMs);
		}
	}

	/**
	 * Safety and operator lines stay allowed while locked. Expiry does not
	 * imply stopping an acquisition; this method only answers the write.
	 */
	public static boolean allowedWhenLocked(String line) {

		Kind kind = classify(line);
		return kind == Kind.READ_OR_HANDSHAKE || kind == Kind.SAFETY_STOP || kind == Kind.OPERATOR_WRITE;
	}

	static String verb(String line) {

		if(line == null) {
			return "";
		}
		String trimmed = line.trim();
		int tab = trimmed.indexOf('\t');
		String token = tab >= 0 ? trimmed.substring(tab + 1) : trimmed;
		int next = token.indexOf('\t');
		if(next >= 0) {
			token = token.substring(0, next);
		}
		int space = token.indexOf(' ');
		if(space >= 0) {
			token = token.substring(0, space);
		}
		StringBuilder safe = new StringBuilder();
		for(int i = 0; i < token.length() && safe.length() < 40; i++) {
			char c = token.charAt(i);
			if((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_') {
				if(c >= 'a' && c <= 'z') {
					safe.append((char)(c - 32));
				} else {
					safe.append(c);
				}
			}
		}
		return safe.toString();
	}

	private static void audit(EngineerSession session, String event) {

		if(session == null) {
			return;
		}
		/*
		 * Reuse the session audit list. noteCancelled is the public hook for
		 * a non-secret event; denied writes go through lock-style audit by
		 * calling note only when we already recorded via a package path.
		 */
		session.recordExternalAudit(event);
	}
}
