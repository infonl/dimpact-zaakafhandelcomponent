#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Creates inbox documenten in the local kind stack: documents in Open Zaak that belong to no zaak.
#
# ZAC adds a document to Inbox documenten when it receives the notification that a document was
# created in Open Zaak. When Open Notificaties runs (./start-kind.sh -n) it sends that notification;
# the script waits for it, and notifies ZAC itself of every document that is not in the inbox after
# the wait, so the script also works without Open Notificaties.
#
# Prerequisites: Python 3.10+, the local kind stack of ./start-kind.sh must be running with ZAC.
#
# Usage:
#   ./scripts/test-data/create-inbox-documenten.py [--count N] [--wait SECONDS]
#                                                  [--zac-url URL] [--keycloak-url URL] [--open-zaak-url URL]
#
# Examples:
#   ./scripts/test-data/create-inbox-documenten.py
#   ./scripts/test-data/create-inbox-documenten.py --count 20 --wait 0

import sys

if sys.version_info < (3, 10):
    print(f"ERROR: Python 3.10+ required, running {sys.version_info.major}.{sys.version_info.minor}")
    sys.exit(1)

import argparse
import base64
import datetime
import json
import time
import uuid

import zac_client
import zac_testdata

# The bronorganisatie of the documents in the local kind stack, the same value as in the integration tests
BRONORGANISATIE = "123443210"
DOCUMENTEN_API_PATH = "/documenten/api/v1/enkelvoudiginformatieobjecten"
INBOX_PAGE_SIZE = 100

# Angular dev server (npm run dev), printed alongside the --zac-url link
FRONTEND_DEV_URL = "http://localhost:4200"


def read_informatieobjecttype_url(open_zaak_url: str) -> str:
    """Read the bijlage informatieobjecttype url as Open Zaak serves it: the Documenten API accepts only that form."""
    status, response = zac_client.http_request(
        "GET",
        f"{open_zaak_url}/catalogi/api/v1/informatieobjecttypen/{zac_testdata.INFORMATIEOBJECTTYPE_BIJLAGE_UUID}",
        headers=zac_client.open_zaak_auth_headers(),
    )
    if status != 200:
        sys.exit(f"ERROR: could not read the informatieobjecttype from Open Zaak (HTTP {status}): {response[:200]}")
    return json.loads(response)["url"]


def create_document_in_open_zaak(titel: str, sequence: int, informatieobjecttype_url: str, open_zaak_url: str) -> str:
    document = zac_testdata.test_document(sequence, titel)
    body = {
        "bronorganisatie": BRONORGANISATIE,
        "creatiedatum": datetime.date.today().isoformat(),
        "titel": titel,
        "auteur": "test-data",
        "taal": "dut",
        "informatieobjecttype": informatieobjecttype_url,
        "inhoud": base64.b64encode(document["bytes"]).decode(),
        "bestandsnaam": document["filename"],
        "bestandsomvang": document["size"],
        "formaat": document["formaat"],
        "vertrouwelijkheidaanduiding": "openbaar",
        "status": "in_bewerking",
    }
    status, response = zac_client.http_request(
        "POST", f"{open_zaak_url}{DOCUMENTEN_API_PATH}", body=body, headers=zac_client.open_zaak_auth_headers()
    )
    if status != 201:
        sys.exit(f"ERROR: could not create '{titel}' in Open Zaak (HTTP {status}): {response[:200]}")
    return json.loads(response)["url"].rstrip("/").rsplit("/", 1)[-1]


def inbox_document_uuids(titel: str, token_manager: zac_client.TokenManager, zac_url: str) -> set[str]:
    """The uuids of the inbox documenten whose titel contains the given text, from all pages."""
    document_uuids = set()
    page = 0
    while True:
        status, response = zac_client.http_request(
            "PUT",
            f"{zac_url}/rest/inboxdocumenten",
            body={"titel": titel, "page": page, "maxResults": INBOX_PAGE_SIZE},
            headers=zac_client.auth_headers(token_manager.get_token()),
        )
        if status != 200:
            sys.exit(f"ERROR: could not list the inbox documenten (HTTP {status}): {response[:200]}")
        results = json.loads(response)["resultaten"]
        document_uuids.update(document["enkelvoudiginformatieobjectUUID"] for document in results)
        if len(results) < INBOX_PAGE_SIZE:
            return document_uuids
        page += 1


def notify_zac(document_uuid: str, open_zaak_url: str, zac_url: str) -> None:
    """Send ZAC the notification that Open Notificaties sends when a document is created in Open Zaak."""
    document_url = f"{open_zaak_url}{DOCUMENTEN_API_PATH}/{document_uuid}"
    body = {
        "kanaal": "documenten",
        "resource": "enkelvoudiginformatieobject",
        "hoofdObject": document_url,
        "resourceUrl": document_url,
        "actie": "create",
        "aanmaakdatum": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    }
    status, response = zac_client.http_request(
        "POST", f"{zac_url}/rest/notificaties", body=body, headers=zac_client.notification_headers()
    )
    if status != 204:
        sys.exit(f"ERROR: could not notify ZAC of document {document_uuid} (HTTP {status}): {response[:200]}")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Create inbox documenten: documents in Open Zaak that belong to no zaak."
    )
    parser.add_argument("--count", type=int, default=5, metavar="N", help="Number of inbox documenten (default: 5)")
    parser.add_argument(
        "--wait",
        type=int,
        default=20,
        metavar="SECONDS",
        help="How long to wait for Open Notificaties before notifying ZAC directly (default: 20). "
        "Keep it when Open Notificaties runs: a notification that arrives later makes ZAC answer HTTP 500.",
    )
    parser.add_argument("--zac-url", default="http://localhost:8080", metavar="URL", help="ZAC base URL")
    parser.add_argument("--keycloak-url", default="http://localhost:8081", metavar="URL", help="Keycloak base URL")
    parser.add_argument("--open-zaak-url", default="http://localhost:8001", metavar="URL", help="Open Zaak base URL")
    args = parser.parse_args()
    if args.count < 1:
        parser.error("--count must be >= 1")

    run_id = f"inbox-document-{datetime.datetime.now():%Y%m%d-%H%M%S}-{uuid.uuid4().hex[:4]}"
    token_manager = zac_client.TokenManager(zac_client.ZAAK_USER, zac_client.ZAAK_PASSWORD, args.keycloak_url)
    informatieobjecttype_url = read_informatieobjecttype_url(args.open_zaak_url)

    print(f"Creating {args.count} inbox documenten in Open Zaak ({run_id}-*)...")
    document_uuids = []
    for sequence in range(1, args.count + 1):
        document_uuids.append(
            create_document_in_open_zaak(f"{run_id}-{sequence}", sequence, informatieobjecttype_url, args.open_zaak_url)
        )
        print(f"  created {run_id}-{sequence}")

    print(f"Waiting up to {args.wait}s for Open Notificaties to put them in the inbox...")
    deadline = time.monotonic() + args.wait
    in_inbox = inbox_document_uuids(run_id, token_manager, args.zac_url)
    while time.monotonic() < deadline and not in_inbox.issuperset(document_uuids):
        time.sleep(1)
        in_inbox = inbox_document_uuids(run_id, token_manager, args.zac_url)

    for sequence, document_uuid in enumerate(document_uuids, start=1):
        if document_uuid not in in_inbox:
            notify_zac(document_uuid, args.open_zaak_url, args.zac_url)
            print(f"  notified ZAC of {run_id}-{sequence} directly")

    in_inbox = inbox_document_uuids(run_id, token_manager, args.zac_url)
    print(f"\nInbox documenten now in ZAC: {len(in_inbox.intersection(document_uuids))} of {args.count}")
    print(f"  {args.zac_url}/documenten/inbox")
    print(f"  {FRONTEND_DEV_URL}/documenten/inbox")


if __name__ == "__main__":
    main()
