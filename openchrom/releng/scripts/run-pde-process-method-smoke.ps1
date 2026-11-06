# Compiles and launches PDE product, then runs the UI smoke test.
param(
	[switch]$Clean
)
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "verify-process-method-editor.ps1")
$proc = & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "launch-pde-product.ps1") -Clean:$Clean
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "ui-smoke-process-method-editor.ps1") -ProductPid $proc.Id