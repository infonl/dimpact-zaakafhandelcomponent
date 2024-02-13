# Developer documentation

The following ZAC developer documentation is available:

- [CONTRIBUTING.md](../../CONTRIBUTING.md) - Instructions on how to contribute to ZAC.
- [Angular](angular.md) - Instructions on how to perform Angular updates
- [Font caching procedures](fontCachingProcedures.md) - Instructions on font caching in ZAC.
- [INSTALL.md](INSTALL.md) - Instructions on how to build, run and test the software.
- [installDockerCompose.md](installDockerCompose.md) - Instructions on how to run the software using Docker Compose.
- [endToEndTypeSafety.md](endToEndTypeSafety.md) - Instructions on how to develop ZAC using end-to-end type safety.
- [testing.md](testing.md) - Instructions on how to run and develop tests for ZAC.
- [updatingDependencies.md](updatingDependencies.md) - Instructions on how to update dependencies in ZAC and (OpenAPI) API specifications used by ZAC.

## Monitoring
ZAC exposes two monitoring endpoints through WildFly by default:

- http://localhost:9990/health
- http://localhost:9990/metrics

Note: These are not secured. To disable, remove the metrics layer from the install-wildfly.sh script.
