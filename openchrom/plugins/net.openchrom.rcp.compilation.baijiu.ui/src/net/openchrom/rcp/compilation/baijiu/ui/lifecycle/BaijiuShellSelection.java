/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.baijiu.ui.lifecycle;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

/**
 * Eclipse E4 {@code AbstractPartRenderer.selectElement} throws
 * {@code "must be visible in the UI presentation"} when a stack/sash
 * {@code selectedElement} is hidden. Baijiu chrome hides research UI
 * (MALDI {@code PartSashContainer}, Welcome, NMR, …). Never select those
 * containers; if {@code workbench.xmi} left one selected, reassign to a
 * visible plant child before render.
 */
public final class BaijiuShellSelection {

	private BaijiuShellSelection() {

	}

	/**
	 * True when {@code element} and every ancestor is presentable and not
	 * chrome-hidden research UI.
	 */
	public static boolean canSelect(MUIElement element) {

		MUIElement walk = element;
		while(walk != null) {
			if(!isPresentable(walk) || BaijiuShellChrome.shouldHide(walk.getElementId())) {
				return false;
			}
			try {
				walk = walk.getParent();
			} catch(RuntimeException | LinkageError e) {
				return false;
			}
		}
		return element != null;
	}

	/**
	 * Select {@code element} in its parent chain. Does not unhide chrome-hidden
	 * research perspectives/sashes. Stops at a user-hidden GC console sash so
	 * 反控 off cannot re-select that stack. No-op when {@code element} is
	 * research UI.
	 */
	public static void selectInParent(MUIElement element) {

		if(element == null) {
			return;
		}
		if(BaijiuShellChrome.shouldHide(element.getElementId()) || hasHiddenResearchAncestor(element)) {
			deselectFromParent(element);
			return;
		}
		element.setToBeRendered(true);
		element.setVisible(true);
		MUIElement walk = element;
		while(walk != null) {
			if(BaijiuShellChrome.shouldHide(walk.getElementId())) {
				deselectFromParent(walk);
				break;
			}
			MElementContainer<MUIElement> parent;
			try {
				parent = walk.getParent();
			} catch(RuntimeException | LinkageError e) {
				break;
			}
			if(parent == null) {
				break;
			}
			if(BaijiuShellChrome.shouldHide(parent.getElementId())) {
				deselectFromParent(parent);
				break;
			}
			if(!parent.isVisible() || !parent.isToBeRendered()) {
				if(isUserHiddenGcConsole(parent)) {
					break;
				}
				if(BaijiuShellChrome.shouldHide(parent.getElementId())) {
					break;
				}
				parent.setToBeRendered(true);
				parent.setVisible(true);
			}
			setSelected(parent, walk);
			walk = parent;
		}
	}

	/**
	 * Before hiding research chrome, move each container's selection off the
	 * elements about to disappear — while those replacements are still
	 * visible, so the renderer does not see a hidden {@code selectedElement}.
	 */
	public static void reassignAwayFrom(Collection<? extends MUIElement> hiding) {

		if(hiding == null || hiding.isEmpty()) {
			return;
		}
		Set<MUIElement> hideSet = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
		for(MUIElement element : hiding) {
			if(element != null) {
				hideSet.add(element);
			}
		}
		for(MUIElement element : hideSet) {
			MElementContainer<MUIElement> parent;
			try {
				parent = element.getParent();
			} catch(RuntimeException | LinkageError e) {
				continue;
			}
			if(parent == null) {
				continue;
			}
			MUIElement selected;
			try {
				selected = parent.getSelectedElement();
			} catch(RuntimeException | LinkageError e) {
				continue;
			}
			if(selected != element && (selected == null || !hideSet.contains(selected))) {
				continue;
			}
			setSelected(parent, firstSelectableChild(parent, hideSet));
		}
	}

	public static void clearHiddenSelections(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		List<MUIElement> elements;
		try {
			elements = modelService.findElements(application, null, MUIElement.class, null);
		} catch(RuntimeException | LinkageError e) {
			return;
		}
		clearHiddenSelections(elements);
	}

	public static void clearHiddenSelections(List<MUIElement> elements) {

		if(elements == null) {
			return;
		}
		for(MUIElement element : elements) {
			if(element instanceof MElementContainer<?> container) {
				reassignIfNeeded(container);
			}
		}
	}

	public static void deselectFromParent(MUIElement element) {

		if(element == null) {
			return;
		}
		MElementContainer<MUIElement> parent;
		try {
			parent = element.getParent();
		} catch(RuntimeException | LinkageError e) {
			return;
		}
		if(parent == null) {
			return;
		}
		MUIElement selected;
		try {
			selected = parent.getSelectedElement();
		} catch(RuntimeException | LinkageError e) {
			return;
		}
		if(selected != element) {
			return;
		}
		setSelected(parent, firstSelectableChild(parent, Set.of(element)));
		if(BaijiuShellChrome.shouldHide(parent.getElementId()) || !isPresentable(parent)) {
			deselectFromParent(parent);
		}
	}

	static boolean isPresentable(MUIElement element) {

		return element != null && element.isVisible() && element.isToBeRendered();
	}

	@SuppressWarnings("unchecked")
	private static void reassignIfNeeded(MElementContainer<?> container) {

		if(container == null) {
			return;
		}
		Object selected;
		try {
			selected = container.getSelectedElement();
		} catch(RuntimeException | LinkageError e) {
			return;
		}
		if(!(selected instanceof MUIElement selectedElement)) {
			return;
		}
		if(canSelect(selectedElement)) {
			return;
		}
		setSelected((MElementContainer<MUIElement>)container, firstSelectableChild(container, Set.of(selectedElement)));
	}

	private static MUIElement firstSelectableChild(MElementContainer<?> container, Set<MUIElement> exclude) {

		if(container == null) {
			return null;
		}
		List<?> children;
		try {
			children = container.getChildren();
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		if(children == null) {
			return null;
		}
		MUIElement preferred = null;
		for(Object child : children) {
			if(!(child instanceof MUIElement element)) {
				continue;
			}
			if(exclude != null && exclude.contains(element)) {
				continue;
			}
			if(!canSelect(element)) {
				continue;
			}
			if(isPreferredPlantSelection(element.getElementId())) {
				return element;
			}
			if(preferred == null) {
				preferred = element;
			}
		}
		return preferred;
	}

	private static boolean isPreferredPlantSelection(String elementId) {

		if(elementId == null || elementId.isBlank()) {
			return false;
		}
		return BaijiuShellChrome.PERSPECTIVE_ID.equals(elementId) //
				|| BaijiuShellChrome.WORKFLOW_STACK_ID.equals(elementId) //
				|| BaijiuShellChrome.WORKBENCH_HOME_PART_ID.equals(elementId) //
				|| BaijiuShellChrome.SEQUENCE_HOME_PART_ID.equals(elementId) //
				|| BaijiuShellChrome.GC_HOME_STACK_ID.equals(elementId) //
				|| BaijiuShellChrome.PLANT_SASH_ID.equals(elementId) //
				|| BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID.equals(elementId) //
				|| BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID.equals(elementId) //
				|| BaijiuShellChrome.ANALYSIS_HOME_PART_ID.equals(elementId);
	}

	private static boolean hasHiddenResearchAncestor(MUIElement element) {

		MUIElement walk = element;
		while(walk != null) {
			if(BaijiuShellChrome.shouldHide(walk.getElementId())) {
				return true;
			}
			try {
				walk = walk.getParent();
			} catch(RuntimeException | LinkageError e) {
				return true;
			}
		}
		return false;
	}

	private static boolean isUserHiddenGcConsole(MUIElement element) {

		if(element == null || !BaijiuShellChrome.GC_HOME_STACK_ID.equals(element.getElementId())) {
			return false;
		}
		try {
			if(BaijiuShellChrome.isGcConsoleHidden(element.getTags())) {
				return true;
			}
		} catch(RuntimeException | LinkageError e) {
			// fall through to visibility
		}
		return !element.isVisible();
	}

	private static void setSelected(MElementContainer<MUIElement> container, MUIElement child) {

		if(container == null) {
			return;
		}
		if(child != null && !isPresentable(child)) {
			child = null;
		}
		if(child != null && BaijiuShellChrome.shouldHide(child.getElementId())) {
			child = null;
		}
		try {
			if(container.getSelectedElement() == child) {
				return;
			}
			container.setSelectedElement(child);
		} catch(RuntimeException | LinkageError e) {
			try {
				container.setSelectedElement(null);
			} catch(RuntimeException | LinkageError e2) {
				// renderer already rejected a hidden selection
			}
		}
	}
}
