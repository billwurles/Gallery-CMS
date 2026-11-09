---
id: CMS-4
title: 'SEC-2: Validate page URLs and uploaded files'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-09T23:51:00Z'
labels:
  - security
dependencies: []
priority: high
type: bug
ordinal: 4000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Page and exhibition `url` values come straight from the form and are used as paths:
`htmlRoot.resolve(url)`, `galleryRoot.resolve(url)`. A URL like `../../something` makes publish write
`index.html` outside the site folder. Restrict slugs to `[a-z0-9-]+`, and generate them from the
title by default. Reserve `exhibitions`, `include` and `images`.

Uploads take the extension from the client's filename, so `.html` or `.svg` files can end up in the
public site. Allow only jpg/png/webp/gif, check that the bytes decode as an image, and set the
extension from the detected type.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Saving a page with `..` or `/` in the URL is rejected with a clear message
- [ ] #2 Non-image uploads are rejected
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-09, found while writing TEST-1: `Filesystem.uploadPainting` takes the extension with `lastIndexOf(".")`, so an uploaded filename with no dot throws StringIndexOutOfBoundsException. Slug and upload validation tests were moved here from TEST-1.
<!-- SECTION:NOTES:END -->
