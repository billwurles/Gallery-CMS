---
id: CMS-15
title: 'SITE-2: Serve right-sized images'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-10T12:33:00Z'
labels:
  - site
  - perf
dependencies:
  - CMS-8
priority: medium
type: feature
ordinal: 21000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The public grid shows the original uploads, which can be several MB each, scaled down by CSS. On publish,
generate web sizes (e.g. 600px thumbnails and a 2000px lightbox version, WebP or JPEG) and use
`srcset` and `loading="lazy"`. nginx currently sends `no-store` for everything, images included (INFRA-2).
<!-- SECTION:DESCRIPTION:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10: measured the live galleries. Only recent-work has large originals (12 paintings, up to 3899px, several MB each, all sent as grid thumbnails too). Every other gallery's files are at most 800px on their longest side (past-work 75, small-pieces 31, the-river 24, the-sea 21, life-class 15, flowers 10): they were already scaled down when they came over from the old site. The lightbox shows each painting as large as the screen allows, but never past its own pixel size (Will: "don't bump up beyond their resolution"), so those galleries need higher-resolution files re-uploaded to show bigger. Uploads keep the original file, and this ticket should then make thumbnails for the grid and a large version for the lightbox.
<!-- SECTION:NOTES:END -->
