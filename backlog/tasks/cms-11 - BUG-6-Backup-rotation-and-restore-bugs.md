---
id: CMS-11
title: 'BUG-6: Backup rotation and restore bugs'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-10T01:24:00Z'
labels:
  - bug
  - data
dependencies:
  - CMS-19
priority: medium
type: bug
ordinal: 11000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The JSON backups are the only copy of the site's content. In `JsonBackup`:
- `filename.contains(filename)` is always true; it was meant to compare against the configured backup
  name. Any file containing "-published" is treated as a backup and parsed with `Integer.parseInt`.
- `getJsonFromBackup` loops `i = 0 .. n-1`, but files are numbered from 1, so the oldest backup is never
  considered.
- Every edit writes a full backup synchronously and rotates files.
Also: backups live on the same volume as the site. Add an off-machine copy, and a "restore this version"
view in the editor (EDIT-3).
<!-- SECTION:DESCRIPTION:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-09: STORE-4 (CMS-22.3) replaces JsonBackup with a snapshot-based BackupService. Only fix what's needed to stop data loss here, and leave the rest to STORE-4.

2026-11-09, found while writing TEST-1: `saveToJson` creates `backupPath.getParent()`, not `backupPath` itself. On a fresh install with no backup folder, every save and every publish throws, and Publish still reports success (BUG-2). backup/JsonBackupTest covers the current rotation; add the regression tests there.

2026-11-09: Fixed the missing backup folder: saveToJson now creates backupPath itself. Regression test: JsonBackupTest.backupCreatesAMissingBackupFolder. The other items on this ticket are still open.

2026-11-10: Fixed the rest of the data-loss bugs in JsonBackup:
- Backup files are matched by a pattern built from the configured name, so other files in the folder are ignored.
- Restore considers every backup including the oldest, skips one that can't be parsed and tries the next, and falls back from drafts to published.
- Rotation deletes everything at or beyond the limit, so lowering the limit works.
- Backups are written to a temp file and moved into place, so a crash can't leave a half-written newest backup.
- If nothing can be restored the app starts with an empty site and logs an error, instead of failing to start.

Each has a regression test in JsonBackupTest; the five restore and rotation tests fail on the old code.

Not done here, tracked elsewhere: every edit still writes a full backup synchronously (STORE-4, CMS-22.3); the off-machine copy is BACKUP-5 (CMS-23.4); the "restore this version" view is BACKUP-4 (CMS-23.3) and EDIT-3 (CMS-13.2).

Commits:
- `b7da79e` Fix saving on a fresh install and publishing with no home image (2026-11-09)
- `d49f156` BACKUP-2, BACKUP-3, BUG-6: version images with backups and stop losing them (2026-11-10)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The backup rotation and restore bugs that could lose the site's content are fixed: backup files are matched by name, the oldest backup is no longer skipped, a corrupt backup no longer blocks restore, and backups are written atomically. The synchronous backup per edit, the off-machine copy and the restore view are left to STORE-4, BACKUP-5 and BACKUP-4.
<!-- SECTION:FINAL_SUMMARY:END -->
