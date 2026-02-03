export const toThumbUrl = (url, width = 480, quality = 80) => {
  if (!url) return url
  // 如果已经是处理过的 URL，直接返回
  if (url.includes('x-oss-process=')) return url
  
  // 检查是否是 OSS/CDN 域名（避免给非 OSS 图片加参数导致 404）
  // 这里假设你的域名是 lzsweetie.fun 或 aliyuncs.com 等
  // 如果不确定，可以简单判断是否包含 http
  
  const hashIndex = url.indexOf('#')
  const hash = hashIndex >= 0 ? url.slice(hashIndex) : ''
  const base = hashIndex >= 0 ? url.slice(0, hashIndex) : url
  const joiner = base.includes('?') ? '&' : '?'
  
  // 阿里云 OSS 图片处理参数
  // 缩略图模式：固定宽，高度自适应，质量 80%
  const process = `x-oss-process=image/resize,w_${width}/quality,q_${quality}`
  return `${base}${joiner}${process}${hash}`
}

export const toPreviewUrl = (url) => {
  if (!url) return url
  if (url.includes('x-oss-process=')) return url

  const hashIndex = url.indexOf('#')
  const hash = hashIndex >= 0 ? url.slice(hashIndex) : ''
  const base = hashIndex >= 0 ? url.slice(0, hashIndex) : url
  const joiner = base.includes('?') ? '&' : '?'
  
  // 预览大图模式：限制最大宽度 1920，保留质量 85%（防止原图太大直接卡死）
  // 增加 interlace,1 实现渐进式加载
  const process = `x-oss-process=image/resize,w_1920,limit_1/quality,q_85/interlace,1`
  return `${base}${joiner}${process}${hash}`
}

export const normalizeMediaUrl = (url) => {
  if (!url) return url
  if (typeof window === 'undefined') return url
  
  // 1. 如果已经是完整链接（http/https开头），且不是本地调试地址，直接返回
  // 这样可以避免把 cdn.lzsweetie.fun 误替换成 www.lzsweetie.fun
  if (url.startsWith('http') && 
      !url.includes('localhost') && 
      !url.includes('127.0.0.1') && 
      !url.includes('0.0.0.0')) {
    return url
  }
  
  // 2. 如果是相对路径（/uploads/xxx），补全 CDN 域名（如果有配置）
  // 假设你的 CDN 域名是 www.lzsweetie.fun 或 cdn.lzsweetie.fun
  // 即使当前访问的是 lzsweetie.fun，我们也强制让图片走 www 域名（因为 www 配了 CDN）
  if (url.startsWith('/')) {
    const origin = window.location.origin
    // 如果当前已经在 www 域名下，直接拼接
    if (origin.includes('www.lzsweetie.fun')) {
      return origin + url
    }
    // 否则强制指向 www.lzsweetie.fun 以利用 CDN 缓存
    return 'https://www.lzsweetie.fun' + url
  }

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
      // 替换掉本地前缀，并强制指向 www 域名
      const path = url.slice(prefix.length)
      return 'https://www.lzsweetie.fun' + path
    }
  }
  return url
}
