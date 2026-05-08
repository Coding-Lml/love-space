<template>
  <div class="diary-page">
    <van-nav-bar title="情侣日记">
      <template #right>
        <van-icon :name="viewMode === 'calendar' ? 'notes-o' : 'calendar-o'" size="20" @click="toggleView" />
      </template>
    </van-nav-bar>

    <section class="diary-hero">
      <div>
        <div class="diary-kicker">PRIVATE NOTES</div>
        <h1>{{ viewMode === 'calendar' ? '按日期找回一天' : '把心情写成记忆' }}</h1>
        <p>{{ diaryStats }}</p>
      </div>
      <button type="button" class="hero-write" @click="goWrite">
        <van-icon name="edit" />
        <span>写</span>
      </button>
    </section>

    <div class="mode-switch">
      <button type="button" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">
        列表
      </button>
      <button type="button" :class="{ active: viewMode === 'calendar' }" @click="switchCalendar">
        日历
      </button>
    </div>

    <div v-if="viewMode === 'calendar'" class="calendar-view">
      <van-calendar
        :poppable="false"
        :show-confirm="false"
        :style="{ height: '400px' }"
        :min-date="minDate"
        :max-date="maxDate"
        :formatter="formatter"
        @month-show="onMonthShow"
        @select="onDateSelect"
      />

      <van-skeleton v-if="calendarLoading" title :row="2" class="diary-skeleton" />

      <article class="selected-diary" v-else-if="selectedDiary">
        <div class="selected-date">
          <strong>{{ formatDiaryDateParts(selectedDiary.diaryDate).day }}</strong>
          <span>{{ formatDiaryDateParts(selectedDiary.diaryDate).month }}</span>
        </div>
        <div class="selected-content">
          <div class="mood-tag">
            {{ getDiaryMood(selectedDiary.mood).emoji }} {{ getDiaryMood(selectedDiary.mood).text }}
          </div>
          <h2>{{ selectedDiary.title || summarizeDiaryText(selectedDiary, 18) }}</h2>
          <p>{{ selectedDiary.content }}</p>
          <small>{{ selectedDiary.user?.nickname || '我们' }}</small>
        </div>
      </article>

      <div class="calendar-empty" v-else-if="selectedDate">
        <van-icon name="notes-o" />
        <h2>{{ selectedDate }} 还没有日记</h2>
        <p>可以把这一天补写下来，之后日历上就会亮起来。</p>
        <van-button type="primary" round size="small" @click="goWrite">写这一天</van-button>
      </div>

      <div class="calendar-empty" v-else>
        <van-icon name="calendar-o" />
        <h2>选择一个日期</h2>
        <p>有记录的日子会带着标记，点开就能看到当天的心情。</p>
      </div>
    </div>

    <div v-else class="list-view">
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-skeleton
          v-if="loading && pageNum === 1 && !diaries.length"
          title
          :row="4"
          class="diary-skeleton"
        />
        <div v-else-if="loadError && !diaries.length" class="list-error">
          <van-icon name="warning-o" />
          <h2>日记加载失败</h2>
          <p>网络可能开了小差，重试一下。</p>
          <van-button type="primary" round size="small" @click="retryList">重试</van-button>
        </div>
        <van-list
          v-else
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="loadMore"
        >
          <article v-for="diary in diaries" :key="diary.id" class="diary-card">
            <div class="diary-date">
              <strong>{{ formatDiaryDateParts(diary.diaryDate).day }}</strong>
              <span>{{ formatDiaryDateParts(diary.diaryDate).month }}</span>
            </div>
            <div class="diary-main">
              <div class="diary-meta">
                <span class="mood-tag">{{ getDiaryMood(diary.mood).emoji }} {{ getDiaryMood(diary.mood).text }}</span>
                <span>{{ diary.user?.nickname || '我们' }}</span>
                <van-icon v-if="diary.visibility === 'self'" name="lock" size="12" color="#9a909e" />
              </div>
              <h2>{{ diary.title || '没有标题的一天' }}</h2>
              <p>{{ summarizeDiaryText(diary, 74) }}</p>
            </div>
          </article>

          <div v-if="!loading && !diaries.length" class="list-empty">
            <van-icon name="edit" />
            <h2>还没有日记</h2>
            <p>第一篇可以很短，只要它是真的。</p>
            <van-button type="primary" round size="small" @click="goWrite">写第一篇</van-button>
          </div>
        </van-list>
      </van-pull-refresh>
    </div>

    <button type="button" class="write-btn" @click="goWrite">
      <van-icon name="edit" />
    </button>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import api from '../api'
import dayjs from 'dayjs'
import {
  collectDiaryDates,
  getCalendarMonthKey,
  getCalendarMonthParams
} from '../utils/diaryCalendar'
import {
  formatDiaryDateParts,
  getDiaryMood,
  summarizeDiaryText
} from '../utils/memoryPresentation'

const router = useRouter()

const viewMode = ref('list')
const diaries = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const loadError = ref(false)

const selectedDate = ref('')
const selectedDiary = ref(null)
const diaryDates = ref([])
const loadedMonths = new Set()
const loadingMonths = new Set()
const calendarLoading = ref(false)

const minDate = new Date(2026, 0, 1)
const maxDate = new Date()

const diaryStats = computed(() => {
  if (viewMode.value === 'calendar') return '用日历把分散的心情重新串起来'
  if (!diaries.value.length) return '日记会保存那些不一定适合发成动态的话'
  const privateCount = diaries.value.filter(item => item.visibility === 'self').length
  return `${diaries.value.length} 篇日记 · ${privateCount} 篇仅自己可见`
})

const mergeUniqueById = (existing, incoming) => {
  const map = new Map()
  for (const item of existing || []) {
    if (item?.id != null) map.set(item.id, item)
  }
  for (const item of incoming || []) {
    if (item?.id != null) map.set(item.id, item)
  }
  return Array.from(map.values())
}

const loadMore = async () => {
  if (loading.value) return
  loading.value = true
  loadError.value = false
  try {
    const res = await api.diary.getList(pageNum.value)
    if (res.code === 200) {
      const records = res.data?.records || []
      diaries.value = pageNum.value === 1 ? records : mergeUniqueById(diaries.value, records)
      finished.value = records.length < 10
      pageNum.value += 1
    } else {
      loadError.value = true
      showToast(res.message || '加载失败')
    }
  } catch (e) {
    loadError.value = true
    showToast('日记加载失败')
  } finally {
    loading.value = false
  }
}

const retryList = () => {
  pageNum.value = 1
  finished.value = false
  loadMore()
}

const onRefresh = async () => {
  pageNum.value = 1
  finished.value = false
  await loadMore()
  refreshing.value = false
}

const toggleView = () => {
  if (viewMode.value === 'calendar') {
    viewMode.value = 'list'
  } else {
    switchCalendar()
  }
}

const switchCalendar = () => {
  viewMode.value = 'calendar'
  loadMonthDiaries(new Date())
}

const loadMonthDiaries = async (source = new Date()) => {
  const monthKey = getCalendarMonthKey(source)
  if (loadedMonths.has(monthKey) || loadingMonths.has(monthKey)) {
    return
  }

  const { year, month } = getCalendarMonthParams(source)
  loadingMonths.add(monthKey)
  calendarLoading.value = true

  try {
    const res = await api.diary.getByMonth(year, month)
    if (res.code === 200) {
      diaryDates.value = collectDiaryDates(diaryDates.value, res.data)
      loadedMonths.add(monthKey)
    }
  } catch (e) {
    showToast('月度日记加载失败')
  } finally {
    loadingMonths.delete(monthKey)
    calendarLoading.value = loadingMonths.size > 0
  }
}

const onMonthShow = (month) => {
  loadMonthDiaries(month)
}

const formatter = (day) => {
  const dateStr = dayjs(day.date).format('YYYY-MM-DD')
  if (diaryDates.value.includes(dateStr)) {
    day.bottomInfo = '💕'
  }
  return day
}

const onDateSelect = async (date) => {
  const dateStr = dayjs(date).format('YYYY-MM-DD')
  selectedDate.value = dateStr
  calendarLoading.value = true
  try {
    const res = await api.diary.getByDate(dateStr)
    selectedDiary.value = res.code === 200 ? res.data : null
  } catch (e) {
    selectedDiary.value = null
  } finally {
    calendarLoading.value = false
  }
}

const goWrite = () => {
  router.push({
    name: 'diaryWrite',
    query: selectedDate.value ? { date: selectedDate.value } : {}
  })
}
</script>

<style scoped>
.diary-page {
  min-height: 100vh;
  padding-bottom: 88px;
}

.diary-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  margin: 14px 12px 10px;
  padding: 18px;
  color: #fff;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(40, 35, 47, 0.94), rgba(124, 92, 255, 0.74)),
    #28232f;
  box-shadow: var(--shadow);
}

.diary-kicker,
.section-kicker {
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 1.6px;
  opacity: 0.72;
}

.diary-hero h1 {
  max-width: 230px;
  margin: 6px 0;
  font-size: 24px;
  line-height: 1.12;
  letter-spacing: 0;
}

.diary-hero p {
  max-width: 240px;
  font-size: 12px;
  line-height: 1.5;
  opacity: 0.82;
}

.hero-write {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 36px;
  padding: 0 12px;
  color: #5b4a7a;
  font-weight: 900;
  border: 0;
  border-radius: 8px;
  background: #fff;
}

.mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  margin: 0 12px 12px;
  padding: 4px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: var(--shadow);
}

.mode-switch button {
  height: 34px;
  font-weight: 800;
  color: var(--text-light);
  border: 0;
  border-radius: 8px;
  background: transparent;
}

.mode-switch button.active {
  color: var(--primary-color);
  background: var(--surface-soft);
}

.calendar-view,
.list-view {
  padding: 0 12px 20px;
}

:deep(.van-calendar) {
  overflow: hidden;
  border-radius: 8px;
  box-shadow: var(--shadow);
}

.diary-skeleton,
.selected-diary,
.calendar-empty,
.list-error,
.list-empty,
.diary-card {
  border: 1px solid rgba(255, 122, 89, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--shadow);
}

.diary-skeleton {
  margin-top: 12px;
  padding: 16px;
}

.selected-diary {
  display: flex;
  gap: 13px;
  margin-top: 12px;
  padding: 14px;
}

.selected-date,
.diary-date {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 54px;
  min-height: 58px;
  border-radius: 8px;
  color: var(--primary-color);
  background: var(--surface-soft);
}

.selected-date strong,
.diary-date strong {
  font-size: 27px;
  line-height: 1;
}

.selected-date span,
.diary-date span {
  font-size: 12px;
  color: var(--text-light);
}

.selected-content {
  min-width: 0;
}

.selected-content h2,
.diary-main h2,
.calendar-empty h2,
.list-error h2,
.list-empty h2 {
  margin: 8px 0 5px;
  font-size: 18px;
  line-height: 1.25;
  letter-spacing: 0;
  color: var(--text-color);
}

.selected-content p,
.diary-main p,
.calendar-empty p,
.list-error p,
.list-empty p {
  font-size: 13px;
  line-height: 1.55;
  color: var(--text-light);
}

.selected-content small {
  display: inline-block;
  margin-top: 8px;
  color: var(--text-lighter);
}

.calendar-empty,
.list-error,
.list-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 12px;
  padding: 28px 18px;
  text-align: center;
}

.calendar-empty .van-icon,
.list-error .van-icon,
.list-empty .van-icon {
  font-size: 34px;
  color: var(--primary-color);
}

.calendar-empty .van-button,
.list-error .van-button,
.list-empty .van-button {
  margin-top: 14px;
}

.diary-card {
  display: flex;
  gap: 13px;
  margin-bottom: 12px;
  padding: 14px;
}

.diary-main {
  flex: 1;
  min-width: 0;
}

.diary-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  color: var(--text-lighter);
  font-size: 12px;
}

.mood-tag {
  padding: 4px 9px;
  font-weight: 800;
  background: var(--surface-soft);
}

.write-btn {
  position: fixed;
  right: 18px;
  bottom: 78px;
  z-index: 10;
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  color: #fff;
  border: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-warm), var(--primary-color));
  box-shadow: var(--shadow-strong);
}

.write-btn .van-icon {
  font-size: 24px;
}

@media (min-width: 768px) {
  .diary-page {
    max-width: 760px;
    margin: 0 auto;
  }
}
</style>
