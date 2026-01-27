<template>
  <div class="home-page">
    <van-nav-bar title="Love Space" />

    <LoveTimer :dashboard="dashboard" :left-user="couple?.user1" :right-user="couple?.user2" />

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

    <div class="recent-section card" v-if="dashboard?.recentMoments?.length">
      <div class="section-title">
        <span>📝</span>
        <span>最近公开动态</span>
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
            <div class="time-text">{{ formatTime(moment.createdAt) }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="empty-hint" v-if="!loading && !dashboard">
      <p>加载中...</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import dayjs from 'dayjs'
import api from '../api'
import LoveTimer from '../components/LoveTimer.vue'

const loading = ref(true)
const dashboard = ref(null)
const couple = ref(null)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await api.guest.getDashboard()
    if (res.code === 200) {
      dashboard.value = res.data?.dashboard || null
      couple.value = res.data?.couple || null
    }
  } catch (e) {
    console.error('获取游客主页数据失败', e)
  } finally {
    loading.value = false
  }
}

const formatTime = (time) => {
  return dayjs(time).format('MM-DD HH:mm')
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  padding-bottom: 70px;
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

.recent-moments {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.moment-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.moment-content {
  flex: 1;
}

.moment-text {
  font-size: 14px;
  color: var(--text-color);
}
</style>

