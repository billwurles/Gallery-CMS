---
id: CMS-9
title: 'BUG-4: Gallery editor: Delete submits the form; fields keyed by order'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-10T09:50:00Z'
labels:
  - bug
  - editor
dependencies: []
priority: high
type: bug
ordinal: 9000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
In `EditGallery.html`:
- The per-painting Delete `<button>`s sit inside `#galleryForm` with no `type`, so they default to
  `submit`. Clicking Delete also fires the "save this gallery?" confirm and the save request. Use `type="button"`.
- Input ids are `title-${painting.order}` and so on. Orders can repeat (uploads start at the gallery size,
  a reorder renumbers from 1, deletes leave gaps), and then two paintings share ids and the save reads the
  wrong painting's title or medium. Key fields by filename instead, and read them from inside each `<li>`.
- The unused `confirmSave` in that page sends DELETE. Remove it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Deleting a painting doesn't trigger a save
- [x] #2 Editing metadata after reorder/upload/delete saves to the right painting
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10: Delete is `type="button"`. Each card's fields carry classes (f-title, f-dimensions, f-medium, f-sold) and are read from inside that card, keyed by its filename, with the order taken from the cards' positions. Also: cancelling the save confirm used to post the form anyway (preventDefault only ran on OK). The unused confirmSave is gone. Checked in a browser on a copy of the live data: a cancelled Delete sent no request, and after moving the first painting to the end and renaming it, one save put it last with the new title and every other title unchanged.

Commits:
- `b8b21e8` BUG-4: gallery editor saves each field to the right painting (2026-11-10)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The gallery editor's Delete no longer submits the form, and each painting's fields are saved to that painting whatever the order. The editor UI touch-up keeps the same approach.
<!-- SECTION:FINAL_SUMMARY:END -->
