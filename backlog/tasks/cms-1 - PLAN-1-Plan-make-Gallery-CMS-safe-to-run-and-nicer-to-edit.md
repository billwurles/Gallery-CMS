---
id: CMS-1
title: 'PLAN-1: Plan: make Gallery-CMS safe to run and nicer to edit'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
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
