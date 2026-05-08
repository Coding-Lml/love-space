<template>
  <div class="diary-page">
    <van-nav-bar title="情侣日记">
      <template #right>
        <van-icon name="notes-o" size="20" @click="toggleView" />
      </template>
    </van-nav-bar>
    
    <!-- 日历视图 -->
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
      
      <!-- 当天日记 -->
      <div class="selected-diary card" v-if="selectedDiary">
        <div class="diary-header">
          <span class="mood-tag">{{ getMoodEmoji(selectedDiary.mood) }} {{ getMoodText(selectedDiary.mood) }}</span>
          <span class="diary-author">{{ selectedDiary.user?.nickname }}</span>
        </div>
        <div class="diary-title" v-if="selectedDiary.title">{{ selectedDiary.title }}</div>
        <div class="diary-content">{{ selectedDiary.content }}</div>
      </div>
      
      <div class="no-diary" v-else-if="selectedDate">
        <p>{{ selectedDate }} 还没有日记</p>
        <van-button type="primary" size="small" round @click="goWrite">写日记</van-button>
      </div>
    </div>
    
    <!-- 列表视图 -->
    <div v-else class="list-view">
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="loadMore"
        >
          <div v-for="diary in diaries" :key="diary.id" class="diary-card card">
            <div class="diary-date">
              <span class="day">{{ getDay(diary.diaryDate) }}</span>
              <span class="month">{{ getMonth(diary.diaryDate) }}月</span>
            </div>
            <div class="diary-main">
              <div class="diary-header">
                <span class="mood-tag">{{ getMoodEmoji(diary.mood) }}</span>
                <span class="diary-author">{{ diary.user?.nickname }}</span>
                <van-icon 
                  v-if="diary.visibility === 'self'" 
                  name="lock" 
                  size="12" 
                  color="#999" 
                />
              </div>
              <div class="diary-title" v-if="diary.title">{{ diary.title }}</div>
              <div class="diary-content">{{ diary.content?.substring(0, 100) }}{{ diary.content?.length > 100 ? '...' : '' }}</div>
            </div>
          </div>
          
          <van-empty v-if="!loading && !diaries.length" description="还没有日记，快来写一篇吧~" />
        </van-list>
      </van-pull-refresh>
    </div>
    
    <!-- 写日记按钮 -->
    <div class="write-btn" @click="goWrite">
      <van-icon name="edit" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import dayjs from 'dayjs'
import {
  collectDiaryDates,
  getCalendarMonthKey,
  getCalendarMonthParams
} from '../utils/diaryCalendar'

const router = useRouter()

const viewMode = ref('list')
const diaries = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)

const selectedDate = ref('')
const selectedDiary = ref(null)
const diaryDates = ref([])
const loadedMonths = new Set()
const loadingMonths = new Set()

const minDate = new Date(2026, 0, 1)
const maxDate = new Date()

// 心情配置
const moodConfig = {
  happy: { emoji: '😊', text: '开心' },
  sad: { emoji: '😢', text: '难过' },
  love: { emoji: '🥰', text: '甜蜜' },
  angry: { emoji: '😤', text: '生气' },
  normal: { emoji: '😐', text: '平静' }
}

const getMoodEmoji = (mood) => moodConfig[mood]?.emoji || '📝'
const getMoodText = (mood) => moodConfig[mood]?.text || '日记'

const getDay = (date) => dayjs(date).format('DD')
const getMonth = (date) => dayjs(date).format('M')

// 加载列表
const loadMore = async () => {
  try {
    const res = await api.diary.getList(pageNum.value)
    if (res.code === 200) {
      if (pageNum.value === 1) {
        diaries.value = res.data.records
      } else {
        diaries.value.push(...res.data.records)
      }
      finished.value = res.data.records.length < 10
      pageNum.value++
    }
  } catch (e) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
}

// 下拉刷新
const onRefresh = async () => {
  pageNum.value = 1
  finished.value = false
  await loadMore()
  refreshing.value = false
}

// 切换视图
const toggleView = () => {
  viewMode.value = viewMode.value === 'calendar' ? 'list' : 'calendar'
  if (viewMode.value === 'calendar') {
    loadMonthDiaries(new Date())
  }
}

// 加载当月日记
const loadMonthDiaries = async (source = new Date()) => {
  const monthKey = getCalendarMonthKey(source)
  if (loadedMonths.has(monthKey) || loadingMonths.has(monthKey)) {
    return
  }

  const { year, month } = getCalendarMonthParams(source)
  loadingMonths.add(monthKey)

  try {
    const res = await api.diary.getByMonth(year, month)
    if (res.code === 200) {
      diaryDates.value = collectDiaryDates(diaryDates.value, res.data)
      loadedMonths.add(monthKey)
    }
  } catch (e) {
    console.error('加载月度日记失败', e)
  } finally {
    loadingMonths.delete(monthKey)
  }
}

const onMonthShow = (month) => {
  loadMonthDiaries(month)
}

// 日历格式化
const formatter = (day) => {
  const dateStr = dayjs(day.date).format('YYYY-MM-DD')
  if (diaryDates.value.includes(dateStr)) {
    day.bottomInfo = '💕'
  }
  return day
}

// 选择日期
const onDateSelect = async (date) => {
  const dateStr = dayjs(date).format('YYYY-MM-DD')
  selectedDate.value = dateStr
  
  try {
    const res = await api.diary.getByDate(dateStr)
    if (res.code === 200) {
      selectedDiary.value = res.data
    }
  } catch (e) {
    selectedDiary.value = null
  }
}

// 写日记
const goWrite = () => {
  router.push({ 
    name: 'diaryWrite',
    query: selectedDate.value ? { date: selectedDate.value } : {}
  })
}

onMounted(() => {
  if (viewMode.value === 'list') {
    // 列表会自动触发 loadMore
  }
})
</script>

<style scoped>
.diary-page {
  min-height: 100vh;
  padding-bottom: 70px;
}

.calendar-view {
  padding: 12px;
}

:deep(.van-calendar) {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--shadow);
}

.selected-diary, .no-diary {
  margin-top: 16px;
  text-align: center;
}

.no-diary {
  padding: 24px;
  color: var(--text-lighter);
}

.no-diary p {
  margin-bottom: 12px;
}

.list-view {
  padding-bottom: 20px;
}

.diary-card {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.diary-date {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 50px;
}

.diary-date .day {
  font-size: 28px;
  font-weight: 700;
  color: var(--primary-color);
  line-height: 1;
}

.diary-date .month {
  font-size: 12px;
  color: var(--text-lighter);
  margin-top: 4px;
}

.diary-main {
  flex: 1;
  min-width: 0;
}

.diary-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.diary-author {
  font-size: 13px;
  color: var(--text-light);
}

.diary-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 8px;
}

.diary-content {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-light);
}

.write-btn {
  position: fixed;
  right: 20px;
  bottom: 80px;
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #ff6b81 0%, #e84a5f 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  box-shadow: 0 4px 12px rgba(255, 107, 129, 0.4);
}
</style>
