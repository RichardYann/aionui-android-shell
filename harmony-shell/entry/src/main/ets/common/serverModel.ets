export interface ServerEntry {
  id: string
  name: string
  url: string
  favorite: boolean
  lastUsedAt: number
}

export function normalizeServerName(url: string): string {
  const trimmed = url.trim().replace(/^https?:\/\//i, '')
  const firstSlash = trimmed.indexOf('/')
  return firstSlash >= 0 ? trimmed.slice(0, firstSlash) : trimmed
}

export function createServerEntry(url: string, name?: string): ServerEntry {
  const now = Date.now()
  return {
    id: `${now}-${Math.floor(Math.random() * 100000)}`,
    name: (name && name.trim().length > 0) ? name.trim() : normalizeServerName(url),
    url: url.trim(),
    favorite: false,
    lastUsedAt: now
  }
}

export function sortServers(items: ServerEntry[]): ServerEntry[] {
  return items.slice().sort((left: ServerEntry, right: ServerEntry) => {
    if (left.favorite !== right.favorite) {
      return left.favorite ? -1 : 1
    }
    return right.lastUsedAt - left.lastUsedAt
  })
}
