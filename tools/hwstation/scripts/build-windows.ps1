# Build CtrlPanel (x64), hwstation-tests (x64) and hwbridge (x86) with the
# Visual Studio that is already installed. This script does not change the
# user or machine PATH.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$vswhere = Join-Path ${env:ProgramFiles(x86)} "Microsoft Visual Studio\Installer\vswhere.exe"
if (-not (Test-Path $vswhere)) {
	throw "vswhere.exe was not found. Visual Studio Build Tools are required."
}
$install = & $vswhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
if (-not $install) {
	throw "No Visual Studio installation with the MSVC toolset was found."
}
$cmake = $null
$cmd = Get-Command cmake -ErrorAction SilentlyContinue
if ($cmd) { $cmake = $cmd.Source }
if (-not $cmake) {
	$candidate = Join-Path $install "Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\cmake.exe"
	if (Test-Path $candidate) { $cmake = $candidate }
}
if (-not $cmake) {
	throw "cmake.exe was not found. Visual Studio's CMake component is required."
}

function Build-One([string]$Generator, [string]$Arch, [string]$BuildDir) {
	if (Test-Path $BuildDir) { Remove-Item -Recurse -Force $BuildDir }
	& $cmake -S $Root -B $BuildDir -G $Generator -A $Arch
	if ($LASTEXITCODE -ne 0) { return $false }
	& $cmake --build $BuildDir --config Release
	if ($LASTEXITCODE -ne 0) { throw "cmake build failed for $Arch" }
	return $true
}

$generators = @("Visual Studio 18 2026", "Visual Studio 17 2022")
$x64 = Join-Path $Root "build-msvc-x64"
$x86 = Join-Path $Root "build-msvc-x86"
$ok64 = $false
foreach ($gen in $generators) {
	Write-Host "Trying $gen x64"
	if (Build-One $gen "x64" $x64) { $ok64 = $true; break }
}
if (-not $ok64) { throw "Could not configure the x64 build." }
$ok86 = $false
foreach ($gen in $generators) {
	Write-Host "Trying $gen Win32"
	if (Build-One $gen "Win32" $x86) { $ok86 = $true; break }
}
if (-not $ok86) { throw "Could not configure the x86 bridge build." }

$dist = Join-Path $Root "dist-msvc"
if (Test-Path $dist) { Remove-Item -Recurse -Force $dist }
New-Item -ItemType Directory -Path $dist | Out-Null
Copy-Item (Join-Path $x64 "Release\CtrlPanel.exe") $dist
Copy-Item (Join-Path $x64 "Release\hwstation-tests.exe") $dist
Copy-Item (Join-Path $x86 "Release\hwbridge.exe") $dist
Copy-Item (Join-Path $x86 "Release\fake_hwsend.dll") $dist
Copy-Item (Join-Path $x86 "Release\hwstation_ipc_repro.dll") $dist
Copy-Item (Join-Path $Root "启动CtrlPanel.bat") $dist
Copy-Item (Join-Path $Root "README.md") $dist
Copy-Item (Join-Path $Root "docs") (Join-Path $dist "docs") -Recurse
Write-Host "MSVC output is in $dist"
Write-Host "Run: dist-msvc\hwstation-tests.exe --offline"
Write-Host "Run: dist-msvc\CtrlPanel.exe --offline-smoke"
