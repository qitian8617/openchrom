# Patches PDE bundle pool JARs with ChemClipse zh_CN localization resources.
param(
	[string]$ChemClipseRoot = "E:\OpenChrom\chemclipse\chemclipse",
	[string]$BundlePool = "E:\OpenChrom\.metadata\.plugins\org.eclipse.pde.core\.bundle_pool\plugins"
)

if(-not (Test-Path $ChemClipseRoot)) {
	throw "ChemClipse root not found: $ChemClipseRoot"
}
if(-not (Test-Path $BundlePool)) {
	throw "PDE bundle pool not found: $BundlePool"
}

$localizationFiles = Get-ChildItem -Path (Join-Path $ChemClipseRoot "plugins") -Recurse -File -Include "bundle_zh_CN.properties", "messages_zh_CN.properties" | Where-Object {
	$_.FullName -notmatch "\\(bin|target)\\"
}

$patchedCount = 0
$skippedCount = 0
foreach($file in $localizationFiles) {
	$pluginDirectory = $file.Directory
	while($null -ne $pluginDirectory -and -not (Test-Path (Join-Path $pluginDirectory.FullName "build.properties"))) {
		$pluginDirectory = $pluginDirectory.Parent
	}
	if($null -eq $pluginDirectory) {
		continue
	}

	$pluginName = $pluginDirectory.Name
	$productJar = Get-ChildItem (Join-Path $BundlePool "$pluginName*.jar") -ErrorAction SilentlyContinue | Where-Object {
		$_.Name -notmatch "sources|javadoc"
	} | Sort-Object LastWriteTime -Descending | Select-Object -First 1
	if($null -eq $productJar) {
		$skippedCount++
		continue
	}

	$entry = $file.FullName.Substring($pluginDirectory.FullName.Length + 1).Replace("\", "/")
	$patchRoot = Join-Path $env:TEMP ("laiendechrom-pde-l10n-" + [Guid]::NewGuid().ToString("N"))
	$entryDirectory = Split-Path $entry -Parent
	New-Item -ItemType Directory -Force -Path (Join-Path $patchRoot $entryDirectory) | Out-Null
	Copy-Item -LiteralPath $file.FullName -Destination (Join-Path $patchRoot $entry) -Force

	Push-Location $patchRoot
	try {
		& jar uf $productJar.FullName $entry
		if($LASTEXITCODE -ne 0) {
			throw "Failed to update $entry in $($productJar.FullName)"
		}
		$patchedCount++
		Write-Host "Patched $($productJar.Name) -> $entry"
	} finally {
		Pop-Location
		if(Test-Path $patchRoot) {
			Remove-Item $patchRoot -Recurse -Force
		}
	}
}

Write-Host "Patched $patchedCount JAR entries ($skippedCount plugins without matching JAR)."
