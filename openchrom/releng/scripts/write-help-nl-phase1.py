import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "plugins"))

FILES = {
    os.path.join(ROOT, "net.openchrom.nl.zh.chemclipse.rcp.app.ui", "src", "nl", "zh", "CN", "toc.xml"): """<?xml version="1.0" encoding="UTF-8"?>
<?NLS TYPE="org.eclipse.help.toc"?>

<toc label="\u8272\u8c31/\u8d28\u8c31">
   <topic label="\u63a7\u4ef6">
      <anchor id="controls"/>
   </topic>
   <topic label="\u6982\u89c8">
      <anchor id="overview"/>
   </topic>
   <topic label="\u6982\u5ff5">
      <anchor id="concepts"/>
   </topic>
   <topic label="\u529f\u80fd">
      <anchor id="functionality"/>
   </topic>
   <topic label="\u5de5\u4f5c\u6d41">
      <anchor id="workflows"/>
   </topic>
   <topic label="\u9996\u9009\u9879">
      <anchor id="preferences"/>
   </topic>
   <topic label="\u6cd5\u5f8b\u4fe1\u606f">
      <anchor id="legal"/>
   </topic>
</toc>
""",
    os.path.join(ROOT, "net.openchrom.nl.zh.chemclipse.ux.extension.xxd.ui", "src", "nl", "zh", "CN", "controls.xml"): """<?xml version="1.0" encoding="UTF-8"?>
<?NLS TYPE="org.eclipse.help.toc"?>

<toc label="\u63a7\u4ef6" link_to="/org.eclipse.chemclipse.rcp.app.ui/toc.xml#controls">
\t<topic label="\u8272\u8c31\u7f16\u8f91\u5668" href="html/controls/chromatogram_editor.html">
\t</topic>
\t<topic href="html/controls/peak_detector.html" label="\u5cf0\u68c0\u6d4b\u5668">
\t</topic>
\t<topic label="\u5cf0/\u626b\u63cf\u5217\u8868" href="html/controls/peak_scan_list.html">
\t</topic>
\t<topic label="\u8272\u8c31\u53e0\u52a0" href="html/controls/chromatogram_overlay.html">
\t</topic>
\t<topic href="html/controls/targets.html" label="\u76ee\u6807\u7269">
\t</topic>
</toc>
""",
}

HTML_FILES = {
    "chromatogram_editor.html": """<h2>\u63a7\u4ef6</h2>

<h3>\u8272\u8c31\u7f16\u8f91\u5668</h3>
<table border="1" cellpadding="10" cellspacing="0" summary="\u8272\u8c31\u7f16\u8f91\u5668\u5feb\u6377\u952e">
  <tr>
    <td><kbd>&larr;</kbd> \u548c <kbd>&rarr;</kbd></td>
    <td>\u653e\u5927\u540e\uff08\u5957\u7d22\u6846\u9009\uff09\uff0c\u4f7f\u7528\u65b9\u5411\u952e\u6216\u6eda\u52a8\u6761\u5728\u8272\u8c31\u56fe\u4e2d\u79fb\u52a8\u3002</td>
  </tr>
  <tr>
    <td>\u9f20\u6807\u6eda\u8f6e</td>
    <td>\u8c03\u6574\u5f3a\u5ea6\u8f74\uff0c\u5b9e\u73b0\u7eb5\u5411\u7f29\u653e\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + <kbd>&larr;</kbd> \u6216 <kbd>&rarr;</kbd></td>
    <td>\u9009\u62e9\u4e0a\u4e00\u4e2a/\u4e0b\u4e00\u4e2a\u626b\u63cf\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + <kbd>Alt</kbd> + \u9f20\u6807\u5de6\u952e</td>
    <td>\u9009\u62e9\u5cf0\u3002</td>
  </tr>
  <tr>
    <td><kbd>Shift</kbd>/<kbd>Alt</kbd> + \u9f20\u6807\u5de6\u952e</td>
    <td>\u4ece\u5de6\u4e0a\u89d2\u5f00\u59cb\u6846\u9009\u653e\u5927\u3002</td>
  </tr>
  <tr>
    <td><kbd>Shift</kbd> + \u9f20\u6807\u5de6\u952e\u53cc\u51fb</td>
    <td>\u91cd\u7f6e\u7f29\u653e\uff081:1\uff09\u3002</td>
  </tr>
</table>
""",
    "peak_detector.html": """<h2>\u63a7\u4ef6</h2>

<h3>\u5cf0\u68c0\u6d4b\u5668</h3>
<p>\u4f7f\u7528\u4e0b\u65b9\u5feb\u6377\u952e\uff08\u6216\u5de5\u5177\u680f\u6309\u94ae\uff09\u8bbe\u7f6e\u6a21\u5f0f\uff0c\u7136\u540e\u901a\u8fc7\u6dfb\u52a0\u6309\u94ae\u6216\u5176\u5feb\u6377\u952e\u786e\u8ba4\u5cf0\u3002</p>
<table border="1" cellpadding="10" cellspacing="0" summary="\u5cf0\u68c0\u6d4b\u5668\u5feb\u6377\u952e">
  <tr>
    <th>\u6309\u952e</th>
    <th>\u8bf4\u660e</th>
  </tr>
  <tr>
    <td><kbd>D</kbd></td>
    <td><b>B</b>\uff1a\u57fa\u7ebf<br/>\u6309\u4f4f <kbd>Ctrl</kbd> \u5e76\u7528\u9f20\u6807\u62d6\u52a8\u3002</td>
  </tr>
  <tr>
    <td><kbd>B</kbd></td>
    <td><b>BB</b>\uff1a\u57fa\u7ebf\u5230\u57fa\u7ebf<br/>\u7528\u9f20\u6807\u5de6\u952e\u5b9a\u4e49\u8d77\u70b9\u548c\u7ec8\u70b9\u3002</td>
  </tr>
  <tr>
    <td><kbd>V</kbd></td>
    <td><b>VV</b>\uff1a\u8c37\u5230\u8c37<br/>\u7528\u9f20\u6807\u5de6\u952e\u5b9a\u4e49\u8d77\u70b9\u548c\u7ec8\u70b9\u3002</td>
  </tr>
  <tr>
    <td><kbd>N</kbd></td>
    <td><b>BV</b>\uff1a\u57fa\u7ebf\u5230\u8c37<br/>\u7528\u9f20\u6807\u5de6\u952e\u5b9a\u4e49\u8d77\u70b9\u548c\u7ec8\u70b9\u3002</td>
  </tr>
  <tr>
    <td><kbd>C</kbd></td>
    <td><b>VB</b>\uff1a\u8c37\u5230\u57fa\u7ebf<br/>\u7528\u9f20\u6807\u5de6\u952e\u5b9a\u4e49\u8d77\u70b9\u548c\u7ec8\u70b9\u3002</td>
  </tr>
  <tr>
    <td><kbd>A</kbd></td>
    <td><b>+</b>\uff1a\u6dfb\u52a0\u5cf0\u3002</td>
  </tr>
</table>
""",
    "peak_scan_list.html": """<h2>\u63a7\u4ef6</h2>

<h3>\u5cf0/\u626b\u63cf\u5217\u8868</h3>
<table border="1" cellpadding="10" cellspacing="0" summary="\u5cf0/\u626b\u63cf\u5217\u8868\u5feb\u6377\u952e">
  <tr>
    <td><kbd>Del</kbd></td>
    <td>\u5220\u9664\u6240\u9009\u5cf0\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + <kbd>i</kbd></td>
    <td>\u5c06\u8be5\u6761\u76ee\u8bbe\u4e3a\u53c2\u4e0e\u5206\u6790\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>i</kbd></td>
    <td>\u5c06\u8be5\u6761\u76ee\u6807\u4e3a\u4e0d\u53c2\u4e0e\u5206\u6790\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + <kbd>s</kbd></td>
    <td>\u5c06\u6240\u9009\u5cf0\u8bbe\u4e3a\u5185\u6807\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + <kbd>g</kbd></td>
    <td>\u6253\u5f00\u5206\u7c7b\u5668\u5bf9\u8bdd\u6846\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + <kbd>d</kbd></td>
    <td>\u5220\u9664\u6240\u6709\u8c31\u5e93\u9274\u5b9a\u7ed3\u679c\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + <kbd>u</kbd></td>
    <td>\u6dfb\u52a0\u672a\u77e5\u76ee\u6807\u7269\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + <kbd>q</kbd></td>
    <td>\u8fd0\u884c\u6240\u9009\u8c31\u5e93\u68c0\u7d22\u3002</td>
  </tr>
</table>
""",
    "chromatogram_overlay.html": """<h2>\u63a7\u4ef6</h2>

<h3>\u8272\u8c31\u53e0\u52a0</h3>
<table border="1" cellpadding="10" cellspacing="0" summary="\u8272\u8c31\u53e0\u52a0\u5feb\u6377\u952e">
  <tr>
    <td><kbd>Shift</kbd> + \u9f20\u6807\u5de6\u952e</td>
    <td>\u4ece\u5de6\u4e0a\u89d2\u62d6\u52a8\u77e9\u5f62\u4ee5\u653e\u5927\u663e\u793a\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + \u9f20\u6807\u5de6\u952e</td>
    <td>\u62d6\u52a8\u76f4\u7ebf\u6d4b\u91cf\u8ddd\u79bb\uff0c\u7ed3\u679c\u663e\u793a\u5728\u6807\u5c3a\u5de5\u5177\u680f\u4e2d\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + \u9f20\u6807\u5de6\u952e\u53cc\u51fb</td>
    <td>\u9009\u62e9\u8272\u8c31\u56fe\uff08\u4ee5\u7c97\u7ebf\u9ad8\u4eae\u663e\u793a\uff09\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + <kbd>Shift</kbd> + \u9f20\u6807\u5de6\u952e\u62d6\u52a8</td>
    <td>\u81ea\u7531\u79fb\u52a8\u8272\u8c31\u56fe\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + <kbd>Alt</kbd> + \u9f20\u6807\u5de6\u952e</td>
    <td>\u9690\u85cf\u8be5\u8272\u8c31\u56fe\u3002</td>
  </tr>
</table>
""",
    "targets.html": """<h2>\u63a7\u4ef6</h2>

<h3>\u76ee\u6807\u7269</h3>
<table border="1" cellpadding="10" cellspacing="0" summary="\u76ee\u6807\u7269\u5feb\u6377\u952e">
  <tr>
    <td><kbd>Del</kbd></td>
    <td>\u5220\u9664\u6240\u9009\u8c31\u5e93\u9274\u5b9a\u7ed3\u679c\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + <kbd>i</kbd></td>
    <td>\u624b\u52a8\u786e\u8ba4\u9274\u5b9a\u7ed3\u679c\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>i</kbd></td>
    <td>\u53d6\u6d88\u624b\u52a8\u786e\u8ba4\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + <kbd>d</kbd></td>
    <td>\u5220\u9664\u6240\u6709\u8c31\u5e93\u9274\u5b9a\u7ed3\u679c\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + <kbd>u</kbd></td>
    <td>\u6dfb\u52a0\u672a\u77e5\u76ee\u6807\u7269\u3002</td>
  </tr>
  <tr>
    <td><kbd>Ctrl</kbd> + <kbd>q</kbd></td>
    <td>\u8fd0\u884c\u6240\u9009\u8c31\u5e93\u68c0\u7d22\u3002</td>
  </tr>
</table>
""",
}

def main():
    html_root = os.path.join(
        ROOT,
        "net.openchrom.nl.zh.chemclipse.ux.extension.xxd.ui",
        "src",
        "nl",
        "zh",
        "CN",
        "html",
        "controls",
    )
    for name, content in HTML_FILES.items():
        FILES[os.path.join(html_root, name)] = content

    for path, content in FILES.items():
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(content)
        print("wrote", path)

if __name__ == "__main__":
    main()