<!--
  ~ SPDX-FileCopyrightText: 2026 INFO.nl
  ~ SPDX-License-Identifier: EUPL-1.2+
  -->

# Document file sizes

ZAC enforces two separate maximum file sizes, because storing a document and opening one in an
editor or converter have very different memory characteristics.

| Setting | Helm value | Environment variable | Default | Applies to |
|---|---|---|---|---|
| Maximum document size | `maxFileSizeMB` | `MAX_FILE_SIZE_MB` | 80 | Uploading and downloading a document |
| Maximum in-memory document size | `maxInMemoryFileSizeMB` | `MAX_IN_MEMORY_FILE_SIZE_MB` | 80 | Preview through PDF conversion, mail attachments, editing through WebDAV, and the inbox productaanvraag preview |

Uploads and downloads are streamed from beginning to end, and a document larger than
`maxInMemoryFileSizeMB` is uploaded to the documents registry in parts rather than base64 encoded in
a single request. Neither therefore costs heap proportional to the size of the document; they cost
temporary disk space and time. The operations behind `maxInMemoryFileSizeMB` cannot stream: they hold
the whole document in memory, which is why that limit is validated against the available heap.

A document between the two limits can be stored, listed and downloaded, but not previewed, mailed or
edited in Office. Attempting that returns `413 Payload Too Large` with
`msg.error.file.too-large-to-open`, rather than running the server out of memory.

## How large the maximum can be

`maxFileSizeMB` is configuration, not a fixed product limit, but it cannot be raised without bound:

- **2047 MB** is the absolute ceiling. The ZGW Documenten API expresses `bestandsomvang` as a 32 bit
  integer number of bytes, so a larger document cannot be described to the documents registry at all.
  Both `FileSizeConfiguration` and the chart refuse a larger value.
- **Around 768 MB** is the ceiling in practice. WildFly buffers every request body to a temporary
  file whose size is capped by `dev.resteasy.entity.file.threshold` in `configure-wildfly.cli`, which
  is 805306368 bytes. That value lives in the ZAC image rather than in the chart, so going beyond it
  takes a code change and a new build, not a `values.yaml` change.

Everything below that is a matter of configuration. The section below works out 500 MB, because that
is the size ZAC has been tested against end to end; the same steps apply to any other value.

## Raising the maximum document size

ZAC refuses to start when the configured limits do not fit in the heap, and `helm install` fails
with the same message before it gets that far. Raising `maxFileSizeMB` to 500 means changing more
than that one value.

### ZAC

- `maxFileSizeMB: 500`.
- `maxInMemoryFileSizeMB`: leave at 80 unless preview, mail and WebDAV editing have to support larger
  documents too. It needs roughly three times its value in heap, and may claim at most half the heap,
  so with the default `-Xmx1024m` the ceiling is 170. Raise `javaOptions` to go beyond that.
- `tmpVolumeSize` and `resources.requests.ephemeral-storage` / `resources.limits.ephemeral-storage`:
  WildFly buffers every request body to a temporary file under `/tmp` and ZAC streams the document
  from it, so these have to hold `maxFileSizeMB` for every concurrent transfer. The default of 4Gi
  covers roughly eight 500MB transfers at once.
- `dev.resteasy.entity.file.threshold` in `configure-wildfly.cli` caps the size of that temporary
  file, and therefore has to stay above `maxFileSizeMB` plus multipart overhead. It is 768MB, which
  leaves room for a 500MB document. Raising `maxFileSizeMB` beyond that means raising this too, which
  is a change to the image rather than to the chart.
- `nginx.client_max_body_size`: at least `maxFileSizeMB` plus multipart overhead.
- `nginx.proxy_timeout`: long enough to move a document of that size over a slow connection.

### The ingress in front of ZAC

The chart does not set these; add them to `ingress.annotations`. For the nginx ingress controller:

```yaml
nginx.ingress.kubernetes.io/proxy-body-size: 600m
nginx.ingress.kubernetes.io/proxy-read-timeout: "1800"
nginx.ingress.kubernetes.io/proxy-send-timeout: "1800"
```

### Open Zaak

- Open Zaak 1.7 or newer, which is what supports uploading a document in parts (`bestandsdelen`).
  Against an older version a document larger than `maxInMemoryFileSizeMB` cannot be stored at all.
- The nginx in front of Open Zaak needs the same `client_max_body_size` and timeouts. A part is as
  large as the documents registry decides, and Open Zaak's `DOCUMENTEN_UPLOAD_CHUNK_SIZE` defaults to
  `MIN_UPLOAD_SIZE`, 4 GiB, so by default it announces a single bestandsdeel covering the whole
  document. That nginx therefore has to accept a request body the size of the document, not the size
  of a chunk. ZAC streams a part rather than buffering it, so however the registry divides a
  document, uploading it costs ZAC no heap.
- Enough storage for the documents themselves.

## Where the limits are enforced

The frontend checks the size before uploading, but that check is advisory. The size that decides is
the number of bytes ZAC actually receives, counted while the upload is being read
(`DocumentContentReader`). An upload beyond the limit is aborted as soon as it passes it, without
ever being written out in full.
