---
id: CMS-24
title: 'INFRA-5: Serve the site and editor through the developmess proxy'
status: In Progress
assignee: []
created_date: '2026-11-10T00:06:00Z'
updated_date: '2026-11-10T00:07:00Z'
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
- [ ] #1 No host ports published; both services on the web network with Traefik labels
- [ ] #2 Site at https://gallery-cms.burl.es and editor at https://gallery-cms-edit.burl.es behind Access
- [ ] #3 Uploads, saving and publishing work through the proxy
<!-- AC:END -->
