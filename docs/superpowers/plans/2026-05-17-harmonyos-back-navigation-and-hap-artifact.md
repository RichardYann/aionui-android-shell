# HarmonyOS Back Navigation And HAP Artifact Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 统一 Harmony Web 页的系统返回键与顶部返回按钮行为，并让 GitHub Actions 直接上传可下载的 `.hap` 文件作为构建产物。

**Architecture:** 在 `WebPage.ets` 中集中收口返回逻辑，用单一方法处理加载中、错误态、浮层态和 Web 历史后退；在 workflow 中新增构建产物整理步骤，把生成的 `.hap` 平铺复制到临时目录后上传，避免用户下载到层层嵌套目录。由于当前仓库没有成熟的 ArkTS 单测基础设施，运行时行为以最小改动 + 编译验证为主，workflow 变更以 YAML 语法和实际 Actions 运行结果验证。

**Tech Stack:** ArkTS, ArkUI, ArkWeb `WebviewController`, GitHub Actions YAML, `actions/upload-artifact`

---

### Task 1: 规划并记录实现边界

**Files:**
- Modify: `docs/superpowers/specs/2026-05-17-harmonyos-back-navigation-design.md`
- Create: `docs/superpowers/plans/2026-05-17-harmonyos-back-navigation-and-hap-artifact.md`

- [ ] **Step 1: 核对设计与现状**

Run: `git log -n 5 --oneline -- harmony-shell/entry/src/main/ets/pages/WebPage.ets`
Expected: 最近提交包含 `fix(harmony): reduce false web errors`

- [ ] **Step 2: 确认当前 workflow 产物上传方式**

Run: `powershell -Command "Get-Content .github/workflows/harmony-release-unsigned.yml | Select-Object -Last 20"`
Expected: 看到 `actions/upload-artifact@v4` 当前直接上传 `**/*.hap` 的路径匹配结果

- [ ] **Step 3: 记录实现计划并提交**

```bash
git add docs/superpowers/plans/2026-05-17-harmonyos-back-navigation-and-hap-artifact.md
git commit -m "docs(harmony): add back navigation implementation plan"
```

### Task 2: 统一 WebPage 返回逻辑

**Files:**
- Modify: `harmony-shell/entry/src/main/ets/pages/WebPage.ets`

- [ ] **Step 1: 先写出目标行为检查清单**

```text
1. switcherVisible=true 时返回键只关闭浮层
2. errorVisible=true 时返回键回 ConnectPage
3. pageLoading=true 时返回键停止加载并回 ConnectPage
4. pageLoading=false 且有 Web 历史时执行 backward()
5. pageLoading=false 且无 Web 历史时回 ConnectPage
6. 顶部返回按钮与系统返回键都调用同一个入口
```

- [ ] **Step 2: 实现统一返回入口**

```ts
private navigateToConnectPage(): void {
  this.switcherVisible = false
  this.errorVisible = false
  this.pageLoading = false
  this.navVisible = true
  router.replaceUrl({ url: 'pages/ConnectPage' })
}

private stopLoadingAndExit(): void {
  try {
    this.controller.stop()
  } catch (_error) {
    console.warn('Failed to stop current page load before exit.')
  }
  this.navigateToConnectPage()
}

private handleBackAction(): boolean {
  if (this.switcherVisible) {
    this.switcherVisible = false
    this.scheduleHide()
    return true
  }
  if (this.errorVisible) {
    this.navigateToConnectPage()
    return true
  }
  if (this.pageLoading) {
    this.stopLoadingAndExit()
    return true
  }
  try {
    if (this.controller.accessBackward()) {
      this.controller.backward()
      this.revealNav()
      return true
    }
  } catch (_error) {
    console.warn('Failed to navigate backward in web history.')
  }
  this.navigateToConnectPage()
  return true
}

onBackPress(): boolean {
  return this.handleBackAction()
}
```

- [ ] **Step 3: 顶部按钮切到统一入口**

```ts
Button($r('app.string.web_back')).onClick(() => this.handleBackAction())
```

- [ ] **Step 4: 运行一次最小编译验证**

Run: `gh run list --repo RichardYann/aionui-android-shell --limit 1 --json databaseId`
Expected: 能继续用 CI 验证新提交；本地无 Harmony CLI 时不强行做本地 assemble

- [ ] **Step 5: 提交返回逻辑变更**

```bash
git add harmony-shell/entry/src/main/ets/pages/WebPage.ets
git commit -m "fix(harmony): unify web back navigation"
```

### Task 3: 直接上传 `.hap` 文件

**Files:**
- Modify: `.github/workflows/harmony-release-unsigned.yml`

- [ ] **Step 1: 在 workflow 中新增产物整理目录**

```yaml
      - name: Collect unsigned HAP
        shell: bash
        run: |
          set -euo pipefail
          artifact_dir="${GITHUB_WORKSPACE}/harmony-artifacts"
          rm -rf "${artifact_dir}"
          mkdir -p "${artifact_dir}"
          find harmony-shell -path '*/build/*/outputs/*' -name '*.hap' -type f -exec cp {} "${artifact_dir}/" \;
          ls -la "${artifact_dir}"
          test -n "$(find "${artifact_dir}" -maxdepth 1 -name '*.hap' -type f -print -quit)"
```

- [ ] **Step 2: 让 upload-artifact 只上传平铺后的 `.hap`**

```yaml
      - name: Upload HAP artifacts
        uses: actions/upload-artifact@v4
        with:
          name: harmony-hap-release-unsigned
          path: harmony-artifacts/*.hap
          if-no-files-found: error
```

- [ ] **Step 3: 检查 workflow 差异**

Run: `git diff -- .github/workflows/harmony-release-unsigned.yml`
Expected: 只新增 `Collect unsigned HAP`，并把上传路径收口到 `harmony-artifacts/*.hap`

- [ ] **Step 4: 提交 workflow 变更**

```bash
git add .github/workflows/harmony-release-unsigned.yml
git commit -m "ci(harmony): upload hap files directly"
```

### Task 4: 推送并验证

**Files:**
- Modify: `harmony-shell/entry/src/main/ets/pages/WebPage.ets`
- Modify: `.github/workflows/harmony-release-unsigned.yml`

- [ ] **Step 1: 推送到远端**

Run: `git push origin main`
Expected: 推送成功，只触发 `harmony-release-unsigned`

- [ ] **Step 2: 观察新的 workflow run**

Run: `gh run list --repo RichardYann/aionui-android-shell --limit 3 --json databaseId,displayTitle,workflowName,status,conclusion,url`
Expected: 新 run 的 `workflowName` 为 `harmony-release-unsigned`

- [ ] **Step 3: 如果成功，确认产物直接是 `.hap`**

Run: `gh run view <RUN_ID> --repo RichardYann/aionui-android-shell`
Expected: artifact 名称不变，但下载后 zip 内直接是 `.hap` 文件而不是多层目录

- [ ] **Step 4: 汇总给用户**

```text
- 返回键统一逻辑已实现
- 系统返回键与顶部按钮已对齐
- workflow 改为上传平铺后的 `.hap`
- 给出最新 run 链接与 commit sha
```
