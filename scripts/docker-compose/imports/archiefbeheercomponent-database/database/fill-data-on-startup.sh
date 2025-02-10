#!/bin/bash

# The number of expected records in the django_migrations table after ArchiefBeheerComponent has finished with the database
# migration scripts.
# Note that this is expected to change in future versions of ArchiefBeheerComponent, so this value should be updated accordingly.
DJANGO_MIGRATIONS_TABLE_RECORDS_COUNT=120

echo ">>>> Waiting until ArchiefBeheerComponent has initialized the database <<<<"
useradd openklant
while true
do
    verifier=$(psql -U archiefbeheercomponent -d archiefbeheercomponent -t -A -c "select count(*) from django_migrations")
    echo "Found migrations: $verifier ..."
    if [ $DJANGO_MIGRATIONS_TABLE_RECORDS_COUNT != "$verifier" ]; then
        echo "ArchiefBeheerComponent not running yet. Sleeping 2 seconds ..."
        sleep 2s
    else
      echo "ArchiefBeheerComponent is running!"
      break
    fi
done

set -e

echo "Running database setup scripts ..."
for file in /docker-entrypoint-initdb.d/database/*.sql; do
    echo "Running $file ..."
    psql -U archiefbeheercomponent archiefbeheercomponent -f "$file"
done

echo ">>>> ArchiefBeheerComponent database was initialized successfully <<<<"
