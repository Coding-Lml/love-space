# Memory Hub Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the phase-two memory hub by redesigning Home, Diary, and Anniversary around existing APIs.

**Architecture:** Keep implementation frontend-scoped. Add small pure presentation helpers for diary, anniversary, and home summaries, then wire those helpers into existing Vue single-file views without changing backend contracts.

**Tech Stack:** Vue 3 Composition API, Vite, Vant 4, dayjs, Node built-in test runner.

---

### Task 1: Presentation Helpers

**Files:**
- Create: `love-space-frontend/src/utils/memoryPresentation.js`
- Create: `love-space-frontend/src/utils/memoryPresentation.test.mjs`

- [ ] **Step 1: Write failing tests**

Create tests for diary summary truncation, recent diary selection, anniversary grouping, and next anniversary selection.

- [ ] **Step 2: Verify tests fail**

Run: `node src/utils/memoryPresentation.test.mjs`
Expected: FAIL because `memoryPresentation.js` does not exist.

- [ ] **Step 3: Implement helper functions**

Implement:
- `getDiaryMood`
- `formatDiaryDateParts`
- `summarizeDiaryText`
- `selectRecentDiaries`
- `splitAnniversaries`
- `selectNextAnniversary`
- `formatAnniversaryDistance`

- [ ] **Step 4: Verify tests pass**

Run: `node src/utils/memoryPresentation.test.mjs`
Expected: PASS.

### Task 2: Home Memory Hub

**Files:**
- Modify: `love-space-frontend/src/views/Home.vue`

- [ ] **Step 1: Connect helper functions**

Import memory helpers and fetch recent diaries independently from dashboard data.

- [ ] **Step 2: Redesign template**

Replace the old home cards with a memory hub layout: hero, quick actions, next anniversary, recent moments, recent diaries, chat action, and stable empty states.

- [ ] **Step 3: Redesign styles**

Add scoped styles matching phase-one visual language and tabbar-safe spacing.

### Task 3: Diary Deep Entry

**Files:**
- Modify: `love-space-frontend/src/views/Diary.vue`

- [ ] **Step 1: Connect helper functions**

Use diary mood/date/summary helpers in list and selected-day rendering.

- [ ] **Step 2: Redesign template**

Add a calm hero, segmented list/calendar control, skeletons, list empty state, calendar selected-day state, and retry action.

- [ ] **Step 3: Redesign styles**

Update list, calendar, selected diary, empty state, and floating write button styles.

### Task 4: Anniversary Deep Entry

**Files:**
- Modify: `love-space-frontend/src/views/Anniversary.vue`

- [ ] **Step 1: Connect helper functions**

Use anniversary grouping and distance helpers for tabs and card labels.

- [ ] **Step 2: Redesign template**

Add stronger together card, improved tabs/sections, empty states, and clearer add/edit popup.

- [ ] **Step 3: Redesign styles**

Update card, list, empty state, popup, emoji selector, and action styles.

### Task 5: Verification

**Files:**
- Test: `love-space-frontend/src/utils/memoryPresentation.test.mjs`
- Test: existing frontend build

- [ ] **Step 1: Run focused tests**

Run:
- `node src/utils/feedPresentation.test.mjs`
- `node src/utils/upload.test.mjs`
- `node src/utils/memoryPresentation.test.mjs`

Expected: all pass.

- [ ] **Step 2: Run production build**

Run: `npm run build`
Expected: Vite build succeeds.

- [ ] **Step 3: Check git status**

Run: `git status --short`
Expected: only intended source/docs changes plus ignored/untracked `.superpowers/` if still present.
