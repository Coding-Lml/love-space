import axios from 'axios'
import { showToast } from 'vant'

// 创建 axios 实例
const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器
http.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
http.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/login'
        return Promise.reject(error)
      }
      showToast(data?.message || '请求失败')
    } else {
      showToast('网络错误，请稍后重试')
    }
    return Promise.reject(error)
  }
)

// API 接口
const api = {
  // 认证相关
  auth: {
    login: (username, password) => http.post('/auth/login', { username, password }),
    register: (username, password, nickname) => http.post('/auth/register', { username, password, nickname }),
    getMe: () => http.get('/auth/me'),
    getPartner: () => http.get('/auth/partner'),
    updateProfile: (data) => http.put('/auth/me', data),
    changePassword: (oldPassword, newPassword) => http.post('/auth/change-password', { oldPassword, newPassword })
  },
  
    // 用户相关
  user: {
    uploadAvatar: (file) => {
      const formData = new FormData()
      formData.append('file', file)
      return http.post('/users/avatar', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    }
  },
  
  // 仪表盘
  dashboard: {
    getData: () => http.get('/dashboard')
  },

  guest: {
    getDashboard: () => http.get('/guest/dashboard'),
    getMoments: (pageNum = 1, pageSize = 10) => http.get('/guest/moments', { params: { pageNum, pageSize } }),
    publishMoment: (formData) => http.post('/guest/moments', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }),
    deleteMoment: (id) => http.delete(`/guest/moments/${id}`),
    addComment: (id, content, replyToCommentId) => http.post(`/guest/moments/${id}/comments`, { content, replyToCommentId }),
    deleteComment: (commentId) => http.delete(`/guest/moments/comments/${commentId}`)
  },
  
  // 动态相关
  moments: {
    getList: (pageNum = 1, pageSize = 10) => http.get('/moments', { params: { pageNum, pageSize } }),
    getPublicList: (pageNum = 1, pageSize = 10) => http.get('/moments/public', { params: { pageNum, pageSize } }),
    getDetail: (id) => http.get(`/moments/${id}`),
    create: (formData) => http.post('/moments', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }),
    delete: (id) => http.delete(`/moments/${id}`),
    like: (id) => http.post(`/moments/${id}/like`),
    addComment: (id, content, replyToCommentId) => http.post(`/moments/${id}/comments`, { content, replyToCommentId }),
    deleteComment: (commentId) => http.delete(`/moments/comments/${commentId}`)
  },
  
  // 日记相关
  diary: {
    getList: (pageNum = 1, pageSize = 10) => http.get('/diaries', { params: { pageNum, pageSize } }),
    getByMonth: (year, month) => http.get('/diaries/month', { params: { year, month } }),
    getByDate: (date) => http.get('/diaries/date', { params: { date } }),
    getDetail: (id) => http.get(`/diaries/${id}`),
    write: (data) => http.post('/diaries', data),
    delete: (id) => http.delete(`/diaries/${id}`)
  },
  
  // 纪念日相关
  anniversary: {
    getAll: () => http.get('/anniversaries'),
    getTogether: () => http.get('/anniversaries/together'),
    getUpcoming: (days = 30) => http.get('/anniversaries/upcoming', { params: { days } }),
    add: (data) => http.post('/anniversaries', data),
    update: (id, data) => http.put(`/anniversaries/${id}`, data),
    delete: (id) => http.delete(`/anniversaries/${id}`)
  },
  
  // 文件上传
  file: {
    upload: (file) => {
      const formData = new FormData()
      formData.append('file', file)
      return http.post('/files/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    }
  },

  chat: {
    history: (beforeId, size = 20) => http.get('/chat/history', {
      params: {
        beforeId,
        size
      }
    }),
    markRead: () => http.post('/chat/read')
  },

  space: {
    current: () => http.get('/spaces/current')
  }
}

export default api
