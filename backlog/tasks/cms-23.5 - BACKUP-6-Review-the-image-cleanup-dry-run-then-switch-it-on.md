---
id: CMS-23.5
title: 'BACKUP-6: Review the image cleanup dry run, then switch it on'
status: Done
assignee: []
created_date: '2026-11-10T01:24:00Z'
updated_date: '2026-11-12T15:17:00Z'
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
- [x] #2 backup.gc.dry-run is false in the deployed app
- [x] #3 The 91 never-archived orphan files are either deleted or accounted for
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10: Reviewed the dry-run log from the deployed app after a publish. It lists 90 gallery files (71 past-work, 15 life-class, 3 new-work, 1 recent-work), the same as the rehearsal. Hashed each one against the live backup's manifest: all 90 are byte-identical duplicates of an image the site uses under another filename, and none is itself referenced. All 189 live images are present in both the gallery and the archive (185 archive files, 33M).

`backup.gc.dry-run=false` is committed in application.properties (`c511ec8`) and takes effect on the next deploy. The 90 files go on the first save or publish after that. Still to do: confirm it after the deploy, and decide on the 91 never-archived orphans.

2026-11-10: confirmed after the deploy: none of the 90 files on the dry-run list is still in the gallery, and the 12:29 publish logged "91 gallery files aren't used by the site or any backup and were never archived; left in place" (most are under gallery/test/). Only criterion 3 remains, and it needs Will's decision.

2026-11-12: Will asked for the 91 never-archived orphans to be deleted. They were all of gallery/wales (53), gallery/new-work (20) and gallery/test (18, including a passport photo), 87 MB; none is a current page. I took the list from the editor's own cleanup log lines and checked that every file existed, no backup JSON named any of them, and each folder held only listed files. Then I deleted them and the three empty folders in the cms container (/appdata/cms/gallery); the gallery is 33 MB now, and the editor and site answer as before. ~/send.zip still holds copies (left as Will's original export). The prod server follow-up is CMS-29.

Commits:
- `c511ec8` BACKUP-6: switch the image cleanup on (2026-11-10)
- `a42dc35` Backlog: BUG-8 done, BACKUP-6 cleanup confirmed, image sizes on SITE-2 (2026-11-10)
(The 91 files were deleted on the server by hand, not by a commit.)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The image cleanup is switched on and has run: the 90 duplicate files went automatically, and the 91 never-archived leftovers were deleted by hand at Will's request. The same clean-up on the prod server is tracked in CMS-29.
<!-- SECTION:FINAL_SUMMARY:END -->
