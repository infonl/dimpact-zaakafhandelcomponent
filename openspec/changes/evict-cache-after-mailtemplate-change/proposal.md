## Why

The cached configuration of a zaaktype holds a full copy of each linked mail template: its koppelingen are loaded
eagerly, and each koppeling loads its `MailTemplate` through a `@ManyToOne`. `MailTemplateService` is
`@Transactional` and clears that cache with `ZaaktypeConfigurationService.clearManagedCache()` before the commit. A
read in another request between the clear and the commit puts the previous template back in the cache, where it stays
for up to an hour. Behandelaars then send mails with a stale onderwerp or body
([PZ-13552](https://dimpact.atlassian.net/browse/PZ-13552)). PZ-13546 fixed the same race for storing a configuration
and a koppeling (ADR 0016), but its design wrongly assumed that `clearManagedCache` never runs inside a transaction.

## What Changes

- `ZaaktypeConfigurationService` gets `evictAll()`: it clears the cache of all configurations at once and fires a CDI
  event. An observer in `TransactionPhase.AFTER_COMPLETION` clears it again after a commit or rollback (ADR 0016).
- `MailTemplateService.delete`, `updateMailtemplate` and `storeMailtemplate` call `evictAll()` instead of
  `clearManagedCache()`.
- `clearManagedCache()` stays a single, immediate clear for the "cache legen" button on the admin page.
- The design of the open change `evict-zaaktype-configuration-cache-after-commit` no longer says that
  `clearManagedCache` never runs inside a transaction that changes the configuration.

Out of scope: the deadline warning windows cache (it holds no mail templates), `createMailtemplate` called directly
by the REST endpoint (a new template is in no koppeling, so in no cached configuration), and clearing the cache of
other ZAC instances.

## Capabilities

### Modified Capabilities
- `zaaktype-configuration`: a change of a mail template is visible at once in the cached configurations, and a
  rolled-back change is never served from the cache.

## Impact

- Backend: `ZaaktypeConfigurationService`, `MailTemplateService`, and a new event class.
- Callers of the changed `MailTemplateService` functions: `MailtemplateBeheerRestService.deleteMailtemplate` and
  `updateMailtemplate` (`MailtemplateBeheerRestService.kt:71,95`). `storeMailtemplate` has no production caller.
- Readers of the cached templates: `MailtemplateRestService.findMailtemplate` (`MailtemplateRestService.kt:42`) and
  `PlanItemsRestService` (`PlanItemsRestService.kt:180`). They do not change.
- Unit tests `ZaaktypeConfigurationServiceTest` and `MailTemplateServiceTest`; integration test
  `ZaaktypeConfigurationRestServiceBpmnMailTest`.
- No REST, OpenAPI, database, Solr, Flowable or frontend change; the test-data scripts do not change. No new ADR: this
  follows ADR 0016.
