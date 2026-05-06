# Upload Performance Experience Design

## Goal

Improve the perceived speed of Love Space by making feeds show useful content sooner, making image uploads smaller before they leave the browser, and showing clear upload progress and failure feedback.

## Scope

This pass focuses on a small, stable improvement set:

- Load guest header data and moment feed data in parallel.
- Add lightweight first-load skeletons to dynamic feeds.
- Compress large images in the browser before upload.
- Show staged publish feedback: preparing files, uploading, and publishing.
- Surface upload progress from Axios to the publish screens.
- Return a backend error when any attached upload fails instead of silently publishing an incomplete moment.

Video transcoding, resumable uploads, direct-to-OSS browser uploads, service workers, and major feed-query rewrites are out of scope for this pass.

## Frontend Design

The frontend keeps the existing Vant uploader and publish flows. A new utility module will prepare selected media before submission. Images above a size threshold will be drawn to canvas, resized to a maximum side length, and encoded as JPEG/WebP-compatible browser output. GIFs and videos will not be transformed.

The publish screens will track a small status object with phase, percent, and message. The UI will show progress inline near the publish button and in the loading toast. Axios calls will accept per-request `onUploadProgress` callbacks through the API wrapper.

The guest feed will stop serializing header and list requests. On mount it will start both requests, letting the moment list appear even if the dashboard takes longer.

## Backend Design

The backend keeps the existing multipart endpoints. Moment publish controllers will fail fast if any uploaded file cannot be stored or identified. This prevents the current silent partial-success behavior.

The existing server-side image optimizer remains as a safety net for non-browser uploads or images that are still large after frontend compression.

## Error Handling

Frontend validation will distinguish unsupported type, oversize file, image preparation failure, timeout/network failure, and server rejection. Backend upload failure will return the existing `Result.error` shape so the current interceptor can display the message.

## Verification

Run frontend production build and backend compile. Manually test publishing text-only, small image, large image, video, mixed media, and invalid file type. Verify the guest feed renders list content without waiting for dashboard completion.
