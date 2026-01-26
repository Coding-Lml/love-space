<template>
  <div class="profile-page">
    <van-nav-bar title="我的" left-arrow @click-left="router.back()" />
    
    <!-- 用户信息卡片 -->
    <div class="profile-card card">
      <div class="avatar-section" @click="showAvatarAction = true">
        <img :src="userStore.user?.avatar || '/default-avatar.png'" class="avatar avatar-large" loading="lazy" decoding="async" />
        <div class="edit-hint">点击更换头像</div>
      </div>
      <div class="user-info">
        <div class="nickname">{{ userStore.user?.nickname }}</div>
        <div class="username">@{{ userStore.user?.username }}</div>
      </div>
    </div>
    
    <!-- 另一半信息 -->
    <div class="partner-card card" v-if="userStore.partner">
      <div class="section-title">
        <span>💕</span>
        <span>我的另一半</span>
      </div>
      <div class="partner-info">
        <img :src="userStore.partner?.avatar || '/default-avatar.png'" class="avatar" loading="lazy" decoding="async" />
        <div class="partner-name">{{ userStore.partner?.nickname }}</div>
      </div>
    </div>
    
    <van-cell-group inset title="设置">
      <van-cell
        title="聊天"
        is-link
        :value="chatStore.unreadCount ? `未读${chatStore.unreadCount}条` : ''"
        @click="goChat"
      />
      <van-cell title="修改昵称" is-link @click="showNicknamePopup = true" />
      <van-cell title="修改密码" is-link @click="showPasswordPopup = true" />
      <van-cell title="关于我们" is-link @click="showAbout = true" />
    </van-cell-group>
    
    <!-- 退出登录 -->
    <div class="logout-section">
      <van-button round block plain type="danger" @click="logout">
        退出登录
      </van-button>
    </div>
    
    <!-- 修改昵称弹窗 -->
    <van-popup v-model:show="showNicknamePopup" position="bottom" round>
      <div class="popup-content">
        <div class="popup-header">修改昵称</div>
        <van-field v-model="newNickname" placeholder="请输入新昵称" maxlength="20" />
        <van-button type="primary" block round @click="updateNickname">保存</van-button>
      </div>
    </van-popup>
    
    <!-- 修改密码弹窗 -->
    <van-popup v-model:show="showPasswordPopup" position="bottom" round>
      <div class="popup-content">
        <div class="popup-header">修改密码</div>
        <van-field v-model="passwordForm.oldPassword" type="password" placeholder="当前密码" />
        <van-field v-model="passwordForm.newPassword" type="password" placeholder="新密码" />
        <van-field v-model="passwordForm.confirmPassword" type="password" placeholder="确认新密码" />
        <van-button type="primary" block round @click="updatePassword">保存</van-button>
      </div>
    </van-popup>
    
    <!-- 关于弹窗 -->
    <van-popup v-model:show="showAbout" position="center" round style="width: 80%; padding: 24px;">
      <div class="about-content">
        <div class="about-icon">💕</div>
        <h3>Love Space</h3>
        <p>我们的专属小天地</p>
        <p class="version">v1.0.0</p>
        <div class="about-couple">
          李梦龙 ❤️ 曾凡芮
        </div>
        <p class="about-date">2026.01.21</p>
      </div>
    </van-popup>
    
    <!-- 头像操作 -->
    <van-action-sheet
      v-model:show="showAvatarAction"
      :actions="avatarActions"
      cancel-text="取消"
      @select="onAvatarSelect"
    />
    
    <!-- 隐藏的文件选择器 -->
    <input 
      ref="fileInput" 
      type="file" 
      accept="image/*" 
      style="display: none;" 
      @change="onFileChange"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { useUserStore } from '../stores/user'
import { useChatStore } from '../stores/chat'
import api from '../api'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()

const showNicknamePopup = ref(false)
const showPasswordPopup = ref(false)
const showAbout = ref(false)
const showAvatarAction = ref(false)

const newNickname = ref('')
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const avatarActions = [
  { name: '从相册选择' }
]

const fileInput = ref(null)

const goChat = () => {
  router.push({ name: 'chat' })
}

// 头像操作
const onAvatarSelect = (action) => {
  if (action.name === '从相册选择') {
    fileInput.value?.click()
  }
}

// 文件选择
const onFileChange = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  
  try {
    const res = await api.user.uploadAvatar(file)
    if (res.code === 200) {
      // 更新 store 中的用户信息
      userStore.user.avatar = res.data
      // 更新本地存储
      localStorage.setItem('user', JSON.stringify(userStore.user))
      showToast({ message: '头像更新成功', icon: 'success' })
    } else {
      showToast(res.message || '上传失败')
    }
  } catch (e) {
    showToast('上传失败')
  }
}

// 修改昵称
const updateNickname = async () => {
  if (!newNickname.value.trim()) {
    showToast('请输入昵称')
    return
  }
  
  const res = await userStore.updateProfile({ nickname: newNickname.value })
  if (res.code === 200) {
    showToast({ message: '修改成功', icon: 'success' })
    showNicknamePopup.value = false
  } else {
    showToast(res.message || '修改失败')
  }
}

// 修改密码
const updatePassword = async () => {
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
    showToast('请填写完整')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    showToast('两次密码不一致')
    return
  }
  
  try {
    const res = await api.auth.changePassword(
      passwordForm.value.oldPassword,
      passwordForm.value.newPassword
    )
    if (res.code === 200) {
      showToast({ message: '修改成功，请重新登录', icon: 'success' })
      showPasswordPopup.value = false
      userStore.logout()
      router.push({ name: 'login' })
    } else {
      showToast(res.message || '修改失败')
    }
  } catch (e) {
    showToast('修改失败')
  }
}

// 退出登录
const logout = async () => {
  try {
    await showConfirmDialog({
      title: '确认退出',
      message: '确定要退出登录吗？'
    })
    userStore.logout()
    router.push({ name: 'login' })
  } catch (e) {
    // 取消
  }
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: var(--bg-color);
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  margin: 12px;
}

.avatar-section {
  text-align: center;
}

.edit-hint {
  font-size: 12px;
  color: var(--text-lighter);
  margin-top: 8px;
}

.user-info {
  flex: 1;
}

.nickname {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-color);
}

.username {
  font-size: 14px;
  color: var(--text-lighter);
  margin-top: 4px;
}

.partner-card {
  margin: 12px;
}

.partner-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-color);
  border-radius: 12px;
}

.partner-name {
  font-size: 16px;
  font-weight: 500;
}

.logout-section {
  padding: 24px 16px;
}

.popup-content {
  padding: 20px;
}

.popup-header {
  font-size: 18px;
  font-weight: 600;
  text-align: center;
  margin-bottom: 20px;
}

.popup-content .van-field {
  margin-bottom: 12px;
  background: var(--bg-color);
  border-radius: 8px;
}

.popup-content .van-button {
  margin-top: 12px;
}

.about-content {
  text-align: center;
}

.about-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.about-content h3 {
  font-size: 20px;
  margin: 0 0 8px;
}

.about-content p {
  color: var(--text-light);
  margin: 4px 0;
}

.version {
  font-size: 12px;
  color: var(--text-lighter);
}

.about-couple {
  font-size: 16px;
  color: var(--primary-color);
  margin-top: 20px;
}

.about-date {
  font-size: 12px;
  color: var(--text-lighter);
}

:deep(.van-button--primary) {
  background: linear-gradient(135deg, #ff6b81 0%, #e84a5f 100%);
  border: none;
}
</style>
