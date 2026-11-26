#!/bin/bash
# AI Gateway MCP - Docker 构建脚本

set -e

echo "============================================================"
echo "AI Gateway MCP - Docker 构建脚本"
echo "============================================================"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 构建模式选择
echo ""
echo "请选择构建模式："
echo "  1) 完整构建（需要访问内部 Maven 仓库）"
echo "  2) 本地构建（使用本地已构建的 JAR 包）"
echo ""
read -p "请输入选择 [1/2]: " choice

case $choice in
    1)
        echo -e "${YELLOW}开始完整构建...${NC}"
        echo "注意：需要确保可以访问内部 Maven 仓库"
        echo ""

        # 可选：传入 Nexus 凭证
        read -p "是否需要传入 Nexus 凭证？[y/N]: " need_creds

        if [[ $need_creds =~ ^[Yy]$ ]]; then
            read -p "Nexus 用户名：" nexus_user
            read -sp "Nexus 密码：" nexus_pass
            echo ""

            docker build \
                --build-arg NEXUS_USERNAME="$nexus_user" \
                --build-arg NEXUS_PASSWORD="$nexus_pass" \
                -t ai-gateway-mcp:latest .
        else
            docker build -t ai-gateway-mcp:latest .
        fi
        ;;

    2)
        echo -e "${YELLOW}使用本地构建模式...${NC}"
        echo ""

        # 检查是否已有 JAR 包
        if [ ! -f "target/*.jar" ]; then
            echo -e "${YELLOW}未找到 JAR 包，开始本地 Maven 构建...${NC}"
            mvn clean package -DskipTests
        fi

        # 检查前端是否已构建
        if [ ! -d "front_end/dist" ]; then
            echo -e "${YELLOW}前端未构建，开始构建前端...${NC}"
            cd front_end
            npm install --registry=https://registry.npmmirror.com
            npm run build
            cd ..
        fi

        echo -e "${YELLOW}使用 Dockerfile.local 构建镜像...${NC}"
        docker build -f Dockerfile.local -t ai-gateway-mcp:latest .
        ;;

    *)
        echo -e "${RED}无效的选择${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}构建完成！${NC}"
echo ""
echo "镜像标签：ai-gateway-mcp:latest"
echo ""
echo "运行方式："
echo "  1. 使用 docker-compose:"
echo "     docker-compose up -d"
echo ""
echo "  2. 使用 docker run:"
echo "     docker run -d \\"
echo "       --name ai-gateway-mcp \\"
echo "       -p 8080:8080 \\"
echo "       -e SPRING_PROFILES_ACTIVE=integration \\"
echo "       -e MYSQL_HOST=localhost \\"
echo "       -e MYSQL_PORT=3306 \\"
echo "       -e MYSQL_DATABASE=ai_gateway_mcp \\"
echo "       -e MYSQL_USERNAME=root \\"
echo "       -e MYSQL_PASSWORD=your_password_here \\"
echo "       ai-gateway-mcp:latest"
echo ""
