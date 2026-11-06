# Builds net.openchrom.xxd.process.supplier.templates from source for PDE.
param(
	[string]$PluginsRoot = "E:\OpenChrom\openchrom\openchrom\plugins",
	[string]$BundlePool = "E:\OpenChrom\.metadata\.plugins\org.eclipse.pde.core\.bundle_pool\plugins",
	[string]$LocalPluginsDir = "E:\OpenChrom\openchrom\openchrom\releng\pde-local-plugins"
)
$cp = (Get-ChildItem $BundlePool -Filter "*.jar" | Where-Object { $_.Name -notmatch '\.source_|sources|javadoc' } | ForEach-Object { $_.FullName }) -join ";"
$pluginRoot = Join-Path $PluginsRoot "net.openchrom.xxd.process.supplier.templates"
$bin = Join-Path $pluginRoot "bin"
$src = Join-Path $pluginRoot "src"
New-Item -ItemType Directory -Force -Path $bin | Out-Null
$javaFiles = Get-ChildItem $src -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
$argFile = Join-Path $env:TEMP "laiende-templates-javac.txt"
$lines = @("-encoding","UTF-8","-source","21","-target","21","-cp",$cp,"-d",$bin) + $javaFiles
[System.IO.File]::WriteAllLines($argFile, $lines)
Write-Host "Compiling templates plugin..."
& javac -proc:none "@$argFile"
if ($LASTEXITCODE -ne 0) { throw "javac failed" }
$staging = Join-Path $env:TEMP "laiende-stage-templates"
if (Test-Path $staging) { Remove-Item $staging -Recurse -Force }
New-Item -ItemType Directory -Force -Path $staging | Out-Null
foreach ($item in @("META-INF","plugin.xml","OSGI-INF")) { Copy-Item (Join-Path $pluginRoot $item) (Join-Path $staging $item) -Recurse -Force }
Copy-Item (Join-Path $bin "*") $staging -Recurse -Force
New-Item -ItemType Directory -Force -Path $LocalPluginsDir | Out-Null
$jarPath = Join-Path $LocalPluginsDir "net.openchrom.xxd.process.supplier.templates_1.6.20.jar"
if (Test-Path $jarPath) { Remove-Item $jarPath -Force }
Push-Location $staging
jar cf $jarPath .
Pop-Location
Remove-Item $staging -Recurse -Force
Copy-Item $jarPath (Join-Path $BundlePool "net.openchrom.xxd.process.supplier.templates_1.6.20.jar") -Force
Write-Host "Deployed net.openchrom.xxd.process.supplier.templates_1.6.20.jar"