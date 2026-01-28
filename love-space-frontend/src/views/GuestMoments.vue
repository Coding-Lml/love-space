<template>
  <div class="moments-page">
    <van-nav-bar title="动态" />

    <LoveTimer :dashboard="dashboard" :left-user="couple?.user1" :right-user="couple?.user2" />

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadMore"
      >
        <div v-for="moment in moments" :key="moment.id" v-memo="[moment.id, moment.likes, moment.liked, moment.comments?.length]" class="moment-card card">
          <div class="moment-header">
            <img :src="moment.user?.avatar || '/default-avatar.png'" class="avatar" loading="lazy" decoding="async" />
            <div class="user-info">
              <div class="nickname">
                {{ moment.user?.nickname || moment.user?.username || '游客' }}
                <van-tag v-if="moment.visibility === 'GUEST'" plain type="primary" size="medium">游客</van-tag>
                <van-tag v-else-if="moment.visibility === 'PUBLIC'" plain type="danger" size="medium">公开</van-tag>
              </div>
              <div class="time-text">{{ formatTime(moment.createdAt) }}</div>
            </div>
          </div>

          <div class="moment-content" v-if="moment.content">
            {{ moment.content }}
          </div>

          <div
            v-if="moment.mediaList?.length"
            class="media-grid"
            :class="{
              single: moment.mediaList.length === 1,
              double: moment.mediaList.length === 2
            }"
          >
            <div
              v-for="(media, index) in moment.mediaList"
              :key="media.id"
              class="media-item"
              @click="onMediaClick(moment.mediaList, index)"
            >
              <img v-if="media.type === 'image'" :src="media.thumbnail || toThumbUrl(media.url)" loading="lazy" decoding="async" />
              <video v-else :src="media.url" preload="metadata" playsinline />
            </div>
          </div>

          <div class="moment-location" v-if="moment.location">
            <van-icon name="location-o" />
            {{ moment.location }}
          </div>

          <div class="moment-actions">
            <div class="action-item" @click="toggleLike(moment)">
              <van-icon :name="moment.liked ? 'like' : 'like-o'" :color="moment.liked ? '#ff6b81' : ''" />
              <span>{{ moment.likes || '赞' }}</span>
            </div>
            <div class="action-item" @click="showCommentInput(moment)">
              <van-icon name="comment-o" />
              <span>{{ moment.comments?.length || '评论' }}</span>
            </div>
          </div>

          <div class="comments-section" v-if="moment.comments?.length">
            <div v-for="comment in moment.comments" :key="comment.id" class="comment-item">
              <span class="comment-user">{{ comment.user?.nickname || comment.user?.username }}：</span>
              <span class="comment-text">{{ comment.content }}</span>
            </div>
          </div>
        </div>

        <van-empty v-if="!loading && !moments.length" description="还没有动态，快来发布吧~" />
      </van-list>
    </van-pull-refresh>

    <div class="publish-btn" @click="openPublish">
      <van-icon name="plus" />
    </div>

    <van-popup v-model:show="showPublishPopup" position="bottom" round>
      <div class="popup-content">
        <div class="popup-header">发布动态</div>
        <van-field
          v-model="publishForm.content"
          type="textarea"
          placeholder="写点什么..."
          rows="4"
          maxlength="500"
          show-word-limit
          autosize
        />
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
        <van-field v-model="publishForm.location" label="位置" placeholder="添加位置" left-icon="location-o" />
        <van-button type="primary" block round :loading="submitting" @click="submitPublish">发布</van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showCommentPopup" position="bottom" round>
      <div class="comment-input-wrapper">
        <van-field
          v-model="commentText"
          placeholder="写评论..."
          autofocus
          @keyup.enter="submitComment"
        >
          <template #button>
            <van-button size="small" type="primary" @click="submitComment">发送</van-button>
          </template>
        </van-field>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showToast, showLoadingToast, closeToast, showImagePreview } from 'vant'
import dayjs from 'dayjs'
import api from '../api'
import { toThumbUrl } from '../utils/media'
import LoveTimer from '../components/LoveTimer.vue'

const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const moments = ref([])

const dashboard = ref(null)
const couple = ref(null)

const showPublishPopup = ref(false)
const submitting = ref(false)
const publishForm = ref({
  content: '',
  location: ''
})
const fileList = ref([])

const showCommentPopup = ref(false)
const commentText = ref('')
const currentMoment = ref(null)

const fetchHeader = async () => {
  try {
    const res = await api.guest.getDashboard()
    if (res.code === 200) {
      dashboard.value = res.data?.dashboard || null
      couple.value = res.data?.couple || null
    }
  } catch (e) {
    console.error('获取游客头部数据失败', e)
  }
}

const loadMore = async () => {
  try {
    const res = await api.guest.getMoments(pageNum.value)
    if (res.code === 200) {
      if (pageNum.value === 1) {
        moments.value = res.data.records
      } else {
        moments.value.push(...res.data.records)
      }
      finished.value = res.data.records.length < 10
      pageNum.value++
    }
  } catch (e) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
}

const onRefresh = async () => {
  pageNum.value = 1
  finished.value = false
  await loadMore()
  refreshing.value = false
}

const formatTime = (time) => {
  const now = dayjs()
  const target = dayjs(time)
  const diff = now.diff(target, 'minute')
  if (diff < 1) return '刚刚'
  if (diff < 60) return `${diff}分钟前`
  if (diff < 1440) return `${Math.floor(diff / 60)}小时前`
  if (diff < 2880) return '昨天'
  return target.format('MM-DD HH:mm')
}

const toggleLike = async (moment) => {
  try {
    const res = await api.moments.like(moment.id)
    if (res.code === 200) {
      moment.liked = res.data
      moment.likes = moment.liked ? (moment.likes || 0) + 1 : moment.likes - 1
    }
  } catch (e) {
    console.error('点赞失败', e)
  }
}

const showCommentInput = (moment) => {
  currentMoment.value = moment
  commentText.value = ''
  showCommentPopup.value = true
}

const submitComment = async () => {
  if (!commentText.value.trim()) return
  try {
    const res = await api.moments.addComment(currentMoment.value.id, commentText.value)
    if (res.code === 200) {
      currentMoment.value.comments.push(res.data)
      showCommentPopup.value = false
      showToast('评论成功')
    }
  } catch (e) {
    console.error('评论失败', e)
  }
}

const openPublish = () => {
  publishForm.value = { content: '', location: '' }
  fileList.value = []
  showPublishPopup.value = true
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

const afterRead = (file) => {
  if (Array.isArray(file)) {
    file.forEach(f => { f.status = 'done' })
  } else {
    file.status = 'done'
  }
}

const onOversize = () => {
  showToast('文件大小不能超过 100MB')
}

const submitPublish = async () => {
  if (!publishForm.value.content && fileList.value.length === 0) {
    showToast('请输入内容或上传图片/视频')
    return
  }

  submitting.value = true
  showLoadingToast({ message: '发布中...', forbidClick: true, duration: 0 })

  try {
    const formData = new FormData()
    if (publishForm.value.content) formData.append('content', publishForm.value.content)
    if (publishForm.value.location) formData.append('location', publishForm.value.location)
    fileList.value.forEach(file => {
      if (file.file) formData.append('files', file.file)
    })

    const res = await api.guest.publishMoment(formData)
    closeToast()

    if (res.code === 200) {
      showToast({ message: '发布成功', icon: 'success' })
      showPublishPopup.value = false
      await onRefresh()
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

const onMediaClick = (mediaList, index) => {
  const target = Array.isArray(mediaList) ? mediaList[index] : null
  if (!target || target.type !== 'image') return
  const images = mediaList.filter(m => m.type === 'image').map(m => m.url)
  const startPosition = mediaList.slice(0, index).filter(m => m.type === 'image').length
  showImagePreview({
    images,
    startPosition,
    closeable: true,
    closeOnClickOverlay: true,
    closeOnClickImage: true,
    closeOnPopstate: true
  })
}

onMounted(async () => {
  await fetchHeader()
  await onRefresh()
})
</script>

<style scoped>
.moments-page {
  min-height: 100vh;
  padding-bottom: 70px;
}

.moment-card {
  margin-bottom: 12px;
}

.moment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.user-info {
  flex: 1;
}

.nickname {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-color);
}

.moment-content {
  font-size: 15px;
  line-height: 1.6;
  color: var(--text-color);
  margin-bottom: 12px;
  white-space: pre-wrap;
}

.moment-location {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-lighter);
  margin: 8px 0;
}

.moment-actions {
  display: flex;
  gap: 24px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
  margin-top: 12px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-light);
  cursor: pointer;
}

.comments-section {
  margin-top: 12px;
  padding: 12px;
  background: var(--bg-color);
  border-radius: 8px;
}

.comment-item {
  font-size: 13px;
  line-height: 1.8;
}

.comment-user {
  color: var(--primary-color);
  font-weight: 500;
}

.comment-text {
  color: var(--text-color);
}

.publish-btn {
  position: fixed;
  right: 20px;
  bottom: 80px;
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #ff6b81 0%, #e84a5f 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28px;
  box-shadow: 0 4px 12px rgba(255, 107, 129, 0.4);
}

.popup-content {
  padding: 16px;
}

.popup-header {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-color);
  margin-bottom: 12px;
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

.comment-input-wrapper {
  padding: 12px;
}

:deep(.van-field__button) {
  padding-left: 8px;
}
</style>
