# Compiles org.eclipse.chemclipse.csd.model into bin/ for PDE runs.
param(
	[string]$PluginRoot = "E:\OpenChrom\chemclipse\chemclipse\plugins\org.eclipse.chemclipse.csd.model",
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
$cp = (Get-ChildItem $BundlePool -Filter "*.jar" | Where-Object { $_.Name -notmatch '\.source_' } | ForEach-Object { $_.FullName }) -join ";"
$files = Get-ChildItem $src -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
$argFile = Join-Path $env:TEMP "laiende-csd-model-javac.txt"
$lines = @("-encoding", "UTF-8", "-source", "21", "-target", "21", "-cp", $cp, "-d", $bin) + $files
[System.IO.File]::WriteAllLines($argFile, $lines)

& javac "-proc:none" "@$argFile" 2>&1 | Out-Null
$classCount = (Get-ChildItem $bin -Recurse -Filter "*.class" -ErrorAction SilentlyContinue).Count
if($classCount -lt 20) {
	throw "csd.model compile failed; only $classCount class files were produced."
}

Get-ChildItem $src -Recurse -Filter "*.properties" | ForEach-Object {
	$relative = $_.FullName.Substring($src.Length + 1)
	$target = Join-Path $bin $relative
	New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null
	Copy-Item $_.FullName $target -Force
}

Write-Host "Compiled $classCount class files into $bin"