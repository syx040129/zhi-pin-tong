# CareerNova · 智聘 AI

> 文档更新：2026-10-08。文件名与引用按当前目录同步；验收记录标注的是历史验证，本轮未重新执行业务测试。 总入口：[项目总文档](docs/项目总文档.md)。

基于 Spring Boot 3 与 Vue 3 的智能招聘毕业设计项目，包含求职者、企业、管理员三角色门户，以及简历分析、岗位匹配、推荐、优化、模拟面试和招聘助手。支持 Mock 规则模式与可配置的真实模型调用。

完整架构、配置、API、数据库、功能状态和验收记录见 [项目总文档](docs/项目总文档.md)。

> 当前可验证结论：本地 Mock 演示可有条件交付。默认完全无密钥启动失败，需使用下方显式 Mock 命令；Swagger 在线文档生成失败。真实模型、Docker 和生产部署未验收，详见总文档的已知问题。

## 环境要求

| 依赖 | 要求 |
| --- | --- |
| Java / Maven | Java 17、Maven 3.9+ |
| Node.js | 18+；仓库 Docker 构建使用 Node 22，本次验证使用 Node 24.20.0，未实测 Node 18 |
| MySQL | 8.0.16+，CHECK 约束要求该版本或以上 |
| Redis | 可选，仓库容器使用 Redis 7，缺失时降级内存缓存 |
| Docker Compose | 仅容器部署需要；本次环境无 Docker，未验证 |

## 快速开始

### 1. 准备数据库

已有数据库不要重新执行 schema.sql；增量升级见 [数据库优化与扩展数据](docs/数据库优化与扩展数据.md)。

首次创建空环境，在项目根目录执行（PowerShell 和其他终端均可）：

```powershell
mysql -u root -p --default-character-set=utf8mb4 -e "source sql/schema.sql"
mysql -u root -p --default-character-set=utf8mb4 -e "source sql/data.sql"
mysql -u root -p --default-character-set=utf8mb4 -e "source sql/demo-data.sql"
```

`schema.sql` 删除并重建业务表。默认开发连接为 localhost:3306/smart_recruitment、root/123456；数据库配置在 backend/src/main/resources/application-dev.yml，使用自己的连接参数时需同步配置。

### 2. 启动 Mock 后端

```powershell
cd backend
mvn spring-boot:run '-Dspring-boot.run.arguments=--ai.enabled=false --ai.mock=true --spring.ai.openai.api-key=not-configured'
```

`not-configured` 仅满足 Spring AI 的非空连接配置校验，不是真实密钥。命令显式关闭真实模型；不改写 .env.local，业务使用规则与本地向量。后端默认 8080，健康入口为 [health](http://localhost:8080/api/common/health)。

### 3. 启动前端

另开终端，从项目根目录执行：

```powershell
cd frontend
npm ci
npm run dev
```

访问 [本地页面](http://localhost:5173)。Vite 将 /api 代理到 8080。常用基础演示账号：user（求职者）、company（企业）、admin（管理员），sql/data.sql 中密码均为 123456；本地已改密的账号以当前数据库为准。

## 构建与检查

```powershell
# backend 目录
mvn package -q
# frontend 目录
npm run type-check
npm run build
npm run preview
```

2026-10-08 最终收尾验证中，后端 21 项测试、前端类型与构建、61 项页面检查、15 项交互检查及独立临时数据库验证通过；构建仍有资源包体积告警。构建通过不等于全部业务或生产验收通过。

Docker 命令、环境变量、测试前提和已知限制统一见 [运行与部署](docs/项目总文档.md#安装运行构建与部署)。专题文档索引也在总文档中维护。

## 文档导航

| 文档 | 内容 |
| --- | --- |
| [项目总文档](docs/项目总文档.md) | 架构、配置、部署、功能状态与验收限制 |
| [系统功能说明](docs/系统功能说明.md) | 三角色功能、页面路由与业务流程 |
| [接口说明](docs/接口说明.md) | 接口方法、路径、参数和响应 |
| [人工智能功能说明](docs/人工智能功能说明.md) | 模式、算法、持久化与降级 |
| [数据库设计](docs/数据库设计.md) | 表字段、关系与业务约束 |
| [数据库优化与扩展数据](docs/数据库优化与扩展数据.md) | SQL 增量升级和演示数据 |
| [前端开发指南](frontend/前端开发指南.md) | 启动、组件生成、状态与浏览器检查 |
| [界面设计规范](docs/界面设计规范.md) | 配色、字体、布局和交互约定 |
| [界面改造与验证](docs/界面改造与验证.md) | 界面改造及历史验证记录 |
| [升级说明与验收](docs/升级说明与验收.md) | 2026-10-08 业务升级与专项验收记录 |

本轮更新仅涉及文档内容与中文文件名，未重新运行业务验收。backups 和 design-system 已不在项目根目录；界面规范现归入 docs，历史备份不再作为可用恢复文件。
