#!/bin/bash

# The number of expected records in the django_migrations table after Open Notificaties has finished with the database
# migration scripts.
# Note that this is expected to change in future versions of Open Notificaties, so this value should be updated accordingly.
DJANGO_MIGRATIONS_TABLE_RECORDS_COUNT=160

echo ">>>> Waiting until Open Notificaties has initialized the database <<<<"
useradd opennotificaties
while true
do
    verifier=$(psql -U opennotificaties -d opennotificaties -t -A -c "select count(*) from django_migrations")
    if [ $DJANGO_MIGRATIONS_TABLE_RECORDS_COUNT != "$verifier" ]; then
        echo "Open Notificaties not running yet. Sleeping 2 seconds ..."
        sleep 2s
    else
      echo "Open Notificaties is running!"
      break
    fi
done

set -e

echo "Running database setup scripts ..."
for file in /docker-entrypoint-initdb.d/database/*.sql; do
    echo "Running $file ..."
    psql -U opennotificaties opennotificaties -f "$file"
done

echo ">>>> Open Notificaties database was initialized successfully <<<<"
