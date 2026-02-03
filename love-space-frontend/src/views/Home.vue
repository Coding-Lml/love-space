<template>
  <div class="home-page">
    <van-nav-bar title="Love Space" left-text="" right-text="">
      <template #right>
        <van-icon name="setting-o" size="20" color="#ff6b81" @click="goProfile" />
      </template>
    </van-nav-bar>
    
    <!-- 恋爱计时器 -->
    <div class="love-timer card">
      <div class="couple-avatars">
        <img :src="userStore.user?.avatar || '/default-avatar.png'" class="avatar avatar-large" fetchpriority="high" decoding="async" />
        <span class="heart-icon heartbeat">❤️</span>
        <img :src="userStore.partner?.avatar || '/default-avatar.png'" class="avatar avatar-large" fetchpriority="high" decoding="async" />
      </div>
      <div class="couple-names">
        <span>{{ userStore.user?.nickname }}</span>
        <span class="and">&</span>
        <span>{{ userStore.partner?.nickname }}</span>
      </div>
      <div class="timer-display" v-if="dashboard">
        <div class="days-count">
          <span class="highlight-number">{{ dashboard.togetherDays }}</span>
          <span class="days-label">天</span>
        </div>
        <div class="time-detail">
          {{ dashboard.togetherHours }}小时 {{ dashboard.togetherMinutes }}分钟 {{ seconds }}秒
        </div>
        <div class="start-date">
          从 {{ dashboard.startDate }} 开始 💕
        </div>
      </div>
    </div>
    
    <!-- 即将到来的纪念日 -->
    <div class="upcoming-section card" v-if="dashboard?.upcomingAnniversaries?.length">
      <div class="section-title">
        <span>📅</span>
        <span>即将到来</span>
      </div>
      <div class="upcoming-list">
        <div 
          v-for="item in dashboard.upcomingAnniversaries" 
          :key="item.id" 
          class="upcoming-item"
        >
          <span class="icon">{{ item.icon || '💕' }}</span>
          <span class="title">{{ item.title }}</span>
          <span class="days">{{ item.daysText }}</span>
        </div>
      </div>
    </div>
    
    <!-- 最近动态 -->
    <div class="recent-section card" v-if="dashboard?.recentMoments?.length">
      <div class="section-title">
        <span>📝</span>
        <span>最近动态</span>
        <span class="more" @click="goMoments">查看全部 ›</span>
      </div>
      <div class="recent-moments">
        <div 
          v-for="moment in dashboard.recentMoments.slice(0, 3)" 
          :key="moment.id" 
          class="moment-item"
        >
          <img :src="moment.user?.avatar" class="avatar" loading="lazy" decoding="async" />
          <div class="moment-content">
            <div class="moment-text">{{ moment.content?.substring(0, 50) }}{{ moment.content?.length > 50 ? '...' : '' }}</div>
            <div class="moment-time">{{ formatTime(moment.createdAt) }}</div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div class="empty-hint" v-if="!loading && (!dashboard?.recentMoments?.length)">
      <p>还没有动态，快去记录美好时光吧 💕</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import api from '../api'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const dashboard = ref(null)
const seconds = ref(0)
let timer = null

// 获取仪表盘数据
const fetchDashboard = async () => {
  loading.value = true
  try {
    const res = await api.dashboard.getData()
    if (res.code === 200) {
      dashboard.value = res.data
      seconds.value = res.data.togetherSeconds || 0
    }
  } catch (e) {
    console.error('获取仪表盘数据失败', e)
  } finally {
    loading.value = false
  }
}

// 秒数计时
const startTimer = () => {
  timer = setInterval(() => {
    seconds.value++
    if (seconds.value >= 60) {
      seconds.value = 0
      // 刷新数据
      fetchDashboard()
    }
  }, 1000)
}

// 格式化时间
const formatTime = (time) => {
  return dayjs(time).format('MM-DD HH:mm')
}

// 跳转
const goProfile = () => router.push({ name: 'profile' })
const goMoments = () => router.push({ name: 'moments' })

onMounted(() => {
  fetchDashboard()
  userStore.fetchPartner()
  startTimer()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  padding-bottom: 70px;
}

.love-timer {
  text-align: center;
  padding: 24px;
  margin-top: 12px;
  background: linear-gradient(135deg, #fff 0%, #fff5f5 100%);
}

.couple-avatars {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 12px;
}

.heart-icon {
  font-size: 28px;
}

.couple-names {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 20px;
}

.couple-names .and {
  margin: 0 8px;
  color: var(--primary-color);
}

.timer-display {
  margin-top: 16px;
}

.days-count {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
}

.days-label {
  font-size: 18px;
  color: var(--text-light);
}

.time-detail {
  font-size: 14px;
  color: var(--text-light);
  margin-top: 8px;
}

.start-date {
  font-size: 13px;
  color: var(--text-lighter);
  margin-top: 12px;
}

.upcoming-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upcoming-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-color);
  border-radius: 12px;
}

.upcoming-item .icon {
  font-size: 24px;
}

.upcoming-item .title {
  flex: 1;
  font-size: 15px;
  color: var(--text-color);
}

.upcoming-item .days {
  font-size: 14px;
  color: var(--primary-color);
  font-weight: 500;
}

.section-title .more {
  margin-left: auto;
  font-size: 13px;
  color: var(--primary-color);
  font-weight: normal;
}

.recent-moments {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.moment-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: var(--bg-color);
  border-radius: 12px;
}

.moment-content {
  flex: 1;
  min-width: 0;
}

.moment-text {
  font-size: 14px;
  color: var(--text-color);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.moment-time {
  font-size: 12px;
  color: var(--text-lighter);
  margin-top: 4px;
}

.empty-hint {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-lighter);
}
</style>
