/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.engineering;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.chemclipse.logging.core.Logger;

import net.openchrom.xxd.control.supplier.temperature.ui.Activator;
import net.openchrom.xxd.control.supplier.temperature.ui.communication.ColumnOvenProgram;

public final class LiveTemperatureTrace {

	public static final String ZONE_INLET = "inlet1";
	public static final String ZONE_DETECTOR = "detector1";
	public static final String ZONE_OVEN = "oven";

	private static final Logger logger = Logger.getLogger(LiveTemperatureTrace.class);
	private static final LiveTemperatureTrace INSTANCE = new LiveTemperatureTrace();
	private static final int MAX_POINTS = 5400;
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	public record Point(LocalDateTime time, String zone, float setpoint, float actual, int duty, boolean valid) {
	}

	private final List<Point> points = new ArrayList<>();
	private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
	private final StringBuilder csvBuffer = new StringBuilder(8192);
	private boolean recording;
	private boolean temperatureControlRunning;
	private Path csvPath;
	private String statusText;
	private volatile Boolean ovenFan;
	private volatile Boolean ovenVent;

	private LiveTemperatureTrace() {

	}

	public static LiveTemperatureTrace getInstance() {

		return INSTANCE;
	}

	public void addListener(Runnable listener) {

		if(listener != null) {
			listeners.add(listener);
		}
	}

	public void removeListener(Runnable listener) {

		listeners.remove(listener);
	}

	public synchronized List<Point> points() {

		return List.copyOf(points);
	}

	public Boolean getOvenFan() {

		return ovenFan;
	}

	public Boolean getOvenVent() {

		return ovenVent;
	}

	public void setOvenSignals(Boolean fan, Boolean vent) {

		ovenFan = fan;
		ovenVent = vent;
		notifyListeners();
	}

	public void setTemperatureControlRunning(boolean running) {

		synchronized(this) {
			if(temperatureControlRunning == running) {
				return;
			}
			temperatureControlRunning = running;
		}
		notifyListeners();
	}

	public synchronized boolean isTemperatureControlRunning() {

		return temperatureControlRunning;
	}

	public synchronized boolean isRecording() {

		return recording;
	}

	public synchronized String statusText() {

		return statusText;
	}

	public void append(String zone, float setpoint, float actual, int duty, boolean valid) {

		if(!Float.isFinite(actual)) {
			return;
		}
		Point point = new Point(LocalDateTime.now(), zone, setpoint, actual, duty, valid);
		synchronized(this) {
			points.add(point);
			if(points.size() > MAX_POINTS) {
				points.remove(0);
			}
			if(recording) {
				appendCsv(point);
			}
		}
		notifyListeners();
	}

	public void clear() {

		synchronized(this) {
			points.clear();
		}
		notifyListeners();
	}

	public void resetForTemperatureControl(boolean chinese) throws IOException {

		clear();
		if(isRecording()) {
			stopRecording(chinese ? "上一轮记录已结束，开始新的三路记录" : "Previous log closed; starting a new 3-zone log", chinese);
		}
		startRecording(chinese);
	}

	public void startRecording(boolean chinese) throws IOException {

		synchronized(this) {
			if(recording) {
				return;
			}
			csvPath = createLogPath("temp-trend");
			csvBuffer.setLength(0);
			csvBuffer.append("timestamp,zone,setpoint,actual,duty,valid\n");
			recording = true;
			statusText = (chinese ? "记录中(三路): " : "Logging (3 zones): ") + csvPath.toAbsolutePath();
		}
		notifyListeners();
	}

	public void stopRecording(String summary, boolean chinese) throws IOException {

		IOException failure = null;
		synchronized(this) {
			if(!recording) {
				return;
			}
			try {
				flushCsv();
			} catch(IOException ex) {
				failure = ex;
			}
			recording = false;
			statusText = summary == null
					? ((chinese ? "记录已停止: " : "Logging stopped: ") + csvPath)
					: summary + "\n" + (chinese ? "文件: " : "File: ") + csvPath;
		}
		notifyListeners();
		if(failure != null) {
			throw failure;
		}
	}

	private void appendCsv(Point point) {

		csvBuffer.append(point.time()).append(',')
				.append(point.zone()).append(',')
				.append(ColumnOvenProgram.formatNumber(point.setpoint())).append(',')
				.append(ColumnOvenProgram.formatNumber(point.actual())).append(',')
				.append(point.duty()).append(',')
				.append(point.valid() ? 1 : 0).append('\n');
		if(csvBuffer.length() >= 4096) {
			try {
				flushCsv();
			} catch(IOException ex) {
				logger.warn("Failed to flush temperature trend CSV", ex);
			}
		}
	}

	private void flushCsv() throws IOException {

		if(csvPath == null || csvBuffer.length() == 0) {
			return;
		}
		Files.writeString(csvPath, csvBuffer.toString(), StandardCharsets.UTF_8, Files.exists(csvPath) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
		csvBuffer.setLength(0);
	}

	private static Path createLogPath(String prefix) throws IOException {

		Path base;
		if(Activator.getDefault() != null) {
			base = Activator.getDefault().getStateLocation().append("temp-traces").toFile().toPath();
		} else {
			base = Path.of(System.getProperty("user.home"), "OpenChrom", "temp-traces");
		}
		Files.createDirectories(base);
		return base.resolve(prefix + "-" + FILE_TS.format(LocalDateTime.now()) + ".csv");
	}

	private void notifyListeners() {

		for(Runnable listener : listeners) {
			listener.run();
		}
	}
}
