# 校园马拉松报名管理系统

面向校内马拉松赛事的在线报名与后台管理系统：报名、审核、支付、数据看板、AI 知识库问答全流程可实际运行。

- **在线演示**：http://101.42.186.195/campus-marathon/login
- **体验账号**：`admin` / `admin123`（管理员）

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 在线报名 | 参赛者选择项目、填写信息，前端即时校验 + 服务端二次复核 |
| 审核流转 | 待审核 → 报名成功 / 驳回，24 小时未支付自动取消 |
| 支付闭环 | 支付宝沙箱，异步回调 + RSA 验签 + 幂等落库，取消报名自动退款 |
| 实时看板 | 聚合 SQL 出 JSON，前端 30 秒轮询渲染 6 类图表与热力矩阵 |
| AI 知识库 | 文档上传切片，BM25 检索 + 大模型 SSE 流式问答 |

## 技术栈

Java 11 · Servlet 4.0 · JSP / JSTL · 原生 JDBC · MySQL 8 · Maven · Tomcat 9 · 支付宝沙箱 SDK · JUnit 5 · H2 · Chart.js

## 本地启动

### 1. 初始化数据库

```bash
mysql -u root -p < sql/marathon.sql
mysql -u root -p campus_marathon < data/marathon_import.sql   # 可选：导入 507 条演示数据
```

### 2. 配置连接信息

复制模板并填入自己的账号密码：

```bash
cp src/main/resources/db.properties.example src/main/resources/db.properties
cp src/main/resources/ai.properties.example src/main/resources/ai.properties         # 可选，AI 问答
cp src/main/resources/alipay.properties.example src/main/resources/alipay.properties # 可选，支付
cp src/main/resources/auth.properties.example src/main/resources/auth.properties     # 可选，管理员账号
```

未配置 AI / 支付宝时会自动降级：问答退化为检索演示，支付模块隐藏。

### 3. 构建与部署

```bash
mvn clean package
# 将 target/campus-marathon.war 复制到 Tomcat 9 的 webapps 目录后启动
```

Windows 下可用 `start-all.ps1` 一键拉起内置 MySQL 与 Tomcat：

```powershell
.\start-all.ps1
```

访问 http://localhost:8081/campus-marathon/login 。

## 目录结构

```
src/main/java/com/campus/marathon/
├── model/       实体与统计模型
├── dao/         数据访问接口与实现
├── service/     业务逻辑（容量控制、支付、检索）
├── web/         Servlet 控制器与过滤器
└── util/        数据库连接、密码、配置加载
src/main/webapp/ JSP 视图与静态资源
sql/             建表脚本
data/            演示数据
scripts/         数据生成与导入脚本
```

## 测试

```bash
mvn test
```

覆盖名额并发控制、支付回调验签、BM25 检索、切片、状态流转等核心逻辑，使用 H2 内存库，不依赖本地 MySQL。
