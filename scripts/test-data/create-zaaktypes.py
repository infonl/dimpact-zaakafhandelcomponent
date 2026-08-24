#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Creates a batch of zaaktypes directly in the local Open Zaak PostgreSQL database, by
# rendering open-zaak/zaaktype-template.sql once per zaaktype and executing the result.
# For each zaaktype it also registers a matching entity type in the local PABC PostgreSQL
# database, by rendering pabc/add-zaaktype-template.sql and executing the result, and then
# configures its zaakafhandelparameters in ZAC by calling configure-zaakafhandelparameters.py.
#
# Every [[XXX_UUID]] placeholder in a template is substituted with a freshly generated
# UUID (unique per zaaktype), and [[ZAAKTYPE_NUMBER]] with a 1-based sequence number, so
# that each rendered script creates one independent zaaktype plus its resultaattypen,
# statustypen, roltypen and zaaktype-informatieobjecttype in Open Zaak, and one matching
# entity type in PABC.
#
# The rendered SQL is piped into the `psql` CLI rather than a Python driver, so this
# script has no dependencies beyond the standard library and a local `psql` executable.
#
# Prerequisites: Python 3.10+, `psql` on PATH, all Docker Compose services (including ZAC)
# running, unless --skip-config is used.
#
# Usage:
#   ./scripts/test-data/create-zaaktypes.py [--count N] [--start-number N]
#                                            [--host HOST] [--port PORT]
#                                            [--dbname DBNAME] [--user USER] [--password PASSWORD]
#                                            [--template PATH]
#                                            [--pabc-host HOST] [--pabc-port PORT]
#                                            [--pabc-dbname DBNAME] [--pabc-user USER]
#                                            [--pabc-password PASSWORD] [--pabc-template PATH]
#                                            [--zac-url URL] [--keycloak-url URL] [--skip-config]
#
# Examples:
#   ./scripts/test-data/create-zaaktypes.py
#   ./scripts/test-data/create-zaaktypes.py --count 25
#   ./scripts/test-data/create-zaaktypes.py --count 10 --start-number 11
#   ./scripts/test-data/create-zaaktypes.py --host localhost --port 54322 --dbname openzaak
#   ./scripts/test-data/create-zaaktypes.py --pabc-host localhost --pabc-port 54329 --pabc-dbname Pabc
#   ./scripts/test-data/create-zaaktypes.py --skip-config

import sys

if sys.version_info < (3, 10):
    print(f"ERROR: Python 3.10+ required, running {sys.version_info.major}.{sys.version_info.minor}")
    sys.exit(1)

import argparse
import os
import pathlib
import subprocess
import uuid

DEFAULT_TEMPLATE_PATH = pathlib.Path(__file__).parent / "open-zaak" / "zaaktype-template.sql"
DEFAULT_PABC_TEMPLATE_PATH = pathlib.Path(__file__).parent / "pabc" / "add-zaaktype-template.sql"
DEFAULT_ZAAKTYPE_COUNT = 10
DEFAULT_ZAAKTYPE_START_NUMBER = 1

# Configures each zaaktype's zaakafhandelparameters in ZAC once it exists in Open Zaak and PABC
CONFIGURE_ZAAKAFHANDELPARAMETERS_SCRIPT = pathlib.Path(__file__).parent / "configure-zaakafhandelparameters.py"

# Placeholders that get one UUID shared by every occurrence within a single rendered zaaktype,
# but a different UUID for every zaaktype in the batch.
UUID_PLACEHOLDERS = [
    "ZAAKTYPE_UUID",
    "RESULTAATTYPE_1_UUID",
    "RESULTAATTYPE_2_UUID",
    "STATUSTYPE_1_UUID",
    "STATUSTYPE_2_UUID",
    "STATUSTYPE_3_UUID",
    "STATUSTYPE_4_UUID",
    "STATUSTYPE_5_UUID",
    "ROLTYPE_1_UUID",
    "ROLTYPE_2_UUID",
    "ROLTYPE_3_UUID",
    "ZAAKTYPEINFORMATIEOBJECTTYPE_1_UUID",
]

# Placeholders in the PABC template that get one UUID shared by every occurrence within a
# single rendered entity type, but a different UUID for every zaaktype in the batch.
PABC_UUID_PLACEHOLDERS = [
    "ENTITY_TYPE_UUID",
]


def render_sql(template: str, zaaktype_number: int, uuid_placeholders: list[str]) -> tuple[str, dict[str, str]]:
    """Substitute all placeholders in the template for a single zaaktype.

    Returns the rendered SQL together with the placeholder -> generated UUID mapping, so
    callers can reuse a generated UUID (e.g. ZAAKTYPE_UUID) after rendering.
    """
    placeholder_uuids = {placeholder: str(uuid.uuid4()) for placeholder in uuid_placeholders}
    rendered = template.replace("[[ZAAKTYPE_NUMBER]]", str(zaaktype_number))
    for placeholder, placeholder_uuid in placeholder_uuids.items():
        rendered = rendered.replace(f"[[{placeholder}]]", placeholder_uuid)
    return rendered, placeholder_uuids


def run_sql(sql: str, host: str, port: int, dbname: str, user: str, password: str) -> None:
    """Execute a SQL script against the Open Zaak database via the psql CLI."""
    command = [
        "psql",
        "--host",
        host,
        "--port",
        str(port),
        "--dbname",
        dbname,
        "--username",
        user,
        "--variable",
        "ON_ERROR_STOP=1",
        "--quiet",
    ]
    environment = {**os.environ, "PGPASSWORD": password}
    subprocess.run(command, input=sql, text=True, env=environment, check=True)


def configure_zaakafhandelparameters(zaaktype_uuid: str, zac_url: str, keycloak_url: str) -> None:
    """Configure a single zaaktype's zaakafhandelparameters in ZAC by calling
    configure-zaakafhandelparameters.py as a subprocess.
    """
    command = [
        sys.executable,
        str(CONFIGURE_ZAAKAFHANDELPARAMETERS_SCRIPT),
        zaaktype_uuid,
        "--zac-url",
        zac_url,
        "--keycloak-url",
        keycloak_url,
    ]
    subprocess.run(command, check=True)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Create test zaaktypes directly in the Open Zaak database.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "--count", type=int, default=DEFAULT_ZAAKTYPE_COUNT, help="number of zaaktypes to create (default: 10)"
    )
    parser.add_argument(
        "--start-number",
        type=int,
        default=DEFAULT_ZAAKTYPE_START_NUMBER,
        help=f"1-based ZAAKTYPE_NUMBER to start numbering from (default: {DEFAULT_ZAAKTYPE_START_NUMBER})",
    )
    parser.add_argument("--host", default="localhost", help="database host (default: localhost)")
    parser.add_argument("--port", type=int, default=54322, help="database port (default: 54322)")
    parser.add_argument("--dbname", default="openzaak", help="database name (default: openzaak)")
    parser.add_argument("--user", default="openzaak", help="database user (default: openzaak)")
    parser.add_argument("--password", default="openzaak", help="database password (default: openzaak)")
    parser.add_argument(
        "--template",
        type=pathlib.Path,
        default=DEFAULT_TEMPLATE_PATH,
        help=f"path to the SQL template (default: {DEFAULT_TEMPLATE_PATH})",
    )
    parser.add_argument("--pabc-host", default="localhost", help="PABC database host (default: localhost)")
    parser.add_argument("--pabc-port", type=int, default=54329, help="PABC database port (default: 54329)")
    parser.add_argument("--pabc-dbname", default="Pabc", help="PABC database name (default: Pabc)")
    parser.add_argument("--pabc-user", default="pabc", help="PABC database user (default: pabc)")
    parser.add_argument("--pabc-password", default="pabc", help="PABC database password (default: pabc)")
    parser.add_argument(
        "--pabc-template",
        type=pathlib.Path,
        default=DEFAULT_PABC_TEMPLATE_PATH,
        help=f"path to the PABC SQL template (default: {DEFAULT_PABC_TEMPLATE_PATH})",
    )
    parser.add_argument(
        "--zac-url", default="http://localhost:8080", help="ZAC base URL (default: http://localhost:8080)"
    )
    parser.add_argument(
        "--keycloak-url",
        default="http://localhost:8081",
        help="Keycloak base URL (default: http://localhost:8081)",
    )
    parser.add_argument(
        "--skip-config",
        action="store_true",
        help="skip configuring zaakafhandelparameters in ZAC after creating each zaaktype",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    template = args.template.read_text()
    pabc_template = args.pabc_template.read_text()

    for batch_index, zaaktype_number in enumerate(
        range(args.start_number, args.start_number + args.count), start=1
    ):
        sql, placeholder_uuids = render_sql(template, zaaktype_number, UUID_PLACEHOLDERS)
        print(f"Creating zaaktype {zaaktype_number} ({batch_index}/{args.count}) in Open Zaak...")
        try:
            run_sql(sql, args.host, args.port, args.dbname, args.user, args.password)
        except subprocess.CalledProcessError as called_process_error:
            print(f"ERROR: failed to create zaaktype {zaaktype_number} in Open Zaak: {called_process_error}")
            sys.exit(1)

        pabc_sql, _ = render_sql(pabc_template, zaaktype_number, PABC_UUID_PLACEHOLDERS)
        print(f"Creating zaaktype {zaaktype_number} ({batch_index}/{args.count}) in PABC...")
        try:
            run_sql(pabc_sql, args.pabc_host, args.pabc_port, args.pabc_dbname, args.pabc_user, args.pabc_password)
        except subprocess.CalledProcessError as called_process_error:
            print(f"ERROR: failed to create zaaktype {zaaktype_number} in PABC: {called_process_error}")
            sys.exit(1)

        if args.skip_config:
            continue
        zaaktype_uuid = placeholder_uuids["ZAAKTYPE_UUID"]
        print(
            f"Configuring zaakafhandelparameters for zaaktype {zaaktype_number} ({batch_index}/{args.count})"
            f" in ZAC ({zaaktype_uuid})..."
        )
        try:
            configure_zaakafhandelparameters(zaaktype_uuid, args.zac_url, args.keycloak_url)
        except subprocess.CalledProcessError as called_process_error:
            print(
                f"ERROR: failed to configure zaakafhandelparameters for zaaktype {zaaktype_number}"
                f" ({zaaktype_uuid}): {called_process_error}"
            )
            sys.exit(1)

    print(f"\nCreated {args.count} zaaktype(s), numbered {args.start_number}-{args.start_number + args.count - 1}.")


if __name__ == "__main__":
    main()
