---
id: CMS-28
title: 'BUG-10: Deleted page text leaves a blank line above the gallery'
status: Done
assignee: []
created_date: '2026-11-10T12:38:00Z'
updated_date: '2026-11-10T12:46:00Z'
labels:
  - bug
  - site
  - editor
dependencies: []
priority: medium
type: bug
ordinal: 45000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Reported by Will: after deleting the stray "," at the top of Recent Work, a line break stays above the gallery for good. Summernote leaves `<p><br></p>` when its text is cleared, and `Page.hasContent()` only ignores whitespace, so the page still counts as having text and the site renders an empty paragraph.

Content with no visible text (only tags, `<br>`, `&nbsp;` and whitespace) should count as no content, so the page is a pure gallery. Images, video and tables still count as content.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Content with no visible text counts as no content, so the published page shows only the gallery
- [x] #2 Images, embedded video and tables still count as content
- [x] #3 Saving such content stores it as empty
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10: `Html.isBlank` treats tags, `<br>`, `&nbsp;`, zero-width spaces and whitespace as nothing to see; img, iframe, video, audio, embed, object, table and hr count. `Page.hasContent()` uses it, and PageController saves such content as "". The site's text block is left out entirely when there's no text.

Found while checking: the editor's hidden content field (a `<textarea name="content">`) sat inside the rich text box, so the editor read it as content and saved it. Live, Recent Work's content was that field followed by the stray comma, and three exhibitions also had it. The field is now outside the box on the page and exhibition editors, and `Html.clean` strips any stored copy in the Page and Exhibition builders, so it goes for every page and exhibition, including ones loaded from old backups. Checked on a copy of the live data: after deleting the comma and saving, Recent Work's content is empty and its published page has no text block. After a save, no page or exhibition in the backup has the field.

Tests: PageTest, HtmlTest (including the live Recent Work content), PageControllerTest, ThymeleafSiteBuilderTest, and ContentManagerAppTest for the editors' markup.

Commits:
- `5a8995c` BUG-9, BUG-10, EDIT-8: pagination, blank text, show text/gallery (2026-11-10)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Deleting a page's text leaves a pure gallery page with no blank line. The editor no longer saves its own hidden field into page and exhibition content, and stored copies are removed automatically.
<!-- SECTION:FINAL_SUMMARY:END -->
