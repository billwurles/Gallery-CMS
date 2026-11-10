---
id: CMS-12
title: 'BUG-7: Orphaned image files on rename and delete'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-10T01:24:00Z'
labels:
  - bug
  - data
dependencies: []
priority: low
type: bug
ordinal: 12000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Renaming a page URL copies its gallery folder and leaves the old one. Deleting a page leaves its gallery
folder (TODO in `PageController`). Replacing the home image is fine, but a failed upload can leave a
stray file. Move files instead of copying, delete with the page (or into a trash folder kept for N days),
and add a one-off cleanup for existing orphans.
<!-- SECTION:DESCRIPTION:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-09: BACKUP-3 (CMS-23.2) garbage-collects gallery files that neither the live site nor any retained backup references, which covers the orphans from page delete and rename. Fix the rename path here, and leave deletion to GC.

2026-11-10: BACKUP-3 (CMS-23.2, `d49f156`) added a cleanup after each backup that removes gallery files nothing references once their content is archived. That covers files orphaned by page delete and rename from now on, once the dry run is switched off (BACKUP-6, CMS-23.5). Still open here: the rename path, which copies the folder instead of moving it, and orphans that were never archived.
<!-- SECTION:NOTES:END -->
