param(
	[string]$OpenChromRoot = "e:\OpenChrom\openchrom\openchrom"
)
$ErrorActionPreference = "Stop"
$pluginsRoot = Join-Path $OpenChromRoot "plugins"
$required = @(
	@{ Id = "net.openchrom.nl.zh.chemclipse.rcp.app.ui"; File = "bin\nl\zh\CN\toc.xml"; Pattern = "\u8272\u8c31" },
	@{ Id = "net.openchrom.nl.zh.chemclipse.ux.extension.xxd.ui"; File = "bin\nl\zh\CN\controls.xml"; Pattern = "\u5cf0\u68c0\u6d4b\u5668" },
	@{ Id = "net.openchrom.nl.zh.chemclipse.ux.extension.xxd.ui"; File = "bin\nl\zh\CN\html\controls\peak_detector.html"; Pattern = "\u5cf0\u68c0\u6d4b\u5668" },
	@{ Id = "net.openchrom.nl.zh.chemclipse.xxd.process.supplier.pca.ui"; File = "bin\nl\zh\CN\controls.xml"; Pattern = "\u8f7d\u8377\u56fe" },
	@{ Id = "net.openchrom.nl.zh.openchrom.xxd.process.supplier.templates.ui"; File = "bin\nl\zh\CN\controls.xml"; Pattern = "\u6a21\u677f\u5ba1\u9605" },
	@{ Id = "net.openchrom.nl.zh.chemclipse.chromatogram.msd.filter.supplier.subtract.ui"; File = "bin\nl\zh\CN\toc_filter.xml"; Pattern = "\u6263\u9664\u8fc7\u6ee4\u5668" },
	@{ Id = "net.openchrom.nl.zh.eclipse.help.base"; File = "bin\nl\zh\CN\doc\help_home.html"; Pattern = "\u4f7f\u7528\u5e2e\u52a9\u7cfb\u7edf" }
)
$errors = @()
foreach ($item in $required) {
	$full = Join-Path (Join-Path $pluginsRoot $item.Id) $item.File
	if (-not (Test-Path $full)) { $errors += "Missing: $full"; continue }
	$content = [System.IO.File]::ReadAllText($full, [System.Text.UTF8Encoding]::new($false))
	if ($content -notmatch $item.Pattern) { $errors += "Pattern failed: $($item.File) in $($item.Id)" }
}
if ($errors.Count -gt 0) { $errors | ForEach-Object { Write-Host "FAIL $_" }; exit 1 }
Write-Host "PASSED: help NL runtime verification succeeded."
exit 0