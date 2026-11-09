---
id: CMS-23
title: 'BACKUP-1: Version images with every backup'
status: To Do
assignee: []
created_date: '2026-11-09T23:53:00Z'
labels:
  - plan
  - backup
  - data
dependencies: []
priority: high
type: task
ordinal: 12100
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Epic and plan from a read of the backup and image code on 2026-11-09.

## What a backup is today
- `JsonBackup` writes the whole `Site` as JSON to `appdata/cms/backup/` on every edit (10 draft copies) and on every publish (5 published copies).
- **Images aren't in the backup.** The JSON only names each painting's file (`gallery/<page-url>/<uuid>.<ext>`). The bytes live only in `appdata/cms/gallery/`, in one copy.
- So a backup is only as good as the gallery folder at the moment you restore it.

## How photos get lost
Uploads get a new UUID filename (`Filesystem.uploadPainting`), so an image file is never overwritten. Images go missing only through these paths:
1. **Deleting a painting** (`GalleryController.deletePainting`, then `Filesystem.deletePainting`) hard-deletes the file straight away. Every older backup that lists that painting now points at nothing, so you can restore the text but not the photo.
2. **Replacing the home image** (`EditorController.uploadGallery`) hard-deletes the old one in the same way.
3. **Disk loss or a bad edit to `appdata/`**: there is one copy of each image, on the same disk as the JSON. The backups don't help.
4. **Renaming a page** copies `gallery/old/` to `gallery/new/` and leaves the old folder behind (BUG-7). Restoring a backup from before the rename still works, but only by luck.
5. **Deleting a page** leaves its image folder on disk with nothing pointing at it (BUG-7). That's harmless for restores, but the folder grows forever, and nothing distinguishes "orphan" from "needed by an older backup".

Restore (`JsonBackup.restore`) only ever reads the newest JSON at startup. There's no way to choose an older snapshot, and nothing checks whether its images still exist.

## Plan
Since images are immutable by name, versioning them is cheap. Every backup records which image files it needs, plus their hashes, and a content-addressed archive keeps one copy of each distinct image for as long as any retained backup needs it. Hard links (appdata is a single bind mount, so one filesystem) mean an unchanged image costs no extra space, however many snapshots reference it.

1. **BACKUP-2** Image archive and manifest written with every backup
2. **BACKUP-3** Stop hard-deleting images; garbage-collect against retained backups instead
3. **BACKUP-4** Restore a chosen snapshot with its images, plus an integrity check
4. **BACKUP-5** Off-machine copy of the backup folder

This works on today's filesystem storage and keeps the backup JSON format backward compatible. STORE-4 (CMS-22.3) later moves the same format behind `ImageStore`, so this work becomes its first implementation rather than throwaway.

Order: after BUG-6 (CMS-11), because BACKUP-2 touches the same rotation code, and before real content goes into the deployed site.
<!-- SECTION:DESCRIPTION:END -->
