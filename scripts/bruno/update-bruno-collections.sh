#!/bin/bash

set -e

#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

help()
{
   echo "Creates various Bruno collections from their corresponding OpenAPI specifications."
   echo "These OpenAPI specifications need to be available for this script to work."
   echo "A normal ZAC Gradle build will generate the ZAC OpenAPI specification."
   echo "OpenAPI specs for other collections are available in the ZAC codebase directly."
   echo "This script required the Bruno CLI to be installed and available in the system PATH."
}

# Generate ZAC API Bruno collection
bru import openapi \
  --source ../../build/generated/openapi/META-INF/openapi/openapi.yaml \
  --output collections/zac-backend-api \
  --collection-name "ZAC Backend API"
