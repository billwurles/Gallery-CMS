---
id: CMS-22.6
title: 'STORE-7: Migrate existing appdata and switch production over'
status: To Do
assignee: []
created_date: '2026-11-09T17:48:00Z'
labels:
  - storage
dependencies:
  - CMS-22.3
  - CMS-22.4
  - CMS-22.5
parent_task_id: CMS-22
priority: medium
type: task
ordinal: 34000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A one-shot, repeatable migration from the live `appdata/cms` (newest JSON backup + `gallery/<url>/` folders) into Mongo and MinIO.

1. Take a full copy of `/srv/apps/gallery-cms/appdata` first.
2. Run the import: the v0 upgrader (STORE-3) assigns IDs, `BackupService.import` (STORE-4) loads the content, and the images are moved from `gallery/<url>/<file>` to `images/<imageId>.<ext>`.
3. Verify: page, painting and exhibition counts match. Every painting's image exists in the bucket. Orphaned files that no painting references are listed (BUG-7) and not imported.
4. Publish to a scratch html_root and diff it against the current live output. Only the image paths should differ.
5. Switch `storage.content=mongo`, `storage.images=s3` in the prod profile and deploy.
6. Rollback: set both back to `file`. The old appdata is untouched by the migration.

Document the runbook in the README.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Migration is idempotent and verified by counts + image existence check
- [ ] #2 Published output diff vs pre-migration shows only expected path changes
- [ ] #3 README has the runbook including rollback
<!-- AC:END -->
