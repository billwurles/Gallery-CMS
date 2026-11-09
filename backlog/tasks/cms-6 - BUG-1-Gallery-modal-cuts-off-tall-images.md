---
id: CMS-6
title: 'BUG-1: Gallery modal cuts off tall images'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
labels:
  - bug
  - site
dependencies: []
priority: high
type: bug
ordinal: 6000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Reported: in the public site's painting modal you only see half of a tall (portrait) image.

Cause, in `templates/site/static/style.css` and `gallery.js`:
- `.painting-modal img` is `width: 100%; height: auto; margin-top: 75px` with **no max-height**, so a
  tall image is as tall as its width allows and overflows the screen.
- `.modal-content` has `max-height: 80%`, but its child isn't constrained by it, and `object-fit`
  does nothing on a div.
- `showModal()` sets `style.display = 'block'`, which overrides the CSS `display: flex` that was meant
  to centre the content.

Fix: modal is a flex column filling the viewport; the image gets `max-width: 100%; max-height:
calc(100dvh - <caption + close button space>); width: auto; height: auto; object-fit: contain`; drop
the 75px margin; JS toggles a class instead of inline `display`. Captions stay visible below the image.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A 1:3 portrait painting is fully visible with its caption at 1366x768, 1920x1080 and on a phone
- [ ] #2 Wide images still fill the width
- [ ] #3 Modal is centred
<!-- AC:END -->
