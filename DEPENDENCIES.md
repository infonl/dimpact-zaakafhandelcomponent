# Dependencies

This document lists the Docker images and versions that the corresponding version of the ZAC application in this repository has been tested against.

## Core Dependencies

- **postgres**: 17.8
- **keycloak**: 26.4.7
- **postgis**: 17-3.4
- **redis**: 8.4.0
- **solr**: 9.10.1-slim
- **openpolicyagent/opa**: 1.13.1-static
- **brp-api/personen-mock**: 2.7.0-202511050738
- **kontextwork-converter**: 1.8.1
- **otel/opentelemetry-collector-contrib**: 0.145.0
- **grafana/tempo**: 2.10.1
- **prom/prometheus**: v3.9.1
- **grafana/grafana**: 12.3.3
- **greenmail/standalone**: 2.1.8
- **rabbitmq**: 4.2.3-alpine
- **nginxinc/nginx-unprivileged**: 1.29.5

## Common Ground components

- **open-zaak**: 1.26.0
- **objects-api**: 3.3.1
- **open-klant**: 2.13.0
- **open-notificaties**: 1.13.0
- **open-archiefbeheer**: 1.1.1
- **pabc-migrations**: 1.0.0
- **pabc-api**: 1.0.0

## Update Process

This file is automatically kept in sync with the main [Docker Compose](docker-compose.yaml) file by Renovate. 
When a Docker image version is updated in the main Docker Compose file, Renovate will automatically update this file in the same PR.
