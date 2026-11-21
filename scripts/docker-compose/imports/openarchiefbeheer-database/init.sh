#!/bin/bash

echo ">>>>  Starting Open Archiefbeheer data import script  <<<<"

sh /docker-entrypoint-initdb.d/database/fill-data-on-startup.sh  &
