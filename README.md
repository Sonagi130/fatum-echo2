# Fatum Echo 2

> 命定回响 — 自托管 AI 对话前端，Android WebView 壳 + 纯前端页面 + 热更新

## 预览

📱 在线体验（GitHub Pages）：  
https://sonagi130.github.io/fatum-echo2/

## 项目结构

```
├── app/
│   ├── build.gradle                    # APK 构建配置 (io.github.sonagi130.echo)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/echo/                # 前端页面（WebView 加载）
│       │   ├── index.html              # 主页面（单文件应用，~7500行）
│       │   ├── album.html              # 相册页面
│       │   ├── sw.js                   # PWA Service Worker
│       │   ├── manifest.webmanifest     # PWA Manifest
│       │   ├── version.json             # 热更新版本号
│       │   ├── web.zip                 # 热更新打包
│       │   └── *.png *.webp *.mp3      # 图标 / 主题 / 音效
│       ├── java/io/github/sonagi130/echo/
│       │   ├── MainActivity.java       # WebView 宿主 + 本地资源拦截
│       │   ├── Updater.java            # 热更新：检查 version.json → 下载 web.zip → 解压
│       │   ├── HearthService.java       # 后台服务
│       │   ├── HearthNotif.java         # 通知管理
│       │   └── HearthAccess.java        # 无障碍服务
│       └── res/                        # Android 资源
├── build.gradle
├── settings.gradle
├── twa/signing.keystore                # 签名
└── .github/workflows/webview.yml       # CI 构建
```

## 功能

- 💬 **流式对话** — 支持 SSE 流式回复，实时渲染
- 🧠 **思考链展示** — AI reasoning 独立折叠块，位于气泡上方，可展开/收起
- 📎 **附件** — 图片/文件发送与展示
- 🎵 **音乐播放器** — 聊天卡片内播放（网易云 API）
- 📷 **相册** — 独立相册页面
- 🌗 **双主题** — Harbor（暗）/ Light（亮）
- 📱 **PWA** — 可安装到桌面，离线可用
- 🔄 **热更新** — 不装新 APK 也能更新前端页面

## 热更新机制

1. App 启动时请求 `version.json` 比对本地版本号
2. 远程版本 > 本地 → 下载 `web.zip`，解压到 `filesDir/echo_www/`
3. WebView 优先读 `echo_www/`，缺失则回退 `assets/echo/`

## 构建

```bash
# 需要 Android SDK + Gradle 8.4
./gradlew assembleRelease
# 输出 APK: app/build/outputs/apk/release/app-release.apk
```

## 技术栈

- 前端：纯 HTML/CSS/JS（无框架，单文件）
- 客户端：Android WebView (Java)
- 通信：SSE (Server-Sent Events)
- 更新：GitHub Pages 托管 version.json + web.zip

## License

MIT