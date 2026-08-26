#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Configures the zaakafhandelparameters of a single zaaktype in ZAC: sets the default group,
# switches the zaaktype to the CMMN case type "generiek-zaakafhandelmodel", sets the
# "Geweigerd" resultaattype as the zaakNietOntvankelijkResultaattype, sets the standard
# GEMEENTE/MEDEWERKER zaakAfzenders, and sets humanTaskParameters and
# userEventListenerParameters for every human task and user event listener of that case
# definition.
#
# The zaaktype's current zaakafhandelparameters are read first and only those fields are
# changed; every other field (including numeric DB ids of already configured nested
# entities) is sent back unchanged, so this is safe to run against both a not-yet-configured
# and an already-configured zaaktype.
#
# HTTP/auth lives in zac_client.py; this script contains only the zaakafhandelparameters
# domain logic.
#
# Prerequisites: Python 3.10+, all Docker Compose services (including ZAC) must be running.
#
# Usage:
#   ./scripts/test-data/configure-zaakafhandelparameters.py ZAAKTYPE_UUID
#                                                            [--zac-url URL] [--keycloak-url URL]
#
# Examples:
#   ./scripts/test-data/configure-zaakafhandelparameters.py 8f24ad2f-ef2d-47fc-b2d9-7325d4922d9a

import sys

if sys.version_info < (3, 10):
    print(f"ERROR: Python 3.10+ required, running {sys.version_info.major}.{sys.version_info.minor}")
    sys.exit(1)

import argparse
import json
import uuid

import zac_client
import zac_testdata

# CMMN case definition every configured zaaktype is switched to
CASE_DEFINITION_KEY = "generiek-zaakafhandelmodel"

# naam of the resultaattype used as zaakNietOntvankelijkResultaattype
NIET_ONTVANKELIJK_RESULTAATTYPE_NAAM = "Geweigerd"

# zaakAfzenders every configured zaaktype is given; "index" is a frontend-only display
# field, not part of RestZaakAfzender, so it is not sent here.
ZAAK_AFZENDERS = [
    {"defaultMail": True, "mail": "GEMEENTE", "speciaal": True, "replyTo": "GEMEENTE"},
    {"defaultMail": False, "mail": "MEDEWERKER", "speciaal": True, "replyTo": None},
]

# Referentietabellen linked to specific human tasks of the "generiek-zaakafhandelmodel" case
# definition, keyed by the task's planItemDefinition id. Only the table's numeric id ends up
# being used on write (RestHumanTaskReferenceTableConverter re-reads the table from the
# database by id), so the rest of a table's fields are looked up but not sent.
HUMAN_TASK_REFERENTIETABELLEN = {
    "ADVIES_INTERN": ["ADVIES"],
}


def _merge_zaak_afzender_ids(desired_afzenders: list[dict], existing_afzenders: list[dict]) -> list[dict]:
    """Carry over the numeric DB id of each existing zaakAfzender, matched by `mail`.

    ZAC's PUT endpoint uses JPA merge semantics: an afzender that already has a row in the
    database must carry its numeric `id`, otherwise Hibernate treats it as a detached entity
    and raises a 500 error.
    """
    existing_by_mail = {
        afzender["mail"]: afzender for afzender in existing_afzenders if "mail" in afzender
    }
    merged_afzenders = []
    for afzender in desired_afzenders:
        afzender = {**afzender}
        existing_afzender = existing_by_mail.get(afzender.get("mail"))
        if existing_afzender and "id" in existing_afzender:
            afzender["id"] = existing_afzender["id"]
        merged_afzenders.append(afzender)
    return merged_afzenders


def _read_reference_table_id(
    code: str, token_manager: zac_client.TokenManager, zac_url: str
) -> tuple[int | None, str | None]:
    """Look up a referentietabel's numeric id by its code. Returns (id, error_message)."""
    status, response_body = zac_client.http_request(
        "GET",
        f"{zac_url}/rest/referentietabellen/code/{code}",
        headers=zac_client.auth_headers(token_manager.get_token()),
    )
    if status != 200:
        return None, f"Failed to read referentietabel '{code}' (HTTP {status}): {response_body[:200]}"
    return json.loads(response_body)["id"], None


def _build_human_task_parameters(
    case_definition: dict, token_manager: zac_client.TokenManager, zac_url: str
) -> tuple[list[dict], str | None]:
    """Build the desired humanTaskParameters, one entry per human task of `case_definition`.

    Returns (human_task_parameters, error_message).
    """
    reference_table_ids: dict[str, int] = {}
    for codes in HUMAN_TASK_REFERENTIETABELLEN.values():
        for code in codes:
            if code not in reference_table_ids:
                reference_table_id, error = _read_reference_table_id(code, token_manager, zac_url)
                if error:
                    return [], error
                reference_table_ids[code] = reference_table_id

    human_task_parameters = []
    for human_task_definition in case_definition["humanTaskDefinitions"]:
        human_task_parameters.append(
            {
                "planItemDefinition": human_task_definition,
                "defaultGroepId": None,
                "formulierDefinitieId": human_task_definition["defaultFormulierDefinitie"],
                "referentieTabellen": [
                    {"veld": code, "tabel": {"id": reference_table_ids[code]}}
                    for code in HUMAN_TASK_REFERENTIETABELLEN.get(human_task_definition["id"], [])
                ],
                "actief": True,
                "doorlooptijd": None,
            }
        )
    return human_task_parameters, None


def _merge_human_task_parameter_ids(desired_parameters: list[dict], existing_parameters: list[dict]) -> list[dict]:
    """Carry over the numeric DB ids of each existing humanTaskParameter and its nested
    referentieTabellen, matched by `planItemDefinition.id` and `veld` respectively.

    ZAC's PUT endpoint uses JPA merge semantics: an entity that already has a row in the
    database must carry its numeric `id`, otherwise Hibernate treats it as a detached entity
    and raises a 500 error.
    """
    existing_by_definition_id = {
        parameter["planItemDefinition"]["id"]: parameter
        for parameter in existing_parameters
        if "planItemDefinition" in parameter
    }
    merged_parameters = []
    for parameter in desired_parameters:
        parameter = {**parameter}
        existing_parameter = existing_by_definition_id.get(parameter["planItemDefinition"]["id"])
        if existing_parameter is None:
            merged_parameters.append(parameter)
            continue
        if "id" in existing_parameter:
            parameter["id"] = existing_parameter["id"]
        existing_reference_tables_by_veld = {
            reference_table["veld"]: reference_table
            for reference_table in existing_parameter.get("referentieTabellen", [])
        }
        merged_reference_tables = []
        for reference_table in parameter["referentieTabellen"]:
            reference_table = {**reference_table}
            existing_reference_table = existing_reference_tables_by_veld.get(reference_table["veld"])
            if existing_reference_table and "id" in existing_reference_table:
                reference_table["id"] = existing_reference_table["id"]
            merged_reference_tables.append(reference_table)
        parameter["referentieTabellen"] = merged_reference_tables
        merged_parameters.append(parameter)
    return merged_parameters


def _build_user_event_listener_parameters(case_definition: dict, existing_parameters: list[dict]) -> list[dict]:
    """Build the desired userEventListenerParameters, one entry per user event listener of
    `case_definition`, carrying over each existing entry's `toelichting` so a value set
    through the ZAC UI is not cleared by this script.
    """
    existing_by_id = {
        parameter["id"]: parameter for parameter in existing_parameters if "id" in parameter
    }
    user_event_listener_parameters = []
    for user_event_listener_definition in case_definition["userEventListenerDefinitions"]:
        existing_parameter = existing_by_id.get(user_event_listener_definition["id"])
        user_event_listener_parameters.append(
            {
                "id": user_event_listener_definition["id"],
                "naam": user_event_listener_definition["naam"],
                "toelichting": existing_parameter.get("toelichting") if existing_parameter else None,
            }
        )
    return user_event_listener_parameters


def configure_zaakafhandelparameters(
    zaaktype_uuid: uuid.UUID, token_manager: zac_client.TokenManager, zac_url: str
) -> dict:
    """Set the default group, CMMN case type, zaakNietOntvankelijkResultaattype,
    zaakAfzenders, humanTaskParameters and userEventListenerParameters on a zaaktype's
    zaakafhandelparameters.

    Returns a result dict with the outcome; raises nothing, so the caller decides how to
    report a failure.
    """
    status, response_body = zac_client.http_request(
        "GET",
        f"{zac_url}/rest/zaakafhandelparameters/{zaaktype_uuid}",
        headers=zac_client.auth_headers(token_manager.get_token()),
    )
    if status != 200:
        return {
            "success": False,
            "step": "read zaakafhandelparameters",
            "status_code": status,
            "error": response_body[:300],
        }
    configuration = json.loads(response_body)

    status, response_body = zac_client.http_request(
        "GET",
        f"{zac_url}/rest/zaakafhandelparameters/case-definitions/{CASE_DEFINITION_KEY}",
        headers=zac_client.auth_headers(token_manager.get_token()),
    )
    if status != 200:
        return {
            "success": False,
            "step": "read case definition",
            "status_code": status,
            "error": response_body[:300],
        }
    case_definition = json.loads(response_body)

    human_task_parameters, error = _build_human_task_parameters(case_definition, token_manager, zac_url)
    if error:
        return {
            "success": False,
            "step": "read referentietabellen",
            "status_code": None,
            "error": error,
        }

    status, response_body = zac_client.http_request(
        "GET",
        f"{zac_url}/rest/zaakafhandelparameters/resultaattypes/{zaaktype_uuid}",
        headers=zac_client.auth_headers(token_manager.get_token()),
    )
    if status != 200:
        return {
            "success": False,
            "step": "read resultaattypes",
            "status_code": status,
            "error": response_body[:300],
        }
    resultaattypes = json.loads(response_body)
    niet_ontvankelijk_resultaattype = next(
        (
            resultaattype
            for resultaattype in resultaattypes
            if resultaattype.get("naam", "").lower() == NIET_ONTVANKELIJK_RESULTAATTYPE_NAAM.lower()
        ),
        None,
    )
    if niet_ontvankelijk_resultaattype is None:
        return {
            "success": False,
            "step": "read resultaattypes",
            "status_code": status,
            "error": f"no resultaattype named '{NIET_ONTVANKELIJK_RESULTAATTYPE_NAAM}' found for this zaaktype",
        }

    configuration["defaultGroepId"] = zac_testdata.ZAAK_GROUP_ID
    configuration["caseDefinition"] = case_definition
    configuration["zaakNietOntvankelijkResultaattype"] = niet_ontvankelijk_resultaattype
    configuration["zaakAfzenders"] = _merge_zaak_afzender_ids(
        ZAAK_AFZENDERS, configuration.get("zaakAfzenders", [])
    )
    configuration["humanTaskParameters"] = _merge_human_task_parameter_ids(
        human_task_parameters, configuration.get("humanTaskParameters", [])
    )
    configuration["userEventListenerParameters"] = _build_user_event_listener_parameters(
        case_definition, configuration.get("userEventListenerParameters", [])
    )

    status, response_body = zac_client.http_request(
        "PUT",
        f"{zac_url}/rest/zaakafhandelparameters",
        body=configuration,
        headers=zac_client.auth_headers(token_manager.get_token()),
    )
    if status != 200:
        return {
            "success": False,
            "step": "update zaakafhandelparameters",
            "status_code": status,
            "error": response_body[:300],
        }

    return {"success": True}


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Set the default group, CMMN case type, zaakNietOntvankelijkResultaattype, "
        "zaakAfzenders, humanTaskParameters and userEventListenerParameters on a zaaktype's "
        "zaakafhandelparameters."
    )
    parser.add_argument(
        "zaaktype_uuid",
        type=uuid.UUID,
        metavar="ZAAKTYPE_UUID",
        help="UUID of the zaaktype to configure",
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

    print(f"ZAC: {args.zac_url}  Keycloak: {args.keycloak_url}")
    print(f"Configuring zaakafhandelparameters for zaaktype {args.zaaktype_uuid}...")

    token_manager = zac_client.TokenManager(
        zac_client.CONFIG_USER, zac_client.CONFIG_PASSWORD, args.keycloak_url
    )
    result = configure_zaakafhandelparameters(args.zaaktype_uuid, token_manager, args.zac_url)

    if result["success"]:
        print(
            f"  [OK] defaultGroepId='{zac_testdata.ZAAK_GROUP_ID}',"
            f" caseDefinition='{CASE_DEFINITION_KEY}',"
            f" zaakNietOntvankelijkResultaattype='{NIET_ONTVANKELIJK_RESULTAATTYPE_NAAM}',"
            f" zaakAfzenders={[afzender['mail'] for afzender in ZAAK_AFZENDERS]},"
            f" humanTaskParameters and userEventListenerParameters set for every human task"
            f" and user event listener of '{CASE_DEFINITION_KEY}'"
        )
    else:
        status = f"HTTP {result['status_code']}: " if result["status_code"] is not None else ""
        print(f"  [FAIL] {result['step']}: {status}{result['error']}")
        sys.exit(1)


if __name__ == "__main__":
    main()
