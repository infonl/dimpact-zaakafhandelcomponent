#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
"""Checks the Architecture Decision Records in docs/adr. See docs/adr/README.md for the rules."""

import argparse
import re
import subprocess
import sys
from pathlib import Path

ADR_DIRECTORY = Path("docs/adr")
NON_ADR_FILES = {"README.md", "template.md"}
ADR_FILE_NAME = re.compile(r"^(\d{4})-[a-z0-9]+(?:-[a-z0-9]+)*\.md$")
STATUSES = {"proposed", "accepted", "rejected", "deprecated", "superseded"}
FINAL_STATUSES = STATUSES - {"proposed"}
ALLOWED_TRANSITIONS = {"accepted": {"accepted", "deprecated", "superseded"}}
FRONT_MATTER_KEYS_CHANGEABLE_WHEN_FINAL = {"status", "superseded-by"}
REQUIRED_FRONT_MATTER_KEYS = ["status", "date", "decision-makers", "tags"]
REQUIRED_SECTIONS = [
    "## Context and Problem Statement",
    "## Considered Options",
    "## Decision Outcome",
    "### Consequences",
]
ADR_NUMBER = re.compile(r"^\d{4}$")
DATE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
TAGS = re.compile(r"^\[[a-z0-9-]+(?:, [a-z0-9-]+)*\]$")
INDEX_ROW = re.compile(r"^\|\s*\[(\d{4})\]\(([^)]+)\)\s*\|\s*(.+?)\s*\|\s*([a-z]+)\s*\|\s*(.+?)\s*\|\s*$")


def split_front_matter(text: str) -> tuple[dict[str, str], str] | None:
    lines = text.split("\n")
    if lines[0] != "---" or "---" not in lines[1:]:
        return None
    end = lines.index("---", 1)
    front_matter = {}
    for line in lines[1:end]:
        key, separator, value = line.partition(":")
        if not separator:
            return None
        front_matter[key.strip()] = value.strip()
    return front_matter, "\n".join(lines[end + 1:])


def title_of(body: str) -> str | None:
    for line in body.split("\n"):
        if line.startswith("# "):
            return line[2:].strip()
    return None


def check_adr(path: Path, front_matter: dict[str, str], body: str, adr_numbers: set[str]) -> list[str]:
    errors = []
    for key in REQUIRED_FRONT_MATTER_KEYS:
        if not front_matter.get(key):
            errors.append(f"{path}: front matter misses '{key}'")
    status = front_matter.get("status", "")
    if status and status not in STATUSES:
        errors.append(f"{path}: status '{status}' is not one of {sorted(STATUSES)}")
    if front_matter.get("date") and not DATE.match(front_matter["date"]):
        errors.append(f"{path}: date '{front_matter['date']}' is not in the format YYYY-MM-DD")
    if front_matter.get("tags") and not TAGS.match(front_matter["tags"]):
        errors.append(f"{path}: tags must be a list of lowercase kebab-case words, such as [security, ci]")
    for key in ("supersedes", "superseded-by"):
        reference = front_matter.get(key)
        if reference and (not ADR_NUMBER.match(reference) or reference not in adr_numbers):
            errors.append(f"{path}: '{key}: {reference}' does not refer to an existing ADR number")
    if (status == "superseded") != bool(front_matter.get("superseded-by")):
        errors.append(f"{path}: an ADR has 'superseded-by' if and only if its status is 'superseded'")
    if not title_of(body):
        errors.append(f"{path}: misses a '# Title' heading")
    lines = body.split("\n")
    for section in REQUIRED_SECTIONS:
        if section not in lines:
            errors.append(f"{path}: misses the section '{section}'")
    return errors


def check_index(adrs: dict[str, tuple[Path, dict[str, str], str]]) -> list[str]:
    readme = ADR_DIRECTORY / "README.md"
    rows = {}
    for line in readme.read_text(encoding="utf-8").split("\n"):
        match = INDEX_ROW.match(line)
        if match:
            number, link, title, status, _ = match.groups()
            rows[number] = (link, title, status)
    errors = []
    for number, (path, front_matter, body) in adrs.items():
        if number not in rows:
            errors.append(f"{readme}: the index misses ADR {number}")
            continue
        link, title, status = rows[number]
        if link != path.name:
            errors.append(f"{readme}: the index links ADR {number} to '{link}' instead of '{path.name}'")
        if title != title_of(body):
            errors.append(f"{readme}: the index title of ADR {number} differs from its '# Title' heading")
        if status != front_matter.get("status"):
            errors.append(f"{readme}: the index status of ADR {number} differs from its front matter")
    for number in rows.keys() - adrs.keys():
        errors.append(f"{readme}: the index lists ADR {number}, which does not exist")
    return errors


def file_at(base: str, path: Path) -> str | None:
    result = subprocess.run(["git", "show", f"{base}:{path.as_posix()}"], capture_output=True, text=True)
    return result.stdout if result.returncode == 0 else None


def files_at(base: str) -> list[Path]:
    result = subprocess.run(
        ["git", "ls-tree", "--name-only", f"{base}:{ADR_DIRECTORY.as_posix()}"],
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        return []
    return [ADR_DIRECTORY / name for name in result.stdout.split() if ADR_FILE_NAME.match(name)]


def check_immutability(base: str) -> list[str]:
    errors = []
    for path in files_at(base):
        base_parts = split_front_matter(file_at(base, path) or "")
        if base_parts is None:
            continue
        base_front_matter, base_body = base_parts
        base_status = base_front_matter.get("status")
        if base_status not in FINAL_STATUSES:
            continue
        if not path.exists():
            errors.append(f"{path}: an ADR with status '{base_status}' must not be deleted or renamed")
            continue
        parts = split_front_matter(path.read_text(encoding="utf-8"))
        if parts is None:
            continue
        front_matter, body = parts
        if body != base_body:
            errors.append(f"{path}: the body of an ADR with status '{base_status}' must not change; write a new ADR")
        for key in (front_matter.keys() | base_front_matter.keys()) - FRONT_MATTER_KEYS_CHANGEABLE_WHEN_FINAL:
            if front_matter.get(key) != base_front_matter.get(key):
                errors.append(f"{path}: '{key}' of an ADR with status '{base_status}' must not change")
        status = front_matter.get("status")
        if status != base_status and status not in ALLOWED_TRANSITIONS.get(base_status, set()):
            errors.append(f"{path}: the status must not change from '{base_status}' to '{status}'")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", help="Git revision to compare with, to check that final ADRs do not change")
    arguments = parser.parse_args()

    errors = []
    adrs = {}
    for path in sorted(ADR_DIRECTORY.glob("*.md")):
        if path.name in NON_ADR_FILES:
            continue
        match = ADR_FILE_NAME.match(path.name)
        if not match:
            errors.append(f"{path}: the file name does not match NNNN-short-title.md")
            continue
        number = match.group(1)
        if number in adrs:
            errors.append(f"{path}: ADR number {number} is also used by {adrs[number][0]}")
            continue
        parts = split_front_matter(path.read_text(encoding="utf-8"))
        if parts is None:
            errors.append(f"{path}: does not start with a valid '---' front matter block")
            continue
        adrs[number] = (path, *parts)

    for path, front_matter, body in adrs.values():
        errors += check_adr(path, front_matter, body, set(adrs))
    errors += check_index(adrs)
    if arguments.base:
        errors += check_immutability(arguments.base)

    for error in errors:
        print(error, file=sys.stderr)
    if errors:
        print(f"\n{len(errors)} ADR error(s) found. See docs/adr/README.md for the rules.", file=sys.stderr)
        return 1
    print(f"{len(adrs)} ADRs are valid.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
