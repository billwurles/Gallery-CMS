---
id: CMS-13.4
title: 'EDIT-5: Drag-and-drop upload with progress'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-09T16:11:00Z'
labels:
  - editor
  - ui
dependencies:
  - CMS-4
parent_task_id: CMS-13
priority: medium
type: feature
ordinal: 17000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Drop many images onto a gallery, with per-file progress and title, dimensions and medium editable
before saving. Send multipart uploads instead of base64 JSON, which inflates files by a third and holds
everything in memory. Resize huge originals on upload, keeping the original separately (SITE-3).
<!-- SECTION:DESCRIPTION:END -->
