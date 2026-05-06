<template>
  <div class="chat-page">
    <van-nav-bar :title="chatTitle" />

    <div ref="messageScroller" class="message-scroller">
      <div class="load-older" v-if="!historyFinished">
        <van-button size="small" plain round type="primary" :loading="historyLoading" @click="loadOlder">
          加载更早消息
        </van-button>
      </div>

      <van-empty v-if="!historyLoading && messages.length === 0" description="还没有聊天，先说一句想你吧" />

      <div
        v-for="message in messages"
        :key="message.id"
        class="message-row"
        :class="{ mine: isMine(message) }"
      >
        <img :src="avatarFor(message)" class="chat-avatar" loading="lazy" decoding="async" />
        <div class="bubble-wrap">
          <div class="message-time">{{ formatTime(message.createdAt) }}</div>
          <div class="message-bubble" :class="message.type">
            <template v-if="message.type === 'text'">
              <div class="text-message">{{ message.content }}</div>
            </template>

            <template v-else-if="message.type === 'image'">
              <img
                class="image-message"
                :src="imageThumb(message)"
                loading="lazy"
                decoding="async"
                @click="previewImage(message)"
              />
            </template>

            <template v-else-if="message.type === 'audio'">
              <div class="audio-message">
                <van-icon name="volume-o" />
                <audio :src="mediaUrl(message.mediaUrl)" controls preload="metadata" />
                <span v-if="messageDuration(message)" class="duration">{{ messageDuration(message) }}s</span>
              </div>
            </template>

            <template v-else-if="message.type === 'sticker'">
              <img class="sticker-message" :src="message.mediaUrl" loading="lazy" decoding="async" />
            </template>
          </div>
          <div v-if="isMine(message)" class="message-status">
            {{ message.status === 'read' ? '已读' : '已发送' }}
          </div>
        </div>
      </div>
    </div>

    <div class="composer safe-area-bottom">
      <div class="tool-row" v-if="showStickerPanel">
        <button
          v-for="sticker in stickers"
          :key="sticker.name"
          type="button"
          class="sticker-option"
          @click="sendSticker(sticker)"
        >
          <img :src="sticker.url" :alt="sticker.name" />
        </button>
      </div>

      <div class="input-row">
        <button type="button" class="icon-button" @click="toggleStickerPanel">
          <van-icon name="smile-o" />
        </button>
        <button type="button" class="icon-button" @click="pickImage">
          <van-icon name="photo-o" />
        </button>
        <van-field
          v-model="draft"
          class="chat-input"
          rows="1"
          autosize
          type="textarea"
          maxlength="2000"
          placeholder="想说点什么..."
          @keyup.enter.exact.prevent="sendText"
        />
        <button
          type="button"
          class="record-button"
          :class="{ recording: isRecording }"
          :disabled="mediaSending"
          @click="toggleRecording"
        >
          <van-icon :name="isRecording ? 'pause-circle-o' : 'music-o'" />
          <span>{{ isRecording ? `${recordSeconds}s` : '语音' }}</span>
        </button>
        <van-button size="small" type="primary" round :loading="sending" @click="sendText">
          发送
        </van-button>
      </div>
    </div>

    <input ref="imageInput" type="file" accept="image/*" class="hidden-input" @change="onImageSelected" />

    <van-image-preview
      v-model:show="showPreview"
      :images="previewImages"
      :closeable="true"
      :show-index="false"
      :max-zoom="3"
    />
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { showToast } from 'vant'
import dayjs from 'dayjs'
import api from '../api'
import { useUserStore } from '../stores/user'
import { addChatSocketListener } from '../utils/chatSocket'
import { normalizeMediaUrl, toPreviewUrl, toThumbUrl } from '../utils/media'
import stickerLove from '../assets/stickers/love.svg'
import stickerMissU from '../assets/stickers/miss-u.svg'
import stickerHug from '../assets/stickers/hug.svg'
import stickerKiss from '../assets/stickers/kiss.svg'
import stickerFlower from '../assets/stickers/flower.svg'
import stickerStar from '../assets/stickers/star.svg'

const userStore = useUserStore()

const pageSize = 20
const pageNum = ref(1)
const historyFinished = ref(false)
const historyLoading = ref(false)
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const mediaSending = ref(false)
const showStickerPanel = ref(false)
const imageInput = ref(null)
const messageScroller = ref(null)
const showPreview = ref(false)
const previewImages = ref([])

const isRecording = ref(false)
const recordSeconds = ref(0)
let mediaRecorder = null
let recordStream = null
let recordChunks = []
let recordTimer = null
let recordStartedAt = 0

const stickers = [
  { name: 'LOVE', url: stickerLove },
  { name: 'MISS U', url: stickerMissU },
  { name: 'HUG', url: stickerHug },
  { name: 'KISS', url: stickerKiss },
  { name: 'FLOWER', url: stickerFlower },
  { name: 'SHINE', url: stickerStar }
]

const chatTitle = computed(() => userStore.partner?.nickname ? `和 ${userStore.partner.nickname} 聊天` : '聊天')

const isMine = (message) => Number(message.fromUserId) === Number(userStore.user?.id)

const avatarFor = (message) => {
  if (isMine(message)) return userStore.user?.avatar || '/default-avatar.png'
  return userStore.partner?.avatar || message.fromUser?.avatar || '/default-avatar.png'
}

const parseExtra = (message) => {
  if (!message?.extra) return {}
  if (typeof message.extra === 'object') return message.extra
  try {
    return JSON.parse(message.extra)
  } catch (e) {
    return {}
  }
}

const mediaUrl = (url) => normalizeMediaUrl(url)

const imageThumb = (message) => {
  const extra = parseExtra(message)
  return mediaUrl(extra.thumbnail || toThumbUrl(message.mediaUrl))
}

const messageDuration = (message) => parseExtra(message).duration

const formatTime = (time) => {
  if (!time) return ''
  const value = dayjs(time)
  return value.isSame(dayjs(), 'day') ? value.format('HH:mm') : value.format('MM-DD HH:mm')
}

const sortMessages = (list) => {
  return [...list].sort((a, b) => {
    const ta = new Date(a.createdAt || 0).getTime()
    const tb = new Date(b.createdAt || 0).getTime()
    if (ta !== tb) return ta - tb
    return Number(a.id || 0) - Number(b.id || 0)
  })
}

const mergeMessage = (message) => {
  if (!message?.id) return
  const map = new Map(messages.value.map(item => [item.id, item]))
  map.set(message.id, { ...map.get(message.id), ...message })
  messages.value = sortMessages(Array.from(map.values()))
}

const loadMessages = async (reset = false) => {
  if (historyLoading.value) return
  historyLoading.value = true
  try {
    const targetPage = reset ? 1 : pageNum.value
    const res = await api.chat.getMessages(targetPage, pageSize)
    if (res.code === 200) {
      const records = (res.data?.records || []).slice().reverse()
      historyFinished.value = records.length < pageSize
      if (reset) {
        messages.value = records
        pageNum.value = 2
        await markRead()
        await nextTick()
        scrollToBottom()
      } else {
        messages.value = sortMessages([...records, ...messages.value])
        pageNum.value += 1
      }
    }
  } catch (e) {
    showToast('聊天记录加载失败')
  } finally {
    historyLoading.value = false
  }
}

const loadOlder = () => loadMessages(false)

const markRead = async () => {
  try {
    await api.chat.markRead()
  } catch (e) {
    console.error('标记已读失败', e)
  }
}

const scrollToBottom = () => {
  const el = messageScroller.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

const sendText = async () => {
  const content = draft.value.trim()
  if (!content || sending.value) return
  sending.value = true
  try {
    const res = await api.chat.sendMessage({ type: 'text', content })
    if (res.code === 200) {
      draft.value = ''
      mergeMessage(res.data)
      await nextTick()
      scrollToBottom()
    } else {
      showToast(res.message || '发送失败')
    }
  } catch (e) {
    showToast('发送失败')
  } finally {
    sending.value = false
  }
}

const sendSticker = async (sticker) => {
  if (sending.value) return
  sending.value = true
  try {
    const res = await api.chat.sendMessage({ type: 'sticker', mediaUrl: sticker.url })
    if (res.code === 200) {
      showStickerPanel.value = false
      mergeMessage(res.data)
      await nextTick()
      scrollToBottom()
    }
  } catch (e) {
    showToast('表情包发送失败')
  } finally {
    sending.value = false
  }
}

const pickImage = () => {
  imageInput.value?.click()
}

const onImageSelected = async (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    showToast('请选择图片')
    return
  }
  mediaSending.value = true
  try {
    const res = await api.chat.sendMedia('image', file)
    if (res.code === 200) {
      mergeMessage(res.data)
      await nextTick()
      scrollToBottom()
    } else {
      showToast(res.message || '图片发送失败')
    }
  } catch (e) {
    showToast('图片发送失败')
  } finally {
    mediaSending.value = false
  }
}

const toggleStickerPanel = () => {
  showStickerPanel.value = !showStickerPanel.value
}

const toggleRecording = async () => {
  if (isRecording.value) {
    stopRecording()
  } else {
    await startRecording()
  }
}

const startRecording = async () => {
  if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
    showToast('当前浏览器不支持录音')
    return
  }
  try {
    recordStream = await navigator.mediaDevices.getUserMedia({ audio: true })
    recordChunks = []
    const mimeType = MediaRecorder.isTypeSupported('audio/webm') ? 'audio/webm' : ''
    mediaRecorder = new MediaRecorder(recordStream, mimeType ? { mimeType } : undefined)
    mediaRecorder.ondataavailable = event => {
      if (event.data?.size) recordChunks.push(event.data)
    }
    mediaRecorder.onstop = sendRecordedVoice
    recordStartedAt = Date.now()
    recordSeconds.value = 0
    recordTimer = setInterval(() => {
      recordSeconds.value = Math.floor((Date.now() - recordStartedAt) / 1000)
    }, 500)
    mediaRecorder.start()
    isRecording.value = true
  } catch (e) {
    showToast('无法访问麦克风')
    cleanupRecorder()
  }
}

const stopRecording = () => {
  if (!mediaRecorder || mediaRecorder.state === 'inactive') {
    cleanupRecorder()
    return
  }
  mediaRecorder.stop()
  isRecording.value = false
  clearInterval(recordTimer)
}

const sendRecordedVoice = async () => {
  const duration = Math.max(1, Math.round((Date.now() - recordStartedAt) / 1000))
  const blob = new Blob(recordChunks, { type: recordChunks[0]?.type || 'audio/webm' })
  cleanupRecorder()
  if (!blob.size) {
    showToast('录音为空')
    return
  }
  const file = new File([blob], `voice-${Date.now()}.webm`, { type: blob.type || 'audio/webm' })
  mediaSending.value = true
  try {
    const res = await api.chat.sendMedia('audio', file, { duration })
    if (res.code === 200) {
      mergeMessage(res.data)
      await nextTick()
      scrollToBottom()
    } else {
      showToast(res.message || '语音发送失败')
    }
  } catch (e) {
    showToast('语音发送失败')
  } finally {
    mediaSending.value = false
    recordSeconds.value = 0
  }
}

const cleanupRecorder = () => {
  clearInterval(recordTimer)
  recordTimer = null
  mediaRecorder = null
  recordStream?.getTracks().forEach(track => track.stop())
  recordStream = null
  recordChunks = []
  isRecording.value = false
}

const previewImage = (message) => {
  previewImages.value = [mediaUrl(toPreviewUrl(message.mediaUrl))]
  showPreview.value = true
}

const removeSocketListener = addChatSocketListener(async (event, data) => {
  if (event === 'message:new') {
    mergeMessage(data)
    await nextTick()
    scrollToBottom()
    if (Number(data?.toUserId) === Number(userStore.user?.id)) {
      await markRead()
    }
  }
  if (event === 'message:read' && Number(data?.readerUserId) !== Number(userStore.user?.id)) {
    messages.value = messages.value.map(message => {
      if (isMine(message)) {
        return { ...message, status: 'read' }
      }
      return message
    })
  }
})

onMounted(async () => {
  if (!userStore.partner) {
    userStore.fetchPartner()
  }
  await loadMessages(true)
})

onUnmounted(() => {
  removeSocketListener()
  cleanupRecorder()
})
</script>

<style scoped>
.chat-page {
  min-height: 100vh;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(255, 247, 248, 0.92)),
    radial-gradient(circle at 20% 0%, rgba(255, 184, 198, 0.34), transparent 36%),
    #fff7f8;
  padding-bottom: 128px;
}

.message-scroller {
  height: calc(100vh - 184px);
  overflow-y: auto;
  padding: 14px 12px 20px;
  scroll-behavior: smooth;
}

.load-older {
  display: flex;
  justify-content: center;
  padding: 4px 0 12px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 14px;
}

.message-row.mine {
  flex-direction: row-reverse;
}

.chat-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 107, 129, 0.35);
  flex: 0 0 auto;
}

.bubble-wrap {
  max-width: min(74vw, 520px);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.message-row.mine .bubble-wrap {
  align-items: flex-end;
}

.message-time,
.message-status {
  font-size: 11px;
  color: var(--text-lighter);
  margin: 0 4px 4px;
}

.message-status {
  margin-top: 4px;
}

.message-bubble {
  border-radius: 16px;
  padding: 10px 12px;
  background: #fff;
  box-shadow: 0 4px 18px rgba(255, 107, 129, 0.13);
  color: var(--text-color);
  overflow: hidden;
}

.message-row.mine .message-bubble {
  background: linear-gradient(135deg, #ff6b81 0%, #ff8ea0 100%);
  color: #fff;
}

.message-bubble.image,
.message-bubble.sticker {
  padding: 4px;
  background: transparent;
  box-shadow: none;
}

.text-message {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.55;
  font-size: 15px;
}

.image-message {
  display: block;
  width: min(58vw, 320px);
  max-height: 360px;
  object-fit: cover;
  border-radius: 14px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.12);
}

.sticker-message {
  display: block;
  width: 112px;
  height: 112px;
  object-fit: contain;
  filter: drop-shadow(0 6px 12px rgba(255, 107, 129, 0.16));
}

.audio-message {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: min(64vw, 320px);
}

.audio-message audio {
  width: 180px;
  height: 32px;
}

.duration {
  font-size: 12px;
  opacity: 0.78;
}

.composer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 50px;
  z-index: 20;
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid var(--border-color);
  box-shadow: 0 -8px 24px rgba(255, 107, 129, 0.12);
}

.tool-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
  padding: 10px 12px 2px;
}

.sticker-option,
.icon-button,
.record-button {
  border: 0;
  background: transparent;
  color: var(--primary-color);
}

.sticker-option {
  aspect-ratio: 1;
  border-radius: 12px;
  background: var(--bg-color);
  padding: 6px;
}

.sticker-option img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 10px 10px;
}

.icon-button {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  border-radius: 50%;
  background: #fff3f5;
  font-size: 19px;
}

.chat-input {
  flex: 1;
  padding: 0;
  border-radius: 18px;
  overflow: hidden;
  background: #f8f8f8;
}

:deep(.chat-input .van-field__body) {
  min-height: 36px;
  padding: 0 10px;
  align-items: center;
}

.record-button {
  height: 34px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0 9px;
  border-radius: 17px;
  background: #fff3f5;
  font-size: 13px;
}

.record-button.recording {
  color: #fff;
  background: var(--primary-color);
}

.hidden-input {
  display: none;
}

@media (min-width: 768px) {
  .message-scroller {
    max-width: 760px;
    margin: 0 auto;
  }

  .composer {
    left: 50%;
    transform: translateX(-50%);
    max-width: 760px;
    border-left: 1px solid var(--border-color);
    border-right: 1px solid var(--border-color);
  }
}
</style>
