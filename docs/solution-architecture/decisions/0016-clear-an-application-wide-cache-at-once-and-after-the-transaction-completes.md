---
status: accepted
date: 2026-11-18
decision-makers: Team Genève
tags: [backend, cdi, caching, transactions]
---

# Clear an application-wide cache at once and again after the transaction that changes its data completes

## Context and Problem Statement

`ZaaktypeConfigurationService` caches the configuration of each zaaktype version and the list of deadline warning
windows for up to an hour. The services that store a configuration or a mailtemplate koppeling cleared these caches
inside their transaction, before the commit. A read in another request between the clearing and the commit put the
previous configuration back in the cache, where it stayed for up to an hour
([PZ-13546](https://dimpact.atlassian.net/browse/PZ-13546)). Clearing only after the commit is not enough either:
code later in the same transaction reads through the cache. On the notification of a new zaaktype version,
`copySmartDocumentsTemplateMappings` reads the configuration that was just stored, and finds none when the cache
still holds the earlier absence. ZAC had no way yet to run code after a transaction completes. How does ZAC clear an
application-wide cache so that it never keeps data that a transaction is still changing, and a transaction still
sees its own change?

## Decision Drivers

* No read between the clearing and the commit can leave the previous data in the cache
* A read inside a transaction that rolls back cannot leave its uncommitted data in the cache
* A read later in the changing transaction sees the change
* The callers that change the data do not have to know when the cache is cleared
* Clearing still works when it is called outside a transaction
* A standard Jakarta EE API, without a dependency on WildFly or Narayana

## Considered Options

* Clear the cache at once, and again in an `AFTER_COMPLETION` transactional observer of a synchronous CDI event
* Clear the cache only in an `AFTER_COMPLETION` transactional observer of a synchronous CDI event
* Register a JTA `Synchronization` with the `TransactionSynchronizationRegistry`
* Clear the cache in the callers, after their transactional method has returned
* Cache the data in the Hibernate second-level cache with the `READ_WRITE` strategy

## Decision Outcome

Chosen option: "Clear the cache at once, and again in an `AFTER_COMPLETION` transactional observer of a synchronous
CDI event", because it is the only option that meets all decision drivers without replacing the cache. The pattern is
known as a delayed double delete for a cache-aside cache.

The function that clears the cache clears it at once, so that the rest of the transaction reads the change from the
database, and fires an event with `Event.fire`, with an `Event` that the constructor injects. An observer method
`@Observes(during = TransactionPhase.AFTER_COMPLETION)` in the bean that owns the cache clears it again, which
removes what a concurrent read cached before the commit, and what a read inside a transaction that rolled back
cached. `AFTER_COMPLETION`, not `AFTER_SUCCESS`, so that the cache is also cleared after a rollback. Clearing a cache
by hand, on the admin page, stays a single, immediate clear. The first cache that follows this decision is that of
`ZaaktypeConfigurationService`.

### Consequences

* Good, because a read later in the changing transaction sees the change, a read before the commit cannot keep the
  previous data, and a rollback cannot leave uncommitted data in the cache.
* Good, because the callers do not change, and every future caller is covered.
* Good, because outside a transaction CDI calls the observer at once (CDI 4.0, §2.8.5), so the cache is cleared twice
  in a row, which costs nothing.
* Bad, because the cache is read from the database once more after each change.
* Bad, because clearing twice looks redundant, so the reason must stay in the code and in this ADR.
* Bad, because a unit test cannot show when the observer runs: that depends on the CDI container and the
  transaction manager.
* Bad, because the container catches and logs an exception of a transactional observer (CDI 4.0, §2.8.5), so a
  failure to clear the cache after the transaction does not reach the caller.

### Confirmation

Not checked automatically. The unit test of the bean asserts that clearing clears the cache at once and fires the
event, and that the observer clears the cache.

## Pros and Cons of the Options

### Clear the cache at once, and again in an `AFTER_COMPLETION` transactional observer of a synchronous CDI event

* Good, because it meets all decision drivers with the CDI API that ZAC already uses for its other events.
* Good, because the observer can stay in the bean that owns the cache, next to its private cache fields.
* Bad, because the cache is cleared twice, and an observer method is called by the container, which makes the flow
  harder to follow in the code.

### Clear the cache only in an `AFTER_COMPLETION` transactional observer of a synchronous CDI event

* Good, because the cache is cleared once.
* Bad, because a read later in the changing transaction gets the previous data from the cache. On the notification
  of a new zaaktype version, copying the SmartDocuments template mappings then finds no configuration, and the
  transaction rolls back.

### Register a JTA `Synchronization` with the `TransactionSynchronizationRegistry`

* Good, because the code shows directly that it runs after the transaction completes.
* Bad, because the code must check itself whether a transaction is active and clear the cache at once if not, which
  CDI does for a transactional observer.
* Bad, because the `TransactionSynchronizationRegistry` must be injected as a resource, and a unit test must mock
  the registry and the synchronization.

### Clear the cache in the callers, after their transactional method has returned

* Good, because it needs no event or synchronization.
* Bad, because every caller must remember it, and a caller that runs inside a larger transaction, such as the
  notification of a zaaktype version, still clears before the commit.

### Cache the data in the Hibernate second-level cache with the `READ_WRITE` strategy

* Good, because its soft locks cover all drivers without any code that clears the cache: a changed entry is locked
  until the transaction completes, a read during the lock goes to the database without caching, and the changing
  transaction sees its own change.
* Bad, because it replaces the Caffeine cache, its statistics and its manual clearing on the admin page, and the
  lookup by zaaktype UUID needs a natural id or the query cache. That is a larger change than PZ-13546.

## More Information

* [PZ-13546](https://dimpact.atlassian.net/browse/PZ-13546)
* [Jakarta CDI 4.0, §2.8.5 Observer notification](https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#observer_notification)
* [Weld reference guide, §11.7 Transactional observers](https://docs.jboss.org/weld/reference/latest/en-US/html_single/#_transactional_observers)
* The OpenSpec change `evict-zaaktype-configuration-cache-after-commit`.
