---
id: CMS-24
title: 'INFRA-5: Serve the site and editor through the developmess proxy'
status: In Progress
assignee: []
created_date: '2026-11-10T00:06:00Z'
updated_date: '2026-11-10T00:35:00Z'
labels:
  - infra
dependencies: []
priority: high
type: task
ordinal: 39000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The site (nginx, 6500) and editor (cms, 6501) were published on all interfaces: the LAN and the server's public IPv6 address. Cloudflare Access only protects traffic through the tunnel, so the editor (no login of its own) was open to anyone who could reach those ports.

Move both behind the developmess Traefik proxy (see ~/projects/CLAUDE.md and developmess's README): no host ports, join the web network, route with labels; every request then needs a Cloudflare Access login.

Both use root-absolute links (site templates: /include/..., /; editor: /upload, /page, /exhibitions/new, redirect:/page/...), so they can't live under dev.burl.es/<path>. They get their own subdomains instead:
- gallery-cms.burl.es -> nginx:80 (published site)
- gallery-cms-edit.burl.es -> cms:8080 (editor)
Cloudflare (by Will): both hostnames on the developmess tunnel -> http://localhost:8800, added to the existing Access app.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 No host ports published; both services on the web network with Traefik labels
- [ ] #2 Site at https://gallery-cms.burl.es and editor at https://gallery-cms-edit.burl.es behind Access
- [ ] #3 Uploads, saving and publishing work through the proxy
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10 (developmess session): deployed a71d285: no host ports, both services on web with Traefik routers gallery-cms (Host gallery-cms.burl.es -> nginx:80) and gallery-cms-edit (Host gallery-cms-edit.burl.es -> cms:8080), SERVER_FORWARD_HEADERS_STRATEGY=native. Verified: LAN ports closed, proxy returns 403 without an Access token, both services answer 200 on the web network. Not reachable from outside yet: neither hostname exists in Cloudflare. Will: 'another session handles it' (this project's session is updating the app, e.g. to run under a dev.burl.es path instead of subdomains). If it moves to paths, change the two routers to Host(`dev.burl.es`) && (Path(`/gallery-cms`) || PathPrefix(`/gallery-cms/`)) etc. with add-slash@file + a strip middleware (see developmess README, 'Putting an app behind the proxy'). Remaining ACs: 2 and 3.
<!-- SECTION:NOTES:END -->
