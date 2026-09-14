/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.core;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.GregorianCalendar;

import org.eclipse.chemclipse.model.core.IChromatogram;

public final class BaijiuSampleInfo {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

	private String sampleNo = "";
	private String liquorName = "";
	private String batchNo = "";
	private BaijiuAromaType aromaType = BaijiuAromaType.NONG;
	private double abvPercent = 0.0d;
	private String analyst = "";
	private String dateText = LocalDate.now().format(DATE_FORMAT);
	private BaijiuRawMaterial rawMaterial = BaijiuRawMaterial.GRAIN;

	public String getSampleNo() {

		return sampleNo;
	}

	public void setSampleNo(String sampleNo) {

		this.sampleNo = nullToEmpty(sampleNo);
	}

	public String getLiquorName() {

		return liquorName;
	}

	public void setLiquorName(String liquorName) {

		this.liquorName = nullToEmpty(liquorName);
	}

	public String getBatchNo() {

		return batchNo;
	}

	public void setBatchNo(String batchNo) {

		this.batchNo = nullToEmpty(batchNo);
	}

	public BaijiuAromaType getAromaType() {

		return aromaType;
	}

	public void setAromaType(BaijiuAromaType aromaType) {

		this.aromaType = aromaType == null ? BaijiuAromaType.NONG : aromaType;
	}

	public double getAbvPercent() {

		return abvPercent;
	}

	public void setAbvPercent(double abvPercent) {

		this.abvPercent = abvPercent;
	}

	public String getAnalyst() {

		return analyst;
	}

	public void setAnalyst(String analyst) {

		this.analyst = nullToEmpty(analyst);
	}

	public String getDateText() {

		return dateText;
	}

	public void setDateText(String dateText) {

		this.dateText = nullToEmpty(dateText);
	}

	public BaijiuRawMaterial getRawMaterial() {

		return rawMaterial;
	}

	public void setRawMaterial(BaijiuRawMaterial rawMaterial) {

		this.rawMaterial = rawMaterial == null ? BaijiuRawMaterial.GRAIN : rawMaterial;
	}

	public java.util.List<String> validate() {

		java.util.List<String> messages = new java.util.ArrayList<>();
		if(sampleNo.isEmpty()) {
			messages.add("\u8bf7\u586b\u5199\u6837\u54c1\u7f16\u53f7\u3002");
		}
		if(abvPercent < 0.0d || abvPercent > 100.0d) {
			messages.add("\u9152\u7cbe\u5ea6\u5e94\u5728 0\u2013100 %vol\u3002");
		} else if(abvPercent <= 0.0d) {
			messages.add("\u8bf7\u586b\u5199\u9152\u7cbe\u5ea6\uff0c\u5426\u5219\u65e0\u6cd5\u6309 GB 2757 \u6298\u7b97\u7532\u9187\u3002");
		}
		return messages;
	}

	public boolean hasBlockingErrors() {

		return abvPercent < 0.0d || abvPercent > 100.0d;
	}

	public void applyMethodDefaults(BaijiuMethodSettings settings) {

		if(settings == null) {
			return;
		}
		if(aromaType == null) {
			aromaType = settings.getAromaTemplate();
		}
	}

	public void writeTo(IChromatogram chromatogram) {

		if(chromatogram == null) {
			return;
		}
		String sampleName = (sampleNo + " " + liquorName).trim();
		if(!sampleName.isEmpty()) {
			chromatogram.setSampleName(sampleName);
		}
		if(!analyst.isEmpty()) {
			chromatogram.setOperator(analyst);
		}
		chromatogram.setDate(toDate());
		chromatogram.setColumnDetails(BaijiuCatalog.COLUMN_DETAILS);
		chromatogram.putHeaderData(BaijiuHeaderKeys.SAMPLE_NO, sampleNo);
		chromatogram.putHeaderData(BaijiuHeaderKeys.LIQUOR_NAME, liquorName);
		chromatogram.putHeaderData(BaijiuHeaderKeys.BATCH, batchNo);
		chromatogram.putHeaderData(BaijiuHeaderKeys.AROMA, aromaType.getLabel());
		chromatogram.putHeaderData(BaijiuHeaderKeys.ABV, Double.toString(abvPercent));
		chromatogram.putHeaderData(BaijiuHeaderKeys.ANALYST, analyst);
		chromatogram.putHeaderData(BaijiuHeaderKeys.DATE, dateText);
		chromatogram.putHeaderData(BaijiuHeaderKeys.RAW_MATERIAL, rawMaterial.getLabel());
		chromatogram.putHeaderData(BaijiuHeaderKeys.METHOD_NOTE, "\u767d\u9152\u6c14\u76f8\u8272\u8c31\u5185\u6807\u6cd5\uff1b\u7532\u9187\u6309 GB 2757 \u6298\u7b97\u9152\u7cbe\u5ea6\u540e\u5224\u5b9a\u3002\u4e0d\u58f0\u660e\u6267\u884c GB 5009.266\u3002");
	}

	public static BaijiuSampleInfo from(IChromatogram chromatogram) {

		BaijiuSampleInfo info = new BaijiuSampleInfo();
		if(chromatogram == null) {
			return info;
		}
		info.setSampleNo(value(chromatogram, BaijiuHeaderKeys.SAMPLE_NO, ""));
		info.setLiquorName(value(chromatogram, BaijiuHeaderKeys.LIQUOR_NAME, chromatogram.getSampleName()));
		info.setBatchNo(value(chromatogram, BaijiuHeaderKeys.BATCH, ""));
		info.setAromaType(BaijiuAromaType.fromId(value(chromatogram, BaijiuHeaderKeys.AROMA, "")));
		info.setAbvPercent(parseDouble(value(chromatogram, BaijiuHeaderKeys.ABV, "0"), 0.0d));
		String analyst = value(chromatogram, BaijiuHeaderKeys.ANALYST, chromatogram.getOperator());
		info.setAnalyst(analyst);
		String dateText = value(chromatogram, BaijiuHeaderKeys.DATE, "");
		if(dateText.isEmpty() && chromatogram.getDate() != null) {
			dateText = DATE_FORMAT.format(chromatogram.getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
		}
		if(!dateText.isEmpty()) {
			info.setDateText(dateText);
		}
		info.setRawMaterial(BaijiuRawMaterial.fromId(value(chromatogram, BaijiuHeaderKeys.RAW_MATERIAL, "")));
		return info;
	}

	public Date toDate() {

		try {
			LocalDate localDate = LocalDate.parse(dateText, DATE_FORMAT);
			return GregorianCalendar.from(localDate.atStartOfDay(java.time.ZoneId.systemDefault())).getTime();
		} catch(DateTimeParseException e) {
			return new Date();
		}
	}

	private static String value(IChromatogram chromatogram, String key, String fallback) {

		String value = chromatogram.getHeaderData(key);
		if(value == null || value.isEmpty()) {
			return fallback == null ? "" : fallback;
		}
		return value;
	}

	private static double parseDouble(String text, double fallback) {

		try {
			return Double.parseDouble(text.trim().replace(',', '.'));
		} catch(NumberFormatException | NullPointerException e) {
			return fallback;
		}
	}

	private static String nullToEmpty(String value) {

		return value == null ? "" : value.trim();
	}
}
