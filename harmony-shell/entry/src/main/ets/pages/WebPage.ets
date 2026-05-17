import { promptAction, router } from '@kit.ArkUI'
import { common, Want } from '@kit.AbilityKit'
import { webview } from '@kit.ArkWeb'
import { DownloadBridge } from '../common/downloadBridge'
import { NotificationBridge, NotificationProxy } from '../common/notificationBridge'
import { ShellStorage } from '../common/storage'
import { isHttpLike } from '../common/urlGuard'
import { createWebUiState } from '../common/webUiState'
import { buildNotificationShim } from '../common/webBridge'
import type { ServerEntry } from '../common/serverModel'

interface WebResourceRequestShape {
  getRequestUrl(): string
  isMainFrame(): boolean
}

interface WebResourceErrorShape {
  getErrorCode(): number
  getErrorInfo(): string
}

interface OnErrorReceiveEventShape {
  request: WebResourceRequestShape
  error: WebResourceErrorShape
}

interface OnProgressChangeEventShape {
  newProgress: number
}

interface HttpErrorResponseShape {
  getResponseCode(): number
}

interface OnHttpErrorReceiveEventShape {
  request: WebResourceRequestShape
  response: HttpErrorResponseShape
}

@Entry
@Component
struct WebPage {
  @State private currentUrl: string = ''
  @State private navVisible: boolean = createWebUiState().navVisible
  @State private bootLoading: boolean = true
  @State private pageLoading: boolean = false
  @State private errorVisible: boolean = createWebUiState().errorVisible
  @State private errorMessage: string = createWebUiState().errorMessage
  @State private switcherVisible: boolean = false
  @State private servers: ServerEntry[] = []
  @State private loadProgress: number = 0

  private controller: webview.WebviewController = new webview.WebviewController()
  private notificationBridge: NotificationBridge = new NotificationBridge()
  private notificationProxy: NotificationProxy = new NotificationProxy(this.notificationBridge)
  private downloadBridge: DownloadBridge = new DownloadBridge()
  private storage?: ShellStorage
  private hideTask?: number

  private isPrimaryRequest(url: string): boolean {
    if (!url) {
      return false
    }
    return url === this.currentUrl || this.currentUrl.startsWith(url) || url.startsWith(this.currentUrl)
  }

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

  private revealNav(): void {
    this.navVisible = true
    this.scheduleHide()
  }

  private showError(message: string): void {
    this.errorVisible = true
    this.errorMessage = message
    this.bootLoading = false
    this.pageLoading = false
    this.navVisible = true
  }

  private clearError(): void {
    this.errorVisible = false
    this.errorMessage = ''
  }

  private navigateToConnectPage(): void {
    this.switcherVisible = false
    this.bootLoading = false
    this.pageLoading = false
    this.navVisible = true
    this.clearError()
    router.replaceUrl({ url: 'pages/ConnectPage', params: { skipAutoOpen: '1' } })
  }

  private stopLoadingAndExit(): void {
    try {
      this.controller.stop()
    } catch (_error) {
      console.warn('Failed to stop current page load before leaving.')
    }
    this.navigateToConnectPage()
  }

  private formatWebError(event: object | undefined): string {
    const unknown = this.currentUrl ? this.currentUrl : '<unknown>'
    const payload = event as Record<string, object> | undefined
    const request = payload && payload.request ? (payload.request as Record<string, Function>) : undefined
    const failedUrl = request && typeof request.getRequestUrl === 'function'
      ? `${request.getRequestUrl()}`
      : unknown

    const error = payload && payload.error ? (payload.error as Record<string, Function>) : undefined
    let code = ''
    let desc = ''
    if (error) {
      try {
        if (typeof error.getErrorCode === 'function') {
          code = `${error.getErrorCode()}`
        }
      } catch (_err) {
        code = ''
      }
      try {
        if (typeof error.getErrorInfo === 'function') {
          desc = `${error.getErrorInfo()}`
        } else if (typeof error.getErrorDescription === 'function') {
          desc = `${error.getErrorDescription()}`
        }
      } catch (_err) {
        desc = ''
      }
    }

    const meta = [code ? `code=${code}` : '', desc ? `desc=${desc}` : ''].filter((item) => item.length > 0).join(', ')
    return meta ? `页面加载失败：${failedUrl}（${meta}）` : `页面加载失败：${failedUrl}`
  }

  private async refreshServers(): Promise<void> {
    if (!this.storage) {
      return
    }
    this.servers = await this.storage.getServers()
  }

  private async openServer(server: ServerEntry): Promise<void> {
    if (!this.storage) {
      return
    }
    await this.storage.setLastUrl(server.url)
    await this.storage.setLastServerId(server.id)
    this.currentUrl = server.url
    this.switcherVisible = false
    this.clearError()
    this.bootLoading = false
    this.pageLoading = true
    this.loadProgress = 0
    this.revealNav()
  }

  private async injectNotificationShim(): Promise<void> {
    const state = await this.notificationBridge.getPermissionState()
    try {
      this.controller.runJavaScript(buildNotificationShim(state))
    } catch (_error) {
      console.error('Failed to inject notification shim')
    }
  }

  private async openExternalUrl(url: string): Promise<void> {
    try {
      const context = getContext(this) as common.UIAbilityContext
      const want: Want = {
        uri: url
      }
      await context.startAbility(want)
    } catch (_error) {
      promptAction.showToast({ message: `无法打开该链接：${url}` })
    }
  }

  private handleFileUploadRequest(): void {
    console.info('ArkWeb file chooser integration point is ready.')
  }

  private handleBackAction(): boolean {
    if (this.switcherVisible) {
      this.switcherVisible = false
      this.revealNav()
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

  @Builder
  private SwitcherSheet() {
    Column({ space: 12 }) {
      Row() {
        Text('切换服务器').fontSize(20).fontWeight(FontWeight.Bold).fontColor(Color.White)
        Blank().layoutWeight(1)
        Button('关闭').onClick(() => {
          this.switcherVisible = false
          this.scheduleHide()
        })
      }
      .width('100%')

      if (this.servers.length === 0) {
        Text('没有已保存服务器').fontColor('#CBD5E1')
      } else {
        ForEach(this.servers, (server: ServerEntry) => {
          Button(`${server.name} · ${server.url}`)
            .width('100%')
            .onClick(() => this.openServer(server))
        }, (server: ServerEntry) => server.id)
      }

      Button('管理服务器')
        .width('100%')
        .onClick(() => this.navigateToConnectPage())
    }
    .width('100%')
    .padding(20)
    .backgroundColor('#0F172A')
    .borderRadius({ topLeft: 20, topRight: 20 })
  }

  build() {
    Stack({ alignContent: Alignment.Top }) {
      Column({ space: 0 }) {
        if (this.navVisible) {
          Column({ space: 0 }) {
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
            .width('100%')
            .padding({ left: 12, right: 12, top: 10, bottom: 10 })
            .backgroundColor('#111827')
            .zIndex(10)

            if (this.pageLoading) {
              Progress({ value: this.loadProgress, total: 100, type: ProgressType.Linear })
                .width('100%')
                .height(2)
                .color('#60A5FA')
                .zIndex(10)
            }
          }
        } else {
          Row() {
            Blank().layoutWeight(1)
          }
          .width('100%')
          .height(8)
          .backgroundColor('#334155')
          .onTouch(() => this.revealNav())
        }

        if (!this.switcherVisible && !this.errorVisible) {
          Web({ src: this.currentUrl, controller: this.controller })
            .width('100%')
            .layoutWeight(1)
            .javaScriptAccess(true)
            .domStorageAccess(true)
            .fileAccess(true)
            .imageAccess(true)
            .onlineImageAccess(true)
            .mixedMode(MixedMode.All)
            .javaScriptProxy({
              object: this.notificationProxy,
              name: 'AionUiHarmonyBridge',
              methodList: ['requestNotificationPermission'],
              controller: this.controller
            })
            .onPageBegin((_event) => {
              this.bootLoading = false
              this.pageLoading = true
              this.loadProgress = 0
              this.clearError()
              this.revealNav()
            })
            .onPageEnd((_event) => {
              this.bootLoading = false
              this.pageLoading = false
              this.loadProgress = 100
              this.revealNav()
              this.injectNotificationShim()
              this.handleFileUploadRequest()
            })
            .onProgressChange((event: OnProgressChangeEventShape) => {
              const next = event.newProgress
              this.loadProgress = next
              if (next > 0) {
                this.bootLoading = false
                this.pageLoading = next < 100
              }
            })
            .onErrorReceive((event: OnErrorReceiveEventShape) => {
              const failedUrl = event.request.getRequestUrl()
              const isMainFrame = event.request.isMainFrame()
              if (!isMainFrame) {
                console.warn(`Ignore subresource load failure: ${failedUrl}`)
                return
              }
              this.showError(this.formatWebError(event))
            })
            .onHttpErrorReceive((event: OnHttpErrorReceiveEventShape) => {
              const failedUrl = event.request.getRequestUrl()
              const isMainFrame = event.request.isMainFrame()
              if (!isMainFrame) {
                return
              }
              const code = `${event.response.getResponseCode()}`
              const suffix = code ? `（http=${code}）` : ''
              this.showError(`页面加载失败：${failedUrl}${suffix}`)
            })
            .onLoadIntercept((event) => {
              const url = event && event.data ? event.data.getRequestUrl() : ''
              if (!url || isHttpLike(url)) {
                return false
              }
              this.openExternalUrl(url)
              return true
            })
            .onDownloadStart((_event) => {
              const sanitizedUrl = this.currentUrl.split('?')[0]
              const segments = sanitizedUrl.split('/')
              const lastSegment = segments.length > 0 ? segments[segments.length - 1] : ''
              const fileName = lastSegment ? lastSegment : 'download.bin'
              this.downloadBridge.startDownload(this.currentUrl, fileName)
            })
        } else {
          Column()
            .width('100%')
            .layoutWeight(1)
            .backgroundColor('#020617')
        }
      }
      .width('100%')
      .height('100%')
      .backgroundColor('#020617')

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

      if (this.errorVisible) {
        Column({ space: 12 }) {
          Text($r('app.string.web_error')).fontSize(20).fontWeight(FontWeight.Bold)
          Text(this.errorMessage).fontSize(13)
          Button($r('app.string.web_retry')).onClick(() => {
            this.clearError()
            this.pageLoading = true
            this.controller.refresh()
          })
          Button($r('app.string.web_switch_server')).onClick(() => {
            this.switcherVisible = true
            this.navVisible = true
          })
        }
        .padding(24)
        .margin({ top: 220, left: 20, right: 20 })
        .width('100%')
        .backgroundColor('#F8FAFC')
        .borderRadius(16)
        .zIndex(30)
      }

      if (this.switcherVisible) {
        Column() {
          Blank().layoutWeight(1)
          this.SwitcherSheet()
        }
        .width('100%')
        .height('100%')
        .backgroundColor('#66000000')
        .zIndex(40)
      }
    }
    .width('100%')
    .height('100%')
  }
}
