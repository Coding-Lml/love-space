# Emoji Panel Design

## Context

The app already has chat stickers under `love-space-frontend/src/assets/stickers`, and chat can send `sticker` messages. Diary writing and moment publishing currently only have plain text inputs. The requested feature is to add more expression options that can be used when writing diaries, chatting, and posting moments.

## Decisions

- Build a shared frontend emoji panel instead of duplicating emoji pickers in each page.
- Start with built-in emoji groups and leave the data shape open for future user-uploaded or favorited stickers.
- Keep this release frontend-only. Diary and moment content continue to store selected emoji inline in their existing `content` fields.
- Use a light, minimal emoji-first style rather than adding a large image sticker system now.
- In chat, tapping an emoji inserts it into the message draft by default. A separate quick-send action sends a single emoji immediately as a text message.

## User Experience

The shared panel appears near the relevant composer or text field. It exposes grouped emoji such as frequent, sweet, missing-you, daily, and celebration. Users can switch groups, tap an emoji to insert it, and close the panel without losing their draft.

Diary writing adds an emoji trigger beside the diary content area and appends selected emoji to the diary body. Moment creation adds the same trigger near the post text area. Chat replaces the current sticker-only drawer with the shared emoji panel while keeping access to the existing image stickers.

## Architecture

Create `src/utils/emojiCatalog.js` as the single source of built-in emoji groups. Create `src/components/EmojiPanel.vue` as the reusable UI component. The component receives optional existing sticker entries and quick-send support through props/events, so it can serve all three pages without knowing page-specific API calls.

Page responsibilities stay small:

- `DiaryWrite.vue`: toggles the panel and appends selected emoji to `form.content`.
- `MomentCreate.vue`: toggles the panel and appends selected emoji to `form.content`.
- `Chat.vue`: toggles the panel, appends emoji to `draft`, quick-sends emoji through the existing text message endpoint, and continues sending current SVG stickers through the existing sticker endpoint.

## Data Flow

`emojiCatalog.js` exports grouped emoji metadata. `EmojiPanel.vue` renders tabs and emits `select-emoji`, `quick-send-emoji`, and `select-sticker`. Parent pages mutate their local draft/content state or call existing send APIs.

No backend schema, DTO, or API changes are needed for this release.

## Testing

Add unit tests for the catalog helpers and for text insertion behavior. Run the frontend test files with Node, run the Vite build, and manually verify diary, moment, and chat layouts in the browser after implementation.
