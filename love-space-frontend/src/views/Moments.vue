<template>
  <div class="moments-page">
    <van-nav-bar title="动态广场" />
    
    <!-- 下拉刷新 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <!-- 动态列表 -->
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadMore"
      >
        <div v-for="moment in moments" :key="moment.id" class="moment-card card">
          <!-- 用户信息 -->
          <div class="moment-header">
            <img :src="moment.user?.avatar" class="avatar" loading="lazy" decoding="async" />
            <div class="user-info">
              <div class="nickname">{{ moment.user?.nickname }}</div>
              <div class="time-text">{{ formatTime(moment.createdAt) }}</div>
            </div>
            <van-icon 
              v-if="moment.userId === userStore.user?.id"
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
              <img v-if="media.type === 'image'" :src="media.thumbnail || toThumbUrl(media.url)" loading="lazy" decoding="async" />
              <video v-else :src="media.url" preload="metadata" playsinline />
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
            <div v-for="comment in moment.comments" :key="comment.id" class="comment-item">
              <span class="comment-user">{{ comment.user?.nickname }}：</span>
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog, showImagePreview } from 'vant'
import { useUserStore } from '../stores/user'
import api from '../api'
import dayjs from 'dayjs'
import { toThumbUrl } from '../utils/media'

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

// 加载数据
const loadMore = async () => {
  try {
    const res = await api.moments.getList(pageNum.value)
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

const onMediaClick = (mediaList, index) => {
  const target = Array.isArray(mediaList) ? mediaList[index] : null
  if (!target || target.type !== 'image') return
  const images = mediaList.filter(m => m.type === 'image').map(m => m.url)
  const startPosition = mediaList.slice(0, index).filter(m => m.type === 'image').length
  showImagePreview({ images, startPosition })
}

// 发布
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
</style>
