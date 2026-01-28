<template>
  <div class="chat-page">
    <van-nav-bar
      title="聊天"
      left-arrow
      @click-left="router.back()"
    />

    <div class="chat-body">
      <div v-if="!chatStore.connected" class="ws-hint" @click="chatStore.connect">
        {{ chatStore.connecting ? '连接中...' : (chatStore.reconnecting ? `网络断开，${Math.ceil(chatStore.reconnectInMs / 1000)}s 后重连（点此立即重连）` : '连接已断开，点此重连') }}
      </div>
      <div class="history-hint" v-if="chatStore.hasMore">
        <van-button size="small" type="primary" plain @click="loadMore" :loading="chatStore.loadingHistory">
          加载更多
        </van-button>
      </div>

      <div class="messages" ref="listRef">
        <div
          v-for="msg in renderedMessages"
          :key="msg.id"
          :class="['msg-item', msg.fromUserId === userStore.user?.id ? 'from-me' : 'from-partner']"
        >
          <div class="msg-content">
            <template v-if="msg.type === 'text'">
              <span>{{ msg.content }}</span>
            </template>
            <template v-else-if="msg.type === 'image'">
              <img :src="toThumbUrl(msg.mediaUrl)" class="msg-image" loading="lazy" decoding="async" @click="previewImage(msg.mediaUrl)" />
            </template>
            <template v-else-if="msg.type === 'audio'">
              <div class="audio-bubble" @click="playAudio(msg.mediaUrl)">
                <van-icon name="volume-o" size="18" />
                <span class="audio-text">语音</span>
                <span v-if="msg.extraObj && msg.extraObj.duration" class="audio-duration">
                  {{ msg.extraObj.duration }}s
                </span>
              </div>
            </template>
            <template v-else>
              <span>[{{ msg.type }}]</span>
            </template>
          </div>
          <div
            v-if="msg.id === lastMyReadId && msg.fromUserId === userStore.user?.id"
            class="read-hint"
          >
            已读
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input">
      <van-field
        v-model="inputValue"
        rows="1"
        autosize
        type="textarea"
        placeholder="说点什么..."
        maxlength="500"
        show-word-limit
      />
      <div class="input-actions">
        <div class="emoji-toggle" @click="toggleEmoji">
          <span>😊</span>
        </div>
        <van-uploader
          :max-count="1"
          :after-read="afterReadImage"
          :max-size="20 * 1024 * 1024"
          accept="image/*"
          @oversize="onOversize"
        >
          <van-icon name="photo-o" size="22" />
        </van-uploader>
        <div
          class="voice-toggle"
          @touchstart.prevent="startRecord"
          @touchend.prevent="stopRecord"
          @mousedown.prevent="startRecord"
          @mouseup.prevent="stopRecord"
        >
          <van-icon :name="recording ? 'pause-circle-o' : 'volume-o'" size="22" />
        </div>
        <van-button type="primary" size="small" round @click="sendText">
          发送
        </van-button>
      </div>
      <div v-if="showEmoji" class="emoji-panel">
        <span
          v-for="e in emojis"
          :key="e"
          class="emoji-item"
          @click="appendEmoji(e)"
        >
          {{ e }}
        </span>
      </div>
      <div v-if="recording" class="recording-hint">
        正在录音，松开后发送
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showImagePreview, showToast } from 'vant'
import { useUserStore } from '../stores/user'
import { useChatStore } from '../stores/chat'
import api from '../api'
import { toThumbUrl } from '../utils/media'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()

const inputValue = ref('')
const listRef = ref(null)
const showEmoji = ref(false)
const emojis = ['😊', '😍', '🥰', '😭', '😡', '🎂', '🌹', '❤️', '💔', '💤', '😘']
const recording = ref(false)
const mediaRecorder = ref(null)
const recordedChunks = ref([])
const recordingStart = ref(0)
const audioStream = ref(null)

const lastMyReadId = computed(() => {
  const id = userStore.user?.id
  if (!id) return null
  const selfMessages = chatStore.messages.filter(m => m.fromUserId === id)
  if (!selfMessages.length) return null
  const last = selfMessages[selfMessages.length - 1]
  return last.status === 'read' ? last.id : null
})

const renderCount = ref(120)
const renderedMessages = computed(() => {
  const list = chatStore.messages
  if (!Array.isArray(list)) return []
  if (list.length <= renderCount.value) return list
  return list.slice(list.length - renderCount.value)
})

const enrichMessages = () => {
  chatStore.messages.forEach(m => {
    if (m.extra && !m.extraObj) {
      try {
        m.extraObj = JSON.parse(m.extra)
      } catch (e) {
        m.extraObj = null
      }
    }
  })
}

const scrollToBottom = () => {
  nextTick(() => {
    if (!listRef.value) return
    listRef.value.scrollTop = listRef.value.scrollHeight
  })
}

const sendText = () => {
  const text = inputValue.value.trim()
  if (!text) return
  chatStore.sendText(text)
  inputValue.value = ''
  scrollToBottom()
}

const afterReadImage = async file => {
  const raw = file.file || file
  try {
    const res = await api.file.upload(raw)
    if (res.code === 200) {
      chatStore.sendMedia('image', res.data)
      scrollToBottom()
    } else {
      showToast(res.message || '上传失败')
    }
  } catch (e) {
    showToast('上传失败')
  }
}

const onOversize = () => {
  showToast('图片不能超过 20MB')
}

const previewImage = url => {
  if (!url) return
  showImagePreview({
    images: [url],
    startPosition: 0,
    closeable: true,
    closeOnClickOverlay: true,
    closeOnClickImage: true,
    closeOnPopstate: true
  })
}

const toggleEmoji = () => {
  showEmoji.value = !showEmoji.value
}

const appendEmoji = e => {
  inputValue.value += e
}

const playAudio = url => {
  if (!url) return
  const audio = new Audio(url)
  audio.play()
}

const startRecord = async () => {
  if (recording.value) return
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    showToast('当前浏览器不支持语音录制')
    return
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    audioStream.value = stream
    recordedChunks.value = []
    const mr = new MediaRecorder(stream)
    mediaRecorder.value = mr
    mr.ondataavailable = e => {
      if (e.data && e.data.size > 0) {
        recordedChunks.value.push(e.data)
      }
    }
    mr.onstop = async () => {
      if (audioStream.value) {
        audioStream.value.getTracks().forEach(t => t.stop())
        audioStream.value = null
      }
      if (!recordedChunks.value.length) return
      const blob = new Blob(recordedChunks.value, { type: 'audio/webm' })
      const file = new File([blob], `voice-${Date.now()}.webm`, { type: blob.type })
      try {
        const res = await api.file.upload(file)
        if (res.code === 200) {
          const duration = Math.max(1, Math.round((Date.now() - recordingStart.value) / 1000))
          chatStore.sendMedia('audio', res.data, { duration })
          scrollToBottom()
        } else {
          showToast(res.message || '语音上传失败')
        }
      } catch (e) {
        showToast('语音上传失败')
      }
    }
    recordingStart.value = Date.now()
    recording.value = true
    mr.start()
  } catch (e) {
    showToast('无法获取麦克风权限')
  }
}

const stopRecord = () => {
  if (!recording.value) return
  recording.value = false
  if (mediaRecorder.value) {
    mediaRecorder.value.stop()
  }
}

const loadMore = async () => {
  await chatStore.loadHistory()
  renderCount.value = Math.min(chatStore.messages.length, renderCount.value + 40)
  enrichMessages()
}

onMounted(async () => {
  chatStore.connect()
  await chatStore.loadHistory()
  enrichMessages()
  chatStore.setActive(true)
  try {
    const res = await api.chat.markRead()
    if (res.code === 200 && Array.isArray(res.data)) {
      const set = new Set(res.data)
      chatStore.messages.forEach(m => {
        if (set.has(m.id)) {
          m.status = 'read'
        }
      })
    }
  } catch (e) {
  }
  scrollToBottom()
})

onUnmounted(() => {
  chatStore.setActive(false)
  if (mediaRecorder.value && recording.value) {
    mediaRecorder.value.stop()
  }
  if (audioStream.value) {
    audioStream.value.getTracks().forEach(t => t.stop())
    audioStream.value = null
  }
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.chat-body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.ws-hint {
  padding: 8px 12px;
  font-size: 12px;
  color: #666;
  background: #fff7f8;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.history-hint {
  padding: 8px 16px 0;
  text-align: center;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px 12px;
}

.msg-item {
  display: flex;
  margin-bottom: 8px;
}

.msg-item.from-me {
  justify-content: flex-end;
}

.msg-item.from-partner {
  justify-content: flex-start;
}

.msg-content {
  max-width: 70%;
  padding: 8px 10px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.msg-item.from-me .msg-content {
  background: #ff6b81;
  color: #fff;
  border-bottom-right-radius: 2px;
}

.msg-item.from-partner .msg-content {
  background: #fff;
  color: #333;
  border-bottom-left-radius: 2px;
}

.msg-image {
  max-width: 180px;
  max-height: 240px;
  border-radius: 8px;
}

.read-hint {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
  text-align: right;
}

.chat-input {
  padding: 6px 10px 10px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
}

.input-actions :deep(.van-uploader__upload) {
  width: auto;
  height: auto;
  padding: 4px;
  background: transparent;
  border: none;
}

.emoji-toggle {
  padding: 4px;
  font-size: 18px;
}

.voice-toggle {
  padding: 4px;
}

.emoji-panel {
  padding: 8px 4px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.emoji-item {
  font-size: 22px;
  padding: 4px;
}

.recording-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #ff6b81;
}

.audio-bubble {
  display: flex;
  align-items: center;
  gap: 4px;
}

.audio-text {
  font-size: 14px;
}

.audio-duration {
  font-size: 12px;
}
</style>
