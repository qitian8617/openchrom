param(
	[int]$StartupTimeoutSec = 180,
	[int]$ProductPid = 0,
	[string]$RuntimeDir = "E:\OpenChrom\runtime\openchrom.compilation.community.product"
)
Add-Type -AssemblyName UIAutomationClient,UIAutomationTypes,System.Windows.Forms

function Wait-ForMainWindow([int]$TimeoutSec) {
	$deadline = (Get-Date).AddSeconds($TimeoutSec)
	while((Get-Date) -lt $deadline) {
		$proc = Get-Process -Name "eclipse","javaw" -ErrorAction SilentlyContinue | Where-Object {
			($ProductPid -eq 0 -or $_.Id -eq $ProductPid) -and $_.MainWindowHandle -ne 0 -and $_.MainWindowTitle -notmatch "Eclipse IDE"
		} | Select-Object -First 1
		if($proc) { return $proc }
		Start-Sleep -Seconds 2
	}
}

function Invoke-MenuPath($Window, [string[]]$Path) {
	$menuBar = $Window.FindFirst([System.Windows.Automation.TreeScope]::Descendants,
		(New-Object System.Windows.Automation.PropertyCondition(
			[System.Windows.Automation.AutomationElement]::ControlTypeProperty,
			[System.Windows.Automation.ControlType]::MenuBar)))
	if(-not $menuBar) { throw "Menu bar not found." }
	$current = $menuBar
	foreach($label in $Path) {
		$items = $current.FindAll([System.Windows.Automation.TreeScope]::Children,
			[System.Windows.Automation.Condition]::TrueCondition)
		$next = $null
		foreach($item in $items) {
			$name = $item.Current.Name
			if($name -and ($name -eq $label -or $name.Contains($label))) { $next = $item; break }
		}
		if(-not $next) { throw "Menu item not found: $label" }
		$expand = $null
		try { $expand = $next.GetCurrentPattern([System.Windows.Automation.ExpandCollapsePattern]::Pattern) } catch {}
		if($expand) { $expand.Expand() } else { [System.Windows.Automation.AutomationElement]::SetFocus($next) }
		Start-Sleep -Milliseconds 500
		$current = $next
	}
	try {
		$invoke = $current.GetCurrentPattern([System.Windows.Automation.InvokePattern]::Pattern)
		if($invoke) { $invoke.Invoke() }
	} catch {}
}

function Test-EditorVisible($Window) {
	$keywords = @("Default Profile","ProcessMethod","Name","Description","Profile","处理方法","默认","Profile")
	$all = $Window.FindAll([System.Windows.Automation.TreeScope]::Descendants,
		[System.Windows.Automation.Condition]::TrueCondition)
	$hits = New-Object System.Collections.Generic.List[string]
	foreach($item in $all) {
		$name = $item.Current.Name
		if([string]::IsNullOrWhiteSpace($name)) { continue }
		foreach($kw in $keywords) {
			if($name.Contains($kw)) { [void]$hits.Add($name); break }
		}
	}
	return $hits
}

$proc = Wait-ForMainWindow $StartupTimeoutSec
if(-not $proc) { throw "PDE product main window did not appear within ${StartupTimeoutSec}s." }
Write-Host "Main window: $($proc.MainWindowTitle) (PID $($proc.Id))"
$window = [System.Windows.Automation.AutomationElement]::FromHandle([IntPtr]$proc.MainWindowHandle)
if(-not $window) { throw "Could not resolve automation root." }
[System.Windows.Automation.AutomationElement]::SetFocus($window)
Start-Sleep -Seconds 3

$menuPaths = @(
	,@("\u5904\u7406","\u521b\u5efa\u5904\u7406\u65b9\u6cd5"),
	,@("\u5904\u7406\u5668","\u521b\u5efa\u5904\u7406\u65b9\u6cd5")
)
$menuTried = $false
$editorHits = New-Object System.Collections.Generic.List[string]
foreach($menuPath in $menuPaths) {
	try {
		Invoke-MenuPath $window $menuPath
		$menuTried = $true
		Start-Sleep -Seconds 2
		1..4 | ForEach-Object { [System.Windows.Forms.SendKeys]::SendWait("{ENTER}"); Start-Sleep -Milliseconds 700 }
		Start-Sleep -Seconds 5
		$editorHits = Test-EditorVisible $window
		if($editorHits.Count -gt 0) { break }
	} catch {
		Write-Host "Menu path failed: $($menuPath -join ' -> ') :: $($_.Exception.Message)"
	}
}

$logFile = Join-Path $RuntimeDir ".metadata\.log"
$logHits = @()
if(Test-Path $logFile) {
	$logHits = Select-String -Path $logFile -Pattern "Initializing process method editor|Process method editor initialized|Failed to initialize the process method editor" -ErrorAction SilentlyContinue
}
Write-Host "UI keyword hits: $($editorHits.Count)"
$editorHits | Select-Object -First 10 | ForEach-Object { Write-Host "  UI: $_" }
if($logHits) { $logHits | ForEach-Object { Write-Host "  LOG: $($_.Line.Trim())" } }

if($editorHits.Count -ge 2 -or ($logHits -match "Process method editor initialized")) {
	Write-Host "SMOKE TEST PASSED"
	exit 0
}
if(-not $menuTried) { throw "Could not open Create Process Method menu." }
throw "SMOKE TEST FAILED: editor UI still blank."