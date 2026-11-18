---
status: accepted
date: 2026-11-18
decision-makers: Team Genève
tags: [backend, cdi, caching, transactions]
---

# Clear an application-wide cache after the transaction that changes its data completes

## Context and Problem Statement

`ZaaktypeConfigurationService` caches the configuration of each zaaktype version and the list of deadline warning
windows for up to an hour. The services that store a configuration or a mailtemplate koppeling cleared these caches
inside their transaction, before the commit. A read in another request between the clearing and the commit put the
previous configuration back in the cache, where it stayed for up to an hour
([PZ-13546](https://dimpact.atlassian.net/browse/PZ-13546)). ZAC had no way yet to run code after a transaction
completes. How does ZAC clear an application-wide cache so that it never keeps data that a transaction is still
changing?

## Decision Drivers

* No read between the clearing and the commit can leave the previous data in the cache
* A read inside a transaction that rolls back cannot leave its uncommitted data in the cache
* The callers that change the data do not have to know when the cache is cleared
* Clearing still works when it is called outside a transaction
* A standard Jakarta EE API, without a dependency on WildFly or Narayana

## Considered Options

* Fire a synchronous CDI event and clear the cache in an `AFTER_COMPLETION` transactional observer
* Register a JTA `Synchronization` with the `TransactionSynchronizationRegistry`
* Clear the cache in the callers, after their transactional method has returned

## Decision Outcome

Chosen option: "Fire a synchronous CDI event and clear the cache in an `AFTER_COMPLETION` transactional observer",
because it meets all decision drivers with the CDI API that ZAC already uses for its other events.

The function that clears the cache fires the event with `Event.fire`, with an `Event` that the constructor injects.
An observer method `@Observes(during = TransactionPhase.AFTER_COMPLETION)` in the bean that owns the cache clears
it. `AFTER_COMPLETION`, not `AFTER_SUCCESS`, so that the cache is also cleared after a rollback. Clearing a cache by
hand, on the admin page, stays immediate. The first cache that follows this decision is that of
`ZaaktypeConfigurationService`.

### Consequences

* Good, because the cache is cleared after the commit, so a read during the transaction cannot keep the previous
  data, and after a rollback, so it cannot keep uncommitted data either.
* Good, because the callers do not change, and every future caller is covered.
* Good, because outside a transaction CDI calls the observer at once (CDI 4.0, §2.8.5).
* Bad, because a unit test cannot show when the observer runs: that depends on the CDI container and the
  transaction manager.
* Bad, because the container catches and logs an exception of a transactional observer (CDI 4.0, §2.8.5), so a
  failure to clear the cache does not reach the caller.

### Confirmation

Not checked automatically. The unit test of the bean asserts that clearing fires the event and does not clear the
cache itself, and that the observer clears it.

## Pros and Cons of the Options

### Fire a synchronous CDI event and clear the cache in an `AFTER_COMPLETION` transactional observer

* Good, because it is the standard CDI API, which Weld implements on top of the JTA transaction of WildFly.
* Good, because the observer can stay in the bean that owns the cache, next to its private cache fields.
* Bad, because an observer method is called by the container, which makes the flow harder to follow in the code.

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

## More Information

* [PZ-13546](https://dimpact.atlassian.net/browse/PZ-13546)
* [Jakarta CDI 4.0, §2.8.5 Observer notification](https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#observer_notification)
* [Weld reference guide, §11.7 Transactional observers](https://docs.jboss.org/weld/reference/latest/en-US/html_single/#_transactional_observers)
* The OpenSpec change `evict-zaaktype-configuration-cache-after-commit`.
