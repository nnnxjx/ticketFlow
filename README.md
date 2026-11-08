# TicketFlow

高并发票务预约与交易平台。当前已建立 Spring Boot 后端、React 用户端与 React 主办方端的基础工程；业务接口按 [开发计划](./plan.md)逐步实现。

## 本地环境

- JDK 17 或 21（本项目编译目标为 Java 17）
- Maven 3.9+
- IDE 启用 Lombok 注解处理（若 IDE 无法识别生成的 getter/setter，可重新导入 Maven 并检查 annotation processing 设置）
- Node.js 20.19+、npm
- 本机 MySQL 8、Redis、RabbitMQ；默认端口分别为 `3306`、`6379`、`5672`
- 不需要 Docker

数据库结构见 [schema.sql](./docs/schema.sql)。如已执行旧版脚本，先看[数据库表设计文档](./docs/数据库表设计.md)中的迁移说明，不要重复执行完整建表脚本。当前 `application.yml` 暂时关闭 Flyway，因为本地数据库可能已经手动建表；完成基线核对后再启用 Flyway。

## 配置本地服务

本机开发可启用 `dev` profile，读取 `backend/src/main/resources/application-dev.yml` 中的本地连接配置。该文件已加入 `.gitignore`，不要提交或分享；数据库密码变化时只改本机文件。

其他环境复制 [.env.example](./.env.example) 的变量设置。本项目不会自动读取根目录 `.env`；可以在 PowerShell 会话中设置环境变量，例如：

```powershell
$env:DB_HOST = '127.0.0.1'
$env:DB_PORT = '3306'
$env:DB_NAME = 'ticketflow'
$env:DB_USER = 'root'
$env:DB_PASSWORD = '<你的本地 MySQL 密码>'
$env:REDIS_HOST = '127.0.0.1'
$env:REDIS_PORT = '6379'
$env:RABBITMQ_HOST = '127.0.0.1'
$env:RABBITMQ_PORT = '5672'
$env:RABBITMQ_USER = 'guest'
$env:RABBITMQ_PASSWORD = 'guest'
```

先启动本地 MySQL、Redis、RabbitMQ。可以用 `Test-NetConnection 127.0.0.1 -Port 3306`（其他端口同理）检查是否监听。RabbitMQ 默认 `guest` 用户通常只允许本机连接，本项目按本机访问配置。

## 启动后端

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = 'dev'
mvn spring-boot:run
```

在 IDE 的运行配置中，也可以将 active profiles 设为 `dev`。不启用 `dev` 时，数据库密码仍由 `DB_PASSWORD` 环境变量提供。

验证 `http://127.0.0.1:8080/actuator/health` 和 `http://127.0.0.1:8080/api/v1/system/ping`。后者返回统一响应和 `X-Request-Id`。受保护业务接口使用 `Authorization: <原始 JWT>` 请求头，不加 Bearer 前缀。具体接口见[接口文档](./docs/接口文档.md)。

接口文档位于 `http://127.0.0.1:8080/swagger-ui.html`，机器可读文档位于 `http://127.0.0.1:8080/v3/api-docs`。已实现接口列表见[接口文档](./docs/接口文档.md)。

## 启动两个前端

分别在两个终端运行：

```powershell
cd frontend-user
npm install
npm run dev
```

```powershell
cd frontend-organizer
npm install
npm run dev
```

用户端为 `http://127.0.0.1:5173`，主办方端为 `http://127.0.0.1:5174`。开发服务器将 `/api` 代理到本机 `8080` 端口。执行 `npm run build` 检查前端生产构建。

## 常见启动问题

- `DB_PASSWORD` 未设置且未启用 `dev`：后端无法解析数据源密码。设置环境变量或启用本机 `dev` profile。
- MySQL、Redis 或 RabbitMQ 未启动：检查服务进程和默认端口。Actuator 健康状态会受依赖服务影响。
- `mvn` 不存在：安装 Maven 并加入 `PATH`；`java -version` 与 `mvn -version` 应显示期望的 JDK。
- Flyway 报已有表无历史记录：不要自动清库；先按步骤 0 核对基线，再启用迁移。

## 当前进度

- [x] 步骤 1 的工程骨架和本地服务配置文件
- [x] 后端已由用户通过 Maven 导入并启动
- [ ] 两个前端的依赖安装与构建验证（当前工具环境中的 npm 依赖下载未完成）
- [ ] 步骤 0 的实际数据库基线核对
- [x] 步骤 3 的演出、场次、票档、场馆和座位配置后端接口代码
- [x] 步骤 4 的无座同步下单、幂等、限购和本人订单查询接口代码
- [x] 步骤 5 的取消、超时关单及模拟支付接口代码
- [x] 步骤 6 的异步出票、持有人电子票查询与主办方检票接口代码
- [x] 步骤 7 的整单模拟退款、票券作废与条件转售接口代码
- [ ] 步骤 7 的本地数据库退款和检票竞态联调
- [ ] 步骤 6 的本地数据库检票竞态联调
- [ ] 步骤 5 的本地数据库竞态联调
- [ ] 步骤 4 的本地 MySQL 并发联调
- [ ] 步骤 3 的前端页面与本地数据库联调
- [ ] 步骤 4 起的交易业务接口与页面
- [x] 步骤 2 的注册、登录、JWT 鉴权、主办方负责人归属接口与 Swagger 文档代码

本地依赖和构建的实际验证结果以执行环境为准，见开发过程记录或后续提交。
