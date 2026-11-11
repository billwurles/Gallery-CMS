---
id: CMS-13.4
title: 'EDIT-5: Drag-and-drop upload with progress'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-11T11:24:00Z'
labels:
  - editor
  - ui
dependencies:
  - CMS-4
parent_task_id: CMS-13
priority: medium
type: feature
ordinal: 17000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Drop many images onto a gallery, with per-file progress and title, dimensions and medium editable
before saving. Send multipart uploads instead of base64 JSON, which inflates files by a third and holds
everything in memory. Resize huge originals on upload, keeping the original separately (SITE-3).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Many images can be dropped or chosen at once, with title, dimensions, medium and sold editable before uploading
- [x] #2 Each file is sent as a multipart upload with its own progress bar, and a refused file doesn't stop the others
- [x] #3 Large originals are kept, and the editor shows small copies of them
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-11: new `POST /page/<url>/gallery/upload` and `POST /upload` for multipart (one `file` plus title, dimensions, medium and sold), alongside the JSON versions, selected by `consumes`. The gallery one is synchronized on the site and puts the image after the highest existing order. A blank title becomes the file name without its extension. `Filesystem.savePainting` takes raw bytes, with the same checks as SEC-2. The upload page keeps the File objects, previews with `URL.createObjectURL`, and sends two requests at a time with XHR upload progress. Each card ends as Uploaded (green bar) or "Not uploaded: <reason>" in red with Remove. When all succeed it goes back to the gallery; otherwise a toast says how many failed. On resizing: the description asked for huge originals to be resized on upload. SITE-2 already makes web sizes on publish, so the originals stay as uploaded (best for a later re-crop or print), and the editor's gallery now loads the cached 900px copy via `?size=thumb` (`ThymeleafSiteBuilder.thumbnail`), the same file a publish uses. Card sizes come from the server (bytes and pixels of the original).

Checked in a browser on a copy of the live data: a real painting JPEG, a fake .jpg and a 6.2 MB 1800x1200 PNG uploaded together. The two real ones were uploaded with progress bars, the fake was marked with its reason, and the toast read "2 uploaded, 1 not". The gallery went from 10 to 12 with the typed dimensions kept, thumbnails at most 900px, and the original sizes listed ("1800 × 1200 px · 6.2 MB" in red). Tests: GalleryControllerTest (multipart upload, title from the file name, order, refused non-image), EditorControllerTest (home image as a file), ContentManagerAppTest (thumb is 900px wide, original 3000px).

Commits:
- `94cf228` EDIT-5: upload each image as a file, with progress (2026-11-11)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Uploads go one file at a time as normal multipart uploads, with a progress bar and result on each card, so a bad file no longer stops the rest. Originals are kept, and the editor shows them as small cached copies.
<!-- SECTION:FINAL_SUMMARY:END -->
