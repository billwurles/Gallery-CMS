---
id: CMS-2
title: 'INFRA-1: Docker image no longer builds: openjdk image removed'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
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
- [ ] #1 `docker compose build` succeeds from a clean checkout
- [ ] #2 Pushing master to dev deploys and the editor comes up
<!-- AC:END -->
