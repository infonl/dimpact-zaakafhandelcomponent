## Context

`ZaaktypeConfigurationService.uuidToConfigurationCache` caches each `ZaaktypeConfiguration` for an hour
(`ZaaktypeConfigurationService.kt:71`). The configuration loads its mailtemplate koppelingen eagerly
(`ZaaktypeConfiguration.kt:108-114`) and each koppeling loads its template with a default-eager `@ManyToOne`
(`ZaaktypeMailtemplateParameters.kt:44-47`), so the cache holds a copy of every linked template.

`MailTemplateService` is `@Transactional` at class level and calls `clearManagedCache()` in `delete`
(`MailTemplateService.kt:34`), `updateMailtemplate` (`:104`) and the create branch of `storeMailtemplate` (`:112`).
So, contrary to the design of `evict-zaaktype-configuration-cache-after-commit` (D5), `clearManagedCache` does run
inside a transaction that changes cached data, and it clears before the commit.

`clearManagedCache()` (`ZaaktypeConfigurationService.kt:125`) uses `invalidateAll()`. Caffeine's `invalidateAll()`
skips entries that are still being loaded, while `invalidate(key)` waits for a running load of that key. So a
whole-cache clear after the commit could miss a read that started before it. `evict(zaaktypeUuid)`
(`ZaaktypeConfigurationService.kt:110-123`) clears per key, at once and again after the transaction completes.

## Goals / Non-Goals

**Goals:**
- No read between the change of a mail template and its commit can leave the previous template in the cache.
- No template read inside a transaction that rolls back stays in the cache.
- The "cache legen" button keeps clearing at once, once.

**Non-Goals:**
- Changing `ZaaktypeConfigurationService` or the "cache legen" button.

## Decisions

### D1. Evict the zaaktypes that link the template, with the existing `evict`

`MailTemplateKoppelingenService` gets `listZaaktypeUuidsLinkedTo(mailTemplateId)`: the distinct
`zaaktypeConfiguration.zaaktypeUuid` of the `ZaaktypeMailtemplateParameters` whose `mailTemplate` is that template.
`MailTemplateService.updateMailtemplate` calls `zaaktypeConfigurationService.evict(uuid)` for each UUID it returns. `MailTemplateService` gets `MailTemplateKoppelingenService` injected; there is no cycle.

`evict` already follows ADR 0016 and clears per key, so a read that is loading when the second clear runs is waited
for and then removed. It also clears the deadline warning windows list, which costs one extra read.

### D2. No eviction on create or delete

A new template is in no koppeling, so no cached configuration holds it. The create branch of `storeMailtemplate`
stops calling `clearManagedCache()`, like `createMailtemplate`, which the REST endpoint calls, never did.

A linked template cannot be deleted: `fk_id_mail_template` is `ON DELETE RESTRICT`
(`V35__mail_template_koppelingen.sql:12-13`). A delete that commits removes a template that no committed koppeling
links, so the deleting instance holds no cached configuration with it, and the delete of a linked one fails. Another
instance may still hold a copy cached before the last koppeling was removed, until it expires. So `delete` stops calling `clearManagedCache()` too.

### D3. `clearManagedCache()` stays as it is

It keeps its immediate, single clear and its return message for `UtilRestService` (`UtilRestService.kt:185`).

## PR split

1. `fix: clear the cached zaaktype configurations after a mail template change completes` — D1-D3 with unit and
   integration tests.

No docs PR: ADR 0016 and the manual need no change.

## Rejected alternatives

- `evictAll()` with `invalidateAll()` twice: misses a load that is still running at the second clear.
- Replace the whole cache with a new instance on each change: covers loads in flight, but makes the cache field
  mutable and its statistics harder to keep, for a rare admin action.

## Risks / Trade-offs

- [The transaction phase cannot be unit tested] → The unit tests check which zaaktypes `evict` is called for; the
  integration test checks the result on WildFly. Neither can force a read between the clear and the commit (ADR
  0016).
- [The template gets a new koppeling while it is being changed] → This happens when another beheerder links it, or
  when the notification of a new zaaktype version copies the koppelingen of the previous version
  (`ZaaktypeConfigurationVersioning.kt:59`). The lookup does not see that koppeling yet, so that zaaktype can keep the
  previous template for up to an hour. Both at the same moment as a template change is rare; "cache legen" fixes it.
