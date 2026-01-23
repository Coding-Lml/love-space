import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'

export const useChatStore = defineStore('chat', () => {
  const currentUser = JSON.parse(localStorage.getItem('user') || 'null')
  const currentUserId = currentUser && currentUser.id ? currentUser.id : null

  const connected = ref(false)
  const connecting = ref(false)
  const ws = ref(null)
  const messages = ref([])
  const loadingHistory = ref(false)
  const hasMore = ref(true)
  const active = ref(false)
  const unreadCount = ref(0)

  const connect = () => {
    if (connected.value || connecting.value) return

    const token = localStorage.getItem('token')
    if (!token) return

    connecting.value = true

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const url = `${protocol}//${window.location.host}/ws/chat?token=${encodeURIComponent(token)}`
    const socket = new WebSocket(url)

    socket.onopen = () => {
      ws.value = socket
      connected.value = true
      connecting.value = false
    }

    socket.onmessage = event => {
      try {
        const payload = JSON.parse(event.data)
        if (payload.event === 'read') {
          handleReadEvent(payload)
        } else {
          appendMessage(payload)
        }
      } catch (e) {
      }
    }

    socket.onclose = () => {
      connected.value = false
      connecting.value = false
      ws.value = null
    }

    socket.onerror = () => {
      socket.close()
    }
  }

  const appendMessage = msg => {
    const exists = messages.value.find(m => m.id === msg.id)
    if (exists) return
    messages.value.push(msg)
    messages.value.sort((a, b) => a.id - b.id)
    if (currentUserId && msg.toUserId === currentUserId && !active.value) {
      unreadCount.value += 1
    }
  }

  const sendMessage = payload => {
    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) return
    ws.value.send(JSON.stringify(payload))
  }

  const sendText = content => {
    if (!content || !content.trim()) return
    sendMessage({
      type: 'text',
      content: content.trim()
    })
  }

  const sendMedia = (type, mediaUrl, extra) => {
    if (!mediaUrl) return
    sendMessage({
      type,
      mediaUrl,
      extra: extra || null
    })
  }

  const loadHistory = async () => {
    if (loadingHistory.value || !hasMore.value) return
    loadingHistory.value = true
    try {
      const beforeId = messages.value.length > 0 ? messages.value[0].id : null
      const res = await api.chat.history(beforeId, 20)
      if (res.code === 200) {
        const list = res.data || []
        if (list.length === 0) {
          hasMore.value = false
        } else {
          const existingIds = new Set(messages.value.map(m => m.id))
          const merged = [...list.filter(m => !existingIds.has(m.id)), ...messages.value]
          merged.sort((a, b) => a.id - b.id)
          messages.value = merged
        }
      }
    } finally {
      loadingHistory.value = false
    }
  }

  const handleReadEvent = payload => {
    const ids = Array.isArray(payload.messageIds) ? payload.messageIds : []
    if (!ids.length) return
    const set = new Set(ids)
    messages.value.forEach(m => {
      if (set.has(m.id)) {
        m.status = 'read'
      }
    })
  }

  const reset = () => {
    if (ws.value) {
      try {
        ws.value.close()
      } catch (e) {
      }
    }
    ws.value = null
    connected.value = false
    connecting.value = false
    messages.value = []
    loadingHistory.value = false
    hasMore.value = true
    active.value = false
    unreadCount.value = 0
  }

  const setActive = flag => {
    active.value = flag
    if (flag) {
      unreadCount.value = 0
    }
  }

  return {
    connected,
    messages,
    loadingHistory,
    hasMore,
    unreadCount,
    connect,
    sendText,
    sendMedia,
    loadHistory,
    reset,
    setActive
  }
})
