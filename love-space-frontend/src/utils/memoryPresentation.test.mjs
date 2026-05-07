import assert from 'node:assert/strict'
import { describe, it } from 'node:test'

import {
  formatAnniversaryDistance,
  formatDiaryDateParts,
  getDiaryMood,
  selectNextAnniversary,
  selectRecentDiaries,
  splitAnniversaries,
  summarizeDiaryText
} from './memoryPresentation.js'

describe('memoryPresentation', () => {
  it('formats diary mood and date parts for memory cards', () => {
    assert.deepEqual(getDiaryMood('love'), { emoji: '🥰', text: '甜蜜' })
    assert.deepEqual(getDiaryMood('unknown'), { emoji: '📝', text: '日记' })
    assert.deepEqual(formatDiaryDateParts('2026-05-08'), {
      day: '08',
      month: '5月',
      date: '2026-05-08'
    })
  })

  it('summarizes diary text from title or content predictably', () => {
    assert.equal(summarizeDiaryText({ title: '晚风很好' }, 12), '晚风很好')
    assert.equal(summarizeDiaryText({ content: '  今天一起吃了很好吃的面还一起散步  ' }, 12), '今天一起吃了很好吃...')
    assert.equal(summarizeDiaryText({}, 12), '还没有写下内容')
  })

  it('selects recent diaries with stable newest-first ordering', () => {
    const diaries = selectRecentDiaries([
      { id: 1, diaryDate: '2026-05-01' },
      { id: 2, diaryDate: '2026-05-08' },
      { id: 3, diaryDate: '2026-05-03' }
    ], 2)

    assert.deepEqual(diaries.map(item => item.id), [2, 3])
  })

  it('splits anniversaries and sorts future dates before past memories', () => {
    const grouped = splitAnniversaries([
      { id: 1, type: 'future', days: 20 },
      { id: 2, type: 'past', days: 500 },
      { id: 3, type: 'future', days: 3 },
      { id: 4, type: 'past', days: 30 }
    ])

    assert.deepEqual(grouped.future.map(item => item.id), [3, 1])
    assert.deepEqual(grouped.past.map(item => item.id), [2, 4])
  })

  it('selects and labels the next anniversary', () => {
    const next = selectNextAnniversary([
      { id: 1, type: 'future', title: '生日', days: 12 },
      { id: 2, type: 'future', title: '旅行', days: 2 }
    ])

    assert.equal(next.id, 2)
    assert.equal(formatAnniversaryDistance(next), '还有 2 天')
    assert.equal(formatAnniversaryDistance({ type: 'past', days: 520 }), '已经 520 天')
    assert.equal(formatAnniversaryDistance({ daysText: '今天' }), '今天')
  })
})
