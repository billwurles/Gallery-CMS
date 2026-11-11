---
id: CMS-13
title: 'EDIT-1: Editor redesign: make the CMS pleasant to use'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-11T11:39:00Z'
labels:
  - editor
  - ui
dependencies: []
priority: high
type: feature
ordinal: 13000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Epic. Reported: the CMS edit front end looks a bit rubbish. What's behind that:
- Three style systems at once: the editor's own `static/css/style.css` (copied from the public site,
  including Vue leftovers like `.router-link-exact-active`), Bootstrap 3.4.1 (end of life) on the page and
  exhibition editors only, and the jQuery UI base theme.
- The nav is the public site's menu. "Publish Site" sits in an `<h1><pre>` where a logo would go, and
  there's no editor structure (dashboard, pages, exhibitions).
- Every result is reported through `alert()`, and every action asks through `confirm()`.
- Gallery editing is a long vertical form per painting.
- Unclear model: every edit is saved as a draft immediately, but nothing says the site isn't live until
  you press Publish.
- Images are uploaded as base64 inside JSON (100 MB request limit, memory heavy).

Subtasks EDIT-2..7. Keep server-rendered Thymeleaf. A SPA isn't needed; a small amount of plain JS
(and SortableJS instead of jQuery UI) is enough.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 One editor style system (no Bootstrap, jQuery UI theme or copied site CSS)
- [x] #2 A real editor structure: sidebar, dashboard, pages, exhibitions, versions
- [x] #3 Toasts and a styled confirm dialog instead of alert() and confirm()
- [x] #4 Gallery editing as a card grid; uploads as files with progress
- [x] #5 It's clear what's draft and what's live, with a preview
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-11: every subtask is done: EDIT-2 layout and stylesheet (CMS-13.1), EDIT-3 draft vs published and preview (CMS-13.2), EDIT-4 gallery card grid (CMS-13.3), EDIT-5 uploads with progress (CMS-13.4), EDIT-6 page and exhibition editors (CMS-13.5), EDIT-7 dashboard and phone layout (CMS-13.6), EDIT-8 show text/gallery (CMS-13.7). The editor stayed server-rendered Thymeleaf with a little plain JS and SortableJS. The commits are listed on each subtask.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The editor redesign is complete: one light layout with a sidebar, dashboard, page, gallery, exhibition and version screens, toasts and dialogs, card-based gallery editing, file uploads with progress, a publish status with preview, and a layout that works on phones.
<!-- SECTION:FINAL_SUMMARY:END -->
