# Memory Hub Design

## Goal

Turn Love Space's owner experience into a stronger memory-centered product by upgrading the home page into a "today and memory hub" and polishing diary and anniversary into two cohesive deep-entry pages.

This phase builds on the interactive feed polish from phase one. The feed already feels like a richer interaction surface; this pass brings the same level of structure and care to the app's main entry and long-term memory pages.

## Scope

This phase includes:

- Redesign the owner home page as a memory hub.
- Surface high-frequency actions on home: publish moment, write diary, add anniversary, continue chat.
- Show recent moments, recent diaries, and the next important anniversary/countdown on home.
- Redesign the diary list and calendar views with clearer hierarchy, empty states, and write-entry behavior.
- Redesign the anniversary page with a stronger together card, countdown/anniversary sections, clearer empty states, and improved add/edit form presentation.
- Add focused frontend utility tests for summary formatting and anniversary/diary presentation logic.

This phase does not include:

- Backend schema changes.
- A unified mixed timeline across moments, diaries, and anniversaries.
- A second major chat-page redesign.
- New database fields or changed API contracts.
- Production deployment, unless requested separately.

## Product Direction

The chosen direction is "memory hub first." Home becomes the main product surface for the couple's current day and recent memories. Diary and anniversary become polished deep-entry pages that home can summarize and link to.

This avoids a broad timeline rewrite while still making the app feel materially more cohesive. It also keeps phase two mostly frontend-scoped, which reduces risk after the phase-one feed changes.

## Home Design

The home page becomes a compact dashboard for the relationship:

- Hero: couple avatars, together days, start date, and live time detail.
- Quick actions: publish moment, write diary, add anniversary, continue chat.
- Next important date: the nearest upcoming anniversary/countdown from existing dashboard data.
- Recent moments: a small list linking to the full feed.
- Recent diaries: one or two recent diary summaries fetched from the existing diary list endpoint.
- Chat status: unread badge or a "continue chat" action using existing unread-count behavior.

Home should tolerate partial data. The page should still show quick actions and stable layout even if recent diaries or dashboard data fail to load.

## Diary Design

Diary remains a two-mode page:

- List mode is the default.
- Calendar mode is available from the page action.

List mode should use stronger memory-card presentation:

- Date block with day and month.
- Mood and author metadata.
- Title when present.
- Content summary with predictable truncation.
- Empty state with a direct write action.

Calendar mode should preserve the existing API flow:

- Month markers come from `GET /diaries/month`.
- Selected-day content comes from `GET /diaries/date`.
- Missing diary for a selected date is a normal empty state, not an error.
- The write action should pass the selected date when available.

## Anniversary Design

Anniversary keeps the current interaction model but improves presentation:

- A prominent "together" card remains at the top.
- Countdown items and past anniversaries remain separated through tabs or sectioned layout.
- Countdown cards should emphasize days remaining.
- Past anniversary cards should emphasize date and accumulated days.
- Empty states should explain the missing category and offer add action.
- The add/edit bottom popup keeps the existing form fields and endpoints, but should have clearer spacing, title, close behavior, and save/delete actions.

Save or delete failures should not close the popup automatically, so user input is not lost.

## Data Flow

Home uses existing APIs:

- `GET /dashboard` for together time, recent moments, and upcoming anniversaries.
- `GET /diaries` with the first page for recent diary summaries.
- `GET /chat/unread-count` or the existing app-level unread state for chat status.

Diary uses existing APIs:

- `GET /diaries` for paginated list mode.
- `GET /diaries/month` for calendar markers.
- `GET /diaries/date` for selected-day content.
- `POST /diaries` remains handled by the existing write page.

Anniversary uses existing APIs:

- `GET /anniversaries/together`.
- `GET /anniversaries`.
- `POST /anniversaries`.
- `PUT /anniversaries/{id}`.
- `DELETE /anniversaries/{id}`.

No backend API contract changes are required for this phase.

## Components And Utilities

Keep the implementation close to existing Vue single-file components, but extract small presentation utilities where they reduce fragile template logic.

Expected utility candidates:

- Home memory summary formatting.
- Diary date, mood, and content summary formatting.
- Anniversary grouping and display-label formatting.

These utilities should be pure functions with lightweight tests. Avoid introducing a broad state-management abstraction for this phase.

## Loading And Error States

Home:

- Load dashboard, recent diaries, and chat status independently.
- Show skeletons or stable loading shells for primary modules.
- If dashboard fails, keep quick actions visible and show an inline retry/error state.
- If recent diaries fail, show a local module-level fallback without blocking the whole page.

Diary:

- Show first-load skeletons for list mode.
- Use toast plus inline retry for list failure.
- Treat selected calendar dates without diary content as a normal empty state.

Anniversary:

- Show first-load skeletons for together/list data.
- Use empty states for missing countdown or past anniversary lists.
- Keep popup open on save/delete failure.

## Visual Direction

The visual language should align with phase one:

- 8px-radius cards and controls unless Vant internals require otherwise.
- Warm primary accents balanced with teal/cool secondary accents.
- No large nested card stacks.
- Dense but readable mobile-first layouts.
- Stable bottom spacing so floating action buttons and the tabbar do not obscure content.

Home can have the strongest hero treatment. Diary and anniversary should feel calmer, optimized for scanning and repeated use.

## Verification

Automated:

- Add or update focused frontend utility tests for diary, anniversary, and home presentation helpers.
- Run existing frontend tests.
- Run `npm run build` in `love-space-frontend`.

Manual:

- Home with full data.
- Home with empty recent moments/diaries.
- Home with a failed secondary module.
- Diary list mode, empty list, and pagination.
- Diary calendar mode, selected date with diary, selected date without diary, write entry with selected date.
- Anniversary list with countdowns and past items.
- Anniversary empty categories.
- Add, edit, and delete anniversary flows.
- Mobile viewport check for bottom tabbar and floating action overlap.
