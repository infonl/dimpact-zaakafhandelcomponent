#!/bin/bash

set -e

#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

help()
{
   echo "(Re)generates the ZAC backend OpenAPI specification and then generates a Bruno collection based on this."
   echo "This script is based on https://gist.github.com/brian-arms/36117243233f7d65105d5a19abe9928c with some modifications to fit our needs."
}

cwd=$(pwd)
cd ../..

# generate the ZAC OpenAPI specification, which is needed for the next step
./gradlew generateOpenApiSpec

cd ${cwd}
# generate a new ZAC API Bruno collection, copying the generic configuration and environments from the previous collection
node update-from-openapi.js ./collections ../../build/generated/openapi/META-INF/openapi/openapi.json
