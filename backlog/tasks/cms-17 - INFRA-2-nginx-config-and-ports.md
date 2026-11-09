---
id: CMS-17
title: 'INFRA-2: nginx config and ports'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-09T16:12:00Z'
labels:
  - infra
dependencies:
  - CMS-2
priority: medium
type: feature
ordinal: 23000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
- nginx publishes host port 80. Per the dev server rule (unique host port per app), give it its own port,
  or route by hostname through a shared proxy.
- `error_page ... /html_root/404.html` points at a path that doesn't exist (the file is `/404.html`).
- `add_header Content-Type "application/js"` is the wrong MIME type, and `add_header` adds a second
  Content-Type header rather than replacing it. Delete those blocks; nginx's mime.types already handles CSS and JS.
- `Cache-Control: no-store` on everything: keep it for HTML, and cache images, CSS and JS for a long time.
<!-- SECTION:DESCRIPTION:END -->
