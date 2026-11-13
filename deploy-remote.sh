#!/usr/bin/env bash
# 服务器端部署脚本：备份旧版本 -> 替换 WAR -> 重启 Tomcat
# 由 redeploy-server.ps1 上传到 /tmp 后执行
set -e

APP=campus-marathon
WEBAPPS=/var/lib/tomcat9/webapps
TS=$(date +%Y%m%d_%H%M%S)
BK=/var/backups/${APP}/${TS}

echo "== 0. 备份目录 $BK"
sudo mkdir -p "$BK"

echo "== 1. 停止 Tomcat"
sudo systemctl stop tomcat9 || true

echo "== 2. 备份现有部署"
if [ -d "$WEBAPPS/$APP" ]; then
    sudo cp -a "$WEBAPPS/$APP" "$BK/webapp"
    sudo cp "$WEBAPPS/$APP/WEB-INF/classes/db.properties" "$BK/db.properties" 2>/dev/null || true
    sudo cp "$WEBAPPS/$APP/WEB-INF/classes/ai.properties" "$BK/ai.properties" 2>/dev/null || true
    echo "   已备份到 $BK"
else
    echo "   未发现 $WEBAPPS/$APP，跳过备份"
fi

echo "== 3. 替换 WAR"
sudo rm -rf "$WEBAPPS/$APP"
sudo rm -f "$WEBAPPS/$APP.war"
sudo cp /tmp/$APP.war "$WEBAPPS/$APP.war"
sudo chown tomcat:tomcat "$WEBAPPS/$APP.war" 2>/dev/null || true

echo "== 4. 启动 Tomcat"
sudo systemctl start tomcat9

echo "== 5. 等待应用解压"
for i in $(seq 1 40); do
    if [ -f "$WEBAPPS/$APP/WEB-INF/web.xml" ]; then break; fi
    sleep 1
done

echo "== 6. 恢复服务器原有配置（数据库/密钥与本地不同，不能覆盖）"
if [ -f "$BK/db.properties" ]; then
    sudo cp "$BK/db.properties" "$WEBAPPS/$APP/WEB-INF/classes/db.properties"
    echo "   已恢复 db.properties"
fi
if [ -f "$BK/ai.properties" ]; then
    sudo cp "$BK/ai.properties" "$WEBAPPS/$APP/WEB-INF/classes/ai.properties"
    echo "   已恢复 ai.properties"
fi

echo "== 7. 结果"
ls -l "$WEBAPPS" | sed 's/^/   /'
echo "   db.properties ->"
sudo grep -E '^jdbc.url' "$WEBAPPS/$APP/WEB-INF/classes/db.properties" | sed 's/^/   /' || true
echo "   tomcat9: $(sudo systemctl is-active tomcat9)"
echo "   BACKUP=$BK"
