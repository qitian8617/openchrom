import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "plugins"))
TARGET = os.path.join(ROOT, "net.openchrom.nl.zh.eclipse.help.base", "src", "nl", "zh", "CN", "doc", "help_home.html")

CONTENT = """<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0//EN">
<html lang="zh-cn">
<head>
<meta name="copyright" content="Copyright (c) IBM Corporation and others 2000, 2006. This page is made available under license. For full details see the LEGAL in the documentation book that contains this page." >
<meta http-equiv="Content-Language" content="zh-cn">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>\u4f7f\u7528 Eclipse \u5e2e\u52a9\u7cfb\u7edf</title>
<link rel="stylesheet" href="book.css" charset="utf-8" type="text/css">
</head>
<body>

<div id="content" role="main">
\t<h1>\u4f7f\u7528\u5e2e\u52a9\u7cfb\u7edf</h1>

\t<p>
\t\u5728\u5de6\u4fa7\u7684<b>\u5185\u5bb9</b>\u6846\u67b6<span class="icon">(<img
\tclass="icon" src="contents_view.svg" alt="\u5185\u5bb9\u56fe\u6807">)</span>\u4e2d\u6d4f\u89c8\u4e3b\u9898\u3002\u5355\u51fb\u4e3b\u9898\u5373\u53ef\u663e\u793a\u3002\u4f7f\u7528<b>\u540e\u9000</b>\u548c
\t<b>\u524d\u8fdb</b>\u6309\u94ae\u53ef\u5728\u5df2\u67e5\u770b\u4e3b\u9898\u7684\u5386\u53f2\u8bb0\u5f55\u4e2d\u5bfc\u822a\u3002
\t</p>

\t<h2>\u641c\u7d22</h2>

\t<p>
\t\u8981\u5728\u6587\u6863\u4e2d\u5feb\u901f\u5b9a\u4f4d\u67d0\u4e00\u4e3b\u9898\uff0c\u8bf7\u5728<b>\u641c\u7d22</b>\u5b57\u6bb5\u4e2d\u8f93\u5165\u67e5\u8be2\u3002\u4f7f\u7528<b>\u641c\u7d22</b>\u6846\u67b6<span
\tclass="icon">(<img class="icon" src="search_results_view.svg" alt="\u641c\u7d22\u56fe\u6807">)</span>
\t\u53ef\u663e\u793a\u641c\u7d22\u89c6\u56fe\u3002\u60a8\u53ef\u4ee5\u901a\u8fc7\u4ec5\u9009\u62e9\u611f\u5174\u8da3\u7684\u7ae0\u8282\u6765\u7f29\u5c0f<b>\u8303\u56f4</b>\u3002
\t</p>

\t<h2>\u540c\u6b65</h2>

\t<p>
\t\u5355\u51fb
\t<b>\u5728\u76ee\u5f55\u4e2d\u663e\u793a</b>\u6309\u94ae<span class="icon">(<img class="icon"
\tsrc="e_synch_toc_nav.svg" alt="\u5728\u76ee\u5f55\u4e2d\u663e\u793a\u56fe\u6807">)</span>\u53ef\u5728\u5bfc\u822a\u6811\u4e2d\u9009\u4e2d\u8be5\u4e3b\u9898\u3002
\t<b>\u4e0e\u5185\u5bb9\u94fe\u63a5</b>\u6309\u94ae<span class="icon">(<img class="icon"
\tsrc="e_auto_synch_toc.svg" alt="\u4e0e\u5185\u5bb9\u94fe\u63a5\u56fe\u6807">)</span>\u53ef\u4fdd\u6301\u5bfc\u822a\u6811\u4e0e\u5f53\u524d\u4e3b\u9898\u540c\u6b65\u3002
\t</p>

\t<h2>\u529f\u80fd\u90e8\u4ef6</h2>

\t<p>
\t\u8981\u663e\u793a\u5e94\u7528\u7a0b\u5e8f\u4e2d\u5df2\u7981\u7528\u529f\u80fd\u7684\u76f8\u5173\u6587\u6863\uff0c\u8bf7\u9009\u62e9<b>\u663e\u793a\u6240\u6709\u4e3b\u9898</b>\u6309\u94ae<span class="icon">(<img class="icon"
\tsrc="e_show_all.svg" alt="\u663e\u793a\u6240\u6709\u4e3b\u9898\u56fe\u6807">)
\t</span>\u3002\u8fd9\u4f1a\u5f71\u54cd\u76ee\u5f55\u548c\u641c\u7d22\u7ed3\u679c\u3002
\t</p>

\t<h2>\u5173\u952e\u5b57\u7d22\u5f15</h2>
\t<p>
\t\u8981\u663e\u793a\u6309\u5b57\u6bcd\u987a\u5e8f\u6392\u5217\u7684\u5173\u952e\u5b57\u7d22\u5f15\uff0c\u8bf7\u9009\u62e9<b>\u7d22\u5f15\u6846\u67b6</b>
\t<span class="icon">(<img class="icon" src="e_index_view.svg" alt="\u7d22\u5f15\u56fe\u6807">)</span>\u3002
\t\u5e76\u975e\u6240\u6709\u4ea7\u54c1\u90fd\u5305\u542b\u7d22\u5f15\u3002
\t</p>

</div>
</body>

</html>
"""

def main():
    os.makedirs(os.path.dirname(TARGET), exist_ok=True)
    with open(TARGET, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(CONTENT)
    print("wrote", TARGET)

if __name__ == "__main__":
    main()