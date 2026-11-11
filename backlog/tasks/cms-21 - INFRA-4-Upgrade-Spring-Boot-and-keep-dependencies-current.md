---
id: CMS-21
title: 'INFRA-4: Upgrade Spring Boot and keep dependencies current'
status: In Progress
assignee: []
created_date: '2026-11-09T16:52:00Z'
updated_date: '2026-11-11T08:33:00Z'
labels:
  - infra
dependencies:
  - CMS-2
priority: medium
type: chore
ordinal: 27000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
pom.xml uses Spring Boot parent 3.4.1 but pins the starters to 3.4.3 and the maven plugin to 3.4.3 by hand,
so versions disagree; the 3.4 line is also past its open-source support window, so security fixes like
the one behind SEC-4 won't keep coming. Move to the current supported Spring Boot release, drop the explicit
starter/plugin versions so the parent manages them, keep Lombok's annotation-processor version in step,
and check Thymeleaf templates still render (site build + editor) after the upgrade.

Also add `.github/dependabot.yml` for weekly Maven, Docker (base images, see INFRA-1) and GitHub Actions
version updates, so outdated dependencies show up as PRs rather than alerts. The editor's CDN libraries
(Bootstrap 3, jQuery UI, Summernote) are covered by EDIT-1.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Builds and runs on the current supported Spring Boot release
- [ ] #2 No hand-pinned Spring versions in pom.xml
- [ ] #3 Dependabot version updates configured
<!-- AC:END -->
