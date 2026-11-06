[CmdletBinding()]
param(
	[switch]$SkipProductBuild,
	[string]$ProductDir,
	[string]$OutputDir,
	[string]$ISCCPath,
	[string[]]$MavenArguments = @("-f", "releng\net.openchrom.aggregator\pom.xml", "clean", "install", "-DskipTests", "-Dtycho.target.version=1.6.20-SNAPSHOT")
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptRoot "..\..\..")).Path
$targetPlatformPom = Join-Path $repoRoot "releng\net.openchrom.targetplatform\pom.xml"
$productProject = Join-Path $repoRoot "products\net.openchrom.rcp.compilation.community.product"
$productTarget = Join-Path $productProject "target"
$installerScript = Join-Path $scriptRoot "laiendechrom.iss"

if([string]::IsNullOrWhiteSpace($OutputDir)) {
	$OutputDir = Join-Path $productTarget "installer"
}

function Resolve-IsccPath {
	param([string]$ConfiguredPath)

	if(-not [string]::IsNullOrWhiteSpace($ConfiguredPath)) {
		if(Test-Path $ConfiguredPath) {
			return (Resolve-Path $ConfiguredPath).Path
		}
		throw "ISCC.exe was not found at '$ConfiguredPath'."
	}

	$command = Get-Command "ISCC.exe" -ErrorAction SilentlyContinue
	if($command -ne $null) {
		return $command.Source
	}

	$commonPaths = @(
		"$env:LOCALAPPDATA\Programs\Inno Setup 6\ISCC.exe",
		"$env:LOCALAPPDATA\Programs\JRSoftware\Inno Setup 6\ISCC.exe",
		"$env:ProgramData\Microsoft\WinGet\Packages\JRSoftware.InnoSetup_Microsoft.Winget.Source_8wekyb3d8bbwe\ISCC.exe",
		"${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe",
		"$env:ProgramFiles\Inno Setup 6\ISCC.exe"
	)

	foreach($path in $commonPaths) {
		if(Test-Path $path) {
			return (Resolve-Path $path).Path
		}
	}

	throw "ISCC.exe was not found. Install Inno Setup 6 or pass -ISCCPath."
}

function Assert-MavenJava21 {
	$mavenVersion = (& mvn -version) | Out-String
	if($LASTEXITCODE -ne 0) {
		throw "Maven is not available on PATH."
	}

	if($mavenVersion -notmatch "Java version:\s*(\d+)") {
		throw "Unable to determine Maven Java version.`n$mavenVersion"
	}

	$majorVersion = [int]$Matches[1]
	if($majorVersion -lt 21) {
		throw "Maven must run with Java 21 or newer. Current Maven runtime:`n$mavenVersion"
	}
}

function Resolve-ProductDirectory {
	param([string]$ConfiguredProductDir)

	if(-not [string]::IsNullOrWhiteSpace($ConfiguredProductDir)) {
		if(-not (Test-Path (Join-Path $ConfiguredProductDir "laiendechrom.exe"))) {
			throw "The configured product directory does not contain laiendechrom.exe: $ConfiguredProductDir"
		}
		return (Resolve-Path $ConfiguredProductDir).Path
	}

	$productsRoot = Join-Path $productTarget "products"
	if(-not (Test-Path $productsRoot)) {
		throw "Tycho product output was not found at '$productsRoot'. Build the product first or pass -ProductDir."
	}

	$windowsZip = Join-Path $productsRoot "net.openchrom.rcp.compilation.community.product.id-win32.win32.x86_64.zip"
	if(Test-Path $windowsZip) {
		$stagingRoot = "C:\ocpkg"
		if(Test-Path $stagingRoot) {
			Remove-Item -Path $stagingRoot -Recurse -Force
		}
		New-Item -ItemType Directory -Force -Path $stagingRoot | Out-Null
		Add-Type -AssemblyName System.IO.Compression.FileSystem
		[System.IO.Compression.ZipFile]::ExtractToDirectory($windowsZip, $stagingRoot)
		$launcher = Get-ChildItem -Path $stagingRoot -Recurse -Filter "laiendechrom.exe" -File -ErrorAction SilentlyContinue | Select-Object -First 1
		if($launcher -ne $null) {
			return $launcher.Directory.FullName
		}
		throw "The Windows product archive does not contain laiendechrom.exe: $windowsZip"
	}

	$launcher = Get-ChildItem -Path $productsRoot -Recurse -Filter "laiendechrom.exe" -File -ErrorAction SilentlyContinue | Where-Object {
		$_.FullName -like "*\win32\win32\x86_64\laiendechrom.exe"
	} | Select-Object -First 1
	if($launcher -eq $null) {
		$launcher = Get-ChildItem -Path $productsRoot -Recurse -Filter "laiendechrom.exe" -File -ErrorAction SilentlyContinue | Select-Object -First 1
	}
	if($launcher -eq $null) {
		throw "Could not find laiendechrom.exe below '$productsRoot'."
	}

	return $launcher.Directory.FullName
}

function Resolve-ChemClipseRoot {
	param([string]$RepoRoot)

	if(-not [string]::IsNullOrWhiteSpace($env:CHEMCLIPSE_ROOT)) {
		return (Resolve-Path $env:CHEMCLIPSE_ROOT).Path
	}

	$openChromRoot = Split-Path $RepoRoot -Parent
	$candidate = Join-Path (Split-Path $openChromRoot -Parent) "chemclipse\chemclipse"
	if(Test-Path $candidate) {
		return (Resolve-Path $candidate).Path
	}

	return $null
}

function Get-PatchSourceFiles {
	param(
		[string]$ChemclipseRoot,
		[string]$PluginName,
		[string[]]$Entries
	)

	$sourceFiles = New-Object 'System.Collections.Generic.HashSet[string]'
	foreach($entry in $Entries) {
		if($entry -eq "fragment.e4xmi" -or $entry -notmatch '\.class$' -or $entry -match '`\$') {
			continue
		}
		$relativeJava = ($entry -replace '\.class$','.java').Replace("/", "\")
		$sourcePath = Join-Path $ChemclipseRoot "plugins\$PluginName\src\$relativeJava"
		if(Test-Path $sourcePath) {
			[void]$sourceFiles.Add($sourcePath)
		}
	}
	return @($sourceFiles)
}

function Compile-ChemClipsePluginsToBin {
	param(
		[string]$ChemclipseRoot,
		[hashtable]$PatchEntriesByPlugin,
		[string]$ProductPlugins
	)

	$classpathParts = @((Get-ChildItem $ProductPlugins -Filter "*.jar" | ForEach-Object { $_.FullName }))
	foreach($pluginName in $PatchEntriesByPlugin.Keys) {
		$bin = Join-Path $ChemclipseRoot "plugins\$pluginName\bin"
		if(Test-Path $bin) {
			$classpathParts += $bin
		}
	}
	$classpath = $classpathParts -join ";"

	foreach($pluginName in $PatchEntriesByPlugin.Keys) {
		$javaFiles = Get-PatchSourceFiles -ChemclipseRoot $ChemclipseRoot -PluginName $pluginName -Entries $PatchEntriesByPlugin[$pluginName]
		if($javaFiles.Count -eq 0) {
			continue
		}
		$bin = Join-Path $ChemclipseRoot "plugins\$pluginName\bin"
		New-Item -ItemType Directory -Force -Path $bin | Out-Null
		$argFile = Join-Path $env:TEMP ("laiendechrom-javac-" + $pluginName + ".txt")
		$lines = @("-encoding", "UTF-8", "-source", "21", "-target", "21", "-cp", $classpath, "-d", $bin) + $javaFiles
		[System.IO.File]::WriteAllLines($argFile, $lines)
		Write-Host "Compiling patched sources in $pluginName ..."
		& javac "-proc:none" "@$argFile"
		if($LASTEXITCODE -ne 0) {
			throw "javac failed for $pluginName with exit code $LASTEXITCODE."
		}
		$classpath = "$classpath;$bin"
	}
}

function Update-PatchedChemClipsePlugins {
	param(
		[string]$ProductDir,
		[string]$RepoRoot
	)

	$chemclipseRoot = Resolve-ChemClipseRoot $RepoRoot
	if($null -eq $chemclipseRoot) {
		Write-Warning "ChemClipse source was not found. Data Explorer fixes require a local ChemClipse checkout at E:\OpenChrom\chemclipse\chemclipse or CHEMCLIPSE_ROOT."
		return
	}

	$pluginsToPatch = @(
		"org.eclipse.chemclipse.support.ui",
		"org.eclipse.chemclipse.ux.extension.ui",
		"org.eclipse.chemclipse.ux.extension.xxd.ui",
		"org.eclipse.chemclipse.processing.ui",
		"org.eclipse.chemclipse.chromatogram.xxd.calculator.peak.resolution.ui",
		"org.eclipse.chemclipse.ux.extension.msd.ui",
		"org.eclipse.chemclipse.ux.extension.wsd.ui"
	)

	Write-Host "Compiling patched ChemClipse plugins from $chemclipseRoot ..."
	$productPlugins = Join-Path $ProductDir "plugins"
	if(-not (Test-Path $productPlugins)) {
		throw "Product plugins directory was not found: $productPlugins"
	}
	$patchEntriesByPlugin = @{
		"org.eclipse.chemclipse.support.ui" = @(
			"org/eclipse/chemclipse/support/ui/activator/AbstractActivatorUI.class",
			"org/eclipse/chemclipse/support/ui/swt/ControlBuilder.class",
			"org/eclipse/chemclipse/support/ui/swt/ControlBuilder`$1.class",
			"org/eclipse/chemclipse/support/ui/swt/ControlBuilder`$2.class",
			"org/eclipse/chemclipse/support/ui/swt/columns/ColumnBuilder.class",
			"org/eclipse/chemclipse/support/ui/swt/columns/ColumnBuilder`$1.class",
			"org/eclipse/chemclipse/support/ui/workbench/DisplayUtils.class",
			"org/eclipse/chemclipse/support/ui/workbench/DisplayUtils`$1.class"
		)
		"org.eclipse.chemclipse.ux.extension.ui" = @(
			"org/eclipse/chemclipse/ux/extension/ui/swt/DataExplorerTreeRoot.class",
			"org/eclipse/chemclipse/ux/extension/ui/swt/DataExplorerTreeUI.class",
			"org/eclipse/chemclipse/ux/extension/ui/swt/MultiDataExplorerTreeUI.class",
			"org/eclipse/chemclipse/ux/extension/ui/preferences/PreferenceSupplierDataExplorer.class"
		)
		"org.eclipse.chemclipse.ux.extension.xxd.ui" = @(
			"org/eclipse/chemclipse/ux/extension/xxd/ui/parts/DataExplorerPart.class",
			"org/eclipse/chemclipse/ux/extension/xxd/ui/internal/provider/IntegerColumnEditingSupport.class",
			"org/eclipse/chemclipse/ux/extension/xxd/ui/swt/MeasurementResultUI.class",
			"org/eclipse/chemclipse/ux/extension/xxd/ui/swt/MeasurementResultUI`$1.class"
		)
		"org.eclipse.chemclipse.processing.ui" = @(
			"org/eclipse/chemclipse/processing/ui/parts/ProcessingInfoPart.class",
			"org/eclipse/chemclipse/processing/ui/E4ProcessSupplierContext.class"
		)
		"org.eclipse.chemclipse.chromatogram.xxd.calculator.peak.resolution.ui" = @(
			"org/eclipse/chemclipse/chromatogram/xxd/calculator/peak/resolution/ui/provider/MeasurementResultTitles.class",
			"org/eclipse/chemclipse/chromatogram/xxd/calculator/peak/resolution/ui/provider/MeasurementResultTitles`$1.class",
			"org/eclipse/chemclipse/chromatogram/xxd/calculator/peak/resolution/ui/provider/MeasurementResultTitles`$2.class",
			"org/eclipse/chemclipse/chromatogram/xxd/calculator/peak/resolution/ui/provider/MeasurementResultTitles`$3.class",
			"org/eclipse/chemclipse/chromatogram/xxd/calculator/peak/resolution/ui/provider/MeasurementResultTitles`$4.class",
			"org/eclipse/chemclipse/chromatogram/xxd/calculator/peak/resolution/ui/provider/MeasurementResultTitles`$5.class",
			"org/eclipse/chemclipse/chromatogram/xxd/calculator/peak/resolution/ui/provider/MeasurementResultTitles`$6.class",
			"org/eclipse/chemclipse/chromatogram/xxd/calculator/peak/resolution/ui/provider/MeasurementResultTitles`$PeakIndex.class",
			"org/eclipse/chemclipse/chromatogram/xxd/calculator/peak/resolution/ui/provider/PeakResolutionResultsLabelProvider.class",
			"org/eclipse/chemclipse/chromatogram/xxd/calculator/peak/resolution/ui/provider/PeakResolutionResultsContentProvider.class",
			"org/eclipse/chemclipse/chromatogram/xxd/calculator/peak/resolution/ui/internal/PeakResolutionAdapterFactory.class"
		)
		"org.eclipse.chemclipse.ux.extension.msd.ui" = @(
			"fragment.e4xmi"
		)
		"org.eclipse.chemclipse.ux.extension.wsd.ui" = @(
			"fragment.e4xmi"
		)
	}
	Compile-ChemClipsePluginsToBin -ChemclipseRoot $chemclipseRoot -PatchEntriesByPlugin $patchEntriesByPlugin -ProductPlugins $productPlugins

	foreach($pluginName in $pluginsToPatch) {
		$productJar = Get-ChildItem -Path $productPlugins -Filter "${pluginName}_*.jar" -ErrorAction SilentlyContinue | Where-Object {
			$_.Name -notmatch "sources|javadoc"
		} | Sort-Object LastWriteTime -Descending | Select-Object -First 1
		if($null -eq $productJar) {
			throw "Product jar not found for $pluginName in $productPlugins"
		}

		$patchRoot = Join-Path $env:TEMP ("laiendechrom-patch-" + $pluginName)
		if(Test-Path $patchRoot) {
			Remove-Item $patchRoot -Recurse -Force
		}
		New-Item -ItemType Directory -Path $patchRoot | Out-Null
		Push-Location $patchRoot
		try {
			foreach($entry in $patchEntriesByPlugin[$pluginName]) {
				$sourcePath = if($entry -eq "fragment.e4xmi") {
					Join-Path $chemclipseRoot "plugins\$pluginName\$entry"
				} else {
					Join-Path (Join-Path $chemclipseRoot "plugins\$pluginName\bin") ($entry.Replace("/", "\"))
				}
				if(-not (Test-Path $sourcePath)) {
					throw "Patch source missing for ${pluginName}: $sourcePath"
				}
				$destPath = Join-Path $patchRoot ($entry.Replace("/", "\"))
				$destDirectory = Split-Path $destPath -Parent
				if(-not [string]::IsNullOrWhiteSpace($destDirectory)) {
					New-Item -ItemType Directory -Force -Path $destDirectory | Out-Null
				}
				Copy-Item -LiteralPath $sourcePath -Destination $destPath -Force
			}
			foreach($entry in $patchEntriesByPlugin[$pluginName]) {
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

		Write-Host "Patched product plugin in place: $($productJar.Name)"
	}
}

function Update-PatchedSwtChartPlugins {
	param(
		[string]$ProductDir,
		[string]$RepoRoot
	)

	$sourceFile = Join-Path (Split-Path $RepoRoot -Parent) "_tmp_swtchart\org\eclipse\swtchart\extensions\core\ScrollableChart.java"
	if(-not (Test-Path $sourceFile)) {
		Write-Warning "Patched ScrollableChart source was not found: $sourceFile"
		return
	}

	$productPlugins = Join-Path $ProductDir "plugins"
	if(-not (Test-Path $productPlugins)) {
		throw "Product plugins directory was not found: $productPlugins"
	}

	$productJar = Get-ChildItem -Path $productPlugins -Filter "org.eclipse.swtchart.extensions_*.jar" -ErrorAction SilentlyContinue | Where-Object {
		$_.Name -notmatch "sources|javadoc"
	} | Sort-Object LastWriteTime -Descending | Select-Object -First 1
	if($null -eq $productJar) {
		throw "Product jar not found for org.eclipse.swtchart.extensions in $productPlugins"
	}

	$patchRoot = Join-Path $env:TEMP "laiendechrom-patch-swtchart"
	if(Test-Path $patchRoot) {
		Remove-Item $patchRoot -Recurse -Force
	}
	New-Item -ItemType Directory -Force -Path $patchRoot | Out-Null

	$swtChartPatchBin = Join-Path (Split-Path $RepoRoot -Parent) "_tmp_swtchart\bin"
	$productPluginClasspath = Join-Path $productPlugins "*"
	$classPath = @($swtChartPatchBin, $productPluginClasspath) -join ";"
	& javac "-proc:none" "-encoding" "UTF-8" "-source" "21" "-target" "21" "-cp" $classPath "-d" $patchRoot $sourceFile
	if($LASTEXITCODE -ne 0) {
		throw "ScrollableChart patch build failed with exit code $LASTEXITCODE."
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

	Write-Host "Patched product plugin in place: $($productJar.Name)"
}

function Update-PatchedChemClipseLocalization {
	param(
		[string]$ProductDir,
		[string]$RepoRoot
	)

	$chemclipseRoot = Resolve-ChemClipseRoot $RepoRoot
	if($null -eq $chemclipseRoot) {
		Write-Warning "ChemClipse source was not found. Chinese localization patches require a local ChemClipse checkout at E:\OpenChrom\chemclipse\chemclipse or CHEMCLIPSE_ROOT."
		return
	}

	$productPlugins = Join-Path $ProductDir "plugins"
	if(-not (Test-Path $productPlugins)) {
		throw "Product plugins directory was not found: $productPlugins"
	}

	$localizationFiles = Get-ChildItem -Path (Join-Path $chemclipseRoot "plugins") -Recurse -File -Include "bundle_zh_CN.properties", "messages_zh_CN.properties" | Where-Object {
		$_.FullName -notmatch "\\(bin|target)\\"
	}

	$patchedCount = 0
	foreach($file in $localizationFiles) {
		$pluginDirectory = $file.Directory
		while($pluginDirectory -ne $null -and -not (Test-Path (Join-Path $pluginDirectory.FullName "build.properties"))) {
			$pluginDirectory = $pluginDirectory.Parent
		}
		if($pluginDirectory -eq $null) {
			continue
		}

		$pluginName = $pluginDirectory.Name
		$productJar = Get-ChildItem -Path $productPlugins -Filter "${pluginName}_*.jar" -ErrorAction SilentlyContinue | Where-Object {
			$_.Name -notmatch "sources|javadoc"
		} | Sort-Object LastWriteTime -Descending | Select-Object -First 1
		if($null -eq $productJar) {
			continue
		}

		$entry = $file.FullName.Substring($pluginDirectory.FullName.Length + 1).Replace("\", "/")
		$patchRoot = Join-Path $env:TEMP ("laiendechrom-localization-" + [Guid]::NewGuid().ToString("N"))
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
		} finally {
			Pop-Location
			if(Test-Path $patchRoot) {
				Remove-Item $patchRoot -Recurse -Force
			}
		}
	}

	Write-Host "Patched ChemClipse Chinese localization resources: $patchedCount"
}

if(-not $SkipProductBuild) {
	Assert-MavenJava21
	Push-Location $repoRoot
	try {
		& mvn "-f" $targetPlatformPom "clean" "install"
		if($LASTEXITCODE -ne 0) {
			throw "Target platform build failed with exit code $LASTEXITCODE."
		}

		& mvn @MavenArguments
		if($LASTEXITCODE -ne 0) {
			throw "Maven product build failed with exit code $LASTEXITCODE."
		}
	} finally {
		Pop-Location
	}
}

$iscc = Resolve-IsccPath $ISCCPath
$resolvedProductDir = Resolve-ProductDirectory $ProductDir
Update-PatchedChemClipsePlugins -ProductDir $resolvedProductDir -RepoRoot $repoRoot
Update-PatchedChemClipseLocalization -ProductDir $resolvedProductDir -RepoRoot $repoRoot
Update-PatchedSwtChartPlugins -ProductDir $resolvedProductDir -RepoRoot $repoRoot
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$generatedInstallerScript = Join-Path $OutputDir "laiendechrom.generated.iss"
Get-Content $installerScript -Raw -Encoding UTF8 | Set-Content $generatedInstallerScript -Encoding UTF8

& $iscc "/DProductDir=$resolvedProductDir" "/DOutputDir=$OutputDir" "/DRepoRoot=$repoRoot" $generatedInstallerScript
if($LASTEXITCODE -ne 0) {
	throw "Inno Setup compiler failed with exit code $LASTEXITCODE."
}

$setupPath = Join-Path $OutputDir "LaiendeChrom_Setup.exe"
Write-Host "Created installer: $setupPath"
