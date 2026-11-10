---
id: CMS-10
title: 'BUG-5: Crashes on missing data instead of clean errors'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-10T09:50:00Z'
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

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Unknown pages, galleries and exhibitions show a not-found page instead of a 500
- [x] #2 Publishing works with no home image, no exhibitions, and paintings without image files
- [x] #3 No list or sort silently drops data
<!-- AC:END -->

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

2026-11-10: Each case from the description and the TEST-1 notes, with its fix:
- null Site on an empty backup folder, and publishing with no home image: fixed earlier (TEST-1).
- `editPage` with a null gallery: uses `hasContent()` / `hasGallery()`, which are null-safe.
- `editGallery` logging before its null check, `saveContent` NPE, the missing "404" view: controllers throw `NotFoundException`, and `EditorAdvice` renders `editor/NotFound` with a 404 status. Unknown items on save or delete return 404 and back up nothing (gallery save, painting delete, page delete, exhibition delete). `Site.removePage` / `removeExhibition` no longer NPE on an unknown URL.
- `copyPageGallery` NPE with no gallery folder: copies each painting's file if it exists and logs the rest.
- `getExhibitionsPage` past the end: returns an empty list.
- no `exhibitions/index.html` with no exhibitions: always builds at least one page.
- `sortPaintingsByAspectRatio` dropping paintings: missing or unreadable files sort last instead of vanishing.
Also found and fixed: renaming an exhibition (title or date change) left the old entry beside the new one, and `copyClassPathResource` never closed its streams. Each fix has a regression test (controller tests, ThymeleafSiteBuilderTest, ExhibitionRepoTest, FilesystemTest, plus a full-app test that the not-found page renders).

Commits:
- `372274b` BUG-3, BUG-5: build the site in a staging folder and swap it in (2026-11-10)
- `2bb145b` BUG-5, BUG-7: 404 page for missing items, move galleries on rename (2026-11-10)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Missing data now gives clean errors: an editor not-found page with a 404 status, 404 responses for API calls on unknown items, and a publish that copes with missing folders, files and exhibitions. No path silently drops paintings any more.
<!-- SECTION:FINAL_SUMMARY:END -->
