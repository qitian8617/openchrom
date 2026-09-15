/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.processor.supplier.baijiu.ui.handlers;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.widgets.Shell;

import net.openchrom.xxd.processor.supplier.baijiu.ui.shell.BaijiuSequenceResultsShell;

public class OpenBaijiuSequenceResultsHandler {

	@Execute
	public void execute(@Active Shell shell) {

		BaijiuSequenceResultsShell.open(shell);
	}
}
