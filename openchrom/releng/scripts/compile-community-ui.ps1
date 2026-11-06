# Compiles net.openchrom.rcp.compilation.community.ui into its bin/ folder for PDE runs.
param(
	[string]$PluginRoot = "E:\OpenChrom\openchrom\openchrom\plugins\net.openchrom.rcp.compilation.community.ui",
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

# Eclipse project encoding is UTF-8. Convert any accidental UTF-16 sources.
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$files = @(Get-ChildItem $src -Recurse -Filter "*.java" | ForEach-Object { $_.FullName })
foreach($file in $files) {
	$bytes = [System.IO.File]::ReadAllBytes($file)
	$isUtf16Bom = ($bytes.Length -ge 2 -and $bytes[0] -eq 0xFF -and $bytes[1] -eq 0xFE)
	$isUtf16Le = (-not $isUtf16Bom -and $bytes.Length -ge 4 -and $bytes[1] -eq 0 -and $bytes[3] -eq 0 -and $bytes[0] -ne 0)
	if($isUtf16Bom -or $isUtf16Le) {
		$text = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::Unicode)
		[System.IO.File]::WriteAllText($file, $text, $utf8NoBom)
		Write-Host "Converted UTF-16 to UTF-8: $file"
	}
}

# Exclude *.source_*.jar — javac may try to parse them and fail on missing annotation packages.
$cpJars = @(Get-ChildItem $BundlePool -Filter "*.jar" | Where-Object { $_.Name -notmatch "\.source_" } | ForEach-Object { $_.FullName })
$cp = (@($bin) + $cpJars) -join ";"
$argFile = Join-Path $env:TEMP "laiende-community-ui-javac.txt"
$lines = @("-encoding", "UTF-8", "-source", "21", "-target", "21", "-proc:none", "-Xlint:none", "-cp", $cp, "-d", $bin) + $files
[System.IO.File]::WriteAllLines($argFile, $lines)

$errFile = Join-Path $env:TEMP "laiende-community-ui-javac.err"
& javac "@$argFile" 2>$errFile
if($LASTEXITCODE -ne 0) {
	Get-Content $errFile -ErrorAction SilentlyContinue | Select-Object -Last 40
	throw "Community UI javac failed with exit code $LASTEXITCODE"
}

$classCount = (Get-ChildItem $bin -Recurse -Filter "*.class" -ErrorAction SilentlyContinue).Count
if($classCount -lt 10) {
	throw "Community UI compile failed; only $classCount class files were produced."
}

$l10nSrc = Join-Path $PluginRoot "OSGI-INF\l10n"
$l10nDest = Join-Path $bin "OSGI-INF\l10n"
if(Test-Path $l10nSrc) {
	New-Item -ItemType Directory -Force -Path $l10nDest | Out-Null
	Copy-Item (Join-Path $l10nSrc "*") $l10nDest -Force
}
$loggingSrc = Join-Path $PluginRoot "OSGI-INF\logging.properties"
$loggingDest = Join-Path $PluginRoot "bin\OSGI-INF\logging.properties"
if(Test-Path $loggingSrc) {
	New-Item -ItemType Directory -Force -Path (Split-Path $loggingDest) | Out-Null
	Copy-Item $loggingSrc $loggingDest -Force
}

Write-Host "Compiled $classCount class files into $bin"

& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "patch-pde-help-server.ps1")
