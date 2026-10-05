# Fatum Echo 2

> 命定回响 — AI 对话前端，纯静态 PWA + Android WebView 壳 + 热更新。

## 在线预览

https://sonagi130.github.io/fatum-echo2/

手机浏览器打开，分享菜单「添加到主屏幕」即可作为独立 App 使用。

## 项目结构

```
├── index.html                # 前端主页面（单文件应用）
├── sw.js                     # PWA Service Worker
├── manifest.webmanifest      # PWA Manifest
├── album.html                # 相册页面
├── version.json              # 热更新版本号
├── *.png / *.webp / *.mp3    # 图标 / 主题图 / 音效
├── app/
│   ├── build.gradle          # APK 构建配置
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/echo/               # 前端页面（WebView 内置）
│       ├── java/io/github/sonagi130/echo/
│       │   ├── MainActivity.java      # WebView 宿主
│       │   ├── Updater.java           # 热更新管理
│       │   ├── HearthService.java     # 后台服务
│       │   ├── HearthNotif.java       # 本地通知
│       │   └── HearthAccess.java      # 无障碍服务
│       └── res/                       # Android 资源
├── build.gradle
├── settings.gradle
└── twa/signing.keystore      # 签名
```

## 功能

- **流式对话** — SSE 实时流式回复
- **思考链** — AI reasoning 独立折叠块，可展开/收起
- **虚拟列表** — 长对话滚动不卡，内容感知估高
- **附件** — 图片/文件发送与展示
- **相册** — 独立相册页面
- **双主题** — Harbor（暗）/ Pearl（亮）
- **PWA** — 可安装到桌面，离线可用
- **热更新** — 不装新 APK 也能更新前端

## 配置

打开 `index.html`，顶部 `<script>` 里有配置块：

```js
const CONFIG = {
  APP_NAME:   "Tidal Echo",
  AI_NAME:    "Claude",
  HUMAN_NAME: "你",
  SINCE:      "2026/01/01",
};
```

改这一处就够。`API_BASE` 是后端地址，默认同源相对路径 `/relay`。

## 热更新机制

1. App 启动时请求 `version.json` 比对版本号
2. 远程版本 > 本地 → 下载 `web.zip`，解压到 `filesDir/echo_www/`
3. WebView 优先读 `echo_www/`，缺失则回退 `assets/echo/`

## 构建

```bash
# 需要 Android SDK + Gradle 8.4
./gradlew assembleRelease
# 输出: app/build/outputs/apk/release/app-release.apk
```

## 技术栈

- 前端：纯 HTML/CSS/JS（无框架，单文件）
- 客户端：Android WebView (Java)
- 通信：SSE
- 托管：GitHub Pages

## License

MIT
