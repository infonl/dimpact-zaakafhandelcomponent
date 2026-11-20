## Context

`ZaaktypeConfigurationService.uuidToConfigurationCache` caches each `ZaaktypeConfiguration` for an hour
(`ZaaktypeConfigurationService.kt:71`). The configuration loads its mailtemplate koppelingen eagerly
(`ZaaktypeConfiguration.kt:108-114`) and each koppeling loads its template with a default-eager `@ManyToOne`
(`ZaaktypeMailtemplateParameters.kt:44-47`), so the cache holds a copy of every linked template.

`MailTemplateService` is `@Transactional` at class level and calls `clearManagedCache()` in `delete`
(`MailTemplateService.kt:34`), `updateMailtemplate` (`:104`) and the create branch of `storeMailtemplate` (`:112`).
So, contrary to the design of `evict-zaaktype-configuration-cache-after-commit` (D5), `clearManagedCache` does run
inside a transaction that changes cached data, and it clears before the commit.

`clearManagedCache()` (`ZaaktypeConfigurationService.kt:125`) is also called by `UtilRestService.clearCaches`
(`UtilRestService.kt:185`), the "cache legen" button, which runs outside such a transaction.

## Goals / Non-Goals

**Goals:**
- No read between the change of a mail template and its commit can leave the previous template in the cache.
- No template read inside a transaction that rolls back stays in the cache.
- The "cache legen" button keeps clearing at once, once.

**Non-Goals:**
- The deadline warning windows cache, which holds no mail templates.
- Changing which mail template operations clear the cache.

## Decisions

### D1. `evictAll()` follows ADR 0016

`evictAll()` calls `uuidToConfigurationCache.invalidateAll()` and fires `AllZaaktypeConfigurationsCacheEvictionEvent`
with the `Event` that the constructor injects, next to the existing `Event<ZaaktypeConfigurationCacheEvictionEvent>`.
`onCacheEvictionOfAll(@Observes(during = TransactionPhase.AFTER_COMPLETION) ...)` calls `invalidateAll()` again. This
is the pattern of `evict` (`ZaaktypeConfigurationService.kt:110-123`), but for all zaaktypes, since one template can be
linked to the configuration of any zaaktype. Outside a transaction CDI calls the observer at once (ADR 0016).

### D2. A separate event, not a nullable zaaktype UUID

`AllZaaktypeConfigurationsCacheEvictionEvent` is a `data object`. Making the UUID of
`ZaaktypeConfigurationCacheEvictionEvent` nullable to mean "all" would hide a second meaning in a null.

### D3. `MailTemplateService` calls `evictAll()`; `clearManagedCache()` stays as it is

The three calls in `MailTemplateService` become `evictAll()`. `clearManagedCache()` keeps its immediate, single clear
and its return message for `UtilRestService`. `createMailtemplate` keeps not clearing: a new template is not linked.

## PR split

1. `fix: clear the zaaktype configuration cache after a mail template change completes` — D1-D3 with unit and
   integration tests.

No docs PR: ADR 0016 and the manual need no change.

## Rejected alternatives

- Evict only the zaaktypes linked to the template: needs a query per change for no real gain, and misses a template
  that is linked inside the same transaction.
- Turn `clearManagedCache()` itself into the double clear: the admin button would fire a needless event, and the
  story asks to keep it immediate and single.

## Risks / Trade-offs

- [The transaction phase cannot be unit tested] → The unit tests check that `evictAll` clears at once and fires the
  event, and that the observer clears; the integration test checks the result on WildFly. Neither can force a read
  between the clear and the commit (ADR 0016).
- [Each mail template change re-reads every configuration once more] → Template changes are rare admin actions.
