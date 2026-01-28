<template>
  <div class="moments-page">
    <van-nav-bar title="动态广场" left-arrow @click-left="router.back()">
      <template #right>
        <van-icon name="plus" size="20" color="#ff6b81" @click="goCreate" />
      </template>
    </van-nav-bar>
    
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadMore"
      >
        <div v-for="moment in moments" :key="moment.id" v-memo="[moment.id, moment.likes, moment.liked, moment.comments?.length]" class="moment-card card">
          <div class="moment-header">
            <img :src="moment.user?.avatar" class="avatar" loading="lazy" decoding="async" />
            <div class="user-info">
              <div class="nickname">{{ moment.user?.nickname }}</div>
              <div class="time-text">{{ formatTime(moment.createdAt) }}</div>
            </div>
            <van-icon
              v-if="moment.userId === userStore.user?.id || userStore.isOwner"
              name="ellipsis"
              @click="showActions(moment)"
            />
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
              <img
                v-if="media.type === 'image'"
                :src="normalizeMediaUrl(media.thumbnail) || toThumbUrl(normalizeMediaUrl(media.url))"
                loading="lazy"
                decoding="async"
                @error="onImageError($event, media.url)"
              />
              <video v-else :src="normalizeMediaUrl(media.url)" preload="metadata" playsinline />
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
            <div v-for="comment in moment.comments" :key="comment.id" class="comment-item" @click="openCommentActions(moment, comment)">
              <span class="comment-user">
                {{ comment.user?.nickname || comment.user?.username }}<template v-if="comment.replyToUser"> 回复 {{ comment.replyToUser?.nickname || comment.replyToUser?.username }}</template>：
              </span>
              <span class="comment-text">{{ comment.content }}</span>
            </div>
          </div>
        </div>
        
        <van-empty v-if="!loading && !moments.length" description="还没有公开动态，快来发布吧~" />
      </van-list>
    </van-pull-refresh>
    
    <van-action-sheet
      v-model:show="showActionSheet"
      :actions="actions"
      cancel-text="取消"
      @select="onActionSelect"
    />
    
    <van-popup v-model:show="showCommentPopup" position="bottom" round>
      <div class="comment-input-wrapper">
        <div v-if="replyToComment" class="replying-bar">
          <span class="replying-text">回复 @{{ replyToComment.user?.nickname || replyToComment.user?.username }}</span>
          <van-icon name="cross" @click="clearReply" />
        </div>
        <van-field
          v-model="commentText"
          :placeholder="replyToComment ? `回复 @${replyToComment.user?.nickname || replyToComment.user?.username}` : '写评论...'"
          autofocus
          @keyup.enter="submitComment"
        >
          <template #button>
            <van-button size="small" type="primary" @click="submitComment">发送</van-button>
          </template>
        </van-field>
      </div>
    </van-popup>

    <van-action-sheet
      v-model:show="showCommentActionSheet"
      :actions="commentActions"
      cancel-text="取消"
      @select="onCommentActionSelect"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog, showImagePreview } from 'vant'
import { useUserStore } from '../stores/user'
import api from '../api'
import dayjs from 'dayjs'
import { toThumbUrl, normalizeMediaUrl } from '../utils/media'

const router = useRouter()
const userStore = useUserStore()

const moments = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)

const showActionSheet = ref(false)
const currentMoment = ref(null)
const actions = [
  { name: '删除', color: '#ee0a24' }
]

const showCommentPopup = ref(false)
const commentText = ref('')
const replyToComment = ref(null)

const showCommentActionSheet = ref(false)
const currentComment = ref(null)
const currentCommentMoment = ref(null)
const commentActions = ref([])

const loadMore = async () => {
  try {
    const res = await api.moments.getPublicList(pageNum.value)
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

const showActions = (moment) => {
  currentMoment.value = moment
  showActionSheet.value = true
}

const onActionSelect = async (action) => {
  if (action.name === '删除') {
    try {
      await showConfirmDialog({
        title: '确认删除',
        message: '删除后无法恢复，确定要删除吗？'
      })
      const res = await api.moments.delete(currentMoment.value.id)
      if (res.code === 200) {
        moments.value = moments.value.filter(m => m.id !== currentMoment.value.id)
        showToast('删除成功')
      }
    } catch (e) {
    }
  }
}

const showCommentInput = (moment) => {
  currentMoment.value = moment
  commentText.value = ''
  replyToComment.value = null
  showCommentPopup.value = true
}

const submitComment = async () => {
  if (!commentText.value.trim()) return
  try {
    const res = await api.moments.addComment(currentMoment.value.id, commentText.value, replyToComment.value?.id)
    if (res.code === 200) {
      currentMoment.value.comments.push(res.data)
      showCommentPopup.value = false
      replyToComment.value = null
      showToast('评论成功')
    }
  } catch (e) {
    console.error('评论失败', e)
  }
}

const clearReply = () => {
  replyToComment.value = null
}

const openCommentActions = (moment, comment) => {
  currentCommentMoment.value = moment
  currentComment.value = comment
  const actions = [{ name: '回复' }]
  const canDelete = userStore.isOwner || moment.userId === userStore.user?.id || comment.userId === userStore.user?.id
  if (canDelete) {
    actions.push({ name: '删除', color: '#ee0a24' })
  }
  commentActions.value = actions
  showCommentActionSheet.value = true
}

const onCommentActionSelect = async (action) => {
  if (!currentComment.value || !currentCommentMoment.value) return
  if (action.name === '回复') {
    currentMoment.value = currentCommentMoment.value
    commentText.value = ''
    replyToComment.value = currentComment.value
    showCommentPopup.value = true
    return
  }
  if (action.name === '删除') {
    try {
      const res = await api.moments.deleteComment(currentComment.value.id)
      if (res.code === 200) {
        currentCommentMoment.value.comments = currentCommentMoment.value.comments.filter(c => c.id !== currentComment.value.id)
        showToast('删除成功')
      }
    } catch (e) {
    }
  }
}

const onMediaClick = (mediaList, index) => {
  const target = Array.isArray(mediaList) ? mediaList[index] : null
  if (!target || target.type !== 'image') return
  const images = mediaList.filter(m => m.type === 'image').map(m => normalizeMediaUrl(m.url))
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

const onImageError = (e, rawUrl) => {
  const el = e?.target
  if (!el || el.dataset.fallbackApplied === '1') return
  el.dataset.fallbackApplied = '1'
  el.src = normalizeMediaUrl(rawUrl)
}

const goCreate = () => router.push({ name: 'momentCreate' })
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

.comment-input-wrapper {
  padding: 12px;
}

:deep(.van-field__button) {
  padding-left: 8px;
}
</style>
