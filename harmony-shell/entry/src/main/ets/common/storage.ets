import preferences from '@ohos.data.preferences'
import type common from '@ohos.app.ability.common'
import { ServerEntry, sortServers } from './serverModel'

const STORE_NAME: string = 'aionui_shell_store'
const KEY_SERVERS: string = 'servers_json'
const KEY_LAST_URL: string = 'last_url'
const KEY_LAST_SERVER_ID: string = 'last_server_id'

export class ShellStorage {
  private store: preferences.Preferences | null = null
  private context: common.UIAbilityContext

  constructor(context: common.UIAbilityContext) {
    this.context = context
  }

  private async ensureStore(): Promise<preferences.Preferences> {
    if (this.store !== null) {
      return this.store
    }
    this.store = await preferences.getPreferences(this.context, STORE_NAME)
    return this.store
  }

  async getServers(): Promise<ServerEntry[]> {
    const pref = await this.ensureStore()
    const raw = await pref.get(KEY_SERVERS, '') as string
    if (!raw || raw.length === 0) {
      return []
    }
    try {
      const parsed = JSON.parse(raw) as ServerEntry[]
      return sortServers(parsed)
    } catch (_error) {
      return []
    }
  }

  async saveServers(servers: ServerEntry[]): Promise<void> {
    const pref = await this.ensureStore()
    await pref.put(KEY_SERVERS, JSON.stringify(sortServers(servers)))
    await pref.flush()
  }

  async upsertServer(entry: ServerEntry): Promise<ServerEntry[]> {
    const current = await this.getServers()
    const next: ServerEntry[] = [entry]
    current.forEach((item: ServerEntry) => {
      if (item.id !== entry.id && item.url !== entry.url) {
        next.push(item)
      }
    })
    await this.saveServers(next)
    return sortServers(next)
  }

  async updateServer(entry: ServerEntry): Promise<ServerEntry[]> {
    const current = await this.getServers()
    const next = current.map((item: ServerEntry) => item.id === entry.id ? entry : item)
    await this.saveServers(next)
    return sortServers(next)
  }

  async removeServer(id: string): Promise<ServerEntry[]> {
    const current = await this.getServers()
    const next = current.filter((item: ServerEntry) => item.id !== id)
    await this.saveServers(next)
    return sortServers(next)
  }

  async getLastUrl(): Promise<string> {
    const pref = await this.ensureStore()
    return (await pref.get(KEY_LAST_URL, '') as string).trim()
  }

  async setLastUrl(url: string): Promise<void> {
    const pref = await this.ensureStore()
    await pref.put(KEY_LAST_URL, url)
    await pref.flush()
  }

  async getLastServerId(): Promise<string> {
    const pref = await this.ensureStore()
    return (await pref.get(KEY_LAST_SERVER_ID, '') as string).trim()
  }

  async setLastServerId(id: string): Promise<void> {
    const pref = await this.ensureStore()
    await pref.put(KEY_LAST_SERVER_ID, id)
    await pref.flush()
  }
}
