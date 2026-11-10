---
id: CMS-8
title: 'BUG-3: Deleted and renamed pages stay live after publish'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-10T09:50:00Z'
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
- [x] #1 After deleting a page and publishing, its URL returns the 404 page
- [x] #2 A failed build leaves the previous site intact
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10: The build writes into `.html_root.building` next to html_root. On success, html_root's old entries move to `.html_root.old`, the new ones move in, and both temp folders are deleted. On failure the staging folder is deleted and html_root is untouched. html_root itself is never replaced, because nginx bind-mounts that directory and would keep serving the replaced one. Its contents are swapped by renames on the same filesystem, so the site is empty for a few milliseconds during the swap. Tests in ThymeleafSiteBuilderTest: a deleted page is gone after the next publish, a failed build (forced by a page folder that clashes with 404.html) leaves the old site and no temp folders, and the html_root folder keeps its identity. Checked on a copy of the live data too: publishing left no temp folders.

Commits:
- `372274b` BUG-3, BUG-5: build the site in a staging folder and swap it in (2026-11-10)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Publishing builds the whole site in a staging folder and swaps it into html_root only when the build has worked. Deleted and renamed pages disappear from the live site, and a failed build leaves the previous site in place.
<!-- SECTION:FINAL_SUMMARY:END -->
