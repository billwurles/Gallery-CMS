---
id: CMS-23.2
title: 'BACKUP-3: Stop hard-deleting images; garbage-collect against retained backups'
status: Done
assignee: []
created_date: '2026-11-09T23:54:00Z'
updated_date: '2026-11-10T01:24:00Z'
labels:
  - backup
  - data
dependencies:
  - CMS-23.1
parent_task_id: CMS-23
priority: high
type: feature
ordinal: 36000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Deleting a painting or replacing the home image currently removes the file at once (`Filesystem.deletePainting`, called from `GalleryController.deletePainting` and `EditorController.uploadGallery`). That breaks every older backup that lists it.

- Deleting removes the painting from the site and its file from `gallery/`, but only once BACKUP-2 has confirmed the image is in the archive. It must never be the last copy. If the archive copy is missing, archive it first.
- **GC** runs after backup rotation. It removes `backup/images/<sha>` files that no retained backup manifest and no image in the live site reference. It also removes `gallery/` files that neither the live site nor any manifest references, which cleans up the orphans left by page delete and rename (most of BUG-7).
- GC is a dry run when `backup.gc.dry-run=true`, and it logs what it would remove. Make dry-run the default for the first deploy, then switch it off.
- Never GC while a backup is being written: run both on one single-threaded executor.
- **Tests**:
  - deleting a painting keeps it restorable from an older backup;
  - an image drops out of the archive only once the last backup that references it has rotated out;
  - live images are never collected;
  - dry run deletes nothing.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Deleting a painting or replacing the home image never removes the last copy of an image any retained backup needs
- [x] #2 GC removes archive files no retained backup needs, and gallery files nothing references whose content is archived, and nothing else
- [x] #3 Dry-run mode logs without deleting
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10: Deletes go through `BackupSite.retireImage`, which archives the file before removing it from the gallery. Cleanup runs after rotation inside the same synchronized save, which gives the "never during a backup" guarantee without a separate executor.

Two differences from the plan:
- Criterion 2 was reworded. A gallery file that nothing references is only deleted if its content is in the archive. Files that were never archived are left in place and counted in one log line, because the cleanup can't tell an orphan from a file that predates the archive. BACKUP-6 (CMS-23.5) covers reviewing those.
- The whole cleanup is skipped if any retained backup can't be read, since its image list is unknown.

`backup.gc.dry-run` defaults to true. Rehearsal on a copy of the live appdata: 90 gallery files would be deleted, all byte-identical duplicates of images still live under another name; 91 never-archived orphans left alone. Nothing was deleted.

Commits:
- `d49f156` BACKUP-2, BACKUP-3, BUG-6: version images with backups and stop losing them (2026-11-10)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Deleting a painting or replacing the home image now archives the file first, so older backups keep working. After each backup rotation a cleanup removes archive files no retained backup needs and unreferenced gallery files whose content is archived, which also clears most of the orphans from page delete and rename (BUG-7). It ships as a dry run that only logs; switching it on is BACKUP-6. Covered by ImageVersioningTest; 174 tests pass.
<!-- SECTION:FINAL_SUMMARY:END -->
