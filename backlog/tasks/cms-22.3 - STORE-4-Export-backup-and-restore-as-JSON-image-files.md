---
id: CMS-22.3
title: 'STORE-4: Export, backup and restore as JSON + image files'
status: To Do
assignee: []
created_date: '2026-11-09T17:47:00Z'
labels:
  - storage
dependencies:
  - CMS-22.2
parent_task_id: CMS-22
priority: medium
type: feature
ordinal: 31000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Split backup from storage. Replace `JsonBackup` with a `BackupService` that works only through `ContentRepository` and `ImageStore`, so it works the same whichever backend is live.

Format (a directory, or the same layout in a `.zip`):
```
<timestamp>-<draft|published>/
  site.json        # schemaVersion, settings, pages, exhibitions (pretty-printed)
  images/<imageId>.<ext>
  manifest.json    # counts + sha256 of each image, created time, app version
```
- **Snapshot on publish** (always) and **on a schedule / after edits** (debounced, e.g. at most every 10 min, async so it does not block the editor; this replaces backup-on-every-edit).
- Images are content-addressed by sha256 in the manifest. Successive snapshots can hard-link (or skip) unchanged images, so keeping 10 snapshots doesn't copy every image 10 times.
- Writes go to a temp dir and are renamed into place, so a half-written snapshot is never seen as the newest.
- Rotation: keep N published + M draft snapshots by timestamp, not by renaming files (fixes BUG-6).
- **Restore**: pick a snapshot in the editor (or CLI flag), validate the manifest, then load into the current backend. **Import**: the same path, used by STORE-7 and for moving between servers.
- Download a snapshot as a zip from the editor.
- Backup location stays `appdata/cms/backup` (bind mount), outside the DB and bucket, so the backup survives losing either.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Publish writes a site.json + images snapshot with manifest; interrupted writes never become the latest
- [ ] #2 Restore of any snapshot round-trips: export -> wipe -> restore -> export gives identical site.json and image hashes
- [ ] #3 Snapshots are taken async and debounced, not on every keystroke-save
- [ ] #4 Rotation keeps the configured counts and never deletes the newest valid snapshot
<!-- AC:END -->
