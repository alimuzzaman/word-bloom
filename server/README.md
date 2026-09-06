# Word Bloom catalog API (reference server)

This directory contains a small, read-only Node.js reference server. It lets the
Android app use a remotely refreshed catalog while keeping
`app/src/main/assets/categories.json` as the offline fallback. It is a development
and contract reference, not a production deployment package.

## Run locally

Node.js 18 or newer is required. From this directory:

```bash
npm test
npm start
```

The default listener is `127.0.0.1:8787`, and the default catalog is the checked-in
Android asset. To use a different validated catalog:

```bash
CATALOG_PATH=/absolute/path/categories.json PORT=8788 npm start
```

For an Android emulator, the host machine's loopback is `10.0.2.2`, so the full URL
to configure in the debug build is:

```text
http://10.0.2.2:8787/v1/catalog
```

The server binds to `127.0.0.1` by default. If a physical device or another host
must reach it during development, set `HOST=0.0.0.0` only on a trusted network and
use the machine's LAN address. Production traffic must use a TLS-terminating proxy,
authentication/rate limiting, and a restricted origin policy.

## Contract

### `GET /v1/catalog`

Returns the complete catalog. The JSON body keeps the same top-level shape as the
offline asset so Kotlin serialization can decode it without an envelope:

```json
{
  "schemaVersion": 2,
  "catalogVersion": "2026.09.02-1",
  "categories": [
    {
      "id": "fish",
      "nameEn": "Fish",
      "nameBn": "মাছ",
      "iconKey": "fish",
      "order": 2,
      "levels": []
    }
  ]
}
```

`catalogVersion` is an opaque, reviewer-owned string (for example
`2026.09.02-1`) in schema 2. It is safe to use as a local cache key; publishers must
change it for every content release. During the schema 1 migration window, the
reference server derives a `sha256-…` value when the old asset has no version. The
server also sends `X-Catalog-Version` with the same value.

The response has a strong `ETag` derived from the exact UTF-8 response bytes. A
client should persist the ETag and send `If-None-Match` on the next request. A match
returns `304 Not Modified` with no body; the client keeps its cached catalog. Weak
ETag tokens are accepted for normal HTTP cache interoperability. The response is
cacheable for five minutes and may be served stale for one day while revalidating.

Schema 2 levels add `ageBand`, a required `heroWord`, a `wordMeanings` array, and a
bilingual `lesson`:

```json
{
  "ageBand": "6-7",
  "wordMeanings": [
    {
      "word": "COD",
      "definitionEn": "A sea fish.",
      "translationBn": "কড মাছ",
      "meaningBn": "এক ধরনের সামুদ্রিক মাছ।"
    }
  ],
  "lesson": {
    "sentenceEn": "The cod swims in the sea.",
    "sentenceBn": "কড মাছ সাগরে সাঁতার কাটে।",
    "usedWords": ["COD"]
  }
}
```

Every schema 2 target word must have exactly one `wordMeanings` entry. A level's
`lesson.usedWords` and English sentence must cover every target word exactly once
as a whole word. Category introductions, completion text, and review status are
also required for schema 2. The category `storyWords` list must exactly match the
unique target words across all of its levels, and `storyEn` must mention each one
as an English whole word; `storyBn` must be non-empty.

`HEAD /v1/catalog` returns the same cache/version headers and content length without
a body. `GET /v1/categories/:categoryId` returns a small envelope containing the
same `schemaVersion`/`catalogVersion` and one `category`; the Android client should
use `/v1/catalog` for an atomic catalog update. `GET /healthz` returns status and the
current version without level data.

### Errors and methods

Only `GET`, `HEAD`, and `OPTIONS` are accepted. Mutation methods return `405` with
`Allow: GET, HEAD, OPTIONS`. Unknown routes return `404`; malformed category IDs
return `400`; a missing category returns `404`. Errors are JSON objects such as
`{"error":"category_not_found"}`.

## Configuration

| Variable | Default | Meaning |
| --- | --- | --- |
| `CATALOG_PATH` | `app/src/main/assets/categories.json` | Absolute or relative path to the catalog file |
| `HOST` | `127.0.0.1` | Bind address; loopback is the safe development default |
| `PORT` | `8787` | TCP port from 1–65535 |
| `CORS_ORIGIN` | unset | One explicit `http(s)` origin or `*`; unset sends no CORS header |

The server reads one file at startup, caps it at 5 MiB, validates schema versions 1
and 2 plus grid invariants, and then serves an immutable in-memory snapshot. New
remote releases should use schema 2 and include a string `catalogVersion`. It never
accepts a file path, catalog body, or write operation from a request. Restart the
process to publish a new file and get a new version/ETag.

## Security and publishing rules

- Keep this reference listener on loopback or behind a trusted reverse proxy.
- Use HTTPS, authentication, rate limits, and access logs before exposing an API to
  the public internet.
- Set `CORS_ORIGIN` to the smallest required origin; do not use `*` with credentials.
- Treat catalog text as untrusted display data. The app must not interpret it as
  HTML, code, a URL, or a command.
- Validate and review Bengali meanings, sentences, and child suitability before
  publishing. The server checks structure and crossword integrity, not educational
  correctness.
- Keep the offline asset packaged in the APK. A failed, stale, or incompatible
  network response must never remove the last known-good catalog.
