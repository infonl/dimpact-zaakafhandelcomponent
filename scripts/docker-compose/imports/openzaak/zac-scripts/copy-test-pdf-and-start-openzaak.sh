#!/bin/bash

#
# SPDX-FileCopyrightText: 2023 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

echo -e "Copying 'fake-test-document.pdf' ZAC test PDF to OpenZaak uploads folder"

mkdir -p /app/private-media/uploads/2023/10
cp /fake-test-document.pdf /app/private-media/uploads/2023/10/

# note that this script is run as the 'openzaak' user (see the Dockerfile of Open Zaak) so
# we do not need to change any file or directory permissions here

echo -e "Starting Open OpenZaak.."

/start.sh
