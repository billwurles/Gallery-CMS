---
id: CMS-27
title: 'BUG-9: Exhibitions pagination links to pages that don''t exist'
status: Done
assignee: []
created_date: '2026-11-10T12:38:00Z'
updated_date: '2026-11-10T12:46:00Z'
labels:
  - bug
  - site
dependencies: []
priority: medium
type: bug
ordinal: 44000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Reported by Will: with one page of exhibitions, "Next" goes to /exhibitions/page-2/, which doesn't exist, so the visitor gets the 404 page. The published exhibitions template always renders the pagination bar, and Previous/Next are real links even on the first and last page (only a "disabled" class marks them).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 No pagination bar when there is one page of exhibitions
- [x] #2 Previous is not a link on the first page, and Next is not a link on the last
- [x] #3 Every pagination link on the published site points to a page that exists
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10: `site/fragments/exhibitions.html` renders the pagination only when totalPages > 1, and Previous/Next are links only when that page exists (otherwise a plain `span.disabled`). The editor's exhibitions list does the same. Tests: ThymeleafSiteBuilderTest checks one page has no pagination, and that with two pages every /exhibitions/ link in the published HTML points to a page that exists. Checked on a published copy of the live data (4 exhibitions): no pagination.

Commits:
- `5a8995c` BUG-9, BUG-10, EDIT-8: pagination, blank text, show text/gallery (2026-11-10)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The exhibitions page shows pagination only when there's more than one page, and never links to a page that doesn't exist.
<!-- SECTION:FINAL_SUMMARY:END -->
