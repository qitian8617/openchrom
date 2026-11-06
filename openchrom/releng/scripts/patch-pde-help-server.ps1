# Patches PDE product config.ini for help/Jetty bundles, SPI Fly, and SLF4J autostart.
param(
	[string]$Workspace = "E:\OpenChrom",
	[string]$ProductConfigName = "openchrom.compilation.community.product"
)

$configIni = Join-Path $Workspace ".metadata\.plugins\org.eclipse.pde.core\$ProductConfigName\config.ini"
$bundlePool = Join-Path $Workspace ".metadata\.plugins\org.eclipse.pde.core\.bundle_pool\plugins"

if(-not (Test-Path $configIni) -or -not (Test-Path $bundlePool)) {
	return
}

$config = Get-Content $configIni -Raw -Encoding UTF8
$changed = $false

if($config -notmatch 'org\.eclipse\.equinox\.http\.jetty') {
	$bundleSpecs = @(
		@{ Id = "org.eclipse.equinox.http.jetty"; Start = $true; Level = 5 },
		@{ Id = "org.eclipse.equinox.http.registry"; Start = $true; Level = 5 },
		@{ Id = "org.eclipse.equinox.http.servlet"; Start = $false; Level = 0 },
		@{ Id = "org.eclipse.equinox.jsp.jasper"; Start = $false; Level = 0 },
		@{ Id = "org.eclipse.equinox.jsp.jasper.registry"; Start = $false; Level = 0 },
		@{ Id = "org.eclipse.help.webapp"; Start = $false; Level = 0 },
		@{ Id = "org.eclipse.jetty.ee8.security"; Start = $false; Level = 0 },
		@{ Id = "org.eclipse.jetty.ee8.server"; Start = $false; Level = 0 },
		@{ Id = "org.eclipse.jetty.ee8.servlet"; Start = $false; Level = 0 },
		@{ Id = "org.eclipse.jetty.http"; Start = $false; Level = 0 },
		@{ Id = "org.eclipse.jetty.io"; Start = $false; Level = 0 },
		@{ Id = "org.eclipse.jetty.security"; Start = $false; Level = 0 },
		@{ Id = "org.eclipse.jetty.server"; Start = $false; Level = 0 },
		@{ Id = "org.eclipse.jetty.session"; Start = $false; Level = 0 },
		@{ Id = "org.eclipse.jetty.servlet-api"; Start = $false; Level = 0 },
		@{ Id = "org.eclipse.jetty.util"; Start = $false; Level = 0 }
	)
	$references = New-Object System.Collections.Generic.List[string]
	foreach($spec in $bundleSpecs) {
		$bundleJar = Get-ChildItem $bundlePool -Filter "$($spec.Id)_*.jar" |
			Where-Object { $_.Name -notmatch '\.(source|nl_)' } |
			Sort-Object Name -Descending |
			Select-Object -First 1
		if($null -eq $bundleJar) {
			Write-Warning "Help server bundle not found in pool: $($spec.Id)"
			continue
		}
		$path = ($bundleJar.FullName -replace '\\', '/')
		$suffix = ""
		if($spec.Start) {
			$suffix = "@$($spec.Level)\:start"
		}
		$references.Add("reference\:file\:$path$suffix")
	}
	if($references.Count -gt 0) {
		$config = $config -replace '(osgi\.bundles=.*?)(\r?\nosgi\.bundles\.defaultStartLevel=)', "`$1,$($references -join ',')`$2"
		$changed = $true
	}
}

if($config -match 'org\.apache\.aries\.spifly\.dynamic\.bundle_[^,\\]+\.jar(?!@[0-9]+\\:start)') {
	$config = [regex]::Replace($config, 'org\.apache\.aries\.spifly\.dynamic\.bundle_[^,\\]+\.jar', { param($m) "$($m.Value)@1\:start" }, 1)
	$changed = $true
}

if($config -match 'slf4j\.simple_[^,\\]+\.jar(?!@[0-9]+\\:start)') {
	$config = [regex]::Replace($config, 'slf4j\.simple_[^,\\]+\.jar(@[0-9]+)?', { param($m) "slf4j.simple_2.0.18.jar@2\:start" }, 1)
	$changed = $true
}

if($changed) {
	[System.IO.File]::WriteAllText($configIni, $config, [System.Text.UTF8Encoding]::new($false))
	Write-Host "Patched PDE config.ini: $configIni"
}