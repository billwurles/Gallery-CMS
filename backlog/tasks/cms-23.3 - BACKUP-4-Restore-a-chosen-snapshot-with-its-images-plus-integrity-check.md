---
id: CMS-23.3
title: 'BACKUP-4: Restore a chosen snapshot with its images, plus integrity check'
status: Done
assignee: []
created_date: '2026-11-09T23:54:00Z'
updated_date: '2026-11-11T11:32:00Z'
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
- [x] #1 Any retained snapshot can be restored from the editor, images included, and the restore is itself undoable
- [x] #2 Integrity check reports corrupt and missing images
- [x] #3 A snapshot can be downloaded as a zip of site.json + images
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-11: JsonBackup gained `snapshots()` (every kept backup, newest first, with pages, paintings, images, whether it has a manifest, and the images that can't be brought back), `restoreSnapshot(id)` (re-links or copies each manifest image from the archive into gallery/<path>, skips ones already right, verifies the hash, and returns the Site plus anything missing; pre-manifest versions are JSON only, listing missing files), `writeZip(id, out)` and `checkIntegrity(site)` (re-hashes every archive file; lists live paintings without a file and manifest images the archive lacks). Versions are identified by the file name without its leading number, because saves renumber the files; ids are checked against the kept list, so nothing else can be named. New backups put milliseconds in the timestamp (yyyyMMddHHmmssSSS) so two saves in one second differ; old names still parse. `Site.replaceWith` swaps the restored contents into the shared Site (its name is no longer final). BackupController: GET /backups (Versions page), POST /backups/<id>/restore (backs up the current site first, then restores and saves), GET /backups/<id>/download (zip), GET /backups/check, and the check once at ApplicationReadyEvent. Versions is linked from the sidebar.

Checked on a copy of the live data: startup logged "186 archived image(s), all intact; every live painting has its file"; the page listed 15 versions. After deleting a painting, restoring the previous version brought it and its image back, and restoring the version saved just before undid the restore. The newest version's zip is 32 MB with site.json and 189 images matching its manifest. Check images reported everything intact. Tests in ImageVersioningTest (a deleted painting restored byte for byte, the list and missing images, unknown ids refused, corrupt and missing images found, zip contents) and ContentManagerAppTest (restore through the editor, undo point, 404, zip, check).

Commits:
- `8a675b4` BACKUP-4: restore any kept version, check images, download a zip (2026-11-11)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Any kept version can be restored from the editor's Versions page, images included, and every restore can be undone from the same list. Versions download as a zip of site.json plus images. An image integrity check runs at startup and from the page.
<!-- SECTION:FINAL_SUMMARY:END -->
