---
id: CMS-12
title: 'BUG-7: Orphaned image files on rename and delete'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-10T09:50:00Z'
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

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Renaming a page moves its gallery folder and leaves nothing under the old URL
- [x] #2 Deleting a page leaves its images to the backup cleanup
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-09: BACKUP-3 (CMS-23.2) garbage-collects gallery files that neither the live site nor any retained backup references, which covers the orphans from page delete and rename. Fix the rename path here, and leave deletion to GC.

2026-11-10: BACKUP-3 (CMS-23.2, `d49f156`) added a cleanup after each backup that removes gallery files nothing references once their content is archived. That covers files orphaned by page delete and rename from now on, once the dry run is switched off (BACKUP-6, CMS-23.5). Still open here: the rename path, which copies the folder instead of moving it, and orphans that were never archived.

2026-11-10: `Filesystem.moveGalleryToNewUrl` replaces the copy. It moves each file into the new folder and removes the old one, and does nothing for a page with no gallery folder. Deleting a page leaves its folder: the images are archived with the backups, and the cleanup removes them from the gallery once nothing live uses them. Tests: PageControllerTest (rename leaves no old folder, delete leaves the files) and FilesystemTest.

Not done here: gallery files that were orphaned before image versioning and never archived. The cleanup keeps those on purpose, and settling them is BACKUP-6 (CMS-23.5) criterion 3.

Commits:
- `2bb145b` BUG-5, BUG-7: 404 page for missing items, move galleries on rename (2026-11-10)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Renaming a page now moves its gallery instead of copying it, so no duplicates are left behind. Files from deleted pages are left to the backup cleanup. The old never-archived orphans are tracked in BACKUP-6.
<!-- SECTION:FINAL_SUMMARY:END -->
