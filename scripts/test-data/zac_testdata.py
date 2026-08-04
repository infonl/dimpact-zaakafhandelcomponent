#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# ZAC test-data catalogue and the per-item create/upload operations.
#
# Holds the local test zaaktypes (CMMN and BPMN), the test documents, and the two
# operations that both create-load.py and create-zaak.py drive: creating a single zaak
# and uploading a single document to a zaak. Bulk/concurrency and zaaktype configuration
# live in the calling scripts.

import datetime
import json
import pathlib
import time

import zac_client

_SCRIPT_DIR = pathlib.Path(__file__).parent

# ---------------------------------------------------------------------------
# Constants — sourced from src/itest/kotlin/nl/info/zac/itest/config/
# ---------------------------------------------------------------------------

# Group used when creating zaken
ZAAK_GROUP_ID = "behandelaars-test-1"
ZAAK_GROUP_NAME = "Test group behandelaars domein test 1"

# Single BPMN process definition uploaded and shared by all 4 BPMN zaaktypes
LOAD_TEST_PROCESS_KEY = "loadTestProcess"
LOAD_TEST_FORM_KEY = "loadTestForm"

# Default bijlage informatieobjecttype UUID — shared by all zaaktypes except zaaktype-test-1
INFORMATIEOBJECTTYPE_BIJLAGE_UUID = "b1933137-94d6-49bc-9e12-afe712512276"

# Zaaktype-test-1 (8f24ad2f) defines its own separate bijlage IOT UUID in the Open Zaak DB setup
# (5-setup-zaaktype-test-1.sql). All other zaaktypes reuse the shared UUID from zaaktype-test-3.
BIJLAGE_UUID_BY_ZAAKTYPE = {
    "8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a": "4a689f8a-11d3-4ddd-ae26-00fb258305a5",
}

CMMN_ZAAKTYPES = [
    {
        "uuid": "8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a",
        "identificatie": "zaaktype-test-1",
        "description": "Test zaaktype 1",
        "productaanvraagtype": "productaanvraag-type-3",
        "domein": "domein_test_2",
    },
    {
        "uuid": "fd2bf643-c98a-4b00-b2b3-9ae0c41ed425",
        "identificatie": "test-zaaktype-2",
        "description": "Test zaaktype 2",
        "productaanvraagtype": "productaanvraag-type-2",
        "domein": "domein_test_1",
    },
    {
        "uuid": "448356ff-dcfb-4504-9501-7fe929077c4f",
        "identificatie": "test-zaaktype-3",
        "description": "Test zaaktype 3",
        "productaanvraagtype": "productaanvraag-type-1",
        "domein": None,
    },
]

BPMN_ZAAKTYPES = [
    {
        "uuid": "26076928-ce07-4d5d-8638-c2d276f6caca",
        "description": "BPMN test zaaktype 1",
        "process_key": LOAD_TEST_PROCESS_KEY,
        "productaanvraagtype": "bpmn-test-1-productaanvraagtype",
        "niet_ontvankelijk_uuid": "82442c7f-05f2-4e9d-a0ae-c038344809af",
    },
    {
        "uuid": "7c27a4ae-4a2a-4eb2-9db9-6cda578ce56e",
        "description": "BPMN test zaaktype 2",
        "process_key": LOAD_TEST_PROCESS_KEY,
        "productaanvraagtype": "bpmn-test-2-productaanvraagtype",
        "niet_ontvankelijk_uuid": "4f9da4cd-a910-4f85-98ca-adb33e215f43",
    },
    {
        "uuid": "e2b2d4f9-3b02-4b3e-b3d5-d26b85a7f37c",
        "description": "BPMN test zaaktype 3",
        "process_key": LOAD_TEST_PROCESS_KEY,
        "productaanvraagtype": "bpmn-test-3-productaanvraagtype",
        "niet_ontvankelijk_uuid": "c1d2e3f4-5678-9abc-def0-1234567890ab",
    },
    {
        "uuid": "f5a7b8c9-d0e1-2345-f012-345678901bcd",
        "description": "BPMN test zaaktype 4",
        "process_key": LOAD_TEST_PROCESS_KEY,
        "productaanvraagtype": "bpmn-test-4-productaanvraagtype",
        "niet_ontvankelijk_uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    },
]

ALL_ZAAKTYPE_UUIDS = [zaaktype["uuid"] for zaaktype in CMMN_ZAAKTYPES] + [
    zaaktype["uuid"] for zaaktype in BPMN_ZAAKTYPES
]

# ---------------------------------------------------------------------------
# Test documents — loaded from documents/ subfolder
# ---------------------------------------------------------------------------


def _load_document(filename: str, formaat: str, titel: str) -> dict:
    data = (_SCRIPT_DIR / "documents" / filename).read_bytes()
    return {"filename": filename, "formaat": formaat, "titel": titel, "bytes": data, "size": len(data)}


TEST_DOCUMENTS = [
    _load_document(
        "fakeWordDocument.docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "load-test-document-docx",
    ),
    _load_document(
        "fäkeTestDocument.pdf",
        "application/pdf",
        "load-test-document-pdf",
    ),
]


# ---------------------------------------------------------------------------
# Zaak creation
# ---------------------------------------------------------------------------


def create_zaak(
    index: int,
    zaaktype_uuid: str,
    token_manager: zac_client.TokenManager,
    zac_url: str,
    omschrijving_prefix: str,
    toelichting: str,
) -> dict:
    """Create a single zaak. Returns result dict with timing info.

    `omschrijving_prefix` and `toelichting` identify the creating script, so zaken from a
    load run can be told apart from hand-made ones when searching or cleaning up locally.
    """
    body = {
        "zaak": {
            "zaaktype": {"uuid": zaaktype_uuid},
            "startdatum": datetime.datetime.now().astimezone().replace(microsecond=0).isoformat(),
            "groep": {"id": ZAAK_GROUP_ID, "naam": ZAAK_GROUP_NAME},
            "communicatiekanaal": "fakeCommunicatiekanaal1",
            "vertrouwelijkheidaanduiding": "OPENBAAR",
            "omschrijving": f"{omschrijving_prefix}-{index}",
            "toelichting": toelichting,
        },
        "bagObjecten": [],
    }
    start = time.monotonic()
    status, response_body = zac_client.http_request(
        "POST",
        f"{zac_url}/rest/zaken/zaak",
        body=body,
        headers=zac_client.auth_headers(token_manager.get_token()),
    )
    elapsed = int((time.monotonic() - start) * 1000)
    zaak_uuid = None
    identificatie = None
    parse_error = None
    if status == 200:
        try:
            parsed_response = json.loads(response_body)
            identificatie = parsed_response.get("identificatie")
            zaak_uuid = parsed_response.get("zaakUUID") or parsed_response.get("uuid")
        except json.JSONDecodeError as jsonDecodeError:
            parse_error = f"Failed to parse zaak creation response JSON: {jsonDecodeError}"
        # POST /rest/zaken/zaak returns only the identificatie, so the uuid — needed for
        # document upload and for building zaak links — has to be read back separately.
        if zaak_uuid is None and identificatie:
            zaak_uuid, parse_error = _find_zaak_uuid(identificatie, token_manager, zac_url)
    return {
        "index": index,
        "zaaktype_uuid": zaaktype_uuid,
        "success": status == 200,
        "status_code": status,
        "zaak_uuid": zaak_uuid,
        "identificatie": identificatie,
        "elapsed_ms": elapsed,
        "error": response_body[:200] if status != 200 else parse_error,
    }


def _find_zaak_uuid(
    identificatie: str, token_manager: zac_client.TokenManager, zac_url: str
) -> tuple[str | None, str | None]:
    """Look up a zaak's uuid by its identificatie. Returns (uuid, error_message)."""
    status, response_body = zac_client.http_request(
        "GET",
        f"{zac_url}/rest/zaken/zaak/id/{identificatie}",
        headers=zac_client.auth_headers(token_manager.get_token()),
    )
    if status != 200:
        return None, f"Failed to read zaak '{identificatie}' (HTTP {status}): {response_body[:200]}"
    try:
        return json.loads(response_body).get("uuid"), None
    except json.JSONDecodeError as jsonDecodeError:
        return None, f"Failed to parse zaak '{identificatie}' response JSON: {jsonDecodeError}"


# ---------------------------------------------------------------------------
# Document upload
# ---------------------------------------------------------------------------


def upload_document_to_zaak(
    zaak_uuid: str,
    zaaktype_uuid: str,
    document: dict,
    token_manager: zac_client.TokenManager,
    zac_url: str,
) -> dict:
    """Upload a single document to a zaak. Returns result dict with timing info."""
    informatieobjecttype_uuid = BIJLAGE_UUID_BY_ZAAKTYPE.get(
        zaaktype_uuid, INFORMATIEOBJECTTYPE_BIJLAGE_UUID
    )
    # ZAC parses this with the strict pattern "yyyy-MM-dd'T'HH:mmXXX", so minute precision
    # is required: an ISO string including seconds is rejected.
    creatiedatum = datetime.datetime.now().astimezone().isoformat(timespec="minutes")
    fields = [
        ("bestandsnaam", document["filename"], None, None),
        ("titel", document["titel"], None, None),
        ("bestandsomvang", str(document["size"]), None, None),
        ("formaat", document["formaat"], None, None),
        ("informatieobjectTypeUUID", informatieobjecttype_uuid, None, None),
        ("vertrouwelijkheidaanduiding", "OPENBAAR", None, None),
        ("status", "in_bewerking", None, None),
        ("creatiedatum", creatiedatum, None, None),
        ("auteur", "load-test", None, None),
        ("taal", "dut", None, None),
        ("file", document["bytes"], document["filename"], document["formaat"]),
    ]
    body, content_type = zac_client.build_multipart(fields)
    start = time.monotonic()
    status, response = zac_client.http_request(
        "POST",
        f"{zac_url}/rest/informatieobjecten/informatieobject/{zaak_uuid}/{zaak_uuid}",
        body=body,
        headers={
            "Authorization": f"Bearer {token_manager.get_token()}",
            "Content-Type": content_type,
        },
    )
    elapsed = int((time.monotonic() - start) * 1000)
    return {
        "zaak_uuid": zaak_uuid,
        "filename": document["filename"],
        "success": status == 200,
        "status_code": status,
        "elapsed_ms": elapsed,
        "error": response[:200] if status != 200 else None,
    }
