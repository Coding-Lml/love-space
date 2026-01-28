<template>
  <div class="profile-page">
    <van-nav-bar title="主页" />

    <div class="profile-card card">
      <div class="avatar-section" @click="showAvatarAction = true">
        <img :src="userStore.user?.avatar || '/default-avatar.png'" class="avatar avatar-large" loading="lazy" decoding="async" />
        <div class="edit-hint">点击更换头像</div>
      </div>
      <div class="user-info">
        <div class="nickname">
          <span>{{ userStore.user?.nickname }}</span>
          <van-tag plain type="primary" size="medium">游客</van-tag>
        </div>
        <div class="username">@{{ userStore.user?.username }}</div>
      </div>
    </div>

    <div class="guest-help card">
      <div class="guest-help-title">游客模式说明</div>
      <div class="guest-help-text">你当前登录的是游客账号。</div>
      <div class="guest-help-text">可用功能：发布游客动态、查看主人公开动态。</div>
      <div class="guest-help-text">不可用功能：日记、纪念日、聊天与空间管理（仅主人可用）。</div>
    </div>

    <van-cell-group inset title="账号与安全">
      <van-cell title="修改昵称" is-link @click="openNickname" />
      <van-cell title="修改密码" is-link @click="showPasswordPopup = true" />
    </van-cell-group>

    <van-cell-group inset title="帮助">
      <van-cell title="关于" is-link @click="showAbout = true" />
    </van-cell-group>

    <div class="logout-section">
      <van-button round block plain type="danger" @click="logout">
        退出登录
      </van-button>
    </div>

    <van-popup v-model:show="showNicknamePopup" position="bottom" round>
      <div class="popup-content">
        <div class="popup-header">修改昵称</div>
        <van-field v-model="newNickname" placeholder="请输入新昵称" maxlength="20" />
        <van-button type="primary" block round @click="updateNickname">保存</van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showPasswordPopup" position="bottom" round>
      <div class="popup-content">
        <div class="popup-header">修改密码</div>
        <van-field v-model="passwordForm.oldPassword" type="password" placeholder="当前密码" />
        <van-field v-model="passwordForm.newPassword" type="password" placeholder="新密码" />
        <van-field v-model="passwordForm.confirmPassword" type="password" placeholder="确认新密码" />
        <van-button type="primary" block round @click="updatePassword">保存</van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showAbout" position="center" round style="width: 80%; padding: 24px;">
      <div class="about-content">
        <div class="about-icon">💗</div>
        <h3>Love Space</h3>
        <p>轻量记录与分享的小站</p>
        <p class="version">v1.0.0</p>
      </div>
    </van-popup>

    <van-action-sheet
      v-model:show="showAvatarAction"
      :actions="avatarActions"
      cancel-text="取消"
      @select="onAvatarSelect"
    />

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
import api from '../api'

const router = useRouter()
const userStore = useUserStore()

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

const openNickname = () => {
  newNickname.value = userStore.user?.nickname || ''
  showNicknamePopup.value = true
}

const onAvatarSelect = (action) => {
  if (action.name === '从相册选择') {
    fileInput.value?.click()
  }
}

const onFileChange = async (e) => {
  const file = e.target.files[0]
  if (!file) return

  try {
    const res = await api.user.uploadAvatar(file)
    if (res.code === 200) {
      userStore.user.avatar = res.data
      localStorage.setItem('user', JSON.stringify(userStore.user))
      showToast({ message: '头像更新成功', icon: 'success' })
    } else {
      showToast(res.message || '上传失败')
    }
  } catch (e) {
    showToast('上传失败')
  }
}

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
      router.replace({ name: 'login' })
    } else {
      showToast(res.message || '修改失败')
    }
  } catch (e) {
    showToast('修改失败')
  }
}

const logout = async () => {
  try {
    await showConfirmDialog({
      title: '确认退出',
      message: '确定要退出登录吗？'
    })
    userStore.logout()
    router.replace({ name: 'login' })
  } catch (e) {
  }
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  padding-bottom: 70px;
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
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-color);
}

.username {
  font-size: 14px;
  color: var(--text-lighter);
  margin-top: 8px;
}

.guest-help {
  padding: 16px;
  margin: 12px;
  background: linear-gradient(135deg, #fff 0%, #fff5f5 100%);
}

.guest-help-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-color);
  margin-bottom: 8px;
}

.guest-help-text {
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-light);
}

.logout-section {
  padding: 20px 16px;
}

.popup-content {
  padding: 20px;
}

.popup-header {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-color);
  margin-bottom: 16px;
}

.about-content {
  text-align: center;
  color: var(--text-color);
}

.about-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.about-content h3 {
  margin: 0 0 8px;
}

.about-content p {
  margin: 6px 0;
  font-size: 14px;
  color: var(--text-light);
}

.version {
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-lighter);
}
</style>
