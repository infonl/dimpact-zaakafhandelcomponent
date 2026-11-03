---
status: accepted
date: 2026-11-03
decision-makers: Team Genève
tags: [dependencies, caching, memory, accepted-risk]
---

# Use JOL to measure the memory usage of the ZTC caches

## Context and Problem Statement

ZAC caches the zaaktypen of the default catalogus and their related types from Open Zaak in Caffeine caches, and
refreshes them in the background ([PZ-13219](https://dimpact.atlassian.net/browse/PZ-13219)). To size the memory of a
ZAC pod, a beheerder needs to know how much memory these caches use. The admin page `/rest/admin/util/cache/ztc`
therefore shows the memory usage of each ZTC cache and the total. Which library or technique measures the deep size of
the cached objects in the running JVM?

## Decision Drivers

* The figure must be the real size of the cached object graphs, including the objects they refer to, and must count
  an object that several caches share only once
* The measurement must work in the ZAC Docker image without a Java agent or extra JVM flags
* A runtime measurement is only needed on the admin page, not on every cache access
* The page is only reachable to admins and not documented in the manual. It is not a problem if it stops working.

## Considered Options

* JOL (Java Object Layout)
* `Instrumentation.getObjectSize` with our own traversal of the object graph
* Ehcache `sizeof`
* Lucene `RamUsageEstimator`
* An estimated weight per cache entry in Caffeine
* No runtime measurement: measure in a test or a heap dump and document the figures

## Decision Outcome

Chosen option: "JOL (Java Object Layout)", because it is the only option that measures the real deep size of the
object graphs, deduplicated by identity, without a Java agent or extra JVM flags.

ZAC uses `org.openjdk.jol:jol-core` (version 0.17) only in `ZtcClientService.estimatedMemoryUsage()`, which runs only
when a beheerder opens the admin page.

### Consequences

* Good, because the admin page shows the measured memory usage per cache and a deduplicated total, which a beheerder
  can use to size a ZAC pod.
* Good, because the measurement costs nothing unless a beheerder opens the admin page.
* Bad, because JOL uses the memory-access methods of `sun.misc.Unsafe`, which
  [JEP 471](https://openjdk.org/jeps/471) deprecates for removal. Since [JEP 498](https://openjdk.org/jeps/498)
  (JDK 24), the JVM logs a warning the first time JOL measures. ZAC runs on Java 25, so it logs this warning.
* Bad, because a later JDK is planned to throw an exception by default when these methods are used. From that JDK on,
  the memory measurement only works when ZAC starts with `--sun-misc-unsafe-memory-access=allow`, until JOL no longer
  uses `sun.misc.Unsafe` or ZAC replaces it.

### Confirmation

Not checked automatically. When ZAC upgrades to a JDK that blocks the memory-access methods of `sun.misc.Unsafe` by
default, we revisit this decision.

## Pros and Cons of the Options

### JOL (Java Object Layout)

* Good, because it measures the deep size of several roots in one traversal and counts each object only once.
* Good, because it is maintained by the OpenJDK project.
* Bad, because it uses `sun.misc.Unsafe`, see the consequences above.

### `Instrumentation.getObjectSize` with our own traversal of the object graph

* Good, because it uses no `sun.misc.Unsafe`.
* Bad, because it only gives the shallow size of an object, so ZAC would have to traverse the object graph itself.
* Bad, because it needs a Java agent, either with `-javaagent` in the startup of WildFly or with a dynamic attach,
  which the JDK also warns about.
* Bad, because reading the private fields of JDK classes such as `String` and `ArrayList` needs `--add-opens` flags.

### Ehcache `sizeof`

* Bad, because it uses `sun.misc.Unsafe` too, or falls back to a Java agent or reflection with the same problems as
  the option above.

### Lucene `RamUsageEstimator`

* Good, because recent versions use no `sun.misc.Unsafe`.
* Bad, because it estimates the shallow size, and a deep size still needs reflection into JDK classes with
  `--add-opens` flags.
* Bad, because it is a large dependency for this one use.

### An estimated weight per cache entry in Caffeine

* Good, because it needs no library, agent or JVM flag, and costs little.
* Bad, because the figure is an estimate and not a measurement.

### No runtime measurement: measure in a test or a heap dump and document the figures

* Good, because ZAC needs no runtime dependency and logs no JDK warning.
* Bad, because the admin page cannot show the memory usage of the caches of a running environment.

## More Information

* [JEP 471: Deprecate the Memory-Access Methods in sun.misc.Unsafe for Removal](https://openjdk.org/jeps/471)
* [JEP 498: Warn upon Use of Memory-Access Methods in sun.misc.Unsafe](https://openjdk.org/jeps/498)
* [JOL](https://github.com/openjdk/jol)
