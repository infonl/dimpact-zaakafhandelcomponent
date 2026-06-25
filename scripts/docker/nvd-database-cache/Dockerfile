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
FROM busybox:1.38.0@sha256:fd8d9aa63ba2f0982b5304e1ee8d3b90a210bc1ffb5314d980eb6962f1a9715d

COPY data /dependency-check-data
COPY build-metadata.txt /nvd-build-metadata.txt

# This image is only ever used as a `docker cp` source and is never executed,
# but a non-root user is set to satisfy image-hardening policy (DS-0002).
# 65534 is the conventional "nobody" uid/gid; `docker cp` reads the layer via the
# daemon regardless of this user, so extraction is unaffected.
USER 65534:65534
