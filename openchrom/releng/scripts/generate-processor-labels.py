import os
import re
import xml.etree.ElementTree as ET

ROOT = r"E:\OpenChrom\chemclipse\chemclipse\plugins"
JAVA_PATTERNS = [
    re.compile(r'public String getName\(\)\s*\{[^}]*?return "([^"]+)";', re.S),
    re.compile(r'private static final String NAME = "([^"]+)";'),
    re.compile(r'super\([^,]+,\s*"([^"]+)"\s*,', re.S),
]
XML_PATTERN = re.compile(r'filterName="([^"]+)"')

TERM_MAP = {
    "Activate/Deactivate by Classification": "按分类激活/停用",
    "Activate/Deactivate for Analysis": "按分析激活/停用",
    "Area Filter": "面积滤波器",
    "Area Percent Filter": "面积百分比滤波器",
    "Asymmetry Filter": "对称性滤波器",
    "Baseline Extractor": "基线提取器",
    "Baseline Subtract": "基线扣除",
    "Cut Off (Intensity)": "截断 (强度)",
    "Cut Off (Zero)": "截断 (零)",
    "Delete Integrations": "删除积分",
    "Delete Peaks": "删除峰",
    "Delete Peaks by Model": "按模型删除峰",
    "Delete Peaks by Quantitation": "按定量删除峰",
    "Delete Peaks by Range": "按范围删除峰",
    "Delete Peaks by Target": "按目标删除峰",
    "Delete Peaks by Trace": "按谱迹删除峰",
    "Delete Quantitation References": "删除定量参考",
    "Delete Quantitations": "删除定量",
    "Delete Scans By Range": "按范围删除扫描",
    "Delete Standards": "删除内标",
    "Delete Targets": "删除目标",
    "Delete Targets Filter": "删除目标滤波器",
    "Divider": "除法器",
    "Gap Filler": "间隙填充",
    "Header Input": "文件头输入",
    "Header Transfer": "文件头转移",
    "High Pass Ions": "高通离子",
    "High Pass Peaks": "高通峰",
    "Invert": "反转",
    "Low Pass Ions": "低通离子",
    "Low Pass Peaks": "低通峰",
    "Mean Normalizer": "均值归一化",
    "Median Normalizing": "中值归一化",
    "Multiplier": "乘数",
    "Divisor": "除数",
    "Name Replacer": "名称替换",
    "Normalizer": "归一化",
    "Nominalize (Unit Mass)": "标称化 (单位质量)",
    "Obfuscator": "混淆器",
    "Peak Classifier (Add)": "峰分类器 (添加)",
    "Peak Classifier (Assign)": "峰分类器 (分配)",
    "Peak Classifier (Remove)": "峰分类器 (移除)",
    "Peak Targets to References Transfer": "峰目标转参考转移",
    "Penalties (Clear)": "惩罚 (清除)",
    "Penalty Calculator": "惩罚计算器",
    "Recalculate Scan Delay/Interval": "重新计算扫描延迟/间隔",
    "Remove Empty Scans": "移除空扫描",
    "Reshape": "重塑",
    "Retention Time Shifter": "保留时间偏移",
    "Retention Time Stretcher": "保留时间拉伸",
    "Savitzky-Golay Smoothing": "Savitzky-Golay 平滑",
    "Scan Clipper": "扫描裁剪",
    "Scan Density": "扫描密度",
    "Scan Duplicator": "扫描复制",
    "Scan Filler (Padding Empty)": "扫描填充 (补空)",
    "Scan Maxima Detector UI": "扫描最大值检测界面",
    "Scan Maxima Identifier UI": "扫描最大值定性界面",
    "Scan Merger (experimental)": "扫描合并 (实验性)",
    "Scan Remover": "扫描移除",
    "Scan Target to Peak Transfer": "扫描目标转峰转移",
    "Scan Targets (Buffer)": "扫描目标 (缓冲)",
    "Scan Targets (Delete)": "扫描目标 (删除)",
    "Scan Targets to References Transfer": "扫描目标转参考转移",
    "Shift Intensity": "偏移强度",
    "Shape Filter": "形状滤波器",
    "Signal to Noise": "信噪比",
    "Subtract Filter (UI)": "扣除滤波 (界面)",
    "Transform": "变换",
    "Unit Sum Normalizer": "单位总和归一化",
    "ZeroSet": "置零",
    "Width Filter": "宽度滤波器",
    "Minimum Number of m/z Filter": "最小 m/z 数量滤波器",
    "Measurement Results": "测量结果",
    "Retention Index Selector": "保留指数选择器",
}

WORD_MAP = [
    ("Filter", "滤波器"),
    ("Normalizer", "归一化器"),
    ("Normalizing", "归一化"),
    ("Calculator", "计算器"),
    ("Classifier", "分类器"),
    ("Detector", "检测器"),
    ("Identifier", "定性器"),
    ("Integrator", "积分器"),
    ("Extractor", "提取器"),
    ("Transfer", "转移"),
    ("Delete", "删除"),
    ("Remove", "移除"),
    ("Clear", "清除"),
    ("Baseline", "基线"),
    ("Chromatogram", "色谱图"),
    ("Peak", "峰"),
    ("Peaks", "峰"),
    ("Scan", "扫描"),
    ("Scans", "扫描"),
    ("Target", "目标"),
    ("Targets", "目标"),
    ("Standard", "内标"),
    ("Standards", "内标"),
    ("Quantitation", "定量"),
    ("Quantitations", "定量"),
    ("Integration", "积分"),
    ("Integrations", "积分"),
    ("Penalty", "惩罚"),
    ("Penalties", "惩罚"),
    ("Analysis", "分析"),
    ("Classification", "分类"),
    ("Intensity", "强度"),
    ("Range", "范围"),
    ("Trace", "谱迹"),
    ("Model", "模型"),
    ("Area", "面积"),
    ("Width", "宽度"),
    ("Shape", "形状"),
    ("Asymmetry", "对称性"),
    ("Signal to Noise", "信噪比"),
    ("High Pass", "高通"),
    ("Low Pass", "低通"),
    ("Ions", "离子"),
    ("Retention Time", "保留时间"),
    ("Header", "文件头"),
    ("Transform", "变换"),
    ("Invert", "反转"),
    ("Multiplier", "乘数"),
    ("Divider", "除法器"),
    ("Obfuscator", "混淆器"),
    ("Experimental", "实验性"),
    ("Add", "添加"),
    ("Assign", "分配"),
    ("Remove", "移除"),
    ("Buffer", "缓冲"),
]


def collect_names():
    names = set()
    for dirpath, _, files in os.walk(ROOT):
        parts = dirpath.split(os.sep)
        if "target" in parts or "bin" in parts:
            continue
        for filename in files:
            path = os.path.join(dirpath, filename)
            if filename.endswith(".java"):
                try:
                    text = open(path, encoding="utf-8", errors="ignore").read()
                except OSError:
                    continue
                for pattern in JAVA_PATTERNS:
                    for match in pattern.finditer(text):
                        value = match.group(1).strip()
                        if value and value not in {"name", "NAME:"} and len(value) < 120:
                            names.add(value)
            elif filename == "plugin.xml":
                try:
                    text = open(path, encoding="utf-8", errors="ignore").read()
                except OSError:
                    continue
                for match in XML_PATTERN.finditer(text):
                    names.add(match.group(1).strip())
    return sorted(names)


def auto_translate(name: str) -> str:
    if name in TERM_MAP:
        return TERM_MAP[name]
    translated = name
    for english, chinese in WORD_MAP:
        translated = translated.replace(english, chinese)
    if translated != name:
        return translated
    return name


def java_escape(text: str) -> str:
    return text.encode("unicode_escape").decode("ascii").replace('"', '\\"')


def main():
    names = collect_names()
    print(f"// Generated processor labels: {len(names)}")
    for name in names:
        zh = auto_translate(name)
        if zh == name:
            continue
        print(f'labels.put("{java_escape(name)}", "{java_escape(zh)}");')


if __name__ == "__main__":
    main()
