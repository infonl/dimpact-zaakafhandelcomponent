## Why

`ZaaktypeConfigurationService.evict` clears the cached configuration of a zaaktype and the cached list of deadline
warning windows while the transaction that stores the change is still running, so before its commit. A read in
another request between the eviction and the commit puts the previous configuration back in the cache, where it
stays for up to an hour unless a beheerder clears the cache by hand
([PZ-13546](https://dimpact.atlassian.net/browse/PZ-13546)). A new zaak can then start the previous process or get
the previous defaults, deadline warnings, SmartDocuments setting or mail templates.

## What Changes

- `ZaaktypeConfigurationService.evict` keeps clearing both caches at once, so that code later in the same
  transaction reads the change from the database. It also fires a CDI event. An observer in
  `TransactionPhase.AFTER_COMPLETION` clears both caches again after the transaction completes, after a commit and
  after a rollback. Outside a transaction CDI calls the observer at once.
- The callers of `evict` do not change: `ZaaktypeConfigurationBeheerService.storeConfiguration`, which the admin
  screen and the notification of a zaaktype version both use, and `MailTemplateKoppelingenService`
  `storeMailtemplateKoppeling` and `delete`.
- Clearing the caches by hand (`clearManagedCache`, `clearListCache`) stays a single, immediate clear.
- A new ADR records clearing an application-wide cache at once and again after the transaction completes, with a
  CDI transactional observer, as the pattern for ZAC.

Out of scope: the cleanup of `hasZaken` and `checkProcessBinding` (PZ-13551), clearing the cache of other ZAC
instances, and replacing the Caffeine cache with the Hibernate second-level cache.

## Capabilities

### Modified Capabilities
- `zaaktype-configuration`: a stored change of a zaaktype configuration is visible at once, also later in the same
  transaction, and a rolled-back change is never served from the cache.

## Impact

- Backend: `ZaaktypeConfigurationService` and a new event class `ZaaktypeConfigurationCacheEvictionEvent`.
- Unit test `ZaaktypeConfigurationServiceTest`; the tests of the callers do not change.
- Integration test `ZaaktypeConfigurationRestServiceBpmnMailTest` gets a step that removes a mailtemplate koppeling
  and reads the mail template of a zaak again.
- New ADR in `docs/solution-architecture/decisions`.
- No REST, OpenAPI, database or frontend change; the test-data scripts do not change.
