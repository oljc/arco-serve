# 🤝 贡献指南

首先，感谢您对 Arco Serve 项目的贡献兴趣！我们欢迎各种形式的贡献。

## 📋 目录

- [开始之前](#开始之前)
- [开发环境设置](#开发环境设置)
- [提交代码](#提交代码)
- [代码规范](#代码规范)
- [测试要求](#测试要求)
- [提交信息规范](#提交信息规范)
- [Pull Request 流程](#pull-request-流程)

## 🚀 开始之前

### 贡献类型

我们接受以下类型的贡献：

- 🐛 **Bug 修复**：修复已知问题
- ✨ **新功能**：添加新的功能或特性
- 📚 **文档改进**：改进文档、示例或注释
- 🎨 **代码优化**：代码重构、性能优化
- 🧪 **测试改进**：添加或改进测试覆盖率

### 贡献前检查

在开始贡献之前，请：

1. 搜索现有的 [Issues](https://github.com/oljc/arco-serve/issues) 和 [Pull Requests](https://github.com/oljc/arco-serve/pulls)
2. 对于较大的变更，请先创建 Issue 进行讨论
3. 阅读我们的 [行为准则](CODE_OF_CONDUCT.md)

## 🛠️ 开发环境设置

### 必需软件

- **Java**: JDK 21 或更高版本
- **Git**: 用于版本控制
- **IDE**: 推荐使用 IntelliJ IDEA 或 VS Code

### 环境配置

1. **Fork 并克隆仓库**
   ```bash
   git clone https://github.com/YOUR_USERNAME/arco-serve.git
   cd arco-serve
   ```

2. **设置上游仓库**
   ```bash
   git remote add upstream https://github.com/oljc/arco-serve.git
   ```

3. **安装依赖**
   ```bash
   ./gradlew build
   ```

4. **运行测试**
   ```bash
   ./gradlew test
   ```

## 📝 提交代码

### 分支策略

- `main`: 主分支，包含稳定的代码
- `develop`: 开发分支，用于集成新功能
- `feature/*`: 功能分支，用于开发新功能
- `fix/*`: 修复分支，用于修复问题

### 工作流程

1. **创建功能分支**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **进行开发**
   - 编写代码
   - 添加测试
   - 更新文档

3. **提交变更**
   ```bash
   git add .
   git commit -m "feat: 添加新功能描述"
   ```

4. **保持同步**
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

5. **推送到你的 Fork**
   ```bash
   git push origin feature/your-feature-name
   ```

开发细节参考 [开发文档](https://github.com/oljc/arco-serve.wiki.git)

## 📏 代码规范

### Java 代码规范

- 使用 4 个空格缩进
- 行长度限制为 120 字符
- 遵循 Google Java Style Guide
- 使用有意义的变量和方法名

### Checkstyle

项目使用 Checkstyle 进行代码风格检查：

```bash
./gradlew checkstyleMain checkstyleTest
```

### PMD

使用 PMD 进行静态代码分析：

```bash
./gradlew pmdMain pmdTest
```

### 代码质量检查

运行所有质量检查：

```bash
./gradlew codeQuality
```

## 🧪 测试要求

### 测试覆盖率

- 新代码的测试覆盖率必须达到 **80%** 以上
- 修改现有代码时，不能降低整体覆盖率

### 测试类型

1. **单元测试**：测试单个组件的功能
2. **集成测试**：测试组件间的交互
3. **架构测试**：使用 ArchUnit 验证架构规则

### 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行单元测试
./gradlew test --tests="*Test"

# 运行集成测试
./gradlew test --tests="*IntegrationTest"

# 生成覆盖率报告
./gradlew jacocoTestReport
```

## 📋 提交信息规范

我们使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

### 格式

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### 类型

- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档变更
- `style`: 代码格式化
- `refactor`: 重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动
- `ci`: CI 配置文件变更
- `build`: 构建系统变更

### 示例

```bash
feat(user): 添加用户注册功能

- 添加用户注册 API
- 实现邮箱验证
- 添加相关测试

Closes #123
```

## 🔄 Pull Request 流程

### 创建 PR

1. 确保你的分支是最新的
2. 推送到你的 Fork
3. 在 GitHub 上创建 Pull Request
4. 填写 PR 模板中的所有必需信息

### PR 要求

- [ ] 代码通过所有 CI 检查
- [ ] 测试覆盖率满足要求
- [ ] 代码符合项目规范
- [ ] 包含适当的文档更新
- [ ] PR 标题遵循 Conventional Commits

### Review 过程

1. **自动检查**：CI 会自动运行所有检查
2. **代码审查**：项目维护者会审查代码
3. **反馈处理**：根据反馈进行修改
4. **合并**：通过审查后合并到主分支

## 🐛 报告问题

使用 [Bug 报告模板](../../issues/new?assignees=&labels=bug&template=bug_report.md&title=%5BBUG%5D+) 来报告问题。

## ✨ 功能请求

使用 [功能请求模板](../../issues/new?assignees=&labels=enhancement&template=feature_request.md&title=%5BFEATURE%5D+) 来提出新功能建议。


再次感谢您的贡献！🎉 
