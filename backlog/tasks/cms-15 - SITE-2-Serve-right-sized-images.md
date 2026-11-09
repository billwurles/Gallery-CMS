---
id: CMS-15
title: 'SITE-2: Serve right-sized images'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-09T16:12:00Z'
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
