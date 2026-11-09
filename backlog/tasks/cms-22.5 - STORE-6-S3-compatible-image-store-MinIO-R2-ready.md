---
id: CMS-22.5
title: 'STORE-6: S3-compatible image store (MinIO, R2-ready)'
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
ordinal: 33000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`S3ImageStore` behind `storage.images=s3`, using AWS SDK v2 `S3Client` with `endpointOverride` and path-style access.

- Config: `s3.endpoint`, `s3.bucket`, `s3.region`, and keys from `~/.config/gallery-cms/s3.env`. The same code targets MinIO (default), Cloudflare R2 or AWS.
- Compose: a `minio` service with a pinned version and a named volume. The console is bound to 127.0.0.1 only, or omitted. A one-shot init creates the bucket.
- Upload streams the multipart file to the bucket (no base64-in-JSON, which ties into EDIT-5). Validate type and size first (SEC-2).
- The editor serves images through a controller that streams from the store, or redirects to a short-lived presigned URL.
- Publish: copy the referenced images from the bucket into `html_root/images/`. Skip unchanged ones by ETag/sha.
- Derived sizes for SITE-2 are stored under `derived/<imageId>/<width>.webp`.
- Later option, not in scope: serve the public images straight from R2 behind Cloudflare instead of copying them to html_root.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Upload, view, delete and publish all work with storage.images=s3 against MinIO
- [ ] #2 Bucket and credentials configured from env file; nothing secret in git or compose
- [ ] #3 Publish copies only referenced images and skips unchanged ones
<!-- AC:END -->
