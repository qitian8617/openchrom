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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Local workstation notes for a column and for machine limits. These numbers
 * are not certified ratings and are not written to the device.
 * <p>
 * Code facts, kept distinct from any proposal the engineer types:
 * firmware absolute oven ceiling {@value #FIRMWARE_OVEN_ABSOLUTE_CEILING_C} °C;
 * the method max stored in the oven program is also the trip threshold in the
 * current firmware, same {@value #METHOD_MAX_ALSO_USED_AS_TRIP_C} °C figure;
 * auxiliary zones default to {@value #AUX_DEFAULT_C} °C and the workstation
 * field allows up to {@value #AUX_CONFIGURABLE_UPPER_C} °C. This class does
 * not copy a proposal into that trip threshold.
 */
public final class DeviceColumnProposal {

	public static final int FIRMWARE_OVEN_ABSOLUTE_CEILING_C = 450;
	public static final int METHOD_MAX_ALSO_USED_AS_TRIP_C = 450;
	public static final int AUX_DEFAULT_C = 400;
	public static final int AUX_CONFIGURABLE_UPPER_C = 500;

	public static final String APPLY_BLOCK_REASON = "设备协议没有色谱柱限值或跳闸阈值的读回/写入。本页只保存工作站本地建议，不会下发固件，也不会把工作温度上限接到保护阈值。";

	private final String model;
	private final String maker;
	private final String column;
	private final String isothermalLimitC;
	private final String programLimitC;
	private final String dwellLimit;
	private final String machineLimitC;

	public DeviceColumnProposal(String model, String maker, String column, String isothermalLimitC, String programLimitC, String dwellLimit, String machineLimitC) {

		this.model = clean(model);
		this.maker = clean(maker);
		this.column = clean(column);
		this.isothermalLimitC = clean(isothermalLimitC);
		this.programLimitC = clean(programLimitC);
		this.dwellLimit = clean(dwellLimit);
		this.machineLimitC = clean(machineLimitC);
	}

	public static DeviceColumnProposal empty() {

		return new DeviceColumnProposal("", "", "", "", "", "", "");
	}

	public boolean deviceApplySupported() {

		return false;
	}

	public String deviceApplyBlockReason() {

		return APPLY_BLOCK_REASON;
	}

	public String codeFactSummary(boolean chinese) {

		if(!chinese) {
			return "Code facts, not certified ratings: firmware oven ceiling " + FIRMWARE_OVEN_ABSOLUTE_CEILING_C + " C; method max is also the trip threshold (" + METHOD_MAX_ALSO_USED_AS_TRIP_C + " C); aux default " + AUX_DEFAULT_C + " C, field allows up to " + AUX_CONFIGURABLE_UPPER_C + " C. No ambient sensor. Proposal is local only.";
		}
		return "以下是当前代码事实，不是认证额定值：固件柱箱绝对上限 " + FIRMWARE_OVEN_ABSOLUTE_CEILING_C + "℃；方法最高温度目前同时用作跳闸阈值（同为 " + METHOD_MAX_ALSO_USED_AS_TRIP_C + "℃）；辅助区默认 " + AUX_DEFAULT_C + "℃，界面可填到 " + AUX_CONFIGURABLE_UPPER_C + "℃。没有环境温度传感器。本页建议值只留在工作站。";
	}

	public String toDocument() {

		StringBuilder text = new StringBuilder();
		text.append("status=LOCAL_PROPOSAL\n");
		text.append("deviceApply=blocked\n");
		text.append("reason=").append(APPLY_BLOCK_REASON).append('\n');
		text.append("firmwareOvenCeilingC=").append(FIRMWARE_OVEN_ABSOLUTE_CEILING_C).append('\n');
		text.append("methodMaxAlsoUsedAsTripC=").append(METHOD_MAX_ALSO_USED_AS_TRIP_C).append('\n');
		text.append("auxDefaultC=").append(AUX_DEFAULT_C).append('\n');
		text.append("auxConfigurableUpperC=").append(AUX_CONFIGURABLE_UPPER_C).append('\n');
		text.append("model=").append(model).append('\n');
		text.append("maker=").append(maker).append('\n');
		text.append("column=").append(column).append('\n');
		text.append("isothermalLimitC=").append(isothermalLimitC).append('\n');
		text.append("programLimitC=").append(programLimitC).append('\n');
		text.append("highTempDwell=").append(dwellLimit).append('\n');
		text.append("validatedMachineLimitC=").append(machineLimitC).append('\n');
		return text.toString();
	}

	public static DeviceColumnProposal fromDocument(String document) {

		Map<String, String> values = new LinkedHashMap<>();
		if(document != null) {
			for(String line : document.split("\\R")) {
				int eq = line.indexOf('=');
				if(eq > 0) {
					values.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
				}
			}
		}
		return new DeviceColumnProposal(values.get("model"), values.get("maker"), values.get("column"), values.get("isothermalLimitC"), values.get("programLimitC"), values.get("highTempDwell"), values.get("validatedMachineLimitC"));
	}

	public void write(Path path) throws IOException {

		Path parent = path.getParent();
		if(parent != null) {
			Files.createDirectories(parent);
		}
		Files.writeString(path, toDocument(), StandardCharsets.UTF_8);
	}

	public static DeviceColumnProposal read(Path path) throws IOException {

		if(path == null || !Files.isRegularFile(path)) {
			return empty();
		}
		return fromDocument(Files.readString(path, StandardCharsets.UTF_8));
	}

	public String model() {

		return model;
	}

	public String maker() {

		return maker;
	}

	public String column() {

		return column;
	}

	public String isothermalLimitC() {

		return isothermalLimitC;
	}

	public String programLimitC() {

		return programLimitC;
	}

	public String dwellLimit() {

		return dwellLimit;
	}

	public String machineLimitC() {

		return machineLimitC;
	}

	private static String clean(String value) {

		if(value == null) {
			return "";
		}
		return value.replace('\n', ' ').replace('\r', ' ').trim();
	}
}
