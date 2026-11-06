# Verifies org.eclipse.chemclipse.rcp.ui.icons is available for PDE workspace builds.
param(
	[string]$RepoRoot = "E:\OpenChrom\openchrom\openchrom",
	[string]$BundlePool = "E:\OpenChrom\.metadata\.plugins\org.eclipse.pde.core\.bundle_pool\plugins",
	[string]$ChemClipseIconsProject = "E:\OpenChrom\chemclipse\chemclipse\plugins\org.eclipse.chemclipse.rcp.ui.icons"
)

$localPluginsDir = Join-Path $RepoRoot "releng\pde-local-plugins"
$iconsJar = @(
	Get-ChildItem $localPluginsDir -Filter "org.eclipse.chemclipse.rcp.ui.icons_*.jar" -ErrorAction SilentlyContinue
	Get-ChildItem $BundlePool -Filter "org.eclipse.chemclipse.rcp.ui.icons_*.jar" -ErrorAction SilentlyContinue
) | Select-Object -First 1

Write-Host "PDE icons plugin check"
Write-Host "  local-plugins : $(if($iconsJar) { $iconsJar.FullName } else { 'MISSING' })"
Write-Host "  chemclipse src: $(if(Test-Path $ChemClipseIconsProject) { $ChemClipseIconsProject } else { 'not found' })"

if(-not $iconsJar) {
	Write-Host ""
	Write-Host "ERROR: org.eclipse.chemclipse.rcp.ui.icons jar missing."
	Write-Host "Run: powershell -File releng\scripts\setup-pde-chart-menu.ps1"
	exit 1
}

Write-Host ""
Write-Host "Jars are on disk. If Eclipse still reports ApplicationImageFactory errors:"
Write-Host "  1. Window -> Preferences -> Plug-in Development -> Target Platform"
Write-Host "     Select OpenChrom -> Reload (target sequence must be 47+)"
Write-Host "  2. Project -> Clean... -> Clean all projects"
Write-Host "  3. Or import workspace project:"
Write-Host "     File -> Import -> Existing Projects -> $ChemClipseIconsProject"
Write-Host ""
exit 0