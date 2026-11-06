param(
	[string]$OpenChromRoot = "e:\OpenChrom\openchrom\openchrom"
)

$ErrorActionPreference = "Stop"
$pluginsRoot = Join-Path $OpenChromRoot "plugins"
$scriptsRoot = Join-Path $OpenChromRoot "releng\scripts"
$utf8 = [System.Text.UTF8Encoding]::new($false)

$settings = "eclipse.preferences.version=1`r`neclipse.project.prefersNature=false`r`nencoding/<project>=UTF-8`r`n"

$classpathResourceOnly = @"
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-21"/>
	<classpathentry kind="con" path="org.eclipse.pde.core.requiredPlugins"/>
	<classpathentry kind="src" path="src"/>
	<classpathentry kind="output" path="bin"/>
</classpath>
"@

$buildPropsResourceOnly = @"
source.. = src/
output.. = bin/
bin.includes = META-INF/,\
               .
"@

$buildPropsWithOsgi = @"
source.. = src/
output.. = bin/
bin.includes = META-INF/,\
               .,\
               OSGI-INF/
"@

$projectResourceOnly = @"
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>__PLUGIN_ID__</name>
	<comment></comment>
	<projects>
	</projects>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>org.eclipse.pde.ManifestBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>org.eclipse.pde.SchemaBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
	</buildSpec>
	<natures>
		<nature>org.eclipse.pde.PluginNature</nature>
		<nature>org.eclipse.jdt.core.javanature</nature>
	</natures>
</projectDescription>
"@

function New-FragmentManifest {
	param(
		[string]$Id,
		[string]$Name,
		[string]$HostBundle,
		[string]$HostVersion
	)
	return @"
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: $Name
Bundle-SymbolicName: $Id;singleton:=true
Bundle-Version: 1.0.0.qualifier
Bundle-Vendor: Laiende
Fragment-Host: $HostBundle;bundle-version="$HostVersion"
Bundle-RequiredExecutionEnvironment: JavaSE-21
Automatic-Module-Name: $Id
"@
}

$fragmentPlugins = @(
	@{ Id = "net.openchrom.nl.zh.chemclipse.rcp.app.ui"; Build = $buildPropsWithOsgi; Classpath = $classpathResourceOnly; Project = $projectResourceOnly; Manifest = (New-FragmentManifest "net.openchrom.nl.zh.chemclipse.rcp.app.ui" "Laiende Chinese Language Fragment for ChemClipse RCP App UI" "org.eclipse.chemclipse.rcp.app.ui" "0.9.0") },
	@{ Id = "net.openchrom.nl.zh.chemclipse.ux.extension.xxd.ui"; Build = $buildPropsWithOsgi; Classpath = $classpathResourceOnly; Project = $projectResourceOnly; Manifest = (New-FragmentManifest "net.openchrom.nl.zh.chemclipse.ux.extension.xxd.ui" "Laiende Chinese Language Fragment for ChemClipse XXD UI" "org.eclipse.chemclipse.ux.extension.xxd.ui" "0.9.0") },
	@{ Id = "net.openchrom.nl.zh.chemclipse.xxd.process.supplier.pca.ui"; Build = $buildPropsResourceOnly; Classpath = $classpathResourceOnly; Project = $projectResourceOnly; Manifest = (New-FragmentManifest "net.openchrom.nl.zh.chemclipse.xxd.process.supplier.pca.ui" "Laiende Chinese Help Fragment for PCA UI" "org.eclipse.chemclipse.xxd.process.supplier.pca.ui" "0.9.0") },
	@{ Id = "net.openchrom.nl.zh.openchrom.xxd.process.supplier.templates.ui"; Build = $buildPropsResourceOnly; Classpath = $classpathResourceOnly; Project = $projectResourceOnly; Manifest = (New-FragmentManifest "net.openchrom.nl.zh.openchrom.xxd.process.supplier.templates.ui" "Laiende Chinese Help Fragment for Templates UI" "net.openchrom.xxd.process.supplier.templates.ui" "1.6.20") },
	@{ Id = "net.openchrom.nl.zh.chemclipse.chromatogram.msd.filter.supplier.subtract.ui"; Build = $buildPropsResourceOnly; Classpath = $classpathResourceOnly; Project = $projectResourceOnly; Manifest = (New-FragmentManifest "net.openchrom.nl.zh.chemclipse.chromatogram.msd.filter.supplier.subtract.ui" "Laiende Chinese Help Fragment for MS Subtract UI" "org.eclipse.chemclipse.chromatogram.msd.filter.supplier.subtract.ui" "0.9.0") },
	@{ Id = "net.openchrom.nl.zh.eclipse.help.base"; Build = $buildPropsResourceOnly; Classpath = $classpathResourceOnly; Project = $projectResourceOnly; Manifest = (New-FragmentManifest "net.openchrom.nl.zh.eclipse.help.base" "Laiende Chinese Help Fragment for Eclipse Help Base" "org.eclipse.help.base" "4.0.0") }
)

function Sync-HelpNlOutput {
	param(
		[string]$PluginDir
	)
	$nlSource = Join-Path $PluginDir "src\nl"
	if (-not (Test-Path $nlSource)) { return }

	$binDir = Join-Path $PluginDir "bin"
	New-Item -ItemType Directory -Force -Path $binDir | Out-Null
	$binNl = Join-Path $binDir "nl"
	if (Test-Path $binNl) { Remove-Item $binNl -Recurse -Force }
	Copy-Item $nlSource $binNl -Recurse -Force

	# Keep a root copy for inspection only; runtime uses dev.properties=bin.
	$rootNl = Join-Path $PluginDir "nl"
	if (Test-Path $rootNl) { Remove-Item $rootNl -Recurse -Force }
	Copy-Item $nlSource $rootNl -Recurse -Force
}

foreach ($plugin in $fragmentPlugins) {
	$dir = Join-Path $pluginsRoot $plugin.Id
	if (-not (Test-Path $dir)) {
		throw "Missing plugin directory: $dir"
	}
	New-Item -ItemType Directory -Force -Path (Join-Path $dir "src") | Out-Null
	$metaInfDir = Join-Path $dir "META-INF"
	New-Item -ItemType Directory -Force -Path $metaInfDir | Out-Null
	$manifestPath = Join-Path $metaInfDir "MANIFEST.MF"
	if (-not (Test-Path $manifestPath)) {
		[System.IO.File]::WriteAllText($manifestPath, $plugin.Manifest + "`r`n", $utf8)
	}
	[System.IO.File]::WriteAllText((Join-Path $dir "build.properties"), $plugin.Build, $utf8)
	[System.IO.File]::WriteAllText((Join-Path $dir ".classpath"), $plugin.Classpath, $utf8)
	if ($null -ne $plugin.Project) {
		$projectXml = $plugin.Project.Replace("__PLUGIN_ID__", $plugin.Id)
		[System.IO.File]::WriteAllText((Join-Path $dir ".project"), $projectXml, $utf8)
	}
	$settingsDir = Join-Path $dir ".settings"
	New-Item -ItemType Directory -Force -Path $settingsDir | Out-Null
	[System.IO.File]::WriteAllText((Join-Path $settingsDir "org.eclipse.core.resources.prefs"), $settings, $utf8)
}

python (Join-Path $scriptsRoot "write-help-nl-phase1.py")
python (Join-Path $scriptsRoot "write-help-nl-phase2.py")
python (Join-Path $scriptsRoot "write-help-nl-phase3.py")

foreach ($plugin in $fragmentPlugins) {
	Sync-HelpNlOutput (Join-Path $pluginsRoot $plugin.Id)
}

Write-Host "Help NL plugins prepared (src/nl synced to bin/nl)."