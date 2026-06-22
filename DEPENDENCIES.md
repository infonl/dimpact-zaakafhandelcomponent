# Dependencies

This document lists the Docker images and versions that the corresponding version of the ZAC application in this repository has been tested against.

## Core Dependencies

- **postgres**: 17.10
- **keycloak**: 26.5.7
- **postgis**: 17-3.4
- **redis**: 8.6.4
- **elasticsearch**: 8.17.4
- **openpolicyagent/opa**: 1.17.1-static
- **brp-api/personen-mock**: 2.7.0-202606151541
- **gotenberg**: 8.34.0
- **otel/opentelemetry-collector-contrib**: 0.154.0
- **grafana/tempo**: 3.0.2
- **prom/prometheus**: v3.12.0
- **grafana/grafana**: 13.0.2
- **greenmail/standalone**: 2.1.9
- **rabbitmq**: 4.2.7-alpine
- **nginxinc/nginx-unprivileged**: 1.31.2

## Common Ground components

- **open-zaak**: 1.27.2
- **objects-api**: 3.3.1
- **open-klant**: 2.15.0
- **open-forms**: 3.4.10
- **open-notificaties**: 1.15.0
- **open-archiefbeheer**: 1.1.1
- **pabc-migrations**: 1.1.0
- **pabc-api**: 1.1.0

## Update Process

This file is automatically kept in sync with the main [Docker Compose](docker-compose.yaml) file by Renovate. 
When a Docker image version is updated in the main Docker Compose file, Renovate will automatically update this file in the same PR.
