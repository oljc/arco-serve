# 多阶段构建 - 构建阶段
FROM gradle:8.5-jdk21 AS builder

# 设置工作目录
WORKDIR /app

# 复制构建文件
COPY build.gradle settings.gradle gradlew* ./
COPY gradle/ ./gradle/

# 复制源代码
COPY src/ ./src/
COPY config/ ./config/

# 构建应用
RUN ./gradlew build -x test --no-daemon

# 运行阶段
FROM eclipse-temurin:21-jre-alpine

# 创建非root用户
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# 安装必要的工具
RUN apk add --no-cache \
    curl \
    tzdata

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建应用目录
RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

# 切换到非root用户
USER appuser

# 设置工作目录
WORKDIR /app

# 从构建阶段复制jar文件
COPY --from=builder --chown=appuser:appgroup /app/build/libs/*.jar app.jar

# 暴露端口
EXPOSE 10086

# 启动应用
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
