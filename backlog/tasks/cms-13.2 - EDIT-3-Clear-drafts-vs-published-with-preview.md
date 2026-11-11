---
id: CMS-13.2
title: 'EDIT-3: Clear drafts vs published, with preview'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-11T11:19:00Z'
labels:
  - editor
  - ui
dependencies:
  - CMS-7
parent_task_id: CMS-13
priority: high
type: feature
ordinal: 15000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Show "Unpublished changes since <time>" in the header, with Publish highlighted only when there's
something to publish. Add a Preview that renders the draft site at `/preview/` behind login, before it
goes live. Show publish progress and errors (BUG-2). Show a list of recent backups with "restore this
version" (BUG-6).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The editor shows whether there are unpublished changes and since when, and highlights Publish only then
- [x] #2 The draft site can be previewed at /preview/ in the editor, behind its login, without touching the live site
- [x] #3 Publishing shows progress and any error
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-11: `JsonBackup.status(site)` compares `valueToTree(site)` with the newest published backup minus its `backupManifest` (cached by path and mtime). It returns changed, lastPublished (that file's mtime) and changedSince (the newest draft's mtime). `EditorAdvice` adds it to every editor view. The sidebar shows a dot and "Unpublished changes since …" or "Live site is up to date", plus "Published …" (times made friendly in the browser), and Publish keeps its colour only when changed. `markPublished()` / `markUnpublished()` update this after a publish, a gallery save, a menu reorder and a delete. The builder gained `buildPreview(site, root, base, editorHome)`: a link base per build (ServerlessLinkBuilder reads the `linkBase` context variable), no swap, no cache pruning, and a banner in base.html. `PreviewController`: GET /preview builds into `preview_root` (next to html_root, which nginx never serves) using the editor's own context path, so the prefix is honoured, and redirects to /preview/. GET /preview/** serves files from there, with folders serving index.html, missing files the 404 page and paths outside the folder refused. Publish progress and errors were done in BUG-2. The restore list is BACKUP-4 (CMS-23.3).

Checked in a browser on a copy of the live data: the preview opens in a new tab with the banner, and its pages and images load; publishing turns the status to up to date (and stays so after a reload); a gallery save turns it back to unpublished, which the server confirms after a reload. Tests: JsonBackupTest (never published; publish then edit then undo), ImageVersioningTest (manifest ignored), ThymeleafSiteBuilderTest (preview in its own folder with prefixed links and the banner, live site untouched, next publish unaffected), ContentManagerAppTest (preview through the prefix, CSS type, 404s, path escape refused, status on every page).

Commits:
- `36e89a4` BUG-2: report publish errors instead of claiming success (2026-11-10)
- `006c91a` EDIT-3: publish status and a preview of the draft site (2026-11-11)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The editor shows whether the live site is behind the draft and since when, with Publish highlighted only when needed, and the draft can be previewed at /preview/ before publishing. Restoring old versions is left to BACKUP-4.
<!-- SECTION:FINAL_SUMMARY:END -->
