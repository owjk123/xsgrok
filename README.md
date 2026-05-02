# XSGrok - AI小说生成器

![Version](https://img.shields.io/badge/version-3.1.0-blue)
![Platform](https://img.shields.io/badge/platform-Android-green)
![License](https://img.shields.io/badge/license-MIT-orange)

## 简介

XSGrok 是一款基于AI的智能小说生成器，让创作变得简单有趣。只需输入一句话创意，AI就能帮你分解设定并生成精彩章节。

## 核心特性

### 🎨 一句话创作
- 输入简单的创意想法
- AI自动分解为6大基础设定（角色、人物关系、时间线、剧情走向等）
- 审阅编辑后一键生成章节

### 📚 书架管理
- 统一管理所有作品
- 快速浏览、继续创作
- 支持导出为TXT格式

### 📖 阅读体验
- 多种字体大小可选（小/中/大/特大）
- 可调节行间距（紧凑/适中/宽松）
- 沉浸阅读模式（隐藏系统栏）
- 深色/浅色主题切换

### ⚙️ API灵活配置
- 支持任意OpenAI兼容API
- 内置连接测试功能
- 多模型选择（GLM、GPT、Claude等）

## 界面预览

**3-Tab简洁导航**
- 创作：AI全自动生成模式
- 书架：作品管理
- 设置：API配置与偏好

**文学色系主题**
- 墨棕主色调，书卷气质
- 暖金色辅助，典雅温馨

## 技术架构

- **UI框架**: Jetpack Compose + Material3
- **架构模式**: MVVM
- **本地存储**: DataStore Preferences
- **网络请求**: HttpURLConnection + 流式响应
- **最小支持**: Android 8.0 (API 26)

## 版本更新

### v3.1.0 (2024)
- ✨ 精简为3-Tab导航
- 🎨 全新文学色系主题
- 📖 阅读体验全面升级（字体/间距/沉浸模式）
- 🔧 API配置引导与测试功能
- 🔒 API Key安全存储（Base64编码）
- 📝 生成进度实时字数统计
- 🗑️ 代码精简，删除冗余页面

### v3.0.x
- 基础功能完善
- 自动模式优化

## 使用说明

### 首次使用

1. 安装APK并启动
2. 在弹出引导中配置API Key
3. 测试连接确保可用
4. 开始创作

### API配置

支持以下服务商：
- [智谱GLM](https://open.bigmodel.cn/)
- [OpenAI GPT](https://platform.openai.com/)
- [Claude](https://www.anthropic.com/)
- 以及其他OpenAI兼容API

### 导出小说

1. 进入阅读页面
2. 点击分享图标
3. 选择分享方式

## 下载

- [Releases](https://github.com/owjk123/xsgrok/releases)

## License

MIT License - 欢迎Star和Fork

---

*用AI点燃创作灵感，让故事自然流淌*
