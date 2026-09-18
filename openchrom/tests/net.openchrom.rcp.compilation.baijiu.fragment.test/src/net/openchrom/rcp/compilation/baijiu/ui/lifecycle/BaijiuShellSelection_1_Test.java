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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.junit.jupiter.api.Test;

public class BaijiuShellSelection_1_Test {

	private static final String MALDI_PERSPECTIVE = "org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.maldi";
	private static final String MALDI_SASH = "org.eclipse.chemclipse.ux.extension.xxd.ui.perspective.maldi.partsashcontainer.0";

	@Test
	public void missingModelDoesNotThrow() {

		BaijiuShellSelection.selectInParent(null);
		BaijiuShellSelection.deselectFromParent(null);
		BaijiuShellSelection.reassignAwayFrom(null);
		BaijiuShellSelection.reassignAwayFrom(List.of());
		BaijiuShellSelection.clearHiddenSelections(null, null);
		BaijiuShellSelection.clearHiddenSelections((List<MUIElement>)null);
		BaijiuShellSelection.selectPlantHomeIfPresent(null, null);
		BaijiuShellSelection.rejectHiddenSelection(null, null);
		assertFalse(BaijiuShellSelection.canSelect(null));
		assertTrue(BaijiuShellSelection.isForbiddenSelection(BaijiuShellChrome.WELCOME_PERSPECTIVE_ID));
	}

	@Test
	public void doesNotSelectHiddenWelcomePerspective() {

		FakeWorkbench workbench = FakeWorkbench.plantWithWelcomeSelected();
		assertTrue(BaijiuShellChrome.shouldHide(BaijiuShellChrome.WELCOME_PERSPECTIVE_ID));
		assertTrue(BaijiuShellChrome.isHiddenResearchPerspective(BaijiuShellChrome.WELCOME_PERSPECTIVE_ID));
		assertFalse(BaijiuShellSelection.canSelect(workbench.welcome));
		assertTrue(BaijiuShellSelection.canSelect(workbench.plant));
		assertEquals(workbench.welcome, workbench.stackNode.selectedProxy());

		BaijiuShellSelection.reassignAwayFrom(List.of(workbench.welcome, workbench.welcomeView));
		assertEquals(workbench.plant, workbench.stackNode.selectedProxy());
		assertNotEquals(workbench.welcome, workbench.stackNode.selectedProxy());

		workbench.welcomeNode.visible = false;
		workbench.welcomeNode.rendered = false;
		workbench.welcomeViewNode.visible = false;
		workbench.welcomeViewNode.rendered = false;

		BaijiuShellSelection.clearHiddenSelections(workbench.all());
		assertEquals(workbench.plant, workbench.stackNode.selectedProxy());
		assertNotEquals(workbench.welcome, workbench.stackNode.selectedProxy());

		BaijiuShellSelection.selectInParent(workbench.welcome);
		assertFalse(workbench.welcomeNode.visible);
		assertNotEquals(workbench.welcome, workbench.stackNode.selectedProxy());
		assertEquals(workbench.plant, workbench.stackNode.selectedProxy());

		BaijiuShellSelection.rejectHiddenSelection(workbench.stack, workbench.welcome);
		assertEquals(workbench.plant, workbench.stackNode.selectedProxy());
		assertNotEquals(workbench.welcome, workbench.stackNode.selectedProxy());
	}

	@Test
	public void doesNotSelectHiddenMaldiSash() {

		FakeWorkbench workbench = FakeWorkbench.plantWithMaldiSelected();
		assertTrue(BaijiuShellChrome.shouldHide(MALDI_SASH));
		assertTrue(BaijiuShellChrome.shouldHide(MALDI_PERSPECTIVE));
		assertFalse(BaijiuShellSelection.canSelect(workbench.maldiSash));
		assertTrue(BaijiuShellSelection.canSelect(workbench.sequence));

		BaijiuShellSelection.reassignAwayFrom(List.of(workbench.maldi, workbench.maldiSash, workbench.maldiPart));
		assertEquals(workbench.plant, workbench.stackNode.selectedProxy());

		workbench.maldiNode.visible = false;
		workbench.maldiNode.rendered = false;
		workbench.maldiSashNode.visible = false;
		workbench.maldiSashNode.rendered = false;
		workbench.maldiPartNode.visible = false;
		workbench.maldiPartNode.rendered = false;

		BaijiuShellSelection.clearHiddenSelections(workbench.all());
		assertEquals(workbench.plant, workbench.stackNode.selectedProxy());
		assertNotEquals(workbench.maldiSash, workbench.stackNode.selectedProxy());
		assertNotEquals(workbench.maldi, workbench.stackNode.selectedProxy());

		BaijiuShellSelection.selectInParent(workbench.maldiSash);
		assertFalse(workbench.maldiSashNode.visible);
		assertNotEquals(workbench.maldiSash, workbench.stackNode.selectedProxy());
		assertNotEquals(workbench.maldi, workbench.stackNode.selectedProxy());

		BaijiuShellSelection.selectInParent(workbench.sequence);
		assertEquals(workbench.sequence, workbench.workflowNode.selectedProxy());
		assertEquals(workbench.workflow, workbench.plantTopNode.selectedProxy());
		assertEquals(workbench.plantTop, workbench.plantSashNode.selectedProxy());
		assertEquals(workbench.plant, workbench.stackNode.selectedProxy());
		assertNotEquals(workbench.maldiSash, workbench.plantSashNode.selectedProxy());
		assertFalse(workbench.maldiSashNode.visible);
	}

	@Test
	public void gcToggleSelectsVisibleWorkflowNotHiddenConsole() {

		FakeWorkbench workbench = FakeWorkbench.plantWithMaldiSelected();
		workbench.plantTopNode.selected = workbench.gcStackNode;
		workbench.gcStackNode.visible = false;
		workbench.gcStackNode.tags.add(BaijiuShellChrome.GC_CONSOLE_HIDDEN_TAG);

		BaijiuShellSelection.clearHiddenSelections(workbench.all());
		assertEquals(workbench.workflow, workbench.plantTopNode.selectedProxy());
		assertNotEquals(workbench.gcStack, workbench.plantTopNode.selectedProxy());

		BaijiuShellSelection.selectInParent(workbench.gcPart);
		assertEquals(workbench.workflow, workbench.plantTopNode.selectedProxy());
	}

	private static final class FakeWorkbench {

		private final Map<FakeNode, MElementContainer<MUIElement>> proxies = new IdentityHashMap<>();
		private final Map<Object, FakeNode> nodes = new IdentityHashMap<>();

		final FakeNode windowNode;
		final FakeNode stackNode;
		final FakeNode plantNode;
		final FakeNode plantSashNode;
		final FakeNode chromStackNode;
		final FakeNode plantTopNode;
		final FakeNode gcStackNode;
		final FakeNode gcPartNode;
		final FakeNode workflowNode;
		final FakeNode sequenceNode;
		final FakeNode maldiNode;
		final FakeNode maldiSashNode;
		final FakeNode maldiPartNode;
		final FakeNode welcomeNode;
		final FakeNode welcomeViewNode;

		final MUIElement window;
		final MUIElement stack;
		final MUIElement plant;
		final MUIElement plantSash;
		final MUIElement chromStack;
		final MUIElement plantTop;
		final MUIElement gcStack;
		final MUIElement gcPart;
		final MUIElement workflow;
		final MUIElement sequence;
		final MUIElement maldi;
		final MUIElement maldiSash;
		final MUIElement maldiPart;
		final MUIElement welcome;
		final MUIElement welcomeView;

		private FakeWorkbench() {

			windowNode = node(BaijiuShellChrome.MAIN_WINDOW_ID);
			stackNode = child(windowNode, BaijiuShellChrome.PERSPECTIVE_STACK_ID);
			plantNode = child(stackNode, BaijiuShellChrome.PERSPECTIVE_ID);
			plantSashNode = child(plantNode, BaijiuShellChrome.PLANT_SASH_ID);
			chromStackNode = child(plantSashNode, BaijiuShellChrome.CHROMATOGRAM_STACK_ID);
			plantTopNode = child(plantSashNode, BaijiuShellChrome.PLANT_TOP_SASH_ID);
			gcStackNode = child(plantTopNode, BaijiuShellChrome.GC_HOME_STACK_ID);
			gcPartNode = child(gcStackNode, BaijiuShellChrome.GC_HOME_PART_ID);
			workflowNode = child(plantTopNode, BaijiuShellChrome.WORKFLOW_STACK_ID);
			sequenceNode = child(workflowNode, BaijiuShellChrome.SEQUENCE_HOME_PART_ID);
			maldiNode = child(stackNode, MALDI_PERSPECTIVE);
			maldiSashNode = child(maldiNode, MALDI_SASH);
			maldiPartNode = child(maldiSashNode, "org.eclipse.chemclipse.ux.extension.xxd.ui.part.massspectrum");
			welcomeNode = child(stackNode, BaijiuShellChrome.WELCOME_PERSPECTIVE_ID);
			welcomeViewNode = child(welcomeNode, "org.eclipse.chemclipse.ux.extension.ui.part.welcomeView");

			window = proxy(windowNode);
			stack = proxy(stackNode);
			plant = proxy(plantNode);
			plantSash = proxy(plantSashNode);
			chromStack = proxy(chromStackNode);
			plantTop = proxy(plantTopNode);
			gcStack = proxy(gcStackNode);
			gcPart = proxy(gcPartNode);
			workflow = proxy(workflowNode);
			sequence = proxy(sequenceNode);
			maldi = proxy(maldiNode);
			maldiSash = proxy(maldiSashNode);
			maldiPart = proxy(maldiPartNode);
			welcome = proxy(welcomeNode);
			welcomeView = proxy(welcomeViewNode);

			stackNode.selected = maldiNode;
			maldiNode.selected = maldiSashNode;
			maldiSashNode.selected = maldiPartNode;
			plantSashNode.selected = plantTopNode;
			plantTopNode.selected = gcStackNode;
			gcStackNode.selected = gcPartNode;
			workflowNode.selected = sequenceNode;
			windowNode.selected = stackNode;
			plantNode.selected = plantSashNode;
		}

		static FakeWorkbench plantWithMaldiSelected() {

			return new FakeWorkbench();
		}

		static FakeWorkbench plantWithWelcomeSelected() {

			FakeWorkbench workbench = new FakeWorkbench();
			workbench.stackNode.selected = workbench.welcomeNode;
			workbench.welcomeNode.selected = workbench.welcomeViewNode;
			return workbench;
		}

		List<MUIElement> all() {

			return List.of(window, stack, plant, plantSash, chromStack, plantTop, gcStack, gcPart, workflow, sequence, maldi, maldiSash, maldiPart, welcome, welcomeView);
		}

		private FakeNode node(String id) {

			FakeNode node = new FakeNode();
			node.id = id;
			return node;
		}

		private FakeNode child(FakeNode parent, String id) {

			FakeNode node = node(id);
			node.parent = parent;
			parent.children.add(node);
			return node;
		}

		@SuppressWarnings("unchecked")
		private MElementContainer<MUIElement> proxy(FakeNode node) {

			MElementContainer<MUIElement> existing = proxies.get(node);
			if(existing != null) {
				return existing;
			}
			MElementContainer<MUIElement> created = (MElementContainer<MUIElement>)Proxy.newProxyInstance(MElementContainer.class.getClassLoader(), new Class<?>[]{MElementContainer.class}, (proxy, method, args) -> {
				switch(method.getName()) {
					case "getElementId":
						return node.id;
					case "isVisible":
						return node.visible;
					case "isToBeRendered":
						return node.rendered;
					case "setVisible":
						node.visible = (Boolean)args[0];
						return null;
					case "setToBeRendered":
						node.rendered = (Boolean)args[0];
						return null;
					case "getParent":
						return node.parent == null ? null : proxy(node.parent);
					case "getSelectedElement":
						return node.selectedProxy();
					case "setSelectedElement":
						node.selected = args == null || args[0] == null ? null : nodes.get(args[0]);
						return null;
					case "getChildren":
						List<MUIElement> children = new ArrayList<>();
						for(FakeNode child : node.children) {
							children.add(proxy(child));
						}
						return children;
					case "getTags":
						return node.tags;
					case "equals":
						return proxy == args[0];
					case "hashCode":
						return System.identityHashCode(proxy);
					case "toString":
						return node.id;
					default:
						Class<?> type = method.getReturnType();
						if(type == boolean.class) {
							return false;
						}
						if(type == int.class || type == long.class) {
							return 0;
						}
						if(List.class.isAssignableFrom(type)) {
							return new ArrayList<>();
						}
						return null;
				}
			});
			proxies.put(node, created);
			nodes.put(created, node);
			return created;
		}

		private final class FakeNode {

			String id;
			boolean visible = true;
			boolean rendered = true;
			FakeNode parent;
			FakeNode selected;
			final List<FakeNode> children = new ArrayList<>();
			final List<String> tags = new ArrayList<>();

			MUIElement selectedProxy() {

				return selected == null ? null : proxy(selected);
			}
		}
	}
}
