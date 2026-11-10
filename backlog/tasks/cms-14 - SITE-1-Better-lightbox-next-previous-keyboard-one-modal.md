---
id: CMS-14
title: 'SITE-1: Better lightbox: next/previous, keyboard, one modal'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-10T10:19:00Z'
labels:
  - site
dependencies:
  - CMS-6
priority: medium
type: feature
ordinal: 20000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
After BUG-1: the gallery currently renders one hidden modal per painting, duplicating every `<img>`.
Use a single modal filled from data attributes, with next/previous arrows, arrow keys, swipe on phones,
Escape and backdrop click to close, focus kept inside the modal while it's open, and proper alt text.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 One modal per gallery, filled from the thumbnails' data attributes
- [x] #2 Previous/next arrows, arrow keys and swipe move between paintings and wrap at the ends
- [x] #3 Escape, Close and a background click close it; focus stays inside and returns to the thumbnail
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10, asked for by Will ("add left and right arrows to the gallery modal"). `fragments/gallery.html` renders the thumbnails as `.painting-open` (role=button, tabindex=0) with data-src/title/dimensions/medium/sold, plus one `#painting-modal`. `gallery.js` fills it, wraps at both ends, preloads the neighbours and shows "n of N". The arrows are hidden when a gallery has one painting. On a phone (under 600px) the arrows sit over the image's edges. Checked with Playwright on a published copy served by the real nginx config: no modal on load; arrows, keys and wrap; the count; a 1:3 painting fits between the arrows at 1366x768; Tab stays inside; a click on the painting keeps it open and a click on the background closes it, with focus back on the thumbnail; Enter opens; Escape closes; swipes on an iPhone 13 go next and previous. No page errors.

Commits:
- `2e5b0fe` SITE-1: one gallery lightbox with previous and next (2026-11-10)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The public gallery has one lightbox with previous and next arrows, arrow keys and swipe, so visitors can move through the paintings without closing it. It wraps at both ends and shows where you are in the gallery.
<!-- SECTION:FINAL_SUMMARY:END -->
