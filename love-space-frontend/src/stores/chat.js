import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'

export const useChatStore = defineStore('chat', () => {
  const connected = ref(false)
  const connecting = ref(false)
  const ws = ref(null)
  const messages = ref([])
  const loadingHistory = ref(false)
  const hasMore = ref(true)
  const active = ref(false)
  const unreadCount = ref(0)
  const reconnecting = ref(false)
  const reconnectInMs = ref(0)
  const lastDisconnectAt = ref(null)
  const lastClose = ref(null)

  let reconnectTimer = null
  let reconnectAttempts = 0
  let manualClose = false
  let listenersBound = false

  const getCurrentUserId = () => {
    try {
      const currentUser = JSON.parse(localStorage.getItem('user') || 'null')
      return currentUser && currentUser.id ? currentUser.id : null
    } catch (e) {
      return null
    }
  }

  const clearReconnectTimer = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  const scheduleReconnect = () => {
    const token = localStorage.getItem('token')
    if (!token) return
    if (connected.value || connecting.value) return
    if (reconnectTimer) return

    reconnectAttempts += 1
    const base = Math.min(30000, 1000 * Math.pow(2, Math.min(reconnectAttempts, 5)))
    const jitter = Math.floor(Math.random() * 400)
    const delay = base + jitter

    reconnecting.value = true
    reconnectInMs.value = delay
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      reconnectInMs.value = 0
      connect()
    }, delay)
  }

  const bindGlobalListeners = () => {
    if (listenersBound) return
    listenersBound = true
    window.addEventListener('online', () => {
      if (!connected.value) {
        clearReconnectTimer()
        connect()
      }
    })
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible' && !connected.value) {
        clearReconnectTimer()
        connect()
      }
    })
  }

  const connect = () => {
    if (connected.value || connecting.value) return

    const token = localStorage.getItem('token')
    if (!token) return

    bindGlobalListeners()
    clearReconnectTimer()
    manualClose = false
    reconnecting.value = false
    connecting.value = true

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const url = `${protocol}//${window.location.host}/ws/chat`
    const socket = new WebSocket(url)

    socket.onopen = () => {
      ws.value = socket
      try {
        socket.send(JSON.stringify({ type: 'auth', token }))
      } catch (e) {
        socket.close()
      }
    }

    socket.onmessage = event => {
      try {
        const payload = JSON.parse(event.data)
        if (payload.event === 'auth') {
          if (payload.status === 'ok') {
            connected.value = true
            connecting.value = false
            reconnecting.value = false
            reconnectAttempts = 0
          } else {
            socket.close()
          }
        } else if (payload.event === 'read') {
          handleReadEvent(payload)
        } else {
          appendMessage(payload)
        }
      } catch (e) {
      }
    }

    socket.onclose = event => {
      connected.value = false
      connecting.value = false
      ws.value = null
      lastDisconnectAt.value = Date.now()
      lastClose.value = {
        code: event?.code ?? null,
        reason: event?.reason ?? null,
        wasClean: event?.wasClean ?? null
      }
      if (!manualClose && event?.reason === 'unauthorized') {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/login'
        return
      }
      if (!manualClose) {
        scheduleReconnect()
      } else {
        reconnecting.value = false
        reconnectInMs.value = 0
      }
    }

    socket.onerror = () => {
      lastClose.value = {
        code: 'error',
        reason: 'socket error',
        wasClean: false
      }
      socket.close()
    }
  }

  const appendMessage = msg => {
    const exists = messages.value.find(m => m.id === msg.id)
    if (exists) return
    messages.value.push(msg)
    messages.value.sort((a, b) => a.id - b.id)
    const currentUserId = getCurrentUserId()
    if (currentUserId && msg.toUserId === currentUserId && !active.value) {
      unreadCount.value += 1
    }
  }

  const sendMessage = payload => {
    if (!connected.value || !ws.value || ws.value.readyState !== WebSocket.OPEN) return
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
    manualClose = true
    clearReconnectTimer()
    if (ws.value) {
      try {
        ws.value.close()
      } catch (e) {
      }
    }
    ws.value = null
    connected.value = false
    connecting.value = false
    reconnecting.value = false
    reconnectInMs.value = 0
    reconnectAttempts = 0
    messages.value = []
    loadingHistory.value = false
    hasMore.value = true
    active.value = false
    unreadCount.value = 0
    lastDisconnectAt.value = null
  }

  const setActive = flag => {
    active.value = flag
    if (flag) {
      unreadCount.value = 0
    }
  }

  return {
    connected,
    connecting,
    reconnecting,
    reconnectInMs,
    lastDisconnectAt,
    lastClose,
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
