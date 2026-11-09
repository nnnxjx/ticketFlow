# TicketFlow 开发约定

适用于本仓库的后续代码修改。需求和交易规则以 `plan.md`、`docs/交易规则.md`、`docs/数据库表设计.md` 为准；若文档冲突，先说明并更新文档，再改实现。

## 后端结构

- 后端采用**分层在前、业务在后**的包结构：`controller/auth`、`mapper/auth`、`dto/auth`、`vo/auth`、`entity/auth`；`organizer`、`event`、`order`、`seat` 等业务同理。Service 接口直接放 `service`，实现放 `service/impl`。`config` 和跨业务通用类保留在顶层。避免为了空目录创建无用文件。
- `service` 根目录放业务接口，命名为 `IXxxService` 并继承 MyBatis-Plus 的 `IService<Entity>`；`service/impl` 放对应的 `XxxServiceImpl`，继承 `ServiceImpl<Mapper, Entity>` 并实现该接口。Controller 依赖接口；实现类标注 `@Service`，事务注解放在实现方法上。常规 CRUD 可直接调用继承来的 `save/getById/getOne/list` 等方法。
- `dto` 只描述客户端传入的数据，如注册、登录、创建演出请求；在 DTO 字段上放参数校验注解。不要信任 DTO 中的价格、用户 ID、订单归属等客户端数据。
- `vo` 只描述返回给客户端的数据，如用户信息、订单详情；不要暴露密码哈希、票券原始令牌等内部字段。
- `entity` 对应数据库表，配合 MyBatis-Plus 映射。Entity 不直接作为 API 请求或响应类型。
- 业务模块的 DTO、VO、Entity 使用独立的顶层类文件。不要频繁使用类内部类；只有与外部类强绑定、很小且不会复用的实现细节才考虑内部类。
- 项目偏好普通 Java 类，不使用 `record`。DTO、VO、Entity 的样板代码优先用 Lombok：请求 DTO 需要无参构造器和 getter/setter，响应 VO 需要 getter 与构造器，Entity 需要 getter/setter。不要对含密码、令牌等敏感字段的类使用会生成 `toString()` 的 `@Data`。
- 数据访问统一使用 MyBatis-Plus Mapper。简单 CRUD 使用 `BaseMapper`；复杂条件更新和并发控制使用明确的 SQL，并检查影响行数。不要在同一模块混用 `JdbcTemplate`。
- Spring 依赖使用字段上的 `@Autowired` 注入，不使用构造函数注入。配置值使用字段上的 `@Value`。
- 新写或修改的代码要有必要的解释性注释，重点说明业务规则、状态转换、并发约束、鉴权边界及不直观的实现原因；不要求逐行注释，避免把代码原样翻译成注释。公共接口和复杂方法优先使用简短 Javadoc。
- 拦截器集中放在 `interceptor` 包。业务 HTTP API 的登录态检查通过 Spring MVC `AuthInterceptor` 完成，公开路径由 `WebMvcConfig` 明确排除；请求 ID 由 `RequestIdInterceptor` 处理。JWT 签名、签发方和账号状态均须校验；存在过期时间时也要校验。仅本地 dev 配置允许暂时签发不过期令牌。对象级权限仍由业务 Service 校验。首版不编写自定义 Filter，也不配置 Spring Security 过滤器链。新增不经过 Spring MVC 的入口（例如 WebSocket 握手）时需单独实现鉴权，不能假设 MVC 拦截器会覆盖。
- 定时任务统一放在顶层 `job` 包；任务类只负责触发、批量参数和日志，具体业务状态转换交给 Service。
- MQ 消费者统一放在顶层 `consumer` 包；监听类只负责接收、解析消息并调用 Service，业务处理留在 Service。
- 鉴权请求头使用 `Authorization: <原始 JWT>`，不加 Bearer 前缀。当前用户 ID 通过 `UserContext` 的 `ThreadLocal` 传递，请求完成或异常时必须调用 `remove()` 清理；异步任务不得直接继承该上下文，应显式传递业务身份。

## 业务边界

- 一个场次只采用一种销售模式。`GENERAL` 使用票档库存，`ASSIGNED` 直接使用座位价格与状态，不创建票档。
- 主办方只有负责人身份，每个用户最多负责一个主办方（数据库对 `owner_user_id` 设唯一键）。管理端从当前用户 ID 推导主办方，不要求客户端传主办方 ID；以演出、场馆等资源归属做对象级权限校验，不引入员工角色体系。
- 所有场次必须关联场馆。仅草稿且没有订单的场次允许删除；删除场次时同一事务删除其票档和场次座位。`venue_seat.area` 用于区域显示与分组，同一场馆不同区域可以有相同排号和座号，因此物理座位唯一键保留区域字段。
- 金额使用 `BigDecimal`/`DECIMAL`，时间按 UTC 存储。订单状态、库存、座位和票券变更遵循 `docs/交易规则.md`，并在事务中以数据库条件更新保证并发正确性。
- 首版只做模拟支付与整单退款，不对外宣称真实资金交易。

## 修改与验证

- 新增或修改 API 时更新 OpenAPI 注解和 `docs/接口文档.md`；确保 Swagger UI 和 `/v3/api-docs` 能展示接口。
- 数据库结构变更使用新的迁移文件，不能默默修改已经执行过的迁移。现有 `docs/schema.sql` 是设计稿，先核对本地实际结构。
- 对关键状态转换和越权路径写有意义的测试。能运行时执行 Maven 测试；依赖或本地服务不可用时，如实记录未验证的范围。
- 本地 MySQL、Redis、RabbitMQ 已由用户安装，不新增 Docker 作为运行前提。密码保存在被忽略的本地配置或环境变量中，不提交凭据。
