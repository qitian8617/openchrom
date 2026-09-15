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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;
import org.eclipse.chemclipse.csd.model.core.IScanCSD;
import org.eclipse.chemclipse.csd.model.core.selection.IChromatogramSelectionCSD;
import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.chemclipse.model.selection.IChromatogramSelection;
import org.eclipse.chemclipse.support.ui.activator.ContextAddon;
import org.eclipse.chemclipse.ux.extension.ui.editors.IChromatogramEditor;
import org.eclipse.chemclipse.ux.extension.xxd.ui.editors.ChromatogramEditorCSD;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public final class ChromatogramEditorNotifier {

	/**
	 * A constant baseline auto-scaled 1:1 fills the plot (red block at 100%).
	 * Keep Y max well above the DC floor so an unlit no-gas trace is a thin
	 * line near zero, not a saturated band.
	 */
	private static final float QUIET_BASELINE_MAX = 20f;
	private static final float QUIET_Y_MAX = 10f;

	private ChromatogramEditorNotifier() {
	}

	public static void publishLiveUpdate(IChromatogramCSD chromatogram, IChromatogramSelectionCSD selection) {

		if(chromatogram == null) {
			return;
		}
		synchronized(chromatogram) {
			if(chromatogram.getNumberOfScans() < 2) {
				return;
			}
		}
		IChromatogramSelectionCSD activeSelection = selection;
		if(activeSelection == null) {
			activeSelection = findEditorSelection(chromatogram);
		}
		if(activeSelection == null) {
			return;
		}
		refreshSelectionRange(activeSelection, chromatogram);
		activeSelection.fireUpdateChange(true);
	}

	public static void publishFinalUpdate(IChromatogramCSD chromatogram) {

		if(chromatogram == null) {
			return;
		}
		IChromatogramSelectionCSD selection = findEditorSelection(chromatogram);
		if(selection != null) {
			refreshSelectionRange(selection, chromatogram);
			selection.fireUpdateChange(true);
		}
	}

	public static void applyDisplayRange(MPart part) {

		IChromatogramSelectionCSD selection = extractCsdSelection(part);
		if(selection == null) {
			return;
		}
		IChromatogram chromatogram = selection.getChromatogram();
		if(chromatogram == null) {
			return;
		}
		refreshSelectionRange(selection, chromatogram);
		selection.fireUpdateChange(true);
	}

	public static IChromatogramSelectionCSD findEditorSelection(IChromatogramCSD chromatogram) {

		if(chromatogram == null) {
			return null;
		}
		for(MPart part : collectCandidateParts()) {
			IChromatogramSelectionCSD selection = extractSelection(part, chromatogram);
			if(selection != null) {
				return selection;
			}
		}
		return null;
	}

	public static MPart findOpenedPart(IChromatogramCSD chromatogram) {

		if(chromatogram == null) {
			return null;
		}
		for(MPart part : collectCandidateParts()) {
			if(part.getObject() == chromatogram || extractSelection(part, chromatogram) != null) {
				return part;
			}
		}
		return null;
	}

	private static Set<MPart> collectCandidateParts() {

		Set<MPart> parts = new LinkedHashSet<>();
		EPartService partService = ContextAddon.getWindowPartService();
		if(partService != null) {
			Collection<MPart> activeParts = partService.getParts();
			if(activeParts != null) {
				parts.addAll(activeParts);
			}
		}
		EModelService modelService = ContextAddon.getModelService();
		MApplication application = ContextAddon.getApplication();
		if(modelService != null && application != null) {
			List<MPart> editorParts = modelService.findElements(application, null, MPart.class);
			if(editorParts != null) {
				for(MPart part : editorParts) {
					if(ChromatogramEditorCSD.ID.equals(part.getElementId())) {
						parts.add(part);
					}
				}
			}
		}
		return parts;
	}

	private static IChromatogramSelectionCSD extractCsdSelection(MPart part) {

		if(part == null) {
			return null;
		}
		Object object = part.getObject();
		if(object instanceof IChromatogramEditor editor && editor.getChromatogramSelection() instanceof IChromatogramSelectionCSD selection) {
			return selection;
		}
		IEclipseContext context = part.getContext();
		if(context != null) {
			IChromatogramEditor editor = context.get(IChromatogramEditor.class);
			if(editor == null) {
				Object controller = context.get(ChromatogramEditorCSD.class);
				if(controller instanceof IChromatogramEditor chromatogramEditor) {
					editor = chromatogramEditor;
				}
			}
			if(editor != null && editor.getChromatogramSelection() instanceof IChromatogramSelectionCSD selection) {
				return selection;
			}
		}
		return null;
	}

	private static IChromatogramSelectionCSD extractSelection(MPart part, IChromatogramCSD chromatogram) {

		return matchSelection(extractCsdSelection(part), chromatogram);
	}

	private static IChromatogramSelectionCSD matchSelection(IChromatogramSelection selection, IChromatogramCSD chromatogram) {

		if(selection instanceof IChromatogramSelectionCSD chromatogramSelectionCSD && selection.getChromatogram() == chromatogram) {
			return chromatogramSelectionCSD;
		}
		return null;
	}

	private static void refreshSelectionRange(IChromatogramSelection selection, IChromatogram chromatogram) {

		int startRetentionTime;
		int stopRetentionTime;
		float minSignal;
		float maxSignal;
		IScanCSD latestScan = null;
		synchronized(chromatogram) {
			int scans = chromatogram.getNumberOfScans();
			if(scans < 1) {
				return;
			}
			startRetentionTime = chromatogram.getStartRetentionTime();
			stopRetentionTime = chromatogram.getStopRetentionTime();
			float dataMin = chromatogram.getMinSignal();
			float dataMax = chromatogram.getMaxSignal();
			if(dataMax <= QUIET_BASELINE_MAX) {
				minSignal = 0f;
				maxSignal = Math.max(QUIET_Y_MAX, dataMax * 20f);
			} else {
				minSignal = Math.min(0f, dataMin);
				maxSignal = Math.max(dataMax * 1.25f, dataMax + 10f);
			}
			if(maxSignal <= minSignal) {
				maxSignal = minSignal + QUIET_Y_MAX;
			}
			if(selection instanceof IChromatogramSelectionCSD && chromatogram instanceof IChromatogramCSD chromatogramCSD) {
				latestScan = chromatogramCSD.getScan(scans);
			}
		}
		selection.setRanges(startRetentionTime, stopRetentionTime, minSignal, maxSignal, false);
		if(selection instanceof IChromatogramSelectionCSD chromatogramSelectionCSD && latestScan != null) {
			chromatogramSelectionCSD.setSelectedScan(latestScan, false);
		}
	}
}
