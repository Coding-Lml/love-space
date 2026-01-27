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
      <!-- 文字内容 -->
      <van-field
        v-model="form.content"
        type="textarea"
        placeholder="记录这一刻的美好..."
        rows="5"
        maxlength="500"
        show-word-limit
        autosize
      />
      
      <!-- 图片/视频上传 -->
      <div class="upload-section">
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
      
      <!-- 位置 -->
      <van-cell-group inset>
        <van-cell title="公开到广场">
          <template #right-icon>
            <van-switch v-model="form.isPublic" size="22px" />
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

const router = useRouter()

const form = ref({
  content: '',
  location: '',
  isPublic: false
})
const fileList = ref([])
const submitting = ref(false)

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
  showLoadingToast({ message: '发布中...', forbidClick: true, duration: 0 })
  
  try {
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
    fileList.value.forEach(file => {
      if (file.file) {
        formData.append('files', file.file)
      }
    })
    
    const res = await api.moments.create(formData)
    closeToast()
    
    if (res.code === 200) {
      showToast({ message: '发布成功 💕', icon: 'success' })
      router.back()
    } else {
      showToast(res.message || '发布失败')
    }
  } catch (e) {
    closeToast()
    showToast('发布失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.create-page {
  min-height: 100vh;
  background: #fff;
}

.create-content {
  padding: 16px;
}

:deep(.van-field__control) {
  font-size: 16px;
  line-height: 1.6;
}

.upload-section {
  margin: 16px 0;
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  background: var(--bg-color);
  border-radius: 8px;
  color: var(--text-lighter);
  font-size: 12px;
  gap: 4px;
}

:deep(.van-uploader__preview-image) {
  border-radius: 8px;
}

:deep(.van-nav-bar__right) .van-button {
  background: linear-gradient(135deg, #ff6b81 0%, #e84a5f 100%);
  border: none;
  padding: 0 16px;
}
</style>
