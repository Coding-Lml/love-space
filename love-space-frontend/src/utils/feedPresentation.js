const ACCENTS = ['#ff7a59', '#f0528d', '#10a7a1', '#7c5cff', '#f6a700']

export const describeMomentMedia = (mediaList) => {
  if (!Array.isArray(mediaList) || mediaList.length === 0) return ''
  const imageCount = mediaList.filter(item => item?.type === 'image').length
  const videoCount = mediaList.filter(item => item?.type === 'video').length
  const parts = []
  if (imageCount) parts.push(`${imageCount} 张照片`)
  if (videoCount) parts.push(`${videoCount} 段视频`)
  return parts.join(' · ')
}

export const getMomentAccent = (moment) => {
  const id = Number(moment?.id || 0)
  const index = Math.abs(Math.max(id, 1) - 1) % ACCENTS.length
  return ACCENTS[index]
}

export const visibilityLabel = (visibility) => {
  return visibility === 'PUBLIC' ? '广场可见' : '只在我们之间'
}
