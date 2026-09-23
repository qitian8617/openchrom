#Requires -Version 5.1
<#
.SYNOPSIS
  Re-jar ordinary directory-shaped OSGi plug-ins so Windows MAX_PATH (260) is not exceeded.

.DESCRIPTION
  Tycho materialize leaves some bundles as directories (Eclipse-BundleShape: dir,
  or a feature unpack). Nested class files under a long bundle name — notably
  org.eclipse.chemclipse.chromatogram.xxd.calculator.supplier.amdiscalri_* /
  ChromatogramRetentionIndexRecalculator$ProcessSupplier.class — then exceed
  MAX_PATH while Inno Setup compiles the tree and again while it installs under
  Program Files.

  This packs those directories into plugins\Name_ver.jar and rewrites
  configuration\org.eclipse.equinox.simpleconfigurator\bundles.info from
  plugins/Name_ver/ to plugins/Name_ver.jar. Equinox simpleconfigurator loads
  that file at startup. The product already sets org.eclipse.update.reconcile=false,
  so the p2 dropins reconciler does not rebuild bundles.info from the profile.

  Left as directories (natives have to stay on disk for the launcher / JNA / JustJ):
    org.eclipse.justj.*
    org.eclipse.equinox.launcher.win32.win32.x86_64_*
    com.sun.jna_*
    any other directory bundle that contains .dll/.exe/.so/.dylib/.jnilib/.node
  Bundles whose names contain amdiscalri, molpeak, rcp.app.ui, rcp.ui.icons,
  feature.branding, or ui.themes are always jarred. They are the long class
  and resource trees that blow MAX_PATH, and they are not native fragments.

  On Windows the plug-in directory is subst'd to a drive letter before jar.exe
  runs, so the JDK is not handed a path that is already over 260 characters.
  Pass -ProductRoot as the materialized win64 folder or a staged copy.

.PARAMETER ProductRoot
  Product directory that contains plugins\ and configuration\.

.PARAMETER JarExe
  Path to jar.exe. Defaults to %JAVA_HOME%\bin\jar.exe, then jar on PATH.
#>
[CmdletBinding()]
param(
    [string]$ProductRoot,
    [string]$JarExe
)

$ErrorActionPreference = "Stop"

function Test-WindowsOs {
    return ([System.Environment]::OSVersion.Platform -eq [System.PlatformID]::Win32NT)
}

function Test-KeepDirectoryPlugin {
    param([string]$Name)
    if ($Name.StartsWith("org.eclipse.justj.")) {
        return $true
    }
    if ($Name -eq "org.eclipse.equinox.launcher.win32.win32.x86_64" -or
        $Name.StartsWith("org.eclipse.equinox.launcher.win32.win32.x86_64_")) {
        return $true
    }
    if ($Name -eq "com.sun.jna" -or $Name.StartsWith("com.sun.jna_")) {
        return $true
    }
    return $false
}

function Test-ForceJarPlugin {
    param([string]$Name)
    return (
        $Name -like "*amdiscalri*" -or
        $Name -like "*molpeak*" -or
        $Name -like "*rcp.app.ui*" -or
        $Name -like "*rcp.ui.icons*" -or
        $Name -like "*feature.branding*" -or
        $Name -like "*ui.themes*"
    )
}

function Test-DirectoryHasNativePayload {
    param([string]$Root)
    $native = @{
        ".dll" = $true
        ".exe" = $true
        ".so" = $true
        ".dylib" = $true
        ".jnilib" = $true
        ".node" = $true
    }
    $stack = New-Object System.Collections.Generic.Stack[string]
    $stack.Push($Root)
    while ($stack.Count -gt 0) {
        $current = $stack.Pop()
        foreach ($file in [System.IO.Directory]::EnumerateFiles($current)) {
            $ext = [System.IO.Path]::GetExtension($file).ToLowerInvariant()
            if ($native.ContainsKey($ext)) {
                return $file
            }
        }
        foreach ($dir in [System.IO.Directory]::EnumerateDirectories($current)) {
            $stack.Push($dir)
        }
    }
    return $null
}

function Read-Utf8Text {
    param([string]$Path)
    $bytes = [System.IO.File]::ReadAllBytes($Path)
    $utf8 = New-Object System.Text.UTF8Encoding $false
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        return $utf8.GetString($bytes, 3, $bytes.Length - 3)
    }
    return $utf8.GetString($bytes)
}

function Write-Utf8TextNoBom {
    param([string]$Path, [string]$Text)
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($Path, $Text, $utf8)
}

function Update-BundlesInfoText {
    param(
        [string]$Text,
        [string[]]$DirectoryNames
    )
    $missing = @()
    foreach ($name in $DirectoryNames) {
        $slashPattern = 'plugins[/\\]' + [regex]::Escape($name) + '[/\\]'
        $barePattern = 'plugins[/\\]' + [regex]::Escape($name) + '(?=,)'
        $jarPattern = 'plugins[/\\]' + [regex]::Escape($name) + '\.jar'
        $slashCount = [regex]::Matches($Text, $slashPattern).Count
        $bareCount = [regex]::Matches($Text, $barePattern).Count
        if ($slashCount -eq 0 -and $bareCount -eq 0) {
            if (-not [regex]::IsMatch($Text, $jarPattern)) {
                $missing += $name
            }
            continue
        }
        $replacement = ('plugins/' + $name + '.jar').Replace('$', '$$')
        if ($slashCount -gt 0) {
            $Text = [regex]::Replace($Text, $slashPattern, $replacement)
        }
        if ($bareCount -gt 0) {
            $Text = [regex]::Replace($Text, $barePattern, $replacement)
        }
    }
    return [pscustomobject]@{
        Text = $Text
        Missing = @($missing)
    }
}

function Test-BundlesInfoReferencesPlugin {
    param([string]$Text, [string]$Name)
    $result = Update-BundlesInfoText -Text $Text -DirectoryNames @($Name)
    return $result.Missing.Count -eq 0
}

function Resolve-JarExecutable {
    param([string]$Explicit)
    if ($Explicit) {
        if (-not (Test-Path -LiteralPath $Explicit)) {
            throw "jar executable not found: $Explicit"
        }
        return $Explicit
    }
    if ($env:JAVA_HOME) {
        $name = "jar"
        if (Test-WindowsOs) {
            $name = "jar.exe"
        }
        $candidate = [System.IO.Path]::Combine($env:JAVA_HOME, "bin", $name)
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }
    $cmd = Get-Command jar -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }
    throw "jar was not found. Set JAVA_HOME to JDK 25 (the same JDK the product build uses)."
}

function Test-JarListsManifest {
    param([string]$JarExecutable, [string]$JarPath)
    $listing = & $JarExecutable --list --file $JarPath
    if ($LASTEXITCODE -ne 0) {
        throw "jar --list failed ($LASTEXITCODE) for $JarPath"
    }
    foreach ($line in @($listing)) {
        $normalized = ([string]$line).Trim().Replace('\', '/')
        if ($normalized -eq "META-INF/MANIFEST.MF") {
            return $true
        }
    }
    return $false
}

function Get-FreeDriveLetters {
    param([int]$Count = 2)
    $taken = @{}
    foreach ($drive in [System.IO.DriveInfo]::GetDrives()) {
        if ($drive.Name.Length -ge 1) {
            $taken[$drive.Name.Substring(0, 1).ToUpperInvariant()] = $true
        }
    }
    $substText = ""
    $substOut = & subst.exe
    if ($substOut) {
        $substText = $substOut | Out-String
    }
    $letters = @()
    foreach ($code in (90..67)) {
        $letter = [string][char]$code
        if ($taken.ContainsKey($letter)) {
            continue
        }
        if ($substText -match ("(?m)^" + $letter + ":")) {
            continue
        }
        $letters += $letter
        if ($letters.Count -ge $Count) {
            return $letters
        }
    }
    throw "Need $Count free drive letters so subst can keep plug-in paths under MAX_PATH."
}

function Update-BundlePathInFile {
    param(
        [string]$Path,
        [string]$Name,
        [switch]$Required
    )
    if (-not [System.IO.File]::Exists($Path)) {
        if ($Required) {
            throw "Required bundle list not found: $Path"
        }
        return
    }
    $text = Read-Utf8Text $Path
    if ($Required -and -not (Test-BundlesInfoReferencesPlugin -Text $text -Name $Name)) {
        throw "bundles.info has no plugins/$Name/ or plugins/$Name.jar entry. Refusing to jar $Name because Equinox would not find it."
    }
    $updated = Update-BundlesInfoText -Text $text -DirectoryNames @($Name)
    if ($updated.Text -ne $text) {
        Write-Utf8TextNoBom -Path $Path -Text $updated.Text
    }
}

function Remove-Children {
    param([string]$ContentsRoot)
    $children = @(Get-ChildItem -LiteralPath $ContentsRoot -Force)
    foreach ($child in $children) {
        if (Test-WindowsOs) {
            $descendants = @(Get-ChildItem -LiteralPath $child.FullName -Force -Recurse -ErrorAction SilentlyContinue)
            foreach ($item in $descendants) {
                try { $item.Attributes = [System.IO.FileAttributes]::Normal } catch { }
            }
            try { $child.Attributes = [System.IO.FileAttributes]::Normal } catch { }
        }
        Remove-Item -LiteralPath $child.FullName -Recurse -Force
    }
}

function Invoke-RejarDirectoryPlugins {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProductRoot,
        [string]$JarExe
    )

    if (-not (Test-Path -LiteralPath $ProductRoot)) {
        throw "Product root does not exist: $ProductRoot"
    }
    $ProductRoot = (Resolve-Path -LiteralPath $ProductRoot).Path
    $pluginsDir = [System.IO.Path]::Combine($ProductRoot, "plugins")
    if (-not (Test-Path -LiteralPath $pluginsDir)) {
        throw "plugins directory not found: $pluginsDir"
    }
    $bundlesInfo = [System.IO.Path]::Combine(
        $ProductRoot, "configuration", "org.eclipse.equinox.simpleconfigurator", "bundles.info")
    if (-not [System.IO.File]::Exists($bundlesInfo)) {
        throw "simpleconfigurator bundles.info not found: $bundlesInfo"
    }

    $jarExecutable = Resolve-JarExecutable $JarExe
    $useSubst = Test-WindowsOs
    $pluginsDrive = $null
    $bundleDrive = $null
    $pluginsMapped = $false
    if ($useSubst) {
        $letters = @(Get-FreeDriveLetters -Count 2)
        $pluginsDrive = $letters[0]
        $bundleDrive = $letters[1]
        $null = & subst.exe "${pluginsDrive}:" $pluginsDir.TrimEnd('\', '/')
        if ($LASTEXITCODE -ne 0) {
            throw "subst ${pluginsDrive}: failed for $pluginsDir (exit $LASTEXITCODE)"
        }
        $pluginsMapped = $true
    }

    $jarred = @()
    $kept = @()
    $skipped = @()
    try {
        $pluginRoot = $pluginsDir
        if ($useSubst) {
            $pluginRoot = "${pluginsDrive}:\"
        }
        $directories = @(Get-ChildItem -LiteralPath $pluginRoot -Force -Directory)
        foreach ($directory in $directories) {
            $name = $directory.Name
            if (Test-KeepDirectoryPlugin $name) {
                Write-Host "Keep directory: $name"
                $kept += $name
                continue
            }

            $bundleMapped = $false
            $contentsRoot = $directory.FullName
            $packed = $false
            try {
                if ($useSubst) {
                    $null = & subst.exe "${bundleDrive}:" ([System.IO.Path]::Combine("${pluginsDrive}:\", $name))
                    if ($LASTEXITCODE -ne 0) {
                        throw "subst ${bundleDrive}: failed for $name (exit $LASTEXITCODE)"
                    }
                    $bundleMapped = $true
                    $contentsRoot = "${bundleDrive}:\"
                }

                $manifest = [System.IO.Path]::Combine($contentsRoot, "META-INF", "MANIFEST.MF")
                if (-not [System.IO.File]::Exists($manifest)) {
                    Write-Warning "Skip directory (no META-INF/MANIFEST.MF): $name"
                    $skipped += $name
                }
                else {
                    $force = Test-ForceJarPlugin $name
                    $nativeFile = $null
                    if (-not $force) {
                        $nativeFile = Test-DirectoryHasNativePayload $contentsRoot
                    }
                    if ($nativeFile) {
                        Write-Host "Keep directory (native payload): $name ($nativeFile)"
                        $kept += $name
                    }
                    else {
                        $infoText = Read-Utf8Text $bundlesInfo
                        if (-not (Test-BundlesInfoReferencesPlugin -Text $infoText -Name $name)) {
                            throw "bundles.info has no plugins/$name/ or plugins/$name.jar entry. Refusing to jar $name because Equinox would not find it."
                        }

                        $tempJar = [System.IO.Path]::Combine(
                            [System.IO.Path]::GetTempPath(),
                            ("rejar-" + [guid]::NewGuid().ToString("n") + ".jar"))
                        $jarSource = $contentsRoot
                        if ($useSubst) {
                            # A trailing backslash is eaten by the Windows C runtime quote rules.
                            $jarSource = "${bundleDrive}:\."
                        }
                        try {
                            & $jarExecutable --create --file $tempJar --no-manifest -C $jarSource .
                            if ($LASTEXITCODE -ne 0) {
                                throw "jar failed ($LASTEXITCODE) while packing $name"
                            }
                            if (-not (Test-JarListsManifest -JarExecutable $jarExecutable -JarPath $tempJar)) {
                                throw "Packed $name but META-INF/MANIFEST.MF is missing from the jar"
                            }
                            $finalJar = [System.IO.Path]::Combine($pluginsDir, ($name + ".jar"))
                            if ($useSubst) {
                                $finalJar = "${pluginsDrive}:\$name.jar"
                            }
                            if (Test-Path -LiteralPath $finalJar) {
                                Remove-Item -LiteralPath $finalJar -Force
                            }
                            Move-Item -LiteralPath $tempJar -Destination $finalJar

                            Update-BundlePathInFile -Path $bundlesInfo -Name $name -Required
                            $iniFiles = @([System.IO.Directory]::GetFiles($ProductRoot, "*.ini"))
                            $configIni = [System.IO.Path]::Combine($ProductRoot, "configuration", "config.ini")
                            if ([System.IO.File]::Exists($configIni)) {
                                $iniFiles += $configIni
                            }
                            foreach ($ini in $iniFiles) {
                                Update-BundlePathInFile -Path $ini -Name $name
                            }

                            Remove-Children $contentsRoot
                            $packed = $true
                        }
                        finally {
                            if (Test-Path -LiteralPath $tempJar) {
                                Remove-Item -LiteralPath $tempJar -Force -ErrorAction SilentlyContinue
                            }
                        }
                    }
                }
            }
            finally {
                if ($bundleMapped) {
                    $null = & subst.exe "${bundleDrive}:" "/d"
                    $bundleMapped = $false
                }
            }

            if ($packed) {
                $directoryPath = $directory.FullName
                if (Test-Path -LiteralPath $directoryPath) {
                    if (Test-WindowsOs) {
                        try {
                            $dirItem = Get-Item -LiteralPath $directoryPath -Force
                            $dirItem.Attributes = [System.IO.FileAttributes]::Directory
                        }
                        catch { }
                    }
                    Remove-Item -LiteralPath $directoryPath -Force -ErrorAction SilentlyContinue
                }
                # subst can keep the directory busy for a moment after subst /d.
                if ((Test-Path -LiteralPath $directoryPath) -and (Test-WindowsOs)) {
                    Start-Sleep -Milliseconds 300
                    Remove-Item -LiteralPath $directoryPath -Force -ErrorAction SilentlyContinue
                }
                if (Test-Path -LiteralPath $directoryPath) {
                    throw "Jarred $name but the exploded directory is still present: $directoryPath"
                }
                Write-Host "Jarred: $name"
                $jarred += $name
            }
        }
    }
    finally {
        if ($pluginsMapped) {
            $null = & subst.exe "${pluginsDrive}:" "/d"
        }
    }

    $infoText = Read-Utf8Text $bundlesInfo
    foreach ($name in $jarred) {
        $jarPattern = 'plugins[/\\]' + [regex]::Escape($name) + '\.jar'
        $dirPattern = 'plugins[/\\]' + [regex]::Escape($name) + '[/\\]'
        if (-not [regex]::IsMatch($infoText, $jarPattern)) {
            throw "bundles.info does not reference plugins/$name.jar after packing"
        }
        if ([regex]::IsMatch($infoText, $dirPattern)) {
            throw "bundles.info still references the directory form of $name"
        }
        $jarPath = [System.IO.Path]::Combine($pluginsDir, ($name + ".jar"))
        if (-not [System.IO.File]::Exists($jarPath)) {
            throw "Missing jar after packing: $jarPath"
        }
        if ([System.IO.Directory]::Exists([System.IO.Path]::Combine($pluginsDir, $name))) {
            throw "Exploded directory still exists after packing: $name"
        }
    }

    Write-Host ("Directory plug-ins jarred: " + $jarred.Count + "; kept: " + $kept.Count + "; skipped: " + $skipped.Count)
    return [pscustomobject]@{
        Jarred = @($jarred)
        Kept = @($kept)
        Skipped = @($skipped)
    }
}

$rejarInvokedAsScript = -not ($MyInvocation.InvocationName -eq ".")
if ($rejarInvokedAsScript) {
    if ([string]::IsNullOrWhiteSpace($ProductRoot)) {
        throw "Pass -ProductRoot, the materialized win64 product directory (or the staged copy)."
    }
    Invoke-RejarDirectoryPlugins -ProductRoot $ProductRoot -JarExe $JarExe | Out-Null
}
