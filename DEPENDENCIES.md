# Dependencies

This document lists the Docker images and versions that the corresponding version of the ZAC application in this repository has been tested against.

## Core Dependencies

- **postgres**: 17.11
- **keycloak**: 26.8.2
- **postgis**: 17-3.4
- **redis**: 8.6.8
- **solr**: 9.11.0-slim
- **openpolicyagent/opa**: 1.22.2-static
- **brp-api/personen-mock**: 2.7.0-202610201628
- **gotenberg**: 8.40.0
- **greenmail/standalone**: 2.1.15
- **nginxinc/nginx-unprivileged**: 1.31.7

## Common Ground components

- **open-zaak**: 1.31.1
- **open-object**: 4.1.1
- **open-klant**: 2.18.1
- **open-forms**: 4.0.7
- **open-notificaties**: 1.17.0
- **open-archiefbeheer**: 2.0.0
- **pabc-migrations**: 2.1.0
- **pabc-api**: 2.1.0

## Update Process

Renovate keeps this file in sync with the images of the local [kind stack](scripts/kind/charts/zac-dev/values.yaml).
When it updates a Docker image version there, it updates this file in the same PR.
