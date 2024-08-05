#!/bin/bash

echo ">>>> Waiting until OpenKlant has initialized the database <<<<"
useradd openklant
while true
do
    verifier=$(psql -U openklant -d openklant -t -A -c "select count(*) from django_site")
    if [ "1" != "$verifier" ]; then
        echo "OpenKlant not running yet. Sleeping 2 seconds ..."
        sleep 2s
    else
      echo "OpenKlant is running!"
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
