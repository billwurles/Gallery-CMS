---
id: CMS-10
title: 'BUG-5: Crashes on missing data instead of clean errors'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-09T23:39:00Z'
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

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-09, found while writing TEST-1:
- An empty backup folder makes `restore()` return null, so `AppConfig.getSite()` hands out a null Site.
- Publishing a site with no home image (filename "") tries to `Files.copy` the gallery folder onto the non-empty html_root, so the build fails.
- `copyPageGallery` throws an NPE when a page has paintings but no `gallery/<url>/` folder (`listFiles()` returns null).
- `GalleryController.editGallery` calls `page.toString()` before its null check, so an unknown page gives a 500 instead of a 404.
- `ExhibitionRepo.getExhibitionsPage` throws for a page past the end (e.g. `/exhibitions?page=5`).
- With no exhibitions, publish writes no `exhibitions/index.html`, but the menu still links to it.
- `sortPaintingsByAspectRatio` silently drops paintings whose image file is missing from the gallery (data loss).

Add a regression test to the TEST-1 suite with each fix.

2026-11-09: Fixed publishing with no home image: the builder now skips the copy and logs a warning. Regression test: ThymeleafSiteBuilderTest.publishesWithoutAHomeImage. The other items are still open.

Commits:
- `b7da79e` Fix saving on a fresh install and publishing with no home image (2026-11-09)
<!-- SECTION:NOTES:END -->
