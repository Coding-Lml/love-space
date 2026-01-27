<template>
  <div class="love-timer card">
    <div class="couple-avatars">
      <img :src="leftAvatar" class="avatar avatar-large" fetchpriority="high" decoding="async" />
      <span class="heart-icon heartbeat">❤️</span>
      <img :src="rightAvatar" class="avatar avatar-large" fetchpriority="high" decoding="async" />
    </div>
    <div class="couple-names">
      <span>{{ leftName }}</span>
      <span class="and">&</span>
      <span>{{ rightName }}</span>
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
</template>

<script setup>
import { computed, ref, watch, onUnmounted } from 'vue'

const props = defineProps({
  dashboard: {
    type: Object,
    default: null
  },
  leftUser: {
    type: Object,
    default: null
  },
  rightUser: {
    type: Object,
    default: null
  }
})

const seconds = ref(0)
let timer = null

const leftAvatar = computed(() => props.leftUser?.avatar || '/default-avatar.png')
const rightAvatar = computed(() => props.rightUser?.avatar || '/default-avatar.png')
const leftName = computed(() => props.leftUser?.nickname || props.leftUser?.username || '')
const rightName = computed(() => props.rightUser?.nickname || props.rightUser?.username || '')

watch(() => props.dashboard, (d) => {
  seconds.value = d?.togetherSeconds || 0
  if (timer) clearInterval(timer)
  if (!d) return
  timer = setInterval(() => {
    seconds.value++
    if (seconds.value >= 60) {
      seconds.value = 0
    }
  }, 1000)
}, { immediate: true })

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
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
</style>

