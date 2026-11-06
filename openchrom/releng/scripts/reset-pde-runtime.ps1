# Clears locked PDE product runtime state so -clean can succeed on next launch.
param(
	[string]$Workspace = "E:\OpenChrom",
	[string]$ProductConfigName = "openchrom.compilation.community.product"
)

$paths = @(
	(Join-Path $Workspace ".metadata\.plugins\org.eclipse.pde.core\$ProductConfigName\org.eclipse.osgi"),
	(Join-Path $Workspace "runtime\$ProductConfigName"),
	(Join-Path $Workspace ".metadata\.plugins\org.eclipse.e4.workbench")
)

Get-Process -Name "java","javaw","eclipse","laiendechrom" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

foreach($path in $paths) {
	if(Test-Path $path) {
		Remove-Item -LiteralPath $path -Recurse -Force -ErrorAction SilentlyContinue
		if(Test-Path $path) {
			Write-Warning "Could not fully delete: $path"
		} else {
			Write-Host "Deleted: $path"
		}
	}
}

Write-Host "PDE runtime reset complete. Relaunch the product with -clean -clearPersistedState."
