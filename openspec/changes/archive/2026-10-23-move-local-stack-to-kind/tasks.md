## 1. Remove Docker Compose

- [x] 1.1 Remove `docker-compose.yaml`, `docker-compose.arm64-override.yaml`, `start-docker-compose.sh`,
  `stop-docker-compose.sh` and `scripts/docker-compose`; `scripts/kind/seed` is the only copy of the seed data
- [x] 1.2 Port `regenerate-openzaak-database-dump.sh` to `scripts/kind`, so that it regenerates the Open Zaak dump
  of `scripts/kind/seed`
- [x] 1.3 Run the OPA policy tests in CI with the OPA image directly instead of through Docker Compose
- [x] 1.4 Describe the kind stack instead of Docker Compose in the documentation, Renovate and `.env` files

## 2. Specs

- [x] 2.1 Remove the `open-formulieren-docker-compose` and `pabc-docker-compose` specs and add
  `open-formulieren-local-stack` and `pabc-local-stack` for the kind stack
