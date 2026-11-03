#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
"""Apply the retention policy for Git tags and GitHub releases of ZAC (ADR 0014).

  (no mode)                    dry run: write keep.txt and delete.txt, change nothing
  --check                      exit 1 when a tag or release does not match the policy
  --apply                      delete the releases and tags that do not match the policy
  --download-chart-packages D  download the chart packages that are kept from their GitHub releases into D

Chart releases (zaakafhandelcomponent-*) are only deleted with --include-charts, because the Helm repository index
points to them until their packages are on the gh-pages branch.

Needs the GitHub CLI (gh). --apply needs a token with 'contents: write'.
"""
import argparse
import re
import subprocess
import sys
import time

REPOSITORY = "infonl/dimpact-zaakafhandelcomponent"
CHART_NAME = "zaakafhandelcomponent"

# Before ZAC 4.7 every build of the main branch created a release, see ADR 0002.
FIRST_VERSION_OF_THE_CURRENT_RELEASE_PROCESS = (4, 7)

# The ZAC versions that a PodiumD release has deployed (zac.image.tag in Dimpact-Samenwerking/helm-charts).
VERSIONS_DEPLOYED_BY_PODIUMD = {
    "3.0.0", "3.5.0", "3.10.0", "3.10.0-1", "3.14.12", "3.17.0", "3.17.16", "3.17.66", "3.18.0",
    "4.0.12", "4.0.12-1", "4.3.61", "4.7.1", "4.7.2", "5.0.1", "5.0.2", "5.4.4", "5.4.5", "5.9.1",
}

# The chart versions that a PodiumD branch or tag depends on, and every chart version from 1.0.340 onwards.
CHART_VERSIONS_PINNED_BY_PODIUMD = {
    12, 19, 20, 25, 46, 98, 116, 133, 134, 137, 165, 204, 208, 224, 228, 251, 257, 297, 301, 338, 355, 362,
}
FIRST_RECENT_CHART_PATCH_VERSION = 340

RELEASE_TAG = re.compile(r"^v(\d+)\.(\d+)\.(\d+)(-dev|-\d+)?$")
CHART_TAG = re.compile(rf"^{CHART_NAME}-1\.0\.(\d+)$")


def is_kept_release(tag: str) -> bool:
    match = RELEASE_TAG.match(tag)
    if not match:
        return False
    major, minor, patch = int(match[1]), int(match[2]), int(match[3])
    return (
        (major, minor) >= FIRST_VERSION_OF_THE_CURRENT_RELEASE_PROCESS
        or patch == 0
        or tag.removeprefix("v") in VERSIONS_DEPLOYED_BY_PODIUMD
    )


def is_kept_chart_package(tag: str) -> bool:
    match = CHART_TAG.match(tag)
    if not match:
        return False
    patch = int(match[1])
    return patch in CHART_VERSIONS_PINNED_BY_PODIUMD or patch >= FIRST_RECENT_CHART_PATCH_VERSION


def gh(*arguments: str) -> str:
    return subprocess.run(["gh", *arguments], check=True, capture_output=True, text=True).stdout


def list_tags() -> set[str]:
    return set(gh("api", "--paginate", f"repos/{REPOSITORY}/tags?per_page=100", "--jq", ".[].name").split())


def list_releases() -> dict[str, int]:
    output = gh("api", "--paginate", f"repos/{REPOSITORY}/releases?per_page=100",
                "--jq", ".[] | [.tag_name, .id] | @tsv")
    return {tag: int(release_id) for tag, release_id in (line.split("\t") for line in output.splitlines())}


def delete_with_retry(path: str) -> None:
    for attempt in range(10):
        result = subprocess.run(["gh", "api", "-X", "DELETE", path], capture_output=True, text=True)
        if result.returncode == 0 or "Not Found" in result.stderr:
            return
        if "rate limit" in result.stderr.lower() or "HTTP 403" in result.stderr or "HTTP 429" in result.stderr:
            wait_seconds = 60 * (attempt + 1)
            print(f"Rate limited, waiting {wait_seconds} seconds", file=sys.stderr)
            time.sleep(wait_seconds)
            continue
        raise RuntimeError(f"DELETE {path} failed: {result.stderr}")
    raise RuntimeError(f"DELETE {path} kept hitting the rate limit")


def download_chart_packages(directory: str, releases: dict[str, int]) -> None:
    for tag in sorted(tag for tag in releases if is_kept_chart_package(tag)):
        gh("release", "download", tag, "--repo", REPOSITORY, "--pattern", f"{tag}.tgz",
           "--dir", directory, "--skip-existing")
        print(f"Downloaded {tag}.tgz")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--apply", action="store_true")
    mode.add_argument("--download-chart-packages", metavar="DIRECTORY")
    parser.add_argument("--include-charts", action="store_true")
    parser.add_argument("--max-minutes", type=int, default=0,
                        help="stop --apply after this many minutes; run again to continue")
    arguments = parser.parse_args()

    tags = list_tags()
    releases = list_releases()

    if arguments.download_chart_packages:
        download_chart_packages(arguments.download_chart_packages, releases)
        return 0

    all_tags = tags | set(releases)
    to_keep = sorted(tag for tag in all_tags if is_kept_release(tag))
    to_delete = sorted(tag for tag in all_tags if not is_kept_release(tag))

    if arguments.check:
        for tag in to_delete:
            print(f"::error::Tag or release outside the retention policy of ADR 0014: {tag}")
        return 1 if to_delete else 0

    if not arguments.include_charts:
        to_delete = [tag for tag in to_delete if not CHART_TAG.match(tag)]

    if not arguments.apply:
        with open("keep.txt", "w") as keep_file:
            keep_file.write("".join(f"{tag}\n" for tag in to_keep))
        with open("delete.txt", "w") as delete_file:
            delete_file.write("".join(f"{tag}\n" for tag in to_delete))
        print(f"Dry run: keep {len(to_keep)}, delete {len(to_delete)}. See keep.txt and delete.txt.")
        return 0

    deadline = time.monotonic() + arguments.max_minutes * 60 if arguments.max_minutes else None
    for count, tag in enumerate(to_delete, start=1):
        if deadline and time.monotonic() > deadline:
            print(f"Stopped after {arguments.max_minutes} minutes; {len(to_delete) - count + 1} left. Run again.")
            return 0
        if tag in releases:
            delete_with_retry(f"repos/{REPOSITORY}/releases/{releases[tag]}")
        if tag in tags:
            delete_with_retry(f"repos/{REPOSITORY}/git/refs/tags/{tag}")
        print(f"[{count}/{len(to_delete)}] Deleted {tag}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
