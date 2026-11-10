---
id: CMS-25
title: 'INFRA-5: Work behind the reverse proxy under a path prefix'
status: In Progress
assignee: []
created_date: '2026-11-10T00:17:00Z'
labels:
  - infra
dependencies: []
priority: medium
type: feature
ordinal: 40000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The editor and the published site both use root-absolute links (`/page/...`, `/include/style.css`, `/exhibitions`), so neither works at `dev.burl.es/<app>/` behind Traefik, which strips the prefix and sends `X-Forwarded-Prefix`.

- Editor: `server.forward-headers-strategy=framework`, so `@{/...}` links and redirects pick up the prefix. Convert the hard-coded links to `@{}`.
- Published site: a `site.base-path` setting (default `/`) used by `ServerlessLinkBuilder` as the base for `@{}` links. Convert the hard-coded links in the site templates.
- Compose: Traefik labels for `dev.burl.es/gallery-cms` (site) and `dev.burl.es/gallery-cms-editor` (editor), and keep the LAN ports.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Editor works at dev.burl.es/gallery-cms-editor/ and on :6501
- [ ] #2 Published site works at dev.burl.es/gallery-cms/ when built with that base path, and at / by default
- [ ] #3 Tests assert no root-absolute links leak through with a prefix set
<!-- AC:END -->
