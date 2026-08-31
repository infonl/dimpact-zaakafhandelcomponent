# Dependencies

This document lists the Docker images and versions that the corresponding version of the ZAC application in this repository has been tested against.

## Core Dependencies

- **postgres**: 17.11
- **keycloak**: 26.7.2
- **postgis**: 17-3.4
- **redis**: 8.6.6
- **solr**: 9.10.1-slim
- **openpolicyagent/opa**: 1.20.0-static
- **brp-api/personen-mock**: 2.7.0-202606291131
- **gotenberg**: 8.36.0
- **otel/opentelemetry-collector-contrib**: 0.159.0
- **grafana/tempo**: 3.0.3
- **prom/prometheus**: v3.14.0
- **grafana/grafana**: 13.2.0
- **greenmail/standalone**: 2.1.13
- **nginxinc/nginx-unprivileged**: 1.31.4

## Common Ground components

- **open-zaak**: 1.29.3
- **open-object**: 4.1.1
- **open-klant**: 2.15.0
- **open-forms**: 3.5.7
- **open-notificaties**: 1.16.2
- **open-archiefbeheer**: 2.0.0
- **pabc-migrations**: 1.1.1
- **pabc-api**: 1.1.1

## Update Process

This file is automatically kept in sync with the main [Docker Compose](docker-compose.yaml) file by Renovate. 
When a Docker image version is updated in the main Docker Compose file, Renovate will automatically update this file in the same PR.
