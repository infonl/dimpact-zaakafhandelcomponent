#!/bin/sh

#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

set -e

 # Wait until the Open Object and its database are actually ready before loading data.
 max_attempts=30
 attempt=1
 until python /app/src/manage.py check >/dev/null 2>&1; do
   if [ "$attempt" -ge "$max_attempts" ]; then
     echo "Open Object did not become ready after $max_attempts attempts." >&2
     exit 1
   fi
   echo "Waiting for Open Object to become ready... (attempt $attempt/$max_attempts)"
   attempt=$((attempt + 1))
   sleep 2
 done

python /app/src/manage.py loaddata demodata
echo "from django.contrib.auth import get_user_model; User = get_user_model(); User.objects.create_superuser('admin', 'admin@example.com', 'admin')" | python /app/src/manage.py shell
