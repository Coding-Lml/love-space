# Emoji Panel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a shared built-in emoji panel for diary writing, chat, and moment publishing.

**Architecture:** Use one catalog utility for grouped built-in emoji and one reusable Vue component for rendering the panel. Diary and moment pages append selected emoji to existing text content; chat appends by default and exposes a quick-send action that uses the existing text message endpoint.

**Tech Stack:** Vue 3 Composition API, Vant, Vite, existing Node-based frontend utility tests.

---

## File Structure

- Create `love-space-frontend/src/utils/emojiCatalog.js`: grouped emoji metadata and pure helpers.
- Create `love-space-frontend/src/utils/emojiCatalog.test.mjs`: tests for catalog shape and insertion behavior.
- Create `love-space-frontend/src/components/EmojiPanel.vue`: shared panel UI.
- Modify `love-space-frontend/src/views/DiaryWrite.vue`: mount panel and append emoji to diary content.
- Modify `love-space-frontend/src/views/MomentCreate.vue`: mount panel and append emoji to moment content.
- Modify `love-space-frontend/src/views/Chat.vue`: replace sticker-only drawer with the shared panel, keep existing stickers, add quick emoji send.
- Modify `.gitignore`: ignore `.superpowers/` visual brainstorming artifacts.

### Task 1: Catalog And Helper Tests

- [ ] **Step 1: Write failing tests**

Create `love-space-frontend/src/utils/emojiCatalog.test.mjs` with:

```js
import assert from 'node:assert/strict'
import { appendToken, emojiGroups, flattenEmojiGroups } from './emojiCatalog.js'

assert.equal(Array.isArray(emojiGroups), true)
assert.ok(emojiGroups.length >= 5)
assert.ok(emojiGroups.every(group => group.key && group.label && Array.isArray(group.items)))

const flat = flattenEmojiGroups()
assert.ok(flat.length >= 30)
assert.ok(flat.some(item => item.value === '🥰'))
assert.equal(appendToken('想你', '🥰'), '想你🥰')
assert.equal(appendToken('', '🥰'), '🥰')
assert.equal(appendToken('晚安 ', '💤'), '晚安 💤')

console.log('emojiCatalog tests passed')
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `node love-space-frontend/src/utils/emojiCatalog.test.mjs`

Expected: FAIL because `emojiCatalog.js` does not exist yet.

- [ ] **Step 3: Implement catalog helpers**

Create `love-space-frontend/src/utils/emojiCatalog.js` exporting `emojiGroups`, `flattenEmojiGroups`, and `appendToken`.

- [ ] **Step 4: Run the test and verify it passes**

Run: `node love-space-frontend/src/utils/emojiCatalog.test.mjs`

Expected: PASS and prints `emojiCatalog tests passed`.

### Task 2: Shared Emoji Panel

- [ ] **Step 1: Implement `EmojiPanel.vue`**

Create a focused component with tab buttons for groups, an emoji grid, optional sticker grid, and optional quick-send buttons for chat.

- [ ] **Step 2: Build-check the component**

Run: `cd love-space-frontend && npm run build`

Expected: PASS with Vite production output.

### Task 3: Diary And Moment Integration

- [ ] **Step 1: Add the panel to `DiaryWrite.vue`**

Import `EmojiPanel` and `appendToken`, add `showEmojiPanel`, append selected emoji to `form.content`, and style the trigger near the content field.

- [ ] **Step 2: Add the panel to `MomentCreate.vue`**

Use the same component and insertion helper for `form.content`.

- [ ] **Step 3: Verify utility tests and build**

Run: `node love-space-frontend/src/utils/emojiCatalog.test.mjs && cd love-space-frontend && npm run build`

Expected: PASS.

### Task 4: Chat Integration

- [ ] **Step 1: Replace the sticker-only drawer**

Import `EmojiPanel`, pass existing sticker entries as the optional sticker section, append selected emoji to `draft`, and keep existing `sendSticker`.

- [ ] **Step 2: Add quick emoji sending**

Add `quickSendEmoji(emoji)` that calls `api.chat.sendMessage({ type: 'text', content: emoji.value })`, then merges the returned message and scrolls to bottom.

- [ ] **Step 3: Verify utility tests and build**

Run: `node love-space-frontend/src/utils/emojiCatalog.test.mjs && cd love-space-frontend && npm run build`

Expected: PASS.

### Task 5: Browser Verification And Release

- [ ] **Step 1: Run a local frontend server**

Run: `cd love-space-frontend && npm run dev -- --host 127.0.0.1`

Expected: Vite serves a local URL.

- [ ] **Step 2: Inspect diary, moment, and chat pages**

Open the local app in the browser and verify the emoji trigger and panel do not overlap the main composer or submit controls.

- [ ] **Step 3: Commit and push**

Run:

```bash
git add .gitignore docs/superpowers/specs/2026-05-08-emoji-panel-design.md docs/superpowers/plans/2026-05-08-emoji-panel.md love-space-frontend/src
git commit -m "feat: add shared emoji panel"
git push
```

- [ ] **Step 4: Deploy frontend**

Use the existing deployment path from `README.md` and `love-space-frontend/deploy.sh`. Because this feature is frontend-only, backend deployment is not required unless the production deploy process rebuilds both services together.
