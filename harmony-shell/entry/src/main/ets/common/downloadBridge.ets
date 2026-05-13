import { promptAction } from '@kit.ArkUI'

export class DownloadBridge {
  async startDownload(url: string, fileName: string): Promise<void> {
    console.info(`Download requested: ${fileName} -> ${url}`)
    promptAction.showToast({ message: `开始下载：${fileName}` })
  }

  async fail(message: string): Promise<void> {
    promptAction.showToast({ message })
  }
}
