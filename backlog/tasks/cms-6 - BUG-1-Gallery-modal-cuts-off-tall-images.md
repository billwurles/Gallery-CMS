---
id: CMS-6
title: 'BUG-1: Gallery modal cuts off tall images'
status: Done
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-10T09:50:00Z'
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
- [x] #1 A 1:3 portrait painting is fully visible with its caption at 1366x768, 1920x1080 and on a phone
- [x] #2 Wide images still fill the width
- [x] #3 Modal is centred
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-10: checked with Playwright on a published copy of the live data plus a 500x1500 test painting. Image box (left, top, width, height) and caption bottom: 1366x768 tall 203x608, caption ends at 738 of 768; 1920x1080 tall 307x920, caption at 1049; iPhone 13 (390x664) tall 168x504, caption at 634. Wide images fill the width (979px at 1366, 358px on the phone). Every image is centred on the screen.

The site had no viewport tag, so phones showed the desktop page zoomed out. It has one now, and the phone menu button and menu text are resized to suit.

Commits:
- `fcd212a` BUG-1: painting view fits tall images on screen (2026-11-10)
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The painting view is a full-screen flex column. The image is capped to the height left after the close button and the caption, so tall paintings show whole at every size. It opens with a class instead of an inline style and closes on Escape or a click outside the painting. The caption is now one title line and one line of size, medium and availability. The published site also got a viewport tag, so phones get its phone layout.
<!-- SECTION:FINAL_SUMMARY:END -->
