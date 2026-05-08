import test from 'node:test'
import assert from 'node:assert/strict'
import {
  collectDiaryDates,
  getCalendarMonthParams
} from './diaryCalendar.js'

test('getCalendarMonthParams reads the visible calendar month', () => {
  assert.deepEqual(
    getCalendarMonthParams({ date: new Date(2026, 8, 12), title: '2026年9月' }),
    { year: 2026, month: 9 }
  )
})

test('collectDiaryDates keeps previous month markers and ignores malformed records', () => {
  assert.deepEqual(
    collectDiaryDates(['2026-05-01'], [
      { diaryDate: '2026-09-12' },
      { diaryDate: '2026-09-12' },
      { diaryDate: '' },
      {}
    ]),
    ['2026-05-01', '2026-09-12']
  )
})
