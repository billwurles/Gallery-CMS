---
id: CMS-22
title: >-
  STORE-1: Move content to a document DB and images to object storage, keep JSON
  + image export
status: To Do
assignee: []
created_date: '2026-11-09T17:47:00Z'
labels:
  - plan
  - storage
dependencies: []
priority: medium
type: task
ordinal: 28000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Epic and plan from a read of the storage code on 2026-11-09.

## How storage works today
- The whole site is one in-memory `Site` object (`AppConfig.getSite()`), loaded at startup from the newest JSON in `backup.path`.
- Every edit calls `BackupSite.backup(site)`, which writes the *entire* site as a new pretty-printed JSON file and renames the older ones (`1-unpublished-<time>-site-data-backup.json` and so on). Publish does the same with `-published`. `JsonBackup` is both the database and the backup.
- Images are files under `gallery.root/<page-url>/<uuid>.<ext>`. Controllers call static `Filesystem.*` helpers with `galleryRoot` directly. Renaming a page copies the folder (and leaves the old one behind, see BUG-7).
- Publish (`ThymeleafSiteBuilder`) reads images from `galleryRoot` and writes static HTML to `html_root` for nginx.

The problems: nothing is transactional, a crash mid-write can leave a broken newest file (BUG-6), images are keyed by a URL that can change, and storage is spread across controllers.

## Target
- **Content** (site settings, pages, galleries/paintings, exhibitions) lives in a document store. Recommended: **MongoDB** (Spring Data MongoDB, a `mongo` service in compose bound to 127.0.0.1, with a named volume). The documents map almost one-to-one onto the current Jackson model.
- **Images** live in an **S3-compatible bucket**, using the AWS SDK v2 with a configurable endpoint. Run **MinIO** in compose by default; the same code works against Cloudflare R2 or AWS later. Keys are `images/<imageId>.<ext>` (plus `derived/` for SITE-2 sizes), with no page URL in the key.
- **Export/backup stays a first-class format**: `site.json` (with `schemaVersion`) plus an `images/` folder, written as a directory or a `.zip`. It doesn't depend on the live store, so a backup can be restored into any backend, including back to plain files.
- Publish still renders static HTML to `html_root`. It pulls image bytes from the image store instead of the filesystem.

## Alternative considered
S3 only, with content stored as JSON objects in the bucket (one per page/exhibition plus `site.json`). That means one fewer container and is fine for a single editor, but there are no queries and no atomic multi-document updates, and drafts vs published would have to be hand-rolled. STORE-2 makes this a drop-in `ContentRepository` implementation, so the choice can be revisited without rework.

## Order
1. STORE-2 storage interfaces, with filesystem implementations that behave exactly as today (pure refactor)
2. STORE-3 stable IDs and a schema version in the model
3. STORE-4 export / backup / restore service (replaces JsonBackup; supersedes most of BUG-6)
4. STORE-5 MongoDB content repository
5. STORE-6 S3/MinIO image store
6. STORE-7 migrate existing appdata and switch over

Sequencing with the rest of the backlog: do this after INFRA-1/SEC-1/SEC-2 and the BUG tickets that are not about storage, and **before EDIT-1**, so the redesigned controllers are written against the new interfaces. EDIT-3 (drafts vs published) should build on STORE-5's draft/published model. SITE-2 (right-sized images) should store derivatives via STORE-6.
<!-- SECTION:DESCRIPTION:END -->
