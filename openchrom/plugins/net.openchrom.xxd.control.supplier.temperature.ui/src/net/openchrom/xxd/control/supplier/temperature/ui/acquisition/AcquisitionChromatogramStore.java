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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.eclipse.chemclipse.csd.converter.chromatogram.ChromatogramConverterCSD;
import org.eclipse.chemclipse.csd.converter.io.IChromatogramCSDWriter;
import org.eclipse.chemclipse.csd.converter.supplier.ocx.io.ChromatogramWriterCSD;
import org.eclipse.chemclipse.csd.model.core.IChromatogramCSD;
import org.eclipse.chemclipse.csd.model.core.IScanCSD;
import org.eclipse.chemclipse.csd.model.implementation.ChromatogramCSD;
import org.eclipse.chemclipse.csd.model.implementation.ScanCSD;
import org.eclipse.chemclipse.logging.core.Logger;
import org.eclipse.chemclipse.model.support.ChromatogramSupport;
import org.eclipse.chemclipse.processing.core.IProcessingMessage;
import org.eclipse.chemclipse.processing.core.IProcessingInfo;
import org.eclipse.chemclipse.xxd.converter.supplier.ocx.settings.Format;
import org.eclipse.core.runtime.NullProgressMonitor;

public final class AcquisitionChromatogramStore {

	private static final Logger logger = Logger.getLogger(AcquisitionChromatogramStore.class);
	public static final String DIRECTORY_PROPERTY = "net.openchrom.gcws.acquisition.dir";
	public static final String ACQUISITION_DIRECTORY = "Acquisitions";
	public static final String OCB_EXTENSION = ".ocb";
	private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
	private static final String DEFAULT_DATA_NAME = "GC-FID";
	private static final byte[] ZIP_MAGIC = {'P', 'K'};

	private AcquisitionChromatogramStore() {
	}

	public static Path resolveDirectory() {

		String override = System.getProperty(DIRECTORY_PROPERTY);
		if(override != null && !override.isBlank()) {
			return Path.of(override.trim());
		}
		return Path.of(System.getProperty("user.home"), "OpenChrom", ACQUISITION_DIRECTORY);
	}

	public static IChromatogramCSD createChromatogram() {

		ChromatogramCSD csd = new ChromatogramCSD();
		csd.setConverterId(Format.CONVERTER_ID_CHROMATOGRAM);
		csd.setSampleName(DEFAULT_DATA_NAME);
		csd.setDataName(DEFAULT_DATA_NAME);
		return csd;
	}

	public static IChromatogramCSD fromPoints(List<AcquisitionPoint> points) {

		IChromatogramCSD chromatogram = createChromatogram();
		if(points == null) {
			return chromatogram;
		}
		for(AcquisitionPoint point : points) {
			appendPoint(chromatogram, point);
		}
		ChromatogramSupport.calculateScanIntervalAndDelay(chromatogram);
		return chromatogram;
	}

	public static void appendPoint(IChromatogramCSD chromatogram, AcquisitionPoint point) {

		if(chromatogram == null || point == null) {
			return;
		}
		IScanCSD scan = new ScanCSD(normalizeSignal(point.signal()));
		scan.setRetentionTime(Math.max(0, point.retentionTimeMs()));
		chromatogram.addScan(scan);
	}

	public static float normalizeSignal(float signal) {

		if(!Float.isFinite(signal) || signal < 0f) {
			return 0f;
		}
		return signal;
	}

	public static AcquisitionSaveResult save(IChromatogramCSD chromatogram) {

		return save(chromatogram, resolveDirectory());
	}

	public static AcquisitionSaveResult save(IChromatogramCSD chromatogram, Path directory) {

		int points = countScans(chromatogram);
		if(chromatogram == null || points < 1) {
			return AcquisitionSaveResult.failed(AcquisitionMessages.noDataReason(true) + " / " + AcquisitionMessages.noDataReason(false), 0, null);
		}
		Path targetDirectory = directory == null ? resolveDirectory() : directory;
		try {
			Files.createDirectories(targetDirectory);
		} catch(IOException e) {
			logger.warn("Cannot create acquisition directory " + targetDirectory, e);
			return AcquisitionSaveResult.failed("Cannot create directory " + targetDirectory + ": " + detail(e), points, null);
		}
		File ocbFile = createFile(targetDirectory, chromatogram, OCB_EXTENSION);
		String ocbError = writeOcb(chromatogram, ocbFile);
		if(ocbError == null && isUsableFile(ocbFile)) {
			chromatogram.setFile(ocbFile);
			chromatogram.setConverterId(Format.CONVERTER_ID_CHROMATOGRAM);
			logger.info("Saved acquisition chromatogram to " + ocbFile.getAbsolutePath());
			return AcquisitionSaveResult.saved(ocbFile, AcquisitionSaveResult.Format.OCB, points);
		}
		File xyFile = createFile(targetDirectory, chromatogram, ChemclipseXyCsdFiles.EXTENSION);
		try {
			ChemclipseXyCsdFiles.write(xyFile.toPath(), chromatogram);
			chromatogram.setFile(xyFile);
			logger.warn("OCX .ocb export failed (" + ocbError + "); wrote ChemClipse CSD XY fallback " + xyFile.getAbsolutePath());
			return AcquisitionSaveResult.saved(xyFile, AcquisitionSaveResult.Format.XY, points);
		} catch(Exception xyError) {
			logger.warn("XY fallback export failed", xyError);
			File emergency = writeEmergencyCopy(targetDirectory, chromatogram, points);
			String reason = "OCX .ocb: " + (ocbError == null ? "empty file" : ocbError) + "; XY: " + detail(xyError);
			return AcquisitionSaveResult.failed(reason, points, emergency);
		}
	}

	private static String writeOcb(IChromatogramCSD chromatogram, File file) {

		try {
			ChromatogramSupport.calculateScanIntervalAndDelay(chromatogram);
			chromatogram.setConverterId(Format.CONVERTER_ID_CHROMATOGRAM);
		} catch(RuntimeException e) {
			logger.warn("Failed to prepare chromatogram metadata", e);
		}
		try {
			IChromatogramCSDWriter writer = new ChromatogramWriterCSD();
			writer.writeChromatogram(file, chromatogram, new NullProgressMonitor());
			if(isUsableOcb(file)) {
				return null;
			}
		} catch(Throwable e) {
			logger.warn("Direct ChromatogramWriterCSD failed", e);
			tryDelete(file);
			String converterError = writeOcbViaConverter(chromatogram, file);
			if(converterError == null && isUsableOcb(file)) {
				return null;
			}
			return "ChromatogramWriterCSD: " + detail(e) + "; converter: " + (converterError == null ? "empty or non-ZIP .ocb" : converterError);
		}
		tryDelete(file);
		String converterError = writeOcbViaConverter(chromatogram, file);
		if(converterError == null && isUsableOcb(file)) {
			return null;
		}
		if(!isUsableFile(file)) {
			return converterError == null ? "OCX writer produced an empty file" : converterError;
		}
		return converterError == null ? "OCX writer produced a non-ZIP .ocb" : converterError;
	}

	private static String writeOcbViaConverter(IChromatogramCSD chromatogram, File file) {

		try {
			IProcessingInfo<File> processingInfo = ChromatogramConverterCSD.getInstance().convert(file, chromatogram, Format.CONVERTER_ID_CHROMATOGRAM, new NullProgressMonitor());
			if(processingInfo == null) {
				return "ChromatogramConverterCSD returned no result";
			}
			if(processingInfo.hasErrorMessages()) {
				return joinMessages(processingInfo);
			}
			File saved = processingInfo.getProcessingResult();
			if(saved != null && isUsableOcb(saved)) {
				if(!saved.getAbsoluteFile().equals(file.getAbsoluteFile())) {
					Files.copy(saved.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				}
				return null;
			}
			if(isUsableOcb(file)) {
				return null;
			}
			return "CSD converter produced an empty or invalid .ocb";
		} catch(Throwable e) {
			logger.warn("ChromatogramConverterCSD export failed", e);
			return detail(e);
		}
	}

	private static File writeEmergencyCopy(Path directory, IChromatogramCSD chromatogram, int points) {

		File emergency = createFile(directory, chromatogram, ".acq.tsv");
		try {
			ChemclipseXyCsdFiles.write(emergency.toPath(), chromatogram);
			return emergency;
		} catch(Exception e) {
			logger.warn("Emergency TSV dump failed after " + points + " points", e);
			return null;
		}
	}

	private static File createFile(Path directory, IChromatogramCSD chromatogram, String extension) {

		String baseName = sanitizeFileName(chromatogram.getDataName());
		if(baseName.isBlank()) {
			baseName = DEFAULT_DATA_NAME;
		}
		String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT);
		String name = baseName + "_" + timestamp + extension;
		File file = directory.resolve(name).toFile();
		int suffix = 1;
		while(file.exists()) {
			file = directory.resolve(baseName + "_" + timestamp + "_" + suffix + extension).toFile();
			suffix++;
		}
		return file;
	}

	public static String sanitizeFileName(String text) {

		if(text == null) {
			return "";
		}
		return text.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
	}

	public static int countScans(IChromatogramCSD chromatogram) {

		if(chromatogram == null) {
			return 0;
		}
		synchronized(chromatogram) {
			return chromatogram.getNumberOfScans();
		}
	}

	private static boolean isUsableFile(File file) {

		return file != null && file.isFile() && file.length() > 0L;
	}

	private static boolean isUsableOcb(File file) {

		if(!isUsableFile(file)) {
			return false;
		}
		try(java.io.InputStream in = Files.newInputStream(file.toPath())) {
			byte[] header = in.readNBytes(2);
			if(header.length < 2) {
				return false;
			}
			return header[0] == ZIP_MAGIC[0] && header[1] == ZIP_MAGIC[1];
		} catch(IOException e) {
			return file.length() > 64L;
		}
	}

	private static void tryDelete(File file) {

		if(file != null && file.exists() && !file.delete()) {
			logger.warn("Could not delete incomplete acquisition file " + file.getAbsolutePath());
		}
	}

	private static String joinMessages(IProcessingInfo<?> info) {

		List<IProcessingMessage> messages = info.getMessages();
		if(messages == null || messages.isEmpty()) {
			return "CSD export failed";
		}
		StringBuilder text = new StringBuilder();
		for(IProcessingMessage message : messages) {
			if(text.length() > 0) {
				text.append("; ");
			}
			text.append(message.getMessage());
		}
		return text.length() == 0 ? "CSD export failed" : text.toString();
	}

	private static String detail(Throwable throwable) {

		if(throwable == null) {
			return "unknown error";
		}
		String message = throwable.getMessage();
		if(message == null || message.isBlank()) {
			return throwable.getClass().getSimpleName();
		}
		return message;
	}
}
