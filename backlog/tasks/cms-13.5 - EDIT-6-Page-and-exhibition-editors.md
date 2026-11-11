---
id: CMS-13.5
title: 'EDIT-6: Page and exhibition editors'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-11T11:05:00Z'
labels:
  - editor
  - ui
dependencies:
  - CMS-13.1
parent_task_id: CMS-13
priority: medium
type: feature
ordinal: 18000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Summernote is currently loaded with Bootstrap 3 just for the editor. Use Summernote's Lite build
(no Bootstrap) or a modern editor. Generate the URL slug from the title, editable, with validation
(SEC-2). For exhibitions, replace the jQuery UI timepicker with native date inputs. Make "Back to
pages" and gallery/content switching obvious.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The rich text editor no longer needs Bootstrap
- [x] #2 Exhibitions use a native date and time input
- [x] #3 The address is generated from the title, editable, and validated
- [x] #4 Switching between a page's text and its gallery is one click
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-11: most of this came with EDIT-2 (`5b2ff52`): summernote-lite with no Bootstrap, a datetime-local input instead of the jQuery UI timepicker, and Edit gallery / Text and settings buttons, with every page in the sidebar. SEC-2 (`303da58`) added address validation. This change adds `addressField()` (fragments/summernote.html). The address is read-only with a Change button; a new page's address follows its title (an exhibition's, its date and title) until typed in, while an existing one never follows, since retitling a page used to change its address and so renamed it. Typed addresses are slugified on leaving the field, and the input has the server's pattern and 100-character limit. Checked in a browser: a new page follows its title, Change plus "Spring  Show_Archive" becomes spring-show-archive and saves there, retitling the CV keeps /cv, a new exhibition gets 2027-01-15-18-30-winter-open, and retitling an exhibition keeps its address.

Commits:
- `5b2ff52` EDIT-2: one editor layout and stylesheet (2026-11-10)
- `303da58` SEC-2: validate page addresses, uploads and image requests (2026-11-11)
- `47f3473` EDIT-6: editable addresses that don't move pages by accident (2026-11-11)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The page and exhibition editors use Summernote's standalone build and native date inputs. Addresses are generated from the title, editable and validated, and existing pages no longer change address when retitled.
<!-- SECTION:FINAL_SUMMARY:END -->
