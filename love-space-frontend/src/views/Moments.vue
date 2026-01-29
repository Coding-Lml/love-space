<template>
  <div class="moments-page">
    <van-nav-bar title="我们的动态">
      <template #right>
        <van-icon name="fire-o" size="20" color="#ff6b81" @click="goSquare" />
      </template>
    </van-nav-bar>
    
    <!-- 下拉刷新 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <!-- 动态列表 -->
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadMore"
      >
        <div v-for="moment in moments" :key="moment.id" v-memo="[moment.id, moment.likes, moment.liked, moment.comments?.length]" class="moment-card card">
          <!-- 用户信息 -->
          <div class="moment-header">
            <img :src="moment.user?.avatar" class="avatar" loading="lazy" decoding="async" />
            <div class="user-info">
              <div class="nickname">{{ moment.user?.nickname }}</div>
              <div class="subline">
                <div class="time-text">{{ formatTime(moment.createdAt) }}</div>
                <van-tag v-if="moment.visibility === 'PUBLIC'" type="danger" plain size="medium">公开</van-tag>
              </div>
            </div>
            <van-icon 
              v-if="moment.userId === userStore.user?.id || userStore.isOwner"
              name="ellipsis" 
              @click="showActions(moment)" 
            />
          </div>
          
          <!-- 内容 -->
          <div class="moment-content" v-if="moment.content">
            {{ moment.content }}
          </div>
          
          <!-- 媒体文件 -->
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
          
          <!-- 位置 -->
          <div class="moment-location" v-if="moment.location">
            <van-icon name="location-o" />
            {{ moment.location }}
          </div>
          
          <!-- 互动栏 -->
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
          
          <!-- 评论列表 -->
          <div class="comments-section" v-if="moment.comments?.length">
            <div v-for="comment in moment.comments" :key="comment.id" class="comment-item" @click="openCommentActions(moment, comment)">
              <span class="comment-user">
                {{ comment.user?.nickname }}<template v-if="comment.replyToUser"> 回复 {{ comment.replyToUser?.nickname }}</template>：
              </span>
              <span class="comment-text">{{ comment.content }}</span>
            </div>
          </div>
        </div>
        
        <!-- 空状态 -->
        <van-empty v-if="!loading && !moments.length" description="还没有动态，快来发布吧~" />
      </van-list>
    </van-pull-refresh>
    
    <!-- 发布按钮 -->
    <div class="publish-btn" @click="goCreate">
      <van-icon name="plus" />
    </div>
    
    <!-- 操作菜单 -->
    <van-action-sheet
      v-model:show="showActionSheet"
      :actions="actions"
      cancel-text="取消"
      @select="onActionSelect"
    />
    
    <!-- 评论输入框 -->
    <van-popup v-model:show="showCommentPopup" position="bottom" round>
      <div class="comment-input-wrapper">
        <div v-if="replyToComment" class="replying-bar">
          <span class="replying-text">回复 @{{ replyToComment.user?.nickname }}</span>
          <van-icon name="cross" @click="clearReply" />
        </div>
        <div class="input-row">
          <div class="emoji-toggle" @click="toggleEmoji">
            <span>😊</span>
          </div>
          <van-field
            v-model="commentText"
            :placeholder="replyToComment ? `回复 @${replyToComment.user?.nickname}` : '写评论...'"
            autofocus
            @keyup.enter="submitComment"
            class="comment-field"
          >
            <template #button>
              <van-button size="small" type="primary" :loading="commentSubmitting" :disabled="commentSubmitting" @click="submitComment">发送</van-button>
            </template>
          </van-field>
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
const commentSubmitting = ref(false)
const showEmoji = ref(false)
const emojis = ['😊', '😍', '🥰', '😭', '😡', '🎂', '🌹', '❤️', '💔', '💤', '😘']

const toggleEmoji = () => {
  showEmoji.value = !showEmoji.value
}

const appendEmoji = e => {
  commentText.value += e
}

const showCommentActionSheet = ref(false)
const currentComment = ref(null)
const currentCommentMoment = ref(null)
const commentActions = ref([])

const mergeUniqueById = (existing, incoming) => {
  const map = new Map()
  for (const item of existing || []) {
    if (item && item.id != null) map.set(item.id, item)
  }
  for (const item of incoming || []) {
    if (item && item.id != null) map.set(item.id, item)
  }
  return Array.from(map.values())
}

const requesting = ref(false)

// 加载数据
const loadMore = async () => {
  if (requesting.value) return
  requesting.value = true
  loading.value = true
  try {
    const res = await api.moments.getList(pageNum.value)
    if (res.code === 200) {
      if (pageNum.value === 1) {
        moments.value = res.data.records
      } else {
        moments.value = mergeUniqueById(moments.value, res.data.records)
      }
      finished.value = res.data.records.length < 10
      pageNum.value++
    } else {
      showToast(res.message || '加载失败')
    }
  } catch (e) {
    console.error('加载失败', e)
    showToast('加载失败，请稍后重试')
  } finally {
    loading.value = false
    requesting.value = false
  }
}

// 下拉刷新
const onRefresh = async () => {
  pageNum.value = 1
  finished.value = false
  await loadMore()
  refreshing.value = false
}

// 格式化时间
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

// 点赞
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

// 显示操作菜单
const showActions = (moment) => {
  currentMoment.value = moment
  showActionSheet.value = true
}

// 操作选择
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
      // 取消删除
    }
  }
}

// 评论
const showCommentInput = (moment) => {
  currentMoment.value = moment
  commentText.value = ''
  replyToComment.value = null
  showCommentPopup.value = true
}

const submitComment = async () => {
  if (commentSubmitting.value) return
  if (!commentText.value.trim()) return
  commentSubmitting.value = true
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
  } finally {
    commentSubmitting.value = false
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

// 发布
const goCreate = () => router.push({ name: 'momentCreate' })
const goSquare = () => router.push({ name: 'square' })
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

.subline {
  display: flex;
  align-items: center;
  gap: 8px;
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

.comment-input-wrapper {
  padding: 12px;
}

:deep(.van-field__button) {
  padding-left: 8px;
}

.replying-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px 10px;
  color: var(--text-light);
  font-size: 13px;
}

.replying-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 10px;
}

.input-row {
  display: flex;
  align-items: center;
}

.emoji-toggle {
  padding: 0 8px;
  font-size: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.comment-field {
  flex: 1;
}

.emoji-panel {
  padding: 8px 4px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  border-top: 1px solid #f5f5f5;
  margin-top: 4px;
}

.emoji-item {
  font-size: 22px;
  padding: 4px;
}
</style>
