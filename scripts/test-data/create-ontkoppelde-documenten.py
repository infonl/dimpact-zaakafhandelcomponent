#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Creates ontkoppelde documenten in the local kind stack: it creates a zaak, uploads documents to it
# and detaches them again. Optionally some of them are locked before they are detached, or removed from
# Open Zaak after they are detached, so the list also shows the "vergrendeld" and the
# "ontbreekt in Open Zaak" indicaties.
#
# A document is removed from the Open Zaak database directly, not through the Documenten API: Open Zaak
# then sends no notification and ZAC keeps the ontkoppelde document, as when a document disappears from
# Open Zaak in production. Through the API, Open Notificaties would make ZAC remove it from its list.
#
# Prerequisites: Python 3.10+, the local kind stack of ./start-kind.sh must be running with ZAC,
# and kubectl on PATH for --missing.
#
# Usage:
#   ./scripts/test-data/create-ontkoppelde-documenten.py [--count N] [--locked N] [--missing N]
#                                                        [--zaaktype UUID] [--zac-url URL] [--keycloak-url URL]
#
# Examples:
#   ./scripts/test-data/create-ontkoppelde-documenten.py
#   ./scripts/test-data/create-ontkoppelde-documenten.py --count 6 --locked 1 --missing 1

import sys

if sys.version_info < (3, 10):
    print(f"ERROR: Python 3.10+ required, running {sys.version_info.major}.{sys.version_info.minor}")
    sys.exit(1)

import argparse
import datetime
import os
import subprocess

import zac_client
import zac_testdata

# Test zaaktype 2: beheerder1 may create zaken of it on a fresh stack
DEFAULT_ZAAKTYPE_UUID = "fd2bf643-c98a-4b00-b2b3-9ae0c41ed425"
REDEN = "Created by create-ontkoppelde-documenten.py"

# Angular dev server (npm run dev), printed alongside the --zac-url link
FRONTEND_DEV_URL = "http://localhost:4200"

REMOVE_FROM_OPEN_ZAAK_SQL = """
BEGIN;
CREATE TEMP TABLE canonical_ids AS
  SELECT canonical_id AS id FROM documenten_enkelvoudiginformatieobject WHERE uuid = :'uuid';
UPDATE documenten_enkelvoudiginformatieobjectcanonical SET latest_version_id = NULL
  WHERE id IN (SELECT id FROM canonical_ids);
DELETE FROM documenten_bestandsdeel WHERE informatieobject_id IN (SELECT id FROM canonical_ids);
DELETE FROM documenten_gebruiksrechten WHERE informatieobject_id IN (SELECT id FROM canonical_ids);
DELETE FROM documenten_objectinformatieobject WHERE informatieobject_id IN (SELECT id FROM canonical_ids);
DELETE FROM documenten_verzending WHERE informatieobject_id IN (SELECT id FROM canonical_ids);
DELETE FROM documenten_enkelvoudiginformatieobject WHERE canonical_id IN (SELECT id FROM canonical_ids);
DELETE FROM documenten_enkelvoudiginformatieobjectcanonical WHERE id IN (SELECT id FROM canonical_ids);
COMMIT;
"""


def create_zaak(zaaktype_uuid: str, run_id: str, token_manager: zac_client.TokenManager, zac_url: str) -> dict:
    result = zac_testdata.create_zaak(
        1, zaaktype_uuid, token_manager, zac_url, omschrijving_prefix=run_id, toelichting=REDEN
    )
    if not result["success"] or not result["zaak_uuid"]:
        sys.exit(
            f"ERROR: could not create a zaak of zaaktype {zaaktype_uuid} (HTTP {result['status_code']}):"
            f" {result['error']}\nIs the zaaktype configured in ZAC? See configure-zaakafhandelparameters.py."
        )
    return result


def upload_documents(
    zaak: dict, count: int, run_id: str, token_manager: zac_client.TokenManager, zac_url: str
) -> list[str]:
    document_uuids = []
    for sequence in range(1, count + 1):
        document = {
            **zac_testdata.TEST_DOCUMENTS[(sequence - 1) % len(zac_testdata.TEST_DOCUMENTS)],
            "titel": f"{run_id}-{sequence}",
        }
        result = zac_testdata.upload_document_to_zaak(
            zaak["zaak_uuid"], zaak["zaaktype_uuid"], document, token_manager, zac_url
        )
        if not result["success"] or not result["document_uuid"]:
            sys.exit(f"ERROR: could not upload {run_id}-{sequence} (HTTP {result['status_code']}): {result['error']}")
        document_uuids.append(result["document_uuid"])
    return document_uuids


def lock_document(document_uuid: str, zaak_uuid: str, token_manager: zac_client.TokenManager, zac_url: str) -> None:
    status, response = zac_client.http_request(
        "POST",
        f"{zac_url}/rest/informatieobjecten/informatieobject/{document_uuid}/lock?zaak={zaak_uuid}",
        headers=zac_client.auth_headers(token_manager.get_token()),
    )
    if status != 200:
        sys.exit(f"ERROR: could not lock document {document_uuid} (HTTP {status}): {response[:200]}")


def detach_document(document_uuid: str, zaak_uuid: str, token_manager: zac_client.TokenManager, zac_url: str) -> None:
    status, response = zac_client.http_request(
        "PUT",
        f"{zac_url}/rest/zaken/zaakinformatieobjecten/ontkoppel",
        body={"zaakUUID": zaak_uuid, "documentUUID": document_uuid, "reden": REDEN},
        headers=zac_client.auth_headers(token_manager.get_token()),
    )
    if status not in (200, 204):
        sys.exit(f"ERROR: could not detach document {document_uuid} (HTTP {status}): {response[:200]}")


def remove_from_open_zaak(document_uuid: str, kube_context: str) -> None:
    result = subprocess.run(
        [
            "kubectl", "--context", kube_context, "-n", "zac", "exec", "-i", "deploy/postgres", "--",
            "psql", "-q", "-U", "postgres", "-d", "openzaak", "-v", "ON_ERROR_STOP=1", "-v", f"uuid={document_uuid}",
        ],
        input=REMOVE_FROM_OPEN_ZAAK_SQL,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        sys.exit(f"ERROR: could not remove document {document_uuid} from Open Zaak: {result.stderr.strip()}")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Create ontkoppelde documenten, optionally locked or missing in Open Zaak."
    )
    parser.add_argument("--count", type=int, default=5, metavar="N", help="Number of ontkoppelde documenten (default: 5)")
    parser.add_argument(
        "--locked", type=int, default=0, metavar="N", help="How many of them to lock before detaching them (default: 0)"
    )
    parser.add_argument(
        "--missing",
        type=int,
        default=0,
        metavar="N",
        help="How many of them to remove from Open Zaak after detaching them (default: 0); needs kubectl",
    )
    parser.add_argument(
        "--zaaktype",
        default=DEFAULT_ZAAKTYPE_UUID,
        metavar="UUID",
        help=f"Zaaktype of the zaak the documents come from (default: {DEFAULT_ZAAKTYPE_UUID}, Test zaaktype 2)",
    )
    parser.add_argument("--zac-url", default="http://localhost:8080", metavar="URL", help="ZAC base URL")
    parser.add_argument("--keycloak-url", default="http://localhost:8081", metavar="URL", help="Keycloak base URL")
    args = parser.parse_args()
    if args.count < 1:
        parser.error("--count must be >= 1")
    if args.locked < 0 or args.missing < 0 or args.locked + args.missing > args.count:
        parser.error("--locked and --missing must be >= 0, and together not more than --count")

    run_id = f"ontkoppeld-document-{datetime.datetime.now():%Y%m%d-%H%M%S}"
    kube_context = f"kind-{os.environ.get('KIND_CLUSTER_NAME', 'zac')}"
    token_manager = zac_client.TokenManager(zac_client.ZAAK_USER, zac_client.ZAAK_PASSWORD, args.keycloak_url)

    print(f"Creating a zaak of zaaktype {args.zaaktype}...")
    zaak = create_zaak(args.zaaktype, run_id, token_manager, args.zac_url)
    print(f"  created {zaak['identificatie']}")

    print(f"Uploading {args.count} documents to {zaak['identificatie']}...")
    document_uuids = upload_documents(zaak, args.count, run_id, token_manager, args.zac_url)

    for sequence, document_uuid in enumerate(document_uuids[: args.locked], start=1):
        lock_document(document_uuid, zaak["zaak_uuid"], token_manager, args.zac_url)
        print(f"  locked {run_id}-{sequence}")

    print("Detaching the documents...")
    for document_uuid in document_uuids:
        detach_document(document_uuid, zaak["zaak_uuid"], token_manager, args.zac_url)

    first_missing = args.count - args.missing
    for sequence, document_uuid in enumerate(document_uuids[first_missing:], start=first_missing + 1):
        remove_from_open_zaak(document_uuid, kube_context)
        print(f"  removed {run_id}-{sequence} from Open Zaak")

    print(
        f"\nCreated {args.count} ontkoppelde documenten ({run_id}-*):"
        f" {args.locked} locked, {args.missing} missing in Open Zaak"
    )
    print(f"  {args.zac_url}/documenten/ontkoppelde")
    print(f"  {FRONTEND_DEV_URL}/documenten/ontkoppelde")


if __name__ == "__main__":
    main()
