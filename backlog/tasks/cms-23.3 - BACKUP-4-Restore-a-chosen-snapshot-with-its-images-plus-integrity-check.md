---
id: CMS-23.3
title: 'BACKUP-4: Restore a chosen snapshot with its images, plus integrity check'
status: To Do
assignee: []
created_date: '2026-11-09T23:54:00Z'
labels:
  - backup
  - data
  - editor
dependencies:
  - CMS-23.1
parent_task_id: CMS-23
priority: medium
type: feature
ordinal: 37000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Today the only restore is "load the newest JSON at startup", and nothing checks that its images exist.

- **Backups page in the editor**: list the draft and published snapshots with time, page count, image count, and any images missing from the archive.
- **Restore a snapshot**:
  1. Take a fresh backup of the current state first, so a restore can itself be undone.
  2. Load the snapshot's Site.
  3. Rehydrate `gallery/<path>` from `backup/images/<sha>` for every manifest entry: hard link where possible, copy otherwise, and verify the hash.
  4. Save.
- A snapshot that predates manifests restores its JSON only, with a warning listing any referenced image files that aren't in `gallery/`.
- **Integrity check**: an endpoint/button plus a startup log line that re-hashes the archive. It reports corrupt files (hash mismatch), live paintings whose file is missing, and manifest entries missing from the archive.
- **Download a snapshot** as a zip (`site.json` + `images/`) for moving it between machines. This is the export format STORE-4 keeps.
- **Tests**: restore an old snapshot after its painting was deleted, and check that the image comes back byte-for-byte; corrupt an archive file and check the integrity check flags it.

This gives the "restore this version" view that EDIT-3 and BUG-6 ask for.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Any retained snapshot can be restored from the editor, images included, and the restore is itself undoable
- [ ] #2 Integrity check reports corrupt and missing images
- [ ] #3 A snapshot can be downloaded as a zip of site.json + images
<!-- AC:END -->
