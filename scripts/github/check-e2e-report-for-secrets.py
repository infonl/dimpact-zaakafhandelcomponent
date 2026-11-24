#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
"""Fails when a file of the Playwright report holds a secret, also inside the zip files in the report.

The nightly report is public, so run this before anything publishes it.
Usage: check-e2e-report-for-secrets.py <report directory> <environment variable with a secret>...
"""

import base64
import html
import io
import json
import os
import re
import sys
import urllib.parse
import zipfile

EMBEDDED_ZIP = re.compile(rb"data:application/zip;base64,([A-Za-z0-9+/=]+)")


def spellings(secret):
    return {
        spelling.encode()
        for spelling in (
            secret,
            json.dumps(secret)[1:-1],
            html.escape(secret),
            urllib.parse.quote(secret, safe=""),
            urllib.parse.quote_plus(secret),
        )
    }


def files_with_secret(name, content, secrets):
    if any(secret in content for secret in secrets):
        yield name
    if content.startswith(b"PK"):
        with zipfile.ZipFile(io.BytesIO(content)) as archive:
            for member in archive.namelist():
                yield from files_with_secret(f"{name}!{member}", archive.read(member), secrets)
    for embedded in EMBEDDED_ZIP.finditer(content):
        yield from files_with_secret(f"{name}!embedded", base64.b64decode(embedded.group(1)), secrets)


def main(report_directory, variables):
    missing = [variable for variable in variables if not os.environ.get(variable)]
    if missing:
        sys.exit(f"These secrets are not set, so the report cannot be checked: {', '.join(missing)}")
    secrets = set().union(*(spellings(os.environ[variable]) for variable in variables))
    paths = [os.path.join(directory, file) for directory, _, files in os.walk(report_directory) for file in files]
    if not paths:
        sys.exit(f"There is no report in {report_directory}.")
    leaks = []
    for path in paths:
        with open(path, "rb") as file:
            leaks += files_with_secret(path, file.read(), secrets)
    if leaks:
        sys.exit("The report holds a secret and must not be published:\n" + "\n".join(sorted(set(leaks))))
    print(f"No secret in the {len(paths)} files of the report.")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2:])
