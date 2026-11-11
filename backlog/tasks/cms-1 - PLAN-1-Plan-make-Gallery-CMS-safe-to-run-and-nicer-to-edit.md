---
id: CMS-1
title: 'PLAN-1: Plan: make Gallery-CMS safe to run and nicer to edit'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-11T11:39:00Z'
labels:
  - plan
dependencies: []
priority: high
type: task
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Epic and plan from a read of the code on 2026-11-09.

What it is: a Spring Boot + Thymeleaf editor (port 8080) that keeps the whole site in memory, saves
a JSON backup on every edit, and on "Publish" renders a static site into `appdata/html_root`, which
nginx serves on port 80.

Reported: tall images in the gallery modal are cut off (BUG-1), and the editor looks rough (EDIT-1).
Found while reading: the editor has no login, page URLs become file paths unchecked, Publish always
reports success, deleted pages stay live, and the Docker runtime image no longer exists.

Order:
1. INFRA-1, SEC-1, SEC-2: it builds, and nobody else can edit the site
2. BUG-1..7: the reported modal bug, then correctness bugs that lose or misreport work
3. EDIT-1 (with subtasks): the editor redesign
4. SITE-1..3, INFRA-2..3, TEST-1: public site polish, ops tidy-up, tests
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Every item in the plan's order (INFRA-1, SEC-1/2, BUG-1..7, EDIT-1, SITE-1..3, INFRA-2/3, TEST-1) is done
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-09: Added STORE-1 (CMS-22, subtasks CMS-22.1-22.6): move content to MongoDB and images to S3/MinIO, keeping JSON + image export/backup. It slots in between steps 2 and 3. Do it after the BUG tickets and before EDIT-1, so the redesigned controllers use the new storage interfaces. STORE-4 supersedes most of BUG-6, and STORE-3 removes most of BUG-7.

2026-11-09: Added BACKUP-1 (CMS-23, subtasks CMS-23.1-23.4): version images with every backup. Do it right after BUG-6, before real content goes into the deployed site and before STORE-1.

2026-11-11: the plan is carried out. INFRA-1 (CMS-2), SEC-1 (CMS-3, archived), SEC-2 (CMS-4), BUG-1 to BUG-7 (CMS-6 to CMS-12), EDIT-1 (CMS-13), SITE-1 to SITE-3 (CMS-14 to CMS-16), INFRA-2 (CMS-17), INFRA-3 (CMS-18) and TEST-1 (CMS-19) are all done, plus later work found along the way (SEC-3, SEC-4 apart from closing the Dependabot alert, INFRA-4, INFRA-5, BUG-8 to BUG-10, BACKUP-2 to BACKUP-4 and BACKUP-7). Still open and needing Will: BACKUP-5 (where the off-machine copy goes), BACKUP-6 criterion 3 (the 91 never-archived files, including a passport photo in gallery/test/), SEC-4 (the Dependabot alert), and the STORE epic (CMS-22).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Gallery-CMS builds and runs behind the proxy, the reported and found bugs are fixed, the editor has been redesigned, the public site is tidied and faster, and it has a test suite of 306 tests.
<!-- SECTION:FINAL_SUMMARY:END -->
