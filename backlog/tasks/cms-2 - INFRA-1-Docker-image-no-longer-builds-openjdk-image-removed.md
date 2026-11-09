---
id: CMS-2
title: 'INFRA-1: Docker image no longer builds: openjdk image removed'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-09T23:39:00Z'
labels:
  - infra
dependencies: []
priority: high
type: bug
ordinal: 2000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The runtime stage uses `openjdk:17-jdk-slim`, which has been removed from Docker Hub, so the next
deploy fails. The code itself compiles (checked with maven:3.9-eclipse-temurin-17). Switch to
`eclipse-temurin:17-jre` (or 21) for runtime and `maven:3.9-eclipse-temurin-17` for the build, and
run as a non-root user. The post-receive hook now deploys `master` as well as `main`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `docker compose build` succeeds from a clean checkout
- [x] #2 Pushing master to dev deploys and the editor comes up
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
The build stage now uses maven:3.9-eclipse-temurin-17 and the runtime uses eclipse-temurin:17-jre. nginx is pinned to 1.27-alpine. Ports are bound to localhost, following the server convention: the public site is on 127.0.0.1:6500 and the editor on 127.0.0.1:6501. Neither is routed through the Cloudflare tunnel yet. The editor has no login (SEC-1), so it needs a Cloudflare Access application in front of it before it gets a public hostname.

First deploy on 2026-11-09: pushing master to dev built and started both containers. The editor returns 200, and a publish wrote a backup and served the starter site on :6500.

Not done: running the app as a non-root user. The bind-mounted ./appdata is created root-owned by Docker, so this needs a decision on ownership (a fixed uid, or a named volume). Left as a follow-up under INFRA-2.

Commits:
- `6288191` INFRA-1: build on maintained Temurin images, bind ports to localhost (2026-11-09)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The Docker image builds again on maintained Temurin images. The app is deployed from master to /srv/apps/Gallery-CMS and running, with the site on 127.0.0.1:6500 and the editor on 127.0.0.1:6501.
<!-- SECTION:FINAL_SUMMARY:END -->
