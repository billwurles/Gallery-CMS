---
id: CMS-16
title: 'SITE-3: Tidy public templates and CSS'
status: In Progress
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-11T05:48:00Z'
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
