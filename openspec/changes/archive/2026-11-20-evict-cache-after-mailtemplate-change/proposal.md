## Why

The cached configuration of a zaaktype holds a full copy of each linked mail template: its koppelingen are loaded
eagerly, and each koppeling loads its `MailTemplate` through a `@ManyToOne`. `MailTemplateService` is
`@Transactional` and clears that cache with `ZaaktypeConfigurationService.clearManagedCache()` before the commit. A
read in another request between the clear and the commit puts the previous template back in the cache, where it stays
for up to an hour. Behandelaars then send mails with a stale onderwerp or body
([PZ-13552](https://dimpact.atlassian.net/browse/PZ-13552)). PZ-13546 fixed the same race for storing a configuration
and a koppeling (ADR 0016), but its design wrongly assumed that `clearManagedCache` never runs inside a transaction.

## What Changes

- `MailTemplateService.updateMailtemplate` (also via `storeMailtemplate`) no longer calls
  `clearManagedCache()`. It looks up the zaaktypes whose configuration links the template and call the existing
  `ZaaktypeConfigurationService.evict(zaaktypeUuid)` for each, which clears at once and again after the transaction
  completes (ADR 0016).
- Creating or deleting a template no longer clears the cache: a new template is in no koppeling, and the database
  refuses to delete a linked one, so neither is in a cached configuration.
- `clearManagedCache()` stays a single, immediate clear for the "cache legen" button on the admin page.
- The design of the open change `evict-zaaktype-configuration-cache-after-commit` no longer says that
  `clearManagedCache` never runs inside a transaction that changes the configuration.

Out of scope: clearing the cache of other ZAC instances, and the gap of `invalidateAll()` with loads in flight for the
"cache legen" button.

## Capabilities

### Modified Capabilities
- `zaaktype-configuration`: a change of a mail template is visible at once in the cached configurations, and a
  rolled-back change is never served from the cache.

## Impact

- Backend: `MailTemplateService`, and a new query in `MailTemplateKoppelingenService`. `ZaaktypeConfigurationService`
  does not change.
- Callers of the changed `MailTemplateService` functions: `MailtemplateBeheerRestService.deleteMailtemplate` and
  `updateMailtemplate` (`MailtemplateBeheerRestService.kt:71,95`). `storeMailtemplate` has no production caller.
- Readers of the cached templates: `MailtemplateRestService.findMailtemplate` (`MailtemplateRestService.kt:42`) and
  `PlanItemsRestService` (`PlanItemsRestService.kt:180`). They do not change.
- Unit tests `MailTemplateServiceTest` and `MailTemplateKoppelingenServiceTest`; integration test
  `ZaaktypeConfigurationRestServiceBpmnMailTest`.
- No REST, OpenAPI, database, Solr, Flowable or frontend change; the test-data scripts do not change. No new ADR: this
  follows ADR 0016.
