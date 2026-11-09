---
id: CMS-23.2
title: 'BACKUP-3: Stop hard-deleting images; garbage-collect against retained backups'
status: To Do
assignee: []
created_date: '2026-11-09T23:54:00Z'
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
- [ ] #1 Deleting a painting or replacing the home image never removes the last copy of an image any retained backup needs
- [ ] #2 GC removes archive and gallery files referenced by nothing retained, and nothing else
- [ ] #3 Dry-run mode logs without deleting
<!-- AC:END -->
