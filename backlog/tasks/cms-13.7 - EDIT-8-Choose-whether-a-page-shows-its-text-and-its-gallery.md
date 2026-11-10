---
id: CMS-13.7
title: 'EDIT-8: Choose whether a page shows its text and its gallery'
status: Done
assignee: []
created_date: '2026-11-10T12:38:00Z'
updated_date: '2026-11-10T12:46:00Z'
labels:
  - editor
  - site
dependencies: []
parent_task_id: CMS-13
priority: medium
type: feature
ordinal: 46000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Asked for by Will: tick or untick whether a page shows a gallery and/or text, either on the page edit screen or on the home page's menu rows.

Chosen: two checkboxes on the page edit screen, "Show text" and "Show gallery", saved with the page. Hiding keeps the text and the paintings, so ticking the box again brings them back. Home page rows already carry a drag handle and the Edit/Delete buttons, and toggles there would save instantly and be easy to hit by accident. The gallery editor says when its gallery is hidden, and its button opens the text editor directly, since a gallery-only page otherwise opens on its gallery.

Stored as `showContent` / `showGallery` on Page, both true by default, so existing backups behave as before.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The page editor has Show text and Show gallery checkboxes, saved with the page
- [x] #2 A hidden gallery or text is left out of the published page but kept, and comes back when ticked
- [x] #3 Backups without the new fields load with both shown
- [x] #4 The gallery editor shows when its gallery is hidden, and links to the page's text and settings
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10: Page has `showContent` / `showGallery` (builder defaults true). `showsContent()` / `showsGallery()` decide what the published page renders, and a hidden gallery's images aren't copied into html_root. PageController takes optional showContent/showGallery parameters: a new page defaults to both on, and an edit that leaves them out keeps the current values. EditPage has the two checkboxes, copied into hidden fields on submit. EditGallery shows a notice when its gallery is hidden, and its "Text and settings" button opens /page/<url>?edit=text.

Also fixed: content.html had `th:if` and `th:replace` on the same element. Thymeleaf runs th:replace first, so the gallery's condition was never applied, and the gallery fragment's own empty check was hiding that. It's now on a wrapping th:block.

Checked in a browser on a copy of the live data: untick Show gallery, save, and the gallery editor shows the notice; after a publish Recent Work has no lightbox, with all 14 paintings still in the editor. Ticking it again brings back all 14.

Commits:
- `5a8995c` BUG-9, BUG-10, EDIT-8: pagination, blank text, show text/gallery (2026-11-10)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Each page has Show text and Show gallery checkboxes on its edit screen. Hiding keeps the text or paintings in the editor and leaves them out of the published page.
<!-- SECTION:FINAL_SUMMARY:END -->
