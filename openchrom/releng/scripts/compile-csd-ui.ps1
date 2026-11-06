# Compiles org.eclipse.chemclipse.ux.extension.csd.ui into its bin/ folder for PDE runs.
param(
	[string]$PluginRoot = "E:\OpenChrom\chemclipse\chemclipse\plugins\org.eclipse.chemclipse.ux.extension.csd.ui",
	[string]$BundlePool = "E:\OpenChrom\.metadata\.plugins\org.eclipse.pde.core\.bundle_pool\plugins"
)

if(-not (Test-Path $PluginRoot)) {
	throw "Plugin root not found: $PluginRoot"
}
if(-not (Test-Path $BundlePool)) {
	throw "PDE bundle pool not found: $BundlePool"
}

$bin = Join-Path $PluginRoot "bin"
$src = Join-Path $PluginRoot "src"
New-Item -ItemType Directory -Force -Path $bin | Out-Null
$cp = (Get-ChildItem $BundlePool -Filter "*.jar" | ForEach-Object { $_.FullName }) -join ";"
$files = Get-ChildItem $src -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
if($files.Count -eq 0) {
	Write-Host "No Java sources in $PluginRoot"
	exit 0
}
$argFile = Join-Path $env:TEMP "laiende-csd-ui-javac.txt"
$lines = @("-encoding", "UTF-8", "-source", "21", "-target", "21", "-cp", $cp, "-d", $bin) + $files
[System.IO.File]::WriteAllLines($argFile, $lines)

& javac "-proc:none" "@$argFile" 2>&1 | Out-Null
$classCount = (Get-ChildItem $bin -Recurse -Filter "*.class" -ErrorAction SilentlyContinue).Count
if($classCount -lt 1) {
	throw "csd.ui compile failed; no class files were produced."
}

Write-Host "Compiled $classCount class files into $bin"