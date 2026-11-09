---
id: CMS-11
title: 'BUG-6: Backup rotation and restore bugs'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-09T16:11:00Z'
labels:
  - bug
  - data
dependencies:
  - CMS-19
priority: medium
type: bug
ordinal: 11000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The JSON backups are the only copy of the site's content. In `JsonBackup`:
- `filename.contains(filename)` is always true; it was meant to compare against the configured backup
  name. Any file containing "-published" is treated as a backup and parsed with `Integer.parseInt`.
- `getJsonFromBackup` loops `i = 0 .. n-1`, but files are numbered from 1, so the oldest backup is never
  considered.
- Every edit writes a full backup synchronously and rotates files.
Also: backups live on the same volume as the site. Add an off-machine copy, and a "restore this version"
view in the editor (EDIT-3).
<!-- SECTION:DESCRIPTION:END -->
