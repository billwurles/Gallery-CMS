---
id: CMS-13.1
title: 'EDIT-2: One editor layout and stylesheet'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
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
