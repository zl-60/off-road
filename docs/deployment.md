# 运行配置

## 本机演示

`scripts/start.ps1` 启动六个独立 JVM，默认仅监听 127.0.0.1；服务 URL、端口、数据库 URL、订单有效期和扫描周期均可通过环境变量覆盖。

| 环境变量 | 默认值 |
| --- | --- |
| USER/ITEM/CART/TRADE/PAY/GATEWAY_PORT | 8081/8082/8083/8084/8085/8080 |
| USER/ITEM/CART/TRADE/PAY_URL | 对应的 http://127.0.0.1:端口 |
| USER/ITEM/CART/TRADE/PAY_DB_URL | 各模块自己的 H2 文件数据库 |
| DB_USER / DB_PASSWORD | sa / 空，供默认 H2 使用 |
| ORDER_TTL_SECONDS | 900 |
| ORDER_SWEEP_MS / OUTBOX_INTERVAL_MS | 3000 / 3000 |
| OUTDOOR_TOKEN_SECRET | 本机演示令牌密钥 |
| OUTDOOR_INTERNAL_KEY | 本机演示内部请求密钥 |

网关和所有模块必须使用同一令牌密钥；内部请求密钥也必须一致。所有模块在部署环境下应提供独立配置，演示账户和默认密钥仅用于本机示例。

当前机器的 Maven 缓存配置保存在忽略目录 `.runtime/settings.xml`；脚本存在此文件时使用它，换机器没有此文件则使用机器自己的 Maven 设置。便携 JDK 存在时仅对当前脚本进程设置 JAVA_HOME。

本机 H2 数据库设置 `WRITE_DELAY=0`，使已提交事务立即刷盘。强制结束进程后恢复的验证与正常关闭重启分别覆盖数据库持久化和业务重试。[H2 刷盘配置](https://h2database.com/html/commands.html#set_write_delay)

## MySQL 模式

不复用原数据库。在独立 MySQL 实例中创建 `shanye_user/item/cart/trade/pay` 五个库，各模块有自己的表，不跨库查询。

```powershell
# 示例：为用户模块配置自己的 MySQL 库，其余模块分别配置对应变量。
$env:USER_DB_URL='jdbc:mysql://127.0.0.1:3306/shanye_user?useSSL=false&serverTimezone=Asia/Shanghai'
$env:DB_USER='shanye'
$env:DB_PASSWORD='你的数据库密码'
$env:SCHEMA_LOCATIONS='classpath:outdoor-schema-mysql.sql'
```

五个模块均提供 `outdoor-schema-mysql.sql`，使用 CREATE TABLE IF NOT EXISTS；首次启动生成演示账户和商品。部署时准备数据库账号授权，参考 `deploy/mysql-init.sql`。

## Nacos / RabbitMQ 容器示例

Maven 的 `middleware` 配置加入 Nacos 注册发现依赖；服务仍用显式 URL 通信。Spring 的 `middleware` 配置将支付通知切换为 RabbitMQ。库存和订单恢复继续依赖持久化操作记录，不需要 RabbitMQ 延迟插件。

```powershell
# 先停止本机演示进程，避免 8080 端口占用。
.\scripts\stop.ps1
mvn -Pmiddleware -DskipTests package
Copy-Item -LiteralPath .\deploy\.env.example -Destination .\deploy\.env
# 按环境修改 deploy/.env 的密码和两个密钥。
docker compose --env-file deploy/.env -f deploy/compose.yml up --build -d
```

镜像使用项目构建产物。MySQL 和 RabbitMQ 数据使用持久卷；用户端/后台仍从网关 8080 访问。Nacos 8848 和 RabbitMQ 控制台 15672 映射在本机地址。环境变量模板中的值默认留空，Docker Compose 会在缺少配置时直接报错。自行生成并填写数据库密码、中间件密码及两个密钥，真实 `.env` 已被 Git 忽略。

通知：支付状态和 Outbox 在同一个数据库事务提交；RabbitMQ 适配器等待发布确认，未确认或无法路由则重试。交易模块使用持久队列、有限重试与死信队列 `shanye.payment-events.dead`。通知消费调用同一幂等完成逻辑；即使通知进入死信，交易模块也会继续从处理中订单恢复，需要结合订单错误和死信队列排查长期失败。

公开版本不包含旧教学环境的配置、证书或共享配置。已运行验证的范围见 `validation.md`，未验证的环境不视为已部署。

## 原 Nginx 前端目录

`scripts/sync-frontend.ps1` 可将主源码同步到自己的 Nginx 目录；必须显式提供目录参数：

```powershell
.\scripts\sync-frontend.ps1 -NginxRoot 'C:\nginx'
```

- `html/hmall-portal`：用户端。
- `html/hmall-admin`：管理端。
- 18080/18081 继续通过 `/api` 转发到网关 8080。
- 18082 指向同一新版管理端。

运行中的 Nginx 修改配置后，需要在它自己的目录执行 `nginx.exe -s reload`；当前项目不会自动重启其他 Nginx 进程。同步脚本保留原有资源。运行前应备份自己的 Nginx 配置和前端目录。
