---
id: CMS-13.6
title: 'EDIT-7: Dashboard and mobile-friendly editor'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-11T11:19:00Z'
labels:
  - editor
  - ui
dependencies:
  - CMS-13.1
parent_task_id: CMS-13
priority: medium
type: feature
ordinal: 19000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Dashboard: home image, pages in menu order (drag to reorder, with SortableJS replacing jQuery UI),
latest exhibitions, and publish status. The whole editor should work on a tablet and phone, so the
artist can mark a painting sold or upload from their phone.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The dashboard shows the home image, the pages in menu order (drag to reorder with SortableJS), the latest exhibitions and the publish status
- [x] #2 Every editor page works on a phone and a tablet with no sideways scrolling
- [x] #3 A painting can be marked sold, and images uploaded, from a phone
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-11: the home page has a Latest exhibitions card (3 newest, with All exhibitions and + New exhibition) and reorders the menu with SortableJS 1.15.7, so jQuery UI is no longer loaded anywhere. Phone layout (under 860px): the top bar is one row (site name with ellipsis, Publish, Menu); gallery cards are at least 240px, so one per row on a phone with their fields readable; the save bar spans the screen; exhibition posts scroll a wide table inside the post. On touch screens (pointer: coarse) checkboxes are 22px and page-row handles and small buttons have bigger hit areas.

Checked with Playwright on iPhone 13, iPad (gen 7) and desktop for home, gallery, page, upload, exhibitions, an exhibition and new page: no sideways scrolling anywhere, and no page errors. On the iPhone a tap on Sold brings up the save bar, and a tap on Save stores it (checked after a reload). The upload page works by tapping "Choose files", which opens the phone's photo picker. Criterion 1 is left open for the publish status, which comes with EDIT-3.

Commits:
- `80db9b2` EDIT-4: gallery editor save bar, drag handles and image sizes (2026-11-11)
- `3e690c3` EDIT-7: dashboard exhibitions, touch reordering and a phone layout (2026-11-11)

2026-11-11: EDIT-3 (`006c91a`) puts the publish status (unpublished changes, last published) and the Preview link in the sidebar of every page, the dashboard included, which covers criterion 1.

Commits:
- `006c91a` EDIT-3: publish status and a preview of the draft site (2026-11-11)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The dashboard shows the home image, the pages in menu order (drag to reorder, touch too), the latest exhibitions and the publish status. Every editor page works on a phone and tablet, so paintings can be marked sold or uploaded from a phone.
<!-- SECTION:FINAL_SUMMARY:END -->
