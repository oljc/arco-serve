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
├── 📁 src/main/java/io/github/oljc/arcoserve/
│   ├── 📄 Application.java                   # 🚀 主应用类
│   ├── 📁 modules/                            # 🎯 业务模块
│   │   ├── 📁 user/                          # 👤 用户模块
│   │   │   ├── 📄 User.java                  # 实体类
│   │   │   ├── 📄 UserController.java        # REST 控制器
│   │   │   ├── 📄 UserService.java           # 业务服务
│   │   │   ├── 📄 UserRepository.java        # 数据仓储
│   │   │   └── 📁 dto/                       # 数据传输对象
│   │   │       ├── 📄 CreateUserRequest.java
│   │   │       └── 📄 UserResponse.java
│   │   ├── 📁 ...                   
│   ├── 📁 shared/                            # 🔗 共享组件
│   │   ├── 📁 config/                        # 全局配置
│   │   │   └── 📄 SecurityConfig.java
│   │   ├── 📁 exception/                     # 异常处理
│   │   │   ├── 📄 BusinessException.java
│   │   │   ├── 📄 ErrorResponse.java
│   │   │   └── 📄 GlobalExceptionHandler.java
│   │   └── 📁 util/                          # 工具类
│   │       └── 📄 ValidationUtils.java
│   └── 📁 infrastructure/                    # 🏗️ 基础设施
│       ├── 📁 database/                      # 数据库相关
│       └── 📁 external/                      # 外部服务
├── 📁 src/main/resources/
│   ├── 📄 application.yml                    # 主配置文件
│   ├── 📄 application-dev.yml                # 开发环境配置
│   ├── 📄 application-prod.yml               # 生产环境配置
│   ├── 📁 config/                           # 配置文件目录
│   ├── 📁 static/                           # 静态资源
│   └── 📁 templates/                        # 模板文件
├── 📁 src/test/java/io/github/oljc/arcoserve/
│   ├── 📁 modules/user/
│   │   └── 📄 UserServiceTest.java           # 单元测试
│   ├── 📁 integration/
│   │   └── 📄 UserIntegrationTest.java       # 集成测试
│   ├── 📁 architecture/
│   │   └── 📄 ArchitectureTest.java          # 架构测试
│   └── 📁 shared/                           # 共享组件测试
├── 📄 build.gradle                          # Gradle 构建配置
├── 📄 README.md                             # 项目文档
└── 📄 .gitignore                            # Git 忽略配置
```

## 贡献

欢迎参与贡献，请参考 [贡献指南](CONTRIBUTING.md)。

后端:
<a href="https://github.com/oljc/arco-serve/graphs/contributors"><img src="https://contrib.rocks/image?repo=oljc/arco-serve" />
</a><br/>

前端:

<a href="https://github.com/oljc/arco-admin/graphs/contributors"><img src="https://contrib.rocks/image?repo=oljc/arco-admin" />
</a><br/>

## 💡 技术支持

#### 联系方式
- 📧 **邮箱**: ljc.byte@gmail.com
- 🐛 **问题反馈**: [GitHub Issues](https://github.com/oljc/arco-serve/issues)

## 📄 许可证

本项目采用 [MIT License](LICENSE) 许可证。

🌟 如果这个项目对你有帮助，请给个 Star ⭐
