# Verifies process method editor build artifacts for PDE runs.
param(
	[string]$UxPluginRoot = "E:\OpenChrom\chemclipse\chemclipse\plugins\org.eclipse.chemclipse.ux.extension.ui",
	[string]$CommunityPluginRoot = "E:\OpenChrom\openchrom\openchrom\plugins\net.openchrom.rcp.compilation.community.ui",
	[string]$DevProperties = "E:\OpenChrom\.metadata\.plugins\org.eclipse.pde.core\openchrom.compilation.community.product\dev.properties"
)

$errors = @()

function Assert-FileExists([string]$Path, [string]$Message) {
	if(-not (Test-Path $Path)) {
		$script:errors += $Message
	}
}

function Assert-Contains([string]$Path, [string]$Pattern, [string]$Message) {
	if(-not (Test-Path $Path)) {
		$script:errors += "$Message (missing file: $Path)"
		return
	}
	$content = Get-Content $Path -Raw -ErrorAction SilentlyContinue
	if($content -notmatch $Pattern) {
		$script:errors += $Message
	}
}

Write-Host "Compiling ux.extension.ui..."
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "compile-ux-extension-ui.ps1")
if($LASTEXITCODE -ne 0) {
	throw "compile-ux-extension-ui.ps1 failed."
}

Write-Host "Compiling community.ui..."
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "compile-community-ui.ps1")
if($LASTEXITCODE -ne 0) {
	throw "compile-community-ui.ps1 failed."
}

$editorClass = Join-Path $UxPluginRoot "bin\org\eclipse\chemclipse\ux\extension\ui\editors\ProcessMethodEditor.class"
$methodUiClass = Join-Path $UxPluginRoot "bin\org\eclipse\chemclipse\ux\extension\ui\editors\ExtendedMethodUI.class"
$addonClass = Join-Path $CommunityPluginRoot "bin\net\openchrom\rcp\compilation\community\ui\localization\ModelLabelLocalizationAddon.class"
$defaultCss = Join-Path $CommunityPluginRoot "stylesheets\default.css"

Assert-FileExists $editorClass "ProcessMethodEditor.class was not produced."
Assert-FileExists $methodUiClass "ExtendedMethodUI.class was not produced."
Assert-FileExists $addonClass "ModelLabelLocalizationAddon.class was not produced."
Assert-Contains $defaultCss "ProcessMethodEditor" "default.css is missing ProcessMethodEditor CSS exemption."

if(Test-Path $DevProperties) {
	$dev = Get-Content $DevProperties -Raw
	if($dev -notmatch "org\.eclipse\.chemclipse\.ux\.extension\.ui=bin") {
		$errors += "dev.properties does not map org.eclipse.chemclipse.ux.extension.ui to bin."
	}
	if($dev -notmatch "net\.openchrom\.rcp\.compilation\.community\.ui=bin") {
		$errors += "dev.properties does not map net.openchrom.rcp.compilation.community.ui to bin."
	}
} else {
	$errors += "dev.properties not found: $DevProperties"
}

if($errors.Count -gt 0) {
	$errors | ForEach-Object { Write-Host "ERROR: $_" }
	throw "Process method editor verification failed."
}

Get-Item $editorClass, $methodUiClass, $addonClass | ForEach-Object {
	Write-Host ("OK {0} ({1} bytes, {2})" -f $_.Name, $_.Length, $_.LastWriteTime)
}
Write-Host "Process method editor verification passed."