---
id: CMS-23.4
title: 'BACKUP-5: Off-machine copy of the backup folder'
status: To Do
assignee: []
created_date: '2026-11-09T23:54:00Z'
labels:
  - backup
  - infra
dependencies:
  - CMS-23.2
parent_task_id: CMS-23
priority: medium
type: feature
ordinal: 38000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The JSON backups, the image archive and the live gallery all sit on one disk under `/srv/apps/Gallery-CMS/appdata`. Losing the disk or the LXC loses everything.

- Sync `appdata/cms/backup/` (JSON plus `images/`) off the machine on a schedule, e.g. nightly plus after each publish. Because the archive is content-addressed and append-mostly, an incremental sync only sends new images.
- Target to decide: Cloudflare R2 (already on Cloudflare; S3 API, matches STORE-6), a NAS on the LAN, or both. Use `rclone sync --backup-dir` (or similar), so that files GC removes locally are kept remotely for 30 days.
- Run it as a small sidecar service in this app's compose file or as a host timer; don't touch other projects. Credentials go in `~/.config/gallery-cms/backup.env`.
- Document restoring from the remote copy onto a fresh server.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Backups and image archive are copied off the machine automatically
- [ ] #2 A restore from the remote copy onto an empty appdata has been tested and documented
<!-- AC:END -->
