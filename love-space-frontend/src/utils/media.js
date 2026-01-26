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

