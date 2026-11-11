---
id: CMS-17
title: 'INFRA-2: nginx config and ports'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-11T05:53:00Z'
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

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The 404 page is served for missing files
- [x] #2 Every file gets one correct Content-Type
- [x] #3 HTML is never stored; images, CSS and JS can be cached safely
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-09: INFRA-1 moved the site to port 6500 and the editor to 6501, so the port part of this ticket is done. Still to do:
- In nginx.conf, `error_page` points at `/html_root/404.html`, but the file is served at `/404.html`.
- nginx.conf overrides the CSS/JS Content-Type headers, and the JS one is wrong (`application/js`).
- Caching: keep `no-store` for HTML only.

2026-11-11: what's left of this ticket was done across other work: the 404 path was already `/404.html`; BUG-8 (`d01c603`) loaded mime.types and removed the duplicate Content-Type blocks; SITE-2 (`3f4836e`) gave images `no-cache` (revalidate, 304). This commit caches versioned CSS/JS (`?v=` is always present on published pages) as `public, max-age=31536000, immutable`, and leaves unversioned requests `no-store`. Checked in nginx:1.27-alpine: `nginx -t` passes, and pages, versioned and unversioned CSS/JS, images, and 404s for missing files each get the expected type and header. Deploying it needs `docker restart gallery-cms-nginx-1`.

Commits:
- `d01c603` BUG-8: stop browsers pairing new pages with an old stylesheet (2026-11-10)
- `3f4836e` SITE-2: publish web-sized images for the grid and the lightbox (2026-11-10)
- `7deba8d` INFRA-2: keep versioned CSS and JS for a year (2026-11-11)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
nginx serves the right content types and the 404 page. Pages are never stored, images revalidate, and versioned CSS/JS are cached for a year.
<!-- SECTION:FINAL_SUMMARY:END -->
