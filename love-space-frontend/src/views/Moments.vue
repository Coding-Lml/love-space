<template>
  <div class="moments-page">
    <van-nav-bar title="我们的动态">
      <template #right>
        <van-icon name="fire-o" size="20" color="#ff5a7a" @click="goSquare" />
      </template>
    </van-nav-bar>
    
    <!-- 下拉刷新 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <section class="feed-hero">
        <div>
          <div class="feed-kicker">LOVE FEED</div>
          <h1>把今天变成一条会发光的动态</h1>
          <p>{{ feedStats }}</p>
        </div>
        <button type="button" class="hero-publish" @click="goCreate">
          <van-icon name="plus" />
          <span>发布</span>
        </button>
      </section>

      <van-skeleton
        v-if="loading && pageNum === 1 && !moments.length"
        title
        :row="4"
        class="feed-skeleton"
      />
      <!-- 动态列表 -->
      <van-list
        v-else
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadMore"
      >
        <div
          v-for="moment in moments"
          :key="moment.id"
          v-memo="[moment.id, moment.likes, moment.liked, moment.comments?.length]"
          class="moment-card card"
          :style="{ '--moment-accent': getMomentAccent(moment) }"
        >
          <div class="moment-accent-line"></div>
          <!-- 用户信息 -->
          <div class="moment-header">
            <img :src="moment.user?.avatar" class="avatar" loading="lazy" decoding="async" />
            <div class="user-info">
              <div class="nickname">{{ moment.user?.nickname }}</div>
              <div class="subline">
                <div class="time-text">{{ formatTime(moment.createdAt) }}</div>
                <span class="visibility-pill" :class="{ public: moment.visibility === 'PUBLIC' }">
                  {{ visibilityLabel(moment.visibility) }}
                </span>
              </div>
            </div>
            <button
              v-if="moment.userId === userStore.user?.id || userStore.isOwner"
              type="button"
              class="more-button"
              @click="showActions(moment)"
            >
              <van-icon name="ellipsis" />
            </button>
          </div>
          
          <!-- 内容 -->
          <div class="moment-content" v-if="moment.content">
            {{ moment.content }}
          </div>
          <div v-else-if="describeMomentMedia(moment.mediaList)" class="moment-content moment-content-muted">
            分享了{{ describeMomentMedia(moment.mediaList) }}
          </div>
          
          <!-- 媒体文件 -->
          <div v-if="describeMomentMedia(moment.mediaList)" class="media-caption">
            <van-icon name="photo-o" />
            <span>{{ describeMomentMedia(moment.mediaList) }}</span>
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
          
          <!-- 位置 -->
          <div class="moment-location" v-if="moment.location">
            <van-icon name="location-o" />
            {{ moment.location }}
          </div>
          
          <!-- 互动栏 -->
          <div class="moment-actions">
            <button type="button" class="action-item" :class="{ liked: moment.liked }" @click="toggleLike(moment)">
              <van-icon :name="moment.liked ? 'like' : 'like-o'" />
              <span>{{ moment.likes || '赞' }}</span>
            </button>
            <button type="button" class="action-item" @click="showCommentInput(moment)">
              <van-icon name="comment-o" />
              <span>{{ moment.comments?.length || '评论' }}</span>
            </button>
          </div>
          
          <!-- 评论列表 -->
          <div class="comments-section" v-if="moment.comments?.length">
            <div class="comments-title">互动回声</div>
            <div v-for="comment in moment.comments" :key="comment.id" class="comment-item" @click="openCommentActions(moment, comment)">
              <span class="comment-user">
                {{ comment.user?.nickname }}<template v-if="comment.replyToUser"> 回复 {{ comment.replyToUser?.nickname }}</template>：
              </span>
              <span class="comment-text">{{ comment.content }}</span>
            </div>
          </div>
        </div>
        
        <!-- 空状态 -->
        <div v-if="!loading && !moments.length" class="feed-empty card">
          <van-icon name="photo-o" />
          <h2>还没有动态</h2>
          <p>第一条可以是一张照片、一句想念，或者今天最想保存的小事。</p>
          <van-button type="primary" round size="small" @click="goCreate">发布第一条</van-button>
        </div>
      </van-list>
    </van-pull-refresh>
    
    <!-- 发布按钮 -->
    <div class="publish-btn" @click="goCreate">
      <van-icon name="plus" />
    </div>
    
    <!-- 图片预览组件 -->
  <van-image-preview
    v-model:show="showPreview"
    :images="previewImages"
    :start-position="previewIndex"
    :closeable="true"
    :loop="true"
    :max-zoom="3"
    :min-zoom="1"
    :show-index="true"
    @change="onPreviewChange"
  />

  <!-- 操作菜单 -->
  <van-action-sheet
      v-model:show="showActionSheet"
      :actions="actions"
      cancel-text="取消"
      @select="onActionSelect"
    />
    
    <!-- 评论输入框 -->
    <van-popup v-model:show="showCommentPopup" position="bottom" round class="comment-popup">
      <div class="comment-input-wrapper">
        <div class="comment-popup-title">写下你的回应</div>
        <div v-if="replyToComment" class="replying-bar">
          <span class="replying-text">回复 @{{ replyToComment.user?.nickname }}</span>
          <van-icon name="cross" @click="clearReply" />
        </div>
        <div class="input-row">
          <button type="button" class="emoji-toggle" @click="toggleEmoji">
            <span>😊</span>
          </button>
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
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { useUserStore } from '../stores/user'
import api from '../api'
import dayjs from 'dayjs'
import { toThumbUrl, toPreviewUrl, normalizeMediaUrl } from '../utils/media'
import { describeMomentMedia, getMomentAccent, visibilityLabel } from '../utils/feedPresentation'

const router = useRouter()
const userStore = useUserStore()

const moments = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)

const feedStats = computed(() => {
  if (!moments.value.length) return '动态、照片、评论，都会在这里汇成你们的日常现场'
  const mediaCount = moments.value.filter(moment => moment.mediaList?.length).length
  const commentCount = moments.value.reduce((sum, moment) => sum + (moment.comments?.length || 0), 0)
  return `${moments.value.length} 条近况 · ${mediaCount} 条带照片/视频 · ${commentCount} 条回应`
})

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
      moment.likes = moment.liked ? (moment.likes || 0) + 1 : Math.max(0, (moment.likes || 0) - 1)
      if (moment.liked) showToast('收到一颗喜欢')
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
      if (!currentMoment.value.comments) currentMoment.value.comments = []
      currentMoment.value.comments.push(res.data)
      showCommentPopup.value = false
      replyToComment.value = null
      showEmoji.value = false
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

const showPreview = ref(false)
const previewImages = ref([])
const previewIndex = ref(0)

const onPreviewChange = (newIndex) => {
  previewIndex.value = newIndex
}

const onMediaClick = (mediaList, index) => {
  const target = Array.isArray(mediaList) ? mediaList[index] : null
  if (!target || target.type !== 'image') return
  
  // 1. 过滤出所有图片
  const imageList = mediaList.filter(m => m.type === 'image')
  
  // 2. 转换 URL
  previewImages.value = imageList.map(m => {
    const rawUrl = m.url || m.thumbnail
    const fullUrl = normalizeMediaUrl(rawUrl)
    return toPreviewUrl(fullUrl)
  })
  
  // 3. 计算索引
  let startPosition = 0
  for (let i = 0; i < index; i++) {
    if (mediaList[i].type === 'image') {
      startPosition++
    }
  }
  
  previewIndex.value = startPosition
  showPreview.value = true
}

const onImageError = (e, rawUrl) => {
  const el = e?.target
  if (!el || el.dataset.fallbackApplied === '1') return
  el.dataset.fallbackApplied = '1'
  el.src = normalizeMediaUrl(rawUrl)
}

// 键盘切换图片
const handleKeyboard = (e) => {
  if (!showPreview.value) return
  if (e.key === 'ArrowLeft' && previewIndex.value > 0) {
    previewIndex.value--
  } else if (e.key === 'ArrowRight' && previewIndex.value < previewImages.value.length - 1) {
    previewIndex.value++
  } else if (e.key === 'Escape') {
    showPreview.value = false
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeyboard)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyboard)
})

// 发布
const goCreate = () => router.push({ name: 'momentCreate' })
const goSquare = () => router.push({ name: 'square' })
</script>

<style scoped>
.moments-page {
  min-height: 100vh;
  padding-bottom: 84px;
  background:
    radial-gradient(circle at 14% 3%, rgba(255, 122, 89, 0.18), transparent 28%),
    radial-gradient(circle at 86% 0%, rgba(16, 167, 161, 0.18), transparent 30%),
    linear-gradient(180deg, #fff8f4 0%, #fff 48%, #f7fbfa 100%);
}

.feed-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  margin: 14px 12px 6px;
  padding: 18px;
  color: #fff;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(255, 122, 89, 0.96), rgba(240, 82, 141, 0.96) 48%, rgba(16, 167, 161, 0.9)),
    #ff5a7a;
  box-shadow: var(--shadow-strong);
}

.feed-hero > div {
  min-width: 0;
}

.feed-kicker {
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 1.8px;
  opacity: 0.8;
}

.feed-hero h1 {
  max-width: 220px;
  margin: 7px 0;
  font-size: 23px;
  line-height: 1.14;
  letter-spacing: 0;
}

.feed-hero p {
  max-width: 230px;
  font-size: 12px;
  line-height: 1.5;
  opacity: 0.86;
}

.hero-publish {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex: 0 0 auto;
  height: 36px;
  padding: 0 12px;
  color: var(--primary-color);
  font-weight: 800;
  border: 0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 22px rgba(40, 35, 47, 0.16);
}

.moment-card {
  position: relative;
  overflow: hidden;
  margin-bottom: 12px;
  padding-top: 18px;
}

.feed-skeleton {
  margin: 12px;
  padding: 16px;
  border-radius: 8px;
  background: #fff;
}

.moment-accent-line {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 5px;
  background: linear-gradient(90deg, var(--moment-accent), rgba(255, 255, 255, 0));
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
  font-weight: 800;
  color: var(--text-color);
}

.visibility-pill {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 8px;
  border-radius: 8px;
  color: var(--accent-cool);
  background: var(--surface-mint);
  font-size: 11px;
  font-weight: 800;
}

.visibility-pill.public {
  color: var(--primary-color);
  background: #fff0f3;
}

.more-button {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 8px;
  color: var(--text-light);
  background: #f7f4f2;
}

.moment-content {
  font-size: 16px;
  line-height: 1.6;
  color: var(--text-color);
  margin-bottom: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

.moment-content-muted {
  color: var(--text-light);
}

.moment-location {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  padding: 5px 9px;
  border-radius: 8px;
  background: #f7f4f2;
  font-size: 12px;
  color: var(--text-light);
  margin: 8px 0;
}

.media-caption {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 8px;
  color: var(--text-light);
  font-size: 12px;
  font-weight: 800;
}

.moment-actions {
  display: flex;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
  margin-top: 12px;
}

.action-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 82px;
  height: 34px;
  padding: 0 12px;
  border: 0;
  border-radius: 8px;
  background: #f7f4f2;
  font-size: 13px;
  font-weight: 800;
  color: var(--text-light);
  cursor: pointer;
}

.action-item.liked {
  color: #fff;
  background: linear-gradient(135deg, var(--accent-warm), var(--primary-color));
  animation: likePop 0.28s ease-out;
}

@keyframes likePop {
  0% { transform: scale(0.94); }
  70% { transform: scale(1.05); }
  100% { transform: scale(1); }
}

.comments-section {
  margin-top: 12px;
  padding: 12px;
  background: linear-gradient(180deg, #fff8f4, #fff);
  border: 1px solid rgba(255, 122, 89, 0.12);
  border-radius: 8px;
}

.comments-title {
  margin-bottom: 5px;
  color: var(--text-lighter);
  font-size: 11px;
  font-weight: 900;
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
  bottom: 86px;
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, var(--accent-warm) 0%, var(--primary-color) 54%, #f0528d 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28px;
  box-shadow: var(--shadow-strong);
  z-index: 10;
}

.feed-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 34px 20px;
  text-align: center;
}

.feed-empty .van-icon {
  color: var(--primary-color);
  font-size: 34px;
}

.feed-empty h2 {
  font-size: 18px;
  color: var(--text-color);
}

.feed-empty p {
  color: var(--text-light);
  font-size: 13px;
  line-height: 1.6;
}

.comment-input-wrapper {
  padding: 14px 12px 18px;
  background: linear-gradient(180deg, #fff, #fff8f4);
}

.comment-popup-title {
  margin-bottom: 10px;
  color: var(--text-color);
  font-size: 15px;
  font-weight: 900;
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
  width: 38px;
  height: 38px;
  padding: 0;
  border: 0;
  border-radius: 8px;
  background: var(--surface-soft);
  font-size: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.comment-field {
  flex: 1;
  overflow: hidden;
  border-radius: 8px;
  background: #fff;
}

.emoji-panel {
  padding: 8px 4px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  border-top: 1px solid rgba(255, 122, 89, 0.12);
  margin-top: 4px;
}

.emoji-item {
  font-size: 22px;
  padding: 4px;
}

@media (min-width: 768px) {
  .moments-page {
    max-width: 760px;
    margin: 0 auto;
  }
}
</style>
