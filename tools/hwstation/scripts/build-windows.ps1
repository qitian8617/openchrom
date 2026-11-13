# Build CtrlPanel (x64), hwstation-tests (x64) and hwbridge (x86) with the
# Visual Studio that is already installed. This script does not change the
# user or machine PATH.
$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $false
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

$LogDir = Join-Path $Root "msvc-logs"
if (Test-Path $LogDir) { Remove-Item -Recurse -Force $LogDir }
New-Item -ItemType Directory -Path $LogDir | Out-Null
[System.IO.File]::WriteAllText((Join-Path $LogDir "started.txt"), "hwstation msvc log capture`r`n", [System.Text.Encoding]::ASCII)

function ConvertTo-CmdArg([string]$Text) {
	if ([string]::IsNullOrEmpty($Text)) { return '""' }
	return '"' + ($Text -replace '"', '""') + '"'
}

function Invoke-CmdLogged {
	param(
		[Parameter(Mandatory = $true)][string]$File,
		[Parameter(Mandatory = $true)][string[]]$ArgumentList,
		[Parameter(Mandatory = $true)][string]$LogPath
	)
	$argText = ($ArgumentList | ForEach-Object { ConvertTo-CmdArg $_ }) -join ' '
	$wrapper = [System.IO.Path]::ChangeExtension($LogPath, ".cmd")
	$body = @"
@echo off
chcp 65001 >nul
$(ConvertTo-CmdArg $File) $argText > $(ConvertTo-CmdArg $LogPath) 2>&1
set RC=%ERRORLEVEL%
echo EXITCODE:%RC% >> $(ConvertTo-CmdArg $LogPath)
exit /b %RC%
"@
	[System.IO.File]::WriteAllText($wrapper, $body.Replace("`n", "`r`n"), [System.Text.Encoding]::ASCII)
	cmd.exe /d /c $(ConvertTo-CmdArg $wrapper)
	return $LASTEXITCODE
}

function Build-One([string]$Generator, [string]$Arch, [string]$BuildDir) {
	$safe = ($Generator -replace '[^A-Za-z0-9]+', '-').Trim('-')
	if (Test-Path $BuildDir) { Remove-Item -Recurse -Force $BuildDir }
	$cfg = Join-Path $LogDir "$Arch-$safe-configure.log"
	Write-Host "configure $Generator $Arch"
	$code = Invoke-CmdLogged -File $cmake -ArgumentList @('-S', $Root, '-B', $BuildDir, '-G', $Generator, '-A', $Arch) -LogPath $cfg
	Write-Host "configure exit $code log $cfg"
	if ($code -ne 0) { return $false }
	$bld = Join-Path $LogDir "$Arch-$safe-build.log"
	Write-Host "build $Generator $Arch"
	$code = Invoke-CmdLogged -File $cmake -ArgumentList @('--build', $BuildDir, '--config', 'Release', '--', '/verbosity:minimal') -LogPath $bld
	Write-Host "build exit $code log $bld"
	if ($code -ne 0) { exit $code }
	return $true
}

$help = & $cmake --help
if ($LASTEXITCODE -ne 0) { throw "cmake --help failed" }
$generators = [regex]::Matches($help, 'Visual Studio \d+ \d{4}') | ForEach-Object { $_.Value } | Select-Object -Unique
if (-not $generators) {
	$generators = @("Visual Studio 18 2026", "Visual Studio 17 2022")
}
Write-Host "CMake Visual Studio generators: $($generators -join ', ')"
$x64 = Join-Path $Root "build-msvc-x64"
$x86 = Join-Path $Root "build-msvc-x86"
$ok64 = $false
foreach ($gen in $generators) {
	Write-Host "Trying $gen x64"
	if (Build-One $gen "x64" $x64) { $ok64 = $true; break }
}
if (-not $ok64) { exit 1 }
$ok86 = $false
foreach ($gen in $generators) {
	Write-Host "Trying $gen Win32"
	if (Build-One $gen "Win32" $x86) { $ok86 = $true; break }
}
if (-not $ok86) { exit 1 }

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
