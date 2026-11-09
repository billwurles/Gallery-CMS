---
id: CMS-22.4
title: 'STORE-5: MongoDB content repository'
status: To Do
assignee: []
created_date: '2026-11-09T17:48:00Z'
labels:
  - storage
dependencies:
  - CMS-22.2
parent_task_id: CMS-22
priority: medium
type: feature
ordinal: 32000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`MongoContentRepository` behind `storage.content=mongo`.

- Collections: `site` (single settings doc), `pages` (each with its embedded gallery list), `exhibitions`. Index on `url` (unique), and on `date` for exhibitions.
- **Drafts vs published**: each document has `draft` and `published` sub-documents (or there are separate `*_published` collections). Publishing copies draft into published in one go. This is the base for EDIT-3. A page deleted in draft is removed from published on the next publish (helps BUG-3).
- Optimistic locking (`@Version`) so two tabs can't silently overwrite each other.
- Drop the long-lived in-memory `Site` singleton. The editor reads from the repository per request. The builder takes a published snapshot.
- Compose: a `mongo` service with a pinned version, a named volume, no published port (only the `cms` service reaches it on the compose network), and credentials from `~/.config/gallery-cms/mongo.env` via `env_file` with `required: false`.
- Tests with Testcontainers.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Editor runs fully on storage.content=mongo with no in-memory Site singleton
- [ ] #2 Publish only renders published state; draft edits do not leak to html_root
- [ ] #3 Concurrent edit of the same page returns a conflict instead of losing data
- [ ] #4 Mongo is not reachable from outside the compose network; secrets come from ~/.config env file
<!-- AC:END -->
