---
id: CMS-23.1
title: 'BACKUP-2: Image archive and manifest written with every backup'
status: Done
assignee: []
created_date: '2026-11-09T23:54:00Z'
updated_date: '2026-11-10T01:24:00Z'
labels:
  - backup
  - data
dependencies:
  - CMS-11
parent_task_id: CMS-23
priority: high
type: feature
ordinal: 35000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Make every JSON backup carry the images it needs.

- **Archive**: `appdata/cms/backup/images/<sha256>.<ext>`, one file per distinct image content. On each backup and publish, hash every image the site references (the home image plus every gallery painting). Add the missing ones to the archive with a hard link from `gallery/`, falling back to a copy if linking fails. Unchanged images are already there, so a backup costs a hash lookup per image and no extra disk.
- **Cache hashes** by (path, size, mtime) in memory, so the per-edit backup doesn't re-read every photo.
- **Manifest inside the backup JSON**: add a top-level `backupManifest` field written alongside the Site fields: `{schemaVersion: 1, created, images: [{path: "work/<uuid>.jpg", sha256, size}]}`. Keeping it in the same file means rotation needs no changes, and old readers ignore it (`FAIL_ON_UNKNOWN_PROPERTIES=false`). Old backups without a manifest still load. They just can't be rehydrated from the archive.
- **Missing images**: if a referenced file is missing at backup time, write the backup anyway, record the image as `missing: true`, and log a warning. Never block a save.
- Write the archive files before the JSON, so a JSON on disk never references an image that isn't in the archive.
- **Tests** (backup/JsonBackupTest):
  - the archive gets one file per distinct image;
  - a second backup adds no files;
  - an archived file is a hard link to its gallery file when both are on the same filesystem;
  - the manifest round-trips;
  - an old manifest-less JSON still restores.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Every draft and published backup has a manifest listing each referenced image with sha256 and size
- [x] #2 Every image in a manifest exists in backup/images/ under its hash
- [x] #3 Backing up an unchanged site adds no files and no disk usage
- [x] #4 Existing backups without a manifest still restore
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10: Built as `ImageArchive` (backup/images/<sha256><.ext>) plus a `backupManifest` object inside each backup JSON, rather than a separate manifest file, so a backup and its image list can't drift apart. Images are hard-linked from the gallery with a verified-copy fallback. Hashes are cached by path, size and mtime.

Rehearsal on a copy of the live appdata: 189 images in the manifest, 0 missing, 185 archive files (33M, all hard links so no extra disk). The first publish took 2.5s and later ones 0.46s.

Commits:
- `d49f156` BACKUP-2, BACKUP-3, BUG-6: version images with backups and stop losing them (2026-11-10)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Every draft and published backup now carries a manifest of the images it needs (path, sha256, size), and the content of each one is kept in a content-addressed archive at backup/images. The archive hard-links from the gallery, so an unchanged site adds no files and no disk. Backups written before this have no manifest and still restore. Covered by ImageVersioningTest; 174 tests pass.
<!-- SECTION:FINAL_SUMMARY:END -->
