#!/bin/bash

set -e

#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

python /app/src/manage.py loaddata demodata
echo "from django.contrib.auth import get_user_model; User = get_user_model(); User.objects.create_superuser('admin', 'admin@example.org', 'admin')" | python /app/src/manage.py shell
