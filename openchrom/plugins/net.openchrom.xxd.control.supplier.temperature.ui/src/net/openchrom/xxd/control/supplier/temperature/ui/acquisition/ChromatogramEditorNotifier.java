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

import java.io.File;
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
import org.eclipse.chemclipse.ux.extension.xxd.ui.swt.editors.ExtendedChromatogramUI;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swtchart.extensions.core.IExtendedChart;

public final class ChromatogramEditorNotifier {

	/**
	 * ChemClipse's default chromatogram series is red ({@code 255,0,0}) and
	 * area-filled down to 0. A flat baseline then paints a red block. The
	 * axis only brackets the stored signals; it does not scale them.
	 */
	static final String CHROMATOGRAM_SERIES_ID = "Chromatogram";
	private static final String DISPLAY_CHROMATOGRAM_KEY = "net.openchrom.gcws.displayChromatogram";

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
		if(!RealtimeAcquisitionManager.getInstance().isReceiving()) {
			publishFinalUpdate(chromatogram);
			return;
		}
		IChromatogramSelectionCSD activeSelection = selection;
		if(activeSelection == null) {
			activeSelection = findEditorSelection(chromatogram);
		}
		if(activeSelection == null) {
			return;
		}
		updateEditor(findOpenedPart(chromatogram), activeSelection, false);
	}

	public static void publishFinalUpdate(IChromatogramCSD chromatogram) {

		if(chromatogram == null) {
			return;
		}
		org.eclipse.swt.widgets.Display display = org.eclipse.swt.widgets.Display.getDefault();
		if(display != null && !display.isDisposed() && display.getThread() != Thread.currentThread()) {
			display.asyncExec(() -> publishFinalUpdate(chromatogram));
			return;
		}
		IChromatogramSelectionCSD selection = findEditorSelection(chromatogram);
		if(selection != null) {
			updateEditor(findOpenedPart(chromatogram), selection, true);
		}
	}

	public static void applyDisplayRange(MPart part) {

		if(part == null) {
			return;
		}
		org.eclipse.swt.widgets.Display display = org.eclipse.swt.widgets.Display.getDefault();
		if(!display.isDisposed() && display.getThread() != Thread.currentThread()) {
			display.asyncExec(() -> applyDisplayRange(part));
			return;
		}
		bindEditorViewport(part);
	}

	/**
	 * Selection owned by the editor that is currently showing this chromatogram.
	 * A selection captured when the editor first opened is not reused after the
	 * part is replaced.
	 */
	public static IChromatogramSelectionCSD selectionForOpenEditor(IChromatogramCSD chromatogram) {

		MPart part = findOpenedPart(chromatogram);
		IChromatogramSelectionCSD fromPart = matchSelection(extractCsdSelection(part), chromatogram);
		if(fromPart != null) {
			return fromPart;
		}
		return findEditorSelection(chromatogram);
	}

	static IChromatogramSelection visibleSelection(IChromatogramSelection editorSelection, IChromatogramSelection cached, IChromatogram chromatogram) {

		return LiveTraceSelection.visible(editorSelection, cached, chromatogram);
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
		File file = chromatogram.getFile();
		String liveLabel = liveEditorLabel(chromatogram);
		for(MPart part : collectCandidateParts()) {
			if(part.getObject() == chromatogram || extractSelection(part, chromatogram) != null) {
				return part;
			}
			IChromatogramCSD extracted = extractChromatogram(part);
			if(extracted == chromatogram) {
				return part;
			}
			if(file != null && extracted != null && fileEquals(file, extracted.getFile())) {
				return part;
			}
			if(liveLabel != null && liveLabel.equals(part.getLabel())) {
				return part;
			}
		}
		return null;
	}

	private static String liveEditorLabel(IChromatogramCSD chromatogram) {

		String name = chromatogram.getDataName();
		if(name == null || name.isBlank()) {
			name = chromatogram.getName();
		}
		if(name == null || name.isBlank()) {
			return null;
		}
		return name + " [CSD]";
	}

	private static boolean fileEquals(File a, File b) {

		if(a == null || b == null) {
			return false;
		}
		return a.getAbsoluteFile().equals(b.getAbsoluteFile());
	}

	private static IChromatogramCSD extractChromatogram(MPart part) {

		IChromatogramSelectionCSD selection = extractCsdSelection(part);
		if(selection != null && selection.getChromatogram() instanceof IChromatogramCSD chromatogram) {
			return chromatogram;
		}
		Object object = part == null ? null : part.getObject();
		if(object instanceof IChromatogramCSD chromatogram) {
			return chromatogram;
		}
		IEclipseContext context = part == null ? null : part.getContext();
		if(context != null) {
			try {
				IChromatogramCSD fromContext = context.get(IChromatogramCSD.class);
				if(fromContext != null) {
					return fromContext;
				}
			} catch(RuntimeException | LinkageError e) {
				return null;
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
		try {
			return extractCsdSelectionFromWidget(part.getWidget());
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
	}

	private static IChromatogramSelectionCSD extractCsdSelectionFromWidget(Object widget) {

		if(widget instanceof ExtendedChromatogramUI editor && !editor.isDisposed() && editor.getChromatogramSelection() instanceof IChromatogramSelectionCSD selection) {
			return selection;
		}
		if(widget instanceof Composite composite && !composite.isDisposed()) {
			for(Control child : composite.getChildren()) {
				IChromatogramSelectionCSD selection = extractCsdSelectionFromWidget(child);
				if(selection != null) {
					return selection;
				}
			}
		}
		return null;
	}

	static void bindEditorViewport(MPart part) {

		ExtendedChromatogramUI editor = part == null ? null : findExtendedEditor(part.getWidget());
		if(editor == null || editor.isDisposed()) {
			return;
		}
		Runnable bind = () -> {
			if(editor.isDisposed()) {
				return;
			}
			IChromatogramSelection selection = extractCsdSelection(part);
			if(selection == null || selection.getChromatogram() == null) {
				return;
			}
			IChromatogram chromatogram = selection.getChromatogram();
			Viewport viewport = readViewport(editor);
			if(viewport == null) {
				refreshSelectionRange(selection, chromatogram);
				editor.update();
				applyChartRange(editor, selection);
				viewport = readViewport(editor);
			}
			DisplayState previous = displayState(editor);
			boolean same = previous != null && previous.chromatogram() == chromatogram;
			DataSignature rendered = same ? previous.dataSignature() : readDataSignature(chromatogram);
			boolean followLive = same && previous.followLive();
			editor.setData(DISPLAY_CHROMATOGRAM_KEY, new DisplayState(chromatogram, rendered, viewport, followLive));
		};
		runOnDisplay(editor, bind);
	}

	static void updateEditor(MPart part, IChromatogramSelection selection, boolean preserveViewport) {

		if(part == null) {
			return;
		}
		ExtendedChromatogramUI editor = findExtendedEditor(part.getWidget());
		if(editor == null || editor.isDisposed()) {
			return;
		}
		Runnable update = () -> {
			if(editor.isDisposed()) {
				return;
			}
			IChromatogram hinted = selection != null ? selection.getChromatogram() : null;
			if(hinted == null && editor.getChromatogramSelection() != null) {
				hinted = editor.getChromatogramSelection().getChromatogram();
			}
			IChromatogramSelection current = visibleSelection(editor.getChromatogramSelection(), selection, hinted);
			if(current == null || current.getChromatogram() == null) {
				return;
			}
			IChromatogram chromatogram = current.getChromatogram();
			DisplayState previous = displayState(editor);
			boolean same = previous != null && previous.chromatogram() == chromatogram;
			Viewport actual = readViewport(editor);
			DataSignature latest = readDataSignature(chromatogram);
			boolean followLive = !preserveViewport;
			if(same && actual != null && latest.equals(previous.dataSignature()) && seriesReachesStop(editor, chromatogram)) {
				editor.setData(DISPLAY_CHROMATOGRAM_KEY, new DisplayState(chromatogram, previous.dataSignature(), actual, followLive));
				return;
			}
			Viewport latestViewport = followLive && same ? latestViewport(actual, chromatogram) : null;
			if(latestViewport != null) {
				current.setRanges((int)Math.round(latestViewport.xMin()), (int)Math.round(latestViewport.xMax()), (float)latestViewport.yMin(), (float)latestViewport.yMax(), false);
			} else if(followLive || !same || actual == null) {
				refreshSelectionRange(current, chromatogram);
			} else {
				current.setRanges((int)Math.round(actual.xMin()), (int)Math.round(actual.xMax()), (float)actual.yMin(), (float)actual.yMax(), false);
			}
			editor.update();
			applyTraceStyle(editor);
			applyChartRange(editor, current);
			if(!seriesReachesStop(editor, chromatogram)) {
				return;
			}
			Viewport renderedViewport = readViewport(editor);
			if(renderedViewport == null) {
				renderedViewport = new Viewport(current.getStartRetentionTime(), current.getStopRetentionTime(), current.getStartAbundance(), current.getStopAbundance());
			}
			editor.setData(DISPLAY_CHROMATOGRAM_KEY, new DisplayState(chromatogram, latest, renderedViewport, followLive));
		};
		runOnDisplay(editor, update);
	}

	private static void runOnDisplay(ExtendedChromatogramUI editor, Runnable runnable) {

		org.eclipse.swt.widgets.Display display = editor.getDisplay();
		if(display.getThread() == Thread.currentThread()) {
			runnable.run();
		} else {
			display.asyncExec(runnable);
		}
	}

	private static DisplayState displayState(ExtendedChromatogramUI editor) {

		return editor.getData(DISPLAY_CHROMATOGRAM_KEY) instanceof DisplayState state ? state : null;
	}

	private static DataSignature readDataSignature(IChromatogram chromatogram) {

		synchronized(chromatogram) {
			return new DataSignature(chromatogram.getNumberOfScans(), chromatogram.getPeaks().size(), System.identityHashCode(chromatogram.getBaselineModel()));
		}
	}

	private static Viewport readViewport(ExtendedChromatogramUI editor) {

		if(editor.getChromatogramChart() == null) {
			return null;
		}
		org.eclipse.swtchart.IAxis xAxis = editor.getChromatogramChart().getBaseChart().getAxisSet().getXAxis(0);
		org.eclipse.swtchart.IAxis yAxis = editor.getChromatogramChart().getBaseChart().getAxisSet().getYAxis(0);
		if(xAxis == null || yAxis == null) {
			return null;
		}
		org.eclipse.swtchart.Range x = xAxis.getRange();
		org.eclipse.swtchart.Range y = yAxis.getRange();
		if(x == null || y == null || !Double.isFinite(x.lower) || !Double.isFinite(x.upper) || !Double.isFinite(y.lower) || !Double.isFinite(y.upper) || x.upper <= x.lower || y.upper <= y.lower) {
			return null;
		}
		return new Viewport(x.lower, x.upper, y.lower, y.upper);
	}

	private static boolean isFullViewport(Viewport actual, IChromatogram chromatogram) {

		AcquisitionTraceGeometry.SignalAxis axis;
		int start;
		int stop;
		synchronized(chromatogram) {
			start = chromatogram.getStartRetentionTime();
			stop = chromatogram.getStopRetentionTime();
			axis = AcquisitionTraceGeometry.signalAxis(chromatogram.getMinSignal(), chromatogram.getMaxSignal());
		}
		return actual.nearlyEquals(new Viewport(start, stop, axis.min(), axis.max()));
	}

	private static Viewport latestViewport(Viewport actual, IChromatogram chromatogram) {

		if(actual == null || isFullViewport(actual, chromatogram)) {
			return null;
		}
		int start;
		int stop;
		synchronized(chromatogram) {
			start = chromatogram.getStartRetentionTime();
			stop = chromatogram.getStopRetentionTime();
		}
		double fullSpan = stop - start;
		double window = actual.xMax() - actual.xMin();
		if(fullSpan <= 0d || window <= 0d) {
			return null;
		}
		if(window >= fullSpan) {
			return new Viewport(start, stop, actual.yMin(), actual.yMax());
		}
		return new Viewport(Math.max(start, stop - window), stop, actual.yMin(), actual.yMax());
	}

	private record DisplayState(IChromatogram chromatogram, DataSignature dataSignature, Viewport viewport, boolean followLive) {
	}

	private record DataSignature(int scans, int peaks, int baselineIdentity) {
	}

	private record Viewport(double xMin, double xMax, double yMin, double yMax) {

		private boolean nearlyEquals(Viewport other) {

			return other != null && close(xMin, other.xMin()) && close(xMax, other.xMax()) && close(yMin, other.yMin()) && close(yMax, other.yMax());
		}

		private static boolean close(double a, double b) {

			double scale = Math.max(1d, Math.max(Math.abs(a), Math.abs(b)));
			return Math.abs(a - b) <= scale * 1.0e-7d;
		}
	}

	private static void applyChartRange(ExtendedChromatogramUI editor, IChromatogramSelection selection) {

		if(selection == null) {
			return;
		}
		IExtendedChart chart = editor.getChromatogramChart();
		if(chart == null) {
			return;
		}
		org.eclipse.swtchart.IAxis xAxis = editor.getChromatogramChart().getBaseChart().getAxisSet().getXAxis(0);
		org.eclipse.swtchart.IAxis yAxis = editor.getChromatogramChart().getBaseChart().getAxisSet().getYAxis(0);
		boolean changed = false;
		if(xAxis != null) {
			org.eclipse.swtchart.Range desired = new org.eclipse.swtchart.Range(selection.getStartRetentionTime(), selection.getStopRetentionTime());
			org.eclipse.swtchart.Range range = xAxis.getRange();
			if(range == null || range.lower != desired.lower || range.upper != desired.upper) {
				xAxis.setRange(desired);
				changed = true;
			}
		}
		if(yAxis != null) {
			org.eclipse.swtchart.Range desired = new org.eclipse.swtchart.Range(selection.getStartAbundance(), selection.getStopAbundance());
			org.eclipse.swtchart.Range range = yAxis.getRange();
			if(range == null || range.lower != desired.lower || range.upper != desired.upper) {
				yAxis.setRange(desired);
				changed = true;
			}
		}
		if(changed) {
			editor.getChromatogramChart().redraw();
		}
	}

	/**
	 * The chromatogram series is the red trace. Peak and baseline series stay
	 * as they are. Area fill stays off so a flat baseline is a line.
	 */
	static void applyTraceStyle(ExtendedChromatogramUI editor) {

		if(editor == null || editor.isDisposed() || editor.getChromatogramChart() == null) {
			return;
		}
		org.eclipse.swtchart.ISeries<?> series = editor.getChromatogramChart().getBaseChart().getSeriesSet().getSeries(CHROMATOGRAM_SERIES_ID);
		if(series instanceof org.eclipse.swtchart.ILineSeries<?> line) {
			line.enableArea(false);
			line.setLineWidth(1);
		}
	}

	private static boolean seriesReachesStop(ExtendedChromatogramUI editor, IChromatogram chromatogram) {

		if(editor == null || editor.isDisposed() || editor.getChromatogramChart() == null || chromatogram == null) {
			return false;
		}
		org.eclipse.swtchart.ISeries<?> series = editor.getChromatogramChart().getBaseChart().getSeriesSet().getSeries(CHROMATOGRAM_SERIES_ID);
		if(series == null || series.getXSeries() == null || series.getXSeries().length == 0) {
			return false;
		}
		double maxX = series.getXSeries()[0];
		for(double x : series.getXSeries()) {
			if(x > maxX) {
				maxX = x;
			}
		}
		int stop;
		synchronized(chromatogram) {
			stop = chromatogram.getStopRetentionTime();
		}
		if(maxX + 1d >= stop) {
			return true;
		}
		/*
		 * Some charts plot minutes while the model stores milliseconds.
		 * Either scale counts as caught up. A stale series still fails both.
		 */
		return Math.abs(maxX * 60000d - stop) <= 1d || (stop > 60000 && Math.abs(maxX - stop / 60000d) <= 0.002d);
	}

	private static ExtendedChromatogramUI findExtendedEditor(Object widget) {

		if(widget instanceof ExtendedChromatogramUI editor) {
			return editor;
		}
		if(widget instanceof Composite composite && !composite.isDisposed()) {
			for(Control child : composite.getChildren()) {
				ExtendedChromatogramUI editor = findExtendedEditor(child);
				if(editor != null) {
					return editor;
				}
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
			AcquisitionTraceGeometry.SignalAxis axis = AcquisitionTraceGeometry.signalAxis(chromatogram.getMinSignal(), chromatogram.getMaxSignal());
			minSignal = axis.min();
			maxSignal = axis.max();
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
