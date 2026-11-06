# Verifies Laiende localization build artifacts and startup performance guards.
param(
	[string]$Workspace = "E:\OpenChrom",
	[string]$OpenChromRoot = "E:\OpenChrom\openchrom\openchrom"
)

$ErrorActionPreference = "Stop"
$failures = @()

function Assert-Condition {
	param([bool]$Condition, [string]$Message)
	if(-not $Condition) {
		$script:failures += $Message
		Write-Host "[FAIL] $Message" -ForegroundColor Red
	} else {
		Write-Host "[ OK ] $Message" -ForegroundColor Green
	}
}

Write-Host "=== Compile workspace plugins ==="
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $OpenChromRoot "releng\scripts\compile-community-ui.ps1")
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $OpenChromRoot "releng\scripts\setup-pde-launch.ps1")

$communityUi = Join-Path $OpenChromRoot "plugins\net.openchrom.rcp.compilation.community.ui"
$addonSource = Join-Path $communityUi "src\net\openchrom\rcp\compilation\community\ui\localization\ModelLabelLocalizationAddon.java"
$addonClass = Join-Path $communityUi "bin\net\openchrom\rcp\compilation\community\ui\localization\ModelLabelLocalizationAddon.class"

Assert-Condition (Test-Path $addonClass) "ModelLabelLocalizationAddon.class compiled"
Assert-Condition (Test-Path (Join-Path $OpenChromRoot "plugins\net.openchrom.rcp.compilation.community.ui\bin\net\openchrom\rcp\compilation\community\ui\startup\SafeWorkbenchStartup.class")) "SafeWorkbenchStartup.class compiled"

$source = Get-Content $addonSource -Raw -Encoding UTF8
Assert-Condition ($source -notmatch "SWT\.Paint") "Paint-time localization removed"
Assert-Condition ($source -match "translationCache") "Translation cache enabled"
Assert-Condition ($source -match "bootstrapComplete") "Bootstrap deduplication enabled"
Assert-Condition ($source -match "LOCALIZED_CONTROL_KEY") "Control localization memoization enabled"

$productFile = Join-Path $OpenChromRoot "products\net.openchrom.rcp.compilation.community.product\openchrom.compilation.community.product"
$product = Get-Content $productFile -Raw -Encoding UTF8
Assert-Condition ($product -match "-Dosgi\.nl=zh_CN") "Product sets zh_CN locale at launch"

$launchFile = Join-Path $Workspace ".metadata\.plugins\org.eclipse.debug.core\.launches\openchrom.compilation.community.product.launch"
if(Test-Path $launchFile) {
	$launch = Get-Content $launchFile -Raw -Encoding UTF8
	Assert-Condition ($launch -match "runtime/openchrom\.compilation\.community\.product") "PDE launch uses workspace runtime folder"
	Assert-Condition ($launch -match "net\.openchrom\.rcp\.compilation\.community\.ui@default:default") "Workspace community.ui plugin selected"
}

if($failures.Count -gt 0) {
	Write-Host "`nVerification failed with $($failures.Count) issue(s)." -ForegroundColor Red
	exit 1
}

Write-Host "`nVerification passed. Launch the product with -clean and confirm:" -ForegroundColor Cyan
Write-Host "  1. Main window appears without visible EN->CN flicker on menus"
Write-Host "  2. View menu shows 8 Chinese groups"
Write-Host "  3. Error log context menu first item is 复制"
Write-Host "  4. File menu hides MSD/WSD open entries"
Write-Host "  5. About dialog shows Laiende branding"
