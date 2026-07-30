#!/bin/sh

#
# SPDX-FileCopyrightText: 2023 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

echo "Stopping Podman Compose environment.."
podman compose --profile "*" --project-name zac down --timeout 20
