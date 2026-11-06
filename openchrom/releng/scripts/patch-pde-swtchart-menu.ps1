# Patches org.eclipse.swtchart.extensions in the PDE bundle pool (ScrollableChart menu l10n).
param(
	[string]$BundlePool = "E:\OpenChrom\.metadata\.plugins\org.eclipse.pde.core\.bundle_pool\plugins",
	[string]$SourceFile = "E:\OpenChrom\openchrom\_tmp_swtchart\org\eclipse\swtchart\extensions\core\ScrollableChart.java",
	[string]$PatchBin = "E:\OpenChrom\openchrom\_tmp_swtchart\bin"
)

if(-not (Test-Path $SourceFile)) {
	throw "ScrollableChart source not found: $SourceFile"
}

$productJar = Get-ChildItem (Join-Path $BundlePool "org.eclipse.swtchart.extensions*.jar") -ErrorAction SilentlyContinue | Where-Object {
	$_.Name -notmatch "sources|javadoc"
} | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if($null -eq $productJar) {
	throw "org.eclipse.swtchart.extensions jar not found in $BundlePool"
}

$patchRoot = Join-Path $env:TEMP "laiendechrom-patch-swtchart-menu"
if(Test-Path $patchRoot) {
	Remove-Item $patchRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $patchRoot | Out-Null

$classPath = @($PatchBin, (Join-Path $BundlePool "*")) -join ";"
# Give javac an explicit, valid heap. On low-memory machines the JVM ergonomic
# defaults can fail with "GC triggered before VM initialization completed".
$javacJvmArgs = @("-J-Xms64m", "-J-Xmx512m")
& javac @javacJvmArgs "-encoding" "UTF-8" "-source" "21" "-target" "21" "-cp" $classPath "-d" $patchRoot $SourceFile
if($LASTEXITCODE -ne 0) {
	Write-Warning "ScrollableChart compile failed with exit code $LASTEXITCODE."
	Write-Warning "This patch only localizes chart menu labels (cosmetic); submenu availability is unaffected."
	Write-Warning "Skipping SWTChart patch. Free up memory and re-run patch-pde-swtchart-menu.ps1 later if needed."
	return
}

$entries = Get-ChildItem (Join-Path $patchRoot "org\eclipse\swtchart\extensions\core") -Filter "ScrollableChart*.class" | ForEach-Object {
	"org/eclipse/swtchart/extensions/core/" + $_.Name
}

Push-Location $patchRoot
try {
	foreach($entry in $entries) {
		& jar uf $productJar.FullName $entry
		if($LASTEXITCODE -ne 0) {
			throw "Failed to update $entry in $($productJar.FullName)"
		}
	}
} finally {
	Pop-Location
	if(Test-Path $patchRoot) {
		Remove-Item $patchRoot -Recurse -Force
	}
}

Write-Host "Patched $($productJar.Name) with $($entries.Count) ScrollableChart class files."
