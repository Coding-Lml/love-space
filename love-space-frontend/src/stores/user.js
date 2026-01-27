import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const partner = ref(null)
  
  const isLoggedIn = computed(() => !!token.value)
  
  // 登录
  async function login(username, password) {
    const res = await api.auth.login(username, password)
    if (res.code === 200) {
      token.value = res.data.token
      user.value = res.data.user
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(res.data.user))
      // 获取另一半信息
      await fetchPartner()
    }
    return res
  }

  async function register(username, password, nickname) {
    const res = await api.auth.register(username, password, nickname)
    if (res.code === 200) {
      token.value = res.data.token
      user.value = res.data.user
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(res.data.user))
      await fetchPartner()
    }
    return res
  }
  
  // 登出
  function logout() {
    token.value = ''
    user.value = null
    partner.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }
  
  // 获取另一半信息
  async function fetchPartner() {
    try {
      const res = await api.auth.getPartner()
      if (res.code === 200) {
        partner.value = res.data
      }
    } catch (e) {
      console.error('获取另一半信息失败', e)
    }
  }
  
  // 更新用户信息
  async function updateProfile(data) {
    const res = await api.auth.updateProfile(data)
    if (res.code === 200) {
      user.value = res.data
      localStorage.setItem('user', JSON.stringify(res.data))
    }
    return res
  }
  
  return {
    token,
    user,
    partner,
    isLoggedIn,
    login,
    register,
    logout,
    fetchPartner,
    updateProfile
  }
})
