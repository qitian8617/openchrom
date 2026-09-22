/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui;

import java.util.Collection;
import java.util.List;

import org.eclipse.chemclipse.model.selection.IChromatogramSelection;
import org.eclipse.chemclipse.support.events.IChemClipseEvents;
import org.eclipse.chemclipse.ux.extension.ui.editors.IChromatogramEditor;
import org.eclipse.chemclipse.ux.extension.ui.support.DataUpdateSupport;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

/**
 * Resolves 「当前谱图」 for 白酒分析 / 推荐积分 / 定量. On the plant shell the
 * active part is usually the workflow page, so the selected tab of the
 * lower-left chromatogram stack wins. Community (no host stack) still uses
 * the active editor, then the last XXD selection event.
 */
public final class ChromatogramBridge {

	public enum ChromatogramSource {
		HOST_STACK, ACTIVE_PART, NONE
	}

	private ChromatogramBridge() {
	}

	public static IChromatogramSelection resolve(EPartService partService) {

		Probe probe = probe(partService);
		ChromatogramSource source = currentChromatogramSource(probe.hostStackPresent, probe.hostEditor, probe.activeEditor);
		if(source == ChromatogramSource.HOST_STACK) {
			return selectionOf(probe.hostSelected);
		}
		if(source == ChromatogramSource.ACTIVE_PART) {
			return selectionOf(probe.active);
		}
		if(useBroadcastFallback(probe.hostStackPresent, source)) {
			return lastBroadcastSelection();
		}
		return null;
	}

	/**
	 * File name of the chromatogram the next 推荐积分 / 定量 action will use.
	 * Empty when nothing is open in the lower-left host.
	 */
	public static String activeFileLabel(EPartService partService) {

		Probe probe = probe(partService);
		ChromatogramSource source = currentChromatogramSource(probe.hostStackPresent, probe.hostEditor, probe.activeEditor);
		MPart part = source == ChromatogramSource.HOST_STACK ? probe.hostSelected : source == ChromatogramSource.ACTIVE_PART ? probe.active : null;
		String cleaned = cleanEditorLabel(part == null ? null : part.getLabel());
		if(!cleaned.isEmpty()) {
			return cleaned;
		}
		IChromatogramSelection selection = selectionOf(part);
		if(selection == null || selection.getChromatogram() == null) {
			return "";
		}
		String name = selection.getChromatogram().getName();
		if(name == null || name.isBlank()) {
			name = selection.getChromatogram().getSampleName();
		}
		return name == null ? "" : name.trim();
	}

	/**
	 * When the lower-left host stack exists, only its selected chromatogram
	 * tab counts. An active 白酒分析 part must not win, and a stale XXD
	 * event must not substitute for "no tab selected".
	 */
	static ChromatogramSource currentChromatogramSource(boolean hostStackPresent, boolean hostSelectedIsEditor, boolean activeIsEditor) {

		if(hostStackPresent) {
			return hostSelectedIsEditor ? ChromatogramSource.HOST_STACK : ChromatogramSource.NONE;
		}
		return activeIsEditor ? ChromatogramSource.ACTIVE_PART : ChromatogramSource.NONE;
	}

	static boolean useBroadcastFallback(boolean hostStackPresent, ChromatogramSource source) {

		return !hostStackPresent && source == ChromatogramSource.NONE;
	}

	static String cleanEditorLabel(String label) {

		if(label == null) {
			return "";
		}
		String text = label.trim();
		if(text.startsWith("*")) {
			text = text.substring(1).trim();
		}
		int marker = text.indexOf(" [CSD]");
		if(marker < 0) {
			marker = text.indexOf("[CSD]");
		}
		if(marker > 0) {
			text = text.substring(0, marker).trim();
		}
		return text;
	}

	private static Probe probe(EPartService partService) {

		Probe probe = new Probe();
		if(partService != null) {
			try {
				probe.active = partService.getActivePart();
			} catch(RuntimeException | LinkageError e) {
				probe.active = null;
			}
			probe.activeEditor = isChromatogramEditor(probe.active);
		}
		MPartStack stack = findHostStack(partService);
		probe.hostStackPresent = stack != null;
		if(stack == null) {
			return probe;
		}
		MUIElement selected = null;
		try {
			selected = stack.getSelectedElement();
		} catch(RuntimeException | LinkageError e) {
			selected = null;
		}
		if(selected instanceof MPart part && isHostEditor(part)) {
			probe.hostSelected = part;
			probe.hostEditor = true;
		}
		return probe;
	}

	private static MPartStack findHostStack(EPartService partService) {

		IEclipseContext context = contextOf(partService);
		if(context == null) {
			return null;
		}
		MApplication application;
		EModelService modelService;
		try {
			application = context.get(MApplication.class);
			modelService = context.get(EModelService.class);
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		if(application == null || modelService == null) {
			return null;
		}
		try {
			MUIElement found = modelService.find(BaijiuPerspectiveIds.CHROMATOGRAM_STACK_ID, application);
			if(found instanceof MPartStack stack) {
				return stack;
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	private static IEclipseContext contextOf(EPartService partService) {

		if(partService == null) {
			return null;
		}
		try {
			MPart active = partService.getActivePart();
			if(active != null && active.getContext() != null) {
				return active.getContext();
			}
		} catch(RuntimeException | LinkageError e) {
			// try other parts
		}
		try {
			Collection<MPart> parts = partService.getParts();
			if(parts == null) {
				return null;
			}
			for(MPart part : parts) {
				if(part != null && part.getContext() != null) {
					return part.getContext();
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	private static boolean isHostEditor(MPart part) {

		return isChromatogramEditor(part) || isCsdPart(part);
	}

	private static boolean isChromatogramEditor(MPart part) {

		return selectionOf(part) != null;
	}

	private static boolean isCsdPart(MPart part) {

		if(part == null) {
			return false;
		}
		String id = part.getElementId();
		if(id != null && (BaijiuPerspectiveIds.CSD_EDITOR_PART_ID.equals(id) || id.startsWith(BaijiuPerspectiveIds.CSD_EDITOR_PART_ID + "."))) {
			return true;
		}
		String label = part.getLabel();
		return label != null && label.contains("[CSD]");
	}

	private static IChromatogramSelection selectionOf(MPart part) {

		IChromatogramEditor editor = editorOf(part);
		if(editor == null) {
			return null;
		}
		try {
			IChromatogramSelection selection = editor.getChromatogramSelection();
			if(selection != null && selection.getChromatogram() != null) {
				return selection;
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	private static IChromatogramEditor editorOf(MPart part) {

		if(part == null) {
			return null;
		}
		try {
			if(part.getObject() instanceof IChromatogramEditor editor) {
				return editor;
			}
			IEclipseContext context = part.getContext();
			if(context != null) {
				IChromatogramEditor editor = context.get(IChromatogramEditor.class);
				if(editor != null) {
					return editor;
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	private static IChromatogramSelection lastBroadcastSelection() {

		org.eclipse.chemclipse.ux.extension.xxd.ui.Activator xxd = org.eclipse.chemclipse.ux.extension.xxd.ui.Activator.getDefault();
		if(xxd == null) {
			return null;
		}
		DataUpdateSupport support = xxd.getDataUpdateSupport();
		if(support == null) {
			return null;
		}
		List<Object> objects = support.getUpdates(IChemClipseEvents.TOPIC_CHROMATOGRAM_XXD_UPDATE_SELECTION);
		if(objects != null && !objects.isEmpty() && objects.get(0) instanceof IChromatogramSelection selection && selection.getChromatogram() != null) {
			return selection;
		}
		return null;
	}

	private static final class Probe {

		private MPart active;
		private MPart hostSelected;
		private boolean hostStackPresent;
		private boolean hostEditor;
		private boolean activeEditor;
	}
}
