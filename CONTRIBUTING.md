# 贡献指南 (CONTRIBUTING)

欢迎为 **Arco Admin Serve** 项目贡献代码！我们非常感谢您的参与和支持。


## 📑 行为准则

参与贡献前请先阅读 [行为准则](.github/CODE_OF_CONDUCT.md)，希望参与项目的贡献者都能严格遵守。

## 📢 问题反馈

项目使用 [Github issues](https://github.com/arco-design/arco-design-vue/issues) 采集 bug 反馈和新 feature 建议。在报告 bug 之前，请确保已经搜索过相似的问题，因为它们可能已经得到解答或正在被修复。

- **邮箱**: ljc.byte@gmail.com
- **GitHub Issues**: [提交问题](https://github.com/oljc/arco-serve/issues)

## 🤝 参与贡献

```mermaid
graph LR
    A[📦 Fork 仓库] --> B[⬇️ 克隆代码]
    B --> C[🌿 新建分支]
    C --> D[🛠 编写代码]
    D --> E[✅ 提交代码]
    E --> F[🔀 创建 PR]
    F --> G[👀 等待审核]
    G --> H[✅ 合并代码]
    H --> I[🗑️ 删除分支]
```

### 拉取代码

```bash
git clone https://github.com/oljc/arco-serve.git
```
### 环境准备

确保您的开发环境满足以下要求：

| 工具 | 版本要求 | 说明 |
|------|----------|------|
| ☕ **JDK** | 21+ | 推荐使用 OpenJDK 或 Eclipse Temurin |
| 🔧 **Gradle** | 8.0+ | 可使用项目内置的 Gradle Wrapper |
| 💻 **IDE** | 任意 | 推荐 IntelliJ IDEA 或 VS Code |

### 克隆项目

```bash
git clone https://github.com/oljc/arco-serve.git
```

### 本地启动

```bash
```

访问以下地址验证服务启动成功：

- 🌐 **健康检查**: http://localhost:9960/actuator/health
- 📊 **监控指标**: http://localhost:9960/actuator/metrics

### 新建分支

分支命名推荐规范（不强制）：<类型>/<描述性名称>

```bash
git checkout -b fix/token-expired-bug
```

### 编写代码

现在你可以自由的修改代码来新增功能或修复 bug。但是请注意项目的 [代码规范](#-代码规范)。

### 提交代码

Commit messages 请遵循 [conventional-changelog 标准](https://www.conventionalcommits.org/zh-hans/v1.0.0/)：

> `<类型>[可选 范围]: <描述>`
> 
> [可选 正文] 
> 
> [可选 脚注]

> - feat: 新特性或功能  
> - fix: 缺陷修复  
> - docs: 文档更新  
> - style: 代码风格或者组件样式更新  
> - refactor: 代码重构，不引入新功能和缺陷修复  
> - perf: 性能优化  
> - test: 单元测试  
> - chore: 其他不修改 src 或测试文件的提交 

示例：
```bash
git add .
git commit -m "feat(user): 添加用户注册功能"
```

## 📏 开发规范

## 许可证

[MIT 协议](./LICENSE)
