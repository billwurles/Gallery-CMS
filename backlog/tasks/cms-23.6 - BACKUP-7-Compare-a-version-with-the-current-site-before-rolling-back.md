---
id: CMS-23.6
title: 'BACKUP-7: Compare a version with the current site before rolling back'
status: Done
assignee: []
created_date: '2026-11-10T09:51:00Z'
updated_date: '2026-11-11T11:39:00Z'
labels:
  - backup
  - editor
dependencies:
  - CMS-23.3
parent_task_id: CMS-23
priority: low
type: feature
ordinal: 42000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Asked for by Will (2026-11-10): roll back to old versions from the editor UI, "maybe with some kind of diff".

BACKUP-4 (CMS-23.3) gives the Backups page and the restore itself. This adds a way to see what a restore would change before doing it:
- On the Backups page, each snapshot gets a "Compare with now" view.
- Summary first: pages added, removed or renamed; paintings added, removed or moved between pages; changes to the menu order and the home image; exhibitions added, removed or edited.
- Detail per page: changed title or address; a text diff of the page content (compared as text with the HTML tags stripped, with the raw HTML available); per-painting metadata changes (title, size, medium, sold, order), with thumbnails taken from the image archive so removed paintings can still be shown.
- Compare any two snapshots as well as a snapshot against the current draft.
- Restore from the compare view, using BACKUP-4's restore, which is itself undoable.

The comparison works on the Site JSON and its manifest, so it needs no new storage.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A snapshot can be compared with the current draft, or with another snapshot, before restoring it
- [x] #2 The comparison lists added, removed and changed pages, paintings and exhibitions, with the text changes in page content
- [x] #3 Removed paintings show their thumbnail from the image archive
- [x] #4 Restore can be started from the comparison
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-11: `backup/VersionDiff.between(from, to)` compares two Sites: pages (added, removed, changed: title, show text/gallery, text, paintings), paintings (added, removed, changed: title, dimensions, medium, sold, position), menu order, home image and exhibitions (title, date, text). Text is compared line by line (LCS) after turning the HTML into plain lines, one per paragraph, heading, list item or row. `JsonBackup.loadVersion(id)` reads a version without restoring it, and `versionImage(id, path)` finds an image as that version had it (its archive copy, else the gallery file; only paths the version uses). BackupController: GET /backups/<id>/compare?from=current|<id> renders editor/Compare (summary, menu, home image, each page and exhibition, a "Compare with" select and Restore this version), and GET /backups/<id>/image?path= serves a 900px copy via the builder's thumbnail cache. Versions rows have "Compare with now".

The description mentioned showing the raw HTML. I left that out: the text diff compares what visitors read, so a change that is only in the markup (styling, tags) isn't listed.

Checked on a copy of the live data: comparing the oldest kept version with now showed Flowers and Recent Work changed and "Something else" going. Recent Work listed one painting coming back, three with title/dimension/position changes, and the stray comma returning in its text. Every thumbnail URL returned an image (removed ones from the archive), and comparing two versions with each other worked. Tests: VersionDiffTest (no difference, page and painting changes, menu/home/exhibitions, text without markup) and ContentManagerAppTest (compare page, archived image, path escape refused, unknown version 404).

Commits:
- `49b9ff1` BACKUP-7: compare a version with the site before restoring it (2026-11-11)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Any kept version can be compared with the site now, or with another version, before restoring it. The comparison shows a summary, then page, painting and exhibition changes with thumbnails (from the archive for deleted paintings) and a line diff of the text. Restore can be started from the comparison.
<!-- SECTION:FINAL_SUMMARY:END -->
