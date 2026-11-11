---
id: CMS-21
title: 'INFRA-4: Upgrade Spring Boot and keep dependencies current'
status: Done
assignee: []
created_date: '2026-11-09T16:52:00Z'
updated_date: '2026-11-11T10:59:00Z'
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
- [x] #1 Builds and runs on the current supported Spring Boot release
- [x] #2 No hand-pinned Spring versions in pom.xml
- [x] #3 Dependabot version updates configured
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-11: Spring Boot 4.1.2 (released 2026-10-22). 3.4 is out of support and 3.5's last release was 3.5.16 in June, so 4.1 is the supported line. Spring 7.0.10, Jackson 3.1, Tomcat 11, Thymeleaf 3.1.5, Lombok 1.18.48, JUnit 6. pom: starters and the Boot plugin are unversioned (parent-managed), web became webmvc, webmvc-test was added for MockMvc auto-configuration, and Lombok's annotation processor uses `${lombok.version}`.

Code changes for Jackson 3: `com.fasterxml.jackson.databind` became `tools.jackson.databind` (the annotations package is unchanged); `JsonNode.fields()` became `properties()`; the backup mapper is built with `JsonMapper.builder()`, ignores unknown fields and keeps `WRITE_DATES_AS_TIMESTAMPS`, so dates stay millisecond numbers as in every existing backup; and since Jackson 3 errors are unchecked, JsonBackup catches `JacksonException`, so an unreadable backup is still logged and skipped. Lombok 1.18.48 refuses `@Builder(builderClassName = "Builder")` with an imported `lombok.Builder`, so the seven model classes say `@lombok.Builder`. The test for `@AutoConfigureMockMvc` imports it from `org.springframework.boot.webmvc.test.autoconfigure`.

Checked: all 284 tests pass. On a fresh copy of the live data the editor starts with no warnings, every editor page answers (the unknown page gives 404), publishing works, and its backup has exactly the same structure and value types as the existing ones; only the sanitised content strings differ (SEC-3). The Docker image builds and serves the editor. `.github/dependabot.yml` asks for weekly Maven (Spring Boot grouped), Dockerfile and docker-compose image updates. The PRs will open on the GitHub mirror: apply updates in the working copy and push to dev, rather than merging on GitHub. The repo has no Actions workflows, so that ecosystem is left out.

Commits:
- `763b726` INFRA-4: Spring Boot 4.1.2, parent-managed versions, Dependabot (2026-11-11)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Gallery-CMS runs on Spring Boot 4.1.2, the supported release line, with every Spring and Lombok version managed by the parent. Backups keep their format. Dependabot will raise weekly PRs for Maven and Docker image updates.
<!-- SECTION:FINAL_SUMMARY:END -->
