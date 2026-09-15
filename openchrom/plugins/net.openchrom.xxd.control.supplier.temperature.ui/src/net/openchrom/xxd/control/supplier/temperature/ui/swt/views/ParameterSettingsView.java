/*******************************************************************************
 * Copyright (c) 2026 OpenChrom.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.openchrom.xxd.control.supplier.temperature.ui.swt.views;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.chemclipse.logging.core.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import net.openchrom.xxd.control.supplier.temperature.ui.Activator;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.LanguageListener;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiColors;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.UiStyles;
import net.openchrom.xxd.control.supplier.temperature.ui.swt.WidgetFactory;

public class ParameterSettingsView extends Composite implements LanguageListener {

	private static final Logger logger = Logger.getLogger(ParameterSettingsView.class);

	private Label spliceLabel;
	private Text spliceText;
	private Button spliceSaveButton;
	private Button channelSaveButton;
	private Label statusLabel;
	private final Label[] channelLabels = new Label[ParameterSettingsStore.CHANNEL_COUNT];
	private final Combo[] channelCombos = new Combo[ParameterSettingsStore.CHANNEL_COUNT];
	private boolean chinese = true;

	public ParameterSettingsView(Composite parent, int style) {

		super(parent, style);
		setBackground(UiStyles.color(getDisplay(), UiColors.BACKGROUND));
		GridLayout layout = new GridLayout(1, false);
		layout.verticalSpacing = 8;
		setLayout(layout);

		Composite spliceCard = WidgetFactory.createCard(this);
		spliceCard.setLayout(new GridLayout(3, false));

		spliceLabel = new Label(spliceCard, SWT.NONE);
		spliceLabel.setBackground(spliceCard.getBackground());
		spliceLabel.setText(chinese ? "\u91C7\u6837\u65F6\u95F4(\u79D2):" : "Sampling Time (sec):");
		spliceText = new Text(spliceCard, SWT.BORDER);
		spliceText.setText("0");
		spliceText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		spliceSaveButton = WidgetFactory.createPrimaryButton(spliceCard, chinese ? "\u4FDD\u5B58" : "Save");

		Composite channelCard = WidgetFactory.createCard(this);
		channelCard.setLayout(new GridLayout(2, false));

		for(int i = 0; i < ParameterSettingsStore.CHANNEL_COUNT; i++) {
			channelLabels[i] = new Label(channelCard, SWT.NONE);
			channelLabels[i].setBackground(channelCard.getBackground());
			channelLabels[i].setText(channelLabelText(i));

			Combo combo = new Combo(channelCard, SWT.READ_ONLY | SWT.DROP_DOWN);
			combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			combo.setData("laiende.skip.localize", Boolean.TRUE);
			fillComboItems(combo);
			combo.select(ParameterSettingsStore.indexOfOptionId(ParameterSettingsStore.DEFAULT_CHANNEL_IDS[i]));
			combo.setData("channelIndex", Integer.valueOf(i));
			channelCombos[i] = combo;
		}

		channelSaveButton = WidgetFactory.createPrimaryButton(channelCard, chinese ? "\u4FDD\u5B58" : "Save");
		channelSaveButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1));

		statusLabel = new Label(this, SWT.WRAP);
		statusLabel.setBackground(getBackground());
		statusLabel.setForeground(UiStyles.color(getDisplay(), UiColors.TEXT_SECONDARY));
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		SelectionAdapter saveListener = new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				saveConfiguration();
			}
		};
		spliceSaveButton.addSelectionListener(saveListener);
		channelSaveButton.addSelectionListener(saveListener);

		loadConfiguration();
	}

	/**
	 * Channel ids currently shown on this page (including unsaved combo changes).
	 */
	String[] currentChannelIds() {

		String[] ids = new String[ParameterSettingsStore.CHANNEL_COUNT];
		for(int i = 0; i < ParameterSettingsStore.CHANNEL_COUNT; i++) {
			String id = selectedOptionId(channelCombos[i]);
			ids[i] = id != null ? id : ParameterSettingsStore.DEFAULT_CHANNEL_IDS[i];
		}
		return ids;
	}

	@Override
	public void onLanguageChanged(boolean chinese) {

		this.chinese = chinese;
		spliceLabel.setText(chinese ? "\u91C7\u6837\u65F6\u95F4(\u79D2):" : "Sampling Time (sec):");
		spliceSaveButton.setText(chinese ? "\u4FDD\u5B58" : "Save");
		channelSaveButton.setText(chinese ? "\u4FDD\u5B58" : "Save");
		for(int i = 0; i < ParameterSettingsStore.CHANNEL_COUNT; i++) {
			channelLabels[i].setText(channelLabelText(i));
			String selectedId = selectedOptionId(channelCombos[i]);
			fillComboItems(channelCombos[i]);
			channelCombos[i].select(ParameterSettingsStore.indexOfOptionId(selectedId));
		}
	}

	private void fillComboItems(Combo combo) {

		ParameterSettingsStore.ChannelOption[] options = ParameterSettingsStore.CHANNEL_OPTIONS;
		String[] items = new String[options.length];
		for(int i = 0; i < options.length; i++) {
			items[i] = options[i].label(chinese);
		}
		combo.setItems(items);
	}

	private String channelLabelText(int index) {

		return chinese ? "\u901A\u9053" + (index + 1) : "Channel " + (index + 1);
	}

	private void saveConfiguration() {

		float samplingTime;
		try {
			samplingTime = parseSamplingTime(spliceText.getText());
		} catch(IllegalArgumentException ex) {
			showMessage(SWT.ICON_ERROR, chinese ? "\u4FDD\u5B58\u5931\u8D25" : "Save failed", ex.getMessage());
			return;
		}

		List<String> channelIds = new ArrayList<>(ParameterSettingsStore.CHANNEL_COUNT);
		for(Combo combo : channelCombos) {
			String id = selectedOptionId(combo);
			if(id == null) {
				showMessage(SWT.ICON_ERROR, chinese ? "\u4FDD\u5B58\u5931\u8D25" : "Save failed", chinese ? "\u8BF7\u4E3A\u6BCF\u4E2A\u901A\u9053\u9009\u62E9\u4E00\u4E2A\u9009\u9879" : "Select an option for every channel");
				return;
			}
			channelIds.add(id);
		}

		Path file = ParameterSettingsStore.resolveConfigFile();
		try {
			ParameterSettingsStore.save(samplingTime, channelIds);
			statusLabel.setText((chinese ? "\u5DF2\u4FDD\u5B58: " : "Saved: ") + file.toAbsolutePath());
			showMessage(SWT.ICON_INFORMATION, chinese ? "\u4FDD\u5B58\u6210\u529F" : "Saved", (chinese ? "\u914D\u7F6E\u5DF2\u5199\u5165:\n" : "Configuration written to:\n") + file.toAbsolutePath());
		} catch(IOException e) {
			logger.warn("Failed to save parameter settings", e);
			showMessage(SWT.ICON_ERROR, chinese ? "\u4FDD\u5B58\u5931\u8D25" : "Save failed", e.getMessage());
		}
	}

	private void loadConfiguration() {

		Path file = ParameterSettingsStore.resolveConfigFile();
		if(!Files.isRegularFile(file)) {
			statusLabel.setText(chinese ? "\u5C1A\u672A\u4FDD\u5B58\u914D\u7F6E\u6587\u4EF6" : "No saved configuration yet");
			return;
		}
		try {
			Float sampling = ParameterSettingsStore.loadSamplingTimeSec();
			if(sampling != null) {
				spliceText.setText(ParameterSettingsStore.formatNumber(sampling.floatValue()));
			}
			ParameterSettingsStore.ChannelOption[] channels = ParameterSettingsStore.loadChannelOptions();
			for(int i = 0; i < ParameterSettingsStore.CHANNEL_COUNT; i++) {
				channelCombos[i].select(ParameterSettingsStore.indexOfOptionId(channels[i].id));
			}
			statusLabel.setText((chinese ? "\u5DF2\u52A0\u8F7D: " : "Loaded: ") + file.toAbsolutePath());
		} catch(RuntimeException e) {
			logger.warn("Failed to load parameter settings from " + file, e);
			statusLabel.setText(chinese ? "\u914D\u7F6E\u6587\u4EF6\u8BFB\u53D6\u5931\u8D25" : "Failed to load configuration file");
		}
	}

	private void showMessage(int style, String title, String message) {

		MessageBox dialog = new MessageBox(getShell(), style | SWT.OK);
		dialog.setText(title);
		dialog.setMessage(message == null ? "" : message);
		dialog.open();
	}

	private static float parseSamplingTime(String text) {

		String raw = text == null ? "" : text.trim();
		if(raw.isEmpty()) {
			throw new IllegalArgumentException("Sampling time is empty");
		}
		try {
			float value = Float.parseFloat(raw);
			if(value < 0f) {
				throw new IllegalArgumentException("Sampling time must be >= 0");
			}
			return value;
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException("Invalid sampling time: " + raw, e);
		}
	}

	private static String selectedOptionId(Combo combo) {

		int index = combo.getSelectionIndex();
		ParameterSettingsStore.ChannelOption[] options = ParameterSettingsStore.CHANNEL_OPTIONS;
		if(index < 0 || index >= options.length) {
			return null;
		}
		return options[index].id;
	}
}

/**
 * Shared read/write for parameter-settings.json (same package so Eclipse resolves it).
 */
final class ParameterSettingsStore {

	private static final Logger STORE_LOGGER = Logger.getLogger(ParameterSettingsStore.class);

	static final String CONFIG_FILE_NAME = "parameter-settings.json";
	static final int CHANNEL_COUNT = 7;

	static final ParameterSettingsStore.ChannelOption[] CHANNEL_OPTIONS = {
			new ChannelOption("inlet1", "\u8FDB\u6837\u53E31", "Inlet 1"),
			new ChannelOption("inlet2", "\u8FDB\u6837\u53E32", "Inlet 2"),
			new ChannelOption("detector1", "\u68C0\u6D4B\u56681", "Detector 1"),
			new ChannelOption("detector2", "\u68C0\u6D4B\u56682", "Detector 2"),
			new ChannelOption("detector3", "\u68C0\u6D4B\u56683", "Detector 3"),
			new ChannelOption("detector4", "\u68C0\u6D4B\u56684", "Detector 4"),
			new ChannelOption("oven", "\u67F1\u7BB1", "Oven"),
			new ChannelOption("aux_oven", "\u5C3E\u7BB1", "Auxiliary oven"),
			new ChannelOption("spare", "\u5907\u7528", "Spare")
	};

	static final String[] DEFAULT_CHANNEL_IDS = {
			"inlet1", "inlet2", "detector1", "detector2", "oven", "aux_oven", "spare"
	};

	private ParameterSettingsStore() {
	}

	static Path resolveConfigFile() {
		Activator activator = Activator.getDefault();
		if(activator != null) {
			IPath state = activator.getStateLocation();
			if(state != null) {
				return state.append(CONFIG_FILE_NAME).toFile().toPath();
			}
		}
		return Path.of(System.getProperty("user.home"), ".openchrom", "temperature-control", CONFIG_FILE_NAME);
	}

	static ParameterSettingsStore.ChannelOption[] loadChannelOptions() {
		ParameterSettingsStore.ChannelOption[] result = new ChannelOption[CHANNEL_COUNT];
		Path file = resolveConfigFile();
		List<String> ids = defaultChannelIdsList();
		if(Files.isRegularFile(file)) {
			try {
				String json = Files.readString(file, StandardCharsets.UTF_8);
				List<String> parsed = parseChannelIds(json);
				if(!parsed.isEmpty()) {
					ids = parsed;
				}
			} catch(IOException | RuntimeException e) {
				STORE_LOGGER.warn("Failed to load " + file.toAbsolutePath() + ", using defaults", e);
			}
		}
		for(int i = 0; i < CHANNEL_COUNT; i++) {
			String id = i < ids.size() ? ids.get(i) : DEFAULT_CHANNEL_IDS[i];
			result[i] = optionById(id);
		}
		return result;
	}

	static void save(float samplingTimeSec, List<String> channelIds) throws IOException {
		Objects.requireNonNull(channelIds, "channelIds");
		Path file = resolveConfigFile();
		Files.createDirectories(file.getParent());
		Files.writeString(file, toJson(samplingTimeSec, channelIds), StandardCharsets.UTF_8);
		STORE_LOGGER.info("Parameter settings saved to " + file.toAbsolutePath());
	}

	static Float loadSamplingTimeSec() {
		Path file = resolveConfigFile();
		if(!Files.isRegularFile(file)) {
			return null;
		}
		try {
			return parseJsonFloat(Files.readString(file, StandardCharsets.UTF_8), "samplingTimeSec");
		} catch(IOException | RuntimeException e) {
			STORE_LOGGER.warn("Failed to read sampling time from " + file, e);
			return null;
		}
	}

	static ParameterSettingsStore.ChannelOption optionById(String id) {
		return CHANNEL_OPTIONS[indexOfOptionId(id)];
	}

	static int indexOfOptionId(String id) {
		if(id == null || id.isBlank()) {
			return 0;
		}
		for(int i = 0; i < CHANNEL_OPTIONS.length; i++) {
			if(CHANNEL_OPTIONS[i].id.equalsIgnoreCase(id)) {
				return i;
			}
		}
		return 0;
	}

	static String formatNumber(float value) {
		if(value == (long)value) {
			return Long.toString((long)value);
		}
		return String.format(Locale.US, "%s", Float.toString(value));
	}

	private static List<String> defaultChannelIdsList() {
		List<String> ids = new ArrayList<>(CHANNEL_COUNT);
		for(String id : DEFAULT_CHANNEL_IDS) {
			ids.add(id);
		}
		return ids;
	}

	private static String toJson(float samplingTimeSec, List<String> channelIds) {
		StringBuilder json = new StringBuilder(512);
		json.append("{\n");
		json.append("  \"samplingTimeSec\": ").append(formatNumber(samplingTimeSec)).append(",\n");
		json.append("  \"channels\": [\n");
		for(int i = 0; i < channelIds.size(); i++) {
			ParameterSettingsStore.ChannelOption option = optionById(channelIds.get(i));
			if(i > 0) {
				json.append(",\n");
			}
			json.append("    {\n");
			json.append("      \"channel\": ").append(i + 1).append(",\n");
			json.append("      \"id\": \"").append(option.id).append("\",\n");
			json.append("      \"zh\": \"").append(escapeJson(option.zh)).append("\",\n");
			json.append("      \"en\": \"").append(escapeJson(option.en)).append("\"\n");
			json.append("    }");
		}
		json.append("\n  ]\n");
		json.append("}\n");
		return json.toString();
	}

	private static Float parseJsonFloat(String json, String key) {
		String pattern = "\"" + key + "\"";
		int keyIdx = json.indexOf(pattern);
		if(keyIdx < 0) {
			return null;
		}
		int colon = json.indexOf(':', keyIdx + pattern.length());
		if(colon < 0) {
			return null;
		}
		int start = colon + 1;
		while(start < json.length() && Character.isWhitespace(json.charAt(start))) {
			start++;
		}
		int end = start;
		while(end < json.length()) {
			char c = json.charAt(end);
			if((c >= '0' && c <= '9') || c == '.' || c == '-' || c == '+' || c == 'e' || c == 'E') {
				end++;
			} else {
				break;
			}
		}
		if(end <= start) {
			return null;
		}
		return Float.valueOf(json.substring(start, end));
	}

	private static List<String> parseChannelIds(String json) {
		List<String> ids = new ArrayList<>(CHANNEL_COUNT);
		int channelsIdx = json.indexOf("\"channels\"");
		if(channelsIdx < 0) {
			return ids;
		}
		int arrayStart = json.indexOf('[', channelsIdx);
		int arrayEnd = json.indexOf(']', arrayStart);
		if(arrayStart < 0 || arrayEnd < 0) {
			return ids;
		}
		String body = json.substring(arrayStart + 1, arrayEnd);
		int cursor = 0;
		while(ids.size() < CHANNEL_COUNT) {
			int idKey = body.indexOf("\"id\"", cursor);
			if(idKey < 0) {
				break;
			}
			int colon = body.indexOf(':', idKey);
			int quote1 = body.indexOf('"', colon + 1);
			int quote2 = body.indexOf('"', quote1 + 1);
			if(colon < 0 || quote1 < 0 || quote2 < 0) {
				break;
			}
			ids.add(body.substring(quote1 + 1, quote2));
			cursor = quote2 + 1;
		}
		return ids;
	}

	private static String escapeJson(String text) {
		return Objects.requireNonNullElse(text, "").replace("\\", "\\\\").replace("\"", "\\\"");
	}

	static final class ChannelOption {
		final String id;
		final String zh;
		final String en;

		ChannelOption(String id, String zh, String en) {
			this.id = id;
			this.zh = zh;
			this.en = en;
		}

		String label(boolean chinese) {
			return chinese ? zh : en;
		}
	}
}

/**
 * Shared inlet1 / detector1 calibration targets (设置温度1) for main-page temp control.
 */
final class AuxTempSetpointStore {

	private static final Logger LOGGER = Logger.getLogger(AuxTempSetpointStore.class);
	private static final String CONFIG_FILE_NAME = "aux-temp-setpoints.json";

	private static volatile Float inletSetpointC;
	private static volatile Float detectorSetpointC;

	static {
		loadFromDisk();
	}

	private AuxTempSetpointStore() {
	}

	static Optional<Float> getInletSetpointC() {

		return Optional.ofNullable(inletSetpointC);
	}

	static Optional<Float> getDetectorSetpointC() {

		return Optional.ofNullable(detectorSetpointC);
	}

	static void putInletSetpointC(float setpointC) {

		inletSetpointC = clamp(setpointC);
		persist();
	}

	static void putDetectorSetpointC(float setpointC) {

		detectorSetpointC = clamp(setpointC);
		persist();
	}

	static void putBoth(float inletC, float detectorC) {

		inletSetpointC = clamp(inletC);
		detectorSetpointC = clamp(detectorC);
		persist();
	}

	private static float clamp(float value) {

		if(value < 0f) {
			return 0f;
		}
		if(value > 450f) {
			return 450f;
		}
		return value;
	}

	private static Path resolveAuxConfigFile() {

		Activator activator = Activator.getDefault();
		if(activator != null) {
			IPath state = activator.getStateLocation();
			if(state != null) {
				return state.append(CONFIG_FILE_NAME).toFile().toPath();
			}
		}
		return Path.of(System.getProperty("user.home"), ".openchrom", "temperature-control", CONFIG_FILE_NAME);
	}

	private static void loadFromDisk() {

		Path file = resolveAuxConfigFile();
		if(!Files.isRegularFile(file)) {
			return;
		}
		try {
			String json = Files.readString(file, StandardCharsets.UTF_8);
			Float inlet = parseFloatKey(json, "inlet1");
			Float detector = parseFloatKey(json, "detector1");
			if(inlet != null) {
				inletSetpointC = clamp(inlet);
			}
			if(detector != null) {
				detectorSetpointC = clamp(detector);
			}
		} catch(IOException | RuntimeException e) {
			LOGGER.warn("Failed to load " + file.toAbsolutePath(), e);
		}
	}

	private static void persist() {

		Path file = resolveAuxConfigFile();
		try {
			Files.createDirectories(file.getParent());
			StringBuilder json = new StringBuilder(128);
			json.append("{\n");
			if(inletSetpointC != null) {
				json.append("  \"inlet1\": ").append(ParameterSettingsStore.formatNumber(inletSetpointC.floatValue()));
			}
			if(detectorSetpointC != null) {
				if(inletSetpointC != null) {
					json.append(",\n");
				}
				json.append("  \"detector1\": ").append(ParameterSettingsStore.formatNumber(detectorSetpointC.floatValue()));
			}
			json.append("\n}\n");
			Files.writeString(file, json.toString(), StandardCharsets.UTF_8);
		} catch(IOException e) {
			LOGGER.warn("Failed to save " + file.toAbsolutePath(), e);
		}
	}

	private static Float parseFloatKey(String json, String key) {

		String pattern = "\"" + key + "\"";
		int keyIdx = json.indexOf(pattern);
		if(keyIdx < 0) {
			return null;
		}
		int colon = json.indexOf(':', keyIdx);
		if(colon < 0) {
			return null;
		}
		int i = colon + 1;
		while(i < json.length() && Character.isWhitespace(json.charAt(i))) {
			i++;
		}
		int start = i;
		while(i < json.length()) {
			char c = json.charAt(i);
			if((c >= '0' && c <= '9') || c == '.' || c == '-' || c == '+' || c == 'e' || c == 'E') {
				i++;
				continue;
			}
			break;
		}
		if(start >= i) {
			return null;
		}
		return Float.parseFloat(json.substring(start, i));
	}
}