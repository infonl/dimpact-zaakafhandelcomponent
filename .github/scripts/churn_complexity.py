#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.11"
# dependencies = ["lizard>=1.17", "javalang>=0.13"]
# ///
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
"""Churn vs complexity map of the code base.

Plots every source file by how often it changes (churn) and how hard it is to work in (complexity),
after Michael Feathers, "Getting Empirical about Refactoring", and the turbulence gem.

    uv run .github/scripts/churn_complexity.py                 # writes build/churn-complexity/index.html
    uv run .github/scripts/churn_complexity.py --ignore-rev <sha> --frames none --open

The history is read from the default branch's current commit (not --all, not --first-parent), without
merge commits. The complexity of every file is measured on the blob in that commit, never on the
working tree, so local changes do not leak into the result.

Complexity tools, all reporting McCabe cyclomatic complexity per function:
  Kotlin       detekt CyclomaticComplexMethod (the detekt version from gradle/libs.versions.toml)
  TS/JS        ESLint `complexity` rule, with the TypeScript compiler API counting the declared functions
  Java         lizard (fallback), with javalang counting the declared functions
  other        lizard (fallback)

Requirements: git, uv, a JDK (detekt), Node.js and the npm dependencies of the frontend (`npm ci` in
src/main/app) for ESLint. Tool results are cached per blob in --cache-dir, so a rerun only measures new blobs.
"""

from __future__ import annotations

import argparse
import collections
import datetime as dt
import fnmatch
import hashlib
import json
import math
import multiprocessing
import os
import re
import shutil
import sqlite3
import statistics
import subprocess
import sys
import tempfile
import urllib.request
import xml.etree.ElementTree as ElementTree
from dataclasses import dataclass, field
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
ASSET_DIR = SCRIPT_DIR / "churn_complexity"
CACHE_VERSION = "1"
DEFAULT_DETEKT_VERSION = "2.0.0-alpha.6"
SECONDS_PER_YEAR = 365 * 24 * 3600

QUADRANTS = ["Danger Zone", "Cowboy Code", "Fertile Ground", "Healthy Closure"]

LANGUAGE_BY_EXTENSION = {
    "kt": "kotlin", "kts": "kotlin", "java": "java",
    "ts": "typescript", "tsx": "typescript", "mts": "typescript", "cts": "typescript",
    "js": "javascript", "jsx": "javascript", "mjs": "javascript", "cjs": "javascript",
    "py": "python", "go": "go", "rb": "ruby", "rs": "rust", "swift": "swift", "scala": "scala",
    "c": "c", "h": "c", "cpp": "cpp", "cc": "cpp", "hpp": "cpp", "cs": "csharp", "php": "php", "lua": "lua",
}
ESLINT_LANGUAGES = {"typescript", "javascript"}
LOCKFILE_NAMES = {
    "package-lock.json", "npm-shrinkwrap.json", "yarn.lock", "pnpm-lock.yaml", "bun.lockb", "gradle.lockfile",
    "Cargo.lock", "Gemfile.lock", "poetry.lock", "uv.lock", "Pipfile.lock", "go.sum", "composer.lock",
    "skills-lock.json",
}
TEST_DIRECTORY_NAMES = {"test", "tests", "itest", "e2e", "__tests__", "spec", "specs", "testing", "integrationTest"}
FIXTURE_DIRECTORY_NAMES = {"fixtures", "fixture", "__fixtures__", "__mocks__", "mocks", "testdata", "test-data"}
TEST_FILE_PATTERNS = [
    "*.spec.*", "*.test.*", "*_test.*", "test_*.py", "*Test.kt", "*Tests.kt", "*IT.kt", "*Test.java",
    "*Tests.java", "*IT.java", "setupJest.*", "globalJest.*", "jest.config.*", "playwright.config.*",
    "cucumber.*", "karma.conf.*", "conftest.py",
]
FIXTURE_FILE_PATTERNS = [
    "*Fixtures.kt", "*Fixture.kt", "*.fixture.*", "*.fixtures.*", "*.test-fixtures.*", "*.mock.*", "*.mocks.*",
]
EXCLUSION_GROUPS = ["tests", "fixtures", "generated", "vendored", "lockfiles", "build output", "tooling & config"]
HOTSPOT_TOOL_MARKERS = [".codeclimate.yml", ".codeclimate.json", ".codescene", "codescene.yml", ".turbulence"]


def log(message: str) -> None:
    print(message, file=sys.stderr, flush=True)


# --------------------------------------------------------------------------------------------- git


class Git:
    def __init__(self, repo: Path):
        self.repo = repo

    def run(self, *args: str, binary: bool = False, check: bool = True, stdin: bytes | None = None):
        completed = subprocess.run(
            ["git", "-c", "core.quotepath=off", *args], cwd=self.repo, input=stdin, capture_output=True, check=False
        )
        if check and completed.returncode != 0:
            raise RuntimeError(f"git {' '.join(args)} failed: {completed.stderr.decode(errors='replace')}")
        return completed.stdout if binary else completed.stdout.decode("utf-8", errors="surrogateescape")

    def tree(self, commit: str) -> dict[str, str]:
        """path -> blob sha of every regular file in the commit."""
        files = {}
        for entry in self.run("ls-tree", "-r", "-z", "--full-tree", commit).split("\0"):
            if not entry:
                continue
            meta, path = entry.split("\t", 1)
            mode, kind, sha = meta.split()
            if kind == "blob" and mode != "120000":
                files[path] = sha
        return files

    def blobs(self, shas: list[str]):
        """Yields (sha, bytes) for every blob, streamed through one `git cat-file --batch`."""
        process = subprocess.Popen(
            ["git", "cat-file", "--batch"], cwd=self.repo, stdin=subprocess.PIPE, stdout=subprocess.PIPE
        )
        import threading

        def feed():
            for sha in shas:
                process.stdin.write(sha.encode() + b"\n")
            process.stdin.close()

        threading.Thread(target=feed, daemon=True).start()
        for _ in shas:
            header = process.stdout.readline().split()
            size = int(header[2])
            content = process.stdout.read(size)
            process.stdout.read(1)
            yield header[0].decode(), content
        process.wait()


@dataclass
class Commit:
    index: int
    sha: str
    time: int
    author: str
    subject: str
    changes: list  # (status, old path, new path)
    numstat: dict  # new path -> (added, deleted)


def parse_log(git: Git, ref: str) -> list[Commit]:
    raw = git.run(
        "log", ref, "--no-merges", "--topo-order", "--reverse", "-M", "-l0", "--raw", "--numstat", "-z",
        "--format=%x1e%H%x1f%at%x1f%aN%x1f%s", binary=True,
    )
    commits = []

    def decode(token: bytes) -> str:
        return token.decode("utf-8", errors="surrogateescape")

    for record in raw.split(b"\x1e")[1:]:
        tokens = record.split(b"\0")
        sha, time, author, subject = decode(tokens[0]).split("\x1f", 3)
        changes, numstat = [], {}
        position = 1
        while position < len(tokens):
            token = decode(tokens[position]).lstrip("\n")
            position += 1
            if not token:
                continue
            if token.startswith(":"):
                status = token.split()[-1]
                if status[0] in "RC":
                    old, new = decode(tokens[position]), decode(tokens[position + 1])
                    position += 2
                else:
                    old = new = decode(tokens[position])
                    position += 1
                changes.append((status[0], old, new))
            else:
                added, deleted, path = token.split("\t", 2)
                if path == "":
                    path = decode(tokens[position + 1])
                    position += 2
                numstat[path] = (int(added) if added != "-" else 0, int(deleted) if deleted != "-" else 0)
        commits.append(Commit(len(commits), sha, int(time), author, subject, changes, numstat))
    return commits


# ------------------------------------------------------------------------------- lineages (renames)


@dataclass
class Lineage:
    id: int
    paths: list  # (commit index, path) in order
    born: int | None  # commit index of creation, None when created by a merge we skipped
    events: list = field(default_factory=list)  # (commit index, added, deleted)


@dataclass
class History:
    commits: list
    lineages: list
    path_history: dict  # path -> sorted [(commit index, lineage id)]
    live: dict  # path -> lineage id after replaying everything
    renames: list  # (sha, old, new, kind)
    mass_candidates: list
    ignored: set


def replay(commits: list[Commit], ignored: set[str], mass_threshold: float, link_conversions: bool) -> History:
    lineages: list[Lineage] = []
    live: dict[str, int] = {}
    retired: dict[str, int] = {}
    path_history: dict[str, list] = collections.defaultdict(list)
    renames, mass_candidates = [], []

    def new_lineage(path: str, index: int, born: int | None) -> int:
        lineage = Lineage(len(lineages), [(index, path)], born)
        lineages.append(lineage)
        path_history[path].append((index, lineage.id))
        return lineage.id

    def move(lineage_id: int, old: str, new: str, index: int):
        live.pop(old, None)
        retired[old] = lineage_id
        live[new] = lineage_id
        lineages[lineage_id].paths.append((index, new))
        path_history[new].append((index, lineage_id))

    for commit in commits:
        index = commit.index
        is_ignored = commit.sha in ignored
        if live and len(commit.changes) / len(live) > mass_threshold:
            mass_candidates.append({
                "sha": commit.sha, "date": iso_date(commit.time), "author": commit.author, "subject": commit.subject,
                "files": len(commit.changes), "share": round(len(commit.changes) / len(live), 3),
            })
        changes = list(commit.changes)
        if link_conversions:
            changes = link_language_conversions(changes)
        for status, old, new in changes:
            added, deleted = commit.numstat.get(new, (0, 0))
            if status == "X":  # delete + add that we link as a conversion (e.g. Foo.java -> Foo.kt)
                lineage_id = live.get(old, retired.get(old))
                if lineage_id is None:
                    lineage_id = new_lineage(old, index, None)
                move(lineage_id, old, new, index)
                added, _ = commit.numstat.get(new, (0, 0))
                _, deleted = commit.numstat.get(old, (0, 0))
                renames.append((commit.sha, old, new, "conversion"))
                if not is_ignored and added + deleted:
                    lineages[lineage_id].events.append((index, added, deleted))
            elif status in "AC":
                live[new] = new_lineage(new, index, index)
            elif status == "D":
                lineage_id = live.pop(old, None)
                if lineage_id is not None:
                    retired[old] = lineage_id
            elif status == "R":
                lineage_id = live.get(old, retired.get(old))
                if lineage_id is None:
                    lineage_id = new_lineage(old, index, None)
                move(lineage_id, old, new, index)
                renames.append((commit.sha, old, new, "rename"))
                if not is_ignored and added + deleted:
                    lineages[lineage_id].events.append((index, added, deleted))
            else:
                lineage_id = live.get(new, retired.get(new))
                if lineage_id is None:
                    lineage_id = new_lineage(new, index, None)
                    live[new] = lineage_id
                if not is_ignored:
                    lineages[lineage_id].events.append((index, added, deleted))
    return History(commits, lineages, dict(path_history), live, renames, mass_candidates, ignored)


def link_language_conversions(changes: list) -> list:
    """Links `D src/main/java/a/Foo.java` + `A src/main/kotlin/a/Foo.kt` in one commit as a conversion.

    Git's rename detection misses these because the content is rewritten. Only unambiguous pairs are linked:
    one deleted Java file and one added Kotlin file with the same name, preferring the same package path.
    """
    deleted = collections.defaultdict(list)
    added = collections.defaultdict(list)
    for position, (status, old, new) in enumerate(changes):
        if status == "D" and old.endswith(".java"):
            deleted[os.path.basename(old)[:-5]].append(position)
        elif status == "A" and new.endswith(".kt"):
            added[os.path.basename(new)[:-3]].append(position)
    linked: dict[int, tuple] = {}
    dropped: set[int] = set()
    for stem, deleted_positions in deleted.items():
        added_positions = added.get(stem, [])
        if not added_positions:
            continue
        if len(deleted_positions) == 1 and len(added_positions) == 1:
            pair = (deleted_positions[0], added_positions[0])
        else:
            pairs = [
                (d, a) for d in deleted_positions for a in added_positions
                if package_path(changes[d][1]) == package_path(changes[a][2])
            ]
            if len(pairs) != 1:
                continue
            pair = pairs[0]
        old, new = changes[pair[0]][1], changes[pair[1]][2]
        linked[pair[1]] = ("X", old, new)
        dropped.add(pair[0])
    return [linked.get(position, change) for position, change in enumerate(changes) if position not in dropped]


def package_path(path: str) -> str:
    return re.sub(r"^.*?src/(main|test|itest)/(java|kotlin)/", "", os.path.dirname(path))


def lineage_at(history: History, path: str, ancestors: set[int]) -> int | None:
    best = None
    for index, lineage_id in history.path_history.get(path, []):
        if index in ancestors:
            best = lineage_id
    return best


# ---------------------------------------------------------------------------------- classification


class Classifier:
    """Assigns every path to `production` or one of the exclusion groups, from the build setup."""

    def __init__(self, git: Git, commit: str, tree: dict[str, str]):
        self.git = git
        self.commit = commit
        self.generated_roots: set[str] = set()
        self.build_output_roots: set[str] = set()
        self.source_roots: set[str] = set()
        self.build_excluded_globs: set[str] = set()
        self.notes: list[str] = []
        self.attributes: dict[str, set[str]] = {}
        self._read_build_setup(tree)

    def text(self, path: str) -> str:
        return self.git.run("show", f"{self.commit}:{path}", check=False)

    def _read_build_setup(self, tree: dict[str, str]):
        for path in tree:
            name = os.path.basename(path)
            directory = os.path.dirname(path)
            prefix = f"{directory}/" if directory else ""
            if name in ("build.gradle.kts", "build.gradle"):
                self._read_gradle(prefix, self.text(path))
            elif name == "angular.json":
                self._read_angular(prefix, self.text(path))
            elif name == "package.json" and "/node_modules/" not in f"/{path}":
                self._read_package_json(prefix, self.text(path))
            elif name == "pom.xml":
                self.build_output_roots.add(f"{prefix}target/")
                self.source_roots.update({f"{prefix}src/main/java/", f"{prefix}src/main/kotlin/"})

    def _read_gradle(self, prefix: str, text: str):
        self.build_output_roots.add(f"{prefix}build/")
        self.source_roots.update({f"{prefix}src/main/kotlin/", f"{prefix}src/main/java/"})
        directories = {
            name: value for name, value in re.findall(r'val\s+(\w+)\s*=\s*layout\.projectDirectory\.dir\("([^"]+)"\)', text)
        }

        def resolve(expression: str) -> str | None:
            expression = expression.strip()
            literal = re.fullmatch(r'"(?:\$rootDir/|\$projectDir/)?([^"$]+)"', expression)
            if literal:
                return literal.group(1)
            nested = re.fullmatch(r'(\w+)\.dir\("([^"]+)"\)(?:\.asFile)?', expression)
            if nested and nested.group(1) in directories:
                return f"{directories[nested.group(1)]}/{nested.group(2)}"
            if expression in directories:
                return directories[expression]
            return None

        candidates = re.findall(r"srcDir\(([^()]*(?:\([^()]*\))?[^()]*)\)", text)
        candidates += re.findall(r"outputDir\.set\(([^()]*)\)", text)
        candidates += re.findall(r"targetDirectory\s*=\s*([^\n]+)", text)
        for name, value in directories.items():
            if "generated" in name.lower() or "generated" in value.lower():
                candidates.append(name)
        for expression in candidates:
            resolved = resolve(expression)
            if resolved and "generated" in resolved.lower():
                self.generated_roots.add(prefix + resolved.strip("/") + "/")

    def _read_angular(self, prefix: str, text: str):
        try:
            config = json.loads(text)
        except json.JSONDecodeError:
            return
        for project in config.get("projects", {}).values():
            root = project.get("sourceRoot") or project.get("root") or "src"
            self.source_roots.add(f"{prefix}{root.strip('/')}/")
            for target in project.get("architect", {}).values():
                ts_config = target.get("options", {}).get("tsConfig")
                if isinstance(ts_config, str) and target.get("builder", "").endswith(("application", "browser")):
                    self._read_ts_config_excludes(os.path.normpath(prefix + ts_config))
                output = target.get("options", {}).get("outputPath")
                if isinstance(output, dict):
                    output = output.get("base")
                if isinstance(output, str):
                    self.build_output_roots.add(f"{prefix}{os.path.normpath(output)}/")

    def _read_ts_config_excludes(self, path: str):
        """Files the production tsconfig excludes from the app build are test code (e.g. test helpers)."""
        text = re.sub(r"(?m)^\s*//.*$|/\*.*?\*/", "", self.text(path), flags=re.S)
        try:
            excludes = json.loads(re.sub(r",(\s*[}\]])", r"\1", text)).get("exclude", [])
        except json.JSONDecodeError:
            return
        directory = os.path.dirname(path)
        for pattern in excludes:
            self.build_excluded_globs.add(os.path.normpath(os.path.join(directory, pattern)))

    def _read_package_json(self, prefix: str, text: str):
        try:
            scripts = json.loads(text).get("scripts", {})
        except json.JSONDecodeError:
            return
        for command in scripts.values():
            for output in re.findall(r"(?:-o|--output)\s+(\S+)", command):
                path = os.path.normpath(os.path.join(prefix, output))
                if "generated" in path:
                    root = path if "/generated/" not in f"{path}/" else path[: path.index("generated") + len("generated")]
                    self.generated_roots.add(root.rstrip("/") + "/")

    def load_attributes(self, paths: set[str]):
        """Reads linguist-generated and linguist-vendored from .gitattributes, as git resolves them."""
        ordered = sorted(paths)
        output = self.git.run(
            "check-attr", f"--source={self.commit}", "-z", "--stdin", "linguist-generated", "linguist-vendored",
            stdin=("\0".join(ordered) + "\0").encode("utf-8", errors="surrogateescape"), binary=True,
        ).decode("utf-8", errors="surrogateescape").split("\0")
        for position in range(0, len(output) - 2, 3):
            path, attribute, value = output[position: position + 3]
            if value in ("set", "true"):
                self.attributes.setdefault(path, set()).add(attribute)

    def classify(self, path: str) -> str:
        """Returns `production`, an exclusion group, or `not code`."""
        name = os.path.basename(path)
        segments = path.split("/")[:-1]
        language = language_of(path)
        attributes = self.attributes.get(path, set())
        if any(path.startswith(root) for root in self.build_output_roots):
            return "build output"
        if "linguist-vendored" in attributes or "node_modules" in segments:
            return "vendored"
        if "linguist-generated" in attributes or any(path.startswith(root) for root in self.generated_roots):
            return "generated"
        if name in LOCKFILE_NAMES:
            return "lockfiles"
        if language is None:
            return "not code"
        if any(segment in FIXTURE_DIRECTORY_NAMES for segment in segments) or any(
            fnmatch.fnmatchcase(name, pattern) for pattern in FIXTURE_FILE_PATTERNS
        ):
            return "fixtures"
        if (
            any(segment in TEST_DIRECTORY_NAMES for segment in segments)
            or any(fnmatch.fnmatchcase(name, pattern) for pattern in TEST_FILE_PATTERNS)
            or any(fnmatch.fnmatchcase(path, pattern) for pattern in self.build_excluded_globs)
        ):
            return "tests"
        if path.endswith(".d.ts"):
            return "tooling & config"
        if self.source_roots and not any(path.startswith(root) for root in self.source_roots):
            return "tooling & config"
        if name.endswith(".kts") or re.search(r"\.config\.[cm]?[jt]s$|rc\.[cm]?js$", name):
            return "tooling & config"
        return "production"


def language_of(path: str) -> str | None:
    if "." not in os.path.basename(path):
        return None
    return LANGUAGE_BY_EXTENSION.get(path.rsplit(".", 1)[1].lower())


# ------------------------------------------------------------------------------ source preprocessing


def strip_comments_and_strings(text: str, language: str) -> str:
    """Blanks comments (and, for counting declarations, string contents) but keeps the line structure."""
    if language == "python":
        return re.sub(r"(?m)#.*$", "", text)
    output, position, length = [], 0, len(text)
    while position < length:
        two = text[position: position + 2]
        character = text[position]
        if two == "//":
            end = text.find("\n", position)
            end = length if end < 0 else end
            position = end
        elif two == "/*":
            end = text.find("*/", position + 2)
            end = length if end < 0 else end + 2
            output.append("\n" * text.count("\n", position, end))
            position = end
        elif text.startswith('"""', position):
            end = text.find('"""', position + 3)
            end = length if end < 0 else end + 3
            output.append('""' + "\n" * text.count("\n", position, end))
            position = end
        elif character in "\"'`":
            end = position + 1
            while end < length and text[end] != character:
                if text[end] == "\\":
                    end += 1
                if end >= length or (text[end] == "\n" and character != "`"):
                    break
                end += 1
            output.append(character * 2 + "\n" * text.count("\n", position, end + 1 if end < length else end))
            position = end + 1
        else:
            output.append(character)
            position += 1
    return "".join(output)


def strip_inline_tests(text: str, language: str) -> tuple[str, int]:
    """Blanks test code that lives inside production files, keeping line numbers. Returns (text, lines stripped)."""
    lines = text.split("\n")
    blanked: set[int] = set()

    def blank_block(start_line: int, open_offset: int):
        depth, line = 0, start_line
        offset = open_offset
        while line < len(lines):
            for character in lines[line][offset:]:
                if character == "{":
                    depth += 1
                elif character == "}":
                    depth -= 1
                    if depth == 0:
                        blanked.update(range(start_line, line + 1))
                        return
            line, offset = line + 1, 0

    if language == "rust":
        for number, line in enumerate(lines):
            if re.match(r"\s*#\[cfg\(test\)\]", line):
                for following in range(number, min(number + 3, len(lines))):
                    match = re.search(r"\bmod\s+\w+\s*\{", lines[following])
                    if match:
                        blanked.add(number)
                        blank_block(following, match.start())
                        break
    elif language in ESLINT_LANGUAGES:
        for number, line in enumerate(lines):
            match = re.search(r"if\s*\(\s*import\.meta\.vitest\s*\)\s*\{", line)
            if match:
                blank_block(number, match.start())
    elif language == "python":
        in_doctest = False
        for number, line in enumerate(lines):
            stripped = line.strip()
            if stripped.startswith(">>>") or (in_doctest and stripped.startswith("...")):
                blanked.add(number)
                in_doctest = True
            elif in_doctest and stripped and not stripped.startswith(('"""', "'''")):
                blanked.add(number)  # expected output of the example above
            else:
                in_doctest = False
    if not blanked:
        return text, 0
    counted = sum(1 for number in blanked if lines[number].strip())
    return "\n".join("" if number in blanked else line for number, line in enumerate(lines)), counted


def lines_of_code(text: str, language: str) -> int:
    return sum(1 for line in strip_comments_and_strings(text, language).split("\n") if line.strip())


def declared_kotlin_functions(stripped: str) -> int:
    return len(re.findall(r"\bfun\b(?!\s+interface)", stripped))


# ------------------------------------------------------------------------------ complexity tools


class ComplexityCache:
    def __init__(self, path: Path):
        path.parent.mkdir(parents=True, exist_ok=True)
        self.connection = sqlite3.connect(path)
        self.connection.execute("create table if not exists blobs (key text primary key, value text)")

    def get_many(self, keys: list[str]) -> dict[str, dict]:
        found = {}
        for start in range(0, len(keys), 500):
            chunk = keys[start: start + 500]
            query = f"select key, value from blobs where key in ({','.join('?' * len(chunk))})"
            for key, value in self.connection.execute(query, chunk):
                found[key] = json.loads(value)
        return found

    def put_many(self, items: dict[str, dict]):
        self.connection.executemany(
            "insert or replace into blobs values (?, ?)", [(key, json.dumps(value)) for key, value in items.items()]
        )
        self.connection.commit()


@dataclass
class Tools:
    detekt_jar: Path | None
    detekt_version: str | None
    node_modules: Path | None
    eslint_version: str | None
    lizard_version: str
    signatures: dict

    def tool_for(self, language: str) -> str:
        if language == "kotlin" and self.detekt_jar:
            return "detekt"
        if language in ESLINT_LANGUAGES and self.node_modules:
            return "eslint"
        return "lizard"

    def describe(self, language: str) -> str:
        tool = self.tool_for(language)
        if tool == "detekt":
            return f"detekt {self.detekt_version} CyclomaticComplexMethod"
        if tool == "eslint":
            return f"ESLint {self.eslint_version} complexity rule"
        return f"lizard {self.lizard_version} cyclomatic complexity"


DETEKT_CONFIG = """
config:
  validation: false
comments: {active: false}
coroutines: {active: false}
empty-blocks: {active: false}
exceptions: {active: false}
naming: {active: false}
performance: {active: false}
potential-bugs: {active: false}
style: {active: false}
complexity:
  active: true
  CyclomaticComplexMethod:
    active: true
    allowedComplexity: 0
    ignoreSingleWhenExpression: false
    ignoreSimpleWhenEntries: false
    ignoreNestingFunctions: true
    ignoreLocalFunctions: true
  CognitiveComplexMethod: {active: true, allowedComplexity: 0}
  LongMethod: {active: true, allowedLines: 0}
"""
for _rule in [
    "ComplexCondition", "ComplexInterface", "CyclomaticComplexClass", "LabeledExpression", "LargeClass",
    "LongParameterList", "MethodOverloading", "NamedArguments", "NestedBlockDepth", "NestedScopeFunctions",
    "ReplaceSafeCallChainWithRun", "StringLiteralDuplication", "TooManyFunctions",
]:
    DETEKT_CONFIG += f"  {_rule}: {{active: false}}\n"


def find_tools(repo: Path, git: Git, commit: str, cache_dir: Path, requested_node_modules: str | None) -> Tools:
    import lizard

    detekt_version = DEFAULT_DETEKT_VERSION
    catalog = git.run("show", f"{commit}:gradle/libs.versions.toml", check=False)
    match = re.search(r'^detekt\s*=\s*"([^"]+)"', catalog, re.M)
    if match:
        detekt_version = match.group(1)
    detekt_jar = None
    if shutil.which("java"):
        group = "dev/detekt" if detekt_version.startswith("2.") else "io/gitlab/arturbosch/detekt"
        detekt_jar = cache_dir / f"detekt-cli-{detekt_version}-all.jar"
        if not detekt_jar.exists():
            url = f"https://repo1.maven.org/maven2/{group}/detekt-cli/{detekt_version}/detekt-cli-{detekt_version}-all.jar"
            log(f"Downloading {url}")
            try:
                urllib.request.urlretrieve(url, detekt_jar)
            except OSError as error:
                log(f"  could not download detekt ({error}); Kotlin falls back to lizard")
                detekt_jar = None
    else:
        log("No `java` on the PATH; Kotlin falls back to lizard")

    node_modules = Path(requested_node_modules) if requested_node_modules else None
    if node_modules is None and shutil.which("node"):
        for candidate in sorted(repo.glob("**/node_modules/eslint/package.json"), key=lambda p: len(p.parts)):
            if len(candidate.relative_to(repo).parts) > 6:
                continue
            modules = candidate.parent.parent
            if (modules / "@typescript-eslint/parser").exists() and (modules / "typescript").exists():
                node_modules = modules
                break
    eslint_version = None
    if node_modules:
        eslint_version = json.loads((node_modules / "eslint/package.json").read_text())["version"]
    else:
        log("No node_modules with eslint, @typescript-eslint/parser and typescript; TS/JS fall back to lizard")
    signatures = {
        "detekt": f"detekt-{detekt_version}-{hashlib.sha1(DETEKT_CONFIG.encode()).hexdigest()[:8]}",
        "eslint": f"eslint-{eslint_version}-{hashlib.sha1((ASSET_DIR / 'ts_complexity.cjs').read_bytes()).hexdigest()[:8]}",
        "lizard": f"lizard-{lizard.version}",
    }
    return Tools(detekt_jar, detekt_version, node_modules, eslint_version, lizard.version, signatures)


def cache_key(tools: Tools, language: str, blob: str) -> str:
    return f"{CACHE_VERSION}:{tools.signatures[tools.tool_for(language)]}:{language}:{blob}"


def measure_blobs(git: Git, tools: Tools, cache: ComplexityCache, wanted: dict[str, tuple[str, str]]) -> dict[str, dict]:
    """wanted: blob -> (language, extension). Returns blob -> measurement, measuring only what is not cached."""
    keys = {blob: cache_key(tools, language, blob) for blob, (language, _) in wanted.items()}
    cached = cache.get_many(list(keys.values()))
    results = {blob: cached[key] for blob, key in keys.items() if key in cached}
    missing = [blob for blob in wanted if blob not in results]
    log(f"Complexity: {len(results)} blobs cached, {len(missing)} to measure")
    if not missing:
        return results
    with tempfile.TemporaryDirectory(prefix="churn-complexity-") as temporary:
        base = Path(temporary)
        by_tool: dict[str, list[str]] = collections.defaultdict(list)
        prepared: dict[str, dict] = {}
        for blob, content in git.blobs(missing):
            language, extension = wanted[blob]
            text = content.decode("utf-8", errors="replace")
            text, stripped = strip_inline_tests(text, language)
            tool = tools.tool_for(language)
            directory = base / tool
            directory.mkdir(exist_ok=True)
            (directory / f"{blob}.{extension}").write_text(text)
            uncommented = strip_comments_and_strings(text, language)
            prepared[blob] = {
                "loc": sum(1 for line in uncommented.split("\n") if line.strip()),
                "stripped": stripped,
                "declared": declared_kotlin_functions(uncommented) if language == "kotlin" else None,
            }
            by_tool[tool].append(blob)
        measured: dict[str, dict] = {}
        if by_tool.get("detekt"):
            measured.update(run_detekt(tools, base / "detekt"))
        if by_tool.get("eslint"):
            measured.update(run_eslint(tools, base / "eslint", by_tool["eslint"], wanted))
        if by_tool.get("lizard"):
            measured.update(run_lizard(base / "lizard", by_tool["lizard"], wanted))
        new_items = {}
        for blob in missing:
            entry = prepared[blob]
            tool_result = measured.get(blob, {"functions": [], "error": None})
            entry["functions"] = tool_result["functions"]
            entry["error"] = tool_result.get("error")
            if tool_result.get("declared") is not None:
                entry["declared"] = tool_result["declared"]
            results[blob] = entry
            new_items[keys[blob]] = entry
        cache.put_many(new_items)
    return results


def run_detekt(tools: Tools, directory: Path) -> dict[str, dict]:
    config = directory.parent / "detekt.yml"
    config.write_text(DETEKT_CONFIG)
    report = directory.parent / "detekt.xml"
    log(f"Running detekt on {len(list(directory.iterdir()))} Kotlin blobs")
    completed = subprocess.run(
        ["java", "-jar", str(tools.detekt_jar), "--config", str(config), "--input", str(directory), "--parallel",
         "--base-path", str(directory), "--report", f"checkstyle:{report}"],
        capture_output=True, text=True,
    )
    if not report.exists():
        raise RuntimeError(f"detekt produced no report: {completed.stdout[-2000:]} {completed.stderr[-2000:]}")
    functions: dict[str, dict[tuple, dict]] = collections.defaultdict(dict)
    for file_element in ElementTree.parse(report).getroot():
        blob = Path(file_element.get("name")).name.split(".")[0]
        for error in file_element:
            source, message = error.get("source"), error.get("message")
            location = (int(error.get("line")), int(error.get("column")))
            entry = functions[blob].setdefault(location, {"line": location[0]})
            if source == "detekt.CyclomaticComplexMethod":
                match = re.search(r"The function (.+?) appears .*?\(complexity: (\d+)\)", message)
                entry["name"], entry["cc"] = match.group(1), int(match.group(2))
            elif source == "detekt.CognitiveComplexMethod":
                entry["cognitive"] = int(re.search(r"\(complexity: (\d+)\)", message).group(1))
            elif source == "detekt.LongMethod":
                match = re.search(r"The function (.+?) is too long \((\d+)\)", message)
                entry["name"], entry["length"] = match.group(1), int(match.group(2))
    results = {}
    for file in directory.iterdir():
        blob = file.name.split(".")[0]
        entries = [entry for entry in functions.get(blob, {}).values() if "cc" in entry]
        results[blob] = {
            "functions": sorted(
                ({"name": e["name"], "line": e["line"], "cc": e["cc"], "length": e.get("length"),
                  "cognitive": e.get("cognitive", 0)} for e in entries), key=lambda e: e["line"]),
        }
    return results


def run_eslint(tools: Tools, directory: Path, blobs: list[str], wanted: dict) -> dict[str, dict]:
    files = [str(directory / f"{blob}.{wanted[blob][1]}") for blob in blobs]
    file_list = directory.parent / "eslint-files.txt"
    file_list.write_text("\n".join(files))
    output = directory.parent / "eslint.json"
    log(f"Running ESLint on {len(files)} TS/JS blobs")
    completed = subprocess.run(
        ["node", str(ASSET_DIR / "ts_complexity.cjs"), str(tools.node_modules), str(file_list), str(output),
         str(directory)], capture_output=True, text=True,
    )
    if completed.returncode != 0:
        raise RuntimeError(f"ESLint run failed: {completed.stderr[-3000:]}")
    results = {}
    for path, result in json.loads(output.read_text()).items():
        blob = Path(path).name.split(".")[0]
        results[blob] = {
            "declared": result["declared"],
            "error": "; ".join(result["fatal"]) or None,
            "functions": [
                {"name": f["name"], "line": f["line"], "cc": f["complexity"],
                 "length": (f["end"] - f["start"] + 1) if f.get("start") else None,
                 "kind": f.get("origin")}
                for f in result["functions"]
                # class field initializers are implicit functions to ESLint; without a branch they add nothing
                if f.get("origin") == "function" or f["complexity"] > 1
            ],
        }
    return results


def lizard_one(arguments: tuple[str, str]) -> tuple[str, dict]:
    import lizard

    path, language = arguments
    blob = Path(path).name.split(".")[0]
    text = Path(path).read_text()
    analysis = lizard.analyze_file.analyze_source_code(path, text)
    declared = None
    if language == "java":
        try:
            import javalang

            tree = javalang.parse.parse(text)
            declared = sum(
                1 for _, node in tree
                if isinstance(node, (javalang.tree.MethodDeclaration, javalang.tree.ConstructorDeclaration))
                and node.body is not None
            )
        except Exception:  # javalang only knows Java 8 syntax
            declared = None
    return blob, {
        "declared": declared,
        "functions": [
            {"name": f.name.split("::")[-1], "line": f.start_line, "cc": f.cyclomatic_complexity,
             "length": f.end_line - f.start_line + 1}
            for f in analysis.function_list
        ],
    }


def run_lizard(directory: Path, blobs: list[str], wanted: dict) -> dict[str, dict]:
    log(f"Running lizard on {len(blobs)} blobs")
    arguments = [(str(directory / f"{blob}.{wanted[blob][1]}"), wanted[blob][0]) for blob in blobs]
    with multiprocessing.Pool() as pool:
        return dict(pool.map(lizard_one, arguments, chunksize=20))


# ------------------------------------------------------------------------------------- the metrics


def iso_date(timestamp: int) -> str:
    return dt.datetime.fromtimestamp(timestamp, dt.timezone.utc).strftime("%Y-%m-%d")


def month_of(timestamp: int) -> str:
    return dt.datetime.fromtimestamp(timestamp, dt.timezone.utc).strftime("%Y-%m")


def select_frames(git: Git, ref_commit: str, mode: str, limit: int | None) -> list[dict]:
    head_time = int(git.run("show", "-s", "--format=%ct", ref_commit).strip())
    frames = []
    if mode == "monthly":
        latest_per_month: dict[str, tuple[str, int]] = {}
        for line in git.run("log", "--first-parent", "--format=%H %ct", ref_commit).splitlines():
            sha, time = line.split()
            month = month_of(int(time))
            if month not in latest_per_month:
                latest_per_month[month] = (sha, int(time))
        for month in sorted(latest_per_month):
            sha, time = latest_per_month[month]
            if sha != ref_commit:
                frames.append({"sha": sha, "time": time, "label": month})
    elif mode == "tags":
        for line in git.run("tag", "--merged", ref_commit, "--sort=creatordate",
                            "--format=%(refname:short) %(objectname) %(*objectname)").splitlines():
            name, sha, peeled = (line.split() + [""])[:3]
            sha = peeled or sha
            if sha != ref_commit:
                frames.append({"sha": sha, "time": int(git.run("show", "-s", "--format=%ct", sha).strip()),
                               "label": name})
    if limit:
        frames = frames[-limit:]
    frames.append({"sha": ref_commit, "time": head_time, "label": "now"})
    return frames


def frame_rows(git: Git, history: History, classifier: Classifier, frame: dict, sha_to_index: dict) -> list[dict]:
    """The files of one frame: identity, path, group, language and churn in both windows."""
    ancestors = {
        sha_to_index[sha] for sha in git.run("rev-list", frame["sha"]).split() if sha in sha_to_index
    }
    year_ago = frame["time"] - SECONDS_PER_YEAR
    rows = []
    for path, blob in git.tree(frame["sha"]).items():
        group = classifier.classify(path)
        if group == "not code" or group == "lockfiles":
            rows.append({"path": path, "group": group, "blob": None})
            continue
        lineage_id = lineage_at(history, path, ancestors)
        commits = lines = commits_year = lines_year = 0
        if lineage_id is not None:
            for index, added, deleted in history.lineages[lineage_id].events:
                if index not in ancestors:
                    continue
                commits += 1
                lines += added + deleted
                if history.commits[index].time > year_ago:
                    commits_year += 1
                    lines_year += added + deleted
        rows.append({
            "path": path, "blob": blob, "group": group, "language": language_of(path), "lineage": lineage_id,
            "commits": commits, "lines": lines, "commits_year": commits_year, "lines_year": lines_year,
        })
    return rows


def quadrant_of(churn: float, complexity: float, churn_threshold: float, complexity_threshold: float) -> str:
    high_churn, high_complexity = churn > churn_threshold, complexity > complexity_threshold
    if high_churn and high_complexity:
        return "Danger Zone"
    if high_complexity:
        return "Cowboy Code"
    if high_churn:
        return "Fertile Ground"
    return "Healthy Closure"


def classify_view(rows: list[dict], churn_key: str, complexity_key: str) -> dict:
    """Same rules as the page: thresholds at the medians, score over the files in view."""
    if not rows:
        return {"threshold": (0, 0), "quadrants": {}, "scores": {}}
    churn_threshold = statistics.median(row[churn_key] for row in rows)
    complexity_threshold = statistics.median(row[complexity_key] for row in rows)
    max_churn = max(row[churn_key] for row in rows)
    max_complexity = max(row[complexity_key] for row in rows)
    quadrants, scores = {}, {}
    for row in rows:
        quadrants[row["path"]] = quadrant_of(row[churn_key], row[complexity_key], churn_threshold, complexity_threshold)
        u = math.log1p(row[churn_key]) / math.log1p(max_churn) if max_churn else 0
        v = math.log1p(row[complexity_key]) / math.log1p(max_complexity) if max_complexity else 0
        scores[row["path"]] = math.hypot(u, v)
    return {"threshold": (churn_threshold, complexity_threshold), "quadrants": quadrants, "scores": scores}


def attach_complexity(rows: list[dict], measurements: dict[str, dict]):
    for row in rows:
        if not row.get("blob") or row["blob"] not in measurements:
            continue
        measurement = measurements[row["blob"]]
        scores = [function["cc"] for function in measurement["functions"]]
        row["total"] = sum(scores)
        row["worst"] = max(scores, default=0)
        row["loc"] = measurement["loc"]
        row["stripped"] = measurement["stripped"]


# -------------------------------------------------------------------------------- checks & reports


def tool_check(rows: list[dict], measurements: dict, tools: Tools) -> dict:
    """Functions the tool reports vs the functions the language declares, and the three longest functions."""
    report = {}
    by_language = collections.defaultdict(list)
    for row in rows:
        if row.get("blob") in measurements and row["group"] == "production":
            by_language[row["language"]].append(row)
    for language, language_rows in sorted(by_language.items()):
        compared = mismatched = reported_total = declared_total = errors = 0
        examples, all_functions = [], []
        for row in language_rows:
            measurement = measurements[row["blob"]]
            if measurement.get("error"):
                errors += 1
            functions = measurement["functions"]
            named = [f for f in functions if f.get("kind", "function") == "function"]
            all_functions += [(f.get("length") or 0, f["cc"], f["name"], row["path"], f["line"]) for f in functions]
            declared = measurement.get("declared")
            if declared is None:
                continue
            compared += 1
            reported_total += len(named)
            declared_total += declared
            if len(named) != declared:
                mismatched += 1
                if len(examples) < 5:
                    examples.append({"path": row["path"], "declared": declared, "reported": len(named)})
        longest = sorted(all_functions, reverse=True)[:3]
        report[language] = {
            "tool": tools.describe(language), "files": len(language_rows), "files_compared": compared,
            "files_mismatched": mismatched, "functions_reported": reported_total, "functions_declared": declared_total,
            "parse_errors": errors, "mismatch_examples": examples,
            "longest": [{"length": l, "cc": c, "name": n, "path": p, "line": line} for l, c, n, p, line in longest],
            "passed": errors == 0 and (compared == 0 or mismatched / compared <= 0.02),
        }
    return report


def spot_check(git: Git, ref_commit: str, rows: list[dict], history: History, tools: Tools,
               measurements: dict, picks: list[str]) -> list[dict]:
    """Churn against `git log --follow --numstat`, complexity against the tool run on that one blob alone."""
    by_path = {row["path"]: row for row in rows}
    results = []
    for path in picks:
        row = by_path[path]
        output = git.run("log", ref_commit, "--follow", "--no-merges", "-M", "--numstat", "-z",
                         "--format=%x1e%H%x1f%s", "--", path, binary=True).decode("utf-8", errors="surrogateescape")
        follow_commits = []
        follow_oldest_path = path
        for record in output.split("\x1e")[1:]:
            header, _, rest = record.partition("\0")
            sha = header.split("\x1f")[0]
            tokens = [token.lstrip("\n") for token in rest.split("\0")]
            added = deleted = 0
            for position, token in enumerate(tokens):
                if re.match(r"^(\d+|-)\t(\d+|-)\t", token):
                    a, d, numstat_path = token.split("\t")[:3]
                    follow_oldest_path = numstat_path or tokens[position + 1]
                    added += int(a) if a != "-" else 0
                    deleted += int(d) if d != "-" else 0
            follow_commits.append((sha, added, deleted))
        creation = follow_commits[-1] if follow_commits else None
        kept = [c for c in follow_commits[:-1] if c[0] not in history.ignored and (c[1] or c[2])]
        follow_lines = sum(a + d for _, a, d in kept)
        # complexity: rerun the tool on this one blob, bypassing the cache
        language = row["language"]
        extension = path.rsplit(".", 1)[1]
        with tempfile.TemporaryDirectory() as temporary:
            base = Path(temporary)
            directory = base / tools.tool_for(language)
            directory.mkdir()
            text = git.run("cat-file", "-p", row["blob"])
            text, _ = strip_inline_tests(text, language)
            (directory / f"{row['blob']}.{extension}").write_text(text)
            tool = tools.tool_for(language)
            if tool == "detekt":
                single = run_detekt(tools, directory)
            elif tool == "eslint":
                single = run_eslint(tools, directory, [row["blob"]], {row["blob"]: (language, extension)})
            else:
                single = run_lizard(directory, [row["blob"]], {row["blob"]: (language, extension)})
        scores = [f["cc"] for f in single[row["blob"]]["functions"]]
        results.append({
            "path": path,
            "churn": {"script_commits": row["commits"], "script_lines": row["lines"],
                      "follow_commits": len(kept), "follow_lines": follow_lines,
                      "follow_creation": creation[0][:10] if creation else None,
                      "follow_oldest_path": follow_oldest_path,
                      "lineage_oldest_path": history.lineages[row["lineage"]].paths[0][1],
                      "match": row["commits"] == len(kept) and row["lines"] == follow_lines},
            "complexity": {"script_total": row["total"], "script_worst": row["worst"],
                           "single_total": sum(scores), "single_worst": max(scores, default=0),
                           "match": row["total"] == sum(scores) and row["worst"] == max(scores, default=0)},
        })
    return results


def details_for(row: dict, history: History, measurements: dict) -> dict:
    lineage = history.lineages[row["lineage"]] if row.get("lineage") is not None else None
    monthly = collections.Counter()
    monthly_lines = collections.Counter()
    authors = collections.Counter()
    largest = []
    last_change = None
    if lineage:
        for index, added, deleted in lineage.events:
            commit = history.commits[index]
            month = month_of(commit.time)
            monthly[month] += 1
            monthly_lines[month] += added + deleted
            authors[commit.author] += 1
            largest.append((added + deleted, commit.sha, iso_date(commit.time), commit.subject[:100]))
            last_change = max(last_change or 0, commit.time)
    largest.sort(reverse=True)
    functions = sorted(measurements[row["blob"]]["functions"], key=lambda f: (-f["cc"], f["line"]))
    return {
        "functions": [[f["name"], f["line"], f["cc"], f.get("length"), f.get("cognitive")] for f in functions],
        "monthly": [[month, monthly[month], monthly_lines[month]] for month in sorted(monthly)],
        "authors": authors.most_common(),
        "largest": [[sha, date, lines, subject] for lines, sha, date, subject in largest[:5]],
        "born": iso_date(history.commits[lineage.born].time) if lineage and lineage.born is not None else None,
        "paths": [path for _, path in lineage.paths] if lineage else [row["path"]],
        "last_change": iso_date(last_change) if last_change else None,
        "authors_year": None,
    }


def forge_url(git: Git) -> str | None:
    remote = git.run("remote", "get-url", "origin", check=False).strip()
    match = re.match(r"^(?:git@|ssh://git@|https://)([^/:]+)[:/](.+?)(?:\.git)?$", remote)
    return f"https://{match.group(1)}/{match.group(2)}" if match else None


def colour_keys(production_paths: list[str]) -> tuple[int, list[str]]:
    """Top-level directory: the first directory level at which the production files split up."""
    depth = 0
    while True:
        prefixes = {"/".join(path.split("/")[: depth + 1]) for path in production_paths if path.count("/") > depth}
        if len(prefixes) != 1:
            break
        depth += 1
    counts = collections.Counter("/".join(path.split("/")[: depth + 1]) for path in production_paths)
    return depth + 1, [key for key, _ in counts.most_common()]


# ------------------------------------------------------------------------------------------- main


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--repo", default=".", help="repository (default: the current directory's repository)")
    parser.add_argument("--ref", help="branch or commit to analyse (default: the remote's default branch)")
    parser.add_argument("--subdir", help="only keep files that resolve into this directory at the analysed commit")
    parser.add_argument("--out", help="output directory (default: <repo>/build/churn-complexity)")
    parser.add_argument("--cache-dir", default=str(Path.home() / ".cache" / "churn-complexity"))
    parser.add_argument("--frames", choices=["monthly", "tags", "none"], default="monthly",
                        help="snapshots for the time slider (default: one per month)")
    parser.add_argument("--max-frames", type=int, help="keep only the most recent N frames")
    parser.add_argument("--ignore-rev", action="append", default=[], help="commit whose churn to ignore (repeatable)")
    parser.add_argument("--ignore-revs-file", help="default: .git-blame-ignore-revs when it exists")
    parser.add_argument("--mass-threshold", type=float, default=0.25,
                        help="list commits touching more than this share of the files (default: 0.25)")
    parser.add_argument("--no-link-conversions", action="store_true",
                        help="do not link a deleted Foo.java to an added Foo.kt in the same commit")
    parser.add_argument("--node-modules", help="node_modules with eslint, @typescript-eslint/parser and typescript")
    parser.add_argument("--spot-checks", type=int, default=5)
    arguments = parser.parse_args()

    repo = Path(subprocess.run(["git", "rev-parse", "--show-toplevel"], cwd=arguments.repo, capture_output=True,
                               text=True, check=True).stdout.strip())
    git = Git(repo)
    out = Path(arguments.out) if arguments.out else repo / "build" / "churn-complexity"
    out.mkdir(parents=True, exist_ok=True)
    cache_dir = Path(arguments.cache_dir)
    cache_dir.mkdir(parents=True, exist_ok=True)

    ref = arguments.ref
    if not ref:
        default_head = git.run("symbolic-ref", "-q", "refs/remotes/origin/HEAD", check=False).strip()
        candidates = [default_head.removeprefix("refs/remotes/")] if default_head else []
        candidates += ["origin/main", "origin/master", "main", "master"]
        ref = next(c for c in candidates if git.run("rev-parse", "-q", "--verify", f"{c}^{{commit}}", check=False).strip())
    ref_commit = git.run("rev-parse", f"{ref}^{{commit}}").strip()
    log(f"Analysing {ref} at {ref_commit}")

    shallow = git.run("rev-parse", "--is-shallow-repository").strip() == "true"
    roots = git.run("rev-list", "--max-parents=0", ref_commit).split()
    if shallow:
        shallow_file = Path(git.run("rev-parse", "--git-path", "shallow").strip())
        if not shallow_file.is_absolute():
            shallow_file = repo / shallow_file
        roots += shallow_file.read_text().split() if shallow_file.exists() else []
    first_time = min(int(git.run("show", "-s", "--format=%ct", root).strip()) for root in roots)

    ignored: dict[str, str] = {}
    ignore_file = Path(arguments.ignore_revs_file) if arguments.ignore_revs_file else repo / ".git-blame-ignore-revs"
    ignore_source = None
    if ignore_file.exists():
        ignore_source = str(ignore_file.relative_to(repo) if ignore_file.is_relative_to(repo) else ignore_file)
        for line in ignore_file.read_text().splitlines():
            line = line.split("#", 1)[0].strip()
            if line:
                ignored[git.run("rev-parse", line).strip()] = ignore_source
    for revision in arguments.ignore_rev:
        ignored[git.run("rev-parse", f"{revision}^{{commit}}").strip()] = "--ignore-rev"

    log("Reading history")
    commits = parse_log(git, ref_commit)
    history = replay(commits, set(ignored), arguments.mass_threshold, not arguments.no_link_conversions)
    sha_to_index = {commit.sha: commit.index for commit in commits}
    log(f"  {len(commits)} non-merge commits, {len(history.lineages)} file lineages")

    head_tree = git.tree(ref_commit)
    classifier = Classifier(git, ref_commit, head_tree)
    frames = select_frames(git, ref_commit, arguments.frames, arguments.max_frames)
    log(f"Collecting {len(frames)} frames")
    frame_data = []
    all_paths: set[str] = set()
    trees = {}
    for frame in frames:
        trees[frame["sha"]] = git.tree(frame["sha"])
        all_paths.update(trees[frame["sha"]])
    classifier.load_attributes(all_paths)
    for frame in frames:
        frame_data.append(frame_rows(git, history, classifier, frame, sha_to_index))

    if arguments.subdir:
        prefix = arguments.subdir.strip("/") + "/"
        keep = {row["lineage"] for row in frame_data[-1] if row["path"].startswith(prefix) and row.get("lineage") is not None}
        frame_data = [[row for row in rows if row["path"].startswith(prefix) or row.get("lineage") in keep]
                      for rows in frame_data]

    tools = find_tools(repo, git, ref_commit, cache_dir, arguments.node_modules)
    wanted = {}
    for rows in frame_data:
        for row in rows:
            if row.get("blob") and row["language"]:
                wanted[row["blob"]] = (row["language"], row["path"].rsplit(".", 1)[1])
    measurements = measure_blobs(git, tools, ComplexityCache(cache_dir / "complexity.sqlite"), wanted)
    for rows in frame_data:
        attach_complexity(rows, measurements)

    head_rows = frame_data[-1]
    head_code = [row for row in head_rows if row.get("blob")]
    production = [row for row in head_code if row["group"] == "production"]

    # ---- consistency checks
    ghosts = sorted(set(history.live) - set(head_tree))
    missing = sorted(set(head_tree) - set(history.live))
    stale_paths = [row["path"] for row in head_rows if row["path"] not in head_tree]
    checks = {
        "tools": tool_check(head_rows, measurements, tools),
        "excluded_points_in_default_view": [row["path"] for row in production if row["group"] != "production"],
        "paths_not_in_commit": stale_paths,
        "replay_paths_not_in_commit": ghosts[:20], "replay_paths_not_in_commit_count": len(ghosts),
        "commit_paths_not_replayed": missing[:20], "commit_paths_not_replayed_count": len(missing),
    }

    # ---- Danger Zone now and a year ago, default view (production, total complexity, commits)
    def danger(rows, churn_key):
        view = [row for row in rows if row.get("group") == "production" and "total" in row]
        classified = classify_view(view, churn_key, "total")
        ranked = sorted((path for path, q in classified["quadrants"].items() if q == "Danger Zone"),
                        key=lambda path: -classified["scores"][path])
        return classified, ranked

    head_classified, head_danger = danger(head_rows, "commits")
    movement = {}
    year_frame_index = None
    for position, frame in enumerate(frames[:-1]):
        if frame["time"] <= frames[-1]["time"] - SECONDS_PER_YEAR:
            year_frame_index = position
    if year_frame_index is not None:
        for window, key in (("all history", "commits"), ("last 12 months", "commits_year")):
            movement[window] = danger_movement(
                head_rows, frame_data[year_frame_index], frames[year_frame_index]["label"], key, danger
            )

    # ---- spot checks: two Danger Zone files, the most renamed file, a TypeScript file, a median file
    picks: list[str] = head_danger[:2]
    renamed = sorted((row for row in production if row.get("lineage") is not None),
                     key=lambda row: -len(history.lineages[row["lineage"]].paths))
    picks += [row["path"] for row in renamed if row["path"] not in picks][:1]
    typescript = sorted((row for row in production if row["language"] == "typescript"), key=lambda row: -row["commits"])
    picks += [row["path"] for row in typescript if row["path"] not in picks][:1]
    by_churn = sorted(production, key=lambda row: row["commits"])
    picks += [row["path"] for row in by_churn[len(by_churn) // 2:] if row["path"] not in picks][:1]
    log("Spot checks")
    spot_checks = spot_check(git, ref_commit, head_rows, history, tools, measurements, picks[: arguments.spot_checks])

    # ---- existing hotspot tools
    hotspot_tools = [path for path in head_tree if os.path.basename(path) in HOTSPOT_TOOL_MARKERS]
    gemfile = head_tree.get("Gemfile")
    if gemfile and "turbulence" in git.run("cat-file", "-p", gemfile):
        hotspot_tools.append("Gemfile (turbulence)")

    # ---- page data
    colour_depth, colour_order = colour_keys([row["path"] for row in production])
    historic = collections.Counter(
        "/".join(row["path"].split("/")[:colour_depth])
        for rows in frame_data for row in rows if row.get("group") == "production"
    )
    colour_order += [key for key, _ in historic.most_common() if key not in colour_order]
    lineage_ids: dict = {}
    files, paths = [], []
    path_index: dict[str, int] = {}

    def file_id(row) -> int:
        key = row["lineage"] if row.get("lineage") is not None else f"path:{row['path']}"
        if key not in lineage_ids:
            lineage_ids[key] = len(files)
            files.append({"head": None})
        return lineage_ids[key]

    def path_id(path: str) -> int:
        if path not in path_index:
            path_index[path] = len(paths)
            paths.append(path)
        return path_index[path]

    group_names = ["production"] + EXCLUSION_GROUPS
    language_names = sorted({row["language"] for rows in frame_data for row in rows if row.get("language")})
    page_frames = []
    for frame, rows in zip(frames, frame_data, strict=True):
        packed = []
        for row in rows:
            if not row.get("blob") or "total" not in row:
                continue
            packed.append([
                file_id(row), path_id(row["path"]), group_names.index(row["group"]),
                language_names.index(row["language"]), row["commits"], row["lines"], row["commits_year"],
                row["lines_year"], row["total"], row["worst"], row["loc"],
            ])
        page_frames.append({"sha": frame["sha"], "date": iso_date(frame["time"]), "label": frame["label"],
                            "rows": packed})
    details = {}
    for row in head_code:
        if "total" not in row:
            continue
        identifier = file_id(row)
        files[identifier]["head"] = row["path"]
        detail = details_for(row, history, measurements)
        year_ago = frames[-1]["time"] - SECONDS_PER_YEAR
        lineage = history.lineages[row["lineage"]] if row.get("lineage") is not None else None
        detail["authors_year"] = len({history.commits[i].author for i, _, _ in (lineage.events if lineage else [])
                                      if history.commits[i].time > year_ago})
        detail["stripped"] = row.get("stripped", 0)
        details[identifier] = detail

    group_counts = collections.Counter(row["group"] for row in head_rows)
    stripped_total = sum(row.get("stripped", 0) for row in head_code)
    renamed_production = [r for r in history.renames if r[2] in {row["path"] for row in production}]
    meta = {
        "repository": forge_url(git) or str(repo),
        "forge": forge_url(git),
        "ref": ref, "commit": ref_commit,
        "analysis_date": dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%d %H:%M UTC"),
        "history": {"from": iso_date(first_time), "to": iso_date(frames[-1]["time"]), "shallow": shallow,
                    "commits": len(commits), "merges_skipped": True,
                    "description": f"{ref} at {ref_commit[:10]}, non-merge commits reachable from it"},
        "subdir": arguments.subdir,
        "groups": {name: group_counts.get(name, 0) for name in group_names + ["not code"]},
        "group_names": group_names, "language_names": language_names,
        "stripped_inline_test_lines": stripped_total,
        "tools": {language: tools.describe(language) for language in language_names},
        "metric": "McCabe cyclomatic complexity per function, as each tool defines it",
        "colour_depth": colour_depth, "colour_order": colour_order,
        "ignored": [{"sha": sha, "source": source, "subject": commits[sha_to_index[sha]].subject if sha in sha_to_index else ""}
                    for sha, source in ignored.items()],
        "ignore_source": ignore_source,
        "mass_candidates": history.mass_candidates,
        "renames": {"total": len(history.renames),
                    "git": sum(1 for r in history.renames if r[3] == "rename"),
                    "conversions": sum(1 for r in history.renames if r[3] == "conversion"),
                    "production_files_with_renames": len({r[2] for r in renamed_production})},
        "checks": checks, "spot_checks": spot_checks, "movement": movement,
        "hotspot_tools": hotspot_tools,
        "danger_top": [{"path": path, "score": round(head_classified["scores"][path], 3)} for path in head_danger[:10]],
        "thresholds": head_classified["threshold"],
        "generated_roots": sorted(classifier.generated_roots), "build_output_roots": sorted(classifier.build_output_roots),
        "source_roots": sorted(classifier.source_roots),
    }
    data = {"meta": meta, "files": files, "paths": paths, "frames": page_frames, "details": details}
    payload = json.dumps(data, separators=(",", ":"))
    (out / "data.json").write_text(payload)
    template = (ASSET_DIR / "report.html").read_text()
    (out / "index.html").write_text(template.replace("/*__DATA__*/null", payload.replace("</", "<\\/")))
    write_summary(out / "summary.md", meta, head_rows, head_classified, production)
    log(f"Wrote {out / 'index.html'} ({len(payload) // 1024} KiB of data)")


def lineage_of(rows: list[dict], path: str):
    return next((row.get("lineage") for row in rows if row["path"] == path), None)


def danger_movement(head_rows: list[dict], then_rows: list[dict], since: str, churn_key: str, danger) -> dict:
    """Files that entered or left the Danger Zone between a snapshot and the analysed commit, by lineage."""
    classified_now, now = danger(head_rows, churn_key)
    classified_then, then = danger(then_rows, churn_key)
    lineage_now = {row["lineage"]: row["path"] for row in head_rows if row["path"] in set(now)}
    lineage_then = {row["lineage"]: row["path"] for row in then_rows if row["path"] in set(then)}
    head_by_lineage = {row["lineage"]: row["path"] for row in head_rows if row.get("lineage") is not None}
    then_by_lineage = {row["lineage"]: row["path"] for row in then_rows if row.get("lineage") is not None}
    entered = sorted((path for lineage, path in lineage_now.items() if lineage not in lineage_then),
                     key=lambda path: -classified_now["scores"][path])
    left = sorted(((lineage, path) for lineage, path in lineage_then.items() if lineage not in lineage_now),
                  key=lambda item: -classified_then["scores"][item[1]])

    def quadrant_before(path: str) -> str:
        path_then = then_by_lineage.get(lineage_of(head_rows, path))
        return classified_then["quadrants"].get(path_then, "out of view") if path_then else "new file"

    def now_or_deleted(lineage, path: str) -> str:
        if lineage not in head_by_lineage:
            return f"{path} (deleted)"
        head_path = head_by_lineage[lineage]
        return f"{head_path} (now {classified_now['quadrants'].get(head_path, 'out of view')})"

    return {
        "since": since,
        "entered": [f"{path} (was {quadrant_before(path)})" for path in entered],
        "left": [now_or_deleted(lineage, path) for lineage, path in left],
    }


def write_summary(path: Path, meta: dict, head_rows: list[dict], classified: dict, production: list[dict]):
    by_path = {row["path"]: row for row in head_rows}
    lines = [
        f"# Churn vs complexity — {meta['ref']} @ {meta['commit'][:10]}",
        "",
        f"Analysed {meta['analysis_date']}. History {meta['history']['from']} → {meta['history']['to']}, "
        f"{meta['history']['commits']} non-merge commits{' (SHALLOW clone)' if meta['history']['shallow'] else ''}.",
        "",
        "## Files",
        "",
        *[f"- {name}: {count}" for name, count in meta["groups"].items()],
        f"- inline test lines stripped from production files: {meta['stripped_inline_test_lines']}",
        "",
        "## Tools",
        "",
        *[f"- {language}: {tool}" for language, tool in meta["tools"].items()],
        "",
        "## Tool check (production files at the analysed commit)",
        "",
    ]
    for language, check in meta["checks"]["tools"].items():
        lines.append(
            f"- {language}: {check['files_compared']} of {check['files']} files compared, {check['files_mismatched']} "
            f"with a different function count ({check['functions_reported']} reported / {check['functions_declared']} "
            f"declared), {check['parse_errors']} parse errors — {'PASS' if check['passed'] else 'CHECK'}")
        for longest in check["longest"]:
            lines.append(f"  - longest: {longest['name']} ({longest['length']} lines) cc={longest['cc']} {longest['path']}:{longest['line']}")
    lines += ["", f"## Danger Zone top ten (thresholds: commits > {classified['threshold'][0]}, total complexity > {classified['threshold'][1]})", ""]
    for entry in meta["danger_top"]:
        row = by_path[entry["path"]]
        lines.append(f"- {entry['path']} — score {entry['score']}, {row['commits']} commits, {row['lines']} lines, "
                     f"total {row['total']}, worst {row['worst']}, {row['loc']} loc")
    lines += ["", "## Movement over the last year", ""]
    for window, moved in meta["movement"].items():
        lines.append(f"- {window} (since {moved['since']}): entered {len(moved['entered'])}, left {len(moved['left'])}")
        lines += [f"  - entered: {p}" for p in moved["entered"][:15]]
        lines += [f"  - left: {p}" for p in moved["left"][:15]]
    lines += ["", "## Mass-change candidates (> share of files touched)", ""]
    lines += [f"- {c['sha'][:10]} {c['date']} {c['files']} files ({c['share']:.0%}) {c['subject']}" for c in meta["mass_candidates"]]
    lines += ["", "## Ignored revisions", ""] + [f"- {c['sha'][:10]} ({c['source']}) {c['subject']}" for c in meta["ignored"]]
    lines += ["", "## Spot checks", ""]
    for check in meta["spot_checks"]:
        churn, complexity = check["churn"], check["complexity"]
        lines.append(
            f"- {check['path']}: churn {churn['script_commits']}c/{churn['script_lines']}l vs --follow "
            f"{churn['follow_commits']}c/{churn['follow_lines']}l ({'match' if churn['match'] else 'DIFFERS'}: "
            f"--follow starts at {churn['follow_oldest_path']}, the lineage at {churn['lineage_oldest_path']}); "
            f"complexity {complexity['script_total']}/{complexity['script_worst']} vs single run "
            f"{complexity['single_total']}/{complexity['single_worst']} ({'match' if complexity['match'] else 'DIFFERS'})")
    lines += ["", "## Consistency", "", "```", json.dumps({k: v for k, v in meta["checks"].items() if k != "tools"}, indent=2), "```"]
    path.write_text("\n".join(lines) + "\n")


if __name__ == "__main__":
    main()
