.PHONY: help dev up down init app prod prod-up prod-down clean build rebuild logs logs-dev logs-prod ps ps-dev ps-prod check backup install check-docker urls
.DEFAULT_GOAL := help

dev: check-docker up app ## 启动！

up: ## 基础开发环境
	@echo "启动开发基础设施..."
	@docker-compose -f docker-compose.yml up -d

down: ## 停止开发基础设施
	@docker-compose -f docker-compose.yml down

init: ## 初始化数据库结构（可选）
	@docker exec -it arco-serve-postgres-dev psql -U devadmin -d bitdb -f /docker-entrypoint-initdb.d/init.sql

app: ## 启动 Spring Boot 本地应用（请在IDE或终端运行）
	@echo "🎯 环境已启动，请使用 IDE 启动 Application.java 或执行：./gradlew bootRun"


prod: prod-up ## 启动生产环境

prod-up: ## 启动生产服务
	@docker-compose -f docker-compose.prod.yml up -d

prod-down: ## 停止生产服务
	@docker-compose -f docker-compose.prod.yml down


build: ## 构建应用镜像
	@docker-compose build app

rebuild: ## 重新构建（无缓存）
	@docker-compose build --no-cache app

clean: ## 清理所有 Docker 容器/网络/镜像
	@docker-compose down -v --rmi local --remove-orphans
	@docker-compose -f docker-compose.dev.yml down -v --rmi local --remove-orphans
	@docker system prune -f

logs: ## 查看开发应用日志
	@docker-compose logs -f app

logs-dev: ## 查看开发基础设施日志
	@docker-compose -f docker-compose.dev.yml logs -f

logs-prod: ## 查看生产应用日志
	@docker-compose -f docker-compose.prod.yml logs -f app

ps: ## 查看开发应用状态
	@docker-compose ps

ps-dev: ## 查看开发基础设施状态
	@docker-compose -f docker-compose.dev.yml ps

ps-prod: ## 查看生产应用状态
	@docker-compose -f docker-compose.prod.yml ps

check: ## 代码检查
	@./gradlew codeQuality

install: ## 安装依赖
	@./gradlew build --refresh-dependencies

check-docker: ## 检查 Docker 是否运行
	@if ! docker info > /dev/null 2>&1; then echo "❌ Docker 未运行"; exit 1; fi

urls: ## 显示本地服务地址
	@echo "🌍 应用地址:     http://localhost:9960"
	@echo "📦 PostgreSQL:   jdbc:postgresql://localhost:5432/bitdb"
	@echo "📦 Redis:        redis://localhost:6379"

help: ## 显示帮助信息
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)
