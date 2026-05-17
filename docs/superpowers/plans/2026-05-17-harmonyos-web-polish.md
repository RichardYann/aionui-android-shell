# HarmonyOS Web Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 HarmonyOS Web 壳在加载反馈、顶部导航召回和应用图标上尽量贴近 Android 版本的现有体验。

**Architecture:** 主要在 `WebPage.ets` 内收口状态机和交互，不改动页面路由或服务器管理结构。加载逻辑从单一 `pageLoading` 调整为“启动空窗态 + 网页加载态”，同时移除调试 UI、精简顶部导航、放宽顶部召回阈值；图标则直接将 Android launcher 图形重绘为 Harmony SVG。

**Tech Stack:** ArkTS, ArkUI, ArkWeb `WebviewController`, HarmonyOS resource SVG, Android VectorDrawable 参考

---

### Task 1: 收口加载状态与正式 UI

**Files:**
- Modify: `harmony-shell/entry/src/main/ets/pages/WebPage.ets`

- [ ] **Step 1: 先记录要删除和保留的运行态元素**

```text
删除：
- debugEnabled
- debugLine
- setDebug()
- “网络自检”按钮
- 顶部调试浮层 Text(debugLine)

保留：
- 返回按钮
- 刷新按钮
- 切换服务器按钮
- 顶部 Progress
- 错误卡片
- 切换服务器底部弹层
```

- [ ] **Step 2: 引入 bootLoading 状态并让进入页面时立即进入启动加载态**

```ts
@State private bootLoading: boolean = true
@State private pageLoading: boolean = false
@State private loadProgress: number = 0

async aboutToAppear(): Promise<void> {
  const context = getContext(this) as common.UIAbilityContext
  this.storage = new ShellStorage(context)
  this.servers = await this.storage.getServers()
  const params = router.getParams() as Record<string, string>
  const initialUrl = (params && params.initialUrl) ? params.initialUrl : await this.storage.getLastUrl()
  this.currentUrl = initialUrl
  this.bootLoading = true
  this.pageLoading = false
  this.loadProgress = 0
  if (!this.currentUrl) {
    router.replaceUrl({ url: 'pages/ConnectPage' })
    return
  }
  this.scheduleHide()
}
```

- [ ] **Step 3: 让真实加载信号尽快关闭中央遮罩**

```ts
.onPageBegin((_event) => {
  this.bootLoading = false
  this.pageLoading = true
  this.loadProgress = 0
  this.clearError()
  this.revealNav()
})
.onProgressChange((event: OnProgressChangeEventShape) => {
  const next = event.newProgress
  this.loadProgress = next
  if (next > 0) {
    this.bootLoading = false
    this.pageLoading = next < 100
  }
})
.onPageEnd((_event) => {
  this.bootLoading = false
  this.pageLoading = false
  this.loadProgress = 100
  this.revealNav()
  this.injectNotificationShim()
  this.handleFileUploadRequest()
})
```

- [ ] **Step 4: 错误和返回场景也同步结束 bootLoading**

```ts
private showError(message: string): void {
  this.errorVisible = true
  this.errorMessage = message
  this.bootLoading = false
  this.pageLoading = false
  this.navVisible = true
}

private navigateToConnectPage(): void {
  this.switcherVisible = false
  this.bootLoading = false
  this.pageLoading = false
  this.navVisible = true
  this.clearError()
  router.replaceUrl({ url: 'pages/ConnectPage', params: { skipAutoOpen: '1' } })
}
```

- [ ] **Step 5: 让中央遮罩只在 bootLoading 时出现，并删除调试 UI 与网络自检**

```ts
Row({ space: 10 }) {
  Button($r('app.string.web_back')).onClick(() => this.handleBackAction())
  Button($r('app.string.web_refresh')).onClick(() => {
    this.clearError()
    this.bootLoading = false
    this.pageLoading = true
    this.controller.refresh()
    this.revealNav()
  })
  Button($r('app.string.web_switch_server')).onClick(async () => {
    await this.refreshServers()
    this.switcherVisible = true
    this.navVisible = true
  })
}
```

```ts
if (this.bootLoading) {
  Column({ space: 12 }) {
    LoadingProgress().width(32).height(32)
    Text($r('app.string.web_loading')).fontColor(Color.White)
  }
  .padding(24)
  .backgroundColor('#CC0F172A')
  .borderRadius(18)
  .margin({ top: 160 })
  .zIndex(20)
}
```

- [ ] **Step 6: 运行差异检查**

Run: `git diff -- harmony-shell/entry/src/main/ets/pages/WebPage.ets`
Expected: 差异集中在状态字段、顶部按钮行、中央遮罩条件和调试移除

- [ ] **Step 7: 提交加载与正式 UI 收口**

```bash
git add harmony-shell/entry/src/main/ets/pages/WebPage.ets
git commit -m "fix(harmony): polish web loading states"
```

### Task 2: 对齐顶部导航隐藏与召回手势

**Files:**
- Modify: `harmony-shell/entry/src/main/ets/pages/WebPage.ets`
- Reference: `android-shell/app/src/main/java/ai/resopod/aionui/shell/ui/web/WebActivity.kt`

- [ ] **Step 1: 对照安卓提炼手势目标**

```text
安卓现状：
- 加载中不隐藏导航
- 非加载时 2500ms 自动隐藏
- 隐藏后留一条薄热区
- 用户向下滑动超过较低阈值即可召回
```

- [ ] **Step 2: 为鸿蒙页面新增手势起点状态**

```ts
@State private navGestureStartY: number = 0
```

- [ ] **Step 3: 把隐藏热区从点击召回改成下滑召回**

```ts
Row() {
  Blank().layoutWeight(1)
}
.width('100%')
.height(6)
.backgroundColor('#334155')
.onTouch((event) => {
  const touch = event.touches && event.touches.length > 0 ? event.touches[0] : undefined
  if (!touch) {
    return
  }
  if (event.type === TouchType.Down) {
    this.navGestureStartY = touch.y
    return
  }
  if (event.type === TouchType.Move && touch.y - this.navGestureStartY > 10) {
    this.revealNav()
    return
  }
})
```

- [ ] **Step 4: 保证自动隐藏条件与安卓一致**

```ts
private scheduleHide(): void {
  if (this.hideTask) {
    clearTimeout(this.hideTask)
  }
  this.hideTask = setTimeout(() => {
    if (!this.bootLoading && !this.pageLoading && !this.errorVisible && !this.switcherVisible) {
      this.navVisible = false
    }
  }, 2500)
}
```

- [ ] **Step 5: 让导航视觉更接近“纯操作条”**

```ts
.padding({ left: 12, right: 12, top: 8, bottom: 8 })
.backgroundColor('#101820')
```

- [ ] **Step 6: 运行差异检查**

Run: `git diff -- harmony-shell/entry/src/main/ets/pages/WebPage.ets`
Expected: 新增 `navGestureStartY`、热区手势逻辑、自动隐藏条件微调、导航条 padding/background 收口

- [ ] **Step 7: 提交导航交互对齐**

```bash
git add harmony-shell/entry/src/main/ets/pages/WebPage.ets
git commit -m "fix(harmony): align nav reveal gesture"
```

### Task 3: 同步鸿蒙图标到安卓同款

**Files:**
- Modify: `harmony-shell/entry/src/main/resources/base/media/app_icon.svg`
- Reference: `android-shell/app/src/main/res/drawable/aionui_launcher_icon.xml`

- [ ] **Step 1: 将 Android vector 结构映射为 Harmony SVG**

```svg
<svg width="512" height="512" viewBox="0 0 80 80" xmlns="http://www.w3.org/2000/svg">
  <path fill="#000000" d="M12 0h56a12 12 0 0 1 12 12v56a12 12 0 0 1-12 12H12A12 12 0 0 1 0 68V12A12 12 0 0 1 12 0Z"/>
  <path fill="none" stroke="#FFFFFF" stroke-width="1" d="M12 0h56a12 12 0 0 1 12 12v56a12 12 0 0 1-12 12H12A12 12 0 0 1 0 68V12A12 12 0 0 1 12 0Z"/>
  <path fill="#FFFFFF" d="M40 20Q38 22 25 40Q23 42 26 42H30Q32 40 40 30Q48 40 50 42H54Q57 42 55 40Q42 22 40 20Z"/>
  <circle cx="40" cy="46" r="3" fill="#FFFFFF"/>
  <path fill="none" stroke="#FFFFFF" stroke-width="3.5" stroke-linecap="round" d="M18 50Q40 70 62 50"/>
</svg>
```

- [ ] **Step 2: 用完整 SVG 替换当前渐变 A 图标**

Run: `git diff -- harmony-shell/entry/src/main/resources/base/media/app_icon.svg`
Expected: 旧的渐变矩形与 A 形路径消失，改为黑底白线条图形

- [ ] **Step 3: 提交图标同步**

```bash
git add harmony-shell/entry/src/main/resources/base/media/app_icon.svg
git commit -m "style(harmony): match android app icon"
```

### Task 4: 推送并验证

**Files:**
- Modify: `harmony-shell/entry/src/main/ets/pages/WebPage.ets`
- Modify: `harmony-shell/entry/src/main/resources/base/media/app_icon.svg`

- [ ] **Step 1: 推送当前分支**

Run: `git push origin main`
Expected: 推送成功，仅触发 `harmony-release-unsigned`

- [ ] **Step 2: 观察最新 run**

Run: `gh run list --repo RichardYann/aionui-android-shell --limit 3 --json databaseId,displayTitle,workflowName,status,conclusion,url`
Expected: 最新 run 的 `workflowName` 为 `harmony-release-unsigned`

- [ ] **Step 3: 若成功，下载并安装新的 `.hap`**

```text
重点人工验证：
- 进入网页页时顶部进度反馈是否更快出现
- 页面出现后中央转圈是否立即消失
- 顶部是否仍有“半透明标题栏”观感
- 导航隐藏后能否更容易下滑召出
- 桌面图标是否与安卓一致
```

- [ ] **Step 4: 汇总结果给用户**

```text
- 加载状态机已优化
- 正式页面调试元素已移除
- 导航召回手感已对齐安卓
- 鸿蒙图标已替换为安卓同款
- 附最新 run 链接与 commit sha
```
