# Dependencies

This document lists the Docker images and versions that this version of the ZAC application has been tested against.

## Core Dependencies

- **postgres**: 17.7
- **keycloak**: 26.3.5
- **postgis**: 17-3.4
- **redis**: 7.4.7
- **solr**: 9.10.0-slim
- **openpolicyagent/opa**: 1.10.1-static
- **brp-api/personen-mock**: 2.7.0-202511050738
- **kontextwork-converter**: 1.8.1
- **otel/opentelemetry-collector**: 0.139.0
- **grafana/tempo**: 2.9.0
- **prom/prometheus**: v3.7.3
- **grafana/grafana**: 12.2.1
- **greenmail/standalone**: 2.1.7
- **rabbitmq**: 4.2.0-alpine
- **nginx**: 1.29.3

## PodiumD dependencies

- **open-zaak**: 1.23.0
- **objects-api**: 3.1.4
- **open-klant**: 2.9.0
- **open-notificaties**: 1.10.0
- **open-archiefbeheer**: 1.1.1
- **pabc-migrations**: 0.0.1-rc.5
- **pabc-api**: 0.0.1-rc.4

## Update Process

This file is automatically kept in sync with the `docker-compose.yaml` file by Renovate. When a Docker image version is updated in the docker-compose.yaml file, Renovate will automatically update this file in the same PR.
