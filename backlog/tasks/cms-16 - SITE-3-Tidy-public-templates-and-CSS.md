---
id: CMS-16
title: 'SITE-3: Tidy public templates and CSS'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-11T05:52:00Z'
labels:
  - site
dependencies:
  - CMS-6
priority: low
type: chore
ordinal: 22000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`base.html` closes `</head>` twice; the first one is on the gallery.js line, so error.js ends up in the body.
`site/static/style.css` defines `.modal-container`, `.modal` and their children twice, and has unused
leftovers. Clean these up after BUG-1 so the modal styles live in one place.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 base.html has one head, with every script in it
- [x] #2 The site stylesheet has no rules for classes nothing uses, and the site looks the same
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-11: base.html's stray `</head>` after the gallery.js line is gone. In style.css I listed every class the site templates and scripts use and removed the rules whose selectors only name unused classes: .modal, .modal img, .modal p, .modal-container (+ .body, .close) and .close (each defined twice), .page .home-title (twice), .inset-image and .content-details. I also removed the commented-out rules and declarations. 718 to 553 lines, 105 to 87 rules; comments and braces balance. The lightbox styles (BUG-1, SITE-1) are the only modal styles left. Checked with Playwright against a published copy of the live data: home, recent-work, cv, exhibitions, an exhibition and the 404 page, on desktop and iPhone 13, plus the open lightbox. All 14 screenshots are byte-identical between the old and new stylesheet.

Commits:
- `f1d79df` SITE-3: tidy the public templates and stylesheet (2026-11-11)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The public templates have valid head markup and the stylesheet has no dead rules or commented-out code, with the site pixel-identical before and after.
<!-- SECTION:FINAL_SUMMARY:END -->
