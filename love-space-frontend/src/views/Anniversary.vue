<template>
  <div class="anniversary-page">
    <van-nav-bar title="纪念日" />

    <section class="together-card" v-if="together">
      <div class="together-kicker">TOGETHER</div>
      <h1>{{ together.title }}</h1>
      <div class="together-days">
        <span>{{ together.days }}</span>
        <small>天</small>
      </div>
      <p>从 {{ together.date }} 开始，每一天都在继续累计。</p>
    </section>

    <van-skeleton v-else-if="loading" title :row="3" class="anniversary-skeleton" />

    <section class="date-hero">
      <div>
        <div class="date-kicker">IMPORTANT DAYS</div>
        <h2>{{ nextAnniversary?.title || '还没有新的倒计时' }}</h2>
        <p>{{ nextAnniversary ? formatAnniversaryDistance(nextAnniversary) : '添加生日、旅行或任何值得期待的日子。' }}</p>
      </div>
      <button type="button" @click="openCreate">
        <van-icon name="plus" />
      </button>
    </section>

    <div class="anniversary-tabs">
      <button type="button" :class="{ active: activeTab === 'future' }" @click="activeTab = 'future'">
        倒计时 <span>{{ grouped.future.length }}</span>
      </button>
      <button type="button" :class="{ active: activeTab === 'past' }" @click="activeTab = 'past'">
        纪念日 <span>{{ grouped.past.length }}</span>
      </button>
    </div>

    <section class="anniversary-list">
      <article
        v-for="item in filteredList"
        :key="item.id"
        class="anniversary-item"
        :class="{ countdown: item.type === 'future' }"
        @click="showDetail(item)"
      >
        <div class="item-icon">{{ item.icon || '❤️' }}</div>
        <div class="item-info">
          <h2>{{ item.title }}</h2>
          <p>{{ item.date }}</p>
        </div>
        <div class="item-days">
          <strong>{{ formatAnniversaryDistance(item) }}</strong>
          <span>{{ item.repeatYearly ? '每年提醒' : '只记这次' }}</span>
        </div>
      </article>

      <div v-if="!loading && !filteredList.length" class="anniversary-empty">
        <van-icon :name="activeTab === 'future' ? 'underway-o' : 'calendar-o'" />
        <h2>{{ activeTab === 'future' ? '还没有倒计时' : '还没有纪念日' }}</h2>
        <p>{{ activeTab === 'future' ? '添加一个未来的日子，首页会优先提醒。' : '把已经发生的重要日子保存下来。' }}</p>
        <van-button type="primary" round size="small" @click="openCreate">添加</van-button>
      </div>
    </section>

    <button type="button" class="add-btn" @click="openCreate">
      <van-icon name="plus" />
    </button>

    <van-popup
      v-model:show="showAddPopup"
      position="bottom"
      round
      :style="{ height: '68%' }"
      @closed="resetForm"
    >
      <div class="popup-content">
        <div class="popup-header">
          <div>
            <div class="popup-kicker">{{ editingItem ? 'EDIT DATE' : 'NEW DATE' }}</div>
            <h2>{{ editingItem ? '编辑纪念日' : '添加纪念日' }}</h2>
          </div>
          <button type="button" @click="showAddPopup = false">
            <van-icon name="cross" />
          </button>
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
            <van-field v-model="form.icon" label="图标">
              <template #input>
                <div class="icon-selector">
                  <button
                    v-for="emoji in emojis"
                    :key="emoji"
                    type="button"
                    class="emoji-item"
                    :class="{ active: form.icon === emoji }"
                    @click="form.icon = emoji"
                  >
                    {{ emoji }}
                  </button>
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
                <van-switch v-model="form.repeatYearly" size="20" active-color="#10a7a1" />
              </template>
            </van-field>
          </van-cell-group>

          <div class="form-actions">
            <van-button round block type="primary" native-type="submit" :loading="saving">
              保存
            </van-button>
            <van-button
              v-if="editingItem && editingItem.title !== '在一起'"
              round
              block
              plain
              type="danger"
              :loading="deleting"
              @click="onDelete"
            >
              删除
            </van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

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
import { computed, ref, onMounted } from 'vue'
import { showToast, showConfirmDialog } from 'vant'
import api from '../api'
import dayjs from 'dayjs'
import {
  formatAnniversaryDistance,
  selectNextAnniversary,
  splitAnniversaries
} from '../utils/memoryPresentation'

const activeTab = ref('future')
const anniversaries = ref([])
const together = ref(null)
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
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

const grouped = computed(() => splitAnniversaries(anniversaries.value))
const filteredList = computed(() => activeTab.value === 'future' ? grouped.value.future : grouped.value.past)
const nextAnniversary = computed(() => selectNextAnniversary(anniversaries.value))

const fetchData = async () => {
  loading.value = true
  try {
    const [togetherRes, listRes] = await Promise.all([
      api.anniversary.getTogether(),
      api.anniversary.getAll()
    ])
    if (togetherRes.code === 200) {
      together.value = togetherRes.data
    }
    if (listRes.code === 200) {
      anniversaries.value = listRes.data || []
    }
  } catch (e) {
    showToast('纪念日加载失败')
  } finally {
    loading.value = false
  }
}

const onDateConfirm = ({ selectedValues }) => {
  form.value.date = selectedValues.join('-')
  showDatePicker.value = false
}

const openCreate = () => {
  resetForm()
  showAddPopup.value = true
}

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

const onSubmit = async () => {
  saving.value = true
  try {
    const res = editingItem.value
      ? await api.anniversary.update(editingItem.value.id, form.value)
      : await api.anniversary.add(form.value)

    if (res.code === 200) {
      showToast({ message: '保存成功', icon: 'success' })
      showAddPopup.value = false
      await fetchData()
    } else {
      showToast(res.message || '保存失败')
    }
  } catch (e) {
    showToast('保存失败')
  } finally {
    saving.value = false
  }
}

const onDelete = async () => {
  if (!editingItem.value) return
  deleting.value = true
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: '确定要删除这个纪念日吗？'
    })

    const res = await api.anniversary.delete(editingItem.value.id)
    if (res.code === 200) {
      showToast({ message: '删除成功', icon: 'success' })
      showAddPopup.value = false
      await fetchData()
    } else {
      showToast(res.message || '删除失败')
    }
  } catch (e) {
    if (typeof e !== 'string') {
      showToast('删除失败')
    }
  } finally {
    deleting.value = false
  }
}

const resetForm = () => {
  editingItem.value = null
  form.value = {
    title: '',
    date: '',
    icon: '❤️',
    type: activeTab.value === 'future' ? 'future' : 'past',
    repeatYearly: true
  }
  datePickerValue.value = [
    dayjs().format('YYYY'),
    dayjs().format('MM'),
    dayjs().format('DD')
  ]
}

onMounted(fetchData)
</script>

<style scoped>
.anniversary-page {
  min-height: 100vh;
  padding: 12px 12px 88px;
}

.together-card {
  overflow: hidden;
  padding: 18px;
  color: #fff;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(255, 122, 89, 0.96), rgba(240, 82, 141, 0.94) 48%, rgba(16, 167, 161, 0.86)),
    #ff5a7a;
  box-shadow: var(--shadow-strong);
}

.together-kicker,
.date-kicker,
.popup-kicker {
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 1.6px;
  opacity: 0.72;
}

.together-card h1 {
  margin: 6px 0 8px;
  font-size: 22px;
  line-height: 1.12;
  letter-spacing: 0;
}

.together-days {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.together-days span {
  font-size: 46px;
  font-weight: 900;
  line-height: 1;
}

.together-days small {
  font-size: 18px;
}

.together-card p {
  margin-top: 8px;
  font-size: 13px;
  opacity: 0.84;
}

.anniversary-skeleton {
  padding: 16px;
  border-radius: 8px;
  background: #fff;
}

.date-hero,
.anniversary-item,
.anniversary-empty {
  border: 1px solid rgba(255, 122, 89, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--shadow);
}

.date-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin: 12px 0;
  padding: 15px;
}

.date-hero h2 {
  margin: 3px 0;
  font-size: 20px;
  line-height: 1.2;
  letter-spacing: 0;
}

.date-hero p {
  font-size: 13px;
  color: var(--text-light);
}

.date-hero button {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  color: #fff;
  border: 0;
  border-radius: 8px;
  background: var(--accent-cool);
}

.anniversary-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  margin-bottom: 12px;
  padding: 4px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: var(--shadow);
}

.anniversary-tabs button {
  height: 34px;
  font-weight: 800;
  color: var(--text-light);
  border: 0;
  border-radius: 8px;
  background: transparent;
}

.anniversary-tabs button.active {
  color: var(--primary-color);
  background: var(--surface-soft);
}

.anniversary-tabs span {
  margin-left: 4px;
  color: var(--text-lighter);
}

.anniversary-list {
  display: grid;
  gap: 10px;
}

.anniversary-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
}

.anniversary-item.countdown {
  border-color: rgba(16, 167, 161, 0.18);
}

.item-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  font-size: 24px;
  border-radius: 8px;
  background: var(--surface-soft);
}

.anniversary-item.countdown .item-icon {
  background: var(--surface-mint);
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-info h2 {
  margin: 0 0 4px;
  font-size: 16px;
  line-height: 1.25;
  letter-spacing: 0;
}

.item-info p,
.item-days span {
  font-size: 12px;
  color: var(--text-lighter);
}

.item-days {
  display: grid;
  justify-items: end;
  gap: 2px;
  flex: 0 0 auto;
}

.item-days strong {
  color: var(--primary-color);
  font-size: 14px;
  white-space: nowrap;
}

.anniversary-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px 18px;
  text-align: center;
}

.anniversary-empty .van-icon {
  font-size: 36px;
  color: var(--primary-color);
}

.anniversary-empty h2 {
  margin: 10px 0 6px;
  font-size: 18px;
  letter-spacing: 0;
}

.anniversary-empty p {
  color: var(--text-light);
  font-size: 13px;
  line-height: 1.5;
}

.anniversary-empty .van-button {
  margin-top: 14px;
}

.add-btn {
  position: fixed;
  right: 18px;
  bottom: 78px;
  z-index: 10;
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  color: #fff;
  border: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-warm), var(--primary-color));
  box-shadow: var(--shadow-strong);
}

.popup-content {
  padding: 18px 0 28px;
}

.popup-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 0 18px 14px;
}

.popup-header h2 {
  margin-top: 3px;
  font-size: 20px;
  letter-spacing: 0;
}

.popup-header button {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 8px;
  color: var(--text-light);
  background: var(--surface-soft);
}

.icon-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.emoji-item {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(255, 122, 89, 0.16);
  border-radius: 8px;
  background: #fff;
}

.emoji-item.active {
  border-color: var(--primary-color);
  background: var(--surface-soft);
}

.form-actions {
  display: grid;
  gap: 10px;
  padding: 18px;
}

:deep(.van-cell-group--inset) {
  margin: 0 12px;
  border-radius: 8px;
  overflow: hidden;
}

@media (min-width: 768px) {
  .anniversary-page {
    max-width: 760px;
    margin: 0 auto;
  }
}
</style>
