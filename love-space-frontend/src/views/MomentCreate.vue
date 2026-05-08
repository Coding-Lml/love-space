<template>
  <div class="create-page">
    <van-nav-bar
      title="发布动态"
      left-arrow
      @click-left="router.back()"
    >
      <template #right>
        <van-button 
          type="primary" 
          size="small" 
          round 
          :loading="submitting"
          @click="submit"
        >
          发布
        </van-button>
      </template>
    </van-nav-bar>
    
    <div class="create-content">
      <section class="compose-hero">
        <div class="compose-kicker">NEW MOMENT</div>
        <h1>把这一刻投进你们的信息流</h1>
        <p>文字、照片、视频和地点会一起成为一条可以被喜欢和回应的动态。</p>
      </section>

      <section class="compose-card">
        <!-- 文字内容 -->
        <van-field
          v-model="form.content"
          class="content-field"
          type="textarea"
          placeholder="记录这一刻的美好..."
          rows="5"
          maxlength="500"
          show-word-limit
          autosize
        />
        <div class="compose-tools">
          <button type="button" class="emoji-trigger" @click="showEmojiPanel = !showEmojiPanel">
            <van-icon name="smile-o" />
            <span>表情</span>
          </button>
        </div>
        <EmojiPanel
          v-if="showEmojiPanel"
          class="moment-emoji-panel"
          @select-emoji="insertEmoji"
        />

        <!-- 图片/视频上传 -->
        <div class="upload-section">
          <div class="section-label">媒体</div>
          <van-uploader
            v-model="fileList"
            multiple
            :max-count="9"
            :max-size="100 * 1024 * 1024"
            accept="image/*,video/*"
            :before-read="beforeRead"
            :after-read="afterRead"
            @oversize="onOversize"
          >
            <div class="upload-trigger">
              <van-icon name="photo-o" size="24" />
              <span>添加图片/视频</span>
            </div>
          </van-uploader>
        </div>

        <div v-if="submitting" class="publish-status" aria-live="polite">
          <div class="publish-status-row">
            <span>{{ publishStatus.message }}</span>
            <span v-if="publishStatus.percent !== null">{{ publishStatus.percent }}%</span>
          </div>
          <van-progress
            v-if="publishStatus.percent !== null"
            :percentage="publishStatus.percent"
            stroke-width="6"
            color="#ff5a7a"
            :show-pivot="false"
          />
        </div>
      </section>
      
      <!-- 位置 -->
      <van-cell-group inset class="publish-options">
        <van-cell title="公开到广场" label="打开后，广场里的访客也能看到这条动态">
          <template #right-icon>
            <van-switch v-model="form.isPublic" size="22px" active-color="#10a7a1" />
          </template>
        </van-cell>
        <van-field
          v-model="form.location"
          label="位置"
          placeholder="添加位置"
          left-icon="location-o"
        />
      </van-cell-group>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import api from '../api'
import { prepareUploadFiles } from '../utils/upload'
import EmojiPanel from '../components/EmojiPanel.vue'
import { appendToken } from '../utils/emojiCatalog'

const router = useRouter()

const form = ref({
  content: '',
  location: '',
  isPublic: false
})
const fileList = ref([])
const submitting = ref(false)
const showEmojiPanel = ref(false)
const publishStatus = ref({
  phase: 'idle',
  percent: null,
  message: ''
})

const setPublishStatus = (phase, message, percent = null) => {
  publishStatus.value = { phase, message, percent }
}

const insertEmoji = emoji => {
  form.value.content = appendToken(form.value.content, emoji)
}

const beforeRead = (file) => {
  const files = Array.isArray(file) ? file : [file]

  const allowedImageTypes = new Set([
    'image/jpeg',
    'image/png',
    'image/gif',
    'image/webp'
  ])
  const allowedVideoTypes = new Set([
    'video/mp4',
    'video/quicktime',
    'video/webm',
    'video/x-msvideo'
  ])

  const invalid = files.find((item) => {
    const raw = item?.file || item
    const type = raw?.type
    if (!type) return true
    if (type.startsWith('image/')) return !allowedImageTypes.has(type)
    if (type.startsWith('video/')) return !allowedVideoTypes.has(type)
    return true
  })

  if (invalid) {
    showToast('仅支持 jpg/png/gif/webp 图片与 mp4/mov/webm/avi 视频')
    return false
  }

  return true
}

// 文件读取后
const afterRead = (file) => {
  if (Array.isArray(file)) {
    file.forEach(f => { f.status = 'done' })
  } else {
    file.status = 'done'
  }
}

// 文件超出大小
const onOversize = () => {
  showToast('文件大小不能超过 100MB')
}

// 提交
const submit = async () => {
  if (!form.value.content && fileList.value.length === 0) {
    showToast('请输入内容或上传图片/视频')
    return
  }
  
  submitting.value = true
  setPublishStatus('preparing', '正在处理图片…', fileList.value.length ? 0 : null)
  showLoadingToast({ message: '正在处理图片…', forbidClick: true, duration: 0 })
  
  try {
    const preparedFiles = await prepareUploadFiles(fileList.value, progress => {
      setPublishStatus('preparing', `正在处理图片 ${progress.current}/${progress.total}`, progress.percent)
    })

    // 构建 FormData
    const formData = new FormData()
    if (form.value.content) {
      formData.append('content', form.value.content)
    }
    if (form.value.location) {
      formData.append('location', form.value.location)
    }
    formData.append('visibility', form.value.isPublic ? 'PUBLIC' : 'SPACE')
    
    // 添加文件
    preparedFiles.forEach(file => {
      if (file.file) {
        formData.append('files', file.file)
      }
    })
    
    setPublishStatus('uploading', '正在上传…', preparedFiles.length ? 0 : null)
    showLoadingToast({ message: '正在上传…', forbidClick: true, duration: 0 })
    const res = await api.moments.create(formData, {
      onUploadProgress: event => {
        if (!event.total) {
          setPublishStatus('uploading', '正在上传…', null)
          return
        }
        const percent = Math.min(95, Math.round((event.loaded / event.total) * 100))
        setPublishStatus('uploading', '正在上传…', percent)
      }
    })
    setPublishStatus('publishing', '正在发布…', 95)
    closeToast()
    
    if (res.code === 200) {
      setPublishStatus('success', '发布成功', 100)
      showToast({ message: '发布成功 💕', icon: 'success' })
      router.back()
    } else {
      setPublishStatus('error', res.message || '发布失败', null)
      showToast(res.message || '发布失败')
    }
  } catch (e) {
    closeToast()
    const message = e?.code === 'ECONNABORTED' ? '上传超时，请检查网络后重试' : '发布失败，请重试'
    setPublishStatus('error', message, null)
    showToast(message)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.create-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 20% 0%, rgba(255, 122, 89, 0.18), transparent 30%),
    linear-gradient(180deg, #fff8f4, #fff 58%, #f7fbfa);
}

.create-content {
  padding: 14px 12px 88px;
}

.compose-hero {
  margin-bottom: 12px;
  padding: 18px;
  color: #fff;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(40, 35, 47, 0.94), rgba(240, 82, 141, 0.9)),
    #28232f;
  box-shadow: var(--shadow-strong);
}

.compose-kicker {
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 1.7px;
  opacity: 0.72;
}

.compose-hero h1 {
  margin: 8px 0;
  font-size: 23px;
  line-height: 1.16;
  letter-spacing: 0;
}

.compose-hero p {
  max-width: 270px;
  font-size: 13px;
  line-height: 1.55;
  opacity: 0.78;
}

.compose-card {
  padding: 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--shadow);
}

.content-field {
  overflow: hidden;
  border-radius: 8px;
  background: #fff8f4;
}

.compose-tools {
  display: flex;
  justify-content: flex-end;
  padding: 8px 2px 0;
}

.emoji-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 30px;
  padding: 0 10px;
  border: 0;
  border-radius: 8px;
  background: var(--surface-soft);
  color: var(--primary-color);
  font-size: 13px;
  font-weight: 900;
}

.moment-emoji-panel {
  margin: 8px -2px 0;
  border-radius: 8px;
  overflow: hidden;
}

:deep(.van-field__control) {
  font-size: 16px;
  line-height: 1.6;
}

.upload-section {
  margin: 16px 0 4px;
}

.section-label {
  margin: 0 0 8px 2px;
  color: var(--text-light);
  font-size: 12px;
  font-weight: 900;
}

.publish-status {
  margin: 12px 0 16px;
  padding: 12px;
  border-radius: 8px;
  background: linear-gradient(180deg, #fff8f4, #fff);
  border: 1px solid var(--border-color);
}

.publish-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--text-light);
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  background: linear-gradient(180deg, var(--surface-soft), #fff);
  border: 1px dashed rgba(255, 90, 122, 0.36);
  border-radius: 8px;
  color: var(--primary-color);
  font-size: 12px;
  font-weight: 800;
  gap: 4px;
}

:deep(.van-uploader__preview-image) {
  border-radius: 8px;
}

.publish-options {
  margin-top: 12px;
  overflow: hidden;
  border-radius: 8px;
  box-shadow: var(--shadow);
}

:deep(.van-nav-bar__right) .van-button {
  background: linear-gradient(135deg, var(--accent-warm) 0%, var(--primary-color) 100%);
  border: none;
  padding: 0 16px;
}
</style>
