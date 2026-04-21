# Dependencies

This document lists the Docker images and versions that the corresponding version of the ZAC application in this repository has been tested against.

## Core Dependencies

- **postgres**: 17.9
- **keycloak**: 26.5.7
- **postgis**: 17-3.4
- **redis**: 8.4.0
- **solr**: 9.10.1-slim
- **openpolicyagent/opa**: 1.15.2-static
- **brp-api/personen-mock**: 2.7.0-202603230846
- **gotenberg**: 8.31.0
- **otel/opentelemetry-collector-contrib**: 0.150.1
- **grafana/tempo**: 2.10.4
- **prom/prometheus**: v3.11.2
- **grafana/grafana**: 13.0.1
- **greenmail/standalone**: 2.1.8
- **rabbitmq**: 4.2.5-alpine
- **nginxinc/nginx-unprivileged**: 1.30.0

## Common Ground components

- **open-zaak**: 1.26.0
- **objects-api**: 3.3.1
- **open-klant**: 2.14.0
- **open-notificaties**: 1.13.0
- **open-archiefbeheer**: 1.1.1
- **pabc-migrations**: 1.1.0
- **pabc-api**: 1.1.0

## Update Process

This file is automatically kept in sync with the main [Docker Compose](docker-compose.yaml) file by Renovate. 
When a Docker image version is updated in the main Docker Compose file, Renovate will automatically update this file in the same PR.
