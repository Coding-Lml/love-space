<template>
  <div class="home-page">
    <van-nav-bar title="Love Space">
      <template #right>
        <van-icon name="setting-o" size="20" color="#ff5a7a" @click="goProfile" />
      </template>
    </van-nav-bar>

    <section class="home-hero">
      <div class="hero-topline">
        <span>LOVE SPACE</span>
        <button type="button" class="hero-chat" @click="goChat">
          <van-icon name="chat-o" />
          <span>{{ chatUnread ? `${chatUnread} 条未读` : '继续聊天' }}</span>
        </button>
      </div>

      <div class="couple-line">
        <img :src="userStore.user?.avatar || '/default-avatar.png'" class="hero-avatar" fetchpriority="high" decoding="async" />
        <div class="hero-heart">❤️</div>
        <img :src="userStore.partner?.avatar || '/default-avatar.png'" class="hero-avatar" fetchpriority="high" decoding="async" />
      </div>

      <div class="hero-copy">
        <div class="hero-kicker">{{ coupleNames }}</div>
        <h1 v-if="dashboard">
          第 <span>{{ dashboard.togetherDays }}</span> 天
        </h1>
        <h1 v-else>今天也在一起</h1>
        <p v-if="dashboard">
          从 {{ dashboard.startDate }} 开始 · {{ dashboard.togetherHours }}小时 {{ dashboard.togetherMinutes }}分钟 {{ seconds }}秒
        </p>
        <p v-else-if="loadingDashboard">正在整理你们的今日记忆...</p>
        <p v-else>恋爱计时暂时没加载出来，但记录入口都可以继续使用。</p>
      </div>
    </section>

    <section class="quick-actions">
      <button type="button" class="quick-action warm" @click="goMomentCreate">
        <van-icon name="plus" />
        <span>发动态</span>
      </button>
      <button type="button" class="quick-action cool" @click="goDiaryWrite">
        <van-icon name="edit" />
        <span>写日记</span>
      </button>
      <button type="button" class="quick-action ink" @click="goAnniversary">
        <van-icon name="calendar-o" />
        <span>纪念日</span>
      </button>
    </section>

    <section class="next-memory" :class="{ empty: !nextAnniversary }">
      <div>
        <div class="section-kicker">NEXT DATE</div>
        <h2>{{ nextAnniversary?.title || '还没有倒计时' }}</h2>
        <p>{{ nextAnniversary ? formatAnniversaryDistance(nextAnniversary) : '添加一个生日、旅行或重要日子，首页会优先提醒。' }}</p>
      </div>
      <button type="button" @click="goAnniversary">
        <van-icon name="arrow" />
      </button>
    </section>

    <section class="memory-grid">
      <article class="memory-panel">
        <div class="panel-header">
          <div>
            <div class="section-kicker">MOMENTS</div>
            <h2>最近动态</h2>
          </div>
          <button type="button" @click="goMoments">全部</button>
        </div>

        <van-skeleton v-if="loadingDashboard && !recentMoments.length" title :row="2" />
        <div v-else-if="recentMoments.length" class="compact-list">
          <button
            v-for="moment in recentMoments"
            :key="moment.id"
            type="button"
            class="compact-item"
            @click="goMoments"
          >
            <img :src="moment.user?.avatar || '/default-avatar.png'" class="mini-avatar" loading="lazy" decoding="async" />
            <span>{{ summarizeMoment(moment) }}</span>
            <small>{{ formatTime(moment.createdAt) }}</small>
          </button>
        </div>
        <div v-else class="panel-empty">
          <span>今天还没有动态</span>
          <button type="button" @click="goMomentCreate">发布</button>
        </div>
      </article>

      <article class="memory-panel">
        <div class="panel-header">
          <div>
            <div class="section-kicker">DIARY</div>
            <h2>最近日记</h2>
          </div>
          <button type="button" @click="goDiary">全部</button>
        </div>

        <van-skeleton v-if="loadingDiaries && !recentDiaries.length" title :row="2" />
        <div v-else-if="recentDiaries.length" class="diary-snaps">
          <button
            v-for="diary in recentDiaries"
            :key="diary.id"
            type="button"
            class="diary-snap"
            @click="goDiary"
          >
            <div class="snap-date">
              <strong>{{ formatDiaryDateParts(diary.diaryDate).day }}</strong>
              <span>{{ formatDiaryDateParts(diary.diaryDate).month }}</span>
            </div>
            <div>
              <span class="snap-mood">{{ getDiaryMood(diary.mood).emoji }} {{ getDiaryMood(diary.mood).text }}</span>
              <p>{{ summarizeDiaryText(diary, 34) }}</p>
            </div>
          </button>
        </div>
        <div v-else class="panel-empty">
          <span>{{ diaryError ? '日记暂时加载失败' : '还没有日记' }}</span>
          <button type="button" @click="diaryError ? fetchRecentDiaries() : goDiaryWrite()">
            {{ diaryError ? '重试' : '去写' }}
          </button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import api from '../api'
import dayjs from 'dayjs'
import {
  formatAnniversaryDistance,
  formatDiaryDateParts,
  getDiaryMood,
  selectNextAnniversary,
  selectRecentDiaries,
  summarizeDiaryText
} from '../utils/memoryPresentation'

const router = useRouter()
const userStore = useUserStore()

const loadingDashboard = ref(true)
const loadingDiaries = ref(true)
const diaryError = ref(false)
const dashboard = ref(null)
const recentDiaries = ref([])
const chatUnread = ref(0)
const seconds = ref(0)
let timer = null

const recentMoments = computed(() => dashboard.value?.recentMoments?.slice(0, 3) || [])
const nextAnniversary = computed(() => selectNextAnniversary(dashboard.value?.upcomingAnniversaries || []))
const coupleNames = computed(() => {
  const left = userStore.user?.nickname || '我'
  const right = userStore.partner?.nickname || '另一半'
  return `${left} & ${right}`
})

const fetchDashboard = async () => {
  loadingDashboard.value = true
  try {
    const res = await api.dashboard.getData()
    if (res.code === 200) {
      dashboard.value = res.data
      seconds.value = res.data.togetherSeconds || 0
    }
  } catch (e) {
    console.error('获取仪表盘数据失败', e)
  } finally {
    loadingDashboard.value = false
  }
}

const fetchRecentDiaries = async () => {
  loadingDiaries.value = true
  diaryError.value = false
  try {
    const res = await api.diary.getList(1, 4)
    if (res.code === 200) {
      recentDiaries.value = selectRecentDiaries(res.data?.records || [], 2)
    } else {
      diaryError.value = true
    }
  } catch (e) {
    diaryError.value = true
  } finally {
    loadingDiaries.value = false
  }
}

const fetchChatUnread = async () => {
  try {
    const res = await api.chat.getUnreadCount()
    if (res.code === 200) {
      chatUnread.value = Number(res.data || 0)
    }
  } catch (e) {
    chatUnread.value = 0
  }
}

const startTimer = () => {
  timer = setInterval(() => {
    seconds.value += 1
    if (seconds.value >= 60) {
      seconds.value = 0
      fetchDashboard()
    }
  }, 1000)
}

const summarizeMoment = (moment) => {
  const text = (moment.content || '').trim()
  if (text) return text.length > 28 ? `${text.slice(0, 25)}...` : text
  if (moment.mediaList?.length) return `分享了 ${moment.mediaList.length} 个媒体`
  return '记录了一条动态'
}

const formatTime = (time) => {
  if (!time) return ''
  return dayjs(time).format('MM-DD HH:mm')
}

const goProfile = () => router.push({ name: 'profile' })
const goMoments = () => router.push({ name: 'moments' })
const goMomentCreate = () => router.push({ name: 'momentCreate' })
const goDiary = () => router.push({ name: 'diary' })
const goDiaryWrite = () => router.push({ name: 'diaryWrite' })
const goAnniversary = () => router.push({ name: 'anniversary' })
const goChat = () => router.push({ name: 'chat' })

onMounted(() => {
  fetchDashboard()
  fetchRecentDiaries()
  fetchChatUnread()
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
  padding: 12px 12px 88px;
}

.home-hero {
  position: relative;
  overflow: hidden;
  padding: 16px;
  color: #fff;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(40, 35, 47, 0.94), rgba(240, 82, 141, 0.88) 54%, rgba(16, 167, 161, 0.84)),
    #28232f;
  box-shadow: var(--shadow-strong);
}

.hero-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 1.7px;
  opacity: 0.86;
}

.hero-chat {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 10px;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.32);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.14);
}

.couple-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hero-avatar {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.78);
}

.hero-heart {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
}

.hero-copy {
  margin-top: 18px;
}

.hero-kicker {
  font-size: 13px;
  font-weight: 800;
  opacity: 0.78;
}

.hero-copy h1 {
  margin: 4px 0;
  font-size: 30px;
  line-height: 1.05;
  letter-spacing: 0;
}

.hero-copy h1 span {
  font-size: 42px;
}

.hero-copy p {
  font-size: 13px;
  line-height: 1.5;
  opacity: 0.82;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin: 12px 0;
}

.quick-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 76px;
  font-weight: 800;
  color: var(--text-color);
  border: 0;
  border-radius: 8px;
  box-shadow: var(--shadow);
}

.quick-action .van-icon {
  font-size: 22px;
}

.quick-action.warm {
  background: var(--surface-soft);
  color: var(--primary-dark);
}

.quick-action.cool {
  background: var(--surface-mint);
  color: var(--accent-cool);
}

.quick-action.ink {
  background: #f2eff8;
  color: #5b4a7a;
}

.next-memory,
.memory-panel {
  border: 1px solid rgba(255, 122, 89, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--shadow);
}

.next-memory {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 15px;
  margin-bottom: 12px;
}

.next-memory.empty {
  background: linear-gradient(135deg, #fff, #f7fbfa);
}

.next-memory h2,
.memory-panel h2 {
  margin: 2px 0;
  font-size: 18px;
  line-height: 1.2;
  letter-spacing: 0;
}

.next-memory p {
  font-size: 13px;
  color: var(--text-light);
  line-height: 1.45;
}

.next-memory button,
.panel-header button,
.panel-empty button {
  flex: 0 0 auto;
  border: 0;
  border-radius: 8px;
  color: var(--primary-color);
  background: var(--surface-soft);
}

.next-memory button {
  width: 34px;
  height: 34px;
}

.section-kicker {
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 1.4px;
  color: var(--accent-cool);
}

.memory-grid {
  display: grid;
  gap: 12px;
}

.memory-panel {
  padding: 14px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-header button,
.panel-empty button {
  min-height: 30px;
  padding: 0 10px;
  font-weight: 800;
}

.compact-list,
.diary-snaps {
  display: grid;
  gap: 8px;
}

.compact-item,
.diary-snap {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 9px;
  min-height: 54px;
  padding: 9px;
  text-align: left;
  border: 1px solid rgba(40, 35, 47, 0.06);
  border-radius: 8px;
  background: #fff;
}

.compact-item span,
.diary-snap p {
  flex: 1;
  min-width: 0;
  color: var(--text-color);
  font-size: 13px;
  line-height: 1.35;
}

.compact-item small {
  flex: 0 0 auto;
  color: var(--text-lighter);
  font-size: 11px;
}

.mini-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

.snap-date {
  display: grid;
  place-items: center;
  width: 42px;
  min-width: 42px;
  min-height: 42px;
  border-radius: 8px;
  color: var(--primary-color);
  background: var(--surface-soft);
}

.snap-date strong {
  font-size: 20px;
  line-height: 1;
}

.snap-date span,
.snap-mood {
  font-size: 11px;
}

.snap-mood {
  display: inline-block;
  margin-bottom: 3px;
  color: var(--text-light);
}

.panel-empty {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 58px;
  color: var(--text-light);
  font-size: 13px;
}

@media (min-width: 768px) {
  .home-page {
    max-width: 760px;
    margin: 0 auto;
  }

  .memory-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
