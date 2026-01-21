<template>
  <div class="anniversary-page">
    <van-nav-bar title="纪念日" />
    
    <!-- 在一起计时 -->
    <div class="together-card card" v-if="together">
      <div class="together-icon">💕</div>
      <div class="together-title">{{ together.title }}</div>
      <div class="together-days">
        <span class="highlight-number">{{ together.days }}</span>
        <span class="days-label">天</span>
      </div>
      <div class="together-date">从 {{ together.date }} 开始</div>
    </div>
    
    <!-- 分类 Tab -->
    <van-tabs v-model:active="activeTab" color="#ff6b81" line-width="40">
      <van-tab title="纪念日" name="past" />
      <van-tab title="倒计时" name="future" />
    </van-tabs>
    
    <!-- 列表 -->
    <div class="anniversary-list">
      <div 
        v-for="item in filteredList" 
        :key="item.id" 
        class="anniversary-item card"
        @click="showDetail(item)"
      >
        <div class="item-icon">{{ item.icon || '❤️' }}</div>
        <div class="item-info">
          <div class="item-title">{{ item.title }}</div>
          <div class="item-date">{{ item.date }}</div>
        </div>
        <div class="item-days">
          <span class="days-value">{{ getDaysDisplay(item) }}</span>
          <span class="days-unit">{{ item.type === 'past' ? '天' : '' }}</span>
        </div>
      </div>
      
      <van-empty v-if="!filteredList.length" description="还没有纪念日" />
    </div>
    
    <!-- 添加按钮 -->
    <div class="add-btn" @click="showAddPopup = true">
      <van-icon name="plus" />
    </div>
    
    <!-- 添加/编辑弹窗 -->
    <van-popup 
      v-model:show="showAddPopup" 
      position="bottom" 
      round
      :style="{ height: '60%' }"
    >
      <div class="popup-content">
        <div class="popup-header">
          <span>{{ editingItem ? '编辑' : '添加' }}纪念日</span>
          <van-icon name="cross" @click="showAddPopup = false" />
        </div>
        
        <van-form @submit="onSubmit">
          <van-cell-group inset>
            <van-field
              v-model="form.title"
              label="名称"
              placeholder="如：生日、相识纪念日"
              :rules="[{ required: true, message: '请输入名称' }]"
            />
            <van-field
              v-model="form.date"
              label="日期"
              placeholder="选择日期"
              readonly
              is-link
              @click="showDatePicker = true"
              :rules="[{ required: true, message: '请选择日期' }]"
            />
            <van-field
              v-model="form.icon"
              label="图标"
              placeholder="选择一个 emoji"
            >
              <template #input>
                <div class="icon-selector">
                  <span 
                    v-for="emoji in emojis" 
                    :key="emoji"
                    class="emoji-item"
                    :class="{ active: form.icon === emoji }"
                    @click="form.icon = emoji"
                  >
                    {{ emoji }}
                  </span>
                </div>
              </template>
            </van-field>
            <van-field label="类型">
              <template #input>
                <van-radio-group v-model="form.type" direction="horizontal">
                  <van-radio name="past">纪念日</van-radio>
                  <van-radio name="future">倒计时</van-radio>
                </van-radio-group>
              </template>
            </van-field>
            <van-field label="每年重复">
              <template #input>
                <van-switch v-model="form.repeatYearly" size="20" active-color="#ff6b81" />
              </template>
            </van-field>
          </van-cell-group>
          
          <div class="form-actions">
            <van-button round block type="primary" native-type="submit">
              保存
            </van-button>
            <van-button 
              v-if="editingItem && editingItem.title !== '在一起'" 
              round 
              block 
              plain 
              type="danger"
              @click="onDelete"
            >
              删除
            </van-button>
          </div>
        </van-form>
      </div>
    </van-popup>
    
    <!-- 日期选择器 -->
    <van-popup v-model:show="showDatePicker" position="bottom" round>
      <van-date-picker
        v-model="datePickerValue"
        title="选择日期"
        @confirm="onDateConfirm"
        @cancel="showDatePicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { showToast, showConfirmDialog } from 'vant'
import api from '../api'
import dayjs from 'dayjs'

const activeTab = ref('past')
const anniversaries = ref([])
const together = ref(null)
const showAddPopup = ref(false)
const showDatePicker = ref(false)
const editingItem = ref(null)

const emojis = ['❤️', '💕', '🎂', '🎉', '✈️', '🏠', '💍', '🌹', '⭐', '🎁']

const form = ref({
  title: '',
  date: '',
  icon: '❤️',
  type: 'past',
  repeatYearly: true
})

const datePickerValue = ref([
  dayjs().format('YYYY'),
  dayjs().format('MM'),
  dayjs().format('DD')
])

// 过滤列表
const filteredList = computed(() => {
  return anniversaries.value.filter(item => item.type === activeTab.value)
})

// 获取天数显示
const getDaysDisplay = (item) => {
  return item.daysText || `${item.days}天`
}

// 加载数据
const fetchData = async () => {
  try {
    // 获取在一起天数
    const togetherRes = await api.anniversary.getTogether()
    if (togetherRes.code === 200) {
      together.value = togetherRes.data
    }
    
    // 获取所有纪念日
    const listRes = await api.anniversary.getAll()
    if (listRes.code === 200) {
      anniversaries.value = listRes.data
    }
  } catch (e) {
    console.error('加载失败', e)
  }
}

// 日期确认
const onDateConfirm = ({ selectedValues }) => {
  form.value.date = selectedValues.join('-')
  showDatePicker.value = false
}

// 显示详情/编辑
const showDetail = (item) => {
  editingItem.value = item
  form.value = {
    title: item.title,
    date: item.date,
    icon: item.icon || '❤️',
    type: item.type,
    repeatYearly: item.repeatYearly
  }
  const d = dayjs(item.date)
  datePickerValue.value = [d.format('YYYY'), d.format('MM'), d.format('DD')]
  showAddPopup.value = true
}

// 提交
const onSubmit = async () => {
  try {
    let res
    if (editingItem.value) {
      res = await api.anniversary.update(editingItem.value.id, form.value)
    } else {
      res = await api.anniversary.add(form.value)
    }
    
    if (res.code === 200) {
      showToast({ message: '保存成功', icon: 'success' })
      showAddPopup.value = false
      resetForm()
      fetchData()
    } else {
      showToast(res.message || '保存失败')
    }
  } catch (e) {
    showToast('保存失败')
  }
}

// 删除
const onDelete = async () => {
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: '确定要删除这个纪念日吗？'
    })
    
    const res = await api.anniversary.delete(editingItem.value.id)
    if (res.code === 200) {
      showToast({ message: '删除成功', icon: 'success' })
      showAddPopup.value = false
      resetForm()
      fetchData()
    } else {
      showToast(res.message || '删除失败')
    }
  } catch (e) {
    // 取消删除
  }
}

// 重置表单
const resetForm = () => {
  editingItem.value = null
  form.value = {
    title: '',
    date: '',
    icon: '❤️',
    type: 'past',
    repeatYearly: true
  }
}

onMounted(fetchData)
</script>

<style scoped>
.anniversary-page {
  min-height: 100vh;
  padding-bottom: 70px;
}

.together-card {
  text-align: center;
  padding: 24px;
  margin: 12px;
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
}

.together-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.together-title {
  font-size: 16px;
  color: #fff;
  margin-bottom: 12px;
}

.together-days {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
}

.together-days .highlight-number {
  font-size: 48px;
  color: #fff;
}

.together-days .days-label {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
}

.together-date {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 8px;
}

.anniversary-list {
  padding: 12px;
}

.anniversary-item {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.item-icon {
  font-size: 32px;
}

.item-info {
  flex: 1;
}

.item-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-color);
}

.item-date {
  font-size: 13px;
  color: var(--text-lighter);
  margin-top: 4px;
}

.item-days {
  text-align: right;
}

.days-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--primary-color);
}

.days-unit {
  font-size: 12px;
  color: var(--text-light);
}

.add-btn {
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
  height: 100%;
  display: flex;
  flex-direction: column;
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: 600;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 16px;
}

.icon-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.emoji-item {
  font-size: 24px;
  padding: 4px;
  border-radius: 8px;
  cursor: pointer;
}

.emoji-item.active {
  background: var(--bg-color);
  box-shadow: 0 0 0 2px var(--primary-color);
}

.form-actions {
  margin-top: auto;
  padding: 16px 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

:deep(.van-button--primary) {
  background: linear-gradient(135deg, #ff6b81 0%, #e84a5f 100%);
  border: none;
}
</style>
