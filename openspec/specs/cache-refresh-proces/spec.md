# cache-refresh-proces Specification

## Purpose
Makes zaaktypes and their related ZTC types quickly available in ZAC by filling the ZTC caches right after
startup, with one paged list request per type instead of one request per zaaktype, and by keeping the cached
types as long as they are used (PZ-13072). Before this, the caches were empty after every start, expired after
an hour, and held at most 1,000 entries each, so that for example listing the zaaktypes for which a user may
create a zaak needed about 1 + 6 × (number of zaaktypes) requests to Open Zaak. Beheerders can see how many
entries and how much memory each ZTC cache holds, so that they can check that the caches fit in the memory of
the pod. See the spike "Zaaktype en zaakafhandelparameters data efficient beschikbaar maken in ZAC" (2026-10-14)
for the background.

## Requirements

### Requirement: Cache refresh after startup
After ZAC has started, the system SHALL refresh the ZTC caches of the default catalogus once, in the background,
so that startup does not wait for the refresh. The refresh SHALL run after the default catalogus has been read
from the configuration, and SHALL run as the system user, because no user session owns it. The system SHALL log
the start of the refresh, and its end together with its duration.

#### Scenario: ZAC starts
- **WHEN** ZAC has started
- **THEN** the system logs that the refresh of the ZTC caches has started
- **AND** refreshes the caches of the default catalogus as the system user, without blocking the startup
- **AND** logs that the refresh has finished, with its duration

#### Scenario: The refresh does not run periodically
- **WHEN** ZAC has been running for some time after startup
- **THEN** the system does not refresh the ZTC caches again by itself; types that are not cached yet are still
  read from the ZTC on demand

### Requirement: Zaaktypes are fetched with paging
The refresh SHALL fetch all zaaktypes of the default catalogus from the ZTC API, following every page of the
result, and SHALL fill both the cache of the zaaktype list per catalogus (`uriToZaakTypeListCache`) and the cache
of zaaktypes per UUID (`uuidToZaakTypeCache`). The system SHALL log how many zaaktypes it cached.

#### Scenario: Zaaktypes spread over several pages
- **WHEN** the zaaktypes of the default catalogus are spread over several pages of the ZTC API
- **THEN** the zaaktype list of the catalogus in the cache contains the zaaktypes of all pages
- **AND** reading any of these zaaktypes by UUID is answered from the cache, without a request to the ZTC

### Requirement: Related types are fetched with one paged list request per type
For each of the following types, the refresh SHALL fetch all items with one paged list request to the ZTC API,
instead of one request per zaaktype, SHALL group them by zaaktype, and SHALL fill these caches:

| Type                           | Caches                                                                  |
|--------------------------------|-------------------------------------------------------------------------|
| Statustypen                    | `uriToStatusTypeListCache`, `uuidToStatusTypeCache`                     |
| Resultaattypen                 | `uriToResultaatTypeListCache`, `uuidToResultaatTypeCache`               |
| Besluittypen                   | `uriToBesluitTypeListCache`, `uuidToBesluitTypeCache`                   |
| Informatieobjecttypen          | `uriToInformatieObjectTypeListCache`, `uuidToInformatieObjectTypeCache` |
| Zaaktype-informatieobjecttypen | `uriToZaakTypeInformatieObjectTypeListCache`                            |
| Eigenschappen                  | `uriToEigenschappenCache`                                               |
| Roltypen                       | `uriToRolTypeListCache`, `uuidToRolTypeCache`                           |

The cached lists SHALL equal what the per-zaaktype requests return: the same status filter (definitieve
eigenschappen; the API default for the other types) and the same order. A besluittype that belongs to several
zaaktypes SHALL be in the list of each of them. Informatieobjecttypen SHALL be cached regardless of their status,
because reading an informatieobjecttype by UUID does not filter on status either. The system SHALL log how many
items of each type it cached.

#### Scenario: A zaaktype with related types
- **WHEN** the caches have been refreshed for a catalogus with a zaaktype that has statustypen, resultaattypen,
  besluittypen, zaaktype-informatieobjecttypen, eigenschappen and roltypen
- **THEN** reading the related types of that zaaktype, as a list per zaaktype or one by one by UUID, is answered
  from the cache, without a request to the ZTC
- **AND** the ZTC was called once per type, not once per zaaktype

#### Scenario: A zaaktype without related types
- **WHEN** the caches have been refreshed for a catalogus with a zaaktype that has no statustypen, resultaattypen,
  besluittypen, informatieobjecttypen, eigenschappen or roltypen
- **THEN** the cache holds an empty list for each of these types of that zaaktype
- **AND** reading them is answered from the cache, without a request to the ZTC

#### Scenario: Types of a zaaktype in another catalogus
- **WHEN** a list request returns a type that belongs to a zaaktype outside the default catalogus
- **THEN** the system does not cache that type

#### Scenario: Related types spread over several pages
- **WHEN** the statustypen are spread over two pages of the ZTC API
- **THEN** the cached statustypen of a zaaktype contain the statustypen of both pages

### Requirement: A failing type does not stop the refresh
When fetching one of the related types fails, the system SHALL log a warning and continue refreshing the other
types; the cache of the failing type is then filled on demand, as before. When fetching the zaaktypes fails, the
system SHALL log a warning and skip the related types, because they are cached per zaaktype.

#### Scenario: The statustypen fail to load
- **WHEN** the list request for statustypen fails during the refresh
- **THEN** the system logs a warning, caches no statustypen, and still caches the other types

#### Scenario: The zaaktypes fail to load
- **WHEN** the list request for zaaktypes fails during the refresh
- **THEN** the system logs a warning and does not request the related types

### Requirement: Cached entries expire only when unused
The ZTC caches SHALL NOT expire as a whole after a fixed time. An entry SHALL be removed when it has been neither
read nor written for the number of seconds configured in `ZTC_CACHE_EXPIRATION_SECONDS` (Helm value
`ztcCacheExpirationSeconds`), which defaults to 86400 (one day). This removes types that no longer exist in the
ZTC, such as a zaaktype that was deleted from Open Zaak, from the cache.

#### Scenario: An entry that is used
- **WHEN** a cached type is read at least once within the configured expiration time
- **THEN** it stays in the cache

#### Scenario: An entry that is no longer used
- **WHEN** a cached type has been neither read nor written for the configured expiration time
- **THEN** the system removes it from the cache

### Requirement: Configurable maximum size of the caches
Each ZTC cache SHALL hold at most the number of entries configured in `ZTC_CACHE_MAX_SIZE` (Helm value
`ztcCacheMaxSize`), which defaults to 10000. Because the refresh fills every cache with all types of the
catalogus, this limit has to be larger than the number of types of the largest cache.

#### Scenario: More entries than the maximum
- **WHEN** a ZTC cache receives more entries than the configured maximum
- **THEN** the cache evicts entries until it holds at most the maximum

### Requirement: Number of entries and memory usage on the admin page
The admin page `/rest/admin/util/cache/ztc` SHALL show, for each ZTC cache, its statistics, its estimated number
of entries as "Estimated cache size: <number> item(s)", and its estimated memory usage, and SHALL show the total
memory usage of all ZTC caches. The memory usage SHALL include the keys and values of a cache and every object
they refer to, and SHALL be shown in readable units together with the exact number of bytes. Because measuring
walks through every cached object, only this page SHALL measure the memory usage: the pages
`/rest/admin/util/cache` and `/rest/admin/util/cache/zhps` show the number of entries but no memory usage. Like the
other admin util pages, the page SHALL require the 'beheren' permission.

#### Scenario: A beheerder opens the ZTC cache page
- **WHEN** a user with the 'beheren' permission opens `/rest/admin/util/cache/ztc`
- **THEN** the page shows the total estimated memory usage of all ZTC caches
- **AND** for each ZTC cache its estimated number of entries and its estimated memory usage

#### Scenario: A beheerder opens the page with all caches
- **WHEN** a user with the 'beheren' permission opens `/rest/admin/util/cache`
- **THEN** the page shows the number of entries of each cache
- **AND** the system does not measure the memory usage of the caches

#### Scenario: A user without the 'beheren' permission
- **WHEN** a user without the 'beheren' permission opens `/rest/admin/util/cache/ztc`
- **THEN** the system refuses the request and does not measure the memory usage of the caches
