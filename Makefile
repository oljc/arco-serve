.PHONY: help start stop restart logs clean build test clean_build

# 默认目标
help:
	@echo "Arco Serve 开发环境命令"
	@echo ""
	@echo "开发环境:"
	@echo "  start    启动开发环境 (PostgreSQL + Redis)"
	@echo "  stop     停止开发环境"
	@echo "  restart  重启开发环境"
	@echo "  logs     查看服务日志"
	@echo "  clean    清理开发环境数据"
	@echo ""

	@echo "应用构建:"
	@echo "  build        构建应用"
	@echo "  test         运行测试"
	@echo "  run          运行应用 (dev profile)"
	@echo "  clean_build  清理构建文件"

# 开发环境管理
start:
	@echo "🚀 启动开发环境..."
	docker-compose up -d postgres redis
	@echo "✅ 开发环境已启动"

stop:
	@echo "🛑 停止开发环境..."
	docker-compose stop postgres redis
	@echo "✅ 开发环境已停止"

restart: stop start

logs:
	@echo "📋 查看服务日志 (Ctrl+C 退出)..."
	docker-compose logs -f postgres redis

clean:
	@echo "🧹 清理开发环境数据..."
	docker-compose down -v

# 应用构建和运行
build:
	@echo "🔨 构建应用..."
	./gradlew build

test:
	@echo "🧪 运行测试..."
	./gradlew test

run:
	@echo "🚀 运行应用 (dev profile)..."
	./gradlew bootRun --args='--spring.profiles.active=dev'

clean_build:
	@echo "🧹 清理构建文件..."
	./gradlew clean

# 完整的开发环境设置
setup: start
	@echo "⏳ 等待服务启动..."
	sleep 10
	@echo "🎉 开发环境设置完成！"
	@echo ""
	@echo "接下来:"
	@echo "  1. 运行应用: make run"
	@echo "  2. 或在 IDEA 中运行 (使用 dev profile)"
