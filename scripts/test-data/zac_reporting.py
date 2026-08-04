#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Summary tables for zaak creation and document upload results.
#
# Both print functions exit with code 1 when any item failed, so the calling script
# reports a non-zero exit status on partial failure.

import sys

import zac_testdata

BANNER_WIDTH = 100
BANNER_TEXT = "Also command line options possible"

# Rows of the 'ZAC' letters, one tuple per row, one string per letter.
# 'X' marks the letter strokes; they are rendered as spaces cut out of a field of '#'.
BANNER_ZAC_GLYPHS = [
    #  Z          A          C
    ("XXXXXXX", ".XXXXX.", ".XXXXX."),
    ("....XX.", "XX...XX", "XX...XX"),
    ("...XX..", "XX...XX", "XX....."),
    ("..XX...", "XXXXXXX", "XX....."),
    (".XX....", "XX...XX", "XX...XX"),
    ("XXXXXXX", "XX...XX", ".XXXXX."),
]
BANNER_GLYPH_PIXEL_WIDTH = 2  # widen each stroke: terminal characters are taller than wide
BANNER_GLYPH_GAP = 3


def print_banner() -> None:
    """Print 'ZAC' as negative space in a hash field, above a hint about --help."""
    full_line = "#" * BANNER_WIDTH
    print()
    print(full_line)
    for row in BANNER_ZAC_GLYPHS:
        cells = (" " * BANNER_GLYPH_GAP).join(row)
        rendered = "".join(
            (" " if cell == "X" else "#") * BANNER_GLYPH_PIXEL_WIDTH for cell in cells
        )
        print(rendered.center(BANNER_WIDTH, "#"))
    print(full_line)
    print(f" {BANNER_TEXT} ".center(BANNER_WIDTH, "#"))
    print(full_line)


def print_stats(zaak_results: list[dict]) -> None:
    """Print a summary table of zaak creation results grouped by zaaktype."""
    print("\n=== Results ===")

    label_by_uuid = {}
    for zaaktype in zac_testdata.CMMN_ZAAKTYPES:
        label_by_uuid[zaaktype["uuid"]] = f"CMMN {zaaktype['description']}"
    for zaaktype in zac_testdata.BPMN_ZAAKTYPES:
        label_by_uuid[zaaktype["uuid"]] = f"BPMN {zaaktype['description']}"

    by_zaaktype: dict[str, list[dict]] = {}
    for result in zaak_results:
        by_zaaktype.setdefault(result["zaaktype_uuid"], []).append(result)

    total_ok = sum(1 for result in zaak_results if result["success"])
    total_fail = len(zaak_results) - total_ok

    header = f"{'Zaaktype':<35} {'OK':>5} {'FAIL':>5} {'Mean(ms)':>10} {'Min(ms)':>8} {'Max(ms)':>8}"
    print(header)
    print("-" * len(header))

    for zaaktype_uuid in zac_testdata.ALL_ZAAKTYPE_UUIDS:
        rows = by_zaaktype.get(zaaktype_uuid, [])
        if not rows:
            continue
        ok = sum(1 for row in rows if row["success"])
        fail = len(rows) - ok
        times = [row["elapsed_ms"] for row in rows if row["success"]]
        if times:
            mean_ms = int(sum(times) / len(times))
            min_ms = min(times)
            max_ms = max(times)
        else:
            mean_ms = min_ms = max_ms = 0
        label = label_by_uuid.get(zaaktype_uuid, zaaktype_uuid[:8])
        print(f"{label:<35} {ok:>5} {fail:>5} {mean_ms:>10} {min_ms:>8} {max_ms:>8}")

    print("-" * len(header))
    all_times = [result["elapsed_ms"] for result in zaak_results if result["success"]]
    if all_times:
        overall_mean = int(sum(all_times) / len(all_times))
        overall_min = min(all_times)
        overall_max = max(all_times)
    else:
        overall_mean = overall_min = overall_max = 0
    print(f"{'TOTAL':<35} {total_ok:>5} {total_fail:>5} {overall_mean:>10} {overall_min:>8} {overall_max:>8}")

    overall_elapsed = sum(result["elapsed_ms"] for result in zaak_results)
    print(f"\nTotal HTTP time (sum): {overall_elapsed}ms")
    if total_fail > 0:
        print(f"WARNING: {total_fail} zaak(en) failed to create.")
        sys.exit(1)


def print_document_stats(document_results: list[dict]) -> None:
    """Print a summary table of document upload results grouped by filename."""
    print("\n=== Document upload results ===")

    by_filename: dict[str, list[dict]] = {}
    for result in document_results:
        by_filename.setdefault(result["filename"], []).append(result)

    total_ok = sum(1 for result in document_results if result["success"])
    total_fail = len(document_results) - total_ok

    header = f"{'Document':<55} {'OK':>5} {'FAIL':>5} {'Mean(ms)':>10} {'Min(ms)':>8} {'Max(ms)':>8}"
    print(header)
    print("-" * len(header))

    for filename, rows in by_filename.items():
        ok = sum(1 for row in rows if row["success"])
        fail = len(rows) - ok
        times = [row["elapsed_ms"] for row in rows if row["success"]]
        mean_ms = int(sum(times) / len(times)) if times else 0
        min_ms = min(times) if times else 0
        max_ms = max(times) if times else 0
        print(f"{filename:<55} {ok:>5} {fail:>5} {mean_ms:>10} {min_ms:>8} {max_ms:>8}")

    print("-" * len(header))
    print(f"{'TOTAL':<55} {total_ok:>5} {total_fail:>5}")

    if total_fail > 0:
        print(f"WARNING: {total_fail} document upload(s) failed.")
        sys.exit(1)
