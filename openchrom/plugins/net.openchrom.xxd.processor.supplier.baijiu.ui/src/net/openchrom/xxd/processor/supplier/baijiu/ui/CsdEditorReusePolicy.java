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

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eclipse.chemclipse.model.core.IChromatogram;
import org.eclipse.chemclipse.model.selection.IChromatogramSelection;
import org.eclipse.chemclipse.ux.extension.ui.editors.IChromatogramEditor;

/**
 * One CSD tab per saved acquisition. Handoff and File→Open reuse an already
 * open live/file editor instead of calling ChemClipse {@code openEditor}.
 */
public final class CsdEditorReusePolicy {

	/**
	 * ChemClipse {@code EditorSupport.MAP_FILE}. Also used on MPart persisted
	 * state after Stop rebinds the live editor to the saved path.
	 */
	public static final String FILE_KEY = "file";
	/**
	 * Written on the live acquisition part after a successful save so handoff
	 * can match by path even if {@code part.getObject()} is the editor POJO.
	 */
	public static final String SAVED_FILE_KEY = "net.openchrom.gcws.savedFile";

	private CsdEditorReusePolicy() {
	}

	public static boolean shouldOpenChemClipseEditor(boolean matchingLiveEditor) {

		return !matchingLiveEditor;
	}

	public static boolean shouldCloseThenOpen(boolean matchingLiveEditor, boolean reused) {

		return matchingLiveEditor && !reused;
	}

	public static String savedEditorLabel(File file) {

		if(file == null) {
			return "";
		}
		String name = file.getName();
		int dot = name.lastIndexOf('.');
		String base = dot > 0 ? name.substring(0, dot) : name;
		return base + " [CSD]";
	}

	public static boolean partMatchesSavedFile(String label, Object partObject, String storedPath, File file) {

		if(file == null) {
			return false;
		}
		if(samePath(storedPath, file)) {
			return true;
		}
		File bound = fileOf(partObject);
		if(sameFile(bound, file)) {
			return true;
		}
		if(bound != null) {
			return false;
		}
		if(partObject instanceof Map) {
			return false;
		}
		if(labelMatchesSavedFile(label, file)) {
			return true;
		}
		return liveAcquisitionLabelMatchesSavedFile(label, file);
	}

	public static boolean labelMatchesSavedFile(String label, File file) {

		if(label == null || label.isBlank() || file == null) {
			return false;
		}
		String name = file.getName();
		if(label.contains(name)) {
			return true;
		}
		int dot = name.lastIndexOf('.');
		String base = dot > 0 ? name.substring(0, dot) : name;
		if(base.isBlank()) {
			return false;
		}
		String trimmed = label.trim();
		return trimmed.equals(base) || trimmed.startsWith(base + " ") || trimmed.startsWith(base + "[");
	}

	/**
	 * Live Start tab is {@code GC-FID [CSD]} before rebind; the saved file is
	 * {@code GC-FID_yyyyMMdd_HHmmss.ocb}. Match the unsuffixed live name as a
	 * prefix of the saved basename.
	 */
	public static boolean liveAcquisitionLabelMatchesSavedFile(String label, File file) {

		if(label == null || label.isBlank() || file == null) {
			return false;
		}
		String liveName = label.replace(" [CSD]", "").trim();
		if(liveName.isBlank() || liveName.contains("_")) {
			return false;
		}
		String name = file.getName();
		int dot = name.lastIndexOf('.');
		String base = dot > 0 ? name.substring(0, dot) : name;
		return base.startsWith(liveName + "_");
	}

	public static File fileOf(Object object) {

		if(object == null) {
			return null;
		}
		if(object instanceof Map<?, ?> map) {
			return asFile(map.get(FILE_KEY));
		}
		if(object instanceof IChromatogram chromatogram) {
			return chromatogram.getFile();
		}
		if(object instanceof IChromatogramEditor editor) {
			try {
				IChromatogramSelection selection = editor.getChromatogramSelection();
				if(selection != null && selection.getChromatogram() != null) {
					return selection.getChromatogram().getFile();
				}
			} catch(RuntimeException | LinkageError e) {
				return null;
			}
		}
		return asFile(invokeGetFile(object));
	}

	public static boolean sameFile(File a, File b) {

		if(a == null || b == null) {
			return false;
		}
		try {
			return a.getCanonicalFile().equals(b.getCanonicalFile());
		} catch(IOException e) {
			return a.getAbsoluteFile().equals(b.getAbsoluteFile());
		}
	}

	public static boolean samePath(String path, File file) {

		if(path == null || path.isBlank() || file == null) {
			return false;
		}
		return sameFile(new File(path), file);
	}

	private static File asFile(Object value) {

		if(value instanceof File file) {
			return file;
		}
		if(value instanceof String path && !path.isBlank()) {
			return new File(path);
		}
		return null;
	}

	private static Object invokeGetFile(Object object) {

		try {
			return object.getClass().getMethod("getFile").invoke(object);
		} catch(RuntimeException | ReflectiveOperationException e) {
			return null;
		}
	}
}
