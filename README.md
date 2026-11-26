# AI Gateway MCP

一个基于 **MCP（Model Context Protocol）** 的 AI 能力网关 / 协议代理服务。它统一纳管上游 MCP 服务（Server、Tool、Prompt、Resource），对外提供标准 MCP 协议端点，并内置 API Key 鉴权与审计日志，帮助团队集中、安全地接入和管理各类 AI 能力。

## 功能特性

- **MCP 服务纳管**：集中登记上游 MCP Server 及其暴露的 Tools、Prompts、Resources
- **标准协议端点**：对外提供 MCP 协议接口（initialize / tools/list / tools/call / resources/* / prompts/*）
- **API Key 鉴权**：基于 Bearer Token 的 API Key 校验，可按 Key 授权访问指定 Server
- **审计日志**：记录每次协议调用（调用方、目标服务、耗时、结果、错误码）
- **管理后台**：Vue 3 + Element Plus 提供的可视化管理界面，支持服务/工具/Prompt/资源的增删改查与导入导出
- **数据库迁移**：Flyway 管理表结构演进，MySQL / H2 双环境支持

## 技术栈

### 后端

| 类别 | 技术 | 说明 |
|------|------|------|
| 框架 | Spring Boot 3.5.x | 信创适配（恒生 JRES Cloud 3.1.8.7） |
| ORM | MyBatis-Flex 1.8.x | 轻量、现代 API |
| 数据库 | MySQL 8.x / H2 | 生产用 MySQL，测试用 H2 内存库 |
| 迁移 | Flyway | 数据库版本管理 |
| 构建 | Maven | JDK 17 |

### 前端

| 类别 | 技术 |
|------|------|
| 框架 | Vue 3.4.x |
| UI 组件库 | Element Plus |
| 状态管理 | Pinia |
| 构建工具 | Vite 5.x |

## 目录结构

```
ai-gateway-mcp/
├── src/                          # 后端源码（Spring Boot）
│   ├── main/java/org/veronicadavichi/
│   │   ├── config/               # 配置与 MCP 协议处理
│   │   ├── controller/           # REST 接口（admin 管理 API）
│   │   ├── service/              # 业务逻辑
│   │   ├── mapper/               # MyBatis-Flex 数据访问
│   │   └── entity/               # 实体
│   └── main/resources/
│       ├── application*.yml      # 多环境配置
│       └── db/migration/         # Flyway 迁移脚本
├── front_end/                    # 前端源码（Vue 3 + Vite）
├── docker/                       # Docker 相关（Maven settings、init.sql）
├── Dockerfile / Dockerfile.local # Docker 构建文件
├── docker-compose.yml            # Docker Compose 编排
└── pom.xml
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 18+（前端）
- MySQL 8.x（集成环境，或使用 H2 快速体验）

### 1. 启动后端

```bash
# 方式一：使用 H2 内存库快速体验（无需外部数据库）
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run

# 方式二：连接外部 MySQL（integration 环境）
# 先准备好数据库，再通过环境变量注入连接信息
MYSQL_HOST=localhost \
MYSQL_PORT=3306 \
MYSQL_DATABASE=ai_gateway_mcp \
MYSQL_USERNAME=root \
MYSQL_PASSWORD=your_password \
SPRING_PROFILES_ACTIVE=integration \
mvn spring-boot:run
```

后端默认监听 **8080** 端口：

- 管理 API：`http://localhost:8080/api/v1/admin/*`
- 健康检查：`http://localhost:8080/actuator/health`

### 2. 启动前端

```bash
cd front_end
npm install
npm run dev
```

前端开发服务器默认监听 **5173** 端口：`http://localhost:5173`

前端通过 Vite 代理将 `/api/v1` 请求转发到后端（默认 `http://localhost:8080`），如需修改后端地址，可在 `front_end/.env` 中配置：

```
VITE_API_PROXY_TARGET=http://localhost:8080
```

## 构建与部署

### 后端打包

```bash
mvn clean package -DskipTests
# 产物：target/ai-gateway-mcp-1.0-SNAPSHOT.jar
```

### 前端构建

```bash
cd front_end
npm run build
# 产物：front_end/dist/（静态资源，可部署到任意静态服务器或后端 static 目录）
```

### Docker 部署

```bash
# 1. 构建后端 JAR + 前端产物
mvn clean package -DskipTests
cd front_end && npm install && npm run build && cd ..

# 2. 构建镜像
docker build -f Dockerfile.local -t ai-gateway-mcp:latest .

# 3. 使用 docker-compose 启动
cp .env.example .env    # 按需修改数据库连接
docker-compose up -d
```

详见 [`DOCKER.md`](./DOCKER.md)。

## 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `SPRING_PROFILES_ACTIVE` | 激活的 Spring 配置（dev / integration / docker） | `dev` |
| `MYSQL_HOST` | MySQL 主机地址 | `localhost` |
| `MYSQL_PORT` | MySQL 端口 | `3306` |
| `MYSQL_DATABASE` | 数据库名 | `ai_gateway_mcp` |
| `MYSQL_USERNAME` | 数据库用户名 | `root` |
| `MYSQL_PASSWORD` | 数据库密码 | 空 |


## 许可证

[待补充]
