#Requires -Version 5.1
<#
.SYNOPSIS
  Materialize the Baijiu FID workstation for win32.win32.x86_64 with Tycho.

.DESCRIPTION
  Does not use Eclipse PDE Product Export. Runs tycho-p2-director-plugin
  materialize-products (bound to the Maven install phase in openchrom/pom.xml).
  Do not pass -Pci: that profile sets the director goals to phase none.

  Maven must run on JDK 25. JAVA_HOME wins over java.exe on PATH.
  A machine where `java -version` prints 21 and `mvn -version` prints 1.8
  will not compile this tree (plug-ins are JavaSE-25; CI uses Temurin 25).

.PARAMETER Stage
  After a successful build, mirror the win64 folder to -Destination
  (default E:\OpenChrom\baijiu-fid-workstation, the Inno Setup SourceDir).

.PARAMETER Destination
  Staging directory used only with -Stage.
#>
[CmdletBinding()]
param(
    [switch]$Stage,
    [string]$Destination = "E:\OpenChrom\baijiu-fid-workstation"
)

$ErrorActionPreference = "Stop"

$OpenChromRoot = Split-Path -Parent $PSScriptRoot
$ProductDir = Join-Path $OpenChromRoot "products\net.openchrom.rcp.compilation.baijiu.product\target\products\net.openchrom.rcp.compilation.baijiu.product.id\win32\win32\x86_64"

function Get-JavaMajor {
    param([string]$JavaExe)
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $output = & $JavaExe -version 2>&1 | Out-String
    $ErrorActionPreference = $previous
    if ($output -match 'version "1\.(\d+)') {
        return [int]$Matches[1]
    }
    if ($output -match 'version "(\d+)') {
        return [int]$Matches[1]
    }
    throw "Could not parse the Java version from:`n$output"
}

if (-not $env:JAVA_HOME) {
    throw @"
JAVA_HOME is not set. Maven uses JAVA_HOME, not the first java.exe on PATH.
Point it at a JDK 25 install (Eclipse Temurin 25, same as CI), then reopen the shell:
  setx JAVA_HOME `"C:\Program Files\Eclipse Adoptium\jdk-25...`"
  `$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25...'
  `$env:Path = "`$env:JAVA_HOME\bin;`$env:Path"
"@
}

$javaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
if (-not (Test-Path $javaExe)) {
    throw "JAVA_HOME does not contain bin\java.exe: $env:JAVA_HOME"
}

$major = Get-JavaMajor $javaExe
if ($major -lt 25) {
    throw "JAVA_HOME is Java $major ($javaExe). This build needs JDK 25. Java 8 and Java 21 cannot compile JavaSE-25 plug-ins."
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "mvn was not found on PATH. Maven 3.9.x is required (CI uses 3.9, engineer PCs have used 3.9.16)."
}

if (-not $env:MAVEN_OPTS) {
    $env:MAVEN_OPTS = "-Xmx2g"
}

Write-Host "JAVA_HOME=$env:JAVA_HOME (major $major)"
Write-Host "MAVEN_OPTS=$env:MAVEN_OPTS"
& mvn -version
if ($LASTEXITCODE -ne 0) {
    throw "mvn -version failed with exit code $LASTEXITCODE"
}

$mvnArgs = @(
    "-f", "releng/net.openchrom.aggregator/pom.xml",
    "-pl", "net.openchrom:net.openchrom.targetplatform,net.openchrom:openchrom.compilation.baijiu",
    "-am",
    "install",
    "-Pwin32-x86_64",
    "-DskipTests"
)

Write-Host "cd $OpenChromRoot"
Write-Host ("mvn " + ($mvnArgs -join " "))

Push-Location $OpenChromRoot
try {
    & mvn @mvnArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Maven exited with code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}

$exe = Join-Path $ProductDir "baijiu-fid.exe"
if (-not (Test-Path $exe)) {
    throw "Materialized launcher not found: $exe"
}

$iniPath = Join-Path $ProductDir "baijiu-fid.ini"
$ini = Get-Content -Path $iniPath -Raw
if ($ini -notmatch [regex]::Escape("-Xmx4096m")) {
    Write-Warning "baijiu-fid.ini does not contain -Xmx4096m. The product vmArgs should have been copied into the launcher ini."
}

Write-Host ""
Write-Host "Win64 product folder:"
Write-Host "  $ProductDir"
Write-Host "Launcher: baijiu-fid.exe  (JustJ 25 is the jre\ directory beside it, or under plugins\)"

if ($Stage) {
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    & robocopy $ProductDir $Destination /MIR /NFL /NDL /NJH /NJS /nc /ns /np
    if ($LASTEXITCODE -ge 8) {
        throw "robocopy failed with exit code $LASTEXITCODE"
    }
    Write-Host "Staged to $Destination"
    Write-Host "Next: compile packaging\BaijiuFID-Setup.iss with Inno Setup (ISCC)."
}
else {
    Write-Host "Copy that folder onto E:\OpenChrom\baijiu-fid-workstation before compiling packaging\BaijiuFID-Setup.iss,"
    Write-Host "or re-run with -Stage."
}
