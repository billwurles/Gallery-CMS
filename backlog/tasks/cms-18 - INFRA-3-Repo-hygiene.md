---
id: CMS-18
title: 'INFRA-3: Repo hygiene'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-09T23:32:00Z'
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

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-09, found while writing TEST-1:
- `target/classes/.../ContentManagerApp.class` is tracked in git and changes on every build. Remove it and ignore `target/`.
- `helper/GroupImages` is dead code and broken: `Jpeg` isn't Comparable, and its aspect ratio uses integer division. Delete it.
- `Filesystem.copyClassPathResource` never closes its streams.
<!-- SECTION:NOTES:END -->
