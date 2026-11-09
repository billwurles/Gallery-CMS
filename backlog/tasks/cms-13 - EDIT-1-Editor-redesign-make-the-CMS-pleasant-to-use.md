---
id: CMS-13
title: 'EDIT-1: Editor redesign: make the CMS pleasant to use'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-09T23:51:00Z'
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
