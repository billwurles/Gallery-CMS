---
id: CMS-23.6
title: 'BACKUP-7: Compare a version with the current site before rolling back'
status: To Do
assignee: []
created_date: '2026-11-10T09:51:00Z'
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
- [ ] #1 A snapshot can be compared with the current draft, or with another snapshot, before restoring it
- [ ] #2 The comparison lists added, removed and changed pages, paintings and exhibitions, with the text changes in page content
- [ ] #3 Removed paintings show their thumbnail from the image archive
- [ ] #4 Restore can be started from the comparison
<!-- AC:END -->
