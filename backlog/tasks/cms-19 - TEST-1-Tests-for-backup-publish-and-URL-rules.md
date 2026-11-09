---
id: CMS-19
title: 'TEST-1: Tests for backup, publish and URL rules'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-09T23:32:00Z'
labels:
  - test
dependencies: []
priority: medium
type: feature
ordinal: 25000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`ContentManagerAppTest` is an empty class. Add tests for: backup rotation and restore order (BUG-6),
site build output for pages, galleries and exhibition pagination, slug validation (SEC-2), and controller
smoke tests with security enabled (SEC-1).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Tests for backup rotation and restore order
- [x] #2 Tests for site build output: pages, galleries, exhibition pagination
- [x] #3 MockMvc tests for every editor controller endpoint
- [x] #4 Full-app smoke test that starts from a backup, renders every editor page and publishes
- [x] #5 Coverage report on mvn verify
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
The suite has 130 tests, run with `mvn test` (about 15s). `mvn verify` also writes a JaCoCo report to target/site/jacoco. Line coverage is 88%. Most of the uncovered code is error handling, plus the dead GroupImages class.

- data/: Site, Page, Gallery, ExhibitionRepo, model defaults, Site JSON round-trip (the backup format)
- backup/JsonBackupTest: numbering, rotation limits, published vs draft, restore order and fallbacks
- helper/FilesystemTest: upload/decode, delete, serve, copy on rename, classpath copy, aspect-ratio sort
- builder/ThymeleafSiteBuilderTest: renders the real public templates into a temp dir and checks the files and HTML
- controllers/editor/*: standalone MockMvc per controller with BackupSite mocked
- ContentManagerAppTest: full context on temp appdata seeded with a backup

The tests pin the current behaviour that is correct. Known bugs are not asserted and no tests are disabled. The bugs found while writing them are recorded on CMS-4, CMS-10, CMS-11 and CMS-18. Each bug fix should add its own regression test.

Moved out of scope: slug validation tests go with SEC-2 (CMS-4), and auth tests go with SEC-1 (CMS-3).

Commits:
- `d02f922` TEST-1: unit test suite for model, backup, filesystem, builder and controllers (2026-11-09)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added a 130-test JUnit 5 suite covering the model, JSON backups, filesystem helpers, static site builder and every editor controller endpoint, plus a full-app smoke test. JaCoCo coverage report on `mvn verify`, at 88% of lines. All tests pass on the current code. Bugs the tests exposed are recorded on their tickets rather than asserted.
<!-- SECTION:FINAL_SUMMARY:END -->
