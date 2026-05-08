import dayjs from 'dayjs'

export function getCalendarMonthParams(source = new Date()) {
  const date = source?.date || source
  const monthDate = dayjs(date)

  return {
    year: monthDate.year(),
    month: monthDate.month() + 1
  }
}

export function getCalendarMonthKey(source = new Date()) {
  const { year, month } = getCalendarMonthParams(source)
  return `${year}-${String(month).padStart(2, '0')}`
}

export function collectDiaryDates(existingDates = [], diaries = []) {
  const dates = new Set(existingDates)

  diaries.forEach(diary => {
    if (diary?.diaryDate) {
      dates.add(String(diary.diaryDate).slice(0, 10))
    }
  })

  return Array.from(dates)
}
