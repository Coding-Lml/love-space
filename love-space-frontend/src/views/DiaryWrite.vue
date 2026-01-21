<template>
  <div class="write-page">
    <van-nav-bar
      title="写日记"
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
          保存
        </van-button>
      </template>
    </van-nav-bar>
    
    <div class="write-content">
      <!-- 日期选择 -->
      <van-cell-group inset>
        <van-cell title="日期" :value="form.diaryDate" @click="showDatePicker = true" is-link />
      </van-cell-group>
      
      <!-- 心情选择 -->
      <div class="mood-section card">
        <div class="section-label">今天的心情</div>
        <div class="mood-list">
          <div 
            v-for="(config, key) in moodConfig" 
            :key="key"
            class="mood-item"
            :class="{ active: form.mood === key }"
            @click="form.mood = key"
          >
            <span class="mood-emoji">{{ config.emoji }}</span>
            <span class="mood-text">{{ config.text }}</span>
          </div>
        </div>
      </div>
      
      <!-- 标题 -->
      <van-cell-group inset>
        <van-field
          v-model="form.title"
          placeholder="给日记起个标题（可选）"
        />
      </van-cell-group>
      
      <!-- 内容 -->
      <div class="content-section card">
        <van-field
          v-model="form.content"
          type="textarea"
          placeholder="今天想说些什么..."
          rows="8"
          maxlength="2000"
          show-word-limit
          autosize
        />
      </div>
      
      <!-- 可见性 -->
      <van-cell-group inset>
        <van-cell title="可见范围">
          <template #right-icon>
            <van-switch 
              v-model="isPrivate" 
              size="20"
              active-color="#ff6b81"
            />
            <span class="visibility-text">{{ isPrivate ? '仅自己' : '双方可见' }}</span>
          </template>
        </van-cell>
      </van-cell-group>
    </div>
    
    <!-- 日期选择器 -->
    <van-popup v-model:show="showDatePicker" position="bottom" round>
      <van-date-picker
        v-model="datePickerValue"
        title="选择日期"
        :min-date="minDate"
        :max-date="maxDate"
        @confirm="onDateConfirm"
        @cancel="showDatePicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import api from '../api'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()

const form = ref({
  diaryDate: dayjs().format('YYYY-MM-DD'),
  mood: 'happy',
  title: '',
  content: '',
  visibility: 'both'
})

const isPrivate = ref(false)
const submitting = ref(false)
const showDatePicker = ref(false)
const datePickerValue = ref([
  dayjs().format('YYYY'),
  dayjs().format('MM'),
  dayjs().format('DD')
])

const minDate = new Date(2026, 0, 1)
const maxDate = new Date()

const moodConfig = {
  happy: { emoji: '😊', text: '开心' },
  love: { emoji: '🥰', text: '甜蜜' },
  normal: { emoji: '😐', text: '平静' },
  sad: { emoji: '😢', text: '难过' },
  angry: { emoji: '😤', text: '生气' }
}

// 日期确认
const onDateConfirm = ({ selectedValues }) => {
  form.value.diaryDate = selectedValues.join('-')
  showDatePicker.value = false
}

// 提交
const submit = async () => {
  if (!form.value.content.trim()) {
    showToast('请输入日记内容')
    return
  }
  
  submitting.value = true
  try {
    const data = {
      ...form.value,
      visibility: isPrivate.value ? 'self' : 'both'
    }
    
    const res = await api.diary.write(data)
    if (res.code === 200) {
      showToast({ message: '保存成功 💕', icon: 'success' })
      router.back()
    } else {
      showToast(res.message || '保存失败')
    }
  } catch (e) {
    showToast('保存失败，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  // 如果有传入日期参数
  if (route.query.date) {
    form.value.diaryDate = route.query.date
    const d = dayjs(route.query.date)
    datePickerValue.value = [d.format('YYYY'), d.format('MM'), d.format('DD')]
  }
})
</script>

<style scoped>
.write-page {
  min-height: 100vh;
  background: var(--bg-color);
}

.write-content {
  padding: 12px;
}

.section-label {
  font-size: 14px;
  color: var(--text-light);
  margin-bottom: 12px;
}

.mood-section {
  margin-bottom: 12px;
}

.mood-list {
  display: flex;
  gap: 8px;
}

.mood-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  border-radius: 12px;
  background: var(--bg-color);
  cursor: pointer;
  transition: all 0.2s;
}

.mood-item.active {
  background: linear-gradient(135deg, #ff6b81 0%, #e84a5f 100%);
}

.mood-item.active .mood-text {
  color: #fff;
}

.mood-emoji {
  font-size: 24px;
  margin-bottom: 4px;
}

.mood-text {
  font-size: 12px;
  color: var(--text-light);
}

.content-section {
  margin: 12px 0;
}

.content-section :deep(.van-field) {
  padding: 0;
}

.visibility-text {
  font-size: 14px;
  color: var(--text-light);
  margin-left: 8px;
}

:deep(.van-cell-group--inset) {
  margin-bottom: 12px;
}

:deep(.van-nav-bar__right) .van-button {
  background: linear-gradient(135deg, #ff6b81 0%, #e84a5f 100%);
  border: none;
  padding: 0 16px;
}
</style>
