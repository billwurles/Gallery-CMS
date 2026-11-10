---
id: CMS-23.5
title: 'BACKUP-6: Review the image cleanup dry run, then switch it on'
status: In Progress
assignee: []
created_date: '2026-11-10T01:24:00Z'
updated_date: '2026-11-10T01:43:00Z'
labels:
  - data
  - backup
dependencies:
  - CMS-23.2
parent_task_id: CMS-23
priority: medium
type: task
ordinal: 41000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
BACKUP-3 (CMS-23.2) ships with `backup.gc.dry-run=true`, so the cleanup only logs "[dry run] Would delete ..." and removes nothing.

After the first deploy and a publish:
- Read the cms container's log and check the "Would delete" lines are files that should go.
- Set `backup.gc.dry-run=false` (application.properties, or `BACKUP_GC_DRYRUN=false` in docker-compose.yml) and deploy.

Rehearsal on a copy of the live appdata (2026-11-10):
- 90 gallery files would be deleted. All 90 are byte-identical duplicates of images the live site still uses under another filename (left over from the send.zip import repair), so nothing is lost.
- 91 gallery files are used by neither the site nor any backup and were never archived. The cleanup leaves these alone on purpose and logs one summary line. Decide whether to delete them by hand, or whether any belong to a page that should come back (the Wales page may have been deleted on purpose).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The dry-run log from the deployed app has been reviewed
- [ ] #2 backup.gc.dry-run is false in the deployed app
- [ ] #3 The 91 never-archived orphan files are either deleted or accounted for
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10: Reviewed the dry-run log from the deployed app after a publish. It lists 90 gallery files (71 past-work, 15 life-class, 3 new-work, 1 recent-work), the same as the rehearsal. Hashed each one against the live backup's manifest: all 90 are byte-identical duplicates of an image the site uses under another filename, and none is itself referenced. All 189 live images are present in both the gallery and the archive (185 archive files, 33M).

`backup.gc.dry-run=false` is committed in application.properties (`c511ec8`) and takes effect on the next deploy. The 90 files go on the first save or publish after that. Still to do: confirm it after the deploy, and decide on the 91 never-archived orphans.
<!-- SECTION:NOTES:END -->
