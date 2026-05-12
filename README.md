# AionUi Shell for Android

Lightweight Android shell for AionUi WebUI.  
一个面向 AionUi WebUI 的轻量 Android 客户端。

Bring your self-hosted AionUi experience to Android with a focused WebView shell designed for quick access, immersive navigation, and practical daily use.  
将你的自托管 AionUi 体验带到 Android 设备上，用更轻量、更顺手的方式快速访问、沉浸浏览并完成日常使用。

## ✨ Features / 功能

- Connect to any AionUi WebUI server through a simple first-run setup
- Automatically reopen the last connected server on later launches
- Navigate with back, refresh, and change-server controls
- Support file upload through standard web file inputs
- Download files through Android system DownloadManager
- 首次启动可快速输入并连接任意 AionUi WebUI 地址
- 后续启动可自动重新打开最近一次连接的服务器
- 提供返回、刷新、切换服务器等常用导航能力
- 支持网页中的标准文件上传能力
- 支持通过 Android 系统下载管理器下载文件

## 🌊 Highlights / 特点

- Lightweight and focused: open your AionUi workspace without extra native complexity
- Immersive navigation: the top bar stays hidden until you intentionally reveal it
- Clear loading feedback: connection and refresh states are easier to understand
- Self-hosted friendly: simple URL-based access flow for personal deployments
- AionUI branding: launcher icon and app presence now better reflect the product identity
- 轻量专注：无需复杂原生层，就能快速进入 AionUi 工作界面
- 沉浸体验：顶部导航默认隐藏，需要时再唤出
- 加载清晰：连接与刷新状态更容易理解
- 自托管友好：适合个人或团队通过 URL 快速接入
- 品牌一致：图标与整体观感更贴近 AionUI 产品形象

## 📦 Release / 发布下载

- Latest releases: https://github.com/RichardYann/aionui-android-shell/releases
- APK naming pattern: `aionui-shell-v<version>.apk`
- Current release flow prioritizes automation first, with formal production signing to be added later
- 最新版本下载入口：https://github.com/RichardYann/aionui-android-shell/releases
- APK 命名规则：`aionui-shell-v<version>.apk`
- 当前发布流程优先打通自动化链路，正式生产签名将在后续补充

## 🛠️ Build / 构建

### Open in Android Studio

1. Open `android-shell/`
2. Let Android Studio sync Gradle
3. Run on a device or emulator

### GitHub Actions

- Debug workflow: `.github/workflows/android-shell-build.yml`
- Release workflow: `.github/workflows/android-release.yml`
- Release artifact: `aionui-shell-v<version>.apk`

### Local APK Outputs

- Debug: `android-shell/app/build/outputs/apk/debug/app-debug.apk`
- Release: `android-shell/app/build/outputs/apk/release/app-release.apk`
