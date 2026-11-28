#!/bin/bash

# The number of expected records in the django_migrations table after Open Archiefbeheer has finished with the database
# migration scripts.
# Note that this is expected to change in future versions of Open Archiefbeheer, so this value should be updated accordingly.
DJANGO_MIGRATIONS_TABLE_RECORDS_COUNT=156

echo ">>>> Waiting until Open Archiefbeheer has initialized the database <<<<"
useradd openarchiefbeheer
while true
do
    verifier=$(psql -U openarchiefbeheer -d openarchiefbeheer -t -A -c "select count(*) from django_migrations")
    echo "Migrations found: $verifier"
    if [ $DJANGO_MIGRATIONS_TABLE_RECORDS_COUNT != "$verifier" ]; then
        echo "Open Archiefbeheer not running yet. Sleeping 2 seconds ..."
        sleep 2s
    else
      echo "Open Archiefbeheer is running!"
      break
    fi
done

set -e

echo "Running database setup scripts ..."
for file in /docker-entrypoint-initdb.d/database/*.sql; do
    echo "Running $file ..."
    psql -U openarchiefbeheer openarchiefbeheer -f "$file"
done

echo ">>>> Open Archiefbeheer database was initialized successfully <<<<"
