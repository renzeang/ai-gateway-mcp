# ============================================================
# AI Gateway MCP - 多阶段构建 Docker 构建文件
# 支持前后端一起运行
# ============================================================

# ------------------------------------------------------------
# 阶段 1: 构建前端
# ------------------------------------------------------------
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/node:20-alpine-linuxarm64 AS frontend-builder

WORKDIR /app/front_end

# 复制前端依赖文件
COPY front_end/package*.json ./

# 安装依赖（使用国内镜像）
RUN npm install --registry=https://registry.npmmirror.com

# 复制前端源代码
COPY front_end/ ./

# 构建前端（生产环境）
RUN npm run build

# ------------------------------------------------------------
# 阶段 2: 构建后端
# ------------------------------------------------------------
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/maven:3.9-eclipse-temurin-17-linuxarm64 AS backend-builder

WORKDIR /app

# 配置 Maven settings.xml（使用项目中的 ci-settings.xml）
COPY docker/ci-settings.xml /root/.m2/settings.xml

# 复制 Maven 配置文件
COPY pom.xml ./

# 复制源代码
COPY src/ ./src/

# 复制前端构建产物到 resources/static（在 Maven 打包之前，这样才会被打包进 JAR）
COPY --from=frontend-builder /app/front_end/dist ./src/main/resources/static

# 打包后端（跳过测试）
# 如需访问内部仓库，通过 -e NEXUS_USERNAME 和 -e NEXUS_PASSWORD 传入凭证
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------
# 阶段 3: 运行环境
# ------------------------------------------------------------
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/eclipse-temurin:17-jre-linuxarm64

WORKDIR /app

# 安装必要的运行时依赖
RUN apt install tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 从构建阶段复制 JAR 文件
COPY --from=backend-builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 健康检查（使用 API 端点）
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/admin/servers?page=1 || exit 1

# JVM 参数优化
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 运行应用
# 环境变量通过 docker run -e 或 docker-compose environment 传入
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
