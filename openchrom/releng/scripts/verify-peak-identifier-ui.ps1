# Verifies PDE runtime includes templates.ui (required for Peak Identifier [Template] table editor).
param(
	[string]$LaunchFile = "E:\OpenChrom\.metadata\.plugins\org.eclipse.debug.core\.launches\openchrom.compilation.community.product.launch",
	[string]$ConfigIni = "E:\OpenChrom\.metadata\.plugins\org.eclipse.pde.core\openchrom.compilation.community.product\config.ini",
	[string]$TemplatesUiRoot = "E:\OpenChrom\openchrom\openchrom\plugins\net.openchrom.xxd.process.supplier.templates.ui"
)

$launchOk = $false
$runtimeOk = $false
$compiledOk = $false
$issues = @()

Write-Host "=== Peak Identifier [Template] UI verification ==="

if(Test-Path $LaunchFile) {
	$launch = Get-Content $LaunchFile -Raw
	$launchOk = ($launch -match [regex]::Escape("net.openchrom.xxd.process.supplier.templates.ui")) -and
		($launch -match [regex]::Escape("net.openchrom.xxd.process.supplier.templates.ui@default:default"))
	if($launchOk) {
		Write-Host "[OK] Launch configuration includes templates.ui"
	} else {
		$issues += "Launch configuration missing templates.ui (run setup-pde-chart-menu.ps1)"
	}
} else {
	$issues += "Launch file not found: $LaunchFile"
}

$compiled = Get-ChildItem (Join-Path $TemplatesUiRoot "bin") -Recurse -Filter "*.class" -ErrorAction SilentlyContinue | Select-Object -First 1
$compiledOk = $null -ne $compiled
if($compiledOk) {
	Write-Host "[OK] templates.ui is compiled"
} else {
	$issues += "templates.ui has no compiled classes (Eclipse: Project -> Build Project)"
}

if(Test-Path $ConfigIni) {
	$config = Get-Content $ConfigIni -Raw
	$runtimeOk = $config -match [regex]::Escape("net.openchrom.xxd.process.supplier.templates.ui")
	$stamp = (Get-Item $ConfigIni).LastWriteTime.ToString("yyyy-MM-dd HH:mm:ss")
	if($runtimeOk) {
		Write-Host "[OK] Runtime config.ini includes templates.ui (updated $stamp)"
	} else {
		Write-Host "[PENDING] Runtime config.ini is stale (updated $stamp) - missing templates.ui"
		Write-Host "          Restart the PDE product launch to regenerate config.ini."
	}
} else {
	Write-Host "[PENDING] Runtime config.ini not found - PDE product has not been launched yet."
}

Write-Host ""
if($launchOk -and $compiledOk -and $runtimeOk) {
	Write-Host "Result: READY - Peak Identifier [Template] should show the table editor."
	exit 0
}

if($launchOk -and $compiledOk -and -not $runtimeOk) {
	Write-Host "Result: CONFIG READY, RUNTIME STALE"
	Write-Host "  1. Stop the running OpenChrom PDE instance (red square in Console, or close the app window)"
	Write-Host "  2. In Eclipse: Run -> openchrom.compilation.community.product (Debug/Run)"
	Write-Host "  3. Re-run this script - config.ini should then include templates.ui"
	Write-Host ""
	Write-Host "Expected UI: toolbar + table (RT, substance, CAS, ...)"
	exit 2
}

Write-Host "Result: NOT READY"
$issues | ForEach-Object { Write-Host "  - $_" }
Write-Host ""
Write-Host "Wrong UI until fixed: Limit Match Factor + multiline Identifier Settings text"
exit 1