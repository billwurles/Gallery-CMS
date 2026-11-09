---
id: CMS-8
title: 'BUG-3: Deleted and renamed pages stay live after publish'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-09T16:11:00Z'
labels:
  - bug
  - site
dependencies:
  - CMS-7
priority: high
type: bug
ordinal: 8000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`ThymeleafSiteBuilder.buildSite` writes into `html_root` without clearing it (there's a TODO). Deleted
pages, old URLs of renamed pages, removed exhibitions and deleted images all stay on the public site
forever. Build into a temp folder next to `html_root` and swap it in when the build succeeds, which also
keeps the old site up if the build fails halfway.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 After deleting a page and publishing, its URL returns the 404 page
- [ ] #2 A failed build leaves the previous site intact
<!-- AC:END -->
