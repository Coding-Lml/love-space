const listeners = new Set()

let socket = null
let activeToken = ''
let reconnectTimer = null
let reconnectAttempts = 0
let shouldReconnect = false

const notify = (event, data) => {
  listeners.forEach(listener => {
    try {
      listener(event, data)
    } catch (e) {
      console.error('聊天事件处理失败', e)
    }
  })
}

const buildSocketUrl = (token) => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/chat?token=${encodeURIComponent(token)}`
}

const scheduleReconnect = () => {
  if (!shouldReconnect || !activeToken) return
  clearTimeout(reconnectTimer)
  const delay = Math.min(1000 * 2 ** reconnectAttempts, 10000)
  reconnectAttempts += 1
  reconnectTimer = setTimeout(() => {
    connectChatSocket(activeToken)
  }, delay)
}

export const connectChatSocket = (token) => {
  if (!token || typeof window === 'undefined') return
  if (socket && socket.readyState === WebSocket.OPEN && activeToken === token) return

  activeToken = token
  shouldReconnect = true

  if (socket) {
    socket.onclose = null
    socket.close()
  }

  socket = new WebSocket(buildSocketUrl(token))

  socket.onopen = () => {
    reconnectAttempts = 0
  }

  socket.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data)
      notify(payload.event, payload.data)
    } catch (e) {
      console.error('聊天消息解析失败', e)
    }
  }

  socket.onclose = () => {
    scheduleReconnect()
  }

  socket.onerror = () => {
    socket?.close()
  }
}

export const disconnectChatSocket = () => {
  shouldReconnect = false
  activeToken = ''
  clearTimeout(reconnectTimer)
  if (socket) {
    socket.onclose = null
    socket.close()
    socket = null
  }
}

export const addChatSocketListener = (listener) => {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export const sendChatPing = () => {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send('ping')
  }
}
