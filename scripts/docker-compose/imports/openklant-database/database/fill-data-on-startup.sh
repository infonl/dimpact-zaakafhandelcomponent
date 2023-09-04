#!/bin/bash

echo ">>>>  Waiting until Open Klant has initialized the database <<<<"
useradd openklant
while true
do
    verifier=$(psql -U openklant -d openklant -t -A -c "select count(*) from django_site")
    if [ "1" = $verifier ]
        then
            echo "Running database setup scripts"
            for file in /docker-entrypoint-initdb.d/database/*.sql
            do
                echo "Running $file"
                psql -U openklant openklant -f $file
            done
            break
        else
            echo "Open Klant is not running yet"
            sleep 5
    fi
done
