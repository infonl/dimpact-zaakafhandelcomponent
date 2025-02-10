#!/bin/bash

echo ">>>>  Starting ArchiefBeheerComponent data import script  <<<<"

sh /docker-entrypoint-initdb.d/database/fill-data-on-startup.sh  &
