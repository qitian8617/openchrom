# Keeps the PDE product launch aligned with Laiende workspace plugins.
param(
	[string]$Workspace = "E:\OpenChrom",
	[string]$LaunchName = "openchrom.compilation.community.product.launch",
	[string]$ProductConfigName = "openchrom.compilation.community.product"
)

$launchFile = Join-Path $Workspace ".metadata\.plugins\org.eclipse.debug.core\.launches\$LaunchName"
$devProperties = Join-Path $Workspace ".metadata\.plugins\org.eclipse.pde.core\$ProductConfigName\dev.properties"
$runtimeDir = Join-Path $Workspace "runtime\$ProductConfigName"

if(-not (Test-Path $launchFile)) {
	throw "Launch configuration not found: $launchFile"
}

$content = Get-Content $launchFile -Raw -Encoding UTF8
$content = $content -replace '<booleanAttribute key="clearConfig" value="true"/>', '<booleanAttribute key="clearConfig" value="false"/>'
$content = $content -replace '<stringAttribute key="location" value="\$\{workspace_loc\}/\.\./runtime-openchrom\.compilation\.community\.product"/>', '<stringAttribute key="location" value="${workspace_loc}/runtime/openchrom.compilation.community.product"/>'
$content = $content -replace '<booleanAttribute key="append\.args" value="true"/>', '<booleanAttribute key="append.args" value="false"/>'
Set-Content -Path $launchFile -Value $content -Encoding UTF8

$requiredWorkspaceBundles = @(
	"net.openchrom.rcp.compilation.community.ui@default:default",
	"net.openchrom.nl.zh.chemclipse.rcp.app.ui@default:default",
	"net.openchrom.nl.zh.chemclipse.ux.extension.xxd.ui@default:default",
	"net.openchrom.nl.zh.chemclipse.xxd.process.supplier.pca.ui@default:default",
	"net.openchrom.nl.zh.openchrom.xxd.process.supplier.templates.ui@default:default",
	"net.openchrom.nl.zh.chemclipse.chromatogram.msd.filter.supplier.subtract.ui@default:default",
	"net.openchrom.nl.zh.eclipse.help.base@default:default",
	"org.eclipse.chemclipse.rcp.app.ui@default:default",
	"org.eclipse.chemclipse.ux.extension.csd.ui@default:default",
	"org.eclipse.chemclipse.ux.extension.ui@default:default",
	"org.eclipse.chemclipse.ux.extension.xxd.ui@default:default",
	"org.eclipse.chemclipse.processing.ui@default:default",
	"org.eclipse.chemclipse.chromatogram.xxd.calculator.supplier.amdiscalri.ui@default:default",
	"org.eclipse.chemclipse.chromatogram.xxd.calculator.peak.resolution@default:default",
	"org.eclipse.chemclipse.chromatogram.xxd.process.supplier.batchprocess@default:default",
	"org.eclipse.chemclipse.chromatogram.xxd.process.supplier.batchprocess.ui@default:default",
	"org.eclipse.chemclipse.csd.model@default:default",
	"net.openchrom.xxd.control.supplier.gc.simulator@default:default",
	"net.openchrom.xxd.control.supplier.temperature.ui@default:default",
	"net.openchrom.xxd.process.supplier.templates@default:default",
	"net.openchrom.xxd.process.supplier.templates.ui@default:default",
	"net.openchrom.xxd.processor.supplier.tracecompare@default:default",
	"net.openchrom.xxd.processor.supplier.tracecompare.ui@default:default",
	"net.openchrom.chromatogram.msd.processor.supplier.massshiftdetector@default:default",
	"net.openchrom.chromatogram.msd.processor.supplier.massshiftdetector.ui@default:default"
)
$requiredAdditionalPlugins = @(
	"net.openchrom.xxd.process.supplier.templates:1.6.20.qualifier:default:false:default:default",
	"net.openchrom.xxd.process.supplier.templates.ui:1.6.20.qualifier:default:false:default:default",
	"net.openchrom.xxd.processor.supplier.tracecompare:1.6.20.qualifier:default:false:default:default",
	"net.openchrom.xxd.processor.supplier.tracecompare.ui:1.6.20.qualifier:default:false:default:default",
	"net.openchrom.chromatogram.msd.processor.supplier.massshiftdetector:1.6.20.qualifier:default:false:default:default",
	"net.openchrom.chromatogram.msd.processor.supplier.massshiftdetector.ui:1.6.20.qualifier:default:false:default:default",
	"org.eclipse.chemclipse.chromatogram.xxd.calculator.supplier.amdiscalri.ui:0.9.0.qualifier:default:false:default:default",
	"org.eclipse.chemclipse.ux.extension.csd.ui:0.9.0.qualifier:default:false:default:default",
	"org.eclipse.chemclipse.ux.extension.xxd.ui:0.9.0.qualifier:default:true:default:default",
	"slf4j.simple:2.0.18:default:true:2:true",
	"org.apache.aries.spifly.dynamic.bundle:1.3.7:default:true:1:true",
	"org.eclipse.equinox.http.jetty:3.9.600.v20251031-0358:default:true:5:true",
	"org.eclipse.equinox.http.registry:1.4.400.v20250611-0943:default:true:5:true",
	"org.eclipse.equinox.http.servlet:1.8.600.v20251111-0344:default:false:default:default",
	"org.eclipse.equinox.jsp.jasper:1.2.400.v20250720-1532:default:false:default:default",
	"org.eclipse.equinox.jsp.jasper.registry:1.3.300.v20250807-0756:default:false:default:default",
	"org.eclipse.help.webapp:3.12.400.v20260112-1030:default:false:default:default",
	"org.eclipse.jetty.ee8.security:12.1.9:default:false:default:default",
	"org.eclipse.jetty.ee8.server:12.1.9:default:false:default:default",
	"org.eclipse.jetty.ee8.servlet:12.1.9:default:false:default:default",
	"org.eclipse.jetty.http:12.1.9:default:false:default:default",
	"org.eclipse.jetty.io:12.1.9:default:false:default:default",
	"org.eclipse.jetty.security:12.1.9:default:false:default:default",
	"org.eclipse.jetty.server:12.1.9:default:false:default:default",
	"org.eclipse.jetty.session:12.1.9:default:false:default:default",
	"org.eclipse.jetty.servlet-api:4.0.9:default:false:default:default",
	"org.eclipse.jetty.util:12.1.9:default:false:default:default",
	"org.eclipse.help.base.nl_zh:4.26.0.v20230220105658:default:false:default:default",
	"org.eclipse.help.webapp.nl_zh:4.26.0.v20230220105658:default:false:default:default",
	"org.eclipse.help.ui.nl_zh:4.26.0.v20230220105658:default:false:default:default",
	"org.eclipse.help.nl_zh:4.26.0.v20230220105658:default:false:default:default"
)
$requiredSelectedTargetBundles = @(
	"org.apache.aries.spifly.dynamic.bundle@1:true",
	"slf4j.simple@2:true",
	"org.eclipse.equinox.http.jetty@5:true",
	"org.eclipse.equinox.http.registry@5:true",
	"org.eclipse.equinox.http.servlet@default:default",
	"org.eclipse.equinox.jsp.jasper@default:default",
	"org.eclipse.equinox.jsp.jasper.registry@default:default",
	"org.eclipse.help.webapp@default:default",
	"org.eclipse.jetty.ee8.security@default:default",
	"org.eclipse.jetty.ee8.server@default:default",
	"org.eclipse.jetty.ee8.servlet@default:default",
	"org.eclipse.jetty.http@default:default",
	"org.eclipse.jetty.io@default:default",
	"org.eclipse.jetty.security@default:default",
	"org.eclipse.jetty.server@default:default",
	"org.eclipse.jetty.session@default:default",
	"org.eclipse.jetty.servlet-api@default:default",
	"org.eclipse.jetty.util@default:default"
)
$bundleBlock = "    <setAttribute key=`"selected_workspace_bundles`">`r`n"
foreach($entry in $requiredWorkspaceBundles) {
	$bundleBlock += "        <setEntry value=`"$entry`"/>`r`n"
}
$bundleBlock += "    </setAttribute>"
$content = Get-Content $launchFile -Raw -Encoding UTF8
$content = [regex]::Replace(
	$content,
	'(?s)<setAttribute key="selected_workspace_bundles">.*?</setAttribute>',
	$bundleBlock
)
Set-Content -Path $launchFile -Value $content -Encoding UTF8

$content = Get-Content $launchFile -Raw -Encoding UTF8
if($content -match '(?s)<setAttribute key="additional_plugins">.*?</setAttribute>') {
	$existingBlock = $Matches[0]
	$entries = [regex]::Matches($existingBlock, 'value="([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
	$entrySet = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
	foreach($entry in $entries) { [void]$entrySet.Add($entry) }
	foreach($entry in $requiredAdditionalPlugins) { [void]$entrySet.Add($entry) }
	$pluginBlock = "    <setAttribute key=`"additional_plugins`">`r`n"
	foreach($entry in ($entrySet | Sort-Object)) {
		$pluginBlock += "        <setEntry value=`"$entry`"/>`r`n"
	}
	$pluginBlock += "    </setAttribute>"
	$content = [regex]::Replace($content, '(?s)<setAttribute key="additional_plugins">.*?</setAttribute>', $pluginBlock)
	Set-Content -Path $launchFile -Value $content -Encoding UTF8
}

$content = Get-Content $launchFile -Raw -Encoding UTF8
if($content -match '(?s)<setAttribute key="selected_target_bundles">.*?</setAttribute>') {
	$existingBlock = $Matches[0]
	$entries = [regex]::Matches($existingBlock, 'value="([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
	$entrySet = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
	foreach($entry in $entries) { [void]$entrySet.Add($entry) }
	foreach($entry in $requiredSelectedTargetBundles) { [void]$entrySet.Add($entry) }
	$bundleBlock = "    <setAttribute key=`"selected_target_bundles`">`r`n"
	foreach($entry in ($entrySet | Sort-Object)) {
		$bundleBlock += "        <setEntry value=`"$entry`"/>`r`n"
	}
	$bundleBlock += "    </setAttribute>"
	$content = [regex]::Replace($content, '(?s)<setAttribute key="selected_target_bundles">.*?</setAttribute>', $bundleBlock)
	Set-Content -Path $launchFile -Value $content -Encoding UTF8
}

$requiredDevEntries = @{
	"net.openchrom.rcp.compilation.community.ui" = "bin"
	"net.openchrom.rcp.compilation.community.ui;1.6.20.qualifier" = "bin"
	"org.eclipse.chemclipse.rcp.app.ui" = "bin"
	"org.eclipse.chemclipse.rcp.app.ui;0.9.0.qualifier" = "bin"
	"org.eclipse.chemclipse.ux.extension.csd.ui" = "bin"
	"org.eclipse.chemclipse.ux.extension.csd.ui;0.9.0.qualifier" = "bin"
	"org.eclipse.chemclipse.ux.extension.ui" = "bin"
	"org.eclipse.chemclipse.ux.extension.ui;0.9.0.qualifier" = "bin"
	"org.eclipse.chemclipse.ux.extension.xxd.ui" = "bin"
	"org.eclipse.chemclipse.ux.extension.xxd.ui;0.9.0.qualifier" = "bin"
	"org.eclipse.chemclipse.processing.ui" = "bin"
	"org.eclipse.chemclipse.processing.ui;0.9.0.qualifier" = "bin"
	"org.eclipse.chemclipse.chromatogram.xxd.calculator.peak.resolution" = "bin"
	"org.eclipse.chemclipse.chromatogram.xxd.calculator.peak.resolution;0.9.0.qualifier" = "bin"
	"org.eclipse.chemclipse.chromatogram.xxd.process.supplier.batchprocess" = "bin"
	"org.eclipse.chemclipse.chromatogram.xxd.process.supplier.batchprocess;0.9.0.qualifier" = "bin"
	"org.eclipse.chemclipse.chromatogram.xxd.process.supplier.batchprocess.ui" = "bin"
	"org.eclipse.chemclipse.chromatogram.xxd.process.supplier.batchprocess.ui;0.9.0.qualifier" = "bin"
	"org.eclipse.chemclipse.csd.model" = "bin"
	"org.eclipse.chemclipse.csd.model;0.9.0.qualifier" = "bin"
	"net.openchrom.nl.zh.chemclipse.rcp.app.ui" = "bin"
	"net.openchrom.nl.zh.chemclipse.rcp.app.ui;1.0.0.qualifier" = "bin"
	"net.openchrom.nl.zh.chemclipse.ux.extension.xxd.ui" = "bin"
	"net.openchrom.nl.zh.chemclipse.ux.extension.xxd.ui;1.0.0.qualifier" = "bin"
	"net.openchrom.nl.zh.chemclipse.xxd.process.supplier.pca.ui" = "bin"
	"net.openchrom.nl.zh.chemclipse.xxd.process.supplier.pca.ui;1.0.0.qualifier" = "bin"
	"net.openchrom.nl.zh.openchrom.xxd.process.supplier.templates.ui" = "bin"
	"net.openchrom.nl.zh.openchrom.xxd.process.supplier.templates.ui;1.0.0.qualifier" = "bin"
	"net.openchrom.nl.zh.chemclipse.chromatogram.msd.filter.supplier.subtract.ui" = "bin"
	"net.openchrom.nl.zh.chemclipse.chromatogram.msd.filter.supplier.subtract.ui;1.0.0.qualifier" = "bin"
	"net.openchrom.nl.zh.eclipse.help.base" = "bin"
	"net.openchrom.nl.zh.eclipse.help.base;1.0.0.qualifier" = "bin"
}

if(Test-Path $devProperties) {
	$lines = Get-Content $devProperties -Encoding UTF8
	$map = @{}
	foreach($line in $lines) {
		if($line -match '^\s*#' -or $line -match '^\s*$' -or $line -notmatch '=') {
			continue
		}
		$parts = $line.Split('=', 2)
		$map[$parts[0]] = $parts[1]
	}
	foreach($entry in $requiredDevEntries.GetEnumerator()) {
		$map[$entry.Key] = $entry.Value
	}
	$header = Get-Content $devProperties -Encoding UTF8 | Where-Object { $_ -match '^\s*#' -or $_ -match '@ignoredot@' }
	$body = $map.GetEnumerator() | Sort-Object Name | ForEach-Object { "$($_.Key)=$($_.Value)" }
	($header + $body) | Set-Content -Path $devProperties -Encoding UTF8
}

New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null

$loggingPropsSource = Join-Path $PSScriptRoot "..\..\plugins\net.openchrom.rcp.compilation.community.ui\OSGI-INF\logging.properties"
$pdeConfigDir = Join-Path $Workspace ".metadata\.plugins\org.eclipse.pde.core\$ProductConfigName"
if(Test-Path $loggingPropsSource) {
	Copy-Item $loggingPropsSource (Join-Path $pdeConfigDir "logging.properties") -Force
}
$loggingPropsTarget = (Join-Path $pdeConfigDir "logging.properties") -replace '\\', '/'
$productFile = Join-Path $PSScriptRoot "..\..\products\net.openchrom.rcp.compilation.community.product\openchrom.compilation.community.product"
if(Test-Path $productFile) {
	$productContent = Get-Content $productFile -Raw -Encoding UTF8
	$strippedProduct = $productContent -replace '\s*-Dslf4j\.provider=[^\r\n]+', ''
	if($strippedProduct -ne $productContent) {
		Set-Content -Path $productFile -Value $strippedProduct -Encoding UTF8
	}
}
$content = Get-Content $launchFile -Raw -Encoding UTF8
$vmArgsToEnsure = @(
	"-Dorg.slf4j.simpleLogger.defaultLogLevel=warn",
	"-Djava.util.logging.config.file=$loggingPropsTarget",
	"-Xms256m",
	"-Xmx2048m"
)
if($content -match 'org\.eclipse\.jdt\.launching\.VM_ARGUMENTS" value="([^"]*)"') {
	$vmArgs = $Matches[1] -replace '\s*-Dslf4j\.provider=[^\s]+', ''
	foreach($vmArg in $vmArgsToEnsure) {
		$flag = ($vmArg -split '=')[0]
		if($vmArgs -notlike "*$flag=*") {
			$vmArgs += " $vmArg"
		}
	}
	$vmArgs = $vmArgs.Trim()
	$content = [regex]::Replace($content, 'org\.eclipse\.jdt\.launching\.VM_ARGUMENTS" value="[^"]*"', "org.eclipse.jdt.launching.VM_ARGUMENTS`" value=`"$vmArgs`"")
	Set-Content -Path $launchFile -Value $content -Encoding UTF8
}

& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "patch-pde-help-server.ps1") -Workspace $Workspace -ProductConfigName $ProductConfigName

& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "setup-help-nl-plugins.ps1") -OpenChromRoot (Join-Path $PSScriptRoot "..\..")

$helpIndex = Join-Path $Workspace ".metadata\.plugins\org.eclipse.pde.core\$ProductConfigName\org.eclipse.help.base\index\zh_CN"
if(Test-Path $helpIndex) {
	Remove-Item $helpIndex -Recurse -Force
	Write-Host "Cleared stale zh_CN help index."
}

& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "compile-community-ui.ps1")
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "compile-rcp-app-ui.ps1")
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "compile-csd-ui.ps1")
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "compile-xxd-ui.ps1")
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "compile-peak-resolution.ps1")
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "compile-csd-model.ps1")
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "compile-batchprocess.ps1")
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "verify-help-nl.ps1") -Workspace $Workspace -OpenChromRoot (Join-Path $PSScriptRoot "..\..")

Write-Host "Updated launch configuration, dev.properties, and compiled workspace plugins."
Write-Host "If -clean failed previously, run reset-pde-runtime.ps1 before relaunching."
Write-Host "Restart the product with -clean -clearPersistedState."
