#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
"""The sessions that the end-to-end tests saved."""

import json
import os
import sys

# Shorter values, such as "true", are no secrets and occur everywhere.
MINIMUM_LENGTH = 16


def saved_session_values(directory):
    values = set()
    for folder, _, files in os.walk(directory):
        for file in files:
            with open(os.path.join(folder, file), encoding="utf-8") as session_file:
                session = json.load(session_file)
            values |= {cookie["value"] for cookie in session["cookies"] if len(cookie["value"]) >= MINIMUM_LENGTH}
    if not values:
        sys.exit(f"There are no saved sessions in {directory}, so the report cannot be checked for them.")
    return values
