---
id: CMS-15
title: 'SITE-2: Serve right-sized images'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-10T21:34:00Z'
labels:
  - site
  - perf
dependencies:
  - CMS-8
priority: medium
type: feature
ordinal: 21000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The public grid shows the original uploads, which can be several MB each, scaled down by CSS. On publish,
generate web sizes (e.g. 600px thumbnails and a 2000px lightbox version, WebP or JPEG) and use
`srcset` and `loading="lazy"`. nginx currently sends `no-store` for everything, images included (INFRA-2).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The grid loads a copy no bigger than 900px and the full-size view one no bigger than 2000px; nothing is enlarged
- [x] #2 Phone photos come out upright
- [x] #3 A publish only resizes images it hasn't resized before
- [x] #4 Browsers can keep published images and revalidate them
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10: measured the live galleries. Only recent-work has large originals (12 paintings, up to 3899px, several MB each, all sent as grid thumbnails too). Every other gallery's files are at most 800px on their longest side (past-work 75, small-pieces 31, the-river 24, the-sea 21, life-class 15, flowers 10): they were already scaled down when they came over from the old site. The lightbox shows each painting as large as the screen allows, but never past its own pixel size (Will: "don't bump up beyond their resolution"), so those galleries need higher-resolution files re-uploaded to show bigger. Uploads keep the original file, and this ticket should then make thumbnails for the grid and a large version for the lightbox.

2026-11-10: `builder/WebImages` makes the sized copies. It reads the size without decoding, and an image already within the limit is hard-linked as is. Larger ones are decoded, turned upright from the EXIF orientation tag (read from the APP1 segment; no imaging library is available offline, so it's done by hand), scaled down in halving steps with bicubic interpolation, and written as JPEG at quality 0.85, or PNG for PNGs. Cached in `appdata/cms/web-images/<sha256 prefix>-<size>.<ext>`; entries a successful publish didn't use are pruned after the swap. Files it can't decode (e.g. CMYK JPEGs) are published untouched. The 900px grid size suits columns of up to ~460 CSS px on 2x screens. WebP isn't used because the JDK has no WebP writer. I didn't add srcset: each spot already gets one right-sized file.

Checked on a fresh copy of the live data: all 188 published paintings have both copies within their limits and nothing linked is missing. Recent Work's grid is 2.0 MB instead of 17 MB, its full-size images 8.6 MB, and the home image 434 KB instead of 2.9 MB. The first publish took 12.9 s (resizing), the second 2.5 s (cached). A real phone photo stored 4032x3024 with orientation 6 came out upright at 675x900. nginx: images get `Cache-Control: no-cache` (revalidation returns 304), while pages, CSS and JS stay no-store. Tested with nginx:1.27-alpine.

Tests: WebImagesTest (sizes, no enlarging, hard link for small images, all eight EXIF orientations, the tag read from a real file, PNG kept, cache reuse and pruning, undecodable files) and ThymeleafSiteBuilderTest (grid and full-size paths and sizes, home image).

Commits:
- `3f4836e` SITE-2: publish web-sized images for the grid and the lightbox (2026-11-10)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The published site serves right-sized images: up to 900px in the gallery grids and up to 2000px in the full-size view and on the home page. They're made once per image and cached, never enlarged, and phone photos are turned upright. Browsers can now keep images between visits.
<!-- SECTION:FINAL_SUMMARY:END -->
