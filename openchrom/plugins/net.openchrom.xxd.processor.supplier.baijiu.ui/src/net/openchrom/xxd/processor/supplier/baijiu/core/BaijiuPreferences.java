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

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

public final class BaijiuPreferences {

	private static final String NODE = "net.openchrom.xxd.processor.supplier.baijiu.ui";
	private static final String ISTD_STOCK = "istd.stock.gl";
	private static final String SAMPLE_ML = "volume.sample.ml";
	private static final String ISTD_ML = "volume.istd.ml";
	private static final String WINDOW = "rt.window.min";
	private static final String RF = "rf.";
	private static final String RT = "rt.";
	private static final String MIX = "mix.";
	private static final String WIN = "win.";
	private static final String SAMPLE_NO = "sample.no";
	private static final String LIQUOR_NAME = "sample.liquor";
	private static final String AROMA = "sample.aroma";
	private static final String ABV = "sample.abv";
	private static final String ANALYST = "sample.analyst";
	private static final String RAW = "sample.raw";

	private BaijiuPreferences() {
	}

	public static BaijiuMethodSettings loadMethod() {

		IEclipsePreferences prefs = prefs();
		BaijiuMethodSettings settings = new BaijiuMethodSettings();
		settings.setIstdStockGramsPerLiter(prefs.getDouble(ISTD_STOCK, BaijiuMethodSettings.DEFAULT_ISTD_STOCK_GL));
		settings.setSampleVolumeMl(prefs.getDouble(SAMPLE_ML, BaijiuMethodSettings.DEFAULT_SAMPLE_ML));
		settings.setIstdVolumeMl(prefs.getDouble(ISTD_ML, BaijiuMethodSettings.DEFAULT_ISTD_ML));
		settings.setDefaultWindowMin(prefs.getDouble(WINDOW, BaijiuMethodSettings.DEFAULT_WINDOW_MIN));
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			String id = compound.getId();
			double rf = prefs.getDouble(RF + id, Double.NaN);
			if(!Double.isNaN(rf) && rf > 0.0d) {
				settings.getResponseFactors().put(id, rf);
			}
			double rt = prefs.getDouble(RT + id, Double.NaN);
			if(!Double.isNaN(rt) && rt > 0.0d) {
				settings.getInstrumentRtMin().put(id, rt);
			}
			double mix = prefs.getDouble(MIX + id, Double.NaN);
			if(!Double.isNaN(mix) && mix >= 0.0d) {
				settings.getMixGramsPerLiter().put(id, mix);
			}
			double window = prefs.getDouble(WIN + id, Double.NaN);
			if(!Double.isNaN(window) && window > 0.0d) {
				settings.getWindowMin().put(id, window);
			}
		}
		return settings;
	}

	public static void saveMethod(BaijiuMethodSettings settings) {

		if(settings == null) {
			return;
		}
		IEclipsePreferences prefs = prefs();
		prefs.putDouble(ISTD_STOCK, settings.getIstdStockGramsPerLiter());
		prefs.putDouble(SAMPLE_ML, settings.getSampleVolumeMl());
		prefs.putDouble(ISTD_ML, settings.getIstdVolumeMl());
		prefs.putDouble(WINDOW, settings.getDefaultWindowMin());
		for(BaijiuCompound compound : BaijiuCatalog.compounds()) {
			String id = compound.getId();
			putOrRemove(prefs, RF + id, settings.getResponseFactors().get(id));
			putOrRemove(prefs, RT + id, settings.getInstrumentRtMin().get(id));
			putOrRemove(prefs, MIX + id, settings.getMixGramsPerLiter().get(id));
			putOrRemove(prefs, WIN + id, settings.getWindowMin().get(id));
		}
		flush(prefs);
	}

	public static void loadSampleDefaults(BaijiuSampleInfo sample) {

		IEclipsePreferences prefs = prefs();
		if(sample.getSampleNo().isEmpty()) {
			sample.setSampleNo(prefs.get(SAMPLE_NO, ""));
		}
		if(sample.getLiquorName().isEmpty()) {
			sample.setLiquorName(prefs.get(LIQUOR_NAME, ""));
		}
		sample.setAromaType(BaijiuAromaType.fromId(prefs.get(AROMA, BaijiuAromaType.NONG.getId())));
		if(sample.getAbvPercent() <= 0.0d) {
			sample.setAbvPercent(prefs.getDouble(ABV, 0.0d));
		}
		if(sample.getAnalyst().isEmpty()) {
			sample.setAnalyst(prefs.get(ANALYST, ""));
		}
		sample.setRawMaterial(BaijiuRawMaterial.fromId(prefs.get(RAW, BaijiuRawMaterial.GRAIN.getId())));
	}

	public static void saveSampleDefaults(BaijiuSampleInfo sample) {

		IEclipsePreferences prefs = prefs();
		prefs.put(SAMPLE_NO, sample.getSampleNo());
		prefs.put(LIQUOR_NAME, sample.getLiquorName());
		prefs.put(AROMA, sample.getAromaType().getId());
		prefs.putDouble(ABV, sample.getAbvPercent());
		prefs.put(ANALYST, sample.getAnalyst());
		prefs.put(RAW, sample.getRawMaterial().getId());
		flush(prefs);
	}

	private static void putOrRemove(IEclipsePreferences prefs, String key, Double value) {

		if(value == null || value.isNaN() || value < 0.0d) {
			prefs.remove(key);
		} else {
			prefs.putDouble(key, value);
		}
	}

	private static IEclipsePreferences prefs() {

		return InstanceScope.INSTANCE.getNode(NODE);
	}

	private static void flush(IEclipsePreferences prefs) {

		try {
			prefs.flush();
		} catch(BackingStoreException e) {
			// keep in-memory values if the store cannot be written
		}
	}
}
