---
id: CMS-13.3
title: 'EDIT-4: Gallery editor as a card grid'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-11T11:03:00Z'
labels:
  - editor
  - ui
dependencies:
  - CMS-9
parent_task_id: CMS-13
priority: high
type: feature
ordinal: 16000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Replace the long form with a grid of cards: thumbnail, title, dimensions and medium editable in place,
a Sold toggle shown as a badge, and drag to reorder by a handle. A sticky save bar appears when
something has changed, and leaving the page with unsaved changes warns first. Show image size and
resolution on hover so overly large uploads are obvious. Build on the fixes in BUG-4.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Paintings are cards with title, dimensions, medium and Sold editable in place, reordered by a handle
- [x] #2 A save bar appears when something has changed, and leaving with unsaved changes warns first
- [x] #3 Each card shows its image's pixel and file size, and large files stand out
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-11: the card grid came with EDIT-2 (`5b2ff52`). This adds the rest. SortableJS 1.15.7 (cdnjs) with a nine-dot handle on each card, which works on touch screens; jQuery UI is no longer loaded on this page. A fixed save bar (Discard / Save changes) shows on any input or reorder, `beforeunload` warns while there are unsaved changes, and a successful save clears it. GalleryController passes each painting's file size, and the page adds the loaded image's pixel size: "3175 × 1972 px · 828 KB", in red with "large file" over 5 MB or 5000 px. The size is always shown rather than on hover, so it works on a phone. confirmDelete takes `data-remove` to drop the card without a reload (fires a `deleted` event; the count updates). Ticking Sold toggles the badge straight away.

Checked in a browser on a copy of the live data: no bar at first; size lines on every card; dragging by the handle reorders and brings up the bar; leaving asks; saving hides the bar and the order and a changed title survive a reload; deleting removes the card, updates the count and keeps another unsaved edit. No page errors.

Commits:
- `5b2ff52` EDIT-2: one editor layout and stylesheet (2026-11-10)
- `80db9b2` EDIT-4: gallery editor save bar, drag handles and image sizes (2026-11-11)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The gallery editor is a card grid with drag-by-handle reordering (touch too), a save bar for unsaved changes with a warning before leaving, image sizes on every card with large files flagged, and in-place deletes.
<!-- SECTION:FINAL_SUMMARY:END -->
