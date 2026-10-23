#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# SPIKE (PZ-13059): renders the timings of up.sh and the memory samples as Markdown.
# Usage: report.py <report dir>
import os
import re
import sys

report_dir = sys.argv[1]
UNITS = {"B": 1 / 2**20, "KiB": 1 / 1024, "MiB": 1, "GiB": 1024, "kB": 1 / 1024, "MB": 1, "GB": 1024}


def to_mib(value):
    match = re.match(r"([\d.]+)\s*([A-Za-z]+)", value.strip())
    return float(match.group(1)) * UNITS.get(match.group(2), 1) if match else 0.0


def fmt(seconds):
    seconds = int(seconds)
    return f"{seconds // 60}m {seconds % 60:02d}s"


print("## Spike: integration tests on kind\n")
timings = os.path.join(report_dir, "timings.tsv")
if os.path.exists(timings):
    print("| Step | Took | Reached after |\n|---|---|---|")
    for line in open(timings):
        parts = line.rstrip("\n").split("\t")
        if len(parts) == 3 and parts[2].isdigit():
            took = fmt(parts[1]) if parts[1].isdigit() else "-"
            print(f"| {parts[0]} | {took} | {fmt(parts[2])} |")
        elif parts and parts[0] == "restarts":
            print(f"\nContainer restarts during the bring-up: {parts[2]}\n")

pulls = os.path.join(report_dir, "image-pulls.tsv")
if os.path.exists(pulls) and os.path.getsize(pulls):
    rows = [line.rstrip("\n").split("\t") for line in open(pulls) if line.count("\t") == 3]
    print("\n| Image pulled up front | Took | Done after | Result |\n|---|---|---|---|")
    for image, took, done, status in sorted(rows, key=lambda row: int(row[2])):
        print(f"| {image.split('@')[0]} | {fmt(took)} | {fmt(done)} | {status} |")

job_env = os.path.join(report_dir, "job.env")
if os.path.exists(job_env):
    values = dict(line.strip().split("=", 1) for line in open(job_env) if "=" in line)
    epochs = {key: int(value) for key, value in values.items() if value.isdigit()}
    start = epochs.get("start_epoch")
    print("\n| Job milestone | Reached after |\n|---|---|")
    for key, epoch in epochs.items():
        if start:
            print(f"| {key} | {fmt(epoch - start)} |")
    if "itest_start" in epochs and "itest_end" in epochs:
        print(f"\nShard test run (Gradle itest task): {fmt(epochs['itest_end'] - epochs['itest_start'])}")

memory = os.path.join(report_dir, "memory.tsv")
if os.path.exists(memory):
    samples = []
    for line in list(open(memory))[1:]:
        parts = line.rstrip("\n").split("\t")
        if len(parts) >= 2 and "/" in parts[1]:
            samples.append(to_mib(parts[1].split("/")[0]))
    if samples:
        print(f"\nkind node container memory (docker stats, {len(samples)} samples): "
              f"peak {max(samples):.0f} MiB, last {samples[-1]:.0f} MiB")

runner_memory = os.path.join(report_dir, "runner-memory-used-mb.tsv")
if os.path.exists(runner_memory):
    used = [int(parts[1]) for parts in (line.split() for line in open(runner_memory)) if len(parts) == 2 and parts[1].isdigit()]
    if used:
        print(f"\nRunner memory used (free -m, includes Gradle and the JVM of the tests): peak {max(used)} MiB")
