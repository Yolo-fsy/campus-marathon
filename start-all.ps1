# ============================================================
# 校园马拉松系统 · 一键启动 / 停止脚本
#
# 用法：
#   .\start-all.ps1            启动 MySQL + Tomcat
#   .\start-all.ps1 -Stop      停止 Tomcat + MySQL
#   .\start-all.ps1 -Status    查看当前状态
#
# 说明：MySQL 使用临时数据目录（%TEMP%\mysql-analytics-data），
#       Tomcat 使用本地 8081 端口。若已在运行会自动跳过。
# ============================================================

param(
    [switch]$Stop,
    [switch]$Status
)

$ErrorActionPreference = 'Stop'

# ---------- 环境路径 ----------
$MySqlHome   = 'D:\MySQL\MySQL Server 8.0\bin'
$ScriptDir   = Split-Path -Parent $MyInvocation.MyCommand.Path
$MySqlData   = Join-Path $ScriptDir 'mysqldata'
$MySqlLog    = Join-Path $ScriptDir 'mysqldata\mysql-console.log'
$TomcatHome  = Join-Path $env:TEMP 'marathon-tools\apache-tomcat-9.0.98'
$JavaHome    = 'C:\Program Files\Java\jdk-17.0.1'

$AppUrl      = 'http://localhost:8081/campus-marathon/login'

function Test-Port([int]$Port) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $iar = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        if ($iar.AsyncWaitHandle.WaitOne(800)) {
            $client.EndConnect($iar)
            return $true
        }
        return $false
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Show-Status {
    $mysql  = Test-Port 3306
    $tomcat = Test-Port 8081
    Write-Host ''
    Write-Host '  服务状态' -ForegroundColor Cyan
    Write-Host ("  MySQL  :3306   " + $(if ($mysql)  { '运行中' } else { '已停止' })) -ForegroundColor $(if ($mysql)  { 'Green' } else { 'DarkGray' })
    Write-Host ("  Tomcat :8081   " + $(if ($tomcat) { '运行中' } else { '已停止' })) -ForegroundColor $(if ($tomcat) { 'Green' } else { 'DarkGray' })
    Write-Host ''
}

# ---------- 停止 ----------
if ($Stop) {
    Write-Host '[1/2] 停止 Tomcat ...' -ForegroundColor Yellow
    if (Test-Port 8081) {
        $env:CATALINA_HOME = $TomcatHome
        $env:CATALINA_BASE = $TomcatHome
        $env:JAVA_HOME     = $JavaHome
        try { & "$TomcatHome\bin\catalina.bat" stop *>&1 | Out-Null } catch { }
        for ($i = 0; $i -lt 20 -and (Test-Port 8081); $i++) { Start-Sleep -Milliseconds 500 }
        if (Test-Port 8081) {
            # 仅结束 Tomcat 的 java 进程，避免误杀 IDE 等其它 Java 程序
            Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
                Where-Object { $_.CommandLine -like '*catalina*' } |
                ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
            Start-Sleep -Seconds 3
        }
        Write-Host '      Tomcat 已停止'
    } else {
        Write-Host '      Tomcat 未运行，跳过'
    }

    Write-Host '[2/2] 停止 MySQL ...' -ForegroundColor Yellow
    if (Test-Port 3306) {
        # 原生命令的 stderr 警告不应中断流程
        $ErrorActionPreference = 'Continue'
        & "$MySqlHome\mysqladmin.exe" -u root -p123456 shutdown *>&1 | Out-Null
        for ($i = 0; $i -lt 40 -and (Test-Port 3306); $i++) { Start-Sleep -Milliseconds 500 }
        $ErrorActionPreference = 'Stop'
        if (Test-Port 3306) {
            Write-Host '      MySQL 停止超时，请手动检查' -ForegroundColor Red
        } else {
            Write-Host '      MySQL 已停止'
        }
    } else {
        Write-Host '      MySQL 未运行，跳过'
    }

    Show-Status
    return
}

if ($Status) {
    Show-Status
    return
}

# ---------- 启动 ----------
if (-not (Test-Path $MySqlData)) {
    throw "MySQL 数据目录不存在：$MySqlData"
}
if (-not (Test-Path "$TomcatHome\bin\catalina.bat")) {
    throw "Tomcat 目录不存在：$TomcatHome"
}
if (-not (Test-Path "$TomcatHome\webapps\campus-marathon.war")) {
    Write-Host '警告：未找到已部署的 campus-marathon.war' -ForegroundColor Yellow
}

Write-Host '[1/2] 启动 MySQL ...' -ForegroundColor Yellow
$existing = @()
if (Test-Port 3306) {
    # 已在监听，无需处理（下方统一输出结果）
} else {
    # 端口未通但已有 mysqld 进程时，可能正在启动或正在退出，绝不能重复拉起
    # （两个 mysqld 同时挂同一个数据目录会导致数据损坏）
    $existing = @(Get-Process -Name mysqld -ErrorAction SilentlyContinue)
    if ($existing.Count -gt 0) {
        Write-Host '      检测到已有 mysqld 进程，等待其就绪 ...'
        $ok = $false
        for ($i = 0; $i -lt 60; $i++) {
            if (Test-Port 3306) { $ok = $true; break }
            Start-Sleep -Milliseconds 500
        }
        if (-not $ok) {
            Write-Host '      该进程未在超时内监听端口，清理后重新启动' -ForegroundColor Yellow
            $existing | ForEach-Object { Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue }
            Start-Sleep -Seconds 3
        }
    }
}

if (-not (Test-Port 3306)) {
    # 通过 WMI 创建独立进程，避免 mysqld 随本脚本退出而被回收
    $inner = "& '$MySqlHome\mysqld.exe' --datadir='$MySqlData' --port=3306 " +
             "*>> '$MySqlLog'"
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($inner))
    $created = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{
        CommandLine = "powershell.exe -NoProfile -ExecutionPolicy Bypass -EncodedCommand $encoded"
    }
    if ($created.ReturnValue -ne 0) { throw "MySQL 进程创建失败，代码 $($created.ReturnValue)" }
    $ok = $false
    for ($i = 0; $i -lt 60; $i++) {
        if (Test-Port 3306) { $ok = $true; break }
        Start-Sleep -Milliseconds 500
    }
    if (-not $ok) { throw 'MySQL 启动超时，请检查数据目录权限' }
    Write-Host '      MySQL 已启动'
} elseif ($existing.Count -eq 0) {
    Write-Host '      MySQL 已在运行，跳过'
} else {
    Write-Host '      MySQL 已就绪'
}

Write-Host '[2/2] 启动 Tomcat ...' -ForegroundColor Yellow
if (Test-Port 8081) {
    Write-Host '      Tomcat 已在运行，跳过'
} else {
    $env:JAVA_HOME     = $JavaHome
    $env:CATALINA_HOME = $TomcatHome
    $env:CATALINA_BASE = $TomcatHome
    # 通过 WMI 创建独立进程，避免 Tomcat 随本脚本所在进程退出而被回收
    $inner = "`$env:JAVA_HOME='$JavaHome'; `$env:CATALINA_HOME='$TomcatHome'; " +
             "`$env:CATALINA_BASE='$TomcatHome'; " +
             "& '$TomcatHome\bin\catalina.bat' run *>> '$TomcatHome\logs\console-out.log'"
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($inner))
    $created = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{
        CommandLine = "powershell.exe -NoProfile -ExecutionPolicy Bypass -EncodedCommand $encoded"
    }
    if ($created.ReturnValue -ne 0) { throw "Tomcat 进程创建失败，代码 $($created.ReturnValue)" }

    $ok = $false
    for ($i = 0; $i -lt 180; $i++) {
        if (Test-Port 8081) { $ok = $true; break }
        Start-Sleep -Milliseconds 1000
    }
    if (-not $ok) { throw 'Tomcat 启动超时，查看 logs\catalina*.log' }
    Write-Host '      Tomcat 已启动'
}

Show-Status
Write-Host '  访问地址：' -ForegroundColor Cyan
Write-Host "  登录页  $AppUrl"
Write-Host '  账号    admin / admin123'
Write-Host ''
