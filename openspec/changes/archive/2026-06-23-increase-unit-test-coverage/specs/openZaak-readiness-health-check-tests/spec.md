## ADDED Requirements

### Requirement: OpenZaakReadinessHealthCheck returns UP when Open Zaak is reachable
When `call()` is invoked and `ZtcClientService.listCatalogus` succeeds, the health check SHALL return a response with status UP.

#### Scenario: Open Zaak reachable
- **WHEN** `call()` is invoked and `ZtcClientService.listCatalogus` returns successfully
- **THEN** a `HealthCheckResponse` with status UP is returned

### Requirement: OpenZaakReadinessHealthCheck returns DOWN when Open Zaak is unreachable
When `call()` is invoked and `ZtcClientService.listCatalogus` throws a `RuntimeException`, the health check SHALL return a response with status DOWN containing the error message.

#### Scenario: Open Zaak unreachable
- **WHEN** `call()` is invoked and `ZtcClientService.listCatalogus` throws a `RuntimeException`
- **THEN** a `HealthCheckResponse` with status DOWN is returned
- **THEN** the response data contains the exception message
