export interface UrlValidationResult {
  valid: boolean
  normalized: string
  reason: string
}

const HTTP_PATTERN: RegExp = /^https?:\/\//i

export function validateHttpUrl(input: string): UrlValidationResult {
  const trimmed = input.trim()
  if (trimmed.length === 0) {
    return { valid: false, normalized: '', reason: 'URL 不能为空' }
  }
  if (!HTTP_PATTERN.test(trimmed)) {
    return { valid: false, normalized: '', reason: '仅支持 http/https 地址' }
  }
  const noSpace = trimmed.replace(/\s+/g, '')
  if (!/^https?:\/\/[A-Za-z0-9._:-]+/i.test(noSpace)) {
    return { valid: false, normalized: '', reason: 'URL 格式不正确' }
  }
  return { valid: true, normalized: noSpace, reason: '' }
}

export function isHttpLike(url: string): boolean {
  return HTTP_PATTERN.test(url.trim())
}
