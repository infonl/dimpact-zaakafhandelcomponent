#!/bin/bash

# The number of expected records in the database migrations table after PABC has finished with the database migration scripts.
# Note that this is expected to change in future versions of PABC, so this value should be updated accordingly.
DATABASE_MIGRATIONS_TABLE_RECORDS_COUNT=3

echo ">>>> Waiting until PABC has initialized the database <<<<"
useradd openklant
while true
do
    verifier=$(psql -U pabc -d Pabc -t -A -c "select count(*) from __EFMigrationsHistory")
    if [ DATABASE_MIGRATIONS_TABLE_RECORDS_COUNT != "$verifier" ]; then
        echo "PABC not running yet. Sleeping 2 seconds ..."
        sleep 2s
    else
      echo "PABC is running!"
      break
    fi
done

set -e

set -e

echo "Running database setup scripts ..."
for file in /docker-entrypoint-initdb.d/database/*.sql; do
    echo "Running $file ..."
    psql -U pabc Pabc -f "$file"
done

echo ">>>> Open Klant database was initialized successfully <<<<"
