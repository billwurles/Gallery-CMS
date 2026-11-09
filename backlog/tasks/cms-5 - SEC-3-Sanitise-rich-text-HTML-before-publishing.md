---
id: CMS-5
title: 'SEC-3: Sanitise rich-text HTML before publishing'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-09T23:51:00Z'
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
