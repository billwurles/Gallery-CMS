---
id: CMS-22.2
title: 'STORE-3: Stable IDs and schema version in the data model'
status: To Do
assignee: []
created_date: '2026-11-09T17:47:00Z'
labels:
  - storage
dependencies:
  - CMS-22.1
parent_task_id: CMS-22
priority: medium
type: chore
ordinal: 30000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Today pages and exhibitions are keyed by URL, paintings are keyed by filename, and image folders are keyed by page URL. A rename means copying files and rewriting keys (BUG-7, BUG-4).

- Give `Page`, `Exhibition` and `Painting` an immutable `id` (UUID). The URL becomes a plain, validated field (SEC-2 rules).
- Each painting references an image by `imageId` (`images/<imageId>.<ext>`), independent of its page. Renaming a page no longer touches images. Deleting a painting deletes its image.
- Add `schemaVersion` to the serialised site, plus an upgrader that reads v0 (current files: no IDs, URL-keyed folders) and assigns IDs. It records the old `gallery/<url>/<file>` location so STORE-7 can move the images.
- Galleries become an ordered list instead of a filename-keyed map with an `order` int.

This removes the copy-on-rename code path and most of BUG-7.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Pages, exhibitions and paintings have stable UUIDs; renaming a page's URL changes no image keys
- [ ] #2 Current v0 JSON backups load via the upgrader with no data loss (test with a real backup)
- [ ] #3 Exported JSON carries schemaVersion
<!-- AC:END -->
