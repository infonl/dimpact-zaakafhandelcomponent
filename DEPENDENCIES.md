# Dependencies

This document lists the Docker images and versions that the corresponding version of the ZAC application in this repository has been tested against.

## Core Dependencies

- **postgres**: 17.9
- **keycloak**: 26.5.7
- **postgis**: 17-3.4
- **redis**: 8.4.0
- **solr**: 9.10.1-slim
- **openpolicyagent/opa**: 1.15.1-static
- **brp-api/personen-mock**: 2.7.0-202603230846
- **kontextwork-converter**: 1.8.3
- **otel/opentelemetry-collector-contrib**: 0.149.0
- **grafana/tempo**: 2.10.3
- **prom/prometheus**: v3.11.1
- **grafana/grafana**: 12.4.2
- **greenmail/standalone**: 2.1.8
- **rabbitmq**: 4.2.5-alpine
- **nginxinc/nginx-unprivileged**: 1.29.5

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
