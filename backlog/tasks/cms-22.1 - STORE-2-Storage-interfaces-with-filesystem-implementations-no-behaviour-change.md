---
id: CMS-22.1
title: >-
  STORE-2: Storage interfaces with filesystem implementations (no behaviour
  change)
status: To Do
assignee: []
created_date: '2026-11-09T17:47:00Z'
labels:
  - storage
dependencies:
  - CMS-2
  - CMS-4
parent_task_id: CMS-22
priority: medium
type: chore
ordinal: 29000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Put all persistence behind two interfaces so the backends can be swapped later.

- `ContentRepository`: load/save site settings (name, home image, menu order), pages, and exhibitions. Expose per-entity save/delete instead of "save the whole Site" (e.g. `savePage(page)`, `deletePage(id)`, `saveExhibition(...)`), even though the first implementation still writes one file.
- `ImageStore`: `put(key, bytes/stream, contentType)`, `get(key)` -> stream + content type, `delete(key)`, `exists(key)`, `list(prefix)`. Nothing above this layer handles a `Path` to an image.
- `FileContentRepository` wraps today's JSON behaviour. `FileImageStore` wraps `gallery.root`.
- Controllers and `ThymeleafSiteBuilder` depend only on the interfaces. Move the static `Filesystem` image helpers (upload, delete, serve, copy-on-rename, aspect-ratio sort) into a `GalleryService` that uses `ImageStore`.
- Select the backend with `storage.content=file|mongo` and `storage.images=file|s3` properties (only `file` exists in this ticket).
- Remove the duplicate `getSaveService`/`getPublishService` beans in `AppConfig`.

Key files: AppConfig.java, backup/*, helper/Filesystem.java, controllers/editor/*, builder/ThymeleafSiteBuilder.java
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 No controller or builder references gallery.root, backup.path or java.nio Files for content/images
- [ ] #2 Backend selected by storage.content / storage.images properties; file is the default
- [ ] #3 Existing appdata loads and publishes identically (same html_root output)
<!-- AC:END -->
