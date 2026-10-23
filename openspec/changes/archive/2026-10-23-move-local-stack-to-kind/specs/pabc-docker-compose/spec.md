## REMOVED Requirements

### Requirement: Docker Compose runs PABC 2.1.0-prerelease
**Reason**: The Docker Compose stack is removed. The kind stack runs the PABC images of
`scripts/kind/charts/zac-dev/values.yaml`, which Renovate keeps up to date.
**Migration**: Start the local stack with `./start-kind.sh`.

### Requirement: Technical Keycloak realm roles are not imported as functional roles
**Reason**: The Docker Compose stack is removed.
**Migration**: The requirement holds for the kind stack; see the `pabc-local-stack` spec.
