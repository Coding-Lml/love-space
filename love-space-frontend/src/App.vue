<template>
  <div class="app-container">
    <router-view v-slot="{ Component }">
      <keep-alive :include="['Home', 'Moments', 'Diary', 'Anniversary']">
        <component :is="Component" />
      </keep-alive>
    </router-view>
    
    <!-- 底部导航栏 -->
    <van-tabbar 
      v-if="showTabbar" 
      v-model="activeTab" 
      active-color="#ff6b81"
      inactive-color="#999"
      @change="onTabChange"
    >
      <van-tabbar-item name="home" icon="wap-home-o">首页</van-tabbar-item>
      <van-tabbar-item name="moments" icon="photo-o">动态</van-tabbar-item>
      <van-tabbar-item name="diary" icon="edit">日记</van-tabbar-item>
      <van-tabbar-item name="anniversary" icon="calendar-o">纪念日</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const activeTab = ref('home')

// 需要显示 tabbar 的页面
const tabbarPages = ['home', 'moments', 'diary', 'anniversary']
const showTabbar = computed(() => tabbarPages.includes(route.name))

// 监听路由变化，同步 tabbar 状态
watch(() => route.name, (name) => {
  if (tabbarPages.includes(name)) {
    activeTab.value = name
  }
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
