# Launches the OpenChrom PDE product with the same settings as the Eclipse launch configuration.
param(
	[string]$EclipseHome = "G:\Tools\eclipse-rcp-2025-12-R-win32-x86_64\eclipse",
	[string]$Workspace = "E:\OpenChrom",
	[string]$ProductConfigName = "openchrom.compilation.community.product",
	[switch]$Clean
)

$eclipseExe = Join-Path $EclipseHome "eclipse.exe"
if(-not (Test-Path $eclipseExe)) { throw "Eclipse launcher not found: $eclipseExe" }

$config = Join-Path $Workspace ".metadata\.plugins\org.eclipse.pde.core\$ProductConfigName"
$devProps = Join-Path $config "dev.properties"
$data = Join-Path $Workspace "runtime\$ProductConfigName"
New-Item -ItemType Directory -Force -Path $data | Out-Null
if(-not (Test-Path $devProps)) { throw "PDE dev.properties not found: $devProps" }

$programArgs = "-os win32 -ws win32 -arch x86_64 -nl zh_CN -consoleLog -clearPersistedState"
if($Clean) { $programArgs += " -clean" }

$vmArgs = "-Xms256m -Xmx2048m -Dosgi.nl=zh_CN -Declipse.log.level=INFO -Djava.util.logging.config.file=$($config -replace '\\','/')/logging.properties"

$argList = @(
	"-nosplash",
	"-data", $data,
	"-configuration", $config,
	"-dev", "file:$($devProps -replace '\\','/')",
	"-product", "net.openchrom.rcp.compilation.community.ui.product",
	"-application", "org.eclipse.chemclipse.rcp.app.ui.org.eclipse.chemclipse.rcp.application"
) + ($programArgs -split ' ') + @("-vmargs") + ($vmArgs -split ' ')

Write-Host "Launching PDE product from $eclipseExe"
Write-Host "Data: $data"
$proc = Start-Process -FilePath $eclipseExe -ArgumentList $argList -PassThru -WindowStyle Normal
Write-Host "Started PID $($proc.Id)"
return $proc