#!/bin/bash

#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

# Starts the Docker Compose stack that the integration tests run against. Booting Keycloak, Open Zaak and
# the mocks takes longer than building the ZAC Docker image and starting Gradle, and depends on neither, so
# CI runs this script in the background and runs the integration tests with 'DO_NOT_START_DOCKER_COMPOSE'.
# ZAC is started last because it needs the freshly built image and the JaCoCo agent that records the
# integration test code coverage, both of which are produced while the rest of the stack is already booting.

set -e

: "${ZAC_DOCKER_IMAGE:?set this to the ZAC Docker image the integration tests run against}"

jacocoAgentJar="build/jacoco/itest/jacoco-agent/org.jacoco.agent-runtime.jar"
compose=(docker compose --profile zac --profile itest --env-file src/itest/docker-compose-itest.env)

servicesWithoutZac=()
while IFS= read -r service; do
  servicesWithoutZac+=("$service")
done < <("${compose[@]}" config --services | grep --invert-match --line-regexp zac)

echo "Starting the Docker Compose stack without ZAC"
"${compose[@]}" up --detach --quiet-pull "${servicesWithoutZac[@]}"

echo "Waiting for the ZAC Docker image '${ZAC_DOCKER_IMAGE}' and for '${jacocoAgentJar}'"
until docker image inspect "${ZAC_DOCKER_IMAGE}" > /dev/null 2>&1 && [ -f "${jacocoAgentJar}" ]; do
  sleep 2
done

echo "Starting ZAC"
"${compose[@]}" up --detach zac
