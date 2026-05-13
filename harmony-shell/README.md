# AionUi Shell (HarmonyOS)

Native HarmonyOS shell for AionUi WebUI.

## Scope

- Multi-server management
- Embedded web shell via ArkWeb
- Immersive top navigation with back/refresh/server switch
- Loading and error overlays for connection feedback
- Upload/download bridge hooks
- Notification permission shim for web apps

## Status

This folder contains the initial HarmonyOS shell implementation that runs in parallel with `android-shell/`.

## Build

### DevEco Studio

1. 打开 DevEco Studio
2. 导入 `harmony-shell/`（Stage 模型工程）
3. 选择 `entry` 模块运行（Debug）

### Command line (optional)

1. 在 `harmony-shell/` 目录执行 `ohpm install`
2. 执行 `hvigorw assembleHap --mode module -p product=default -p buildMode=debug --no-daemon`

如果你本机 DevEco/SDK 版本与工程中的 `compatibleSdkVersion` 不一致，优先以 DevEco 创建的新工程模板版本为准，并同步调整 `@ohos/hvigor-ohos-plugin` 的版本号以匹配。

## GitHub Actions

如果你要用 GitHub 托管 runner 云构建，可以直接让 workflow 从一个公开可下载的对象存储 URL 拉取“鸿蒙 Command Line Tools”压缩包。

- Repository variables
  - `HARMONY_CLI_URL`: command line tools zip 下载地址（可选；workflow 已内置默认值）

Workflow: `.github/workflows/harmony-release-unsigned.yml`
