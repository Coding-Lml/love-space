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
        <van-tabbar-item name="chat" icon="chat-o" :badge="chatUnreadBadge">聊天</van-tabbar-item>
        <van-tabbar-item name="profile" icon="user-o">我的</van-tabbar-item>
      </template>
      <template v-else>
        <van-tabbar-item name="guestMoments" icon="photo-o">动态</van-tabbar-item>
        <van-tabbar-item name="guestHome" icon="wap-home-o">主页</van-tabbar-item>
      </template>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from './stores/user'
import api from './api'
import { addChatSocketListener, connectChatSocket, disconnectChatSocket } from './utils/chatSocket'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('home')
const chatUnread = ref(0)

const ownerTabbarPages = ['home', 'moments', 'diary', 'anniversary', 'chat', 'profile']
const guestTabbarPages = ['guestMoments', 'guestHome']
const showTabbar = computed(() => {
  if (!userStore.isLoggedIn) return false
  return userStore.isOwner ? ownerTabbarPages.includes(route.name) : guestTabbarPages.includes(route.name)
})
const chatUnreadBadge = computed(() => {
  if (!chatUnread.value) return undefined
  return chatUnread.value > 99 ? '99+' : String(chatUnread.value)
})

const fetchChatUnread = async () => {
  if (!userStore.isLoggedIn || !userStore.isOwner) {
    chatUnread.value = 0
    return
  }
  try {
    const res = await api.chat.getUnreadCount()
    if (res.code === 200) {
      chatUnread.value = Number(res.data || 0)
    }
  } catch (e) {
    console.error('获取聊天未读数失败', e)
  }
}

// 监听路由变化，同步 tabbar 状态
watch(() => route.name, (name) => {
  if (userStore.isOwner && ownerTabbarPages.includes(name)) {
    activeTab.value = name
  } else if (userStore.isGuest && guestTabbarPages.includes(name)) {
    activeTab.value = name
  }
}, { immediate: true })

const onTabChange = (name) => {
  router.push({ name })
}

const removeChatListener = addChatSocketListener((event, data) => {
  if (event === 'unread:update') {
    chatUnread.value = Number(data?.count || 0)
  }
  if (event === 'message:new' && data?.toUserId === userStore.user?.id && route.name !== 'chat') {
    fetchChatUnread()
  }
})

watch(
  () => [userStore.isLoggedIn, userStore.isOwner, userStore.token],
  ([loggedIn, owner, token]) => {
    if (loggedIn && owner && token) {
      connectChatSocket(token)
      fetchChatUnread()
    } else {
      chatUnread.value = 0
      disconnectChatSocket()
    }
  },
  { immediate: true }
)

onUnmounted(() => {
  removeChatListener()
  disconnectChatSocket()
})
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #fff5f5 0%, #ffeef0 100%);
  padding-bottom: 50px;
}
</style>
