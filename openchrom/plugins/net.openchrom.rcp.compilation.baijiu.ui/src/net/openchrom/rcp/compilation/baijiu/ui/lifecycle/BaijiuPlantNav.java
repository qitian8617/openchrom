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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Plant information architecture. Six primary navigation entries plus a
 * collapsed maintenance control. Maintenance opens 工程师/维护模式: temperature
 * program, gas, FID debug, acquisition diagnostics, method editing, and
 * system parameters. Daily method-pack stays on {@link Section#METHOD}.
 * About and reset layout stay as secondary system actions. The chromatogram
 * host is not a navigation target, so switching entries does not unload it.
 * <p>
 * Console page keys match {@code TemperatureControlPanel} engineer pages
 * ({@code COLUMN_OVEN}, {@code DETECTOR}, {@code AUX_PID}, {@code PARAMETERS}).
 * Branding does not depend on that bundle; it passes the key through OSGi.
 */
public final class BaijiuPlantNav {

	public static final String LATER = "后续版本提供";
	public static final String ENGINEER_MODE = "工程师/维护模式";
	/** Column-oven program page on the GC console. */
	public static final String PAGE_OVEN = "COLUMN_OVEN";
	/** Detector page: gas flow, valves, and H₂/air pressure. */
	public static final String PAGE_GAS = "DETECTOR";
	/** Inlet/detector PID debug page. Not on the operator console bar. */
	public static final String PAGE_FID = "AUX_PID";
	/** Settings page, parameter tab. */
	public static final String PAGE_PARAMETERS = "PARAMETERS";
	/** Dedicated engineer page. Does not reopen the operator remote panel. */
	public static final String PAGE_DEVICE = "ENGINEER_DEVICE";
	public static final String PAGE_TEMP = "ENGINEER_TEMP";
	public static final String PAGE_GAS_ENG = "ENGINEER_GAS";
	public static final String PAGE_LINK = "ENGINEER_LINK";
	public static final String ACTION_ABOUT = "about";
	public static final String ACTION_RESET = "reset";
	public static final String ACTION_SELECT_VIEW = "selectView";
	public static final String ACTION_THEME = "theme";

	private static final AtomicReference<Section> ACTIVE = new AtomicReference<>(Section.ACQUIRE);
	private static final AtomicReference<Target> ACTIVE_TARGET = new AtomicReference<>();
	private static final AtomicReference<TaskOpener> OPENER = new AtomicReference<>();
	private static final AtomicReference<Runnable> SECTION_WATCH = new AtomicReference<>();
	private static final Map<Section, List<Target>> TARGETS = targetsBySection();
	private static final Map<String, Section> LEGACY = legacyLabels();

	private BaijiuPlantNav() {

	}

	public enum Section {

		ACQUIRE("连接与采集", false), //
		METHOD("方法包", false), //
		CALIBRATE("校正", false), //
		SEQUENCE("序列与批跑", false), //
		QUANT("定量结果 / QC", false), //
		REPORT("报告", false), //
		MAINTENANCE("维护", true);

		private final String label;
		private final boolean maintenance;

		Section(String label, boolean maintenance) {

			this.label = label;
			this.maintenance = maintenance;
		}

		public String label() {

			return label;
		}

		public boolean maintenance() {

			return maintenance;
		}
	}

	/**
	 * One existing part, or a maintenance action that does not open a part.
	 * {@code stub} pages paint a gray note instead of a new workflow.
	 */
	public record Target(String partId, String label, boolean stub, String actionKey) {

		public boolean opensPart() {

			return partId != null && !partId.isBlank();
		}

		/**
		 * About / reset layout. Shown under the engineer tools, not as the
		 * maintenance story.
		 */
		public boolean secondary() {

			return ACTION_ABOUT.equals(actionKey) || ACTION_RESET.equals(actionKey);
		}
	}

	public static List<Section> primarySections() {

		return List.of( //
				Section.ACQUIRE, //
				Section.METHOD, //
				Section.CALIBRATE, //
				Section.SEQUENCE, //
				Section.QUANT, //
				Section.REPORT);
	}

	public static Section maintenanceSection() {

		return Section.MAINTENANCE;
	}

	public static List<Target> targets(Section section) {

		if(section == null) {
			return List.of();
		}
		List<Target> found = TARGETS.get(section);
		return found == null ? List.of() : found;
	}

	/**
	 * Engineer toolbox under {@link Section#MAINTENANCE}. Excludes about
	 * and reset layout.
	 */
	public static List<Target> engineerTargets() {

		return filterMaintenance(false);
	}

	public static List<Target> systemTargets() {

		return filterMaintenance(true);
	}

	/**
	 * Opens an existing GC-console page. Does not invent a plant-home part.
	 */
	public static boolean opensConsole(Target target) {

		if(target == null || target.stub() || target.opensPart()) {
			return false;
		}
		String key = target.actionKey();
		return PAGE_OVEN.equals(key) || PAGE_GAS.equals(key) || PAGE_FID.equals(key) || PAGE_PARAMETERS.equals(key);
	}

	/**
	 * Dedicated engineer content. Password is required before the page opens.
	 * These actions do not reopen the operator remote-control panel.
	 */
	public static boolean opensEngineerPage(Target target) {

		if(target == null || target.stub() || target.opensPart()) {
			return false;
		}
		String key = target.actionKey();
		return PAGE_DEVICE.equals(key) || PAGE_TEMP.equals(key) || PAGE_GAS_ENG.equals(key) || PAGE_LINK.equals(key);
	}

	private static List<Target> filterMaintenance(boolean secondary) {

		List<Target> found = new ArrayList<>();
		for(Target target : targets(Section.MAINTENANCE)) {
			if(target.secondary() == secondary) {
				found.add(target);
			}
		}
		return List.copyOf(found);
	}

	public static Target primaryTarget(Section section) {

		List<Target> found = targets(section);
		for(Target target : found) {
			if(target.opensPart() && !target.stub()) {
				return target;
			}
		}
		return found.isEmpty() ? null : found.get(0);
	}

	public static Section active() {

		Section section = ACTIVE.get();
		return section == null ? Section.ACQUIRE : section;
	}

	public static void activate(Section section) {

		if(section == null) {
			return;
		}
		ACTIVE.set(section);
		ACTIVE_TARGET.set(primaryTarget(section));
		notifySectionWatch();
	}

	public static void activate(Section section, Target target) {

		activate(section);
		if(target != null) {
			ACTIVE_TARGET.set(target);
			notifySectionWatch();
		}
	}

	/**
	 * Optional listener for a section change. Task pages do not use it to
	 * paint a second copy of the left-nav children. The runnable must
	 * tolerate being called off a disposed widget and must not touch SWT
	 * from a unit test.
	 */
	public static void setSectionWatch(Runnable watch) {

		SECTION_WATCH.set(watch);
	}

	private static void notifySectionWatch() {

		Runnable watch = SECTION_WATCH.get();
		if(watch == null) {
			return;
		}
		try {
			watch.run();
		} catch(RuntimeException ignored) {
			// A disposed strip must not break navigation.
		}
	}

	public static Target activeTarget() {

		return ACTIVE_TARGET.get();
	}

	/**
	 * Child rows under an expanded parent. A section with only its own
	 * page has no children, so 连接与采集 does not indent a copy of itself.
	 */
	public static List<Target> childTargets(Section section) {

		List<Target> all = targets(section);
		if(all.size() <= 1) {
			return List.of();
		}
		return all;
	}

	public static int stepOf(Section section) {

		int index = primarySections().indexOf(section);
		return index < 0 ? 0 : index + 1;
	}

	/**
	 * Only the active parent shows its children. Maintenance never expands
	 * into the workflow column.
	 */
	public static boolean expandsChildren(Section section) {

		return section != null && section == active() && !section.maintenance() && !childTargets(section).isEmpty();
	}

	public interface TaskOpener {

		void open(Section section, Target target);
	}

	public static void setTaskOpener(TaskOpener opener) {

		OPENER.set(opener);
	}

	public static void openTarget(Section section, Target target) {

		if(section == null || target == null) {
			return;
		}
		activate(section, target);
		TaskOpener opener = OPENER.get();
		if(opener != null) {
			opener.open(section, target);
		}
	}

	/**
	 * Section that should stay highlighted for this part. The active section
	 * wins when it lists the part (响应因子 and 样品结果 share one part).
	 */
	public static Section sectionForPart(String partId) {

		if(partId == null || partId.isBlank()) {
			return null;
		}
		Section active = active();
		if(lists(active, partId)) {
			return active;
		}
		Section primary = primaryOwner(partId);
		if(primary != null) {
			return primary;
		}
		if(lists(Section.MAINTENANCE, partId)) {
			return Section.MAINTENANCE;
		}
		return null;
	}

	public static Section sectionForLegacyLabel(String label) {

		if(label == null || label.isBlank()) {
			return null;
		}
		String normalized = BaijiuShellChrome.normalizeMenuLabel(label);
		return LEGACY.get(normalized);
	}

	public static boolean isTaskAreaPart(String partId) {

		if(partId == null || partId.isBlank()) {
			return false;
		}
		if(BaijiuShellChrome.NAV_PART_ID.equals(partId) || BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID.equals(partId)) {
			return false;
		}
		return taskPartIds().contains(partId);
	}

	/**
	 * Parts the nav may select. The chromatogram host is absent, so a nav
	 * switch cannot unload it.
	 */
	public static Set<String> taskPartIds() {

		Set<String> ids = new LinkedHashSet<>();
		for(List<Target> targets : TARGETS.values()) {
			for(Target target : targets) {
				if(target.opensPart()) {
					ids.add(target.partId());
				}
			}
		}
		ids.remove(BaijiuShellChrome.CHROMATOGRAM_HOME_PART_ID);
		ids.remove(BaijiuShellChrome.NAV_PART_ID);
		/*
		 * Legacy 白酒操作 host stays a task-stack part so selecting it cannot
		 * unload the chromatogram. It is not an engineer-toolbox card.
		 */
		ids.add(BaijiuShellChrome.WORKBENCH_HOME_PART_ID);
		return ids;
	}

	public static boolean switchesTaskOnly(String partId) {

		return isTaskAreaPart(partId);
	}

	static Section primaryOwner(String partId) {

		if(BaijiuShellChrome.ACQUIRE_HOME_PART_ID.equals(partId)) {
			return Section.ACQUIRE;
		}
		if(BaijiuShellChrome.METHOD_PACK_HOME_PART_ID.equals(partId)) {
			return Section.METHOD;
		}
		if(BaijiuShellChrome.WIZARD_HOME_PART_ID.equals(partId) || BaijiuShellChrome.INTEGRATION_HOME_PART_ID.equals(partId)) {
			return Section.QUANT;
		}
		if(BaijiuShellChrome.SEQUENCE_HOME_PART_ID.equals(partId) || BaijiuShellChrome.BATCH_RESULTS_HOME_PART_ID.equals(partId) || BaijiuShellChrome.SIMPLE_BATCH_HOME_PART_ID.equals(partId) || BaijiuShellChrome.PARALLEL_HOME_PART_ID.equals(partId)) {
			return Section.SEQUENCE;
		}
		if(BaijiuShellChrome.ANALYSIS_HOME_PART_ID.equals(partId) || BaijiuShellChrome.QC_HOME_PART_ID.equals(partId)) {
			return Section.QUANT;
		}
		if(BaijiuShellChrome.REPORT_HOME_PART_ID.equals(partId)) {
			return Section.REPORT;
		}
		if(BaijiuShellChrome.WORKBENCH_HOME_PART_ID.equals(partId)) {
			return Section.MAINTENANCE;
		}
		return null;
	}

	private static boolean lists(Section section, String partId) {

		for(Target target : targets(section)) {
			if(partId.equals(target.partId())) {
				return true;
			}
		}
		return false;
	}

	private static Map<Section, List<Target>> targetsBySection() {

		Map<Section, List<Target>> map = new LinkedHashMap<>();
		map.put(Section.ACQUIRE, List.of(part(BaijiuShellChrome.ACQUIRE_HOME_PART_ID, "连接与采集", false)));
		map.put(Section.METHOD, List.of(part(BaijiuShellChrome.METHOD_PACK_HOME_PART_ID, "方法包", false)));
		map.put(Section.CALIBRATE, List.of(part(BaijiuShellChrome.ANALYSIS_HOME_PART_ID, "响应因子 RF", false)));
		map.put(Section.SEQUENCE, List.of( //
				part(BaijiuShellChrome.SEQUENCE_HOME_PART_ID, "进样序列", false), //
				part(BaijiuShellChrome.BATCH_RESULTS_HOME_PART_ID, "批处理结果", false), //
				part(BaijiuShellChrome.SIMPLE_BATCH_HOME_PART_ID, "简单批量", false), //
				part(BaijiuShellChrome.PARALLEL_HOME_PART_ID, "平行样", false)));
		map.put(Section.QUANT, List.of( //
				part(BaijiuShellChrome.ANALYSIS_HOME_PART_ID, "样品结果", false), //
				part(BaijiuShellChrome.WIZARD_HOME_PART_ID, "三步向导", false), //
				part(BaijiuShellChrome.INTEGRATION_HOME_PART_ID, "推荐积分", false), //
				part(BaijiuShellChrome.QC_HOME_PART_ID, "QC 摘要", true)));
		map.put(Section.REPORT, List.of(part(BaijiuShellChrome.REPORT_HOME_PART_ID, "预览与导出", false)));
		/*
		 * Four engineer pages. They are not links back into the operator
		 * remote panel, and they do not own method / integration / quant /
		 * calibration / report editors.
		 */
		map.put(Section.MAINTENANCE, List.of( //
				action(PAGE_DEVICE, "设备配置"), //
				action(PAGE_TEMP, "温度标定/整定"), //
				action(PAGE_GAS_ENG, "气路与FID"), //
				action(PAGE_LINK, "通信与采集"), //
				action(ACTION_ABOUT, "关于"), //
				action(ACTION_RESET, "重置窗口布局")));
		return map;
	}

	private static Target part(String partId, String label, boolean stub) {

		return new Target(partId, label, stub, null);
	}

	private static Target action(String actionKey, String label) {

		return new Target(null, label, false, actionKey);
	}

	private static Map<String, Section> legacyLabels() {

		Map<String, Section> map = new LinkedHashMap<>();
		remember(map, Section.ACQUIRE, "连接与采集", "气相色谱控制台", "气相色谱工作台", "显示/隐藏反控", "反控", "打开谱图", "打开色谱图", "开始分析", "open chromatogram", "start analysis", "toggle gc console");
		remember(map, Section.METHOD, "方法包");
		remember(map, Section.CALIBRATE, "校正", "混标校正", "响应因子", "响应因子 RF");
		remember(map, Section.SEQUENCE, "序列与批跑", "进样序列", "批处理结果", "简单批量", "平行样", "injection sequence", "batch results", "simple batch");
		remember(map, Section.QUANT, "定量结果 / QC", "定量结果/QC", "定量/白酒分析", "白酒分析", "样品结果", "三步向导", "推荐积分", "recommended integration", "QC 摘要", "QC摘要", "analysis");
		remember(map, Section.REPORT, "报告", "预览报告", "预览与导出", "preview report", "report");
		remember(map, Section.MAINTENANCE, "维护", ENGINEER_MODE, "温度程序", "气路", "FID 调试", "采集诊断", "方法编辑", "系统参数", "白酒操作", "高级处理", "高级操作", "重置窗口布局", "关于", "选择视图", "浅色主题", "切换厂工作台", "许可", "许可 / 版本", "baijiu actions", "about", "reset layout");
		return map;
	}

	private static void remember(Map<String, Section> map, Section section, String... labels) {

		for(String label : labels) {
			map.put(BaijiuShellChrome.normalizeMenuLabel(label), section);
		}
	}

	/**
	 * Operator phrases owned by this navigation map. Used by the fragment
	 * test to keep product names out of the plant nav.
	 */
	public static List<String> operatorPhrases() {

		List<String> phrases = new ArrayList<>();
		phrases.add(LATER);
		for(Section section : Section.values()) {
			phrases.add(section.label());
			for(Target target : targets(section)) {
				phrases.add(target.label());
			}
		}
		for(String label : LEGACY.keySet()) {
			phrases.add(label);
		}
		List<String> copy = new ArrayList<>();
		for(String phrase : phrases) {
			if(phrase != null && !phrase.isBlank()) {
				copy.add(phrase);
			}
		}
		copy.sort(String::compareTo);
		return List.copyOf(copy);
	}
}
