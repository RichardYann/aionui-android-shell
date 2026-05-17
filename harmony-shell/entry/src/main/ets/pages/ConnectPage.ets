import { promptAction, router } from '@kit.ArkUI'
import { common } from '@kit.AbilityKit'
import { createServerEntry, ServerEntry, sortServers } from '../common/serverModel'
import { ShellStorage } from '../common/storage'
import { validateHttpUrl } from '../common/urlGuard'

@Entry
@Component
struct ConnectPage {
  @State private inputUrl: string = ''
  @State private servers: ServerEntry[] = []
  @State private loading: boolean = true
  @State private renameTargetId: string = ''
  @State private renameValue: string = ''

  private storage?: ShellStorage

  async aboutToAppear(): Promise<void> {
    const context = getContext(this) as common.UIAbilityContext
    this.storage = new ShellStorage(context)
    this.servers = await this.storage.getServers()
    const lastUrl = await this.storage.getLastUrl()
    const params = router.getParams() as Record<string, string>
    const skipAutoOpen = params && params.skipAutoOpen === '1'
    this.loading = false
    if (lastUrl && !skipAutoOpen) {
      router.replaceUrl({ url: 'pages/WebPage', params: { initialUrl: lastUrl } })
    }
  }

  private async saveAndOpen(): Promise<void> {
    if (!this.storage) {
      return
    }
    const checked = validateHttpUrl(this.inputUrl)
    if (!checked.valid) {
      promptAction.showToast({ message: checked.reason })
      return
    }

    const nextEntry = createServerEntry(checked.normalized)
    this.servers = await this.storage.upsertServer(nextEntry)
    await this.storage.setLastUrl(nextEntry.url)
    await this.storage.setLastServerId(nextEntry.id)
    router.replaceUrl({ url: 'pages/WebPage', params: { initialUrl: nextEntry.url } })
  }

  private async openServer(server: ServerEntry): Promise<void> {
    if (!this.storage) {
      return
    }
    const next: ServerEntry = {
      id: server.id,
      name: server.name,
      url: server.url,
      favorite: server.favorite,
      lastUsedAt: Date.now()
    }
    this.servers = await this.storage.updateServer(next)
    await this.storage.setLastUrl(server.url)
    await this.storage.setLastServerId(server.id)
    router.replaceUrl({ url: 'pages/WebPage', params: { initialUrl: server.url } })
  }

  private async toggleFavorite(server: ServerEntry): Promise<void> {
    if (!this.storage) {
      return
    }
    const next: ServerEntry = {
      id: server.id,
      name: server.name,
      url: server.url,
      favorite: !server.favorite,
      lastUsedAt: server.lastUsedAt
    }
    this.servers = await this.storage.updateServer(next)
  }

  private beginRename(server: ServerEntry): void {
    this.renameTargetId = server.id
    this.renameValue = server.name
  }

  private async commitRename(server: ServerEntry): Promise<void> {
    if (!this.storage) {
      return
    }
    const nextName = this.renameValue.trim()
    if (nextName.length === 0) {
      promptAction.showToast({ message: '名称不能为空' })
      return
    }
    const next: ServerEntry = {
      id: server.id,
      name: nextName,
      url: server.url,
      favorite: server.favorite,
      lastUsedAt: server.lastUsedAt
    }
    this.servers = await this.storage.updateServer(next)
    this.renameTargetId = ''
    this.renameValue = ''
  }

  private async removeServer(server: ServerEntry): Promise<void> {
    if (!this.storage) {
      return
    }
    this.servers = await this.storage.removeServer(server.id)
    const lastServerId = await this.storage.getLastServerId()
    if (lastServerId === server.id) {
      await this.storage.setLastServerId('')
      await this.storage.setLastUrl('')
    }
  }

  @Builder
  private ServerCard(server: ServerEntry) {
    Column({ space: 10 }) {
      if (this.renameTargetId === server.id) {
        TextInput({ text: this.renameValue, placeholder: '输入新名称' })
          .onChange((value: string) => this.renameValue = value)
          .width('100%')
        Row({ space: 8 }) {
          Button('保存').onClick(() => this.commitRename(server))
          Button('取消').onClick(() => {
            this.renameTargetId = ''
            this.renameValue = ''
          })
        }
      } else {
        Row() {
          Column({ space: 4 }) {
            Text(server.name).fontSize(18).fontWeight(FontWeight.Medium).fontColor(Color.White)
            Text(server.url).fontSize(12).fontColor('#C7D2FE')
          }
          .alignItems(HorizontalAlign.Start)
          .layoutWeight(1)
          if (server.favorite) {
            Text('★').fontColor('#F59E0B').fontSize(20)
          }
        }.width('100%')
      }

      Row({ space: 8 }) {
        Button('打开').onClick(() => this.openServer(server))
        Button(server.favorite ? '取消收藏' : '收藏').onClick(() => this.toggleFavorite(server))
        Button('重命名').onClick(() => this.beginRename(server))
        Button('删除').onClick(() => this.removeServer(server))
      }
      .width('100%')
      .justifyContent(FlexAlign.SpaceBetween)
    }
    .padding(16)
    .width('100%')
    .backgroundColor('#1E293B')
    .borderRadius(16)
  }

  build() {
    Scroll() {
      Column({ space: 18 }) {
        Column({ space: 8 }) {
          Text($r('app.string.connect_title'))
            .fontSize(28)
            .fontWeight(FontWeight.Bold)
            .fontColor(Color.White)
          Text('保存常用服务器，后续可自动恢复最近一次连接。')
            .fontSize(14)
            .fontColor('#CBD5E1')
        }
        .alignItems(HorizontalAlign.Start)
        .width('100%')

        TextInput({ text: this.inputUrl, placeholder: $r('app.string.connect_hint') })
          .width('100%')
          .onChange((value: string) => this.inputUrl = value)
          .backgroundColor('#FFFFFF')
          .borderRadius(14)

        Button($r('app.string.connect_action'))
          .width('100%')
          .height(48)
          .onClick(() => this.saveAndOpen())

        Text($r('app.string.connect_recent'))
          .width('100%')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .fontColor(Color.White)

        if (this.loading) {
          LoadingProgress().width(32).height(32)
        } else if (this.servers.length === 0) {
          Text('还没有已保存服务器').fontColor('#94A3B8')
        } else {
          ForEach(sortServers(this.servers), (server: ServerEntry) => {
            this.ServerCard(server)
          }, (server: ServerEntry) => server.id)
        }
      }
      .padding(20)
      .width('100%')
      .alignItems(HorizontalAlign.Start)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#0F172A')
  }
}
