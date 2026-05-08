import dayjs from 'dayjs'

const DIARY_MOODS = {
  happy: { emoji: '😊', text: '开心' },
  love: { emoji: '🥰', text: '甜蜜' },
  normal: { emoji: '😐', text: '平静' },
  sad: { emoji: '😢', text: '难过' },
  angry: { emoji: '😤', text: '生气' }
}

const DEFAULT_DIARY_MOOD = { emoji: '📝', text: '日记' }

const numericDays = (item) => {
  const days = Number(item?.days)
  return Number.isFinite(days) ? days : Number.MAX_SAFE_INTEGER
}

export const getDiaryMood = (mood) => DIARY_MOODS[mood] || DEFAULT_DIARY_MOOD

export const formatDiaryDateParts = (date) => {
  const value = dayjs(date)
  if (!value.isValid()) {
    return { day: '--', month: '--', date: '' }
  }
  return {
    day: value.format('DD'),
    month: `${value.format('M')}月`,
    date: value.format('YYYY-MM-DD')
  }
}

export const summarizeDiaryText = (diary, maxLength = 48) => {
  const source = (diary?.title || diary?.content || '').trim()
  if (!source) return '还没有写下内容'
  if (source.length <= maxLength) return source
  return `${source.slice(0, Math.max(0, maxLength - 3))}...`
}

export const selectRecentDiaries = (diaries, limit = 2) => {
  if (!Array.isArray(diaries)) return []
  return [...diaries]
    .filter(item => item?.id != null)
    .sort((a, b) => dayjs(b.diaryDate || b.createdAt || 0).valueOf() - dayjs(a.diaryDate || a.createdAt || 0).valueOf())
    .slice(0, limit)
}

export const splitAnniversaries = (anniversaries) => {
  const items = Array.isArray(anniversaries) ? anniversaries : []
  return {
    future: items
      .filter(item => item?.type === 'future')
      .sort((a, b) => numericDays(a) - numericDays(b)),
    past: items
      .filter(item => item?.type !== 'future')
      .sort((a, b) => numericDays(b) - numericDays(a))
  }
}

export const selectNextAnniversary = (anniversaries) => splitAnniversaries(anniversaries).future[0] || null

export const formatAnniversaryDistance = (item) => {
  if (!item) return ''
  if (item.daysText) return item.daysText
  const days = Number(item.days)
  if (!Number.isFinite(days)) return ''
  if (item.type === 'future') return days === 0 ? '就是今天' : `还有 ${days} 天`
  return `已经 ${days} 天`
}
