---
id: CMS-18
title: 'INFRA-3: Repo hygiene'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
labels:
  - infra
dependencies: []
priority: low
type: chore
ordinal: 24000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`target/` is ignored, but `target/classes/.../ContentManagerApp.class` is still committed; remove it. The
same goes for `.idea/compiler.xml`. `deployLatest.sh` is superseded by the dev remote's post-receive hook.
`pom.xml` groupId and description still name a single client site from the iWeb import. `GroupImages` is
dead code. Add a README covering how to run, the appdata layout, and how publish works.
<!-- SECTION:DESCRIPTION:END -->
