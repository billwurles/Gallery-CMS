---
id: CMS-26
title: 'BUG-8: New pages served with an old cached stylesheet and script'
status: In Progress
assignee: []
created_date: '2026-11-10T10:13:00Z'
updated_date: '2026-11-10T10:13:00Z'
labels:
  - bug
  - site
dependencies: []
priority: high
type: bug
ordinal: 43000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Reported by Will after the BUG-1 deploy: on a gallery page (e.g. recent-work) every painting opened in a modal at once, each needing its own Close, and tall images still ran off the screen.

Cause: the published HTML is sent `no-store`, but nginx's `.css` and `.js` locations replaced the server's no-cache headers, so the stylesheet and scripts had no caching rules and browsers (and Cloudflare's edge, which caches .css/.js by default) kept the old copies. nginx also never loaded `mime.types`, so every file was sent as `text/plain`, with the CSS and JS getting a second `Content-Type` header. The BUG-1 pages no longer hide the modals inline; they rely on the new CSS. With the old cached CSS every modal showed at once, with no height cap.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Every file the site serves has one correct Content-Type and the no-cache headers
- [x] #2 Published pages link to the stylesheet and scripts by a content-hash version, so a changed file always gets a new URL
- [ ] #3 Gallery pages show no modal until a painting is clicked, even for a browser that cached the old stylesheet
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10: nginx.conf now includes mime.types and sets the no-cache headers once at server level with `always`, so they apply to every file and to error responses. The separate .css/.js locations are gone. Checked with nginx:1.27-alpine serving a published copy: `nginx -t` passes; pages, CSS, JS and images each get one correct type plus the no-cache headers; the /gallery-cms redirect and the 404 page still work. `ThymeleafSiteBuilder` hashes the published include files (first 12 hex characters of their SHA-256), and base.html links to `include/<file>?v=<hash>`. ThymeleafSiteBuilderTest checks the links and that the version matches the files. Criterion 3 needs the deploy plus a publish, which rewrites the pages with the versioned links.

Commits:
- `d01c603` BUG-8: stop browsers pairing new pages with an old stylesheet (2026-11-10)
<!-- SECTION:NOTES:END -->
