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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.MUILabel;
import org.eclipse.e4.ui.model.application.ui.SideValue;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledToolItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarElement;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.e4.ui.workbench.modeling.IWindowCloseHandler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

import net.openchrom.rcp.compilation.baijiu.ui.parts.BaijiuHomePanels;

/**
 * Activates plant-home parts by element id. Prefers the concrete Parts hosted
 * in the plant-home stacks ({@code contributionURI} to branding-bundle
 * {@code BaijiuGcHomePart} / {@code BaijiuSequenceHomePart} /
 * {@code BaijiuAnalysisHomePart} / {@code BaijiuWorkbenchHomePart} /
 * {@code BaijiuChromatogramHomePart}) so {@code @PostConstruct} runs in this
 * bundle. Those hosts OSGi-load the real SWT panels; rendering does not
 * depend on foreign-bundle {@code contributionURI}. Chromatogram / live
 * acquisition embeds the ChemClipse CSD editor into the left 谱图/采集
 * PartStack ({@code partstack.plantChromatogram}) as a real e4 child, not a
 * stolen widget inside {@code part.chromatogramHome}. A concrete empty-state
 * Part sits first in that stack so cold start is not a blank gray void. The GC
 * console is a singleton top-level SWT Shell ({@code BaijiuGcConsoleShell}),
 * not a sash child and not a rendered E4 TrimmedWindow. No Java
 * dependency on baijiu.ui / temperature.ui (branding stays soft).
 */
public final class BaijiuShellParts {

	private static final AtomicBoolean revealingPlantWindowChrome = new AtomicBoolean();

	private BaijiuShellParts() {

	}

	static boolean isRevealingPlantWindowChrome() {

		return revealingPlantWindowChrome.get();
	}

	/**
	 * Show plant-home hosts. Workbench (白酒操作) is activated last so the
	 * right sidebar PartStack opens on that tab. Sequence, analysis, and the
	 * other workflow pages stay visible siblings on the left stack (GUI is
	 * created when the operator selects the tab — not during cold start).
	 * Chromatogram stays on the left sash: empty-state Part selected until a
	 * CSD is opened. The ChemClipse editor Area placeholder stays in the
	 * model but is not rendered into that stack (nested empty frames).
	 * GC console is an independent OS window (default hidden; toolbar 反控
	 * shows it). Returns true when the plant-home surface is shown —
	 * never fall back to the community workbench perspective.
	 */
	public static boolean showPlantHomeParts(MApplication application, EModelService modelService, EPartService partService) {

		if(application == null || modelService == null) {
			return false;
		}
		BaijiuShellModel.ensureIndependentGcWindow(application, modelService);
		suppressE4GcWindow(application, modelService);
		applyGcConsoleVisibility(application, modelService);
		boolean gc = BaijiuGcConsoleShell.isShowing();
		revealStackChildren(application, modelService, BaijiuShellChrome.CHROMATOGRAM_STACK_ID);
		revealStackChildren(application, modelService, BaijiuShellChrome.WORKFLOW_STACK_ID);
		boolean sequence = findPart(modelService, application, BaijiuShellChrome.SEQUENCE_HOME_PART_ID) != null //
				|| findPart(modelService, application, BaijiuShellChrome.SEQUENCE_PART_ID) != null;
		boolean analysis = findPart(modelService, application, BaijiuShellChrome.ANALYSIS_HOME_PART_ID) != null;
		boolean workbench = showPart(application, modelService, partService, BaijiuShellChrome.WORKBENCH_HOME_PART_ID, null);
		boolean chromatogram = revealChromatogramHost(application, modelService, partService);
		forceCreatePlantHomeGuis(application, modelService);
		dedupePlantWorkflowStack(application, modelService);
		revealPlantWindowChrome(application, modelService);
		syncGcToggleToolItem(application, modelService);
		if(!BaijiuShellModel.plantHomeSurfacePresent(application, modelService)) {
			return false;
		}
		return workbench || sequence || analysis || chromatogram || gc;
	}

	/**
	 * Create only the first-paint plant-home widgets: left 谱图/采集 and
	 * right 白酒操作. Other left workflow tabs stay as CTabItems; their
	 * {@code @PostConstruct} runs when selected. Does not createGui the
	 * ChemClipse editor Area into the left stack. Ends by selecting 白酒操作
	 * on the right and 谱图/采集 on the left (empty-state, or the embedded
	 * CSD chart).
	 */
	public static void forceCreatePlantHomeGuis(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		suppressE4GcWindow(application, modelService);
		if(isGcConsoleHidden(application, modelService) || !BaijiuGcConsoleShell.isShowing()) {
			BaijiuGcConsoleShell.hide();
		}
		parkChromatogramEditorArea(application, modelService);
		forceCreateGui(application, modelService, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID);
		forceCreateGui(application, modelService, BaijiuShellChrome.WORKBENCH_HOME_PART_ID);
		restoreDefaultTabSelection(application, modelService);
	}

	public static boolean showChromatogram(MApplication application, EModelService modelService, EPartService partService) {

		if(application == null || modelService == null) {
			return false;
		}
		parkChromatogramEditorArea(application, modelService);
		showElementAndAncestors(modelService.find(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID, application));
		showElementAndAncestors(modelService.find(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, application));
		showElementAndAncestors(modelService.find(BaijiuShellChrome.WORKFLOW_STACK_ID, application));
		boolean hosted = hostOpenCsdEditors(application, modelService, partService);
		dedupePlantWorkflowStack(application, modelService);
		if(!hosted) {
			MPart home = findPart(modelService, application, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID);
			if(home != null) {
				BaijiuShellSelection.selectInParent(home);
				forceCreateGui(application, modelService, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID);
			}
		}
		return hosted || findPart(modelService, application, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID) != null || findPlantChromatogram(application, modelService) != null;
	}

	public static boolean showAnalysis(MApplication application, EModelService modelService, EPartService partService) {

		if(showPart(application, modelService, partService, BaijiuShellChrome.ANALYSIS_HOME_PART_ID, null)) {
			forceCreateGui(application, modelService, BaijiuShellChrome.ANALYSIS_HOME_PART_ID);
			return true;
		}
		return showPart(application, modelService, partService, BaijiuShellChrome.ANALYSIS_PART_ID, null);
	}

	public static boolean showSequence(MApplication application, EModelService modelService, EPartService partService) {

		if(showPart(application, modelService, partService, BaijiuShellChrome.SEQUENCE_HOME_PART_ID, null)) {
			forceCreateGui(application, modelService, BaijiuShellChrome.SEQUENCE_HOME_PART_ID);
			return true;
		}
		return showPart(application, modelService, partService, BaijiuShellChrome.SEQUENCE_PART_ID, null);
	}

	public static boolean showIntegration(MApplication application, EModelService modelService, EPartService partService) {

		return showWorkflowTab(application, modelService, partService, BaijiuShellChrome.INTEGRATION_HOME_PART_ID);
	}

	public static boolean showWizard(MApplication application, EModelService modelService, EPartService partService) {

		return showWorkflowTab(application, modelService, partService, BaijiuShellChrome.WIZARD_HOME_PART_ID);
	}

	public static boolean showBatchResults(MApplication application, EModelService modelService, EPartService partService) {

		return showWorkflowTab(application, modelService, partService, BaijiuShellChrome.BATCH_RESULTS_HOME_PART_ID);
	}

	public static boolean showSimpleBatch(MApplication application, EModelService modelService, EPartService partService) {

		return showWorkflowTab(application, modelService, partService, BaijiuShellChrome.SIMPLE_BATCH_HOME_PART_ID);
	}

	public static boolean showParallel(MApplication application, EModelService modelService, EPartService partService) {

		return showWorkflowTab(application, modelService, partService, BaijiuShellChrome.PARALLEL_HOME_PART_ID);
	}

	public static boolean showReport(MApplication application, EModelService modelService, EPartService partService) {

		return showWorkflowTab(application, modelService, partService, BaijiuShellChrome.REPORT_HOME_PART_ID);
	}

	static boolean showWorkflowTab(MApplication application, EModelService modelService, EPartService partService, String partId) {

		if(showPart(application, modelService, partService, partId, null)) {
			forceCreateGui(application, modelService, partId);
			return true;
		}
		return false;
	}

	public static boolean isGcConsoleHidden(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return false;
		}
		MUIElement window = gcConsoleWindow(application, modelService);
		if(window != null) {
			return BaijiuShellChrome.isGcConsoleHidden(window.getTags());
		}
		MUIElement stack = modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application);
		if(stack != null) {
			return BaijiuShellChrome.isGcConsoleHidden(stack.getTags());
		}
		return false;
	}

	public static boolean toggleGcConsole(MApplication application, EModelService modelService, EPartService partService) {

		boolean show = isGcConsoleHidden(application, modelService);
		setGcConsoleVisible(application, modelService, partService, show);
		return show;
	}

	private static boolean gcVisibilityBusy;

	public static void setGcConsoleVisible(MApplication application, EModelService modelService, EPartService partService, boolean visible) {

		if(application == null || modelService == null || gcVisibilityBusy) {
			return;
		}
		gcVisibilityBusy = true;
		try {
			BaijiuShellModel.ensureIndependentGcWindow(application, modelService);
			suppressE4GcWindow(application, modelService);
			MUIElement window = gcConsoleWindow(application, modelService);
			MUIElement stack = modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application);
			MUIElement target = window != null ? window : stack;
			if(target != null) {
				if(visible) {
					removeTag(target, BaijiuShellChrome.GC_CONSOLE_HIDDEN_TAG);
					if(stack != null && stack != target) {
						removeTag(stack, BaijiuShellChrome.GC_CONSOLE_HIDDEN_TAG);
					}
				} else {
					addTag(target, BaijiuShellChrome.GC_CONSOLE_HIDDEN_TAG);
					if(stack != null && stack != target) {
						addTag(stack, BaijiuShellChrome.GC_CONSOLE_HIDDEN_TAG);
					}
				}
			}
			installGcOsWindowHideHook(application, modelService, partService);
			if(visible) {
				BaijiuGcConsoleShell.show();
			} else {
				BaijiuGcConsoleShell.hide();
			}
			syncGcToggleToolItem(application, modelService);
		} finally {
			gcVisibilityBusy = false;
		}
	}

	public static void applyGcConsoleVisibility(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		BaijiuShellModel.ensureIndependentGcWindow(application, modelService);
		suppressE4GcWindow(application, modelService);
		installGcOsWindowHideHook(application, modelService, partService(application));
		/*
		 * Never open the OS window during chrome apply / plant-home reveal.
		 * Cold start and stale workbench.xmi (tool selected=true) stay closed.
		 * Persist hide when the Shell is not showing so 反控 starts unchecked.
		 */
		if(!BaijiuGcConsoleShell.isShowing()) {
			persistGcConsoleHidden(application, modelService);
			BaijiuGcConsoleShell.hide();
		}
		syncGcToggleToolItem(application, modelService);
	}

	public static void revealPlantToolbar(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		forceShowChrome(modelService.find(BaijiuShellChrome.ECLIPSE_MAIN_TOOLBAR_ID, application));
		forceShowChrome(modelService.find(BaijiuShellChrome.TRIMBAR_TOP_ID, application));
		MUIElement toolbar = modelService.find(BaijiuShellChrome.PLANT_TOOLBAR_ID, application);
		showElementAndAncestors(toolbar);
		forceShowChrome(modelService.find(BaijiuShellChrome.OPEN_CHROMATOGRAM_TOOLITEM_ID, application));
		forceShowChrome(modelService.find(BaijiuShellChrome.TOGGLE_GC_TOOLITEM_ID, application));
		if(toolbar instanceof MToolBar plant) {
			ensurePlantToolbarContents(modelService, application, plant);
		} else if(toolbar instanceof MElementContainer<?> container) {
			List<?> children = container.getChildren();
			if(children != null) {
				for(Object child : children) {
					if(child instanceof MUIElement element) {
						forceShowChrome(element);
					}
				}
			}
		}
		attachPlantToolbarToVisibleTrim(application, modelService, toolbar instanceof MToolBar plant ? plant : null);
	}

	/**
	 * Re-paint 文件 / 白酒 / 视图 / 帮助 and the plant toolbar after chrome
	 * hide, after ChromatogramEditorCSD is selected, on every cold start
	 * (persisted workbench.xmi may have hidden them), and on {@code @PreSave}
	 * so shutdown cannot persist them hidden. Eclipse compatibility otherwise
	 * replaces the TrimmedWindow menu and top trim with the editor's empty
	 * action bars, then {@code setMainMenu(null)} on hardClose (bug 398847)
	 * so the next launch has no bar. Recreate the contributions if
	 * {@code find} misses the detached menu, then {@code createGui} once the
	 * Shell exists. Persisted {@code visible=false} / {@code HiddenExplicitly}
	 * on {@link BaijiuShellChrome#PLANT_WINDOW_CHROME_IDS} is ignored.
	 */
	public static void revealPlantWindowChrome(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		if(!revealingPlantWindowChrome.compareAndSet(false, true)) {
			BaijiuWindowIcons.apply(application, modelService);
			return;
		}
		try {
			preferPlantLookupWindow(application, modelService);
			ensurePlantChromeModel(application, modelService);
			dedupePlantWorkflowStack(application, modelService);
			forceShowPlantWindowChrome(application, modelService);
			reattachWindowMainMenu(application, modelService);
			ensureEditorRequiredMenus(application, modelService);
			showTopTrimBars(application, modelService);
			revealPlantToolbar(application, modelService);
			hideNonPlantTopTrim(application, modelService);
			revealPlantToolbar(application, modelService);
			forceShowPlantWindowChrome(application, modelService);
			ensureEditorRequiredMenus(application, modelService);
			recreatePlantChromeWidgets(application, modelService);
			sanitizePlantMenuContributions(application, modelService);
			BaijiuWindowIcons.apply(application, modelService);
		} finally {
			revealingPlantWindowChrome.set(false);
		}
	}

	private static void forceShowPlantWindowChrome(MApplication application, EModelService modelService) {

		for(String id : BaijiuShellChrome.PLANT_WINDOW_CHROME_IDS) {
			MUIElement found = modelService.find(id, application);
			forceShowChrome(found);
			if(found != null && BaijiuShellChrome.shouldCreateGuiForPlantChrome(found.getElementId(), found.getWidget() != null)) {
				forceCreateElement(application, modelService, found);
			}
		}
	}

	/**
	 * ChemClipse {@code AbstractGroupHandler} uses
	 * {@code application.getChildren().get(0).getMainMenu()}. The GC
	 * console TrimmedWindow must not sit first — it has no View menu.
	 */
	static void preferPlantLookupWindow(MApplication application, EModelService modelService) {

		if(application == null) {
			return;
		}
		List<MWindow> children;
		try {
			children = application.getChildren();
		} catch(RuntimeException | LinkageError e) {
			return;
		}
		if(children == null) {
			return;
		}
		MWindow plant = plantWindow(application, modelService);
		if(plant == null) {
			return;
		}
		try {
			if(!children.contains(plant)) {
				children.add(0, plant);
				return;
			}
			int index = children.indexOf(plant);
			if(index > 0) {
				children.remove(plant);
				children.add(0, plant);
			}
		} catch(RuntimeException | LinkageError e) {
			// immutable application children
		}
	}

	/**
	 * Eclipse bug 398847: compatibility {@code setMainMenu(null)} on close
	 * drops {@code menu.main} from the containment tree. Recreate the plant
	 * window menu / 文件 / 白酒 / 视图 / 帮助 and {@code trimbar.top} +
	 * {@code toolbar.plant} so fragments and GroupHandler have a parent, and
	 * so a second launch without Clean still paints chrome.
	 */
	static void ensurePlantChromeModel(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		preferPlantLookupWindow(application, modelService);
		MWindow plant = plantWindow(application, modelService);
		if(plant == null) {
			return;
		}
		MMenu menu = plant.getMainMenu();
		if(menu == null) {
			menu = findMenu(modelService, application, BaijiuShellChrome.MAIN_MENU_ID);
		}
		if(menu == null) {
			menu = findMenu(modelService, application, BaijiuShellChrome.ECLIPSE_MAIN_MENU_ID);
		}
		if(menu == null) {
			menu = createEditorRequiredMenu(modelService, BaijiuShellChrome.MAIN_MENU_ID);
		}
		if(menu != null) {
			if(menu.getElementId() == null || menu.getElementId().isBlank()) {
				menu.setElementId(BaijiuShellChrome.MAIN_MENU_ID);
			}
			forceShowChrome(menu);
			if(plant.getMainMenu() != menu) {
				try {
					plant.setMainMenu(menu);
				} catch(RuntimeException | LinkageError e) {
					// older E4
				}
			}
			ensurePlantTopMenus(modelService, application, menu);
		}
		if(plant instanceof MTrimmedWindow trimmed) {
			ensurePlantTopTrim(modelService, application, trimmed);
		}
	}

	public static MWindow plantWindow(MApplication application, EModelService modelService) {

		if(application == null) {
			return null;
		}
		if(modelService != null) {
			MUIElement found = modelService.find(BaijiuShellChrome.MAIN_WINDOW_ID, application);
			if(found instanceof MWindow window && !BaijiuShellChrome.GC_WINDOW_ID.equals(window.getElementId())) {
				return window;
			}
		}
		List<MWindow> children;
		try {
			children = application.getChildren();
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		if(children == null) {
			return null;
		}
		for(MWindow window : children) {
			if(window != null && BaijiuShellChrome.MAIN_WINDOW_ID.equals(window.getElementId())) {
				return window;
			}
		}
		for(MWindow window : children) {
			if(window != null && !BaijiuShellChrome.GC_WINDOW_ID.equals(window.getElementId())) {
				return window;
			}
		}
		return null;
	}

	private static void ensurePlantTopMenus(EModelService modelService, MApplication application, MMenu mainMenu) {

		if(mainMenu == null) {
			return;
		}
		ensureTopMenuChild(modelService, application, mainMenu, BaijiuShellChrome.FILE_MENU_ID, "文件");
		ensureTopMenuChild(modelService, application, mainMenu, BaijiuShellChrome.BAIJIU_MENU_ID, "白酒");
		ensureTopMenuChild(modelService, application, mainMenu, BaijiuShellChrome.VIEW_MENU_ID, "视图");
		ensureTopMenuChild(modelService, application, mainMenu, BaijiuShellChrome.HELP_MENU_ID, "帮助");
		MMenu view = liveMenuChild(mainMenu, BaijiuShellChrome.VIEW_MENU_ID);
		if(view == null) {
			view = findMenu(modelService, application, BaijiuShellChrome.VIEW_MENU_ID);
		}
		ensureViewMenuContents(modelService, application, view);
		MMenu file = liveMenuChild(mainMenu, BaijiuShellChrome.FILE_MENU_ID);
		if(file == null) {
			file = findMenu(modelService, application, BaijiuShellChrome.FILE_MENU_ID);
		}
		ensureFileMenuContents(modelService, application, file);
		MMenu help = liveMenuChild(mainMenu, BaijiuShellChrome.HELP_MENU_ID);
		if(help == null) {
			help = findMenu(modelService, application, BaijiuShellChrome.HELP_MENU_ID);
		}
		if(help == null) {
			help = liveMenuChild(mainMenu, BaijiuShellChrome.ECLIPSE_HELP_MENU_ID);
		}
		ensureHelpMenuContents(modelService, application, help);
		dedupePlantMenuChildren(mainMenu);
		dedupePlantMenuChildren(view);
		orderPlantTopMenus(mainMenu);
		applyEditorRequiredMenuVisibility(mainMenu);
	}

	private static void ensureTopMenuChild(EModelService modelService, MApplication application, MMenu mainMenu, String elementId, String label) {

		MMenu child = liveMenuChild(mainMenu, elementId);
		if(child == null) {
			child = findMenu(modelService, application, elementId);
		}
		if(child == null) {
			child = createEditorRequiredMenu(modelService, elementId);
			if(child != null && (child.getLabel() == null || child.getLabel().isBlank())) {
				child.setLabel(label);
			}
		}
		if(child == null) {
			return;
		}
		if(child.getLabel() == null || child.getLabel().isBlank()) {
			child.setLabel(label);
		}
		forceShowChrome(child);
		attachMenuChild(mainMenu, child);
	}

	private static MUIElement mainMenuChild(MMenu mainMenu, String elementId) {

		if(mainMenu == null || elementId == null) {
			return null;
		}
		try {
			List<?> children = mainMenu.getChildren();
			if(children == null) {
				return null;
			}
			for(Object child : children) {
				if(child instanceof MUIElement element && elementId.equals(element.getElementId())) {
					return element;
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	private static MMenu liveMenuChild(MMenu parent, String elementId) {

		return mainMenuChild(parent, elementId) instanceof MMenu menu ? menu : null;
	}

	static List<String> childIdsOf(MMenu menu) {

		List<String> ids = new ArrayList<>();
		if(menu == null) {
			return ids;
		}
		try {
			List<?> children = menu.getChildren();
			if(children == null) {
				return ids;
			}
			for(Object child : children) {
				if(child instanceof MUIElement element) {
					ids.add(element.getElementId());
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return ids;
		}
		return ids;
	}

	static int countMenuChildrenWithId(MMenu menu, String elementId) {

		return BaijiuShellChrome.countMenuChildrenWithId(childIdsOf(menu), elementId);
	}

	@SuppressWarnings("rawtypes")
	static void dedupePlantMenuChildren(MMenu menu) {

		if(menu == null) {
			return;
		}
		try {
			List children = menu.getChildren();
			if(children == null) {
				return;
			}
			Map<String, MUIElement> winners = new HashMap<>();
			for(int i = 0; i < children.size(); i++) {
				Object child = children.get(i);
				if(!(child instanceof MUIElement element)) {
					continue;
				}
				String id = element.getElementId();
				if(!BaijiuShellChrome.isSingletonMenuChildId(id)) {
					continue;
				}
				winners.put(id, preferredPlantMenuChild(winners.get(id), element));
			}
			for(int i = 0; i < children.size();) {
				Object child = children.get(i);
				if(!(child instanceof MUIElement element)) {
					i++;
					continue;
				}
				String id = element.getElementId();
				if(!BaijiuShellChrome.isSingletonMenuChildId(id)) {
					i++;
					continue;
				}
				MUIElement winner = winners.get(id);
				if(winner == element) {
					i++;
					continue;
				}
				adoptPlantMenuDuplicate(winner, element);
				disposeMenuWidget(element);
				children.remove(i);
			}
		} catch(RuntimeException | LinkageError e) {
			// menu children not writable
		}
	}

	static MUIElement preferredPlantMenuChild(MUIElement first, MUIElement second) {

		if(first == null) {
			return second;
		}
		if(second == null) {
			return first;
		}
		return plantMenuChildScore(second) > plantMenuChildScore(first) ? second : first;
	}

	static int plantMenuChildScore(MUIElement element) {

		if(element == null) {
			return -1;
		}
		int score = 0;
		if(commandOf(element) != null) {
			score += 4;
		}
		if(element instanceof MDirectMenuItem direct) {
			try {
				String uri = direct.getContributionURI();
				if(uri != null && !uri.isBlank()) {
					score += 4;
				}
			} catch(RuntimeException | LinkageError e) {
				// uri not readable
			}
		}
		if(element instanceof MMenu menu) {
			if(hasExecutableSelectViewChild(menu)) {
				score += 3;
			}
			try {
				List<?> children = menu.getChildren();
				if(children != null && !children.isEmpty()) {
					score += 1;
				}
			} catch(RuntimeException | LinkageError e) {
				// children not readable
			}
		}
		return score;
	}

	static boolean hasExecutableSelectViewChild(MMenu menu) {

		if(menu == null) {
			return false;
		}
		try {
			List<?> children = menu.getChildren();
			if(children == null) {
				return false;
			}
			for(Object child : children) {
				if(child instanceof MUIElement element && isExecutableSelectViewItem(element)) {
					return true;
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
		return false;
	}

	static boolean isExecutableSelectViewItem(MUIElement element) {

		if(element == null) {
			return false;
		}
		String id = element.getElementId();
		if(!BaijiuShellChrome.SELECT_VIEW_MENU_ID.equals(id) && !BaijiuShellChrome.isViewMenuKeepLabel(labelOf(element))) {
			return false;
		}
		if(commandOf(element) != null) {
			return true;
		}
		if(element instanceof MDirectMenuItem direct) {
			try {
				String uri = direct.getContributionURI();
				return uri != null && !uri.isBlank();
			} catch(RuntimeException | LinkageError e) {
				return false;
			}
		}
		return false;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void adoptPlantMenuDuplicate(MUIElement keep, MUIElement drop) {

		if(keep == null || drop == null || keep == drop) {
			return;
		}
		if(keep instanceof MHandledMenuItem keepHandled && drop instanceof MHandledMenuItem dropHandled) {
			try {
				if(keepHandled.getCommand() == null && dropHandled.getCommand() != null) {
					keepHandled.setCommand(dropHandled.getCommand());
				}
			} catch(RuntimeException | LinkageError e) {
				// command not writable
			}
			return;
		}
		if(keep instanceof MDirectMenuItem keepDirect && drop instanceof MDirectMenuItem dropDirect) {
			try {
				if((keepDirect.getContributionURI() == null || keepDirect.getContributionURI().isBlank()) && dropDirect.getContributionURI() != null && !dropDirect.getContributionURI().isBlank()) {
					keepDirect.setContributionURI(dropDirect.getContributionURI());
				}
			} catch(RuntimeException | LinkageError e) {
				// uri not writable
			}
			return;
		}
		if(!(keep instanceof MMenu keepMenu) || !(drop instanceof MMenu dropMenu)) {
			return;
		}
		try {
			List dropChildren = dropMenu.getChildren();
			List keepChildren = keepMenu.getChildren();
			if(dropChildren == null || keepChildren == null) {
				return;
			}
			List snapshot = new ArrayList(dropChildren);
			for(Object child : snapshot) {
				if(!(child instanceof MMenuElement element)) {
					continue;
				}
				String id = element.getElementId();
				if(id != null && !id.isBlank()) {
					MUIElement existing = mainMenuChild(keepMenu, id);
					if(existing != null) {
						adoptPlantMenuDuplicate(existing, element);
						continue;
					}
				}
				dropChildren.remove(element);
				if(!keepChildren.contains(element)) {
					keepChildren.add(element);
				}
			}
		} catch(RuntimeException | LinkageError e) {
			// menu children not writable
		}
	}

	private static void disposeMenuWidget(MUIElement element) {

		if(element == null) {
			return;
		}
		try {
			Object widget = element.getWidget();
			if(widget instanceof Widget swt && !swt.isDisposed()) {
				swt.dispose();
			}
		} catch(RuntimeException | LinkageError e) {
			// widget already gone
		}
		try {
			element.setWidget(null);
		} catch(RuntimeException | LinkageError e) {
			// widget not writable
		}
	}

	static void ensurePlantTopMenus(MMenu mainMenu) {

		ensurePlantTopMenus(null, null, mainMenu);
	}

	/**
	 * Re-hide the chromatogram top-level label without {@code createGui} on
	 * 视图. Full chrome reveal here was the #64 视图 spam loop: CSD sets
	 * {@code visible=true}, reveal createGui'd the cascade, another 视图.
	 */
	static void hideChromatogramMenuLabel(MApplication application, EModelService modelService) {

		if(application == null || BaijiuShellChrome.researchMenusVisible()) {
			return;
		}
		MMenu mainMenu = windowMainMenu(application);
		if(mainMenu != null) {
			applyEditorRequiredMenuVisibility(mainMenu);
			dedupePlantMenuChildren(mainMenu);
		}
		if(modelService != null) {
			MUIElement found = modelService.find(BaijiuShellChrome.CHROMATOGRAM_MENU_ID, application);
			if(found instanceof MMenu menu) {
				applyEditorRequiredMenuVisibility(menu);
			} else if(found != null) {
				applyEditorRequiredMenuVisibility(found);
			}
		}
		MWindow plant = plantWindow(application, modelService);
		if(plant != null && plant.getWidget() instanceof Shell shell && !shell.isDisposed()) {
			Menu bar = shell.getMenuBar();
			if(bar != null && !bar.isDisposed()) {
				BaijiuShellMenus.sanitizeMainMenuBar(bar);
			}
		}
	}

	private static void ensurePlantTopTrim(EModelService modelService, MApplication application, MTrimmedWindow window) {

		if(window == null) {
			return;
		}
		MTrimBar top = findTrimBar(modelService, application, BaijiuShellChrome.TRIMBAR_TOP_ID);
		if(top == null) {
			top = trimBarOnWindow(window, BaijiuShellChrome.TRIMBAR_TOP_ID);
		}
		if(top == null) {
			top = createTrimBar(modelService, BaijiuShellChrome.TRIMBAR_TOP_ID);
		}
		if(top == null) {
			return;
		}
		try {
			top.setSide(SideValue.TOP);
		} catch(RuntimeException | LinkageError e) {
			// older E4
		}
		forceShowChrome(top);
		attachTrimBar(window, top);
		MToolBar toolbar = findToolBar(modelService, application, BaijiuShellChrome.PLANT_TOOLBAR_ID);
		if(toolbar == null) {
			toolbar = toolBarOnTrim(top, BaijiuShellChrome.PLANT_TOOLBAR_ID);
		}
		if(toolbar == null) {
			toolbar = toolbarHostingPlantItems(modelService, application);
		}
		if(toolbar == null) {
			toolbar = createToolBar(modelService, BaijiuShellChrome.PLANT_TOOLBAR_ID);
		}
		if(toolbar != null) {
			if(toolbar.getElementId() == null || toolbar.getElementId().isBlank()) {
				toolbar.setElementId(BaijiuShellChrome.PLANT_TOOLBAR_ID);
			}
			forceShowChrome(toolbar);
			attachToolBar(top, toolbar);
			ensurePlantToolbarContents(modelService, application, toolbar);
		}
	}

	private static MTrimBar findTrimBar(EModelService modelService, MApplication application, String id) {

		if(modelService == null || application == null || id == null) {
			return null;
		}
		MUIElement found = modelService.find(id, application);
		if(found instanceof MTrimBar bar) {
			return bar;
		}
		return null;
	}

	private static MTrimBar trimBarOnWindow(MTrimmedWindow window, String id) {

		if(window == null) {
			return null;
		}
		try {
			List<MTrimBar> bars = window.getTrimBars();
			if(bars == null) {
				return null;
			}
			for(MTrimBar bar : bars) {
				if(bar != null && id.equals(bar.getElementId())) {
					return bar;
				}
				if(bar != null && containsPlantToolbar(bar)) {
					return bar;
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	private static boolean containsPlantToolbar(MTrimBar bar) {

		try {
			List<?> children = bar.getChildren();
			if(children == null) {
				return false;
			}
			for(Object child : children) {
				if(child instanceof MUIElement element && BaijiuShellChrome.PLANT_TOOLBAR_ID.equals(element.getElementId())) {
					return true;
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
		return false;
	}

	private static MToolBar findToolBar(EModelService modelService, MApplication application, String id) {

		if(modelService == null || application == null || id == null) {
			return null;
		}
		MUIElement found = modelService.find(id, application);
		if(found instanceof MToolBar bar) {
			return bar;
		}
		return null;
	}

	private static MToolBar toolBarOnTrim(MTrimBar trim, String id) {

		if(trim == null) {
			return null;
		}
		try {
			List<?> children = trim.getChildren();
			if(children == null) {
				return null;
			}
			for(Object child : children) {
				if(child instanceof MToolBar bar && id.equals(bar.getElementId())) {
					return bar;
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	private static MTrimBar createTrimBar(EModelService modelService, String elementId) {

		MTrimBar bar = null;
		if(modelService != null) {
			try {
				bar = modelService.createModelElement(MTrimBar.class);
			} catch(RuntimeException | LinkageError e) {
				bar = null;
			}
		}
		if(bar == null) {
			try {
				bar = MBasicFactory.INSTANCE.createTrimBar();
			} catch(RuntimeException | LinkageError e) {
				return null;
			}
		}
		bar.setElementId(elementId);
		bar.setToBeRendered(true);
		bar.setVisible(true);
		return bar;
	}

	private static MToolBar createToolBar(EModelService modelService, String elementId) {

		MToolBar bar = null;
		if(modelService != null) {
			try {
				bar = modelService.createModelElement(MToolBar.class);
			} catch(RuntimeException | LinkageError e) {
				bar = null;
			}
		}
		if(bar == null) {
			try {
				bar = MMenuFactory.INSTANCE.createToolBar();
			} catch(RuntimeException | LinkageError e) {
				return null;
			}
		}
		bar.setElementId(elementId);
		bar.setToBeRendered(true);
		bar.setVisible(true);
		return bar;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void attachTrimBar(MTrimmedWindow window, MTrimBar bar) {

		if(window == null || bar == null) {
			return;
		}
		try {
			List bars = window.getTrimBars();
			if(bars != null && !bars.contains(bar)) {
				bars.add(0, bar);
			}
		} catch(RuntimeException | LinkageError e) {
			// immutable trim
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void attachToolBar(MTrimBar trim, MToolBar toolbar) {

		if(trim == null || toolbar == null) {
			return;
		}
		try {
			MElementContainer<?> parent = toolbar.getParent();
			if(parent != null && parent != trim) {
				List siblings = parent.getChildren();
				if(siblings != null) {
					siblings.remove(toolbar);
				}
			}
			List children = trim.getChildren();
			if(children != null) {
				int current = children.indexOf(toolbar);
				if(current < 0) {
					children.add(0, toolbar);
				} else if(current > 0) {
					children.remove(toolbar);
					children.add(0, toolbar);
				}
			}
		} catch(RuntimeException | LinkageError e) {
			// immutable
		}
	}

	static void ensurePlantToolbarContents(EModelService modelService, MApplication application, MToolBar toolbar) {

		if(toolbar == null) {
			return;
		}
		for(String id : BaijiuShellChrome.PLANT_TOOLBAR_ITEM_IDS) {
			MHandledToolItem item = findPlantToolItem(modelService, application, toolbar, id);
			if(item == null) {
				item = createPlantToolItem(modelService, application, id);
			}
			if(item == null) {
				continue;
			}
			restorePlantToolItem(modelService, application, item);
			attachToolItem(toolbar, item);
		}
		restoreGcConsoleToolItemIcon(modelService, application);
	}

	/**
	 * Java 21: do not compare {@code getParent()} to {@code MToolBar} (#49).
	 * Membership is {@code toolbar.getChildren().contains(item)} (#56).
	 */
	private static MToolBar toolbarHostingPlantItems(EModelService modelService, MApplication application) {

		for(String id : BaijiuShellChrome.PLANT_TOOLBAR_ITEM_IDS) {
			MUIElement found = findElement(modelService, application, id);
			if(found == null) {
				continue;
			}
			MToolBar host = toolbarContaining(modelService, application, found);
			if(host != null) {
				return host;
			}
		}
		return null;
	}

	private static MToolBar toolbarContaining(EModelService modelService, MApplication application, MUIElement item) {

		if(item == null) {
			return null;
		}
		if(modelService != null && application != null) {
			try {
				List<MToolBar> toolbars = modelService.findElements(application, null, MToolBar.class, null);
				if(toolbars != null) {
					for(MToolBar toolbar : toolbars) {
						if(toolbarContains(toolbar, item)) {
							return toolbar;
						}
					}
				}
			} catch(RuntimeException | LinkageError e) {
				// findElements not available
			}
		}
		return toolbarContainingInWindows(application, item);
	}

	private static MToolBar toolbarContainingInWindows(MApplication application, MUIElement item) {

		if(application == null || item == null) {
			return null;
		}
		try {
			List<MWindow> windows = application.getChildren();
			if(windows == null) {
				return null;
			}
			for(MWindow window : windows) {
				if(!(window instanceof MTrimmedWindow trimmed)) {
					continue;
				}
				List<MTrimBar> bars = trimmed.getTrimBars();
				if(bars == null) {
					continue;
				}
				for(MTrimBar bar : bars) {
					if(bar == null) {
						continue;
					}
					List<?> children = bar.getChildren();
					if(children == null) {
						continue;
					}
					for(Object child : children) {
						if(child instanceof MToolBar toolbar && toolbarContains(toolbar, item)) {
							return toolbar;
						}
					}
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	static boolean toolbarContains(MToolBar toolbar, MUIElement item) {

		if(toolbar == null || item == null) {
			return false;
		}
		try {
			List<?> children = toolbar.getChildren();
			return children != null && children.contains(item);
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
	}

	private static MHandledToolItem findPlantToolItem(EModelService modelService, MApplication application, MToolBar toolbar, String id) {

		if(id == null || id.isBlank()) {
			return null;
		}
		if(toolbar != null) {
			try {
				List<?> children = toolbar.getChildren();
				if(children != null) {
					for(Object child : children) {
						if(child instanceof MHandledToolItem item && id.equals(item.getElementId())) {
							return item;
						}
					}
				}
			} catch(RuntimeException | LinkageError e) {
				// toolbar children not readable
			}
		}
		MUIElement found = findElement(modelService, application, id);
		return found instanceof MHandledToolItem item ? item : null;
	}

	private static MHandledToolItem createPlantToolItem(EModelService modelService, MApplication application, String id) {

		MHandledToolItem item = null;
		if(modelService != null) {
			try {
				item = modelService.createModelElement(MHandledToolItem.class);
			} catch(RuntimeException | LinkageError e) {
				item = null;
			}
		}
		if(item == null) {
			try {
				item = MMenuFactory.INSTANCE.createHandledToolItem();
			} catch(RuntimeException | LinkageError e) {
				return null;
			}
		}
		item.setElementId(id);
		item.setToBeRendered(true);
		item.setVisible(true);
		if(BaijiuShellChrome.TOGGLE_GC_TOOLITEM_ID.equals(id)) {
			try {
				item.setType(org.eclipse.e4.ui.model.application.ui.menu.ItemType.CHECK);
			} catch(RuntimeException | LinkageError e) {
				// older E4
			}
		}
		restorePlantToolItem(modelService, application, item);
		return item;
	}

	private static void restorePlantToolItem(EModelService modelService, MApplication application, MHandledToolItem item) {

		if(item == null) {
			return;
		}
		forceShowChrome(item);
		String id = item.getElementId();
		String icon = BaijiuShellChrome.plantToolbarItemIconUri(id);
		if(icon != null && !icon.isBlank()) {
			try {
				item.setIconURI(icon);
			} catch(RuntimeException | LinkageError e) {
				// icon not writable
			}
		}
		String label = BaijiuShellChrome.plantToolbarItemLabel(id);
		if(label != null && !label.isBlank()) {
			try {
				if(item.getLabel() == null || item.getLabel().isBlank()) {
					item.setLabel(label);
				}
				if(item.getTooltip() == null || item.getTooltip().isBlank()) {
					item.setTooltip(label);
				}
			} catch(RuntimeException | LinkageError e) {
				// label not writable
			}
		}
		try {
			if(item.getCommand() != null) {
				return;
			}
		} catch(RuntimeException | LinkageError e) {
			return;
		}
		MCommand command = findCommand(modelService, application, BaijiuShellChrome.plantToolbarItemCommandId(id));
		if(command == null) {
			return;
		}
		try {
			item.setCommand(command);
		} catch(RuntimeException | LinkageError e) {
			// command not writable
		}
	}

	/**
	 * {@code temperature.ui.toolbar.open} (气相色谱控制台 / GC控制台) is
	 * kept by {@code KEEP_ID_PREFIXES} and can leak onto the plant coolbar
	 * with a missing ChemClipse GIF. Force the plant {@code gc_console.png}
	 * without attaching a second GC button to {@code toolbar.plant}.
	 */
	private static void restoreGcConsoleToolItemIcon(EModelService modelService, MApplication application) {

		applyPlantChromeIconUri(findElement(modelService, application, BaijiuShellChrome.TOGGLE_GC_TOOLITEM_ID));
		applyPlantChromeIconUri(findElement(modelService, application, BaijiuShellChrome.TEMPERATURE_OPEN_TOOLITEM_ID));
		applyPlantChromeIconUri(findElement(modelService, application, BaijiuShellChrome.TEMPERATURE_OPEN_MENU_ID));
		applyPlantChromeIconUri(findElement(modelService, application, BaijiuShellChrome.GC_CONTROL_PART_ID));
	}

	private static void applyPlantChromeIconUri(MUIElement element) {

		if(!(element instanceof MUILabel labeled)) {
			return;
		}
		String icon = BaijiuShellChrome.plantChromeIconUri(element.getElementId());
		if(icon == null || icon.isBlank()) {
			return;
		}
		try {
			labeled.setIconURI(icon);
		} catch(RuntimeException | LinkageError e) {
			// iconURI not writable
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void attachToolItem(MToolBar toolbar, MHandledToolItem item) {

		if(toolbar == null || item == null) {
			return;
		}
		try {
			MElementContainer<?> parent = item.getParent();
			if(parent != null && parent != toolbar) {
				List siblings = parent.getChildren();
				if(siblings != null) {
					siblings.remove(item);
				}
			}
			List children = toolbar.getChildren();
			if(children != null && !children.contains(item) && item instanceof MToolBarElement element) {
				children.add(element);
			}
		} catch(RuntimeException | LinkageError e) {
			// immutable toolbar
		}
	}

	static void recreatePlantChromeWidgets(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MWindow plant = plantWindow(application, modelService);
		if(plant == null || plant.getWidget() == null) {
			return;
		}
		MMenu menu = plant.getMainMenu();
		if(menu != null) {
			forceCreateElement(application, modelService, menu);
			applyEditorRequiredMenuVisibility(menu);
			if(plant.getWidget() instanceof Shell shell && !shell.isDisposed()) {
				Menu bar = shell.getMenuBar();
				if(bar != null && !bar.isDisposed()) {
					BaijiuShellMenus.sanitizeMainMenuBar(bar);
				}
			}
		}
		if(plant instanceof MTrimmedWindow trimmed) {
			MTrimBar live = liveTopTrim(application, modelService);
			if(live != null) {
				forceCreateElement(application, modelService, live);
			}
			MTrimBar top = findTrimBar(modelService, application, BaijiuShellChrome.TRIMBAR_TOP_ID);
			if(top == null) {
				top = trimBarOnWindow(trimmed, BaijiuShellChrome.TRIMBAR_TOP_ID);
			}
			if(top != null && top != live) {
				forceCreateElement(application, modelService, top);
			}
			MUIElement toolbar = modelService.find(BaijiuShellChrome.PLANT_TOOLBAR_ID, application);
			if(toolbar instanceof MToolBar plantToolbar) {
				ensurePlantToolbarContents(modelService, application, plantToolbar);
				forceCreateElement(application, modelService, plantToolbar);
			} else if(toolbar != null) {
				forceCreateElement(application, modelService, toolbar);
			}
		}
	}

	/**
	 * Keep {@link BaijiuShellChrome#EDITOR_REQUIRED_MENU_IDS} as {@code MMenu}
	 * children of the live window main menu so GroupHandler
	 * {@code getSubMenu} does not throw {@code NotDefinedException}. 视图
	 * stays the ChemClipse View menu id; 色谱 / 色谱图 stays defined but
	 * {@code visible=false} unless the research escape hatch is on.
	 */
	static void ensureEditorRequiredMenus(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		preferPlantLookupWindow(application, modelService);
		MMenu mainMenu = windowMainMenu(application);
		if(mainMenu == null) {
			ensurePlantChromeModel(application, modelService);
			mainMenu = windowMainMenu(application);
		}
		if(mainMenu == null) {
			mainMenu = findMenu(modelService, application, BaijiuShellChrome.MAIN_MENU_ID);
		}
		if(mainMenu == null) {
			mainMenu = findMenu(modelService, application, BaijiuShellChrome.ECLIPSE_MAIN_MENU_ID);
		}
		if(mainMenu == null) {
			return;
		}
		forceShowChrome(mainMenu);
		ensureEditorRequiredMenus(mainMenu, modelService, application);
	}

	/**
	 * Idempotent: a second call must not append another {@code menu.view}.
	 * Used by fragment tests without an {@code EModelService.find} index.
	 */
	static void ensureEditorRequiredMenus(MMenu mainMenu) {

		ensureEditorRequiredMenus(mainMenu, null, null);
	}

	static void ensureEditorRequiredMenus(MMenu mainMenu, EModelService modelService, MApplication application) {

		if(mainMenu == null) {
			return;
		}
		forceShowChrome(mainMenu);
		for(String id : BaijiuShellChrome.EDITOR_REQUIRED_MENU_IDS) {
			MMenu menu = liveMenuChild(mainMenu, id);
			if(menu == null) {
				menu = findMenu(modelService, application, id);
			}
			if(menu == null) {
				menu = createEditorRequiredMenu(modelService, id);
			}
			if(menu == null) {
				continue;
			}
			attachMenuChild(mainMenu, menu);
			applyEditorRequiredMenuVisibility(menu);
			if(BaijiuShellChrome.VIEW_MENU_ID.equals(id)) {
				ensureViewMenuContents(modelService, application, menu);
				dedupePlantMenuChildren(menu);
			}
		}
		dedupePlantMenuChildren(mainMenu);
		orderPlantTopMenus(mainMenu);
		applyEditorRequiredMenuVisibility(mainMenu);
	}

	private static MMenu windowMainMenu(MApplication application) {

		MWindow plant = plantWindow(application, null);
		if(plant != null && plant.getMainMenu() != null) {
			return plant.getMainMenu();
		}
		if(application == null) {
			return null;
		}
		List<MWindow> windows;
		try {
			windows = application.getChildren();
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		if(windows == null) {
			return null;
		}
		for(MWindow window : windows) {
			if(window == null || BaijiuShellChrome.GC_WINDOW_ID.equals(window.getElementId())) {
				continue;
			}
			MMenu mainMenu = window.getMainMenu();
			if(mainMenu != null) {
				return mainMenu;
			}
		}
		return null;
	}

	private static MMenu createEditorRequiredMenu(EModelService modelService, String elementId) {

		MMenu menu = null;
		if(modelService != null) {
			try {
				menu = modelService.createModelElement(MMenu.class);
			} catch(RuntimeException | LinkageError e) {
				menu = null;
			}
		}
		if(menu == null) {
			try {
				menu = MMenuFactory.INSTANCE.createMenu();
			} catch(RuntimeException | LinkageError e) {
				return null;
			}
		}
		menu.setElementId(elementId);
		if(BaijiuShellChrome.VIEW_MENU_ID.equals(elementId)) {
			menu.setLabel("视图");
		} else if(BaijiuShellChrome.CHROMATOGRAM_MENU_ID.equals(elementId)) {
			menu.setLabel("色谱");
		} else if(BaijiuShellChrome.isHelpMenuId(elementId)) {
			menu.setLabel("帮助");
		} else if(BaijiuShellChrome.FILE_MENU_ID.equals(elementId)) {
			menu.setLabel("文件");
		} else if(BaijiuShellChrome.BAIJIU_MENU_ID.equals(elementId)) {
			menu.setLabel("白酒");
		}
		menu.setToBeRendered(true);
		if(BaijiuShellChrome.isEditorRequiredMenu(elementId)) {
			menu.setVisible(BaijiuShellChrome.editorRequiredMenuVisible(elementId));
		} else {
			menu.setVisible(true);
		}
		return menu;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void attachMenuChild(MMenu parent, MMenu child) {

		if(parent == null || child == null || parent == child) {
			return;
		}
		try {
			String id = child.getElementId();
			if(!BaijiuShellChrome.shouldAppendMenuChild(childIdsOf(parent), id)) {
				MMenu existing = liveMenuChild(parent, id);
				if(existing != null && existing != child) {
					forceShowChrome(existing);
					applyEditorRequiredMenuVisibility(existing);
				}
				return;
			}
			MElementContainer<?> currentParent = child.getParent();
			if(currentParent != null && currentParent != parent) {
				List siblings = currentParent.getChildren();
				if(siblings != null) {
					siblings.remove(child);
				}
			}
			List children = parent.getChildren();
			if(children != null && !children.contains(child)) {
				children.add(child);
			}
		} catch(RuntimeException | LinkageError e) {
			// menu children not writable
		}
	}

	/**
	 * View paints; chromatogram stays a child with {@code visible=false}
	 * and {@code toBeRendered=true} so GroupHandler still finds the id.
	 */
	static void applyEditorRequiredMenuVisibility(MMenu menu) {

		if(menu == null) {
			return;
		}
		applyEditorRequiredMenuVisibility((MUIElement)menu);
		try {
			List<?> children = menu.getChildren();
			if(children == null) {
				return;
			}
			for(Object child : children) {
				if(child instanceof MMenu childMenu) {
					applyEditorRequiredMenuVisibility(childMenu);
				} else if(child instanceof MUIElement element) {
					applyEditorRequiredMenuVisibility(element);
				}
			}
		} catch(RuntimeException | LinkageError e) {
			// menu children not readable
		}
	}

	private static void applyEditorRequiredMenuVisibility(MUIElement element) {

		if(element == null) {
			return;
		}
		String id = element.getElementId();
		if(!BaijiuShellChrome.isEditorRequiredMenu(id)) {
			return;
		}
		removeTag(element, IPresentationEngine.HIDDEN_EXPLICITLY);
		element.setToBeRendered(true);
		element.setVisible(BaijiuShellChrome.editorRequiredMenuVisible(id));
	}

	/**
	 * JFace MenuManager omits an empty 视图 cascade. Keep Select View as a
	 * visible child so the top-level label paints. Prefer a commanded
	 * ChemClipse item over a dummy; DirectMenuItem fallback opens the
	 * same dialog when the command is not in the model yet. Icon URI is
	 * cleared every pass: ChemClipse's missing command image paints a red
	 * square on cold start, and later contributions can re-attach it.
	 */
	static void ensureViewMenuContents(EModelService modelService, MApplication application, MMenu viewMenu) {

		if(viewMenu == null) {
			return;
		}
		MUIElement select = bestSelectViewItem(viewMenu);
		if(select == null && modelService != null && application != null) {
			select = findSelectViewElement(modelService, application);
		}
		if(select == null) {
			select = createSelectViewItem(modelService, application);
		}
		select = ensureSelectViewExecutable(modelService, application, viewMenu, select);
		if(select instanceof MMenuElement item) {
			paintSelectViewItem(select);
			attachMenuElement(viewMenu, item);
		}
		dedupePlantMenuChildren(viewMenu);
		sanitizeViewMenuChildren(viewMenu);
		try {
			List<?> children = viewMenu.getChildren();
			if(children != null) {
				for(Object child : children) {
					if(child instanceof MUIElement element && BaijiuShellChrome.SELECT_VIEW_MENU_ID.equals(element.getElementId())) {
						paintSelectViewItem(element);
						if(element instanceof MHandledMenuItem handled) {
							bindSelectViewCommand(modelService, application, handled);
							paintSelectViewItem(handled);
						} else if(element instanceof MDirectMenuItem direct) {
							bindSelectViewDirectHandler(direct);
							paintSelectViewItem(direct);
						}
					}
				}
			}
		} catch(RuntimeException | LinkageError e) {
			// view children not readable
		}
	}

	/**
	 * 选择视图 is label-only. Empty {@code iconURI} plus a null SWT image
	 * so a missing ChemClipse command icon cannot paint a red square.
	 * Idempotent: later fragment merges that restore an icon are cleared
	 * on the next chrome sanitize without rebuilding {@code workbench.xmi}.
	 */
	static void paintSelectViewItem(MUIElement element) {

		if(element == null) {
			return;
		}
		forceShowChrome(element);
		if(element instanceof MUILabel labeled) {
			try {
				labeled.setLabel(BaijiuShellChrome.SELECT_VIEW_TITLE_ZH);
			} catch(RuntimeException | LinkageError e) {
				// label not writable
			}
			clearSelectViewIcon(labeled);
		}
		clearSelectViewWidgetImage(element);
	}

	static void clearSelectViewIcon(MUILabel labeled) {

		if(labeled == null) {
			return;
		}
		try {
			String uri = labeled.getIconURI();
			if(uri != null && uri.isEmpty()) {
				return;
			}
			labeled.setIconURI("");
		} catch(RuntimeException | LinkageError e) {
			// iconURI not writable
		}
	}

	static void clearSelectViewWidgetImage(MUIElement element) {

		if(element == null) {
			return;
		}
		try {
			Object widget = element.getWidget();
			if(widget instanceof MenuItem item) {
				BaijiuShellMenus.clearSelectViewImage(item);
			}
		} catch(RuntimeException | LinkageError e) {
			// widget not an SWT MenuItem / already closing
		}
	}

	private static MUIElement bestSelectViewItem(MMenu viewMenu) {

		MUIElement first = mainMenuChild(viewMenu, BaijiuShellChrome.SELECT_VIEW_MENU_ID);
		MUIElement best = first;
		try {
			List<?> children = viewMenu.getChildren();
			if(children != null) {
				for(Object child : children) {
					if(!(child instanceof MUIElement element)) {
						continue;
					}
					if(!BaijiuShellChrome.SELECT_VIEW_MENU_ID.equals(element.getElementId()) && !BaijiuShellChrome.isViewMenuKeepLabel(labelOf(element))) {
						continue;
					}
					best = preferredPlantMenuChild(best, element);
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return first;
		}
		return best;
	}

	private static MUIElement findSelectViewElement(EModelService modelService, MApplication application) {

		MUIElement found = findElement(modelService, application, BaijiuShellChrome.SELECT_VIEW_MENU_ID);
		if(modelService == null || application == null) {
			return found;
		}
		try {
			List<MHandledMenuItem> handled = modelService.findElements(application, BaijiuShellChrome.SELECT_VIEW_MENU_ID, MHandledMenuItem.class, null);
			if(handled != null) {
				for(MHandledMenuItem item : handled) {
					found = preferredPlantMenuChild(found, item);
				}
			}
		} catch(RuntimeException | LinkageError e) {
			// older E4
		}
		try {
			List<MDirectMenuItem> directs = modelService.findElements(application, BaijiuShellChrome.SELECT_VIEW_MENU_ID, MDirectMenuItem.class, null);
			if(directs != null) {
				for(MDirectMenuItem item : directs) {
					found = preferredPlantMenuChild(found, item);
				}
			}
		} catch(RuntimeException | LinkageError e) {
			// older E4
		}
		return found;
	}

	private static MUIElement ensureSelectViewExecutable(EModelService modelService, MApplication application, MMenu viewMenu, MUIElement select) {

		if(select instanceof MHandledMenuItem handled) {
			bindSelectViewCommand(modelService, application, handled);
			paintSelectViewItem(handled);
			if(isExecutableSelectViewItem(handled)) {
				return handled;
			}
			MDirectMenuItem direct = createSelectViewDirectItem(modelService);
			if(direct == null) {
				return handled;
			}
			replaceMenuChild(viewMenu, handled, direct);
			return direct;
		}
		if(select instanceof MDirectMenuItem direct) {
			bindSelectViewDirectHandler(direct);
			paintSelectViewItem(direct);
			return direct;
		}
		paintSelectViewItem(select);
		return select;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void replaceMenuChild(MMenu parent, MUIElement oldChild, MMenuElement newChild) {

		if(parent == null || oldChild == null || newChild == null || oldChild == newChild) {
			return;
		}
		try {
			List children = parent.getChildren();
			if(children == null) {
				return;
			}
			int index = children.indexOf(oldChild);
			disposeMenuWidget(oldChild);
			if(index >= 0) {
				children.set(index, newChild);
			} else if(!children.contains(newChild)) {
				children.add(0, newChild);
			}
		} catch(RuntimeException | LinkageError e) {
			// menu children not writable
		}
	}

	private static MMenuElement createSelectViewItem(EModelService modelService, MApplication application) {

		MCommand command = findSelectViewCommand(modelService, application, null);
		if(command != null) {
			MHandledMenuItem handled = createHandledMenuItem(modelService);
			if(handled == null) {
				return createSelectViewDirectItem(modelService);
			}
			handled.setElementId(BaijiuShellChrome.SELECT_VIEW_MENU_ID);
			handled.setLabel(BaijiuShellChrome.SELECT_VIEW_TITLE_ZH);
			handled.setToBeRendered(true);
			handled.setVisible(true);
			clearSelectViewIcon(handled);
			try {
				handled.setCommand(command);
			} catch(RuntimeException | LinkageError e) {
				return createSelectViewDirectItem(modelService);
			}
			paintSelectViewItem(handled);
			return handled;
		}
		return createSelectViewDirectItem(modelService);
	}

	private static MHandledMenuItem createHandledMenuItem(EModelService modelService) {

		MHandledMenuItem item = null;
		if(modelService != null) {
			try {
				item = modelService.createModelElement(MHandledMenuItem.class);
			} catch(RuntimeException | LinkageError e) {
				item = null;
			}
		}
		if(item == null) {
			try {
				item = MMenuFactory.INSTANCE.createHandledMenuItem();
			} catch(RuntimeException | LinkageError e) {
				return null;
			}
		}
		return item;
	}

	private static MDirectMenuItem createSelectViewDirectItem(EModelService modelService) {

		MDirectMenuItem item = null;
		if(modelService != null) {
			try {
				item = modelService.createModelElement(MDirectMenuItem.class);
			} catch(RuntimeException | LinkageError e) {
				item = null;
			}
		}
		if(item == null) {
			try {
				item = MMenuFactory.INSTANCE.createDirectMenuItem();
			} catch(RuntimeException | LinkageError e) {
				return null;
			}
		}
		item.setElementId(BaijiuShellChrome.SELECT_VIEW_MENU_ID);
		item.setLabel(BaijiuShellChrome.SELECT_VIEW_TITLE_ZH);
		item.setToBeRendered(true);
		item.setVisible(true);
		clearSelectViewIcon(item);
		bindSelectViewDirectHandler(item);
		paintSelectViewItem(item);
		return item;
	}

	private static void bindSelectViewDirectHandler(MDirectMenuItem item) {

		if(item == null) {
			return;
		}
		try {
			String uri = item.getContributionURI();
			if(uri != null && !uri.isBlank()) {
				return;
			}
		} catch(RuntimeException | LinkageError e) {
			// uri not readable
		}
		try {
			item.setContributionURI(BaijiuShellChrome.SELECT_VIEW_DIRECT_HANDLER_URI);
		} catch(RuntimeException | LinkageError e) {
			// uri not writable
		}
	}

	private static void bindSelectViewCommand(EModelService modelService, MApplication application, MHandledMenuItem item) {

		if(item == null) {
			return;
		}
		try {
			MCommand existing = item.getCommand();
			if(isSelectViewCommand(existing)) {
				return;
			}
		} catch(RuntimeException | LinkageError e) {
			// command not readable
		}
		MCommand command = findSelectViewCommand(modelService, application, item);
		if(command == null) {
			return;
		}
		try {
			item.setCommand(command);
		} catch(RuntimeException | LinkageError e) {
			// command not writable
		}
	}

	private static boolean isSelectViewCommand(MCommand command) {

		if(command == null) {
			return false;
		}
		try {
			String id = command.getElementId();
			return BaijiuShellChrome.SELECT_VIEW_COMMAND_ID.equals(id) || BaijiuShellChrome.ECLIPSE_SHOW_VIEW_COMMAND_ID.equals(id);
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
	}

	private static MCommand findSelectViewCommand(EModelService modelService, MApplication application, MHandledMenuItem exclude) {

		MCommand fromTool = commandOf(findElement(modelService, application, BaijiuShellChrome.SELECT_VIEW_TOOLITEM_ID));
		if(fromTool != null) {
			return fromTool;
		}
		MCommand chemclipse = findCommand(modelService, application, BaijiuShellChrome.SELECT_VIEW_COMMAND_ID);
		if(chemclipse != null) {
			return chemclipse;
		}
		if(modelService != null && application != null) {
			try {
				List<MHandledMenuItem> items = modelService.findElements(application, BaijiuShellChrome.SELECT_VIEW_MENU_ID, MHandledMenuItem.class, null);
				if(items != null) {
					for(MHandledMenuItem item : items) {
						if(item == exclude) {
							continue;
						}
						MCommand command = commandOf(item);
						if(command != null) {
							return command;
						}
					}
				}
			} catch(RuntimeException | LinkageError e) {
				// older E4
			}
			try {
				List<MHandledToolItem> tools = modelService.findElements(application, BaijiuShellChrome.SELECT_VIEW_TOOLITEM_ID, MHandledToolItem.class, null);
				if(tools != null) {
					for(MHandledToolItem item : tools) {
						MCommand command = commandOf(item);
						if(command != null) {
							return command;
						}
					}
				}
			} catch(RuntimeException | LinkageError e) {
				// older E4
			}
		}
		return findCommand(modelService, application, BaijiuShellChrome.ECLIPSE_SHOW_VIEW_COMMAND_ID);
	}

	private static MUIElement findElement(EModelService modelService, MApplication application, String elementId) {

		if(modelService == null || application == null || elementId == null || elementId.isBlank()) {
			return null;
		}
		try {
			MUIElement found = modelService.find(elementId, application);
			if(found != null) {
				return found;
			}
		} catch(RuntimeException | LinkageError e) {
			// find index miss
		}
		return null;
	}

	private static MCommand commandOf(MUIElement element) {

		if(element instanceof MHandledMenuItem item) {
			try {
				return item.getCommand();
			} catch(RuntimeException | LinkageError e) {
				return null;
			}
		}
		if(element instanceof MHandledToolItem item) {
			try {
				return item.getCommand();
			} catch(RuntimeException | LinkageError e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Hide ChemClipse {@code xxd.ui.view.*} cascades without removing them
	 * (GroupHandler still looks them up). Keep Select View painted.
	 */
	static void sanitizeViewMenuChildren(MMenu viewMenu) {

		if(viewMenu == null || BaijiuShellChrome.researchMenusVisible()) {
			return;
		}
		try {
			List<?> children = viewMenu.getChildren();
			if(children == null) {
				return;
			}
			for(Object child : children) {
				if(!(child instanceof MUIElement element)) {
					continue;
				}
				if(BaijiuShellChrome.shouldHideViewMenuChild(element.getElementId(), labelOf(element))) {
					hideMenuChild(element);
				} else if(BaijiuShellChrome.isViewMenuKeepId(element.getElementId()) || BaijiuShellChrome.isViewMenuKeepLabel(labelOf(element))) {
					paintSelectViewItem(element);
				}
			}
		} catch(RuntimeException | LinkageError e) {
			// view children not readable
		}
	}

	/**
	 * Idempotent File menu: ensure ChemClipse Save exists, hide import /
	 * export / save-all, translate keep items.
	 */
	static void ensureFileMenuContents(EModelService modelService, MApplication application, MMenu fileMenu) {

		if(fileMenu == null) {
			return;
		}
		MUIElement save = mainMenuChild(fileMenu, BaijiuShellChrome.SAVE_MENU_ID);
		if(save == null && modelService != null && application != null) {
			MUIElement found = modelService.find(BaijiuShellChrome.SAVE_MENU_ID, application);
			if(found instanceof MMenuElement) {
				save = found;
			}
		}
		if(save == null) {
			save = createSaveMenuItem(modelService, application);
		}
		if(save instanceof MMenuElement item) {
			forceShowChrome(save);
			if(save instanceof MUILabel labeled) {
				labeled.setLabel("保存");
			}
			attachMenuElement(fileMenu, item);
		}
		sanitizeFileMenuChildren(fileMenu);
	}

	/**
	 * JFace MenuManager omits an empty 帮助 cascade (same as 视图). Keep a
	 * single 关于 DirectMenuItem so the top-level label paints. ChemClipse
	 * / Eclipse About stay hidden.
	 */
	static void ensureHelpMenuContents(EModelService modelService, MApplication application, MMenu helpMenu) {

		if(helpMenu == null) {
			return;
		}
		if(helpMenu.getLabel() == null || helpMenu.getLabel().isBlank()) {
			helpMenu.setLabel("帮助");
		}
		forceShowChrome(helpMenu);
		MUIElement about = bestHelpAboutItem(helpMenu);
		if(about == null && modelService != null && application != null) {
			about = findHelpAboutElement(modelService, application);
		}
		if(about == null) {
			about = createAboutDirectItem(modelService);
		}
		about = ensureAboutExecutable(helpMenu, about);
		if(about instanceof MMenuElement item) {
			forceShowChrome(about);
			if(about instanceof MUILabel labeled) {
				labeled.setLabel(BaijiuShellChrome.ABOUT_LABEL_ZH);
			}
			attachMenuElement(helpMenu, item);
		}
		dedupePlantMenuChildren(helpMenu);
		sanitizeHelpMenuChildren(helpMenu);
	}

	private static MUIElement bestHelpAboutItem(MMenu helpMenu) {

		MUIElement first = mainMenuChild(helpMenu, BaijiuShellChrome.PLANT_ABOUT_MENU_ID);
		MUIElement best = first;
		try {
			List<?> children = helpMenu.getChildren();
			if(children != null) {
				for(Object child : children) {
					if(!(child instanceof MUIElement element)) {
						continue;
					}
					if(isPlantAboutItem(element)) {
						best = preferredPlantMenuChild(best, element);
					}
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return first;
		}
		return best;
	}

	private static MUIElement findHelpAboutElement(EModelService modelService, MApplication application) {

		MUIElement found = findElement(modelService, application, BaijiuShellChrome.PLANT_ABOUT_MENU_ID);
		return found instanceof MMenuElement ? found : null;
	}

	private static boolean isPlantAboutItem(MUIElement element) {

		if(element == null) {
			return false;
		}
		if(BaijiuShellChrome.PLANT_ABOUT_MENU_ID.equals(element.getElementId())) {
			return true;
		}
		if(element instanceof MDirectMenuItem direct) {
			try {
				if(BaijiuShellChrome.isAboutDirectHandlerUri(direct.getContributionURI())) {
					return true;
				}
			} catch(RuntimeException | LinkageError e) {
				// uri not readable
			}
		}
		String id = element.getElementId();
		if(id != null && !id.isBlank()) {
			return false;
		}
		return BaijiuShellChrome.isHelpMenuKeepLabel(labelOf(element));
	}

	private static MUIElement ensureAboutExecutable(MMenu helpMenu, MUIElement about) {

		if(about instanceof MDirectMenuItem direct) {
			bindAboutDirectHandler(direct);
			return direct;
		}
		MDirectMenuItem created = createAboutDirectItem(null);
		if(created == null) {
			return about;
		}
		if(about != null) {
			replaceMenuChild(helpMenu, about, created);
		}
		return created;
	}

	private static MDirectMenuItem createAboutDirectItem(EModelService modelService) {

		MDirectMenuItem item = null;
		if(modelService != null) {
			try {
				item = modelService.createModelElement(MDirectMenuItem.class);
			} catch(RuntimeException | LinkageError e) {
				item = null;
			}
		}
		if(item == null) {
			try {
				item = MMenuFactory.INSTANCE.createDirectMenuItem();
			} catch(RuntimeException | LinkageError e) {
				return null;
			}
		}
		item.setElementId(BaijiuShellChrome.PLANT_ABOUT_MENU_ID);
		item.setLabel(BaijiuShellChrome.ABOUT_LABEL_ZH);
		item.setToBeRendered(true);
		item.setVisible(true);
		bindAboutDirectHandler(item);
		return item;
	}

	private static void bindAboutDirectHandler(MDirectMenuItem item) {

		if(item == null) {
			return;
		}
		try {
			item.setContributionURI(BaijiuShellChrome.ABOUT_DIRECT_HANDLER_URI);
		} catch(RuntimeException | LinkageError e) {
			// uri not writable
		}
	}

	static void sanitizeFileMenuChildren(MMenu fileMenu) {

		if(fileMenu == null || BaijiuShellChrome.researchMenusVisible()) {
			return;
		}
		try {
			List<?> children = fileMenu.getChildren();
			if(children == null) {
				return;
			}
			boolean seenSave = false;
			for(Object child : children) {
				if(!(child instanceof MUIElement element)) {
					continue;
				}
				String id = element.getElementId();
				String label = labelOf(element);
				boolean chemclipseSave = BaijiuShellChrome.SAVE_MENU_ID.equals(id);
				boolean saveLabel = BaijiuShellChrome.isFileMenuKeepLabel(label) && "save".equals(BaijiuShellChrome.normalizeMenuLabel(label == null ? "" : label));
				if(chemclipseSave || saveLabel) {
					if(seenSave && !chemclipseSave) {
						hideMenuChild(element);
						continue;
					}
					seenSave = true;
				}
				if(BaijiuShellChrome.shouldHideFileMenuChild(id, label)) {
					hideMenuChild(element);
					continue;
				}
				if(element instanceof MUILabel labeled) {
					String translated = BaijiuShellChrome.translateFileMenuItem(label);
					if(translated != null && !translated.equals(label)) {
						labeled.setLabel(translated);
					}
				}
				if(chemclipseSave) {
					forceShowChrome(element);
					if(element instanceof MUILabel labeled) {
						labeled.setLabel("保存");
					}
				}
			}
		} catch(RuntimeException | LinkageError e) {
			// file children not readable
		}
	}

	/**
	 * Re-apply 视图 / 文件 / 白酒 / 帮助 allowlists. Does not {@code createGui}
	 * cascades (that appends another 视图). Idempotent. Last SWT pass is
	 * {@link BaijiuShellMenus#sanitizePlantCascades}.
	 */
	static void sanitizePlantMenuContributions(MApplication application, EModelService modelService) {

		if(application == null) {
			return;
		}
		MMenu mainMenu = windowMainMenu(application);
		if(mainMenu == null && modelService != null) {
			mainMenu = findMenu(modelService, application, BaijiuShellChrome.MAIN_MENU_ID);
		}
		if(mainMenu != null) {
			applyEditorRequiredMenuVisibility(mainMenu);
			dedupePlantMenuChildren(mainMenu);
		}
		MMenu view = liveMenuChild(mainMenu, BaijiuShellChrome.VIEW_MENU_ID);
		if(view == null && modelService != null) {
			view = findMenu(modelService, application, BaijiuShellChrome.VIEW_MENU_ID);
		}
		ensureViewMenuContents(modelService, application, view);
		MMenu file = liveMenuChild(mainMenu, BaijiuShellChrome.FILE_MENU_ID);
		if(file == null && modelService != null) {
			file = findMenu(modelService, application, BaijiuShellChrome.FILE_MENU_ID);
		}
		ensureFileMenuContents(modelService, application, file);
		MMenu baijiu = liveMenuChild(mainMenu, BaijiuShellChrome.BAIJIU_MENU_ID);
		if(baijiu == null && modelService != null) {
			baijiu = findMenu(modelService, application, BaijiuShellChrome.BAIJIU_MENU_ID);
		}
		sanitizeBaijiuMenuChildren(baijiu);
		MMenu help = liveMenuChild(mainMenu, BaijiuShellChrome.HELP_MENU_ID);
		if(help == null && modelService != null) {
			help = findMenu(modelService, application, BaijiuShellChrome.HELP_MENU_ID);
		}
		if(help == null && mainMenu != null) {
			help = liveMenuChild(mainMenu, BaijiuShellChrome.ECLIPSE_HELP_MENU_ID);
		}
		ensureHelpMenuContents(modelService, application, help);
		hideChromatogramMenuLabel(application, modelService);
		hideNonPlantTopTrim(application, modelService);
		MWindow plant = plantWindow(application, modelService);
		if(plant != null && plant.getWidget() instanceof Shell shell && !shell.isDisposed()) {
			Menu bar = shell.getMenuBar();
			if(bar != null && !bar.isDisposed()) {
				BaijiuShellMenus.sanitizeMainMenuBar(bar);
				BaijiuShellMenus.sanitizePlantCascades(bar);
			}
		}
	}

	static void sanitizeBaijiuMenuChildren(MMenu baijiuMenu) {

		if(baijiuMenu == null || BaijiuShellChrome.researchMenusVisible()) {
			return;
		}
		try {
			List<?> children = baijiuMenu.getChildren();
			if(children == null) {
				return;
			}
			for(Object child : children) {
				if(!(child instanceof MUIElement element)) {
					continue;
				}
				if(BaijiuShellChrome.shouldHideBaijiuCascadeChild(element.getElementId(), labelOf(element))) {
					hideMenuChild(element);
				}
			}
		} catch(RuntimeException | LinkageError e) {
			// baijiu children not readable
		}
	}

	static void sanitizeHelpMenuChildren(MMenu helpMenu) {

		if(helpMenu == null || BaijiuShellChrome.researchMenusVisible()) {
			return;
		}
		try {
			List<?> children = helpMenu.getChildren();
			if(children == null) {
				return;
			}
			for(Object child : children) {
				if(!(child instanceof MUIElement element)) {
					continue;
				}
				if(BaijiuShellChrome.shouldHideHelpMenuChild(element.getElementId(), labelOf(element))) {
					hideMenuChild(element);
				} else {
					forceShowChrome(element);
				}
			}
		} catch(RuntimeException | LinkageError e) {
			// help children not readable
		}
	}

	private static MHandledMenuItem createSaveMenuItem(EModelService modelService, MApplication application) {

		MHandledMenuItem item = null;
		if(modelService != null) {
			try {
				item = modelService.createModelElement(MHandledMenuItem.class);
			} catch(RuntimeException | LinkageError e) {
				item = null;
			}
		}
		if(item == null) {
			try {
				item = MMenuFactory.INSTANCE.createHandledMenuItem();
			} catch(RuntimeException | LinkageError e) {
				return null;
			}
		}
		item.setElementId(BaijiuShellChrome.SAVE_MENU_ID);
		item.setLabel("保存");
		item.setToBeRendered(true);
		item.setVisible(true);
		MCommand command = findCommand(modelService, application, BaijiuShellChrome.SAVE_COMMAND_ID);
		if(command == null) {
			command = findCommand(modelService, application, BaijiuShellChrome.ECLIPSE_SAVE_COMMAND_ID);
		}
		if(command != null) {
			try {
				item.setCommand(command);
			} catch(RuntimeException | LinkageError e) {
				// command not writable
			}
		}
		return item;
	}

	private static MCommand findCommand(EModelService modelService, MApplication application, String commandId) {

		if(commandId == null || commandId.isBlank()) {
			return null;
		}
		if(modelService != null && application != null) {
			try {
				MUIElement found = modelService.find(commandId, application);
				if(found instanceof MCommand command) {
					return command;
				}
			} catch(RuntimeException | LinkageError e) {
				// find index miss
			}
			try {
				List<MCommand> commands = modelService.findElements(application, commandId, MCommand.class, null);
				if(commands != null) {
					for(MCommand command : commands) {
						if(command != null) {
							return command;
						}
					}
				}
			} catch(RuntimeException | LinkageError e) {
				// older E4
			}
		}
		if(application != null) {
			try {
				List<MCommand> commands = application.getCommands();
				if(commands != null) {
					for(MCommand command : commands) {
						if(command != null && commandId.equals(command.getElementId())) {
							return command;
						}
					}
				}
			} catch(RuntimeException | LinkageError e) {
				return null;
			}
		}
		return null;
	}

	private static void hideMenuChild(MUIElement element) {

		if(element == null) {
			return;
		}
		if(!BaijiuShellChrome.allowsWalkHide(element.getElementId(), labelOf(element))) {
			return;
		}
		try {
			element.setVisible(false);
			element.setToBeRendered(false);
		} catch(RuntimeException | LinkageError e) {
			// already disposed
		}
		try {
			List<String> tags = element.getTags();
			if(tags != null && !tags.contains(IPresentationEngine.HIDDEN_EXPLICITLY)) {
				tags.add(IPresentationEngine.HIDDEN_EXPLICITLY);
			}
		} catch(RuntimeException | LinkageError e) {
			// immutable tags
		}
	}

	private static String labelOf(MUIElement element) {

		if(element instanceof MUILabel labeled) {
			String localized = labeled.getLocalizedLabel();
			if(localized != null && !localized.isBlank()) {
				return localized;
			}
			return labeled.getLabel();
		}
		return null;
	}

	/**
	 * File / 白酒 / 视图 / 帮助 in that order. Chromatogram may remain a
	 * later child for lookup; it is not painted.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	static void orderPlantTopMenus(MMenu mainMenu) {

		if(mainMenu == null) {
			return;
		}
		try {
			List children = mainMenu.getChildren();
			if(children == null) {
				return;
			}
			for(int i = 0; i < BaijiuShellChrome.PLANT_TOP_MENU_IDS.size(); i++) {
				String id = BaijiuShellChrome.PLANT_TOP_MENU_IDS.get(i);
				MUIElement child = mainMenuChild(mainMenu, id);
				if(child == null) {
					continue;
				}
				int current = children.indexOf(child);
				if(current < 0) {
					children.add(Math.min(i, children.size()), child);
				} else if(current != i) {
					children.remove(child);
					children.add(Math.min(i, children.size()), child);
				}
			}
		} catch(RuntimeException | LinkageError e) {
			// menu children not writable
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void attachMenuElement(MMenu parent, MMenuElement child) {

		if(parent == null || child == null) {
			return;
		}
		try {
			String id = child.getElementId();
			if(!BaijiuShellChrome.shouldAppendMenuChild(childIdsOf(parent), id)) {
				return;
			}
			MElementContainer<?> currentParent = child.getParent();
			if(currentParent != null && currentParent != parent) {
				List siblings = currentParent.getChildren();
				if(siblings != null) {
					siblings.remove(child);
				}
			}
			List children = parent.getChildren();
			if(children != null && !children.contains(child)) {
				children.add(0, child);
			}
		} catch(RuntimeException | LinkageError e) {
			// menu children not writable
		}
	}

	/**
	 * After CSD action bars steal the top coolbar, {@code toolbar.plant}
	 * may be orphaned from {@code trimbar.top}. Re-attach it to the live
	 * TOP trim (ChemClipse trimbar.top or Eclipse {@code main.toolbar})
	 * at index 0 without walk-hiding chart toolitems.
	 */
	private static void attachPlantToolbarToVisibleTrim(MApplication application, EModelService modelService, MToolBar toolbar) {

		if(application == null || toolbar == null) {
			return;
		}
		MTrimBar live = liveTopTrim(application, modelService);
		if(live != null) {
			forceShowChrome(live);
			attachToolBar(live, toolbar);
			forceShowChrome(toolbar);
		}
	}

	private static MTrimBar liveTopTrim(MApplication application, EModelService modelService) {

		MWindow plant = plantWindow(application, modelService);
		if(plant instanceof MTrimmedWindow trimmed) {
			MTrimBar withWidget = firstTopTrimWithWidget(trimmed);
			if(withWidget != null) {
				return withWidget;
			}
			MTrimBar hosting = trimBarContainingPlantToolbar(trimmed);
			if(hosting != null) {
				return hosting;
			}
			MTrimBar firstTop = firstTopTrim(trimmed);
			if(firstTop != null) {
				return firstTop;
			}
			MTrimBar onWindow = trimBarOnWindow(trimmed, BaijiuShellChrome.TRIMBAR_TOP_ID);
			if(onWindow != null) {
				return onWindow;
			}
		}
		MTrimBar chemclipse = findTrimBar(modelService, application, BaijiuShellChrome.TRIMBAR_TOP_ID);
		if(chemclipse != null) {
			return chemclipse;
		}
		MUIElement eclipse = modelService == null ? null : modelService.find(BaijiuShellChrome.ECLIPSE_MAIN_TOOLBAR_ID, application);
		if(eclipse instanceof MTrimBar bar) {
			return bar;
		}
		return null;
	}

	private static MTrimBar trimBarContainingPlantToolbar(MTrimmedWindow window) {

		if(window == null) {
			return null;
		}
		try {
			List<MTrimBar> bars = window.getTrimBars();
			if(bars == null) {
				return null;
			}
			for(MTrimBar bar : bars) {
				if(bar != null && containsPlantToolbar(bar)) {
					return bar;
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	private static MTrimBar firstTopTrimWithWidget(MTrimmedWindow window) {

		if(window == null) {
			return null;
		}
		try {
			List<MTrimBar> bars = window.getTrimBars();
			if(bars == null) {
				return null;
			}
			for(MTrimBar bar : bars) {
				if(bar != null && isRenderedTopTrim(bar) && bar.getWidget() != null) {
					return bar;
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	private static boolean isRenderedTopTrim(MTrimBar bar) {

		if(bar == null) {
			return false;
		}
		try {
			if(bar.getSide() != SideValue.TOP) {
				return false;
			}
		} catch(RuntimeException | LinkageError e) {
			// older E4: treat as top
		}
		return bar.isToBeRendered() && bar.isVisible();
	}

	private static MTrimBar firstTopTrim(MTrimmedWindow window) {

		if(window == null) {
			return null;
		}
		try {
			List<MTrimBar> bars = window.getTrimBars();
			if(bars == null) {
				return null;
			}
			for(MTrimBar bar : bars) {
				if(bar != null && isRenderedTopTrim(bar)) {
					return bar;
				}
			}
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
		return null;
	}

	static void hideNonPlantTopTrim(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		hideNonPlantTrimChildren(modelService.find(BaijiuShellChrome.TRIMBAR_TOP_ID, application));
		hideNonPlantTrimChildren(modelService.find(BaijiuShellChrome.ECLIPSE_MAIN_TOOLBAR_ID, application));
	}

	private static void hideNonPlantTrimChildren(MUIElement element) {

		hideNonPlantTrimChildren(element, false);
	}

	private static void hideNonPlantTrimChildren(MUIElement element, boolean insidePlantToolbar) {

		if(!(element instanceof MElementContainer<?> container)) {
			return;
		}
		List<?> children = container.getChildren();
		if(children == null) {
			return;
		}
		for(Object child : children) {
			if(!(child instanceof MUIElement ui)) {
				continue;
			}
			String id = ui.getElementId();
			if(BaijiuShellChrome.TRIMBAR_TOP_ID.equals(id) || BaijiuShellChrome.ECLIPSE_MAIN_TOOLBAR_ID.equals(id)) {
				hideNonPlantTrimChildren(ui, false);
				continue;
			}
			if(BaijiuShellChrome.mustForceShowPlantChrome(id) || BaijiuShellChrome.isPlantToolbarContribution(id)) {
				forceShowChrome(ui);
				hideNonPlantTrimChildren(ui, true);
				continue;
			}
			if(containsPlantChrome(ui)) {
				hideNonPlantTrimChildren(ui, false);
				continue;
			}
			if(insidePlantToolbar) {
				forceShowChrome(ui);
				continue;
			}
			if(BaijiuShellChrome.shouldHideEclipseCoolbarFiller(id)) {
				ui.setVisible(false);
				ui.setToBeRendered(false);
			}
		}
	}

	private static boolean containsPlantChrome(MUIElement element) {

		if(element == null) {
			return false;
		}
		String id = element.getElementId();
		if(BaijiuShellChrome.mustForceShowPlantChrome(id) || BaijiuShellChrome.isPlantToolbarContribution(id)) {
			return true;
		}
		if(!(element instanceof MElementContainer<?> container)) {
			return false;
		}
		List<?> children = container.getChildren();
		if(children == null) {
			return false;
		}
		for(Object child : children) {
			if(child instanceof MUIElement ui && containsPlantChrome(ui)) {
				return true;
			}
		}
		return false;
	}

	public static void syncGcToggleToolItem(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement found = modelService.find(BaijiuShellChrome.TOGGLE_GC_TOOLITEM_ID, application);
		if(found instanceof org.eclipse.e4.ui.model.application.ui.menu.MItem item) {
			item.setSelected(BaijiuGcConsoleShell.isShowing());
		}
	}

	static void persistGcConsoleHidden(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement window = gcConsoleWindow(application, modelService);
		MUIElement stack = modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application);
		MUIElement target = window != null ? window : stack;
		if(target != null) {
			addTag(target, BaijiuShellChrome.GC_CONSOLE_HIDDEN_TAG);
			if(stack != null && stack != target) {
				addTag(stack, BaijiuShellChrome.GC_CONSOLE_HIDDEN_TAG);
			}
		}
	}

	static boolean hostOpenCsdEditors(MApplication application, EModelService modelService, EPartService partService) {

		if(application == null || modelService == null) {
			return false;
		}
		MUIElement stackElement = modelService.find(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, application);
		if(!(stackElement instanceof MPartStack plantStack)) {
			return false;
		}
		List<MPart> editors = modelService.findElements(application, BaijiuShellChrome.CSD_EDITOR_PART_ID, MPart.class, null);
		if(editors == null || editors.isEmpty()) {
			return false;
		}
		MPart home = findPart(modelService, application, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID);
		boolean hosted = false;
		MPart last = null;
		for(MPart part : editors) {
			if(part == null || !hasCsdInput(part)) {
				continue;
			}
			if(!hostCsdInPlantStack(partService, plantStack, part)) {
				continue;
			}
			last = part;
			hosted = true;
		}
		if(hosted && last != null) {
			hideEmptyChromatogramHome(home, true);
			BaijiuShellSelection.selectInParent(last);
			dedupePlantWorkflowStack(application, modelService);
			revealPlantWindowChrome(application, modelService);
		} else {
			hideEmptyChromatogramHome(home, false);
		}
		return hosted;
	}

	/**
	 * Java 21: do not compare {@code getParent()} to {@code MPartStack} (#49).
	 * Membership is {@code plantStack.getChildren().contains(part)} (#56).
	 */
	static boolean dockIntoPlantChromatogramStack(MPartStack plantStack, MPart part) {

		if(part == null || plantStack == null) {
			return false;
		}
		try {
			if(plantStack.getChildren().contains(part)) {
				return true;
			}
			MElementContainer<MUIElement> parent = part.getParent();
			if(parent != null) {
				parent.getChildren().remove(part);
			}
			if(!plantStack.getChildren().contains(part)) {
				plantStack.getChildren().add(part);
			}
			return plantStack.getChildren().contains(part);
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
	}

	static boolean dockOffWorkflowTabs(MApplication application, EModelService modelService, MPartStack plantStack, MPart part) {

		return dockIntoPlantChromatogramStack(plantStack, part);
	}

	static boolean addToSharedElements(MApplication application, MPart part) {

		if(application == null || part == null) {
			return false;
		}
		try {
			List<MWindow> windows = application.getChildren();
			if(windows == null) {
				return false;
			}
			for(MWindow window : windows) {
				if(window == null) {
					continue;
				}
				List<MUIElement> shared = window.getSharedElements();
				if(shared == null) {
					continue;
				}
				if(!shared.contains(part)) {
					shared.add(part);
				}
				return true;
			}
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
		return false;
	}

	static boolean embedCsdEditor(MApplication application, EPartService partService, MPart part, Composite host) {

		if(part == null || application == null) {
			return false;
		}
		EModelService modelService = null;
		try {
			if(application.getContext() != null) {
				modelService = application.getContext().get(EModelService.class);
			}
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
		MUIElement stackElement = modelService == null ? null : modelService.find(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, application);
		if(!(stackElement instanceof MPartStack plantStack)) {
			return false;
		}
		return hostCsdInPlantStack(partService, plantStack, part);
	}

	static boolean hostCsdInPlantStack(EPartService partService, MPartStack plantStack, MPart part) {

		if(!dockIntoPlantChromatogramStack(plantStack, part)) {
			return false;
		}
		part.setVisible(true);
		part.setToBeRendered(true);
		if(partService != null && part.getWidget() == null) {
			try {
				partService.showPart(part, PartState.CREATE);
			} catch(RuntimeException | LinkageError e) {
				try {
					partService.showPart(part, PartState.ACTIVATE);
				} catch(RuntimeException | LinkageError e2) {
					// ChemClipse openEditor may already have constructed the widget
				}
			}
		}
		return plantStack.getChildren().contains(part);
	}

	static void hideEmptyChromatogramHome(MPart home, boolean hide) {

		if(home == null) {
			return;
		}
		home.setToBeRendered(true);
		home.setVisible(!hide);
		if(!hide && homeWidget(home) != null) {
			BaijiuHomePanels.createChromatogramEmptyState(homeWidget(home));
		}
	}

	/**
	 * Selecting 谱图/采集 or a 白酒操作 button must not drop a hosted CSD
	 * from {@code partstack.plantChromatogram}.
	 */
	public static boolean selectionClearsHostedEditor() {

		return false;
	}

	static boolean reparentEditorWidget(MPart part, Composite host) {

		if(part == null || host == null || host.isDisposed()) {
			return false;
		}
		if(part.getWidget() instanceof Control control && !control.isDisposed()) {
			BaijiuHomePanels.hostEditor(host, control);
			return control.getParent() == host || BaijiuHomePanels.isAncestor(control, host) || BaijiuHomePanels.isAncestor(host, control);
		}
		return false;
	}

	static Composite homeWidget(MPart home) {

		if(home != null && home.getWidget() instanceof Composite composite && !composite.isDisposed()) {
			return composite;
		}
		return null;
	}

	static void revealChromatogramPlaceholder(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		parkChromatogramEditorArea(application, modelService);
		showElementAndAncestors(modelService.find(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID, application));
		showElementAndAncestors(modelService.find(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, application));
		showElementAndAncestors(modelService.find(BaijiuShellChrome.WORKFLOW_STACK_ID, application));
		revealStackChildren(application, modelService, BaijiuShellChrome.CHROMATOGRAM_STACK_ID);
		revealStackChildren(application, modelService, BaijiuShellChrome.WORKFLOW_STACK_ID);
	}

	static boolean revealChromatogramHost(MApplication application, EModelService modelService, EPartService partService) {

		revealChromatogramPlaceholder(application, modelService);
		boolean emptyState = showPart(application, modelService, partService, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID, null);
		if(emptyState) {
			forceCreateGui(application, modelService, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID);
		}
		return emptyState || findPart(modelService, application, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID) != null;
	}

	/**
	 * Keep {@code placeholder.plantChromatogram} in the model (required id)
	 * but never paint the ChemClipse editor Area into the left PartStack.
	 * An MPlaceholder for an MArea does not get a CTabItem; {@code createGui}
	 * on it fills 谱图/采集 with nested empty sash/stack frames.
	 */
	static void parkChromatogramEditorArea(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement found = findPlantChromatogram(application, modelService);
		if(found == null) {
			return;
		}
		BaijiuShellSelection.deselectFromParent(found);
		found.setVisible(false);
		found.setToBeRendered(false);
	}

	static void attachChromatogramPlaceholder(MApplication application, EModelService modelService) {

		parkChromatogramEditorArea(application, modelService);
	}

	static void restoreDefaultTabSelection(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		revealStackChildren(application, modelService, BaijiuShellChrome.WORKFLOW_STACK_ID);
		revealStackChildren(application, modelService, BaijiuShellChrome.CHROMATOGRAM_STACK_ID);
		if(!hostOpenCsdEditors(application, modelService, partService(application))) {
			MPart chromatogramHome = findPart(modelService, application, BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID);
			if(chromatogramHome != null) {
				BaijiuShellSelection.selectInParent(chromatogramHome);
			}
		}
		MPart workbench = findPart(modelService, application, BaijiuShellChrome.WORKBENCH_HOME_PART_ID);
		if(workbench != null) {
			BaijiuShellSelection.selectInParent(workbench);
		}
	}

	static void revealStackChildren(MApplication application, EModelService modelService, String stackId) {

		if(application == null || modelService == null || stackId == null || stackId.isBlank()) {
			return;
		}
		MUIElement stack = modelService.find(stackId, application);
		showElementAndAncestors(stack);
		if(!(stack instanceof MElementContainer<?> container)) {
			return;
		}
		List<?> children = container.getChildren();
		if(children == null) {
			return;
		}
		for(Object child : children) {
			if(child instanceof MUIElement element) {
				if(isParkedEditorArea(element)) {
					element.setVisible(false);
					element.setToBeRendered(false);
					continue;
				}
				element.setVisible(true);
				element.setToBeRendered(true);
			}
		}
	}

	static void dedupePlantWorkflowStack(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement workflow = modelService.find(BaijiuShellChrome.WORKFLOW_STACK_ID, application);
		MUIElement chromatogram = modelService.find(BaijiuShellChrome.CHROMATOGRAM_STACK_ID, application);
		if(workflow instanceof MPartStack workflowStack) {
			dedupePlantWorkflowStack(workflowStack, chromatogram instanceof MPartStack chromatogramStack ? chromatogramStack : null);
		}
	}

	/**
	 * Right sash is a single 白酒操作 tab. Extra plant-home copies, the
	 * community shared workbench part, and E4 generated clones are removed.
	 * A CSD editor that landed here is moved to 谱图/采集.
	 */
	static void dedupePlantWorkflowStack(MPartStack workflow, MPartStack chromatogram) {

		if(workflow == null) {
			return;
		}
		List<?> children;
		try {
			children = workflow.getChildren();
		} catch(RuntimeException | LinkageError e) {
			return;
		}
		if(children == null || children.isEmpty()) {
			return;
		}
		MUIElement keep = null;
		int keepPriority = 0;
		for(Object child : children) {
			if(!(child instanceof MUIElement element)) {
				continue;
			}
			int priority = BaijiuShellChrome.plantWorkflowOpsPriority(element.getElementId(), labelOf(element));
			if(priority > keepPriority) {
				keep = element;
				keepPriority = priority;
			}
		}
		boolean alreadyKeptOps = false;
		for(int i = 0; i < children.size();) {
			Object child = children.get(i);
			if(!(child instanceof MUIElement element)) {
				i++;
				continue;
			}
			String id = element.getElementId();
			String label = labelOf(element);
			if(element == keep) {
				element.setVisible(true);
				element.setToBeRendered(true);
				alreadyKeptOps = true;
				i++;
				continue;
			}
			if(BaijiuShellChrome.shouldMoveOffPlantWorkflow(id, label)) {
				if(chromatogram != null && element instanceof MPart part && dockIntoPlantChromatogramStack(chromatogram, part)) {
					continue;
				}
				i++;
				continue;
			}
			if(!BaijiuShellChrome.shouldKeepPlantWorkflowChild(id, label, alreadyKeptOps) && BaijiuShellChrome.isPlantWorkflowOpsChild(id, label)) {
				element.setVisible(false);
				element.setToBeRendered(false);
				children.remove(i);
				continue;
			}
			i++;
		}
		if(keep != null) {
			BaijiuShellSelection.selectInParent(keep);
		}
	}

	static boolean isParkedEditorArea(MUIElement element) {

		if(element == null) {
			return false;
		}
		String id = element.getElementId();
		return BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID.equals(id) || BaijiuShellChrome.EDITOR_AREA_ID.equals(id);
	}

	private static void showElementAndAncestors(MUIElement element) {

		if(isParkedEditorArea(element)) {
			return;
		}
		MUIElement walk = element;
		while(walk != null) {
			if(isParkedEditorArea(walk)) {
				break;
			}
			forceShowChrome(walk);
			walk = walk.getParent();
		}
	}

	private static void forceShowChrome(MUIElement element) {

		if(element == null || isParkedEditorArea(element)) {
			return;
		}
		removeTag(element, IPresentationEngine.HIDDEN_EXPLICITLY);
		element.setVisible(true);
		element.setToBeRendered(true);
		try {
			Object widget = element.getWidget();
			if(widget instanceof Control control && !control.isDisposed()) {
				control.setVisible(true);
			}
		} catch(RuntimeException | LinkageError e) {
			// headless fragment tests / widget not an SWT Control
		}
	}

	private static void reattachWindowMainMenu(MApplication application, EModelService modelService) {

		MMenu plantMenu = findMenu(modelService, application, BaijiuShellChrome.MAIN_MENU_ID);
		if(plantMenu == null) {
			plantMenu = findMenu(modelService, application, BaijiuShellChrome.ECLIPSE_MAIN_MENU_ID);
		}
		forceShowChrome(plantMenu);
		if(plantMenu instanceof MElementContainer<?> container) {
			List<?> children = container.getChildren();
			if(children != null) {
				for(Object child : children) {
					if(child instanceof MUIElement element && BaijiuShellChrome.isPlantWindowChrome(element.getElementId())) {
						forceShowChrome(element);
					}
				}
			}
		}
		List<MWindow> windows = application.getChildren();
		if(windows == null) {
			return;
		}
		for(MWindow window : windows) {
			if(window == null || BaijiuShellChrome.GC_WINDOW_ID.equals(window.getElementId())) {
				continue;
			}
			MMenu current = window.getMainMenu();
			if(plantMenu != null && current != plantMenu) {
				try {
					window.setMainMenu(plantMenu);
					current = plantMenu;
				} catch(RuntimeException | LinkageError e) {
					// older E4 / already the window menu
				}
			}
			forceShowChrome(current);
			if(current instanceof MElementContainer<?> container) {
				List<?> children = container.getChildren();
				if(children != null) {
					for(Object child : children) {
						if(child instanceof MUIElement element && BaijiuShellChrome.isPlantWindowChrome(element.getElementId())) {
							forceShowChrome(element);
						}
					}
				}
			}
		}
	}

	private static void showTopTrimBars(MApplication application, EModelService modelService) {

		List<MWindow> windows = application.getChildren();
		if(windows == null) {
			return;
		}
		for(MWindow window : windows) {
			if(!(window instanceof MTrimmedWindow trimmed) || BaijiuShellChrome.GC_WINDOW_ID.equals(window.getElementId())) {
				continue;
			}
			List<MTrimBar> bars = trimmed.getTrimBars();
			if(bars == null) {
				continue;
			}
			for(MTrimBar bar : bars) {
				if(bar == null) {
					continue;
				}
				if(isTopPlantTrim(bar)) {
					forceShowChrome(bar);
				}
			}
		}
	}

	private static boolean isTopPlantTrim(MTrimBar bar) {

		String id = bar.getElementId();
		if(BaijiuShellChrome.shouldHide(id)) {
			return false;
		}
		if(BaijiuShellChrome.isPlantWindowChrome(id)) {
			return true;
		}
		if(!(bar instanceof MElementContainer<?> container)) {
			try {
				return bar.getSide() == SideValue.TOP;
			} catch(RuntimeException | LinkageError e) {
				return false;
			}
		}
		List<?> children = container.getChildren();
		if(children != null) {
			for(Object child : children) {
				if(child instanceof MUIElement element && BaijiuShellChrome.PLANT_TOOLBAR_ID.equals(element.getElementId())) {
					return true;
				}
			}
		}
		try {
			return bar.getSide() == SideValue.TOP;
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
	}

	private static MMenu findMenu(EModelService modelService, MApplication application, String menuId) {

		MUIElement found = modelService.find(menuId, application);
		if(found instanceof MMenu menu) {
			return menu;
		}
		return null;
	}

	private static void addTag(MUIElement element, String tag) {

		if(element == null || tag == null || tag.isBlank()) {
			return;
		}
		try {
			List<String> tags = element.getTags();
			if(tags == null || tags.contains(tag)) {
				return;
			}
			tags.add(tag);
		} catch(RuntimeException | LinkageError e) {
			// immutable tag list
		}
	}

	private static void removeTag(MUIElement element, String tag) {

		if(element == null || tag == null || tag.isBlank()) {
			return;
		}
		try {
			List<String> tags = element.getTags();
			if(tags != null) {
				tags.remove(tag);
			}
		} catch(RuntimeException | LinkageError e) {
			// immutable tag list
		}
	}

	public static boolean forceCreateGui(MApplication application, EModelService modelService, String partId) {

		if(application == null || modelService == null || partId == null || partId.isBlank()) {
			return false;
		}
		MPart part = findPart(modelService, application, partId);
		if(part == null) {
			return false;
		}
		part.setVisible(true);
		part.setToBeRendered(true);
		BaijiuShellSelection.selectInParent(part);
		IPresentationEngine engine = presentationEngine(application, part);
		if(engine == null) {
			return false;
		}
		try {
			if(part.getWidget() instanceof Composite composite && !composite.isDisposed() && part.getObject() != null && composite.getChildren().length > 0) {
				return true;
			}
			Object created = engine.createGui(part);
			if(created instanceof Composite composite && !composite.isDisposed()) {
				composite.layout(true, true);
			}
			return part.getWidget() != null || part.getObject() != null;
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
	}

	static boolean forceCreateElement(MApplication application, EModelService modelService, MUIElement element) {

		if(application == null || modelService == null || element == null) {
			return false;
		}
		element.setVisible(true);
		element.setToBeRendered(true);
		if(!BaijiuShellChrome.shouldCreateGuiForPlantChrome(element.getElementId(), element.getWidget() != null)) {
			return element.getWidget() != null;
		}
		IPresentationEngine engine = presentationEngine(application, element instanceof MPart part ? part : null);
		if(engine == null) {
			engine = fromContext(windowContext(element));
		}
		if(engine == null) {
			return false;
		}
		try {
			Object created = engine.createGui(element);
			if(created instanceof Composite composite && !composite.isDisposed()) {
				composite.layout(true, true);
			}
			if(element instanceof MPlaceholder placeholder && placeholder.getRef() != null) {
				MUIElement ref = placeholder.getRef();
				ref.setVisible(true);
				ref.setToBeRendered(true);
				engine.createGui(ref);
			}
			return element.getWidget() != null || created != null;
		} catch(RuntimeException | LinkageError e) {
			return false;
		}
	}

	static void trySetCurSharedRef(MUIElement shared, MPlaceholder placeholder) {

		if(shared == null || placeholder == null) {
			return;
		}
		if(shared instanceof MPart part) {
			try {
				part.setCurSharedRef(placeholder);
				return;
			} catch(RuntimeException | LinkageError e) {
				// older E4 / Area
			}
		}
		try {
			java.lang.reflect.Method setter = shared.getClass().getMethod("setCurSharedRef", MPlaceholder.class);
			setter.invoke(shared, placeholder);
		} catch(RuntimeException | LinkageError | ReflectiveOperationException e) {
			// MArea has no curSharedRef; createGui on the placeholder is enough
		}
	}

	public static boolean showPart(MApplication application, EModelService modelService, EPartService partService, String partId) {

		return showPart(application, modelService, partService, partId, null);
	}

	public static boolean showPart(MApplication application, EModelService modelService, EPartService partService, String partId, String preferredPlaceholderId) {

		if(application == null || modelService == null || partId == null || partId.isBlank()) {
			return false;
		}
		if(BaijiuShellChrome.GC_HOME_PART_ID.equals(partId) || BaijiuShellChrome.GC_CONTROL_PART_ID.equals(partId)) {
			if(gcConsoleWindow(application, modelService) != null || modelService.find(BaijiuShellChrome.GC_HOME_PART_ID, application) != null) {
				setGcConsoleVisible(application, modelService, partService, true);
				return true;
			}
		}
		MPart part = findPart(modelService, application, partId);
		if(part == null) {
			return false;
		}
		MPlaceholder placeholder = findPlaceholder(modelService, application, part, partId, preferredPlaceholderId);
		if(placeholder == null && part.getParent() == null) {
			return false;
		}
		part.setVisible(true);
		part.setToBeRendered(true);
		if(placeholder != null) {
			if(placeholder.getRef() != part) {
				placeholder.setRef(part);
			}
			placeholder.setVisible(true);
			placeholder.setToBeRendered(true);
			if(hasHiddenResearchAncestor(placeholder)) {
				return false;
			}
			BaijiuShellSelection.selectInParent(placeholder);
			try {
				part.setCurSharedRef(placeholder);
			} catch(RuntimeException | LinkageError e) {
				// older E4: showPart below is enough
			}
		}
		if(hasHiddenResearchAncestor(part)) {
			return placeholder != null;
		}
		BaijiuShellSelection.selectInParent(part);
		if(partService != null && (placeholder == null || BaijiuShellSelection.canSelect(placeholder))) {
			try {
				partService.showPart(part, PartState.ACTIVATE);
				return true;
			} catch(RuntimeException | LinkageError e) {
				// stack selection above
			}
		}
		return placeholder != null || part.getParent() != null;
	}

	static MPart findPart(EModelService modelService, MApplication application, String partId) {

		MUIElement found = modelService.find(partId, application);
		if(found instanceof MPart part) {
			return part;
		}
		List<MPart> parts = modelService.findElements(application, partId, MPart.class, null);
		if(parts != null && !parts.isEmpty()) {
			return parts.get(0);
		}
		return null;
	}

	static MPlaceholder findPlaceholder(EModelService modelService, MApplication application, MPart part, String partId, String preferredPlaceholderId) {

		if(preferredPlaceholderId != null && !preferredPlaceholderId.isBlank()) {
			MUIElement preferred = modelService.find(preferredPlaceholderId, application);
			if(preferred instanceof MPlaceholder placeholder && !hasHiddenResearchAncestor(placeholder)) {
				return placeholder;
			}
		}
		if(part != null) {
			try {
				MPlaceholder current = part.getCurSharedRef();
				if(current != null && !hasHiddenResearchAncestor(current)) {
					return current;
				}
			} catch(RuntimeException | LinkageError e) {
				// older E4: ignore
			}
		}
		List<MPlaceholder> placeholders = modelService.findElements(application, null, MPlaceholder.class, null);
		if(placeholders == null) {
			return null;
		}
		for(MPlaceholder placeholder : placeholders) {
			if(placeholder == null || hasHiddenResearchAncestor(placeholder)) {
				continue;
			}
			if(part != null && placeholder.getRef() == part) {
				return placeholder;
			}
			if(placeholder.getRef() instanceof MPart ref && partId.equals(ref.getElementId())) {
				return placeholder;
			}
			if(partId.equals(placeholder.getElementId())) {
				return placeholder;
			}
		}
		return null;
	}

	/**
	 * ChemClipse {@code EditorSupport.MAP_FILE} is {@code "file"}. Branding
	 * does not Require-Bundle chemclipse.support. Skip leftover
	 * workbench.xmi CSD parts with no chromatogram so empty chart chrome is
	 * not embedded into 谱图/采集 on cold start.
	 */
	static boolean hasCsdInput(MPart part) {

		if(part == null) {
			return false;
		}
		Object object = part.getObject();
		if(object instanceof Map<?, ?> map) {
			Object file = map.get("file");
			if(file instanceof String path && !path.isBlank()) {
				return true;
			}
			if(file instanceof File) {
				return true;
			}
		} else if(object != null) {
			return true;
		}
		String label = part.getLabel();
		return label != null && label.contains("[CSD]");
	}

	private static IEclipseContext windowContext(MUIElement element) {

		MUIElement walk = element;
		while(walk != null) {
			if(walk instanceof MWindow window) {
				try {
					return window.getContext();
				} catch(RuntimeException | LinkageError e) {
					return null;
				}
			}
			try {
				walk = walk.getParent();
			} catch(RuntimeException | LinkageError e) {
				return null;
			}
		}
		return null;
	}

	private static IPresentationEngine presentationEngine(MApplication application, MPart part) {

		IPresentationEngine engine = fromContext(application == null ? null : application.getContext());
		if(engine == null && part != null) {
			engine = fromContext(part.getContext());
		}
		return engine;
	}

	private static IPresentationEngine fromContext(IEclipseContext context) {

		if(context == null) {
			return null;
		}
		try {
			return context.get(IPresentationEngine.class);
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
	}

	static MUIElement findPlantChromatogram(MApplication application, EModelService modelService) {

		MUIElement plant = modelService.find(BaijiuShellChrome.PERSPECTIVE_ID, application);
		if(plant != null) {
			MUIElement scoped = modelService.find(BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID, plant);
			if(scoped != null) {
				return scoped;
			}
		}
		return modelService.find(BaijiuShellChrome.CHROMATOGRAM_PLACEHOLDER_ID, application);
	}

	static MUIElement gcConsoleWindow(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return null;
		}
		return modelService.find(BaijiuShellChrome.GC_WINDOW_ID, application);
	}

	/**
	 * Never paint the E4 TrimmedWindow / GC Part inside the FID main shell.
	 * #45 left that window as an MDI/Part child; the operator UI is
	 * {@link BaijiuGcConsoleShell}.
	 */
	public static void suppressE4GcWindow(MApplication application, EModelService modelService) {

		if(application == null || modelService == null) {
			return;
		}
		MUIElement window = gcConsoleWindow(application, modelService);
		if(window instanceof org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow trimmed) {
			BaijiuShellModel.applyGcWindowBounds(trimmed);
			BaijiuShellModel.reparentToApplication(application, trimmed);
		}
		BaijiuShellModel.neverRenderGcWindow(window);
		BaijiuShellModel.disposeGcWindowWidget(window);
		BaijiuShellModel.neverRenderGcWindow(modelService.find(BaijiuShellChrome.GC_WINDOW_SASH_ID, application));
		BaijiuShellModel.neverRenderGcWindow(modelService.find(BaijiuShellChrome.GC_HOME_STACK_ID, application));
		BaijiuShellModel.neverRenderGcWindow(modelService.find(BaijiuShellChrome.GC_HOME_PART_ID, application));
		BaijiuShellModel.neverRenderGcWindow(modelService.find(BaijiuShellChrome.GC_CONTROL_PLACEHOLDER_ID, application));
		BaijiuShellModel.neverRenderGcWindow(modelService.find(BaijiuShellChrome.GC_PERSPECTIVE_PLACEHOLDER_ID, application));
		MUIElement plantSash = modelService.find(BaijiuShellChrome.PLANT_SASH_ID, application);
		MUIElement placeholder = modelService.find(BaijiuShellChrome.GC_CONTROL_PLACEHOLDER_ID, application);
		if(placeholder != null && plantSash != null && isAncestor(plantSash, placeholder)) {
			BaijiuShellSelection.deselectFromParent(placeholder);
			BaijiuShellModel.neverRenderGcWindow(placeholder);
		}
	}

	private static boolean isAncestor(MUIElement ancestor, MUIElement element) {

		MUIElement walk = element;
		while(walk != null) {
			if(walk == ancestor) {
				return true;
			}
			try {
				walk = walk.getParent();
			} catch(RuntimeException | LinkageError e) {
				return false;
			}
		}
		return false;
	}

	static void installGcOsWindowHideHook(MApplication application, EModelService modelService, EPartService partService) {

		BaijiuGcConsoleShell.setOnHide(() -> setGcConsoleVisible(application, modelService, partService, false));
	}

	static void bringGcWindowToFront(MUIElement window) {

		BaijiuShellModel.disposeGcWindowWidget(window);
		BaijiuGcConsoleShell.show();
	}

	static void hideGcWindowShell(MUIElement window) {

		BaijiuShellModel.disposeGcWindowWidget(window);
		BaijiuGcConsoleShell.hide();
	}

	static void installGcWindowCloseHandler(MApplication application, EModelService modelService, EPartService partService, MWindow window) {

		installGcOsWindowHideHook(application, modelService, partService);
		if(window == null) {
			return;
		}
		try {
			if(window.getContext() != null) {
				window.getContext().set(IWindowCloseHandler.class, closing -> {
					setGcConsoleVisible(application, modelService, partService, false);
					return false;
				});
			}
		} catch(RuntimeException | LinkageError e) {
			// older E4
		}
		BaijiuShellModel.disposeGcWindowWidget(window);
	}

	private static EPartService partService(MApplication application) {

		if(application == null || application.getContext() == null) {
			return null;
		}
		try {
			return application.getContext().get(EPartService.class);
		} catch(RuntimeException | LinkageError e) {
			return null;
		}
	}

	static boolean hasHiddenResearchAncestor(MUIElement element) {

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
}
