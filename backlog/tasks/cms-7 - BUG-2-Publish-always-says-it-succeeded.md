---
id: CMS-7
title: 'BUG-2: Publish always says it succeeded'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
labels:
  - bug
  - editor
dependencies: []
priority: high
type: bug
ordinal: 7000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`EditorController.saveSiteToJSON` builds `ResponseEntity.status(500)...` in both catch blocks but never
returns it, so it always falls through to `ok("Site saved")`. A failed publish (disk full, missing home
image, bad page) looks like success. Return the error, log it, and show the message in the editor.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A failing publish shows the error in the editor and leaves the live site unchanged
<!-- AC:END -->
