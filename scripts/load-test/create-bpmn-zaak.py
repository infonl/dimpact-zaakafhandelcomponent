#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Creates one zaak of BPMN zaaktype "BPMN test zaaktype 1" in ZAC and uploads
# two documents (a .docx and a .pdf) to it.
#
# The zaaktype is configured with the "Send Confirmation Email And Sign Documents
# Process" BPMN process definition (and its two form.io task forms), taken from the
# integration test resources, unless --skip-config is given.
# All HTTP/auth/upload logic is reused from create-load.py.
#
# Prerequisites: Python 3.10+, all Docker Compose services (including ZAC) must be running.
#
# Usage:
#   ./scripts/load-test/create-bpmn-zaak.py [--count N] [--skip-config]
#                                           [--zac-url URL] [--keycloak-url URL]
#
# Examples:
#   ./scripts/load-test/create-bpmn-zaak.py
#   ./scripts/load-test/create-bpmn-zaak.py --count 3 --skip-config

import sys

if sys.version_info < (3, 10):
    print(f"ERROR: Python 3.10+ required, running {sys.version_info.major}.{sys.version_info.minor}")
    sys.exit(1)

import argparse
import importlib.util
import pathlib
import time

_SCRIPT_DIR = pathlib.Path(__file__).parent

# create-load.py contains a dash, so it cannot be imported with a normal import statement
_spec = importlib.util.spec_from_file_location("create_load", _SCRIPT_DIR / "create-load.py")
create_load = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(create_load)

ZAAKTYPE_DESCRIPTION = "BPMN test zaaktype 1"

# "Send Confirmation Email And Sign Documents Process" — shared with the integration tests
# (see BPMN_DOCUMENT_SIGN_* in src/itest/kotlin/nl/info/zac/itest/config/ItestConfiguration.kt)
_ITEST_BPMN_DIR = _SCRIPT_DIR.parents[1] / "src" / "itest" / "resources" / "bpmn" / "document-sign"
PROCESS_KEY = "sendConfirmationEmailAndSignDocumentsProcess"
PROCESS_FILENAME = f"{PROCESS_KEY}.bpmn"
FORM_FILENAMES = ["selectDocumentsForm.json", "signDocumentsForm.json"]

BPMN_ZAAKTYPE = {
    **next(
        zaaktype for zaaktype in create_load.BPMN_ZAAKTYPES if zaaktype["description"] == ZAAKTYPE_DESCRIPTION
    ),
    "process_key": PROCESS_KEY,
}


def upload_process_definition_and_forms(token: str, zac_url: str) -> None:
    """Upload the document-sign BPMN process definition and its two form.io task forms.

    Re-uploading a process definition deploys a new version in Flowable and re-uploading a form
    overwrites the stored content, so both are safe to repeat on subsequent runs.
    """
    print(f"\n=== Uploading BPMN process definition '{PROCESS_KEY}' and forms ===")

    uploads = [(f"{zac_url}/rest/bpmn-process-definitions", PROCESS_FILENAME)] + [
        (f"{zac_url}/rest/bpmn-process-definitions/{PROCESS_KEY}/forms", filename)
        for filename in FORM_FILENAMES
    ]
    for url, filename in uploads:
        t0 = time.monotonic()
        status, body = create_load._http(
            "POST",
            url,
            body={"filename": filename, "content": (_ITEST_BPMN_DIR / filename).read_text()},
            headers=create_load._auth_headers(token),
        )
        elapsed = int((time.monotonic() - t0) * 1000)
        ok = "OK" if status == 201 else "FAIL"
        print(f"  [{ok}] {filename}: HTTP {status} ({elapsed}ms)")
        if status != 201:
            print(f"         Response: {body[:300]}")


def configure_zaaktype(token: str, zac_url: str) -> None:
    """Configure the BPMN zaaktype to use the document-sign process definition."""
    upload_process_definition_and_forms(token, zac_url)

    print(f"\n=== Configuring BPMN zaaktype '{ZAAKTYPE_DESCRIPTION}' ===")
    t0 = time.monotonic()
    status, body = create_load._http(
        "POST",
        f"{zac_url}/rest/zaaktype-bpmn-configuration",
        body=create_load._bpmn_body(BPMN_ZAAKTYPE),
        headers=create_load._auth_headers(token),
    )
    elapsed = int((time.monotonic() - t0) * 1000)
    ok = "OK" if status == 200 else "FAIL"
    print(f"  [{ok}] BPMN {ZAAKTYPE_DESCRIPTION}: HTTP {status} ({elapsed}ms)")
    if status != 200:
        print(f"         Response: {body[:300]}")


def create_zaken_with_documents(
    count: int, token_manager, zac_url: str
) -> tuple[list[dict], list[dict]]:
    """Create `count` zaken of the BPMN zaaktype and upload both test documents to each."""
    print(f"\n=== Creating {count} zaak(en) of '{ZAAKTYPE_DESCRIPTION}' ===")
    zaak_results = []
    for index in range(1, count + 1):
        result = create_load.create_zaak(index, BPMN_ZAAKTYPE["uuid"], token_manager, zac_url)
        zaak_results.append(result)
        ok = "OK" if result["success"] else "FAIL"
        print(
            f"  [{ok}] zaak {index}: HTTP {result['status_code']} ({result['elapsed_ms']}ms)"
            f" uuid={result['zaak_uuid']}"
        )
        if not result["success"]:
            print(f"         Response: {result['error']}")

    document_results = create_load.upload_documents_to_zaken(zaak_results, token_manager, zac_url, 1)
    return zaak_results, document_results


def main() -> None:
    parser = argparse.ArgumentParser(
        description=f"Create zaak(en) of BPMN zaaktype '{ZAAKTYPE_DESCRIPTION}' with two documents attached."
    )
    parser.add_argument(
        "--count",
        type=int,
        default=1,
        metavar="N",
        help="Number of zaken to create (default: 1)",
    )
    parser.add_argument(
        "--skip-config",
        action="store_true",
        help="Skip process definition/form upload and zaaktype configuration (use when already configured)",
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

    print(f"ZAC — {args.count} zaak(en) of '{ZAAKTYPE_DESCRIPTION}' + 2 documents each")
    print(f"ZAC: {args.zac_url}  Keycloak: {args.keycloak_url}")

    wall_start = time.monotonic()

    if not args.skip_config:
        print(f"\nObtaining config token ({create_load.CONFIG_USER})...")
        config_token = create_load.get_token(
            create_load.CONFIG_USER, create_load.CONFIG_PASSWORD, args.keycloak_url
        )
        configure_zaaktype(config_token, args.zac_url)
    else:
        print("\nSkipping process definition/form upload and zaaktype configuration (--skip-config)")

    print(f"\nObtaining zaak creation token ({create_load.ZAAK_USER})...")
    token_manager = create_load.TokenManager(
        create_load.ZAAK_USER, create_load.ZAAK_PASSWORD, args.keycloak_url
    )

    zaak_results, document_results = create_zaken_with_documents(args.count, token_manager, args.zac_url)

    create_load.print_document_stats(document_results)
    create_load.print_stats(zaak_results)

    print(f"Wall-clock time: {time.monotonic() - wall_start:.1f}s")


if __name__ == "__main__":
    main()
