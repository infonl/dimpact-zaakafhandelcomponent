#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
"""Fails when a file of the Playwright report holds a secret, a saved session or a logged secret header, also inside the
zip files in the report.

The nightly report is public, so run this before anything publishes it.
Usage: check-e2e-report-for-secrets.py <report directory> <directory with the saved sessions> <environment variable
with a secret>...
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

from e2e_sessions import saved_session_values

EMBEDDED_ZIP = re.compile(rb"data:application/zip;base64,([A-Za-z0-9+/=]+)")

# The log of a failed API call. A session that a test renewed is not in the saved sessions. Scripts in a trace mention
# these headers too, but not as an item of a list at the start of a line.
ANSI_CODE = rb"(?:\x1b|\\+u001b)\[[0-9;]*m"
LOGGED_HEADER = re.compile(
    rb"(?i)(?:^|[\r\n]|\\+n)(?:[ \t]|" + ANSI_CODE + rb")*-[ \t]+(?:authorization|cookie|set-cookie):[ \t]*"
    rb"(?!REDACTED|\s|\\+n)\S"
)


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
    if any(secret in content for secret in secrets) or LOGGED_HEADER.search(content):
        yield name
    if content.startswith(b"PK"):
        with zipfile.ZipFile(io.BytesIO(content)) as archive:
            for member in archive.namelist():
                yield from files_with_secret(f"{name}!{member}", archive.read(member), secrets)
    for embedded in EMBEDDED_ZIP.finditer(content):
        yield from files_with_secret(f"{name}!embedded", base64.b64decode(embedded.group(1)), secrets)


def main(report_directory, sessions_directory, variables):
    missing = [variable for variable in variables if not os.environ.get(variable)]
    if missing:
        sys.exit(f"These secrets are not set, so the report cannot be checked: {', '.join(missing)}")
    values = {os.environ[variable] for variable in variables} | saved_session_values(sessions_directory)
    secrets = set().union(*map(spellings, values))
    paths = [os.path.join(directory, file) for directory, _, files in os.walk(report_directory) for file in files]
    if not paths:
        sys.exit(f"There is no report in {report_directory}.")
    leaks = []
    for path in paths:
        with open(path, "rb") as file:
            leaks += files_with_secret(path, file.read(), secrets)
    if leaks:
        names = "\n".join(sorted(set(leaks)))
        sys.exit(f"The report holds a secret or a logged secret header and must not be published:\n{names}")
    print(f"No secret in the {len(paths)} files of the report.")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3:])
