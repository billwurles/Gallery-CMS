---
id: CMS-7
title: 'BUG-2: Publish always says it succeeded'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-10T09:50:00Z'
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
- [x] #1 A failing publish shows the error in the editor and leaves the live site unchanged
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10: Publish now builds the site first and saves the published backup only once the build has worked, so no published backup describes a site that never went live. A failed build returns a 500 saying the live site is unchanged, with the cause. A failed backup after a good build returns a 500 saying the site is live but its backup failed. The Publish button shows the server's message. BUG-3 is what keeps the live site unchanged on a failed build. Tests: EditorControllerTest covers the order and both failures.

Commits:
- `36e89a4` BUG-2: report publish errors instead of claiming success (2026-11-10)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Publish errors come back to the editor as a 500 with the cause instead of being swallowed. The editor builds first and writes the published backup second.
<!-- SECTION:FINAL_SUMMARY:END -->
