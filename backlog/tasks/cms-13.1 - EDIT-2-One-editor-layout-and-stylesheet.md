---
id: CMS-13.1
title: 'EDIT-2: One editor layout and stylesheet'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-10T09:57:00Z'
labels:
  - editor
  - ui
dependencies: []
parent_task_id: CMS-13
priority: high
type: feature
ordinal: 14000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Drop Bootstrap 3, the jQuery UI theme and the copied site CSS from the editor. Write one small editor
stylesheet with CSS variables for colour, spacing and type, or use a classless base such as Pico.css.
Layout: a header with the site name, the draft/published status and the Publish button, plus a sidebar
with Dashboard, Pages, Exhibitions and Home image. The editor stays visually separate from the artist's
site. Toast messages replace `alert()`, and a styled confirm dialog replaces `confirm()` for destructive actions.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Bootstrap 3, the jQuery UI theme and the copied site CSS are gone from the editor; one stylesheet with CSS variables
- [x] #2 Every editor page shares the sidebar layout chosen by Will, and it works on a phone
- [x] #3 Toasts and a styled confirm dialog replace alert() and confirm()
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10: Will picked from three mockups (options page: https://claude.ai/artifact/KKqFw98wf5C6GHVcTxtQKb): option B's left sidebar with option A's light styling and blue accent. "+ New page" goes at the top of the sidebar list with B's dashed border. The page-order drag handle is three close lines making a square, not two columns of three dots.

2026-11-10: Built as chosen. `static/css/editor.css` holds the stylesheet, `fragments/head.html` the shared head, `fragments/navigation.html` the sidebar, and `fragments/summernote.html` the rich text editor (summernote-lite, no Bootstrap) used by the page and exhibition editors. `static/js/scripts.js` has toast(), flash() (a toast shown after the next page load), confirmDialog(), confirmDelete() (data-url, data-what) and confirmSave(). `EditorAdvice` supplies `siteName` to every view. The exhibition date uses a native datetime-local field instead of the timepicker add-on.

Checked in a browser on a copy of the live data: menu reorder by dragging the grip; delete with the dialog (cancel keeps the page, confirm deletes it); gallery edit and save; page rename and save; new exhibition; image upload; Publish with its toast. No script errors on any page. ContentManagerAppTest checks that every editor page renders with editor.css and the sidebar, with no Bootstrap or old CSS.

Not in this task: the draft/published status in the header is EDIT-3 (CMS-13.2). The layout covers some of EDIT-4 (card grid), EDIT-5 (drag-and-drop upload; still no progress bar) and EDIT-7 (mobile), so check those against what's here before starting them.

Commits:
- `5b2ff52` EDIT-2: one editor layout and stylesheet (2026-11-10)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The editor has one light layout: a left sidebar with the site name, + New page, every page and Publish, which becomes a top bar on phones. One CSS-variable stylesheet replaces Bootstrap, the jQuery UI theme and the copied site CSS. In-page toasts and a confirm dialog replace browser alerts.
<!-- SECTION:FINAL_SUMMARY:END -->
