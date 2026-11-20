# One Windows pass: offline tests, then the x86 WPF build.
# Does not install SDKs, does not call install_windows.ps1, and does not start CtrlPanel.
# If .NET SDK 10.0.201 is present, this process pins it without setx or a user-profile change.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$dotnetHome = Join-Path $env:ProgramFiles "dotnet"
$sdk10 = Join-Path $dotnetHome "sdk\10.0.201"
$globalPath = Join-Path $root "global.json"
$original = [System.IO.File]::ReadAllText($globalPath)
$pinned = $false
if (Test-Path $sdk10) {
    $env:DOTNET_MULTILEVEL_LOOKUP = "0"
    $env:DOTNET_ROOT = $dotnetHome
    $env:PATH = "$dotnetHome;" + $env:PATH
    $pin = @'
{
  "sdk": {
    "version": "10.0.201",
    "rollForward": "disable",
    "allowPrerelease": true
  }
}
'@
    [System.IO.File]::WriteAllText($globalPath, $pin)
    $pinned = $true
    Write-Host "Pinned this process to SDK 10.0.201"
}
try {
    Write-Host ("dotnet " + (& dotnet --version))
    & dotnet run --project (Join-Path $root "tests\HwMinimal.Tests\HwMinimal.Tests.csproj") -c Release --nologo
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    & dotnet build (Join-Path $root "src\CtrlPanel\CtrlPanel.csproj") -c Release -r win-x86 --self-contained false -p:PlatformTarget=x86 --nologo
    exit $LASTEXITCODE
}
finally {
    if ($pinned) {
        [System.IO.File]::WriteAllText($globalPath, $original)
    }
}
