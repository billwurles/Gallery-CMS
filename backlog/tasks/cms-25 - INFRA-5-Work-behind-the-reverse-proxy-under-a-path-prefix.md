---
id: CMS-25
title: 'INFRA-5: Work behind the reverse proxy under a path prefix'
status: Done
assignee: []
created_date: '2026-11-10T00:17:00Z'
updated_date: '2026-11-10T09:38:00Z'
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
- [x] #1 Editor works at dev.burl.es/gallery-cms-editor/ and on :6501
- [x] #2 Published site works at dev.burl.es/gallery-cms/ when built with that base path, and at / by default
- [x] #3 Tests assert no root-absolute links leak through with a prefix set
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10: Will confirmed the editor and site work through the proxy. ThymeleafSiteBuilderTest checks that no root-absolute link is published when a prefix is set.

Commits:
- `2fcb950` INFRA-5: work behind the reverse proxy under a path prefix (2026-11-10)
- `a1fa14d` INFRA-5: drop the subdomain routes, dev.burl.es paths only (2026-11-10)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The editor honours X-Forwarded-Prefix (server.forward-headers-strategy=framework) and every editor link is an @{} link. The published site is built with site.base-path (SITE_BASE_PATH, /gallery-cms in compose) and nginx strips the prefix, so the same files work through the proxy and at / on the LAN port.
<!-- SECTION:FINAL_SUMMARY:END -->
