# 构建阶段
FROM gradle:8.5-jdk21-alpine AS builder

WORKDIR /app

# 复制Gradle配置文件
COPY build.gradle .
COPY settings.gradle .
COPY gradle gradle
COPY gradlew .

# 复制源代码
COPY src src
COPY config config

# 构建应用
RUN ./gradlew build -x test --no-daemon

# 运行阶段
FROM openjdk:21-jre-slim

WORKDIR /app

# 创建非root用户
RUN addgroup --system app && adduser --system --group app

# 复制构建的jar文件
COPY --from=builder /app/build/libs/*.jar app.jar

# 更改文件所有者
RUN chown app:app app.jar

# 切换到非root用户
USER app

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:9960/actuator/health || exit 1

# 暴露端口
EXPOSE 9960

# 启动应用
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
