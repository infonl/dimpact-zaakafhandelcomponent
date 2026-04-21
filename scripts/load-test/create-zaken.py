#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Local load & performance test script for ZAC.
#
# Uploads a BPMN process definition and form.io task forms to ZAC, creates
# zaakafhandelparameters for all 7 zaaktypes in Open Zaak (3 CMMN, 4 BPMN),
# then creates a user-specified number of zaken distributed across all zaaktypes.
#
# Prerequisites: all Docker Compose services (including ZAC) must be running.
#
# Usage:
#   python load_test.py <zaken_count> [--skip-config] [--concurrency N]
#                       [--zac-url URL] [--keycloak-url URL]
#
# Examples:
#   python load_test.py 10
#   python load_test.py 100 --skip-config --concurrency 4

import argparse
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Any

# ---------------------------------------------------------------------------
# Constants — sourced from src/itest/kotlin/nl/info/zac/itest/config/
# ---------------------------------------------------------------------------

KEYCLOAK_REALM = "zaakafhandelcomponent"
KEYCLOAK_CLIENT_ID = "zaakafhandelcomponent"
KEYCLOAK_CLIENT_SECRET = "keycloakZaakafhandelcomponentClientSecret"

# beheerder1newiam = BEHEERDER_ELK_ZAAKTYPE (PABC flag = true)
CONFIG_USER = "beheerder1newiam"
CONFIG_PASSWORD = "beheerder1newiam"

# Use the same beheerder user for zaak creation: they have access to all zaaktypes
# (behandelaar1newiam is restricted to domein_test_1 only)
ZAAK_USER = "beheerder1newiam"
ZAAK_PASSWORD = "beheerder1newiam"

# Group used when creating zaken (PABC flag = true → new IAM group)
ZAAK_GROUP_ID = "behandelaars-test-1"
ZAAK_GROUP_NAME = "Test group behandelaars domein test 1 - new IAM"

# Niet-ontvankelijk resultaattype shared by all CMMN zaaktypes
CMMN_NIET_ONTVANKELIJK_UUID = "dd2bcd87-ed7e-4b23-a8e3-ea7fe7ef00c6"

# Single BPMN process definition uploaded and shared by all 4 BPMN zaaktypes
LOAD_TEST_PROCESS_KEY = "loadTestProcess"
LOAD_TEST_FORM_KEY = "loadTestForm"

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

ALL_ZAAKTYPE_UUIDS = [z["uuid"] for z in CMMN_ZAAKTYPES] + [z["uuid"] for z in BPMN_ZAAKTYPES]

# ---------------------------------------------------------------------------
# Embedded BPMN process definition and form.io task form
#
# A minimal Flowable BPMN 2.0 process with:
#   - one user task assigned to the zaak's behandelaar / groep
#   - formKey referencing the embedded loadTestForm
# All 4 BPMN zaaktypes in this script share this single process definition.
# ---------------------------------------------------------------------------

LOAD_TEST_BPMN = """\
<?xml version="1.0" encoding="UTF-8"?>
<definitions
  xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:flowable="http://flowable.org/bpmn"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
  typeLanguage="http://www.w3.org/2001/XMLSchema"
  expressionLanguage="http://www.w3.org/1999/XPath"
  targetNamespace="http://flowable.org/loadtest">

  <process id="loadTestProcess" name="Load Test Process" isExecutable="true"
           flowable:candidateStarterGroups="flowableUser">
    <documentation>Simple BPMN process used by the ZAC local load test script.</documentation>

    <startEvent id="start" name="Start" flowable:initiator="initiator"/>

    <userTask id="loadTestTask" name="Load Test Task"
              flowable:assignee="${var:get(zaakBehandelaar)}"
              flowable:candidateGroups="${zaakGroep}"
              flowable:formKey="loadTestForm"
              flowable:formFieldValidation="false">
      <extensionElements>
        <flowable:static-form-key><![CDATA[loadTestForm]]></flowable:static-form-key>
        <flowable:task-candidates-type><![CDATA[all]]></flowable:task-candidates-type>
      </extensionElements>
    </userTask>

    <endEvent id="end" name="End"/>

    <sequenceFlow id="flow1" sourceRef="start" targetRef="loadTestTask"/>
    <sequenceFlow id="flow2" sourceRef="loadTestTask" targetRef="end"/>
  </process>

  <bpmndi:BPMNDiagram id="BPMNDiagram_loadTestProcess">
    <bpmndi:BPMNPlane bpmnElement="loadTestProcess" id="BPMNPlane_loadTestProcess">
      <bpmndi:BPMNShape bpmnElement="start" id="BPMNShape_start">
        <omgdc:Bounds height="30.0" width="30.0" x="100.0" y="135.0"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape bpmnElement="loadTestTask" id="BPMNShape_loadTestTask">
        <omgdc:Bounds height="80.0" width="100.0" x="200.0" y="110.0"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape bpmnElement="end" id="BPMNShape_end">
        <omgdc:Bounds height="28.0" width="28.0" x="370.0" y="136.0"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge bpmnElement="flow1" id="BPMNEdge_flow1">
        <omgdi:waypoint x="130.0" y="150.0"/>
        <omgdi:waypoint x="200.0" y="150.0"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge bpmnElement="flow2" id="BPMNEdge_flow2">
        <omgdi:waypoint x="300.0" y="150.0"/>
        <omgdi:waypoint x="370.0" y="150.0"/>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>
"""

LOAD_TEST_FORM = """\
{
  "display": "form",
  "name": "loadTestForm",
  "title": "Load Test Form",
  "type": "form",
  "components": [
    {
      "title": "Load test task",
      "label": "Load test task",
      "type": "panel",
      "key": "loadTestPanel",
      "input": false,
      "tableView": false,
      "components": [
        {
          "label": "Toelichting",
          "type": "textarea",
          "key": "toelichting",
          "input": true,
          "validate": {
            "required": false
          }
        }
      ]
    },
    {
      "type": "button",
      "label": "Afronden",
      "key": "submit",
      "disableOnInvalid": false,
      "input": true,
      "tableView": false
    }
  ]
}
"""

# ---------------------------------------------------------------------------
# HTTP helpers
# ---------------------------------------------------------------------------


def _http(method: str, url: str, body: Any = None, headers: dict | None = None) -> tuple[int, str]:
    """Perform an HTTP request. Returns (status_code, response_body)."""
    if headers is None:
        headers = {}
    data = None
    if body is not None:
        if isinstance(body, dict) and headers.get("Content-Type") == "application/x-www-form-urlencoded":
            data = urllib.parse.urlencode(body).encode()
        else:
            data = json.dumps(body).encode()
            headers.setdefault("Content-Type", "application/json")
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, resp.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()


def get_token(username: str, password: str, keycloak_url: str) -> str:
    """Obtain a Keycloak Bearer token via Resource Owner Password flow."""
    url = f"{keycloak_url}/realms/{KEYCLOAK_REALM}/protocol/openid-connect/token"
    body = {
        "grant_type": "password",
        "client_id": KEYCLOAK_CLIENT_ID,
        "client_secret": KEYCLOAK_CLIENT_SECRET,
        "username": username,
        "password": password,
    }
    status, response = _http("POST", url, body=body, headers={"Content-Type": "application/x-www-form-urlencoded"})
    if status != 200:
        print(f"ERROR: Keycloak auth failed for '{username}' (HTTP {status}): {response[:200]}")
        sys.exit(1)
    return json.loads(response)["access_token"]


def _auth_headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}


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
    status, body = _http(
        "POST",
        f"{zac_url}/rest/bpmn-process-definitions",
        body={"filename": f"{LOAD_TEST_PROCESS_KEY}.bpmn", "content": LOAD_TEST_BPMN},
        headers=_auth_headers(token),
    )
    elapsed = int((time.monotonic() - t0) * 1000)
    ok = "OK" if status == 201 else "FAIL"
    print(f"  [{ok}] BPMN process '{LOAD_TEST_PROCESS_KEY}': HTTP {status} ({elapsed}ms)")
    if status != 201:
        print(f"         Response: {body[:300]}")

    # Upload form.io task form
    t0 = time.monotonic()
    status, body = _http(
        "POST",
        f"{zac_url}/rest/bpmn-process-definitions/{LOAD_TEST_PROCESS_KEY}/forms",
        body={"filename": f"{LOAD_TEST_FORM_KEY}.json", "content": LOAD_TEST_FORM},
        headers=_auth_headers(token),
    )
    elapsed = int((time.monotonic() - t0) * 1000)
    ok = "OK" if status == 201 else "FAIL"
    print(f"  [{ok}] Form '{LOAD_TEST_FORM_KEY}': HTTP {status} ({elapsed}ms)")
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
        "defaultGroepId": ZAAK_GROUP_ID,
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
        "groepNaam": ZAAK_GROUP_NAME,
        "defaultBehandelaarId": ZAAK_USER,
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

    for zaaktype in CMMN_ZAAKTYPES:
        # GET first: ZAC's PUT uses JPA merge semantics. When a record already exists every
        # nested entity needs its numeric DB id in the request body, otherwise Hibernate raises
        # "detached entity passed to persist". We fetch the existing config and merge the IDs.
        get_status, get_body = _http(
            "GET",
            f"{zac_url}/rest/zaakafhandelparameters/{zaaktype['uuid']}",
            headers=_auth_headers(token),
        )
        desired = _cmmn_body(zaaktype)
        if get_status == 200:
            desired = _merge_existing_ids(desired, json.loads(get_body))

        t0 = time.monotonic()
        status, body = _http(
            "PUT",
            f"{zac_url}/rest/zaakafhandelparameters",
            body=desired,
            headers=_auth_headers(token),
        )
        elapsed = int((time.monotonic() - t0) * 1000)
        ok = "OK" if status == 200 else "FAIL"
        print(f"  [{ok}] CMMN {zaaktype['description']}: HTTP {status} ({elapsed}ms)")
        if status != 200:
            print(f"         Response: {body[:500]}")

    for zaaktype in BPMN_ZAAKTYPES:
        t0 = time.monotonic()
        status, body = _http(
            "POST",
            f"{zac_url}/rest/zaaktype-bpmn-configuration",
            body=_bpmn_body(zaaktype),
            headers=_auth_headers(token),
        )
        elapsed = int((time.monotonic() - t0) * 1000)
        ok = "OK" if status == 200 else "FAIL"
        print(f"  [{ok}] BPMN {zaaktype['description']}: HTTP {status} ({elapsed}ms)")
        if status != 200:
            print(f"         Response: {body[:300]}")


# ---------------------------------------------------------------------------
# Zaak creation
# ---------------------------------------------------------------------------


def create_zaak(index: int, zaaktype_uuid: str, token: str, zac_url: str) -> dict:
    """Create a single zaak. Returns result dict with timing info."""
    body = {
        "zaak": {
            "zaaktype": {"uuid": zaaktype_uuid},
            "startdatum": "2020-01-01T00:00:00+01:00",
            "groep": {"id": ZAAK_GROUP_ID, "naam": ZAAK_GROUP_NAME},
            "communicatiekanaal": "fakeCommunicatiekanaal1",
            "vertrouwelijkheidaanduiding": "OPENBAAR",
            "omschrijving": f"load-test-zaak-{index}",
            "toelichting": "Created by ZAC load test script",
        },
        "bagObjecten": [],
    }
    t0 = time.monotonic()
    status, response_body = _http("POST", f"{zac_url}/rest/zaken/zaak", body=body, headers=_auth_headers(token))
    elapsed = int((time.monotonic() - t0) * 1000)
    zaak_uuid = None
    parse_error = None
    if status == 200:
        try:
            parsed_response = json.loads(response_body)
            zaak_uuid = parsed_response.get("zaakUUID") or parsed_response.get("uuid")
        except json.JSONDecodeError as exc:
            parse_error = f"Failed to parse zaak creation response JSON: {exc}"
    return {
        "index": index,
        "zaaktype_uuid": zaaktype_uuid,
        "success": status == 200,
        "status_code": status,
        "zaak_uuid": zaak_uuid,
        "elapsed_ms": elapsed,
        "error": response_body[:200] if status != 200 else parse_error,
    }


def create_zaken(n: int, token: str, zac_url: str, concurrency: int) -> list[dict]:
    """Create n zaken, distributed round-robin across all 7 zaaktypes."""
    print(f"\n=== Creating {n} zaken (concurrency={concurrency}) ===")
    results = []
    completed = 0

    def _task(i: int) -> dict:
        zaaktype_uuid = ALL_ZAAKTYPE_UUIDS[i % len(ALL_ZAAKTYPE_UUIDS)]
        return create_zaak(i + 1, zaaktype_uuid, token, zac_url)

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
# Stats reporting
# ---------------------------------------------------------------------------


def print_stats(results: list[dict]) -> None:
    """Print a summary table of results grouped by zaaktype."""
    print("\n=== Results ===")

    label_map = {}
    for z in CMMN_ZAAKTYPES:
        label_map[z["uuid"]] = f"CMMN {z['description']}"
    for z in BPMN_ZAAKTYPES:
        label_map[z["uuid"]] = f"BPMN {z['description']}"

    by_zaaktype: dict[str, list[dict]] = {}
    for r in results:
        by_zaaktype.setdefault(r["zaaktype_uuid"], []).append(r)

    total_ok = sum(1 for r in results if r["success"])
    total_fail = len(results) - total_ok

    header = f"{'Zaaktype':<35} {'OK':>5} {'FAIL':>5} {'Mean(ms)':>10} {'Min(ms)':>8} {'Max(ms)':>8}"
    print(header)
    print("-" * len(header))

    for uuid in ALL_ZAAKTYPE_UUIDS:
        rows = by_zaaktype.get(uuid, [])
        if not rows:
            continue
        ok = sum(1 for r in rows if r["success"])
        fail = len(rows) - ok
        times = [r["elapsed_ms"] for r in rows if r["success"]]
        if times:
            mean_ms = int(sum(times) / len(times))
            min_ms = min(times)
            max_ms = max(times)
        else:
            mean_ms = min_ms = max_ms = 0
        label = label_map.get(uuid, uuid[:8])
        print(f"{label:<35} {ok:>5} {fail:>5} {mean_ms:>10} {min_ms:>8} {max_ms:>8}")

    print("-" * len(header))
    all_times = [r["elapsed_ms"] for r in results if r["success"]]
    if all_times:
        overall_mean = int(sum(all_times) / len(all_times))
        overall_min = min(all_times)
        overall_max = max(all_times)
    else:
        overall_mean = overall_min = overall_max = 0
    print(f"{'TOTAL':<35} {total_ok:>5} {total_fail:>5} {overall_mean:>10} {overall_min:>8} {overall_max:>8}")

    overall_elapsed = sum(r["elapsed_ms"] for r in results)
    print(f"\nTotal HTTP time (sum): {overall_elapsed}ms")
    if total_fail > 0:
        print(f"WARNING: {total_fail} zaak(en) failed to create.")
        sys.exit(1)


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
    args = parser.parse_args()

    if args.zaken_count < 1:
        parser.error("zaken_count must be >= 1")
    if args.concurrency < 1:
        parser.error("--concurrency must be >= 1")

    print(f"ZAC load test — {args.zaken_count} zaken, concurrency={args.concurrency}")
    print(f"ZAC: {args.zac_url}  Keycloak: {args.keycloak_url}")

    wall_start = time.monotonic()

    if not args.skip_config:
        print("\nObtaining config token (beheerder1newiam)...")
        config_token = get_token(CONFIG_USER, CONFIG_PASSWORD, args.keycloak_url)
        upload_bpmn_process_definitions(config_token, args.zac_url)
        create_zaakafhandelparameters(config_token, args.zac_url)
    else:
        print("\nSkipping BPMN upload and zaakafhandelparameters creation (--skip-config)")

    print("\nObtaining zaak creation token (beheerder1newiam)...")
    zaak_token = get_token(ZAAK_USER, ZAAK_PASSWORD, args.keycloak_url)

    results = create_zaken(args.zaken_count, zaak_token, args.zac_url, args.concurrency)

    print_stats(results)

    wall_elapsed = time.monotonic() - wall_start
    print(f"Wall-clock time: {wall_elapsed:.1f}s")


if __name__ == "__main__":
    main()
