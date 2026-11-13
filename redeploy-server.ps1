# ============================================================
# 重新部署校园马拉松系统到云服务器
#
# 用法（在本目录的 PowerShell 中执行）：
#   .\redeploy-server.ps1
#
# 执行过程中会提示输入 SSH 密码（最多 2 次），脚本不保存密码。
# 服务器上原有的 db.properties / ai.properties 会自动保留。
# ============================================================

param(
    [string]$Server = 'ubuntu@101.42.186.195',
    [string]$War    = 'target\campus-marathon.war'
)

$ErrorActionPreference = 'Stop'
$App = 'campus-marathon'

if (-not (Test-Path $War)) {
    throw "找不到 WAR 包：$War，请先执行 mvn package"
}

Write-Host '[1/3] 上传 WAR 与部署脚本（会提示输入 SSH 密码）' -ForegroundColor Cyan
scp $War "deploy-remote.sh" "${Server}:/tmp/"
if ($LASTEXITCODE -ne 0) { throw '上传失败，请检查密码或网络' }

Write-Host '[2/3] 在服务器上备份并替换（可能再次提示密码）' -ForegroundColor Cyan
ssh $Server "sed -i 's/\r$//' /tmp/deploy-remote.sh && bash /tmp/deploy-remote.sh"
if ($LASTEXITCODE -ne 0) { throw '远程部署失败，请查看上方输出' }

Write-Host '[3/3] 验证公网访问' -ForegroundColor Cyan
Start-Sleep -Seconds 3
$url = 'http://101.42.186.195/campus-marathon/login'
try {
    $r = Invoke-WebRequest $url -TimeoutSec 20 -UseBasicParsing
    Write-Host ("   登录页 HTTP " + $r.StatusCode + "，地址：" + $url) -ForegroundColor Green
} catch {
    Write-Host ('   登录页访问异常：' + $_.Exception.Message) -ForegroundColor Yellow
    Write-Host '   可稍等 10 秒后手动刷新页面试试。'
}

Write-Host ''
Write-Host '部署完成。管理员账号 admin / admin123' -ForegroundColor Green
