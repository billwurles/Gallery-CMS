---
id: CMS-4
title: 'SEC-2: Validate page URLs and uploaded files'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-11T06:05:00Z'
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
- [x] #1 Saving a page with `..` or `/` in the URL is rejected with a clear message
- [x] #2 Non-image uploads are rejected
- [x] #3 Exhibition addresses follow the same rule, and the site build refuses any folder outside the site
- [x] #4 The editor serves only images of existing paintings and the current home image
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-09, found while writing TEST-1: `Filesystem.uploadPainting` takes the extension with `lastIndexOf(".")`, so an uploaded filename with no dot throws StringIndexOutOfBoundsException. Slug and upload validation tests were moved here from TEST-1.

2026-11-11: `data/Slugs`: `[a-z0-9]+(-[a-z0-9]+)*`, at most 100 characters; pages can't use exhibitions/include/images. PageController (new page and rename) re-shows the editor with an `error` message, which is also used for an address already in use now. ExhibitionController returns a 400 with the message, and the exhibition editor shows it. The editor's `slugify` drops accents and turns any other run of characters into one hyphen, so titles normally make a valid address. `ThymeleafSiteBuilder.inside()` refuses a page, exhibition or gallery folder outside its base, and fails the build so the live site is kept.

`helper/UploadedImages`: decodes the base64 and detects the type from the magic bytes (JPEG, PNG, GIF, WebP); JPEG, PNG and GIF must also decode with ImageIO (the JDK can't read WebP, so WebP is checked by header only). The saved name is a UUID plus the detected extension, which also fixes the no-dot crash. `uploadNewPaintings` checks every file before writing any. `EditorAdvice` turns `InvalidUploadException` into a 400 with the message, and the upload page shows it in the toast and now sends each file's real name.

Also closed: `GET /page/<url>/gallery/image/<file>` served any file in the gallery folder, including unused leftovers like gallery/test/ (which holds a passport photo, see BACKUP-6). It now needs an existing page and a painting with that filename, and `GET /image/<file>` needs the current home image. `Filesystem.getImageFromPainting` also refuses paths outside the gallery.

Checked in a browser on a copy of the live data: "Café & Bar_2 — Été" becomes cafe-bar-2-ete; "Images" is refused with its message; all 8 existing pages save under their current addresses; a text file named fake.jpg uploaded with a real PNG is refused, naming fake.jpg, with nothing saved; the PNG alone uploads as .png and is served. Tests: PageControllerTest (11 unsafe or reserved addresses, new and rename), ExhibitionControllerTest, FilesystemTest (extension from bytes, HTML/SVG/text refused, damaged JPEG, batch rejection, WebP, paths outside the gallery), GalleryControllerTest and EditorControllerTest (400 with reason, image serving limits), ThymeleafSiteBuilderTest (escaping address fails the publish and keeps the old site).

Commits:
- `303da58` SEC-2: validate page addresses, uploads and image requests (2026-11-11)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Page and exhibition addresses are restricted to safe slugs, and the build refuses anything that would escape the site folder. Uploads are accepted only if their bytes are a real JPEG, PNG, GIF or WebP, and are saved with the true extension. The editor serves only images that belong to the site.
<!-- SECTION:FINAL_SUMMARY:END -->
