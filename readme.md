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

### 🚀 构建(build)

```bash
# 1. 克隆项目(如果你不想自己build可以直接到release下载后至第4步)
git clone https://github.com/pewee-live/netease-dl.git
#切换到项目目录
cd netease-dl

# 2. 修改配置(可选)
# 打开 src/main/resources/application.properties，修改：
download.path=/你的本地下载目录,注意下载目录必须以/结尾

# 3. 构建项目
./gradlew build    # 或 gradle build
cd build/libs/  	#切换到Build目录

# 4. 启动项目(也可以在这里通过注入参数的方式修改下载路径,注意下载目录必须以/结尾)
java  -jar   neteasemusic-1.0.0.jar --download.path=/media/music/

# 5. 访问页面
浏览器访问 http://127.0.0.1:8080/

```

## 🐳 Docker 使用

```bash
#拉取镜像
docker pull peweelive/netease-music-dl:latest
#启动
docker run -d -p 8080:8080 --privileged  --name=netease-music-dl  -v {你自己的路径}:/media/music/ --restart unless-stopped  -e TZ=Asia/Shanghai peweelive/netease-music-dl:latest
#浏览器访问 http://127.0.0.1:8080/

```

如果你需要自己构建dockeriamge,参考build中的步骤打包生成jar后
```bash

docker build -t {你自己的dockerid}/netease-music-dl:latest .

docker push {你自己的dockerid}/netease-music-dl:dl:latest

```

## 🙋 联系方式
👤 作者：pewee

📧 邮箱：690450725@qq.com

🌐 GitHub：pewee-live

🎉 欢迎 Star、Fork 和 Issue 交流～

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
