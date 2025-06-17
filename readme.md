# 🎵 网易云音乐无损下载器（Java + Spring Boot + Thymeleaf）

本项目是一个基于 **Java 后端（Spring Boot）+ 前端 Thymeleaf** 实现的网易云音乐无损下载工具，支持查询与下载单曲、歌单和专辑的音频资源，并展示下载歌词信息，支持二维码扫码登录。

> 📌 本项目中的音乐解析 API 完全基于 [Suxiaoqinx/Netease_url](https://github.com/Suxiaoqinx/Netease_url) Python 项目，用 Java 重写。请求参数和响应结构与该项目完全一致。

---

## ✨ 功能特性

- ✅ 查询并展示单曲信息、歌词、封面图
- ✅ 查询并分页展示歌单内容
- ✅ 查询专辑信息及其歌曲列表
- ✅ 支持关键词搜索歌曲
- ✅ 支持下载单曲、歌单、专辑音频资源
- ✅ 支持扫码登录功能

---

## 🖼️ 页面截图

| 歌单下载页 | 专辑下载页 |
|------------|-------------|
| ![歌单下载页面](https://raw.githubusercontent.com/pewee-live/netease-dl/refs/heads/master/pics/1.JPG) | ![专辑下载页面](https://raw.githubusercontent.com/pewee-live/netease-dl/refs/heads/master/pics/2.JPG) |

---

## 🧩 页面说明

前端使用 **HTML + JavaScript + Axios** 编写，无任何前端框架依赖，简单易用，主要分为以下模块：

### 🎵 歌单模块

- 加载用户歌单列表
- 查看歌单详情、分页查看歌曲
- 提供歌单下载按钮

### 💽 专辑模块

- 输入专辑 ID，查询专辑详细信息及歌曲列表
- 提供专辑下载按钮

### 🔍 搜索模块

- 输入关键词搜索歌曲
- 可扩展：点击后加入下载功能

### 🎧 查看歌曲模块

- 输入歌曲 ID 与音质（level）进行信息查看
- 展示歌曲名、文件大小、专辑名、歌手名、歌词和翻译歌词

---

## ⚙️ 使用说明

### 📦 环境要求

- ✅ Java 8 或以上版本
- ✅ Maven 3.x
- ✅ 网络可访问网易云音乐（含其 CDN）
- ✅ 推荐使用 Chrome 浏览器
- ✅ 操作系统兼容：Windows / macOS / Linux

---

### 🚀 启动方式

```bash
# 1. 克隆项目
git clone https://github.com/pewee-live/netease-dl.git

# 2. 修改配置
# 打开 src/main/resources/application.properties，修改：
download.path=/你的本地下载目录

# 3. 构建项目
cd netease-dl
./gradlew build    # 或 gradle build

# 4. 启动项目
java -jar build/libs/neteasemusic-1.0.0.jar

# 5. 访问页面
浏览器访问 http://127.0.0.1:8080/

```

## 🐳 Docker 使用（TODO）
敬请期待 Docker 化部署方式。

## 🙋 联系方式
👤 作者：pewee

📧 邮箱：690450725@qq.com

🌐 GitHub：pewee-live

🎉 欢迎 Star、Fork 和 Issue 交流～
