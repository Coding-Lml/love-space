# Upload Performance Experience Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve feed responsiveness and publish/upload feedback without changing the core product flow.

**Architecture:** Keep existing Vue/Vant pages and Spring multipart endpoints. Add one frontend media preparation utility, pass upload progress through the API wrapper, update publish pages to use staged feedback, and make backend controllers fail fast on upload errors.

**Tech Stack:** Vue 3, Vite, Vant, Axios, Spring Boot, MyBatis-Plus.

---

### Task 1: Media Preparation Utility

**Files:**
- Create: `love-space-frontend/src/utils/upload.js`
- Modify: `love-space-frontend/src/views/MomentCreate.vue`
- Modify: `love-space-frontend/src/views/GuestMoments.vue`

- [x] Create `prepareUploadFiles(files, onProgress)` in `love-space-frontend/src/utils/upload.js`.
- [x] For JPEG/PNG/WebP images larger than 900 KB, resize with canvas to max side 1600 px and quality 0.82.
- [x] Leave GIFs and videos untouched.
- [x] Return Vant uploader file items with `file` replaced by the prepared `File`.
- [x] In `MomentCreate.vue` and `GuestMoments.vue`, call the utility before building `FormData`.

### Task 2: Upload Progress API

**Files:**
- Modify: `love-space-frontend/src/api/index.js`
- Modify: `love-space-frontend/src/views/MomentCreate.vue`
- Modify: `love-space-frontend/src/views/GuestMoments.vue`

- [x] Allow `api.moments.create(formData, config)` and `api.guest.publishMoment(formData, config)`.
- [x] Pass `onUploadProgress` from the publish pages to Axios.
- [x] Compute percent from `progressEvent.loaded / progressEvent.total` when `total` is available.
- [x] Show a stable message when `total` is not available.

### Task 3: Publish Feedback UI

**Files:**
- Modify: `love-space-frontend/src/views/MomentCreate.vue`
- Modify: `love-space-frontend/src/views/GuestMoments.vue`

- [x] Add a `publishStatus` ref with `phase`, `percent`, and `message`.
- [x] Show an inline Vant progress bar while submitting.
- [x] Use messages for preparing, uploading, publishing, success, and failure.
- [x] Reset status when opening a new publish flow or leaving the page.

### Task 4: Feed First-Load Responsiveness

**Files:**
- Modify: `love-space-frontend/src/views/GuestMoments.vue`
- Optional Modify: `love-space-frontend/src/views/Moments.vue`
- Optional Modify: `love-space-frontend/src/views/Square.vue`

- [x] Change guest page mount to run `fetchHeader()` and `onRefresh()` concurrently.
- [x] Add a lightweight skeleton for first load when `loading && pageNum === 1 && !moments.length`.
- [x] Keep infinite scroll behavior unchanged.

### Task 5: Backend Upload Failure Guard

**Files:**
- Modify: `love-space-backend/src/main/java/com/lovespace/controller/MomentController.java`
- Modify: `love-space-backend/src/main/java/com/lovespace/controller/GuestController.java`

- [x] If `fileService.uploadFile(file)` returns non-200, immediately return `Result.error(uploadResult.getMessage())`.
- [x] If media type is unknown after upload, return `Result.error("不支持的文件类型")`.
- [x] Leave service methods and database schema unchanged.

### Task 6: Verification

**Commands:**
- `node --test src/utils/upload.test.mjs` in `love-space-frontend`
- `npm run build` in `love-space-frontend`
- `./mvnw -q -DskipTests compile` in `love-space-backend`

- [x] Verify all commands pass.
- [ ] Manually test text-only publish, image publish, mixed image/video publish, invalid file rejection, and guest feed first render.
