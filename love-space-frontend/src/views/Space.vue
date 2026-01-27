<template>
  <div class="space-page">
    <van-nav-bar title="我的空间" left-arrow @click-left="router.back()" />
    
    <div class="card space-card" v-if="spaceDetail?.space">
      <div class="space-name">{{ spaceDetail.space.name }}</div>
      <div class="space-meta">空间ID：{{ spaceDetail.space.id }}</div>
    </div>

    <div class="card" v-if="spaceDetail?.members?.length">
      <div class="section-title">
        <span>👥</span>
        <span>空间成员</span>
      </div>
      <div class="member-list">
        <div v-for="m in spaceDetail.members" :key="m.id" class="member-item">
          <img :src="m.avatar || '/default-avatar.png'" class="avatar" loading="lazy" decoding="async" />
          <div class="member-info">
            <div class="member-name">{{ m.nickname }}</div>
            <div class="member-username">@{{ m.username }}</div>
          </div>
        </div>
      </div>
    </div>

    <van-empty v-if="!loading && !spaceDetail" description="暂无空间信息" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const loading = ref(false)
const spaceDetail = ref(null)

const fetchSpace = async () => {
  loading.value = true
  try {
    const res = await api.space.current()
    if (res.code === 200) {
      spaceDetail.value = res.data
    }
  } catch (e) {
    console.error('获取空间失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchSpace()
})
</script>

<style scoped>
.space-page {
  min-height: 100vh;
  padding-bottom: 70px;
}

.space-card {
  padding: 20px;
}

.space-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-color);
}

.space-meta {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-lighter);
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-color);
  border-radius: 12px;
}

.member-info {
  flex: 1;
}

.member-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-color);
}

.member-username {
  font-size: 12px;
  color: var(--text-lighter);
  margin-top: 2px;
}
</style>

