## Why

ZAC is developed and tested locally and in CI only on a kind (Kubernetes) cluster, with the stack of
`scripts/kind` ([PZ-13059](https://dimpact.atlassian.net/browse/PZ-13059)). The Docker Compose stack
(`docker-compose.yaml`, `start-docker-compose.sh` and `scripts/docker-compose`) is removed. The specs that describe
how Open Formulieren and PABC run in the Docker Compose stack no longer describe anything.

## What Changes

- **BREAKING** for developers: `docker-compose.yaml`, its arm64 override, `start-docker-compose.sh`,
  `stop-docker-compose.sh` and `scripts/docker-compose` are removed. `./start-kind.sh` starts the local stack.
- The Docker Compose specs `open-formulieren-docker-compose` and `pabc-docker-compose` are removed.
- The behaviour of those specs that still holds is specified for the kind stack instead: Open Formulieren starts
  with the `-f` option, preconfigured with the services that it needs, and PABC skips the technical Keycloak realm
  roles when it imports functional roles.

## Capabilities

### New Capabilities

- `open-formulieren-local-stack`: how Open Formulieren runs in the local kind stack.
- `pabc-local-stack`: how PABC is configured in the local kind stack.

### Removed Capabilities

- `open-formulieren-docker-compose`: replaced by `open-formulieren-local-stack`.
- `pabc-docker-compose`: replaced by `pabc-local-stack`.

## Impact

- Developers start the local stack with `./start-kind.sh` (see `docs/development/installKind.md`).
- The `DOCKER_COMPOSE_<NAME>_EMAIL_ADDRESS` variables of `.env` are renamed to `KIND_<NAME>_EMAIL_ADDRESS`.
- No change to ZAC itself.
