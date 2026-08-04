#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Local load test script for ZAC.
#
# Uploads a BPMN process definition and form.io task forms to ZAC, creates
# zaakafhandelparameters for all 7 zaaktypes in Open Zaak (3 CMMN, 4 BPMN),
# then creates a user-specified number of zaken distributed across all zaaktypes.
#
# HTTP/auth lives in zac_client.py, the zaaktype/document catalogue and the single-item
# create/upload operations in zac_testdata.py, the summary tables in zac_reporting.py.
#
# Prerequisites: Python 3.10+, all Docker Compose services (including ZAC) must be running.
#
# Usage:
#   ./scripts/test-data/create-load.py <zaken_count> [--skip-config]
#                                      [--concurrency N] [--zac-url URL]
#                                      [--keycloak-url URL] [--add-documents]
#
# Examples:
#   ./scripts/test-data/create-load.py 10
#   ./scripts/test-data/create-load.py 100 --skip-config --concurrency 4
#   ./scripts/test-data/create-load.py 50 --add-documents --concurrency 4

import sys

if sys.version_info < (3, 10):
    print(f"ERROR: Python 3.10+ required, running {sys.version_info.major}.{sys.version_info.minor}")
    sys.exit(1)

import argparse
import json
import pathlib
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

import zac_client
import zac_reporting
import zac_testdata

_SCRIPT_DIR = pathlib.Path(__file__).parent

# ---------------------------------------------------------------------------
# Constants — sourced from src/itest/kotlin/nl/info/zac/itest/config/
# ---------------------------------------------------------------------------

# Niet-ontvankelijk resultaattype shared by all CMMN zaaktypes
CMMN_NIET_ONTVANKELIJK_UUID = "dd2bcd87-ed7e-4b23-a8e3-ea7fe7ef00c6"

# Stamped on every zaak this script creates, to tell them apart from create-zaak.py's
ZAAK_OMSCHRIJVING_PREFIX = "load-test-zaak"
ZAAK_TOELICHTING = "Created by ZAC load test script"

# ---------------------------------------------------------------------------
# BPMN process definition and form.io task form — loaded from bpmn/ subfolder
#
# A minimal Flowable BPMN 2.0 process with:
#   - one user task assigned to the zaak's behandelaar / groep
#   - formKey referencing the loadTestForm
# All 4 BPMN zaaktypes in this script share this single process definition.
# ---------------------------------------------------------------------------

LOAD_TEST_BPMN = (_SCRIPT_DIR / "bpmn" / f"{zac_testdata.LOAD_TEST_PROCESS_KEY}.bpmn").read_text()
LOAD_TEST_FORM = (_SCRIPT_DIR / "bpmn" / f"{zac_testdata.LOAD_TEST_FORM_KEY}.json").read_text()


# ---------------------------------------------------------------------------
# BPMN process definition and form upload
# ---------------------------------------------------------------------------


def upload_bpmn_process_definitions(token: str, zac_url: str) -> None:
    """Upload the load test BPMN process definition and its form.io task form.

    Re-uploading a BPMN process definition deploys a new version in Flowable, which is harmless.
    Re-uploading a form simply overwrites the stored form content.
    Both operations are therefore safe to repeat on subsequent runs.
    """
    print("\n=== Uploading BPMN process definition and forms ===")

    # Upload BPMN process definition
    t0 = time.monotonic()
    status, body = zac_client.http_request(
        "POST",
        f"{zac_url}/rest/bpmn-process-definitions",
        body={"filename": f"{zac_testdata.LOAD_TEST_PROCESS_KEY}.bpmn", "content": LOAD_TEST_BPMN},
        headers=zac_client.auth_headers(token),
    )
    elapsed = int((time.monotonic() - t0) * 1000)
    ok = "OK" if status == 201 else "FAIL"
    print(f"  [{ok}] BPMN process '{zac_testdata.LOAD_TEST_PROCESS_KEY}': HTTP {status} ({elapsed}ms)")
    if status != 201:
        print(f"         Response: {body[:300]}")

    # Upload form.io task form
    t0 = time.monotonic()
    status, body = zac_client.http_request(
        "POST",
        f"{zac_url}/rest/bpmn-process-definitions/{zac_testdata.LOAD_TEST_PROCESS_KEY}/forms",
        body={"filename": f"{zac_testdata.LOAD_TEST_FORM_KEY}.json", "content": LOAD_TEST_FORM},
        headers=zac_client.auth_headers(token),
    )
    elapsed = int((time.monotonic() - t0) * 1000)
    ok = "OK" if status == 201 else "FAIL"
    print(f"  [{ok}] Form '{zac_testdata.LOAD_TEST_FORM_KEY}': HTTP {status} ({elapsed}ms)")
    if status != 201:
        print(f"         Response: {body[:300]}")


# ---------------------------------------------------------------------------
# Zaakafhandelparameters creation
# ---------------------------------------------------------------------------


def _cmmn_body(zaaktype: dict) -> dict:
    """Build the PUT /rest/zaakafhandelparameters payload for a CMMN zaaktype."""
    return {
        "humanTaskParameters": [
            {
                "planItemDefinition": {
                    "defaultFormulierDefinitie": "AANVULLENDE_INFORMATIE",
                    "id": "AANVULLENDE_INFORMATIE",
                    "naam": "Aanvullende informatie",
                    "type": "HUMAN_TASK",
                },
                "defaultGroepId": None,
                "formulierDefinitieId": "AANVULLENDE_INFORMATIE",
                "referentieTabellen": [],
                "actief": True,
                "doorlooptijd": None,
            },
            {
                "planItemDefinition": {
                    "defaultFormulierDefinitie": "GOEDKEUREN",
                    "id": "GOEDKEUREN",
                    "naam": "Goedkeuren",
                    "type": "HUMAN_TASK",
                },
                "defaultGroepId": None,
                "formulierDefinitieId": "GOEDKEUREN",
                "referentieTabellen": [],
                "actief": True,
                "doorlooptijd": None,
            },
            {
                "planItemDefinition": {
                    "defaultFormulierDefinitie": "ADVIES",
                    "id": "ADVIES_INTERN",
                    "naam": "Advies intern",
                    "type": "HUMAN_TASK",
                },
                "defaultGroepId": None,
                "formulierDefinitieId": "ADVIES",
                "referentieTabellen": [
                    {
                        "veld": "ADVIES",
                        "tabel": {
                            "aantalWaarden": 5,
                            "code": "ADVIES",
                            "id": 1,
                            "naam": "Advies",
                            "systeem": True,
                        },
                    }
                ],
                "actief": True,
                "doorlooptijd": None,
            },
            {
                "planItemDefinition": {
                    "defaultFormulierDefinitie": "EXTERN_ADVIES_VASTLEGGEN",
                    "id": "ADVIES_EXTERN",
                    "naam": "Advies extern",
                    "type": "HUMAN_TASK",
                },
                "defaultGroepId": None,
                "formulierDefinitieId": "EXTERN_ADVIES_VASTLEGGEN",
                "referentieTabellen": [],
                "actief": True,
                "doorlooptijd": None,
            },
            {
                "planItemDefinition": {
                    "defaultFormulierDefinitie": "DOCUMENT_VERZENDEN_POST",
                    "id": "DOCUMENT_VERZENDEN_POST",
                    "naam": "Document verzenden",
                    "type": "HUMAN_TASK",
                },
                "defaultGroepId": None,
                "formulierDefinitieId": "DOCUMENT_VERZENDEN_POST",
                "referentieTabellen": [],
                "actief": True,
                "doorlooptijd": None,
            },
        ],
        "mailtemplateKoppelingen": [
            {
                "mailtemplate": {
                    "body": "<p>Beste {ZAAK_INITIATOR},</p><p></p><p>Uw verzoek over {ZAAK_TYPE} met zaaknummer "
                    "{ZAAK_NUMMER} wordt niet in behandeling genomen. Voor meer informatie gaat u naar Mijn Loket.</p>"
                    "<p></p><p>Met vriendelijke groet,</p><p></p><p>Gemeente Dommeldam</p>",
                    "defaultMailtemplate": True,
                    "id": 2,
                    "mail": "ZAAK_NIET_ONTVANKELIJK",
                    "mailTemplateNaam": "Zaak niet ontvankelijk",
                    "onderwerp": "<p>Wij hebben uw verzoek niet in behandeling genomen (zaaknummer: {ZAAK_NUMMER})</p>",
                    "variabelen": [
                        "GEMEENTE",
                        "ZAAK_NUMMER",
                        "ZAAK_TYPE",
                        "ZAAK_STATUS",
                        "ZAAK_REGISTRATIEDATUM",
                        "ZAAK_STARTDATUM",
                        "ZAAK_STREEFDATUM",
                        "ZAAK_FATALEDATUM",
                        "ZAAK_OMSCHRIJVING",
                        "ZAAK_TOELICHTING",
                        "ZAAK_INITIATOR",
                        "ZAAK_INITIATOR_ADRES",
                    ],
                }
            }
        ],
        "userEventListenerParameters": [
            {"id": "INTAKE_AFRONDEN", "naam": "Intake afronden", "toelichting": None},
            {"id": "ZAAK_AFHANDELEN", "naam": "Zaak afhandelen", "toelichting": None},
        ],
        "valide": False,
        "zaakAfzenders": [
            {"defaultMail": True, "mail": "GEMEENTE", "speciaal": True, "replyTo": "GEMEENTE"},
            {"defaultMail": False, "mail": "MEDEWERKER", "speciaal": True, "replyTo": None},
        ],
        "zaakbeeindigParameters": [],
        "zaaktype": {
            "beginGeldigheid": "2023-09-21",
            "doel": zaaktype["description"],
            "identificatie": zaaktype["identificatie"],
            "nuGeldig": True,
            "omschrijving": zaaktype["description"],
            "servicenorm": False,
            "uuid": zaaktype["uuid"],
            "versiedatum": "2023-09-21",
            "vertrouwelijkheidaanduiding": "openbaar",
        },
        "intakeMail": "BESCHIKBAAR_UIT",
        "afrondenMail": "BESCHIKBAAR_UIT",
        "caseDefinition": {
            "humanTaskDefinitions": [
                {
                    "defaultFormulierDefinitie": "AANVULLENDE_INFORMATIE",
                    "id": "AANVULLENDE_INFORMATIE",
                    "naam": "Aanvullende informatie",
                    "type": "HUMAN_TASK",
                },
                {
                    "defaultFormulierDefinitie": "GOEDKEUREN",
                    "id": "GOEDKEUREN",
                    "naam": "Goedkeuren",
                    "type": "HUMAN_TASK",
                },
                {
                    "defaultFormulierDefinitie": "ADVIES",
                    "id": "ADVIES_INTERN",
                    "naam": "Advies intern",
                    "type": "HUMAN_TASK",
                },
                {
                    "defaultFormulierDefinitie": "EXTERN_ADVIES_VASTLEGGEN",
                    "id": "ADVIES_EXTERN",
                    "naam": "Advies extern",
                    "type": "HUMAN_TASK",
                },
                {
                    "defaultFormulierDefinitie": "DOCUMENT_VERZENDEN_POST",
                    "id": "DOCUMENT_VERZENDEN_POST",
                    "naam": "Document verzenden",
                    "type": "HUMAN_TASK",
                },
            ],
            "key": "generiek-zaakafhandelmodel",
            "naam": "Generiek zaakafhandelmodel",
            "userEventListenerDefinitions": [
                {
                    "defaultFormulierDefinitie": "DEFAULT_TAAKFORMULIER",
                    "id": "INTAKE_AFRONDEN",
                    "naam": "Intake afronden",
                    "type": "USER_EVENT_LISTENER",
                },
                {
                    "defaultFormulierDefinitie": "DEFAULT_TAAKFORMULIER",
                    "id": "ZAAK_AFHANDELEN",
                    "naam": "Zaak afhandelen",
                    "type": "USER_EVENT_LISTENER",
                },
            ],
        },
        "domein": zaaktype["domein"],
        "defaultGroepId": zac_testdata.ZAAK_GROUP_ID,
        "defaultBehandelaarId": None,
        "einddatumGeplandWaarschuwing": None,
        "uiterlijkeEinddatumAfdoeningWaarschuwing": None,
        "productaanvraagtype": zaaktype["productaanvraagtype"],
        "zaakNietOntvankelijkResultaattype": {
            "archiefNominatie": "VERNIETIGEN",
            "archiefTermijn": "5 jaren",
            "besluitVerplicht": False,
            "id": CMMN_NIET_ONTVANKELIJK_UUID,
            "naam": "Geweigerd",
            "naamGeneriek": "Geweigerd",
            "toelichting": "Het door het orgaan behandelen van een aanvraag, melding of verzoek om toestemming voor het "
            "doen of laten van een derde waar het orgaan bevoegd is om over te beslissen",
            "vervaldatumBesluitVerplicht": False,
        },
        "smartDocuments": {"enabledForZaaktype": True},
        "betrokkeneKoppelingen": {"brpKoppelen": True, "kvkKoppelen": True},
        "brpDoelbindingen": {
            "zoekWaarde": "BRPACT-ZoekenAlgemeen",
            "raadpleegWaarde": "BRPACT-AlgemeneTaken",
            "verwerkingregisterWaarde": "Algemeen",
        },
        "automaticEmailConfirmation": {
            "enabled": True,
            "templateName": "Ontvangstbevestiging",
            "emailSender": "GEMEENTE",
            "emailReply": "reply@example.com",
        },
    }


def _bpmn_body(zaaktype: dict) -> dict:
    """Build the POST /rest/zaaktype-bpmn-configuration payload for a BPMN zaaktype."""
    return {
        "zaaktypeUuid": zaaktype["uuid"],
        "zaaktypeOmschrijving": zaaktype["description"],
        "bpmnProcessDefinitionKey": zaaktype["process_key"],
        "productaanvraagtype": zaaktype["productaanvraagtype"],
        "groepNaam": zac_testdata.ZAAK_GROUP_ID,
        "defaultBehandelaarId": zac_client.ZAAK_USER,
        "betrokkeneKoppelingen": {"brpKoppelen": True, "kvkKoppelen": True},
        "brpDoelbindingen": {
            "zoekWaarde": "BRPACT-ZoekenAlgemeen",
            "raadpleegWaarde": "BRPACT-AlgemeneTaken",
            "verwerkingregisterWaarde": "Algemeen",
        },
        "zaakbeeindigParameters": [],
        "zaakNietOntvankelijkResultaattype": {
            "archiefNominatie": "VERNIETIGEN",
            "archiefTermijn": "5 jaren",
            "besluitVerplicht": False,
            "id": zaaktype["niet_ontvankelijk_uuid"],
            "naam": "Geweigerd",
            "naamGeneriek": "Geweigerd",
            "toelichting": "fakeNietOntvankelijkToelichting",
            "vervaldatumBesluitVerplicht": False,
        },
    }


def _merge_existing_ids(desired: dict, existing: dict) -> dict:
    """Merge numeric DB IDs from an existing zaakafhandelparameters GET response into a
    desired PUT body.  ZAC's PUT endpoint uses JPA merge semantics: when updating an existing
    record every nested entity that already has a row in the database must carry its numeric
    `id`, otherwise Hibernate treats it as a detached entity and raises a 500 error.
    """
    result = {**desired}

    # Top-level ID of the zaakafhandelparameters record
    if "id" in existing:
        result["id"] = existing["id"]

    # Simple nested entities that have a single `id`
    for key in ("automaticEmailConfirmation", "betrokkeneKoppelingen", "brpDoelbindingen"):
        if key in existing and key in result and "id" in existing[key]:
            result[key] = {**result[key], "id": existing[key]["id"]}

    # humanTaskParameters — match by planItemDefinition.id (string key like "GOEDKEUREN")
    if "humanTaskParameters" in existing and "humanTaskParameters" in result:
        existing_by_def = {
            p["planItemDefinition"]["id"]: p
            for p in existing["humanTaskParameters"]
            if "planItemDefinition" in p
        }
        merged_params = []
        for param in result["humanTaskParameters"]:
            param = {**param}
            def_id = param.get("planItemDefinition", {}).get("id")
            ex_param = existing_by_def.get(def_id) if def_id else None
            if ex_param:
                if "id" in ex_param:
                    param["id"] = ex_param["id"]
                # referentieTabellen — match by veld
                if param.get("referentieTabellen") and ex_param.get("referentieTabellen"):
                    ex_refs = {r["veld"]: r for r in ex_param["referentieTabellen"] if "veld" in r}
                    merged_refs = []
                    for ref in param["referentieTabellen"]:
                        ref = {**ref}
                        ex_ref = ex_refs.get(ref.get("veld"))
                        if ex_ref:
                            if "id" in ex_ref:
                                ref["id"] = ex_ref["id"]
                            if "tabel" in ref and "tabel" in ex_ref and "id" in ex_ref["tabel"]:
                                ref["tabel"] = {**ref["tabel"], "id": ex_ref["tabel"]["id"]}
                        merged_refs.append(ref)
                    param["referentieTabellen"] = merged_refs
            merged_params.append(param)
        result["humanTaskParameters"] = merged_params

    # mailtemplateKoppelingen — match by mailtemplate.mail (e.g. "ZAAK_NIET_ONTVANKELIJK")
    if "mailtemplateKoppelingen" in existing and "mailtemplateKoppelingen" in result:
        ex_by_mail = {
            k["mailtemplate"]["mail"]: k
            for k in existing["mailtemplateKoppelingen"]
            if "mailtemplate" in k and "mail" in k["mailtemplate"]
        }
        merged_koppelingen = []
        for koppeling in result["mailtemplateKoppelingen"]:
            koppeling = {**koppeling}
            mail_type = koppeling.get("mailtemplate", {}).get("mail")
            ex_koppeling = ex_by_mail.get(mail_type) if mail_type else None
            if ex_koppeling:
                if "id" in ex_koppeling:
                    koppeling["id"] = ex_koppeling["id"]
                if "mailtemplate" in koppeling and "id" in ex_koppeling.get("mailtemplate", {}):
                    koppeling["mailtemplate"] = {
                        **koppeling["mailtemplate"],
                        "id": ex_koppeling["mailtemplate"]["id"],
                    }
            merged_koppelingen.append(koppeling)
        result["mailtemplateKoppelingen"] = merged_koppelingen

    # zaakAfzenders — match by mail field (e.g. "GEMEENTE", "MEDEWERKER")
    if "zaakAfzenders" in existing and "zaakAfzenders" in result:
        ex_by_mail = {a["mail"]: a for a in existing["zaakAfzenders"] if "mail" in a}
        merged_afzenders = []
        for afzender in result["zaakAfzenders"]:
            afzender = {**afzender}
            ex_afzender = ex_by_mail.get(afzender.get("mail"))
            if ex_afzender and "id" in ex_afzender:
                afzender["id"] = ex_afzender["id"]
            merged_afzenders.append(afzender)
        result["zaakAfzenders"] = merged_afzenders

    return result


def create_zaakafhandelparameters(token: str, zac_url: str) -> None:
    """Create zaakafhandelparameters for all 7 zaaktypes (3 CMMN, 4 BPMN)."""
    print("\n=== Creating zaakafhandelparameters ===")

    for zaaktype in zac_testdata.CMMN_ZAAKTYPES:
        # GET first: ZAC's PUT uses JPA merge semantics. When a record already exists every
        # nested entity needs its numeric DB id in the request body, otherwise Hibernate raises
        # "detached entity passed to persist". We fetch the existing config and merge the IDs.
        get_status, get_body = zac_client.http_request(
            "GET",
            f"{zac_url}/rest/zaakafhandelparameters/{zaaktype['uuid']}",
            headers=zac_client.auth_headers(token),
        )
        desired = _cmmn_body(zaaktype)
        if get_status == 200:
            desired = _merge_existing_ids(desired, json.loads(get_body))

        t0 = time.monotonic()
        status, body = zac_client.http_request(
            "PUT",
            f"{zac_url}/rest/zaakafhandelparameters",
            body=desired,
            headers=zac_client.auth_headers(token),
        )
        elapsed = int((time.monotonic() - t0) * 1000)
        ok = "OK" if status == 200 else "FAIL"
        print(f"  [{ok}] CMMN {zaaktype['description']}: HTTP {status} ({elapsed}ms)")
        if status != 200:
            print(f"         Response: {body[:500]}")

    for zaaktype in zac_testdata.BPMN_ZAAKTYPES:
        t0 = time.monotonic()
        status, body = zac_client.http_request(
            "POST",
            f"{zac_url}/rest/zaaktype-bpmn-configuration",
            body=_bpmn_body(zaaktype),
            headers=zac_client.auth_headers(token),
        )
        elapsed = int((time.monotonic() - t0) * 1000)
        ok = "OK" if status == 200 else "FAIL"
        print(f"  [{ok}] BPMN {zaaktype['description']}: HTTP {status} ({elapsed}ms)")
        if status != 200:
            print(f"         Response: {body[:300]}")


# ---------------------------------------------------------------------------
# Bulk zaak creation
# ---------------------------------------------------------------------------


def create_zaken(n: int, token_manager: zac_client.TokenManager, zac_url: str, concurrency: int) -> list[dict]:
    """Create n zaken, distributed round-robin across all 7 zaaktypes."""
    print(f"\n=== Creating {n} zaken (concurrency={concurrency}) ===")
    results = []
    completed = 0

    def _task(i: int) -> dict:
        zaaktype_uuid = zac_testdata.ALL_ZAAKTYPE_UUIDS[i % len(zac_testdata.ALL_ZAAKTYPE_UUIDS)]
        return zac_testdata.create_zaak(
            i + 1,
            zaaktype_uuid,
            token_manager,
            zac_url,
            omschrijving_prefix=ZAAK_OMSCHRIJVING_PREFIX,
            toelichting=ZAAK_TOELICHTING,
        )

    with ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = {executor.submit(_task, i): i for i in range(n)}
        for future in as_completed(futures):
            result = future.result()
            results.append(result)
            completed += 1
            if completed % 10 == 0 or completed == n:
                successes = sum(1 for r in results if r["success"])
                print(f"  Progress: {completed}/{n} (success: {successes})")
            if not result["success"]:
                print(
                    f"  ERROR zaak {result['index']} (zaaktype {result['zaaktype_uuid'][:8]}...): "
                    f"HTTP {result['status_code']} — {result['error']}"
                )

    return results


# ---------------------------------------------------------------------------
# Bulk document upload
# ---------------------------------------------------------------------------


def upload_documents_to_zaken(
    zaak_results: list[dict], token_manager: zac_client.TokenManager, zac_url: str, concurrency: int
) -> list[dict]:
    """Upload all test documents to every successfully created zaak."""
    successful = [r for r in zaak_results if r["success"] and r["zaak_uuid"]]
    tasks = [(r["zaak_uuid"], r["zaaktype_uuid"], doc) for r in successful for doc in zac_testdata.TEST_DOCUMENTS]
    total = len(tasks)
    print(f"\n=== Uploading documents ({len(zac_testdata.TEST_DOCUMENTS)} per zaak) to {len(successful)} zaken"
          f" ({total} total, concurrency={concurrency}) ===")

    doc_results: list[dict] = []
    completed = 0

    def _task(zaak_uuid: str, zaaktype_uuid: str, doc: dict) -> dict:
        return zac_testdata.upload_document_to_zaak(zaak_uuid, zaaktype_uuid, doc, token_manager, zac_url)

    with ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = {executor.submit(_task, zaak_uuid, zaaktype_uuid, doc): (zaak_uuid, doc) for zaak_uuid, zaaktype_uuid, doc in tasks}
        for future in as_completed(futures):
            result = future.result()
            doc_results.append(result)
            completed += 1
            if completed % 10 == 0 or completed == total:
                successes = sum(1 for r in doc_results if r["success"])
                print(f"  Progress: {completed}/{total} (success: {successes})")
            if not result["success"]:
                print(
                    f"  ERROR doc '{result['filename']}' zaak {result['zaak_uuid'][:8]}...: "
                    f"HTTP {result['status_code']} — {result['error']}"
                )

    return doc_results


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------


def main() -> None:
    parser = argparse.ArgumentParser(
        description="ZAC local load & performance test script. "
        "Uploads a BPMN process definition, creates zaakafhandelparameters for all 7 zaaktypes, "
        "then creates N zaken."
    )
    parser.add_argument(
        "zaken_count",
        type=int,
        help="Number of zaken to create",
    )
    parser.add_argument(
        "--skip-config",
        action="store_true",
        help="Skip BPMN upload and zaakafhandelparameters creation (use when already configured)",
    )
    parser.add_argument(
        "--concurrency",
        type=int,
        default=1,
        metavar="N",
        help="Number of parallel threads for zaak creation (default: 1)",
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
    parser.add_argument(
        "--add-documents",
        action="store_true",
        help="Upload a .docx and a .pdf document to each successfully created zaak",
    )
    args = parser.parse_args()

    if args.zaken_count < 1:
        parser.error("zaken_count must be >= 1")
    if args.concurrency < 1:
        parser.error("--concurrency must be >= 1")

    zac_reporting.print_banner()

    print(f"ZAC load test — {args.zaken_count} zaken, concurrency={args.concurrency}"
          + (" + documents" if args.add_documents else ""))
    print(f"ZAC: {args.zac_url}  Keycloak: {args.keycloak_url}")

    wall_start = time.monotonic()

    if not args.skip_config:
        print(f"\nObtaining config token ({zac_client.CONFIG_USER})...")
        config_token = zac_client.get_token(zac_client.CONFIG_USER, zac_client.CONFIG_PASSWORD, args.keycloak_url)
        upload_bpmn_process_definitions(config_token, args.zac_url)
        create_zaakafhandelparameters(config_token, args.zac_url)
    else:
        print("\nSkipping BPMN upload and zaakafhandelparameters creation (--skip-config)")

    print(f"\nObtaining zaak creation token ({zac_client.ZAAK_USER})...")
    zaak_token_manager = zac_client.TokenManager(zac_client.ZAAK_USER, zac_client.ZAAK_PASSWORD, args.keycloak_url)

    results = create_zaken(args.zaken_count, zaak_token_manager, args.zac_url, args.concurrency)

    if args.add_documents:
        doc_results = upload_documents_to_zaken(results, zaak_token_manager, args.zac_url, args.concurrency)
        zac_reporting.print_document_stats(doc_results)

    zac_reporting.print_stats(results)

    wall_elapsed = time.monotonic() - wall_start
    print(f"Wall-clock time: {wall_elapsed:.1f}s")


if __name__ == "__main__":
    main()
