#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Creates a batch of zaaktypes directly in the local Open Zaak PostgreSQL database, by
# rendering open-zaak/zaaktype-template.sql once per zaaktype and executing the result.
# For each zaaktype it also registers a matching entity type in the local PABC PostgreSQL
# database, by rendering pabc/add-zaaktype-template.sql and executing the result.
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
# Prerequisites: Python 3.10+, `psql` on PATH, the Open Zaak and PABC Docker Compose
# services running.
#
# Usage:
#   ./scripts/test-data/create-zaaktypes.py [--count N]
#                                            [--host HOST] [--port PORT]
#                                            [--dbname DBNAME] [--user USER] [--password PASSWORD]
#                                            [--template PATH]
#                                            [--pabc-host HOST] [--pabc-port PORT]
#                                            [--pabc-dbname DBNAME] [--pabc-user USER]
#                                            [--pabc-password PASSWORD] [--pabc-template PATH]
#
# Examples:
#   ./scripts/test-data/create-zaaktypes.py
#   ./scripts/test-data/create-zaaktypes.py --count 25
#   ./scripts/test-data/create-zaaktypes.py --host localhost --port 54322 --dbname openzaak
#   ./scripts/test-data/create-zaaktypes.py --pabc-host localhost --pabc-port 54329 --pabc-dbname Pabc

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


def render_sql(template: str, zaaktype_number: int, uuid_placeholders: list[str]) -> str:
    """Substitute all placeholders in the template for a single zaaktype."""
    rendered = template.replace("[[ZAAKTYPE_NUMBER]]", str(zaaktype_number))
    for placeholder in uuid_placeholders:
        rendered = rendered.replace(f"[[{placeholder}]]", str(uuid.uuid4()))
    return rendered


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


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Create test zaaktypes directly in the Open Zaak database.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "--count", type=int, default=DEFAULT_ZAAKTYPE_COUNT, help="number of zaaktypes to create (default: 10)"
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
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    template = args.template.read_text()
    pabc_template = args.pabc_template.read_text()

    for zaaktype_number in range(1, args.count + 1):
        sql = render_sql(template, zaaktype_number, UUID_PLACEHOLDERS)
        print(f"Creating zaaktype {zaaktype_number}/{args.count} in Open Zaak...")
        try:
            run_sql(sql, args.host, args.port, args.dbname, args.user, args.password)
        except subprocess.CalledProcessError as called_process_error:
            print(f"ERROR: failed to create zaaktype {zaaktype_number} in Open Zaak: {called_process_error}")
            sys.exit(1)

        pabc_sql = render_sql(pabc_template, zaaktype_number, PABC_UUID_PLACEHOLDERS)
        print(f"Creating zaaktype {zaaktype_number}/{args.count} in PABC...")
        try:
            run_sql(pabc_sql, args.pabc_host, args.pabc_port, args.pabc_dbname, args.pabc_user, args.pabc_password)
        except subprocess.CalledProcessError as called_process_error:
            print(f"ERROR: failed to create zaaktype {zaaktype_number} in PABC: {called_process_error}")
            sys.exit(1)

    print(f"\nCreated {args.count} zaaktype(s).")


if __name__ == "__main__":
    main()
