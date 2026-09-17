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

import org.junit.jupiter.api.Test;

public class BaijiuShellAddon_1_Test {

	@Test
	public void missingModelDoesNotThrow() {

		BaijiuShellAddon.applyChrome(null, null);
		BaijiuShellAddon.selectBaijiuPerspective(null, null);
		BaijiuShellAddon.revealPlantParts(null, null);
		BaijiuShellAddon.hideTopWindowMenus(null, null);
		BaijiuShellParts.forceCreatePlantHomeGuis(null, null);
	}
}
