#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
"""Removes the sessions and the answers of the REST APIs from the traces in a Playwright report, in place.

The nightly report is public. Check the result with check-e2e-report-for-secrets.py before anything publishes it.
Usage: redact-e2e-report.py <report directory> <directory with the saved sessions>
"""

import io
import json
import os
import re
import sys
import zipfile

from e2e_sessions import saved_session_values

REDACTED = "REDACTED"
SECRET_HEADERS = {"authorization", "cookie", "set-cookie", "x-csrf-token", "x-xsrf-token"}
# Playwright logs the headers of the requests of the steps as text.
LOGGED_HEADER = re.compile(rf"(?im)^(\s*(?:[<>]\s*)?(?:{'|'.join(sorted(SECRET_HEADERS))})\s*:).*$")
SECRET_PARAMETERS = re.compile(
    r"(?i)([?&#](?:code|state|session_state|session_code|iss|execution|tab_id|client_data|id_token_hint|access_token)=)"
    r"[^&#\"'\s<>\\]+"
)
# The page in the trace needs these to look like the page of ZAC.
KEPT_RESOURCES = (".css", ".js", ".mjs", ".svg", ".png", ".jpg", ".jpeg", ".gif", ".ico", ".webp", ".woff", ".woff2", ".ttf")
SOURCES = (".ts", ".js", ".feature")
# Without a background of its own the report is unreadable in a browser that shows pages transparent.
BACKGROUND = "<style>html{background-color:Canvas}</style>"


def redact(value):
    if isinstance(value, list):
        return [redact(item) for item in value]
    if isinstance(value, dict):
        if str(value.get("name", "")).lower() in SECRET_HEADERS and "value" in value:
            return {**value, "value": REDACTED}
        redacted = {}
        for key, item in value.items():
            lowered = key.lower()
            if lowered in SECRET_HEADERS and isinstance(item, str):
                redacted[key] = REDACTED
            elif lowered in {"cookies", "storagestate"}:
                redacted[key] = [] if isinstance(item, list) else None
            elif lowered == "postdata":
                redacted[key] = None
            else:
                redacted[key] = redact(item)
        return redacted
    if isinstance(value, str):
        return LOGGED_HEADER.sub(rf"\1 {REDACTED}", value)
    return value


def redact_text(text, sessions):
    for session in sessions:
        text = text.replace(session, REDACTED)
    return SECRET_PARAMETERS.sub(rf"\1{REDACTED}", text)


def redact_lines(text, sessions):
    lines = []
    for line in text.splitlines():
        try:
            line = json.dumps(redact(json.loads(line)), ensure_ascii=False, separators=(",", ":"))
        except ValueError:
            pass
        lines.append(redact_text(line, sessions))
    return "\n".join(lines) + "\n"


def redact_trace(content, sessions):
    target = io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(content)) as source, zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as redacted:
        for name in source.namelist():
            member = source.read(name)
            if name.endswith((".trace", ".network", ".stacks")):
                redacted.writestr(name, redact_lines(member.decode(), sessions))
            elif name.startswith("resources/") and name.lower().endswith(KEPT_RESOURCES) or name.endswith(SOURCES):
                redacted.writestr(name, member)
    return target.getvalue()


def main(report_directory, sessions_directory):
    sessions = saved_session_values(sessions_directory)
    traces = 0
    for directory, _, files in os.walk(os.path.join(report_directory, "data")):
        for file in files:
            path = os.path.join(directory, file)
            with open(path, "rb") as report_file:
                content = report_file.read()
            if file.endswith(".zip"):
                content = redact_trace(content, sessions)
                traces += 1
            elif file.endswith((".md", ".txt", ".json")):
                content = redact_text(content.decode(), sessions).encode()
            else:
                continue
            with open(path, "wb") as report_file:
                report_file.write(content)

    index = os.path.join(report_directory, "index.html")
    with open(index, encoding="utf-8") as report_file:
        page = report_file.read()
    with open(index, "w", encoding="utf-8") as report_file:
        report_file.write(page.replace("</head>", f"{BACKGROUND}</head>", 1))
    print(f"Redacted {traces} traces, with {len(sessions)} values of sessions.")


if __name__ == "__main__":
    main(*sys.argv[1:])
