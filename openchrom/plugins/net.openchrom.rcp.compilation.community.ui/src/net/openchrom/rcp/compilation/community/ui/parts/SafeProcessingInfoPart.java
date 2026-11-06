/*******************************************************************************
 * Copyright (c) 2026 Laiende.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.rcp.compilation.community.ui.parts;

import org.eclipse.chemclipse.processing.core.IMessageProvider;
import org.eclipse.chemclipse.processing.core.IProcessingMessage;
import org.eclipse.chemclipse.processing.core.MessageType;
import org.eclipse.chemclipse.processing.ui.support.ProcessingInfoUpdateNotifier;
import org.eclipse.chemclipse.processing.ui.swt.ProcessingInfoUI;
import org.eclipse.chemclipse.progress.core.InfoType;
import org.eclipse.chemclipse.progress.core.StatusLineLogger;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class SafeProcessingInfoPart {

	private static final String POPUP_MENU_ID = "#PopUpMenu";
	private static final String POPUP_MENU_POSTFIX = "PopUpMenu";

	@Inject
	private Composite parent;
	@Inject
	private ProcessingInfoUpdateNotifier updateNotifier;
	private ProcessingInfoUI processingInfoUI;

	@PostConstruct
	private void createControl() {

		parent.setLayout(new FillLayout());
		processingInfoUI = new ProcessingInfoUI(parent);
		TableViewer tableViewer = processingInfoUI.getTableViewer();
		tableViewer.getTable().addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {

				if(e.keyCode == 99 && e.stateMask == 262144) {
					processingInfoUI.copyToClipboard();
				}
			}
		});

		initContextMenu();
		processingInfoUI.update(updateNotifier.getProcessingInfo());
	}

	public void focus() {

		setFocus();
	}

	@Focus
	public void setFocus() {

		if(processingInfoUI != null && parent != null && !parent.isDisposed()) {
			processingInfoUI.setFocus();
		}
	}

	private void initContextMenu() {

		MenuManager menuManager = new MenuManager(POPUP_MENU_ID, getClass().getName() + POPUP_MENU_POSTFIX);
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(manager -> {
			IAction action = new Action() {

				@Override
				public void run() {

					super.run();
					if(processingInfoUI != null) {
						processingInfoUI.copyToClipboard();
					}
				}
			};
			action.setText("Copy selection to clipboard");
			manager.add(action);
		});
		TableViewer tableViewer = processingInfoUI.getTableViewer();
		Menu menu = menuManager.createContextMenu(tableViewer.getTable());
		tableViewer.getTable().setMenu(menu);
	}

	@Inject
	public void update(@Optional IMessageProvider data) {

		if(processingInfoUI != null) {
			processingInfoUI.update(data);
		}
		if(data != null) {
			for(IProcessingMessage message : data.getMessages()) {
				if(message.getMessageType() == MessageType.ERROR) {
					StatusLineLogger.setInfo(InfoType.ERROR_MESSAGE, message.getMessage());
				} else if(message.getMessageType() == MessageType.WARN) {
					StatusLineLogger.setInfo(InfoType.ERROR_MESSAGE, message.getMessage());
				} else {
					StatusLineLogger.setInfo(InfoType.MESSAGE, message.getMessage());
				}
			}
		}
	}

}
