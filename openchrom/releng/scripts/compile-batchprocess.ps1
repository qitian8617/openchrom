# Compiles batch process plugins into bin/ for PDE runs.
param(
	[string]$ChemClipseRoot = "E:\OpenChrom\chemclipse\chemclipse\plugins",
	[string]$BundlePool = "E:\OpenChrom\.metadata\.plugins\org.eclipse.pde.core\.bundle_pool\plugins"
)

if(-not (Test-Path $BundlePool)) {
	throw "PDE bundle pool not found: $BundlePool"
}

function Compile-Plugin {
	param(
		[string]$PluginRoot
	)

	if(-not (Test-Path $PluginRoot)) {
		throw "Plugin root not found: $PluginRoot"
	}

	$bin = Join-Path $PluginRoot "bin"
	$src = Join-Path $PluginRoot "src"
	New-Item -ItemType Directory -Force -Path $bin | Out-Null
	$cp = (Get-ChildItem $BundlePool -Filter "*.jar" | Where-Object { $_.Name -notmatch '\.source_' } | ForEach-Object { $_.FullName }) -join ";"
	$files = Get-ChildItem $src -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
	if($files.Count -eq 0) {
		Write-Host "No Java sources in $PluginRoot"
		return
	}

	$argFile = Join-Path $env:TEMP ("laiende-batchprocess-javac-{0}.txt" -f ([IO.Path]::GetFileName($PluginRoot)))
	$lines = @("-encoding", "UTF-8", "-source", "21", "-target", "21", "-cp", $cp, "-d", $bin) + $files
	[System.IO.File]::WriteAllLines($argFile, $lines)

	& javac "-proc:none" "@$argFile" 2>&1 | Out-Null
	$classCount = (Get-ChildItem $bin -Recurse -Filter "*.class" -ErrorAction SilentlyContinue).Count
	if($classCount -lt 1) {
		throw "Compile failed for $PluginRoot; no class files were produced."
	}

	Get-ChildItem $src -Recurse -Filter "*.properties" | ForEach-Object {
		$relative = $_.FullName.Substring($src.Length + 1)
		$target = Join-Path $bin $relative
		New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null
		Copy-Item $_.FullName $target -Force
	}

	Write-Host "Compiled $classCount class files into $bin"
}

Compile-Plugin (Join-Path $ChemClipseRoot "org.eclipse.chemclipse.chromatogram.xxd.process.supplier.batchprocess")
Compile-Plugin (Join-Path $ChemClipseRoot "org.eclipse.chemclipse.chromatogram.xxd.process.supplier.batchprocess.ui")