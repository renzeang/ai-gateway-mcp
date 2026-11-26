# AI Gateway MCP - Docker 部署指南

## 构建说明

### 前置条件

由于项目使用了内部 Maven 仓库（jrescloud 依赖），**推荐使用本地构建方式**。

### 方式：使用本地构建的 JAR 包（推荐）

```bash
# 1. 构建后端 JAR 包
mvn clean package -DskipTests

# 2. 构建前端（如果尚未构建）
cd front_end && npm install && npm run build && cd ..

# 3. 构建 Docker 镜像
docker build -f Dockerfile.local -t ai-gateway-mcp:latest .
```

构建成功后会看到：
```
#10 naming to docker.io/library/ai-gateway-mcp:latest done
```

## 快速开始

### 方式一：使用 Docker Compose（推荐）

```bash
# 1. 确保 .env 文件已配置（已预配置）
cat .env

# 2. 启动应用
docker-compose up -d

# 3. 查看日志
docker-compose logs -f

# 4. 停止服务
docker-compose down
```

启动后访问：
- 前端页面：http://localhost:8080
- 后端 API：http://localhost:8080/api/v1/admin

### 方式二：使用 docker run

```bash
docker run -d \
  --name ai-gateway-mcp \
  -p 8080:8080 \
  --env-file .env \
  ai-gateway-mcp:latest

# 查看日志
docker logs -f ai-gateway-mcp

# 停止并删除
docker stop ai-gateway-mcp && docker rm ai-gateway-mcp
```

### 测试验证

```bash
# 健康检查
curl http://localhost:8080/api/v1/admin/servers

# 预期输出：
# {"code":"SUCCESS","data":{"records":[...],"total":1,"page":1,"size":10}}
```

### 方式二：单独构建和运行

```bash
# 构建镜像
docker build -t ai-gateway-mcp:latest .

# 运行容器（使用外部 MySQL）
docker run -d \
  --name ai-gateway-mcp \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=integration \
  -e MYSQL_HOST=localhost \
  -e MYSQL_PORT=3306 \
  -e MYSQL_DATABASE=ai_gateway_mcp \
  -e MYSQL_USERNAME=root \
  -e MYSQL_PASSWORD=your_password_here \
  ai-gateway-mcp:latest
```

## 环境配置

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `SPRING_PROFILES_ACTIVE` | Spring 激活的配置 | `integration` |
| `MYSQL_HOST` | MySQL 主机地址 | `localhost` |
| `MYSQL_PORT` | MySQL 端口 | `3306` |
| `MYSQL_DATABASE` | 数据库名称 | `ai_gateway_mcp` |
| `MYSQL_USERNAME` | 数据库用户名 | `root` |
| `MYSQL_PASSWORD` | 数据库密码 | - |
| `JAVA_OPTS` | JVM 参数 | `-Xms256m -Xmx512m` |

### 使用 H2 内存数据库（测试用）

```bash
docker run -d \
  --name ai-gateway-mcp \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  ai-gateway-mcp:latest
```

访问 H2 控制台：http://localhost:8080/h2-console

### 使用外部 MySQL（生产/集成环境）

```bash
docker run -d \
  --name ai-gateway-mcp \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=integration \
  -e MYSQL_HOST=localhost \
  -e MYSQL_PORT=3306 \
  -e MYSQL_DATABASE=ai_gateway_mcp \
  -e MYSQL_USERNAME=root \
  -e MYSQL_PASSWORD=your_password_here \
  ai-gateway-mcp:latest
```

## 目录结构

```
ai-gateway-mcp/
├── Dockerfile              # Docker 构建文件
├── docker-compose.yml      # Docker Compose 配置
├── .dockerignore          # Docker 忽略文件
├── docker/
│   └── mysql/
│       └── init.sql       # MySQL 初始化脚本
├── front_end/             # 前端代码
└── src/                   # 后端代码
```

## 常见问题

### 1. 容器启动失败

检查日志：
```bash
docker-compose logs ai-gateway-mcp
```

### 2. 数据库连接失败

确保 MySQL 服务已启动并健康：
```bash
docker-compose ps
docker-compose logs mysql
```

### 3. 端口被占用

修改 `docker-compose.yml` 中的端口映射：
```yaml
ports:
  - "自定义端口:8080"
```

### 4. 数据持久化

MySQL 数据存储在 Docker 卷中：
```bash
# 查看数据卷
docker volume ls

# 备份数据
docker run --rm \
  -v ai-gateway-mcp_mysql_data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/mysql-backup.tar.gz /data
```

## 构建优化

### 本地开发构建

```bash
# 仅构建后端（快速）
docker build --target backend-builder -t ai-gateway-mcp:dev .

# 使用构建缓存
docker build --cache-from ai-gateway-mcp:latest -t ai-gateway-mcp:latest .
```

### 多平台构建

```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t ai-gateway-mcp:latest \
  --push .
```

## 健康检查

```bash
# 检查容器健康状态
docker inspect --format='{{.State.Health.Status}}' ai-gateway-mcp-app

# API 健康检查端点
curl http://localhost:8080/actuator/health
```

## 日志管理

```bash
# 查看应用日志
docker-compose logs -f ai-gateway-mcp

# 查看数据库日志
docker-compose logs -f mysql

# 导出日志
docker-compose logs ai-gateway-mcp > app.log
```

## 资源限制

在 `docker-compose.yml` 中添加：

```yaml
ai-gateway-mcp:
  deploy:
    resources:
      limits:
        cpus: '2'
        memory: 1G
      reservations:
        cpus: '0.5'
        memory: 512M
```
