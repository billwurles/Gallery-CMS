---
id: CMS-9
title: 'BUG-4: Gallery editor: Delete submits the form; fields keyed by order'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
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
- [ ] #1 Deleting a painting doesn't trigger a save
- [ ] #2 Editing metadata after reorder/upload/delete saves to the right painting
<!-- AC:END -->
