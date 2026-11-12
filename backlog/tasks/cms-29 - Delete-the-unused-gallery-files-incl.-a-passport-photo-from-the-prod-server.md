---
id: CMS-29
title: Delete the unused gallery files (incl. a passport photo) from the prod server
status: To Do
assignee:
  - Will
created_date: '2026-11-12T15:17:00Z'
labels:
  - data
  - reminder
dependencies: []
priority: high
type: chore
ordinal: 47000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Reminder for Will: do on the prod server what was done on the dev server on 2026-11-12.

The gallery held 91 image files that no page and no backup used, and that were never archived (BACKUP-6). They were the whole of three folders from the old site, none of them a current page:
- `appdata/cms/gallery/test/` (18 files, 20 MB): **includes a photo of a passport**
- `appdata/cms/gallery/wales/` (53 files, 52 MB): the deleted Wales page
- `appdata/cms/gallery/new-work/` (20 files, 15 MB)

On the dev server, all three folders were deleted. Checked first: every listed file existed, no backup JSON named any of them, and each folder held only the listed files.

On prod:
1. Check the three folders hold nothing a page uses. The editor logs "N gallery files aren't used by the site or any backup and were never archived" after each save, with one DEBUG line per file.
2. Delete `gallery/test`, `gallery/wales` and `gallery/new-work`.
3. Also remove the passport photo from any copy of the old site export. On the dev server, `~/send.zip` (the iWeb/old-site export) still contains `cms/gallery/test/` (19 entries) and the wales/new-work folders (78 entries). It was left untouched because it's Will's original export.

Until the SEC-2 change is deployed there, the prod editor serves any file in the gallery folder by address, so that photo can be fetched from the editor by anyone who knows or guesses its address.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 gallery/test, gallery/wales and gallery/new-work are gone from the prod server's appdata
- [ ] #2 No copy of the passport photo is left in old site exports or backups (e.g. send.zip)
<!-- AC:END -->
