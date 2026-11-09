---
id: CMS-14
title: 'SITE-1: Better lightbox: next/previous, keyboard, one modal'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-09T16:12:00Z'
labels:
  - site
dependencies:
  - CMS-6
priority: medium
type: feature
ordinal: 20000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
After BUG-1: the gallery currently renders one hidden modal per painting, duplicating every `<img>`.
Use a single modal filled from data attributes, with next/previous arrows, arrow keys, swipe on phones,
Escape and backdrop click to close, focus kept inside the modal while it's open, and proper alt text.
<!-- SECTION:DESCRIPTION:END -->
