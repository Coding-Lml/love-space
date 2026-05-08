import assert from 'node:assert/strict'
import test from 'node:test'

import { appendToken, emojiGroups, flattenEmojiGroups } from './emojiCatalog.js'

test('emoji catalog exposes grouped built-in emoji', () => {
  assert.equal(Array.isArray(emojiGroups), true)
  assert.ok(emojiGroups.length >= 5)
  assert.ok(emojiGroups.every(group => group.key && group.label && Array.isArray(group.items)))

  const flat = flattenEmojiGroups()
  assert.ok(flat.length >= 30)
  assert.ok(flat.some(item => item.value === '🥰'))
})

test('appendToken appends emoji without changing existing spacing', () => {
  assert.equal(appendToken('想你', '🥰'), '想你🥰')
  assert.equal(appendToken('', '🥰'), '🥰')
  assert.equal(appendToken('晚安 ', '💤'), '晚安 💤')
})
