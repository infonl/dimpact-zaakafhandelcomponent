#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Creates zaak(en) of any of the local test zaaktypes (CMMN or BPMN) in ZAC and
# optionally uploads test documents (a .docx and a .pdf, cycled) to each.
#
# Zaaktype and number of documents per zaak are chosen with --zaaktype / --doc-count;
# when either is omitted the script prompts with a selectable list.
# --count sets the number of zaken to create.
#
# The zaaktype configuration is never touched: the zaaktype is expected to be configured
# already, so whatever BPMN process definition or zaakafhandelparameters are set up in ZAC
# (by hand or otherwise) stay as they are. Use create-load.py to configure zaaktypes.
# All HTTP/auth/upload logic is reused from create-load.py.
#
# At the end, a link to each created zaak is printed for both the ZAC port (8080)
# and the Angular dev server port (4200).
#
# Prerequisites: Python 3.10+, all Docker Compose services (including ZAC) must be running.
#
# Usage:
#   ./scripts/load-test/create-zaak.py [--zaaktype NAME_OR_NUMBER] [--doc-count N]
#                                      [--count N]
#                                      [--zac-url URL] [--keycloak-url URL]
#
# Examples:
#   ./scripts/load-test/create-zaak.py
#   ./scripts/load-test/create-zaak.py --zaaktype "BPMN test zaaktype 1" --doc-count 2
#   ./scripts/load-test/create-zaak.py --zaaktype 4 --doc-count 0 --count 3

import sys

if sys.version_info < (3, 10):
    print(f"ERROR: Python 3.10+ required, running {sys.version_info.major}.{sys.version_info.minor}")
    sys.exit(1)

import argparse
import importlib.util
import json
import pathlib
import time

_SCRIPT_DIR = pathlib.Path(__file__).parent

# create-load.py contains a dash, so it cannot be imported with a normal import statement
_spec = importlib.util.spec_from_file_location("create_load", _SCRIPT_DIR / "create-load.py")
create_load = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(create_load)

ZAAKTYPES = [{**zaaktype, "flavour": "CMMN"} for zaaktype in create_load.CMMN_ZAAKTYPES] + [
    {**zaaktype, "flavour": "BPMN"} for zaaktype in create_load.BPMN_ZAAKTYPES
]

DOCUMENT_COUNT_CHOICES = [0, 1, 2, 5, 10]
DEFAULT_DOCUMENT_COUNT = 2

# Angular dev server (npm run dev), printed alongside the --zac-url link
FRONTEND_DEV_URL = "http://localhost:4200"


def find_zaaktype(selection: str) -> dict | None:
    """Resolve a zaaktype by list number (1-based) or by (case-insensitive) description."""
    if selection.isdigit() and 1 <= int(selection) <= len(ZAAKTYPES):
        return ZAAKTYPES[int(selection) - 1]
    return next(
        (
            zaaktype
            for zaaktype in ZAAKTYPES
            if zaaktype["description"].lower() == selection.strip().lower()
        ),
        None,
    )


def select_zaaktype_interactively() -> dict:
    print("\nAvailable zaaktypes:")
    for number, zaaktype in enumerate(ZAAKTYPES, start=1):
        print(f"  {number}. [{zaaktype['flavour']}] {zaaktype['description']}")

    while True:
        answer = input(f"\nSelect zaaktype [1-{len(ZAAKTYPES)}] (default 1): ").strip() or "1"
        zaaktype = find_zaaktype(answer)
        if zaaktype:
            return zaaktype
        print(f"  Unknown zaaktype '{answer}', please try again.")


def select_document_count_interactively() -> int:
    default_number = DOCUMENT_COUNT_CHOICES.index(DEFAULT_DOCUMENT_COUNT) + 1
    print("\nNumber of documents per zaak:")
    for number, count in enumerate(DOCUMENT_COUNT_CHOICES, start=1):
        print(f"  {number}. {count}")

    while True:
        answer = (
            input(
                f"\nSelect number of documents [1-{len(DOCUMENT_COUNT_CHOICES)}]"
                f" (default {default_number}: {DEFAULT_DOCUMENT_COUNT}): "
            ).strip()
            or str(default_number)
        )
        if answer.isdigit() and 1 <= int(answer) <= len(DOCUMENT_COUNT_CHOICES):
            return DOCUMENT_COUNT_CHOICES[int(answer) - 1]
        print(f"  Invalid choice '{answer}', please try again.")


def create_zaken(zaaktype: dict, count: int, token_manager, zac_url: str) -> list[dict]:
    print(f"\n=== Creating {count} zaak(en) of '{zaaktype['description']}' ===")
    zaak_results = []
    for index in range(1, count + 1):
        result = create_load.create_zaak(index, zaaktype["uuid"], token_manager, zac_url)
        zaak_results.append(result)
        ok = "OK" if result["success"] else "FAIL"
        print(
            f"  [{ok}] zaak {index}: HTTP {result['status_code']} ({result['elapsed_ms']}ms)"
            f" uuid={result['zaak_uuid']}"
        )
        if not result["success"]:
            print(f"         Response: {result['error']}")
    return zaak_results


def upload_documents(
    zaak_results: list[dict], document_count: int, token_manager, zac_url: str
) -> list[dict]:
    """Upload `document_count` documents to every successfully created zaak.

    create-load.py only offers an all-documents-per-zaak upload, so the two test documents
    are cycled here to reach the requested count. Titels include the zaak index so every
    uploaded document has a unique titel across the whole run.
    """
    if document_count == 0:
        print("\n=== Uploading documents: none requested ===")
        return []

    successful_zaken = [result for result in zaak_results if result["success"] and result["zaak_uuid"]]
    print(
        f"\n=== Uploading documents ({document_count} per zaak) to {len(successful_zaken)} zaken"
        f" ({document_count * len(successful_zaken)} total) ==="
    )

    document_results = []
    for zaak_result in successful_zaken:
        for sequence in range(1, document_count + 1):
            base_document = create_load.LOAD_TEST_DOCUMENTS[
                (sequence - 1) % len(create_load.LOAD_TEST_DOCUMENTS)
            ]
            document = {
                **base_document,
                "titel": f"{base_document['titel']}-zaak{zaak_result['index']}-{sequence}",
            }
            result = create_load.upload_document_to_zaak(
                zaak_result["zaak_uuid"], zaak_result["zaaktype_uuid"], document, token_manager, zac_url
            )
            document_results.append(result)
            if not result["success"]:
                print(
                    f"  ERROR doc '{result['filename']}' zaak {result['zaak_uuid'][:8]}...: "
                    f"HTTP {result['status_code']} — {result['error']}"
                )
    return document_results


def print_zaak_links(zaak_results: list[dict], token_manager, zac_url: str) -> None:
    """Print links to each created zaak, for both the WildFly and the Angular dev server port.

    The Angular route uses the zaak identificatie (not the uuid), so it has to be read back.
    """
    print("\n=== Links ===")
    for result in zaak_results:
        if not result["success"] or not result["zaak_uuid"]:
            continue
        status, body = create_load._http(
            "GET",
            f"{zac_url}/rest/zaken/zaak/{result['zaak_uuid']}",
            headers=create_load._auth_headers(token_manager.get_token()),
        )
        if status != 200:
            print(f"  zaak {result['index']}: could not read identificatie (HTTP {status})")
            continue
        identificatie = json.loads(body)["identificatie"]
        print(f"  zaak {result['index']} ({identificatie}):")
        print(f"    {zac_url}/zaken/{identificatie}")
        print(f"    {FRONTEND_DEV_URL}/zaken/{identificatie}")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Create zaak(en) of a CMMN or BPMN test zaaktype, optionally with documents attached."
    )
    parser.add_argument(
        "--zaaktype",
        metavar="NAME_OR_NUMBER",
        help="Zaaktype description or list number; prompts with a selectable list when omitted",
    )
    parser.add_argument(
        "--doc-count",
        type=int,
        metavar="N",
        help="Number of documents per zaak (0 = none); prompts with a selectable list when omitted",
    )
    parser.add_argument(
        "--count",
        type=int,
        default=1,
        metavar="N",
        help="Number of zaken to create (default: 1)",
    )
    parser.add_argument(
        "--zac-url",
        default="http://localhost:8080",
        metavar="URL",
        help="ZAC base URL (default: http://localhost:8080)",
    )
    parser.add_argument(
        "--keycloak-url",
        default="http://localhost:8081",
        metavar="URL",
        help="Keycloak base URL (default: http://localhost:8081)",
    )
    args = parser.parse_args()

    if args.count < 1:
        parser.error("--count must be >= 1")
    if args.doc_count is not None and args.doc_count < 0:
        parser.error("--doc-count must be >= 0")

    if args.zaaktype:
        zaaktype = find_zaaktype(args.zaaktype)
        if not zaaktype:
            known = ", ".join(f"'{item['description']}'" for item in ZAAKTYPES)
            parser.error(f"unknown --zaaktype '{args.zaaktype}'; known zaaktypes: {known}")
    else:
        zaaktype = select_zaaktype_interactively()

    document_count = (
        args.doc_count if args.doc_count is not None else select_document_count_interactively()
    )

    print(
        f"\nZAC — {args.count} zaak(en) of [{zaaktype['flavour']}] '{zaaktype['description']}'"
        f" + {document_count} document(en) each"
    )
    print(f"ZAC: {args.zac_url}  Keycloak: {args.keycloak_url}")

    wall_start = time.monotonic()

    print(f"\nObtaining zaak creation token ({create_load.ZAAK_USER})...")
    token_manager = create_load.TokenManager(
        create_load.ZAAK_USER, create_load.ZAAK_PASSWORD, args.keycloak_url
    )

    zaak_results = create_zaken(zaaktype, args.count, token_manager, args.zac_url)
    document_results = upload_documents(zaak_results, document_count, token_manager, args.zac_url)

    if document_results:
        create_load.print_document_stats(document_results)
    create_load.print_stats(zaak_results)
    print_zaak_links(zaak_results, token_manager, args.zac_url)

    print(f"Wall-clock time: {time.monotonic() - wall_start:.1f}s")


if __name__ == "__main__":
    main()
