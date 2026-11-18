## Context

`ZaaktypeConfigurationService` keeps two Caffeine caches that expire one hour after they were filled: the
configuration per zaaktype UUID, and the list of deadline warning windows. `evict(zaaktypeUuid)` clears the entry of
one zaaktype and the whole list. Three callers call it inside a `@Transactional(REQUIRED)` method, so before the
commit:

- `ZaaktypeConfigurationBeheerService.storeConfiguration`, called by `ZaaktypeConfigurationRestService` (the admin
  screen) and by `upsertConfiguration` / `updateZaaktypeConfiguration` (the notification of a zaaktype version).
  On the notification route, `copySmartDocumentsTemplateMappings` runs afterwards in the same transaction.
- `MailTemplateKoppelingenService.storeMailtemplateKoppeling` and `MailTemplateKoppelingenService.delete`.

`clearManagedCache`, `clearListCache` and `UtilRestService.clearCaches` run outside such a transaction. ZAC has no
after-commit pattern yet: it uses no `TransactionSynchronizationRegistry` and no transactional observer.

## Goals / Non-Goals

**Goals:**
- No read between `evict` and the commit can leave the previous configuration in the cache.
- No configuration that was read inside a transaction that rolls back stays in the cache.
- All current and future callers of `evict` are covered without changing them.

**Non-Goals:**
- Clearing the cache of other ZAC instances. ZAC runs as one instance.
- The cleanup of `hasZaken` and `checkProcessBinding` (PZ-13551).

## Decisions

### D1. `evict` fires a synchronous CDI event; a transactional observer clears the caches

`evict` calls `Event<ZaaktypeConfigurationCacheEvictionEvent>.fire`, with an `Event` that the constructor injects.
`onCacheEviction(@Observes(during = TransactionPhase.AFTER_COMPLETION) ...)` clears both caches. `fire`, not
`fireAsync`: an asynchronous observer cannot be transactional (CDI 4.0, §2.8.4) and would run on another thread at
an unknown moment.

The CDI 4.0 specification, §2.8.5 "Observer notification", defines the behaviour that this relies on: "If the
observer method is a transactional observer method and there is currently a JTA transaction in progress, the
container calls the observer method during the appropriate transaction completion phase. [...] Otherwise, the
container calls the observer immediately." The Weld reference guide, §11.7 "Transactional observers", says the same
for Weld: "AFTER_COMPLETION observers are called during the after completion phase of the transaction".

### D2. After completion, not after success

`AFTER_COMPLETION` clears the caches after a commit and after a rollback. A read inside the writing transaction sees
the uncommitted row and caches it; after a rollback that entry must go too. The cost is at most one extra database
read after a rollback.

### D3. The observer lives in `ZaaktypeConfigurationService` itself

The caches are private fields of `ZaaktypeConfigurationService`; a separate observer bean would need a public method
on the service to clear them, which callers could call directly as well. CDI invokes a non-static observer method on
the contextual instance of the declaring bean, as a business method invocation (CDI 4.0, §2.4.5.6 "Invocation of
observer methods"). That is the instance that Weld builds with the `@Inject` constructor, not the instance that the
no-arg constructor of `@NoArgConstructor` creates for the client proxy, so the observer sees the same caches as
`findConfiguration`. `@AllOpen` makes the method open, so the proxy can delegate it. The `@ApplicationScoped` context
is always active, so the observer is never skipped for an inactive scope.

The observer method is public, like the other functions of the service. Calling it directly clears the caches at
once, as `clearManagedCache` does; it is not part of the contract that the callers use.

### D4. An exception in the observer does not fail the save

CDI 4.0, §2.8.5: "If the observer method is a transactional observer method, any exception is caught and logged by
the container." An exception in the observer cannot roll back the transaction, which has already completed, and it
does not reach the caller. Outside a transaction, the observer is not transactional at that moment, and an exception
would reach the caller of `evict`. Clearing a Caffeine cache does not throw in practice.

### D5. Clearing by hand stays immediate

`clearManagedCache` and `clearListCache` keep clearing the caches directly. A beheerder expects an empty cache when
the request returns, and these functions do not run in a transaction that changes the configuration.

### D6. Record the pattern in an ADR

Clearing an application-wide cache after the transaction completes is a cross-cutting pattern that other caches
will follow, so a new ADR records it.

## Risks / Trade-offs

- [The transaction-phase behaviour cannot be unit tested] → The unit test checks that `evict` fires the event and
  does not clear the caches, and that the observer clears both caches. The integration test checks on WildFly that
  a removed mailtemplate koppeling is no longer used after the save, which fails when the observer is not called at
  all. Neither test can force a read between the eviction and the commit; that relies on the CDI specification.
- [A read that starts before the commit and caches its result after the eviction] → Caffeine runs the loader of
  `Cache.get(key, loader)` inside the compute of its map, and `invalidate(key)` waits for a running compute of that
  key, so the configuration per zaaktype cannot keep a previous row. `invalidateAll()` on the list of deadline warning
  windows does not wait for a load that is still running; that list can keep the previous windows until it expires.
  This window is a few milliseconds and the same as today; the change does not make it larger.
