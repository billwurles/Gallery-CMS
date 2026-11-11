---
id: CMS-5
title: 'SEC-3: Sanitise rich-text HTML before publishing'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-11T08:33:00Z'
labels:
  - security
dependencies: []
priority: medium
type: feature
ordinal: 5000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Page and exhibition content from the Summernote editor is rendered with `th:utext`, unescaped, into the public site. The real risk is pasted junk (scripts, tracking embeds, Word styling). Sanitise on save with the OWASP Java HTML Sanitizer, allowing formatting, links, images and lists.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Scripts, event handlers, forms, foreign iframes and javascript:/data: links are removed from page and exhibition text
- [x] #2 The formatting the editor makes, and everything the live content uses, is kept
- [x] #3 The live content looks the same after sanitising
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-11: OWASP Java HTML Sanitizer 20261011.1 (a month old, from before a burst of six releases in early November; no runtime dependencies). The policy in `data/Html` allows the editor's elements and attributes, `allowStyling()` for style attributes, links matching http/https/mailto/tel/relative, img src matching http/https/relative or data:image/(png|jpeg|gif|webp);base64, and iframes only from youtube.com/embed, youtube-nocookie.com/embed and player.vimeo.com/video. `Html.clean` (stray editor field, then the policy) runs in the Page and Exhibition builders, so content is sanitised on save, on backup load and on toBuilder. The policy came from a census of the live content: p, br, b, strong, span, div, table/tbody/tr/td, h2 and a; class, align and lang; font-size, font-weight, font-family, color, background-color, margin, line-height, letter-spacing, border and outline styles.

Checked: built the previous commit in a worktree, published the live backup with both versions, and screenshotted the CV, the exhibitions list and the three exhibitions on desktop and iPhone 13. All 10 are byte-identical. HtmlTest covers 10 kinds of dangerous content, the editor's formatting, and every tag, attribute and style the live content uses.

Commits:
- `26053ac` SEC-3: sanitise page and exhibition text (2026-11-11)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Page and exhibition text is sanitised with the OWASP HTML Sanitizer wherever it's created: scripts, event handlers, foreign embeds and dangerous links are removed, and the editor's formatting is kept. The live site renders identically.
<!-- SECTION:FINAL_SUMMARY:END -->
