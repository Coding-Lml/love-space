<template>
  <div class="app-container">
    <router-view v-slot="{ Component }">
      <keep-alive :include="['Home', 'Moments', 'Diary', 'Anniversary']">
        <component :is="Component" />
      </keep-alive>
    </router-view>
    
    <van-tabbar
      v-if="showTabbar"
      v-model="activeTab"
      active-color="#ff6b81"
      inactive-color="#999"
      @change="onTabChange"
    >
      <template v-if="userStore.isOwner">
        <van-tabbar-item name="home" icon="wap-home-o">首页</van-tabbar-item>
        <van-tabbar-item name="moments" icon="photo-o">动态</van-tabbar-item>
        <van-tabbar-item name="diary" icon="edit">日记</van-tabbar-item>
        <van-tabbar-item name="anniversary" icon="calendar-o">纪念日</van-tabbar-item>
        <van-tabbar-item name="profile" icon="user-o" :badge="chatStore.unreadCount || ''">我的</van-tabbar-item>
      </template>
      <template v-else>
        <van-tabbar-item name="guestMoments" icon="photo-o">动态</van-tabbar-item>
        <van-tabbar-item name="guestHome" icon="wap-home-o">主页</van-tabbar-item>
      </template>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from './stores/user'
import { useChatStore } from './stores/chat'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()

const activeTab = ref('home')

const ownerTabbarPages = ['home', 'moments', 'diary', 'anniversary', 'profile']
const guestTabbarPages = ['guestMoments', 'guestHome']
const showTabbar = computed(() => {
  if (!userStore.isLoggedIn) return false
  return userStore.isOwner ? ownerTabbarPages.includes(route.name) : guestTabbarPages.includes(route.name)
})

// 监听路由变化，同步 tabbar 状态
watch(() => route.name, (name) => {
  if (userStore.isOwner && ownerTabbarPages.includes(name)) {
    activeTab.value = name
  } else if (userStore.isGuest && guestTabbarPages.includes(name)) {
    activeTab.value = name
  }
}, { immediate: true })

watch(() => userStore.isLoggedIn, (loggedIn) => {
  if (loggedIn && userStore.isOwner) {
    chatStore.connect()
  } else {
    chatStore.reset()
  }
}, { immediate: true })

watch(() => route.name, (name) => {
  chatStore.setActive(name === 'chat')
}, { immediate: true })

const onTabChange = (name) => {
  router.push({ name })
}
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #fff5f5 0%, #ffeef0 100%);
  padding-bottom: 50px;
}
</style>
