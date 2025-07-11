<div align="center">

<a href="https://github.com/oljc/arco-admin">
  <img width="180" src="https://github.com/user-attachments/assets/09c91ec6-1de8-400e-878c-e1066667ff08" alt="Arco admin logo">
</a>

 # Arco Admin Serve 

![Java](https://img.shields.io/badge/Java-21-orange?style=plastic&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen?style=plastic&logo=springboot)
![Gradle](https://img.shields.io/badge/Gradle-8.0+-blue?style=plastic&logo=gradle)
![License](https://img.shields.io/badge/License-MIT-4080FF?style=plastic)
[![前端地址](https://img.shields.io/badge/前端地址-Arco%20Admin-brightgreen?style=plastic&logo=github)](https://github.com/oljc/arco-admin)

**基于 Spring Boot 3.5 + JDK 21 的现代模块化 Arco Admin 后端应用仓库**

*遵循官方推荐的最佳实践，参考 Spring Modulith 架构思路，采用按功能分模块的组织方式*

</div>

## ✨ 项目特色

- 🎯 **企业级架构设计** - 流行趋势 PBF 设计，DDD 分层领域驱动融合模块化单体架构模式
- 🚀 **现代化技术栈** - 基于 Java 21 + Spring Boot 3.5 最新技术
- 🔒 **安全性优先** - 借鉴字节安全设计，提供完整的认证授权、接口安全、数据脱敏等特性
- 📊 **可观测性** - 内置监控、日志、指标收集等生产级特性
- 📈 **高可扩展** - 模块化设计，支持平滑拆分为微服务

## 🏗️ 架构设计

### 设计原则

本项目采用**模块化单体架构**，参考近期的架构趋势借鉴了 Spring Modulith 架构思路同时融合 DDD + Clean Architecture 设计思想，采用按功能分模块的组织方式。


## 🚀 技术栈

### 核心框架
- Java 21
- Spring Boot 3.5.3

### 代码质量
- Jacoco
- Checkstyle

## 项目结构

```
arco-serve/
├── 📁 config                               # 项目相关配置
├── 📁 gradle                               # Gradle 配置
├── 📁 src/main/java/io/github/oljc/arcoserve/
│   ├── 📄 Application.java                   # 主应用类
│   ├── 📁 modules/                            # 业务
│   │   └── 📁 user/                          # 用户模块
│   │       └── 📄 UserController.java
│   │   └── 📁 chat/                          # 聊天模块
│   │       └── 📄 ChatController.java
│   └── 📁 shared/                            # 共享组件
│       ├── 📁 annotation/                    # 自定义注解
│       │   └── 📄 Signature.java
│       ├── 📁 config/                        # 全局配置
│       │   └── 📄 WebConfig.java
│       ├── 📁 exception/                     # 异常处理
│       │   ├── 📄 BusinessException.java
│       │   ├── 📄 Code.java
│       │   └── 📄 GlobalExceptionHandler.java
│       ├── 📁 response/                      # 响应处理
│       │   ├── 📄 ApiResponse.java
│       │   ├── 📄 PageData.java
│       │   └── 📄 ResponseAdvice.java
│       ├── 📁 util/                          # 工具类
│       └── 📁 web/                           # Web 组件（拦截器/过滤器/切面等）
├── 📁 src/main/resources/
│   ├── 📄 application-dev.yml                # 开发环境配置
│   ├── 📄 application-prod.yml               # 生产环境配置
│   ├── 📁 config/                           # 配置文件目录
│   ├── 📁 db/                               # 数据库脚本
│   │   ├── 📄 build.sql                     # 构建脚本
│   │   ├── 📄 data.sql                      # 数据脚本
│   │   ├── 📁 core/                         # 核心脚本
│   │   │   ├── 📄 base.sql                  # 基础脚本
│   │   │   └── 📄 enums.sql                 # 枚举脚本
│   │   └── 📁 schema/                       # 数据表结构
│   │       ├── 📄 auth.sql                  # 认证相关表
│   │       ├── 📄 logs.sql                  # 日志表
│   │       └── 📄 user.sql                  # 用户表
│   ├── 📁 static                            # 静态资源
│   └── 📁 templates                         # 模板文件
├── 📁 src/test/java/io/github/oljc/arcoserve/
│   ├── 📄 ApplicationTests.java             # 应用测试
│   ├── 📁 modules/                          # 模块测试
│   │   ├── 📁 user
│   │   └── 📁 chat
│   └── 📁 shared/                           # 共享组件测试
├── 📄 build.gradle                          # Gradle 构建配置
├── 📄 settings.gradle                       # Gradle 设置
├── 📄 gradlew                               # Gradle Wrapper 脚本
└── ... 
```

更多细节参考 [开发文档](https://github.com/oljc/arco-serve.wiki.git)

## 贡献

欢迎参与贡献，请参考 [贡献指南](CONTRIBUTING.md)。

<a href="https://github.com/oljc/arco-serve/graphs/contributors"><img src="https://contrib.rocks/image?repo=oljc/arco-serve" />
</a><br/>

前端:

<a href="https://github.com/oljc/arco-admin/graphs/contributors"><img src="https://contrib.rocks/image?repo=oljc/arco-admin" />
</a><br/>

- 📧 **邮箱**: ljc.byte@gmail.com
- 🐛 **问题反馈**: [GitHub Issues](https://github.com/oljc/arco-serve/issues)

## 📄 许可证

本项目采用 [MIT License](LICENSE) 许可证。

🌟 如果这个项目对你有帮助，请给个 Star ⭐
