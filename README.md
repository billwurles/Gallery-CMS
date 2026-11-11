# Gallery CMS

An editor for an artist's portfolio site. Pages, galleries and exhibition posts are edited in a small
Spring Boot web app. Publishing renders the whole site as static HTML, which nginx serves.

- **Editor:** Spring Boot 3, Thymeleaf, a little jQuery and Summernote (rich text). It has no login of its
  own; on the dev server it sits behind Cloudflare Access.
- **Published site:** plain HTML, CSS and images in `appdata/html_root`, served by nginx.
- **Data:** JSON backup files plus image folders under `appdata/cms`. There is no database.

## Running it

With Docker (as deployed):

```sh
docker compose up -d --build
```

- Published site: `http://<host>:6500/` on the LAN, `https://dev.burl.es/gallery-cms/` through the proxy.
- Editor: `http://<host>:6501/` on the LAN, `https://dev.burl.es/gallery-cms-editor/` through the proxy.

`SITE_BASE_PATH` (default `/gallery-cms`) is the path prefix the published links are built with. nginx
strips it, so the same files work at the root on the LAN port and under the prefix through the proxy. Set
it to empty to publish for the root only.

Locally, without Docker:

```sh
mvn spring-boot:run          # editor on :8080, data in ./appdata (application.properties)
mvn test                     # unit and MockMvc tests
```

The `prod` profile (`application-prod.properties`, used by the container) puts the data in `/appdata`.

## Deploying (dev server)

Pushing `master` to the `dev` remote builds and restarts the containers in `/srv/apps/Gallery-CMS` and
mirrors the branch to GitHub. A push doesn't restart nginx when only `nginx.conf` changed: run
`docker restart gallery-cms-nginx-1` after one.

## Data layout

```
appdata/
  cms/
    gallery/                 uploaded images: <page-url>/<random-name>.jpg, home image at the top
    web-images/              cache of resized copies made when publishing (safe to delete)
    backup/
      1-unpublished-*.json   newest draft (the editor saves one on every change)
      1-published-*.json     newest published version; older numbers are older versions
      images/<sha256>.<ext>  every image any kept backup needs, hard-linked from gallery/
  html_root/                 the published site, replaced as a whole on each publish
```

On start the editor loads the newest draft backup. Each backup file lists the images it needs, and those
images are kept in `backup/images/` for as long as any kept backup refers to them, so an older version
never loses its photos. After each backup, images that nothing uses any more are removed
(`backup.gc.dry-run=true` only logs what would go).

## How publishing works

**Publish site** in the editor:

1. Renders every page, the gallery images, the exhibition pages and the 404 page into
   `appdata/.html_root.building`.
2. Only if all of that worked, swaps the new files into `html_root`, so a failed build leaves the live site
   as it was and deleted pages disappear. The `html_root` folder itself is kept, because nginx mounts it.
3. Saves the published backup.

For each painting the grid gets `images/thumbs/<file>` (at most 900px) and the full-size view gets
`images/<file>` (at most 2000px). Images are never enlarged, and phone photos are turned upright. The
stylesheet and scripts are linked with a content hash (`style.css?v=...`), so browsers and Cloudflare
never pair new pages with old files.

## Settings

`src/main/resources/application.properties`:

| Property | Meaning |
|---|---|
| `gallery.root`, `html.root`, `backup.path` | where the data lives |
| `num.published.backups`, `num.unpublished.backups` | how many versions to keep |
| `posts.per.page` | exhibitions per page on the published site |
| `site.base-path` | prefix for published links (env `SITE_BASEPATH`) |
| `backup.gc.dry-run` | `true` to only log what the image clean-up would delete |

## Work tracking

Tasks live in `backlog/` (Backlog.md, prefix `CMS-`).
