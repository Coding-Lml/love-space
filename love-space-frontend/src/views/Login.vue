<template>
  <div class="login-page">
    <div class="login-header">
      <div class="hearts">
        <span class="heart">💕</span>
      </div>
      <h1 class="title">Love Space</h1>
      <p class="subtitle">我们的小天地</p>
    </div>
    
    <div class="login-card">
      <van-tabs v-model:active="activeTab" animated>
        <van-tab title="登录">
          <van-form @submit="onLogin">
            <van-cell-group inset>
              <van-field
                v-model="loginForm.username"
                name="username"
                label="用户名"
                placeholder="请输入用户名"
                :rules="[{ required: true, message: '请输入用户名' }]"
              />
              <van-field
                v-model="loginForm.password"
                type="password"
                name="password"
                label="密码"
                placeholder="请输入密码"
                :rules="[{ required: true, message: '请输入密码' }]"
              />
            </van-cell-group>
            
            <div class="login-btn-wrapper">
              <van-button 
                round 
                block 
                type="primary" 
                native-type="submit"
                :loading="loading"
                loading-text="登录中..."
              >
                登录
              </van-button>
            </div>
          </van-form>
        </van-tab>
        <van-tab title="注册">
          <van-form @submit="onRegister">
            <van-cell-group inset>
              <van-field
                v-model="registerForm.username"
                name="username"
                label="用户名"
                placeholder="3-50位，建议英文/数字"
                :rules="[{ required: true, message: '请输入用户名' }]"
              />
              <van-field
                v-model="registerForm.nickname"
                name="nickname"
                label="昵称"
                placeholder="可选"
              />
              <van-field
                v-model="registerForm.password"
                type="password"
                name="password"
                label="密码"
                placeholder="至少6位"
                :rules="[{ required: true, message: '请输入密码' }]"
              />
              <van-field
                v-model="registerForm.confirmPassword"
                type="password"
                name="confirmPassword"
                label="确认密码"
                placeholder="再输入一次密码"
                :rules="[{ required: true, message: '请确认密码' }]"
              />
            </van-cell-group>
            
            <div class="login-btn-wrapper">
              <van-button 
                round 
                block 
                type="primary" 
                native-type="submit"
                :loading="loading"
                loading-text="注册中..."
              >
                注册并进入
              </van-button>
            </div>
          </van-form>
        </van-tab>
      </van-tabs>
    </div>
    
    <div class="login-footer">
      <p>李梦龙 ❤️ 曾凡芮</p>
      <p class="date">2026.01.21 开始</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const activeTab = ref(0)
const loginForm = ref({
  username: '',
  password: ''
})
const registerForm = ref({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const onLogin = async () => {
  loading.value = true
  try {
    const res = await userStore.login(loginForm.value.username, loginForm.value.password)
    if (res.code === 200) {
      showToast({
        message: '欢迎回来 💕',
        icon: 'like-o'
      })
      router.replace({ name: userStore.isOwner ? 'home' : 'guestMoments' })
    } else {
      showToast(res.message || '登录失败')
    }
  } catch (e) {
    showToast('登录失败，请重试')
  } finally {
    loading.value = false
  }
}

const onRegister = async () => {
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    showToast('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    const res = await userStore.register(registerForm.value.username, registerForm.value.password, registerForm.value.nickname)
    if (res.code === 200) {
      showToast({
        message: '注册成功 💕',
        icon: 'success'
      })
      router.replace({ name: userStore.isOwner ? 'home' : 'guestMoments' })
    } else {
      showToast(res.message || '注册失败')
    }
  } catch (e) {
    showToast('注册失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 50%, #fecfef 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.hearts {
  margin-bottom: 16px;
}

.heart {
  font-size: 64px;
  animation: heartbeat 1s ease-in-out infinite;
  display: inline-block;
}

@keyframes heartbeat {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

.title {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
  margin: 0;
}

.subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  margin-top: 8px;
}

.login-card {
  width: 100%;
  max-width: 360px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  padding: 24px 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.login-btn-wrapper {
  padding: 24px 16px 8px;
}

.login-footer {
  margin-top: 40px;
  text-align: center;
  color: #fff;
}

.login-footer p {
  margin: 4px 0;
  font-size: 14px;
}

.login-footer .date {
  opacity: 0.8;
  font-size: 12px;
}

:deep(.van-cell-group--inset) {
  margin: 0;
}

:deep(.van-button--primary) {
  background: linear-gradient(135deg, #ff6b81 0%, #e84a5f 100%);
  border: none;
  height: 48px;
  font-size: 16px;
}
</style>
