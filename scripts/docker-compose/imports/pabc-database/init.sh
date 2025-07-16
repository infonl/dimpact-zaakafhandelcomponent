#!/bin/bash

echo ">>>>  Starting PABC data import script  <<<<"

sh /docker-entrypoint-initdb.d/database/fill-data-on-startup.sh  &
