---
id: CMS-3
title: 'SEC-1: Editor has no login and is published on port 8080'
status: To Do
assignee: []
created_date: '2026-11-09T16:11:00Z'
updated_date: '2026-11-09T23:32:00Z'
labels:
  - security
dependencies:
  - CMS-2
priority: high
type: bug
ordinal: 3000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
docker-compose publishes 8080, and every editor route (upload, delete page, delete painting,
publish) is open to anyone who can reach it. Add Spring Security form login with an admin user
from env (password hash, not plain text), CSRF protection (the jQuery/fetch calls must send the
token), and stop publishing 8080 to the world: bind it to 127.0.0.1 or put it behind the proxy on
a separate hostname with HTTPS. `/save` (publish) is a GET, so make it a POST, otherwise CSRF
protection doesn't cover it.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Every editor URL redirects to login when signed out
- [ ] #2 State-changing requests are POST/DELETE with CSRF tokens
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-09: Add auth tests to the TEST-1 suite. Every editor URL in ContentManagerAppTest should reject anonymous requests and render for a logged-in user.
<!-- SECTION:NOTES:END -->
