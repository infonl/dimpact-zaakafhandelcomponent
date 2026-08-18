#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

# Data-only image that ships a pre-built OWASP Dependency-Check NVD database
# (the `dependency-check-data` directory). It is never executed; the
# "Publish NVD Database Cache" workflow builds and pushes it, and the
# "Dependency Security Scan" workflow pulls it and extracts the database with
# `docker cp`. This way the scan never has to download from the rate-limited
# NVD API itself, and the database survives the 10GB GitHub Actions cache
# eviction that previously caused repeated cold downloads and timeouts.
#
# A tiny busybox base (rather than `scratch`) is used so that `docker create`
# and `docker cp` work reliably and the image can be inspected if needed.
FROM busybox:1.38.0@sha256:dc2d74b28e4cf8984fa52af1f39bc7c3d9c73760b41a74d629f5d11b1ab28616

COPY data /dependency-check-data
COPY build-metadata.txt /nvd-build-metadata.txt

# This image is only ever used as a `docker cp` source and is never executed,
# but a non-root user is set to satisfy image-hardening policy (DS-0002).
# 65534 is the conventional "nobody" uid/gid; `docker cp` reads the layer via the
# daemon regardless of this user, so extraction is unaffected.
USER 65534:65534

# No container is ever run from this image, so no HEALTHCHECK applies; this
# satisfies image-hardening policy (DS-0026).
HEALTHCHECK NONE
