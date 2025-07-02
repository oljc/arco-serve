# Docker 部署指南

## 简介
本项目支持使用 Docker 和 Docker Compose 进行部署，使用最新的 PostgreSQL 16.1 作为数据库。

## 环境要求
- Docker 20.10+
- Docker Compose 2.0+

## 快速开始

### 1. 开发环境（仅启动数据库）

适合在本地开发时使用，只启动 PostgreSQL 数据库，应用程序在 IDE 中运行：

```bash
# 启动开发数据库
docker-compose -f docker-compose.dev.yml up -d

# 查看日志
docker-compose -f docker-compose.dev.yml logs -f

# 停止数据库
docker-compose -f docker-compose.dev.yml down
```

然后在 IDE 中以 `dev` profile 运行应用程序：
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 2. 完整部署（数据库 + 应用）

启动完整的应用栈：

```bash
# 构建并启动所有服务
docker-compose up --build -d

# 查看日志
docker-compose logs -f

# 停止所有服务
docker-compose down
```

## 数据库配置

### 默认配置
- **数据库名称**: `arco_serve`
- **用户名**: `arco_user`
- **密码**: `arco_password`
- **端口**: `5432`

### 生产环境配置
生产环境建议通过环境变量覆盖默认配置：

```bash
export DATABASE_URL=jdbc:postgresql://your-db-host:5432/your-db-name
export DATABASE_USERNAME=your-username
export DATABASE_PASSWORD=your-secure-password
export DATABASE_POOL_SIZE=20
export ADMIN_USERNAME=your-admin
export ADMIN_PASSWORD=your-secure-admin-password
```

## 应用访问

### 开发环境
- **应用地址**: http://localhost:9960
- **健康检查**: http://localhost:9960/actuator/health

### 生产环境
- **应用地址**: http://localhost:10086
- **健康检查**: http://localhost:10086/actuator/health

## 数据库管理

### 连接数据库
```bash
# 进入 PostgreSQL 容器
docker exec -it arco-serve-postgres-dev psql -U arco_user -d arco_serve

# 或者使用本地客户端连接
psql -h localhost -p 5432 -U arco_user -d arco_serve
```

### 查看初始数据
数据库包含以下测试用户：
- `admin` / `admin@example.com`
- `john.doe` / `john.doe@example.com`
- `jane.smith` / `jane.smith@example.com`

## 故障排除

### 常见问题

1. **端口冲突**
   ```bash
   # 检查端口占用
   lsof -i :5432
   lsof -i :9960
   ```

2. **容器无法启动**
   ```bash
   # 查看详细日志
   docker-compose logs postgres
   docker-compose logs app
   ```

3. **数据库连接失败**
   ```bash
   # 检查数据库健康状态
   docker-compose ps
   docker exec arco-serve-postgres-dev pg_isready -U arco_user
   ```

### 清理数据

```bash
# 停止并删除容器和网络
docker-compose down

# 删除数据卷（注意：这会删除所有数据）
docker-compose down -v

# 删除镜像
docker rmi $(docker images "arco-serve*" -q)
```

## 开发建议

### 数据持久化
- 开发环境数据存储在 `postgres_dev_data` 卷中
- 生产环境数据存储在 `postgres_data` 卷中

### 配置文件
- 开发环境: `application-dev.yml`
- Docker环境: `application-docker.yml`
- 生产环境: `application-prod.yml`

### 数据库迁移
项目使用 JPA/Hibernate 进行数据库管理：
- 开发环境: `ddl-auto: update`
- 生产环境: `ddl-auto: validate`

建议在生产环境使用 Liquibase 或 Flyway 进行数据库版本管理。 
