# 部署「AI 客服」到云服务器（101.42.186.195）
# 用法：在 PowerShell 中执行  powershell -ExecutionPolicy Bypass -File deploy-support.ps1
# 执行过程中会提示输入服务器密码（请手动输入，脚本不保存任何密码）

$server = 'ubuntu@101.42.186.195'
$src = 'c:\Users\30370\Desktop\try.1\temp_20261029_01\target\campus-marathon'
$webapp = '/var/lib/tomcat9/webapps/campus-marathon'

Write-Host '=== 1/3 上传文件 ===' -ForegroundColor Cyan
scp "$src\assets\css\support.css" "$src\assets\js\support.js" "$src\WEB-INF\views\header.jsp" "${server}:/tmp/"
if ($LASTEXITCODE -ne 0) { Write-Host '上传失败，已中止。' -ForegroundColor Red; exit 1 }

Write-Host '=== 2/3 覆盖文件并重启 Tomcat ===' -ForegroundColor Cyan
ssh $server "sudo mkdir -p $webapp/assets/js; sudo cp /tmp/support.css $webapp/assets/css/support.css; sudo cp /tmp/support.js $webapp/assets/js/support.js; sudo cp /tmp/header.jsp $webapp/WEB-INF/views/header.jsp; sudo chown tomcat:tomcat $webapp/assets/css/support.css $webapp/assets/js/support.js $webapp/WEB-INF/views/header.jsp; sudo systemctl restart tomcat9; sleep 6; sudo systemctl is-active tomcat9"
if ($LASTEXITCODE -ne 0) { Write-Host '部署命令执行失败。' -ForegroundColor Red; exit 1 }

Write-Host '=== 3/3 验证 ===' -ForegroundColor Cyan
try {
    $js = Invoke-WebRequest -Uri 'http://101.42.186.195/campus-marathon/assets/js/support.js?v=2' -TimeoutSec 20
    $css = Invoke-WebRequest -Uri 'http://101.42.186.195/campus-marathon/assets/css/support.css?v=2' -TimeoutSec 20
    $html = Invoke-WebRequest -Uri 'http://101.42.186.195/campus-marathon/login' -TimeoutSec 20
    Write-Host ("support.js  : " + $js.StatusCode + "，含「转人工服务」: " + ($js.Content -like '*转人工服务*'))
    Write-Host ("support.css : " + $css.StatusCode)
    Write-Host ("页面引入    : " + ($html.Content -like '*support.js?v=2*'))
} catch {
    Write-Host ('验证请求异常：' + $_.Exception.Message) -ForegroundColor Yellow
}
Write-Host '完成。' -ForegroundColor Green
