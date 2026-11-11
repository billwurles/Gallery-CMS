---
id: CMS-18
title: 'INFRA-3: Repo hygiene'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-11T05:48:00Z'
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

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 No build output or IDE files tracked in git
- [x] #2 Dead code and the superseded deploy script removed
- [x] #3 README covers running, deploying, the data layout and publishing
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-09, found while writing TEST-1:
- `target/classes/.../ContentManagerApp.class` is tracked in git and changes on every build. Remove it and ignore `target/`.
- `helper/GroupImages` is dead code and broken: `Jpeg` isn't Comparable, and its aspect ratio uses integer division. Delete it.
- `Filesystem.copyClassPathResource` never closes its streams.

2026-11-11: untracked target/classes/.../ContentManagerApp.class and .idea/compiler.xml (both folders were already ignored; removed the redundant .gitignore line for the class file). Deleted deployLatest.sh and helper/GroupImages (unused). pom: groupId es.burl, name "Gallery CMS", a description of the project rather than the client site. The `copyClassPathResource` stream leak was fixed earlier, in BUG-5 (`372274b`). The README covers running (Docker and local), deploying, the nginx-restart note, the appdata layout, how publishing works and the settings. `mvn test` no longer dirties a tracked file.

Commits:
- `1ba4d5c` INFRA-3: repo hygiene and a README (2026-11-11)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The repo no longer tracks build output or IDE files, dead code and the old deploy script are gone, the pom describes the project, and there's a README.
<!-- SECTION:FINAL_SUMMARY:END -->
