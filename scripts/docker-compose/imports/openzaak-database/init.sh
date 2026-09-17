#!/bin/bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Runs once, when the Open Zaak database container starts with an empty data directory, right after Postgres
# has restored the pre-migrated Open Zaak database from 00-restore-migrated-database.sql.
# Inserts the ZAC test data so that Open Zaak starts on a fully provisioned database and has no migrations to apply.

set -euo pipefail

echo ">>>>  Starting Open Zaak data import script  <<<<"

schema_check="$(psql --username openzaak --dbname openzaak --tuples-only --no-align \
  --command "SELECT to_regclass('public.accounts_user')")"
if [ -z "$schema_check" ]; then
  echo "The Open Zaak database schema is missing. Regenerate 00-restore-migrated-database.sql with scripts/docker-compose/regenerate-openzaak-database-dump.sh"
  exit 1
fi

for file in /docker-entrypoint-initdb.d/database/*.sql; do
  echo "Running $file ..."
  psql --username openzaak --dbname openzaak \
    --set ON_ERROR_STOP=1 \
    --set BAG_API_CLIENT_MP_REST_URL="${BAG_API_CLIENT_MP_REST_URL:-}" \
    --set BAG_API_KEY="${BAG_API_KEY:-}" \
    --file "$file"
done

echo ">>>>  Data import script finished <<<<"
