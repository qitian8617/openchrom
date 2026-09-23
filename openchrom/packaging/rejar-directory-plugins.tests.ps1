#Requires -Version 5.1
<#
.SYNOPSIS
  Fixture test for packaging\rejar-directory-plugins.ps1.
  Run: pwsh -File packaging/rejar-directory-plugins.tests.ps1
#>
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "rejar-directory-plugins.ps1")

function Assert-True {
    param($Condition, [string]$Message)
    if (-not $Condition) {
        throw "ASSERT: $Message"
    }
}

function New-BundleDir {
    param([string]$Plugins, [string]$Name, [string]$SymbolicName, [switch]$WithExe)
    $root = [System.IO.Path]::Combine($Plugins, $Name)
    $meta = [System.IO.Path]::Combine($root, "META-INF")
    [System.IO.Directory]::CreateDirectory($meta) | Out-Null
    $manifest = "Manifest-Version: 1.0`r`nBundle-SymbolicName: $SymbolicName`r`nBundle-Version: 1.0.0`r`nEclipse-BundleShape: dir`r`n`r`n"
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText([System.IO.Path]::Combine($meta, "MANIFEST.MF"), $manifest, $utf8)
    if ($WithExe) {
        [System.IO.File]::WriteAllText([System.IO.Path]::Combine($root, "readme.exe"), "not-a-real-binary")
    }
    return $root
}

function Test-BundlesInfoRewriter {
    $foo = "com.example.foo_1.0.0"
    $bar = "com.example.foo.bar_1.0.0"
    $text = @(
        "#encoding=UTF-8"
        "com.example.foo,1.0.0,../../plugins/$foo/,4,false"
        "com.example.foo.bar,1.0.0,plugins/$bar/,4,false"
        "com.example.foo.extra,1.0.0,plugins/${foo}.extra/,4,false"
        "already.jar,1.0.0,plugins/already.jar_1.0.0.jar,4,false"
        "bare,1.0.0,plugins/bare.bundle_1.0.0,4,false"
        "win,1.0.0,plugins\win.bundle_1.0.0\,4,false"
        "url,1.0.0,file:/E:/OpenChrom/plugins/$foo/,4,false"
    ) -join "`r`n"
    $text = $text + "`r`n"

    $once = Update-BundlesInfoText -Text $text -DirectoryNames @($foo)
    Assert-True ($once.Missing.Count -eq 0) "foo should be found"
    Assert-True ($once.Text.Contains("plugins/$foo.jar")) "foo directory should become a jar reference"
    Assert-True (-not $once.Text.Contains("plugins/$foo/")) "foo directory form should be gone"
    Assert-True ($once.Text.Contains("plugins/$bar/")) "foo.bar must not be rewritten with foo"
    Assert-True ($once.Text.Contains("plugins/${foo}.extra/")) "a longer directory name must not be rewritten"
    Assert-True ($once.Text.Contains("file:/E:/OpenChrom/plugins/$foo.jar")) "file URL directory form should become a jar"

    $twice = Update-BundlesInfoText -Text $once.Text -DirectoryNames @($foo)
    Assert-True ($twice.Text -eq $once.Text) "rewriting foo again must be a no-op"
    Assert-True (-not $twice.Text.Contains("$foo.jar.jar")) "jar reference must not be doubled"

    $rest = Update-BundlesInfoText -Text $twice.Text -DirectoryNames @("bare.bundle_1.0.0", "win.bundle_1.0.0", "missing.bundle_1.0.0")
    Assert-True ($rest.Text.Contains("plugins/bare.bundle_1.0.0.jar,4,false")) "bare directory location should gain .jar"
    Assert-True ($rest.Text.Contains("plugins/win.bundle_1.0.0.jar,4,false")) "backslash directory location should become a forward-slash jar"
    Assert-True ($rest.Missing -contains "missing.bundle_1.0.0") "unknown bundle should be reported missing"
    Assert-True ($rest.Text.StartsWith("#encoding=UTF-8`r`n")) "header and CRLF should be preserved"
}

function New-FixtureRoot {
    $root = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), ("rejar-fixture-" + [guid]::NewGuid().ToString("n")))
    $plugins = [System.IO.Path]::Combine($root, "plugins")
    $configDir = [System.IO.Path]::Combine($root, "configuration", "org.eclipse.equinox.simpleconfigurator")
    [System.IO.Directory]::CreateDirectory($plugins) | Out-Null
    [System.IO.Directory]::CreateDirectory($configDir) | Out-Null

    $amdis = "org.eclipse.chemclipse.chromatogram.xxd.calculator.supplier.amdiscalri_9.0.0"
    $amdisRoot = New-BundleDir -Plugins $plugins -Name $amdis -SymbolicName "org.eclipse.chemclipse.chromatogram.xxd.calculator.supplier.amdiscalri" -WithExe
    $classDir = [System.IO.Path]::Combine(
        $amdisRoot, "org", "eclipse", "chemclipse", "chromatogram", "xxd", "calculator", "supplier", "amdiscalri", "impl")
    [System.IO.Directory]::CreateDirectory($classDir) | Out-Null
    [System.IO.File]::WriteAllBytes(
        [System.IO.Path]::Combine($classDir, 'ChromatogramRetentionIndexRecalculator$ProcessSupplier.class'),
        [byte[]](1, 2, 3, 4))

    New-BundleDir -Plugins $plugins -Name "net.openchrom.feature.branding_1.6.32" -SymbolicName "net.openchrom.feature.branding" | Out-Null
    New-BundleDir -Plugins $plugins -Name "org.eclipse.ui.themes_1.2.0" -SymbolicName "org.eclipse.ui.themes" | Out-Null
    New-BundleDir -Plugins $plugins -Name "org.eclipse.chemclipse.chromatogram.xxd.calculator.supplier.molpeak_1.0.0" -SymbolicName "org.eclipse.chemclipse.chromatogram.xxd.calculator.supplier.molpeak" | Out-Null
    New-BundleDir -Plugins $plugins -Name "com.sun.jna.platform_5.14.0" -SymbolicName "com.sun.jna.platform" | Out-Null
    New-BundleDir -Plugins $plugins -Name "com.example.foo_1.0.0" -SymbolicName "com.example.foo" | Out-Null
    New-BundleDir -Plugins $plugins -Name "com.example.foo.bar_1.0.0" -SymbolicName "com.example.foo.bar" | Out-Null

    New-BundleDir -Plugins $plugins -Name "org.eclipse.equinox.launcher.win32.win32.x86_64_1.2.900" -SymbolicName "org.eclipse.equinox.launcher.win32.win32.x86_64" | Out-Null
    New-BundleDir -Plugins $plugins -Name "org.eclipse.justj.openjdk.hotspot.jre.full.stripped.win32.x86_64_21.0.5" -SymbolicName "org.eclipse.justj.openjdk.hotspot.jre.full.stripped.win32.x86_64" | Out-Null
    New-BundleDir -Plugins $plugins -Name "com.sun.jna_5.14.0" -SymbolicName "com.sun.jna" | Out-Null

    $swt = New-BundleDir -Plugins $plugins -Name "org.eclipse.swt.win32.win32.x86_64_3.128.0" -SymbolicName "org.eclipse.swt.win32.win32.x86_64"
    [System.IO.File]::WriteAllText([System.IO.Path]::Combine($swt, "swt-win32-4960.dll"), "dll")

    $stray = [System.IO.Path]::Combine($plugins, "not.a.bundle_1.0.0")
    [System.IO.Directory]::CreateDirectory($stray) | Out-Null
    [System.IO.File]::WriteAllText([System.IO.Path]::Combine($stray, "readme.txt"), "stray")

    [System.IO.File]::WriteAllText([System.IO.Path]::Combine($plugins, "already.a.jar_1.0.0.jar"), "not-really")

    $lines = @(
        "#encoding=UTF-8"
        "#version=1"
        "org.eclipse.equinox.simpleconfigurator,1.5.0,plugins/org.eclipse.equinox.simpleconfigurator_1.5.0.jar,1,true"
        "org.eclipse.chemclipse.chromatogram.xxd.calculator.supplier.amdiscalri,9.0.0,../../plugins/org.eclipse.chemclipse.chromatogram.xxd.calculator.supplier.amdiscalri_9.0.0/,4,false"
        "net.openchrom.feature.branding,1.6.32,plugins/net.openchrom.feature.branding_1.6.32/,4,false"
        "org.eclipse.ui.themes,1.2.0,plugins\org.eclipse.ui.themes_1.2.0\,4,false"
        "org.eclipse.chemclipse.chromatogram.xxd.calculator.supplier.molpeak,1.0.0,plugins/org.eclipse.chemclipse.chromatogram.xxd.calculator.supplier.molpeak_1.0.0,4,false"
        "org.eclipse.equinox.launcher.win32.win32.x86_64,1.2.900,plugins/org.eclipse.equinox.launcher.win32.win32.x86_64_1.2.900/,4,false"
        "org.eclipse.justj.openjdk.hotspot.jre.full.stripped.win32.x86_64,21.0.5,plugins/org.eclipse.justj.openjdk.hotspot.jre.full.stripped.win32.x86_64_21.0.5/,4,false"
        "com.sun.jna,5.14.0,plugins/com.sun.jna_5.14.0/,4,false"
        "com.sun.jna.platform,5.14.0,plugins/com.sun.jna.platform_5.14.0/,4,false"
        "org.eclipse.swt.win32.win32.x86_64,3.128.0,plugins/org.eclipse.swt.win32.win32.x86_64_3.128.0/,4,false"
        "com.example.foo,1.0.0,plugins/com.example.foo_1.0.0/,4,false"
        "com.example.foo.bar,1.0.0,plugins/com.example.foo.bar_1.0.0/,4,false"
    )
    $utf8 = New-Object System.Text.UTF8Encoding $false
    $bundlesInfo = [System.IO.Path]::Combine($configDir, "bundles.info")
    [System.IO.File]::WriteAllText($bundlesInfo, (($lines -join "`r`n") + "`r`n"), $utf8)

    $ini = @(
        "-startup"
        "plugins/org.eclipse.equinox.launcher_1.6.0.jar"
        "--launcher.library"
        "plugins/org.eclipse.equinox.launcher.win32.win32.x86_64_1.2.900"
        "-vmargs"
        "-Xmx4096m"
    ) -join "`r`n"
    [System.IO.File]::WriteAllText([System.IO.Path]::Combine($root, "baijiu-fid.ini"), $ini + "`r`n", $utf8)

    $configIni = @(
        "osgi.bundles=reference\:file\:org.eclipse.equinox.simpleconfigurator_1.5.0.jar@1\:start"
        "org.eclipse.update.reconcile=false"
        "osgi.splashPath=platform:/base/plugins/net.openchrom.rcp.compilation.baijiu.ui"
    ) -join "`r`n"
    [System.IO.Directory]::CreateDirectory([System.IO.Path]::Combine($root, "configuration")) | Out-Null
    [System.IO.File]::WriteAllText([System.IO.Path]::Combine($root, "configuration", "config.ini"), $configIni + "`r`n", $utf8)
    return $root
}

function Test-RejarFixture {
    $root = New-FixtureRoot
    try {
        $jar = (Get-Command jar -ErrorAction Stop).Source
        $first = Invoke-RejarDirectoryPlugins -ProductRoot $root -JarExe $jar
        $plugins = [System.IO.Path]::Combine($root, "plugins")
        $amdis = "org.eclipse.chemclipse.chromatogram.xxd.calculator.supplier.amdiscalri_9.0.0"

        Assert-True ($first.Jarred -contains $amdis) "amdiscalri must be jarred even if a .exe resource is present"
        Assert-True ($first.Jarred -contains "net.openchrom.feature.branding_1.6.32") "branding must be jarred"
        Assert-True ($first.Jarred -contains "org.eclipse.ui.themes_1.2.0") "ui.themes must be jarred"
        Assert-True ($first.Jarred -contains "org.eclipse.chemclipse.chromatogram.xxd.calculator.supplier.molpeak_1.0.0") "molpeak must be jarred"
        Assert-True ($first.Jarred -contains "com.sun.jna.platform_5.14.0") "com.sun.jna.platform is Java and must be jarred"
        Assert-True ($first.Jarred -contains "com.example.foo_1.0.0") "foo must be jarred"
        Assert-True ($first.Jarred -contains "com.example.foo.bar_1.0.0") "foo.bar must be jarred"
        Assert-True ($first.Kept -contains "org.eclipse.equinox.launcher.win32.win32.x86_64_1.2.900") "launcher fragment must stay a directory"
        Assert-True ($first.Kept -contains "org.eclipse.justj.openjdk.hotspot.jre.full.stripped.win32.x86_64_21.0.5") "JustJ fragment must stay a directory"
        Assert-True ($first.Kept -contains "com.sun.jna_5.14.0") "com.sun.jna must stay a directory"
        Assert-True ($first.Kept -contains "org.eclipse.swt.win32.win32.x86_64_3.128.0") "SWT fragment with a dll must stay a directory"
        Assert-True ($first.Skipped -contains "not.a.bundle_1.0.0") "a folder without a manifest is skipped"

        Assert-True (-not (Test-Path -LiteralPath ([System.IO.Path]::Combine($plugins, $amdis)))) "exploded amdiscalri directory must be removed"
        Assert-True (Test-Path -LiteralPath ([System.IO.Path]::Combine($plugins, ($amdis + ".jar")))) "amdiscalri jar must exist"
        Assert-True (Test-Path -LiteralPath ([System.IO.Path]::Combine($plugins, "org.eclipse.justj.openjdk.hotspot.jre.full.stripped.win32.x86_64_21.0.5", "META-INF", "MANIFEST.MF"))) "JustJ directory must remain"
        Assert-True (Test-Path -LiteralPath ([System.IO.Path]::Combine($plugins, "com.sun.jna_5.14.0"))) "JNA directory must remain"
        Assert-True (Test-Path -LiteralPath ([System.IO.Path]::Combine($plugins, "not.a.bundle_1.0.0", "readme.txt"))) "non-bundle directory must remain"

        $amdisJar = [System.IO.Path]::Combine($plugins, ($amdis + ".jar"))
        $listing = & $jar --list --file $amdisJar
        $joined = ($listing | Out-String)
        Assert-True ($joined.Contains("META-INF/MANIFEST.MF")) "jar must contain the manifest"
        Assert-True ($joined.Contains('org/eclipse/chemclipse/chromatogram/xxd/calculator/supplier/amdiscalri/impl/ChromatogramRetentionIndexRecalculator$ProcessSupplier.class')) "jar must contain the long class entry"
        Assert-True (-not $joined.Contains('\')) "jar entries must use forward slashes"
        $manifestDir = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), ("rejar-manifest-" + [guid]::NewGuid().ToString("n")))
        [System.IO.Directory]::CreateDirectory($manifestDir) | Out-Null
        $previousDir = Get-Location
        try {
            Set-Location -LiteralPath $manifestDir
            & $jar --extract --file $amdisJar META-INF/MANIFEST.MF
            if ($LASTEXITCODE -ne 0) { throw "jar --extract failed" }
            $manifestText = [System.IO.File]::ReadAllText([System.IO.Path]::Combine($manifestDir, "META-INF", "MANIFEST.MF"))
            Assert-True ($manifestText.Contains("Bundle-SymbolicName: org.eclipse.chemclipse.chromatogram.xxd.calculator.supplier.amdiscalri`r`n")) "original manifest bytes must be stored, not rewritten"
            Assert-True ($manifestText.Contains("Eclipse-BundleShape: dir`r`n")) "Eclipse-BundleShape header must survive re-jar"
        }
        finally {
            Set-Location $previousDir
            Remove-Item -LiteralPath $manifestDir -Recurse -Force
        }

        $infoPath = [System.IO.Path]::Combine($root, "configuration", "org.eclipse.equinox.simpleconfigurator", "bundles.info")
        $infoBytes = [System.IO.File]::ReadAllBytes($infoPath)
        Assert-True (-not ($infoBytes.Length -ge 3 -and $infoBytes[0] -eq 0xEF -and $infoBytes[1] -eq 0xBB -and $infoBytes[2] -eq 0xBF)) "bundles.info must not gain a UTF-8 BOM"
        $info = [System.Text.Encoding]::UTF8.GetString($infoBytes)
        Assert-True ($info.Contains("../../plugins/$amdis.jar")) "amdiscalri bundles.info entry must point at the jar"
        Assert-True (-not $info.Contains("../../plugins/$amdis/")) "amdiscalri directory entry must be gone"
        Assert-True ($info.Contains("plugins/net.openchrom.feature.branding_1.6.32.jar")) "branding entry must be a jar"
        Assert-True ($info.Contains("plugins/org.eclipse.ui.themes_1.2.0.jar")) "themes backslash entry must become a jar path"
        Assert-True ($info.Contains("plugins/org.eclipse.chemclipse.chromatogram.xxd.calculator.supplier.molpeak_1.0.0.jar,4,false")) "bare molpeak entry must gain .jar"
        Assert-True ($info.Contains("plugins/org.eclipse.equinox.launcher.win32.win32.x86_64_1.2.900/")) "launcher fragment entry must stay a directory"
        Assert-True ($info.Contains("plugins/org.eclipse.justj.openjdk.hotspot.jre.full.stripped.win32.x86_64_21.0.5/")) "JustJ entry must stay a directory"
        Assert-True ($info.Contains("plugins/com.sun.jna_5.14.0/")) "JNA entry must stay a directory"
        Assert-True ($info.Contains("plugins/com.sun.jna.platform_5.14.0.jar")) "JNA platform entry must be a jar"
        Assert-True ($info.Contains("plugins/com.example.foo_1.0.0.jar")) "foo entry must be a jar"
        Assert-True ($info.Contains("plugins/com.example.foo.bar_1.0.0.jar")) "foo.bar entry must be a jar"
        Assert-True ($info.Contains("#encoding=UTF-8`r`n#version=1`r`n")) "bundles.info CRLF header must survive"
        Assert-True ($info.Contains("plugins/org.eclipse.swt.win32.win32.x86_64_3.128.0/")) "SWT entry must stay a directory"

        $ini = [System.IO.File]::ReadAllText([System.IO.Path]::Combine($root, "baijiu-fid.ini"))
        Assert-True ($ini.Contains("plugins/org.eclipse.equinox.launcher_1.6.0.jar")) "launcher -startup jar must be unchanged"
        Assert-True ($ini.Contains("--launcher.library`r`nplugins/org.eclipse.equinox.launcher.win32.win32.x86_64_1.2.900`r`n")) "launcher.library directory path must be unchanged"
        $configIni = [System.IO.File]::ReadAllText([System.IO.Path]::Combine($root, "configuration", "config.ini"))
        Assert-True ($configIni.Contains("osgi.splashPath=platform:/base/plugins/net.openchrom.rcp.compilation.baijiu.ui")) "splash path must be unchanged"
        Assert-True ($configIni.Contains("org.eclipse.update.reconcile=false")) "reconcile flag must be unchanged"

        $second = Invoke-RejarDirectoryPlugins -ProductRoot $root -JarExe $jar
        Assert-True ($second.Jarred.Count -eq 0) "second run must not jar anything new"
        Assert-True ($second.Kept -contains "com.sun.jna_5.14.0") "second run must still keep JNA"
        $infoAfter = [System.IO.File]::ReadAllText($infoPath)
        Assert-True ($infoAfter -eq $info) "second run must not rewrite bundles.info"
    }
    finally {
        if (Test-Path -LiteralPath $root) {
            Remove-Item -LiteralPath $root -Recurse -Force
        }
    }
}

function Test-MissingBundlesInfoEntryDoesNotDelete {
    $root = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), ("rejar-missing-" + [guid]::NewGuid().ToString("n")))
    try {
        $plugins = [System.IO.Path]::Combine($root, "plugins")
        $configDir = [System.IO.Path]::Combine($root, "configuration", "org.eclipse.equinox.simpleconfigurator")
        [System.IO.Directory]::CreateDirectory($configDir) | Out-Null
        $name = "org.example.missing_1.0.0"
        New-BundleDir -Plugins $plugins -Name $name -SymbolicName "org.example.missing" | Out-Null
        $utf8 = New-Object System.Text.UTF8Encoding $false
        [System.IO.File]::WriteAllText(
            [System.IO.Path]::Combine($configDir, "bundles.info"),
            "#encoding=UTF-8`r`norg.eclipse.equinox.simpleconfigurator,1.5.0,plugins/org.eclipse.equinox.simpleconfigurator_1.5.0.jar,1,true`r`n",
            $utf8)
        $jar = (Get-Command jar -ErrorAction Stop).Source
        $threw = $false
        try {
            Invoke-RejarDirectoryPlugins -ProductRoot $root -JarExe $jar | Out-Null
        }
        catch {
            $threw = $true
            Assert-True ($_.Exception.Message -match "org.example.missing_1.0.0") "error should name the bundle that has no bundles.info entry"
        }
        Assert-True $threw "missing bundles.info entry must fail the re-jar"
        Assert-True (Test-Path -LiteralPath ([System.IO.Path]::Combine($plugins, $name, "META-INF", "MANIFEST.MF"))) "source directory must remain when bundles.info cannot be updated"
        Assert-True (-not (Test-Path -LiteralPath ([System.IO.Path]::Combine($plugins, ($name + ".jar"))))) "no jar should be published when bundles.info cannot be updated"
    }
    finally {
        if (Test-Path -LiteralPath $root) {
            Remove-Item -LiteralPath $root -Recurse -Force
        }
    }
}

Test-BundlesInfoRewriter
Test-RejarFixture
Test-MissingBundlesInfoEntryDoesNotDelete
Write-Host "rejar-directory-plugins tests passed"
