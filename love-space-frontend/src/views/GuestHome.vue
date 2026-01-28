<template>
  <div class="home-page">
    <van-nav-bar title="主页" />

    <div class="intro card" v-if="couple">
      <div class="couple-avatars">
        <img :src="couple?.user1?.avatar || '/default-avatar.png'" class="avatar avatar-large" fetchpriority="high" decoding="async" />
        <span class="heart-icon">❤️</span>
        <img :src="couple?.user2?.avatar || '/default-avatar.png'" class="avatar avatar-large" fetchpriority="high" decoding="async" />
      </div>
      <div class="couple-names">
        <span>{{ couple?.user1?.nickname || couple?.user1?.username }}</span>
        <span class="and">&</span>
        <span>{{ couple?.user2?.nickname || couple?.user2?.username }}</span>
      </div>
      <div class="start-date" v-if="dashboard?.startDate">
        从 {{ dashboard.startDate }} 开始 💕
      </div>
    </div>

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

    <van-empty v-if="!loading && !dashboard" description="暂无内容" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import dayjs from 'dayjs'
import api from '../api'

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

.intro {
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
}

.couple-names .and {
  margin: 0 8px;
  color: var(--primary-color);
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
