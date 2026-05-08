<template>
  <section class="emoji-panel" aria-label="表情面板">
    <div class="emoji-tabs" role="tablist" aria-label="表情分类">
      <button
        v-for="group in groups"
        :key="group.key"
        type="button"
        class="emoji-tab"
        :class="{ active: activeGroupKey === group.key }"
        role="tab"
        :aria-selected="activeGroupKey === group.key"
        @click="activeGroupKey = group.key"
      >
        {{ group.label }}
      </button>
    </div>

    <div class="emoji-grid">
      <button
        v-for="emoji in activeItems"
        :key="`${activeGroupKey}-${emoji.value}`"
        type="button"
        class="emoji-choice"
        :aria-label="emoji.title || emoji.value"
        @click="$emit('select-emoji', emoji)"
      >
        <span class="emoji-glyph">{{ emoji.value }}</span>
        <button
          v-if="quickSend"
          type="button"
          class="emoji-quick"
          :aria-label="`快速发送${emoji.title || emoji.value}`"
          @click.stop="$emit('quick-send-emoji', emoji)"
        >
          <van-icon name="guide-o" />
        </button>
      </button>
    </div>

    <div v-if="stickers.length" class="sticker-section">
      <div class="panel-label">贴纸</div>
      <div class="sticker-grid">
        <button
          v-for="sticker in stickers"
          :key="sticker.name"
          type="button"
          class="sticker-choice"
          @click="$emit('select-sticker', sticker)"
        >
          <img :src="sticker.url" :alt="sticker.name" />
          <span>{{ sticker.name }}</span>
        </button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { emojiGroups } from '../utils/emojiCatalog'

const props = defineProps({
  groups: {
    type: Array,
    default: () => emojiGroups
  },
  stickers: {
    type: Array,
    default: () => []
  },
  quickSend: {
    type: Boolean,
    default: false
  }
})

defineEmits(['select-emoji', 'quick-send-emoji', 'select-sticker'])

const activeGroupKey = ref(props.groups[0]?.key || 'frequent')

const activeItems = computed(() => {
  const activeGroup = props.groups.find(group => group.key === activeGroupKey.value)
  return activeGroup?.items || props.groups[0]?.items || []
})
</script>

<style scoped>
.emoji-panel {
  padding: 10px;
  border-top: 1px solid rgba(255, 122, 89, 0.14);
  background:
    linear-gradient(180deg, rgba(255, 248, 244, 0.96), rgba(255, 255, 255, 0.98));
}

.emoji-tabs {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
  margin-bottom: 10px;
}

.emoji-tab,
.emoji-choice,
.sticker-choice {
  border: 0;
  font: inherit;
  background: transparent;
}

.emoji-tab {
  min-width: 0;
  height: 30px;
  border-radius: 8px;
  color: var(--text-light);
  background: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  font-weight: 900;
}

.emoji-tab.active {
  color: #fff;
  background: linear-gradient(135deg, var(--accent-warm), var(--primary-color));
  box-shadow: 0 8px 18px rgba(255, 90, 122, 0.18);
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
}

.emoji-choice {
  position: relative;
  aspect-ratio: 1;
  min-width: 0;
  border-radius: 8px;
  color: var(--text-color);
  background: #fff;
  box-shadow: 0 6px 18px rgba(40, 35, 47, 0.07);
}

.emoji-glyph {
  font-size: 25px;
  line-height: 1;
}

.emoji-quick {
  position: absolute;
  right: 3px;
  bottom: 3px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border: 0;
  border-radius: 50%;
  color: #fff;
  background: var(--accent-cool);
  font-size: 11px;
}

.sticker-section {
  margin-top: 12px;
}

.panel-label {
  margin-bottom: 8px;
  color: var(--text-light);
  font-size: 12px;
  font-weight: 900;
}

.sticker-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.sticker-choice {
  aspect-ratio: 1;
  min-width: 0;
  padding: 6px;
  border-radius: 8px;
  background: linear-gradient(180deg, var(--surface-soft), #fff);
  color: var(--text-light);
  font-size: 10px;
  font-weight: 900;
}

.sticker-choice img {
  display: block;
  width: 100%;
  height: calc(100% - 16px);
  object-fit: contain;
}

.sticker-choice span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1;
}

@media (min-width: 768px) {
  .emoji-grid {
    grid-template-columns: repeat(8, minmax(0, 1fr));
  }
}
</style>
