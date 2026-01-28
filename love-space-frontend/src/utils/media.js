export const toThumbUrl = (url, width = 480, quality = 80) => {
  if (!url) return url
  if (url.includes('x-oss-process=')) return url

  const hashIndex = url.indexOf('#')
  const hash = hashIndex >= 0 ? url.slice(hashIndex) : ''
  const base = hashIndex >= 0 ? url.slice(0, hashIndex) : url
  const joiner = base.includes('?') ? '&' : '?'
  const process = `x-oss-process=image/resize,w_${width}/quality,q_${quality}`
  return `${base}${joiner}${process}${hash}`
}

export const normalizeMediaUrl = (url) => {
  if (!url) return url
  if (typeof window === 'undefined') return url
  const origin = window.location?.origin
  if (!origin) return url

  const patterns = [
    'http://127.0.0.1',
    'http://localhost',
    'http://0.0.0.0',
    'https://127.0.0.1',
    'https://localhost',
    'https://0.0.0.0'
  ]
  for (const prefix of patterns) {
    if (url.startsWith(prefix)) {
      return origin + url.slice(prefix.length)
    }
  }
  return url
}
