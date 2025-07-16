#!/bin/bash

set -e

# Wait here a bit for the Objects API database to be initialized (by Objecten).
# In the end better to add a health check to the Objects API service in Docker Compose but
# that is not trivial without curl and wget.
sleep 10

#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

python /app/src/manage.py loaddata demodata
