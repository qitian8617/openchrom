[CmdletBinding()]
param(
	[string[]]$Plugins = @(
		"net.openchrom.csd.converter.supplier.cdf",
		"net.openchrom.rcp.compilation.community.ui"
	)
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$aggregatorPom = Join-Path $repoRoot "releng\net.openchrom.aggregator\pom.xml"

Write-Host "Building plugins with Tycho..."
$pluginPaths = ($Plugins | ForEach-Object { "../../plugins/$_" }) -join ","
& mvn -f $aggregatorPom -pl $pluginPaths -am package "-DskipTests"
if($LASTEXITCODE -ne 0) {
	throw "Maven build failed with exit code $LASTEXITCODE."
}

foreach($pluginName in $Plugins) {
	$pluginDir = Join-Path $repoRoot "plugins\$pluginName"
	$jar = Get-ChildItem (Join-Path $pluginDir "target\*.jar") -ErrorAction SilentlyContinue | Select-Object -First 1
	if($null -eq $jar) {
		throw "No built jar found for $pluginName"
	}

	$binDir = Join-Path $pluginDir "bin"
	if(Test-Path $binDir) {
		Remove-Item $binDir -Recurse -Force
	}
	New-Item -ItemType Directory -Path $binDir | Out-Null

	Push-Location $binDir
	& jar xf $jar.FullName
	Pop-Location

	Write-Host "Synced $($jar.Name) -> $binDir"
}

Write-Host "PDE bin/ sync complete. Restart the product launch with -clearPersistedState."
