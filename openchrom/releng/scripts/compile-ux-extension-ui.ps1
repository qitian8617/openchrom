# Compiles org.eclipse.chemclipse.ux.extension.ui into its bin/ folder for PDE runs.
param(
	[string]$PluginRoot = "E:\OpenChrom\chemclipse\chemclipse\plugins\org.eclipse.chemclipse.ux.extension.ui",
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

$jar = Get-ChildItem $BundlePool -Filter "org.eclipse.chemclipse.ux.extension.ui_*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if($jar) {
	$existing = (Get-ChildItem $bin -Recurse -Filter "*.class" -ErrorAction SilentlyContinue).Count
	if($existing -lt 50) {
		Add-Type -AssemblyName System.IO.Compression.FileSystem
		[System.IO.Compression.ZipFile]::ExtractToDirectory($jar.FullName, $bin)
	}
}

$cpJars = @(Get-ChildItem $BundlePool -Filter "*.jar" | Where-Object { $_.Name -notmatch "\.source_" } | ForEach-Object { $_.FullName })
$cp = (@($bin) + $cpJars) -join ";"
$files = Get-ChildItem $src -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
$argFile = Join-Path $env:TEMP "ux-extension-ui-javac.txt"
$lines = @("-encoding", "UTF-8", "-source", "21", "-target", "21", "-proc:none", "-Xlint:none", "-cp", $cp, "-d", $bin) + $files
[System.IO.File]::WriteAllLines($argFile, $lines)

$errFile = Join-Path $env:TEMP "ux-extension-ui-javac.err"
& javac "@$argFile" 2>$errFile
if($LASTEXITCODE -ne 0) {
	Get-Content $errFile -ErrorAction SilentlyContinue | Select-Object -Last 40
	throw "ux.extension.ui javac failed with exit code $LASTEXITCODE"
}

$classCount = (Get-ChildItem $bin -Recurse -Filter "*.class" -ErrorAction SilentlyContinue).Count
if($classCount -lt 50) {
	throw "ux.extension.ui compile failed; only $classCount class files were produced."
}

$l10nSrc = Join-Path $PluginRoot "OSGI-INF\l10n"
$l10nDest = Join-Path $bin "OSGI-INF\l10n"
if(Test-Path $l10nSrc) {
	New-Item -ItemType Directory -Force -Path $l10nDest | Out-Null
	Copy-Item (Join-Path $l10nSrc "*") $l10nDest -Force
}

Write-Host "Compiled $classCount class files into $bin"