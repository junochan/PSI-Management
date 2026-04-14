# 智链进销存管理系统（Smart IMS）

面向中小型商贸企业的进销存一体化管理平台，提供采购、销售、库存、客户与供应商、售后、权限与操作日志等能力，配套 Vue 管理端与 Spring Boot JSON API。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite 5、Element Plus、Pinia、Vue Router、ECharts、Axios |
| 后端 | Java 17、Spring Boot 3.2、Spring Security、JWT、MyBatis-Plus、Knife4j |
| 数据库 | MySQL 8.x |
| 可选能力 | 阿里云 DashScope 多模态向量（以图搜商品/库存图）、本地向量 JSON 缓存 |

## 仓库结构

```text
PSI-Management/
├── docs/                          # PRD、数据库脚本与迁移
│   ├── 进销存管理系统PRD.md
│   └── database/                  # 建库、初始化、增量迁移 SQL
├── psi-manage-system-backend/     # Spring Boot 后端（context-path: /api）
├── psi-manage-system-front/       # Vue 3 管理前端
└── skills/                        # Agent 技能与 psims CLI（可选）
```

## 环境要求

- **JDK** 17+
- **Maven** 3.6+
- **Node.js** 18+（前端与 `skills/.../psi-cli`）
- **MySQL** 8.x，数据库名建议 `smart_ims`（与配置一致）

## 数据库初始化

1. 创建数据库并导入主结构（按你环境选择其一或组合执行）：

   - 全量参考：`docs/database/smart_ims.sql`
   - 导航菜单等初始化：`docs/database/navigation_init.sql`
   - 其他迁移文件见 `docs/database/migration_*.sql`（按时间顺序按需执行）

2. 修改后端数据源：编辑 `psi-manage-system-backend/src/main/resources/application.yml` 中的 `spring.datasource.url`、`username`、`password`，或使用 Spring Profile / 环境变量覆盖（生产环境勿提交真实密码）。

## 后端运行

```bash
cd psi-manage-system-backend
mvn spring-boot:run
```

默认：

- 服务端口：**8080**
- 上下文路径：**/api**（例如接口根为 `http://localhost:8080/api/...`）
- API 文档（Knife4j）：`http://localhost:8080/api/doc.html`（若已启用 knife4j）

日志文件默认写入后端目录下 `logs/smart-ims.log`（见 `logback-spring.xml` / `application.yml`）。

## 前端运行

```bash
cd psi-manage-system-front
npm install
npm run dev
```

按 Vite 控制台提示访问本地开发地址（通常为 `http://localhost:5173`）。前端通过代理或配置的 **API 基址** 访问后端；请保证浏览器请求指向 `http://localhost:8080/api`（或与你的部署一致）。

生产构建：

```bash
npm run build
```

产物在 `psi-manage-system-front/dist/`，可部署到任意静态资源服务器，并配置反向代理到后端 `/api`。

## 环境变量说明（节选）

以下常用于生产或本地覆盖，**勿将密钥写入 Git**：

| 变量 | 说明 |
| --- | --- |
| `DASHSCOPE_API_KEY` | 启用「以图搜图」等 DashScope 能力时必填 |
| `APP_ATTACHMENT_DIR` | 附件根目录，默认 `data` |
| `APP_EMBEDDING_STORE_DIR` | 向量缓存目录，默认 `data/image-embeddings` |
| `APP_IMAGE_SEARCH_SIMILARITY_THRESHOLD` | 相似度阈值，默认 `0.7` |

JWT、数据库账号等建议在 `application-local.yml`（需自行创建并加入 `.gitignore`）或部署平台密钥管理中配置。

## 命令行工具 psims（可选）

仓库内 `skills/psi-smart-ims-overview/psi-cli` 提供 `psims` CLI，可在终端使用 JWT 调用与 Web 一致的 API。安装与用法见该目录下说明及 `skills/psi-smart-ims-overview/SKILL.md`。

## 文档

- 产品需求与功能范围：`docs/进销存管理系统PRD.md`
- 数据库脚本：`docs/database/`

## 开源协议

本项目采用 [Apache License 2.0](LICENSE)。
