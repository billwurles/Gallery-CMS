---
id: CMS-10
title: 'BUG-5: Crashes on missing data instead of clean errors'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
labels:
  - bug
dependencies: []
priority: medium
type: bug
ordinal: 10000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
- `AppConfig.getSite`: if restoring the backup throws, or the backup folder exists but is empty, `site`
  stays null and every editor page fails with a 500.
- `PageController.editPage` calls `page.getGallery().isEmpty()` when the gallery can be null.
- `GalleryController.editGallery` logs `page.toString()` before its null check.
- `saveContent` throws a NullPointerException for an unknown page.
- Several handlers return the view `"404"`, which doesn't exist.
- `generateHomePage` throws a NullPointerException on a new site with no home image.
Add a real not-found page and handle each case; when no backup can be read, start empty and say so loudly
instead of failing.
<!-- SECTION:DESCRIPTION:END -->
