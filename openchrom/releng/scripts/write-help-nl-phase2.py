import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "plugins"))

FILES = {}

def add(rel_plugin, rel_path, content):
    FILES[os.path.join(ROOT, rel_plugin, "src", "nl", "zh", "CN", *rel_path.split("/"))] = content

# PCA controls
add("net.openchrom.nl.zh.chemclipse.xxd.process.supplier.pca.ui", "controls.xml", """<?xml version="1.0" encoding="UTF-8"?>
<?NLS TYPE="org.eclipse.help.toc"?>

<toc label="\\u63a7\\u4ef6" link_to="/org.eclipse.chemclipse.rcp.app.ui/toc.xml#controls">
\t<topic label="\\u8f7d\\u8377\\u56fe" href="html/controls/loadings_plot.html">
\t</topic>
\t<topic label="\\u5f97\\u5206\\u56fe" href="html/controls/score_plot.html">
\t</topic>
\t<topic label="S\\u56fe" href="html/controls/s_plot.html">
\t</topic>
</toc>
""")

add("net.openchrom.nl.zh.chemclipse.xxd.process.supplier.pca.ui", "html/controls/loadings_plot.html", """<h2>\\u63a7\\u4ef6</h2>

<h3>\\u8f7d\\u8377\\u56fe</h3>
<table border="1" cellpadding="10" cellspacing="0" summary="\\u8f7d\\u8377\\u56fe\\u5feb\\u6377\\u952e">
\t<tr>
\t\t<td>\\u9f20\\u6807\\u5de6\\u952e\\u6309\\u4e0b</td>
\t\t<td>\\u5f00\\u59cb\\u6846\\u9009\\u653e\\u5927</td>
\t</tr>
\t<tr>
\t\t<td>\\u9f20\\u6807\\u5de6\\u952e\\u653e\\u5f00</td>
\t\t<td>\\u7ed3\\u675f\\u6846\\u9009\\u653e\\u5927</td>
\t</tr>
\t<tr>
\t\t<td><kbd>Ctrl</kbd>(Win/Linux)/<kbd>CMD</kbd>(Mac) + \\u9f20\\u6807\\u5de6\\u952e\\u6309\\u4e0b</td>
\t\t<td>\\u5f00\\u59cb\\u9009\\u62e9\\u7279\\u5f81\\uff0c\\u5728\\u7279\\u5f81\\u5217\\u8868\\u4e2d\\u9ad8\\u4eae\\u663e\\u793a</td>
\t</tr>
\t<tr>
\t\t<td><kbd>Ctrl</kbd>(Win/Linux)/<kbd>CMD</kbd>(Mac) + \\u9f20\\u6807\\u5de6\\u952e\\u653e\\u5f00</td>
\t\t<td>\\u7ed3\\u675f\\u9009\\u62e9\\u7279\\u5f81</td>
\t</tr>
</table>
""")

add("net.openchrom.nl.zh.chemclipse.xxd.process.supplier.pca.ui", "html/controls/score_plot.html", """<h2>\\u63a7\\u4ef6</h2>

<h3>\\u5f97\\u5206\\u56fe</h3>
<table border="1" cellpadding="10" cellspacing="0" summary="\\u5f97\\u5206\\u56fe\\u5feb\\u6377\\u952e">
\t<tr>
\t\t<td>\\u9f20\\u6807\\u5de6\\u952e\\u6309\\u4e0b</td>
\t\t<td>\\u5f00\\u59cb\\u6846\\u9009\\u653e\\u5927</td>
\t</tr>
\t<tr>
\t\t<td>\\u9f20\\u6807\\u5de6\\u952e\\u653e\\u5f00</td>
\t\t<td>\\u7ed3\\u675f\\u6846\\u9009\\u653e\\u5927</td>
\t</tr>
\t<tr>
\t\t<td><kbd>Ctrl</kbd>(Win/Linux)/<kbd>CMD</kbd>(Mac) + \\u9f20\\u6807\\u5de6\\u952e\\u6309\\u4e0b</td>
\t\t<td>\\u5f00\\u59cb\\u9009\\u62e9\\u6837\\u54c1\\uff0c\\u5728\\u6837\\u54c1\\u5217\\u8868\\u4e2d\\u9ad8\\u4eae\\u663e\\u793a</td>
\t</tr>
\t<tr>
\t\t<td><kbd>Ctrl</kbd>(Win/Linux)/<kbd>CMD</kbd>(Mac) + \\u9f20\\u6807\\u5de6\\u952e\\u653e\\u5f00</td>
\t\t<td>\\u7ed3\\u675f\\u9009\\u62e9\\u6837\\u54c1</td>
\t</tr>
</table>
""")

add("net.openchrom.nl.zh.chemclipse.xxd.process.supplier.pca.ui", "html/controls/s_plot.html", """<h2>\\u63a7\\u4ef6</h2>

<h3>S\\u56fe</h3>
<table border="1" cellpadding="10" cellspacing="0" summary="S\\u56fe\\u5feb\\u6377\\u952e">
\t<tr>
\t\t<td>\\u9f20\\u6807\\u5de6\\u952e\\u6309\\u4e0b</td>
\t\t<td>\\u5f00\\u59cb\\u6846\\u9009\\u653e\\u5927</td>
\t</tr>
\t<tr>
\t\t<td>\\u9f20\\u6807\\u5de6\\u952e\\u653e\\u5f00</td>
\t\t<td>\\u7ed3\\u675f\\u6846\\u9009\\u653e\\u5927</td>
\t</tr>
\t<tr>
\t\t<td><kbd>Ctrl</kbd>(Win/Linux)/<kbd>CMD</kbd>(Mac) + \\u9f20\\u6807\\u5de6\\u952e\\u6309\\u4e0b</td>
\t\t<td>\\u5f00\\u59cb\\u9009\\u62e9\\u7279\\u5f81\\uff0c\\u5728\\u7279\\u5f81\\u5217\\u8868\\u4e2d\\u9ad8\\u4eae\\u663e\\u793a</td>
\t</tr>
\t<tr>
\t\t<td><kbd>Ctrl</kbd>(Win/Linux)/<kbd>CMD</kbd>(Mac) + \\u9f20\\u6807\\u5de6\\u952e\\u653e\\u5f00</td>
\t\t<td>\\u7ed3\\u675f\\u9009\\u62e9\\u7279\\u5f81</td>
\t</tr>
\t<tr>
\t\t<td>\\u53cc\\u51fb</td>
\t\t<td>\\u9009\\u62e9\\u6700\\u8fd1\\u7684\\u7279\\u5f81</td>
\t</tr>
\t<tr>
\t\t<td><kbd>Ctrl</kbd>(Win/Linux)/<kbd>CMD</kbd>(Mac) + \\u53cc\\u51fb</td>
\t\t<td>\\u5c06\\u6700\\u8fd1\\u7684\\u7279\\u5f81\\u6dfb\\u52a0\\u5230\\u5f53\\u524d\\u9009\\u62e9\\uff0c\\u6216\\u4ece\\u5f53\\u524d\\u9009\\u62e9\\u4e2d\\u79fb\\u9664</td>
\t</tr>
</table>
""")

# Templates UI
add("net.openchrom.nl.zh.openchrom.xxd.process.supplier.templates.ui", "controls.xml", """<?xml version="1.0" encoding="UTF-8"?>
<?NLS TYPE="org.eclipse.help.toc"?>

<toc label="\\u63a7\\u4ef6" link_to="/org.eclipse.chemclipse.rcp.app.ui/toc.xml#controls">
\t<topic label="\\u6a21\\u677f\\u5ba1\\u9605\\u754c\\u9762" href="html/controls/template_review_ui.html">
\t</topic>
</toc>
""")

add("net.openchrom.nl.zh.openchrom.xxd.process.supplier.templates.ui", "html/controls/template_review_ui.html", """<h2>\\u63a7\\u4ef6</h2>

<h3>\\u6a21\\u677f\\u5ba1\\u9605\\u754c\\u9762</h3>
<table border="1" cellpadding="10" cellspacing="0" summary="\\u6a21\\u677f\\u5ba1\\u9605\\u754c\\u9762\\u5feb\\u6377\\u952e">
  <tr>
    <td>\\u6570\\u5b57\\u952e\\u76d8 <kbd>+</kbd> \\u548c <kbd>-</kbd></td>
    <td>\\u653e\\u5927\\u548c\\u7f29\\u5c0f\\u3002\\u7f29\\u653e\\u504f\\u79fb\\u53ef\\u5728\\u8bbe\\u7f6e\\u9875\\u9762\\u4e2d\\u5b9a\\u4e49\\u3002</td>
  </tr>
  <tr>
    <td>\\u53cc\\u51fb\\u8272\\u8c31\\u7247\\u6bb5\\u5de6/\\u53f3\\u534a\\u90e8</td>
    <td>\\u79fb\\u52a8\\u5230\\u4e0a\\u4e00\\u4e2a/\\u4e0b\\u4e00\\u4e2a\\u5cf0\\u3002</td>
  </tr>
</table>
""")

# MS Subtract
add("net.openchrom.nl.zh.chemclipse.chromatogram.msd.filter.supplier.subtract.ui", "toc_filter.xml", """<?xml version="1.0" encoding="UTF-8"?>
<?NLS TYPE="org.eclipse.help.toc"?>

<toc label="MS \\u6263\\u9664\\u8fc7\\u6ee4\\u5668" link_to="../org.eclipse.chemclipse.rcp.app.ui/toc.xml#functionality">
\t<topic label="MS \\u6263\\u9664\\u8fc7\\u6ee4\\u5668" href="html/plugin/main.html"/>
</toc>
""")

add("net.openchrom.nl.zh.chemclipse.chromatogram.msd.filter.supplier.subtract.ui", "html/plugin/main.html", """<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">

<html>
<head>
\t<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
\t<title>\\u6263\\u9664\\u8fc7\\u6ee4\\u5668</title>
</head>

<body>
<h1>\\u6263\\u9664\\u8fc7\\u6ee4\\u5668</h1>
\\u6263\\u9664\\u8fc7\\u6ee4\\u5668\\u53ef\\u7528\\u4e8e\\u4ece\\u8272\\u8c31\\u9009\\u533a\\u6216\\u6240\\u9009\\u5cf0\\u4e2d\\u6263\\u9664\\uff08\\u53e0\\u52a0\\u7684\\uff09\\u8d28\\u8c31\\u3002
</body>
</html>
""")


def decode(content: str) -> str:
    return content.encode("utf-8").decode("unicode_escape")


def main() -> None:
    for path, content in FILES.items():
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(decode(content))
        print("wrote", path)


if __name__ == "__main__":
    main()