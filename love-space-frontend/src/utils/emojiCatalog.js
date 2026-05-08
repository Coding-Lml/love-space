export const emojiGroups = [
  {
    key: 'frequent',
    label: '常用',
    items: [
      { value: '🥰', title: '甜甜' },
      { value: '😘', title: '亲亲' },
      { value: '💕', title: '爱心' },
      { value: '🥺', title: '想你' },
      { value: '😭', title: '委屈' },
      { value: '✨', title: '闪闪' }
    ]
  },
  {
    key: 'sweet',
    label: '甜蜜',
    items: [
      { value: '😍', title: '喜欢' },
      { value: '🤗', title: '抱抱' },
      { value: '💋', title: '啵啵' },
      { value: '💌', title: '情书' },
      { value: '🌹', title: '花花' },
      { value: '💍', title: '约定' }
    ]
  },
  {
    key: 'miss',
    label: '想你',
    items: [
      { value: '🌙', title: '月亮' },
      { value: '⭐', title: '星星' },
      { value: '🥹', title: '眼泪汪汪' },
      { value: '🫶', title: '比心' },
      { value: '😚', title: '贴贴' },
      { value: '😴', title: '梦里见' }
    ]
  },
  {
    key: 'daily',
    label: '日常',
    items: [
      { value: '☀️', title: '早安' },
      { value: '💤', title: '晚安' },
      { value: '🍚', title: '吃饭' },
      { value: '☕', title: '喝点' },
      { value: '📷', title: '拍照' },
      { value: '🎧', title: '听歌' }
    ]
  },
  {
    key: 'celebrate',
    label: '纪念',
    items: [
      { value: '🎂', title: '生日' },
      { value: '🎁', title: '礼物' },
      { value: '🎉', title: '庆祝' },
      { value: '🌈', title: '好天气' },
      { value: '🍰', title: '蛋糕' },
      { value: '🥂', title: '干杯' }
    ]
  }
]

export const flattenEmojiGroups = (groups = emojiGroups) => {
  return groups.flatMap(group => group.items.map(item => ({
    ...item,
    groupKey: group.key,
    groupLabel: group.label
  })))
}

export const appendToken = (text, token) => {
  const value = typeof token === 'string' ? token : token?.value
  if (!value) return text || ''
  return `${text || ''}${value}`
}
