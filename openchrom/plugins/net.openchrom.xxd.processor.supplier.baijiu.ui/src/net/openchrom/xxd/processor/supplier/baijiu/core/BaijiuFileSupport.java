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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class BaijiuFileSupport {

	private BaijiuFileSupport() {
	}

	public static void writeUtf8(Path file, String text, boolean excelBom) throws IOException {

		if(file == null) {
			throw new IOException("Output file is null.");
		}
		Path target = file.toAbsolutePath();
		Path parent = target.getParent();
		if(parent == null) {
			throw new IOException("Output directory is missing: " + file);
		}
		Files.createDirectories(parent);
		Path temporary = Files.createTempFile(parent, ".baijiu-", ".tmp");
		try {
			try(OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(temporary), StandardCharsets.UTF_8)) {
				if(excelBom) {
					writer.write('\uFEFF');
				}
				writer.write(text == null ? "" : text);
			}
			try {
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch(AtomicMoveNotSupportedException e) {
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	public static String safeCsvText(String value) {

		String text = value == null ? "" : value;
		int index = 0;
		while(index < text.length() && Character.isWhitespace(text.charAt(index))) {
			index++;
		}
		if(index < text.length()) {
			char first = text.charAt(index);
			if(first == '=' || first == '+' || first == '-' || first == '@') {
				return "'" + text;
			}
		}
		return text;
	}
}
