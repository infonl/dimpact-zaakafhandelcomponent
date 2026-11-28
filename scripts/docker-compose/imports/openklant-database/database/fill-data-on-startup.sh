#!/bin/bash

# The number of expected records in the django_migrations table after Open Klant has finished with the database
# migration scripts.
# Note that this is expected to change in future versions of Open Klant, so this value should be updated accordingly.
DJANGO_MIGRATIONS_TABLE_RECORDS_COUNT=138

echo ">>>> Waiting until Open Klant has initialized the database <<<<"
useradd openklant
while true
do
    verifier=$(psql -U openklant -d openklant -t -A -c "select count(*) from django_migrations")
    if [ $DJANGO_MIGRATIONS_TABLE_RECORDS_COUNT != "$verifier" ]; then
        echo "Open Klant not running yet. Sleeping 2 seconds ..."
        sleep 2s
    else
      echo "Open Klant is running!"
      break
    fi
done

set -e

echo "Running database setup scripts ..."
for file in /docker-entrypoint-initdb.d/database/*.sql; do
    echo "Running $file ..."
    psql -U openklant openklant -f "$file"
done

echo ">>>> Open Klant database was initialized successfully <<<<"
