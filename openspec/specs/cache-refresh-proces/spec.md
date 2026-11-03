# cache-refresh-proces Specification

## Purpose
Makes zaaktypes and their related ZTC types quickly available in ZAC by filling the ZTC caches right after
startup, again after the caches have been cleared and periodically, with one paged list request per type instead
of one request per zaaktype, and by keeping the cached types as long as they are used (PZ-13072). Before this, the
caches were empty after every start, expired after an hour, and held at most 1,000 entries each, so that for example listing the zaaktypes for which a user may
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

### Requirement: Periodic cache refresh
After ZAC has started, the system SHALL refresh the ZTC caches of the default catalogus in the background every
number of seconds configured in `ZTC_CACHE_REFRESH_INTERVAL_SECONDS` (Helm value `ztcCacheRefreshIntervalSeconds`),
which defaults to 3600 (one hour), in the same way as after startup. This makes changes in the ZTC that Open Zaak
sends no notification for appear in ZAC, such as a zaaktype that was published with the publish action of the API or
with the bulk publish action of the Open Zaak admin, which would otherwise stay missing from a zaaktype list that is
read often and therefore never expires. A periodic refresh that is requested while a refresh runs SHALL be combined
with it as described in "Cache refresh after the caches have been cleared". When the interval is 0, the system SHALL
NOT refresh the ZTC caches periodically. When the Helm value is missing or empty, the Helm chart SHALL use the default
of 3600, while a value of 0 SHALL be kept.

#### Scenario: The refresh interval has passed
- **WHEN** ZAC has been running for the configured refresh interval since startup or since the previous periodic
  refresh
- **THEN** the system refreshes the ZTC caches of the default catalogus in the background

#### Scenario: A zaaktype is published without a notification
- **WHEN** a zaaktype is published with the publish action of the Open Zaak API
- **THEN** the zaaktype appears in the cached zaaktype list of its catalogus at the latest after the next periodic
  refresh

#### Scenario: The periodic refresh is turned off
- **WHEN** the refresh interval is configured as 0
- **THEN** the system refreshes the ZTC caches only after startup and after the caches have been cleared

### Requirement: Cache refresh after the caches have been cleared
After a beheerder has cleared the ZTC caches with `/rest/admin/util/cache/clear` or
`/rest/admin/util/cache/ztc/clear`, the system SHALL refresh the ZTC caches of the default catalogus in the
background, in the same way as after startup, so that the types are again available without one request per
zaaktype. When refreshes are requested while a refresh runs, the system SHALL run one more refresh after the running
one, because the running refresh may have read the ZTC before the change, and SHALL NOT run a refresh per request.
When a refresh fails, also with an error instead of an exception, the system SHALL log a warning and still run later
refreshes.

#### Scenario: A beheerder clears the caches
- **WHEN** a user with the 'beheren' permission opens `/rest/admin/util/cache/clear` or
  `/rest/admin/util/cache/ztc/clear`
- **THEN** the system clears the ZTC caches
- **AND** refreshes the ZTC caches in the background, and shows that it does so

#### Scenario: Several refreshes are requested while a refresh runs
- **WHEN** several refreshes are requested while a refresh runs
- **THEN** the system runs one more refresh after the running one

#### Scenario: A refresh fails
- **WHEN** a refresh fails with an exception or an error
- **THEN** the system logs a warning
- **AND** still runs the refresh that is requested next

### Requirement: Notifications invalidate only the changed types
When the system receives a notification about a changed type in the ZTC, it SHALL remove only that type and the
cached entries that depend on it from the ZTC caches, and SHALL NOT clear whole caches or refresh the ZTC caches,
so that one change does not make the next reads of all other types go to the ZTC. The removed entries are filled on
demand when they are read again.

| Notification                                          | Entries that the system removes                                                                                                                                                       |
|-------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Zaaktype created or updated                           | The zaaktype, the zaaktype lists of the catalogi, and the statustypen, resultaattypen, besluittype lists, zaaktype-informatieobjecttypen, informatieobjecttype lists, eigenschappen and roltypen of that zaaktype |
| Besluittype created or updated                        | The besluittype, and the besluittype lists of the zaaktypen that it is or was linked to                                                                                              |
| Besluittype deleted                                   | The besluittype, and the besluittype lists that contain it                                                                                                                           |
| Informatieobjecttype created, updated or deleted      | The informatieobjecttype, and the informatieobjecttype lists that contain it                                                                                                         |

To find the zaaktypen that a created or updated besluittype is linked to now, the system SHALL read the besluittype
from the ZTC. When that read fails, the system SHALL log a warning and remove all besluittype lists.

#### Scenario: A notification about an updated zaaktype
- **WHEN** the system receives a notification that a zaaktype has been created or updated
- **THEN** the system removes that zaaktype, its related types and the zaaktype lists from the ZTC caches
- **AND** the related types of the other zaaktypen stay in the caches

#### Scenario: A notification about a besluittype that is linked to another zaaktype
- **WHEN** the system receives a notification that a besluittype has been updated, and the besluittype is now also
  linked to a zaaktype whose besluittype list is in the cache
- **THEN** the system removes the besluittype and the besluittype lists of all zaaktypen that it is linked to
- **AND** the besluittype lists of the other zaaktypen stay in the cache

#### Scenario: A notification about a changed besluittype or informatieobjecttype
- **WHEN** the system receives a notification that a besluittype or an informatieobjecttype has been created,
  updated or deleted, for example because its einde geldigheid has changed
- **THEN** the system removes that type from the ZTC caches, so that ZAC does not keep checking the validity of a type
  against a cached einde geldigheid
- **AND** the other types of that kind stay in the caches

#### Scenario: A besluittype that cannot be read
- **WHEN** the system receives a notification that a besluittype has been updated, and the besluittype cannot be read
  from the ZTC
- **THEN** the system logs a warning and removes all besluittype lists from the cache

### Requirement: A refresh does not put back entries that were removed while it ran
A refresh SHALL write the types it fetched to the caches only after it has fetched all types. It SHALL NOT write the
entries that were invalidated or cleared while it ran, because the fetched types may predate the change that caused the
removal and would put back entries that the removal removed, such as a deleted type. It SHALL write all other entries,
so that frequent notifications or clears do not stop a refresh from filling the caches.

#### Scenario: A type is deleted while a refresh runs
- **WHEN** a notification that a besluittype has been deleted arrives while a refresh runs that has already fetched
  that besluittype
- **THEN** the running refresh writes neither that besluittype nor the besluittype lists that contain it to the
  caches
- **AND** writes the other types it fetched

#### Scenario: A cache is cleared while a refresh runs
- **WHEN** the besluittype caches are cleared while a refresh runs
- **THEN** the running refresh writes no besluittypen to the caches
- **AND** writes the other types it fetched

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
because reading an informatieobjecttype by UUID does not filter on status either. Besluittypen SHALL be fetched from
all catalogi, because a zaaktype can be linked to a besluittype of another catalogus and reading the besluittypen of
a zaaktype does not filter on catalogus either. The system SHALL log how many items of each type it cached.

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

#### Scenario: A besluittype of another catalogus
- **WHEN** a zaaktype of the default catalogus is linked to a besluittype of another catalogus
- **THEN** the cached besluittypen of that zaaktype contain that besluittype

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
`ztcCacheExpirationSeconds`), which defaults to 86400 (one day). Because the system ignores notifications about deleted
zaaktypen, a type that no longer exists in the ZTC, such as a zaaktype that was deleted from Open Zaak, is eventually removed
from the cache, once it has been neither read nor written for that time. A periodic refresh replaces the cached lists
of the zaaktypes that still exist, so a deleted type disappears from these lists after the next periodic refresh.

#### Scenario: An entry that is used
- **WHEN** a cached type is read at least once within the configured expiration time
- **THEN** it stays in the cache

#### Scenario: An entry that is no longer used
- **WHEN** a cached type has been neither read nor written for the configured expiration time
- **THEN** the system removes it from the cache

#### Scenario: A deleted type that is still read
- **WHEN** a type that was deleted from the ZTC is still read at least once within the configured expiration time
- **THEN** it stays in the cache of types per UUID

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
they refer to, and SHALL be shown in readable units together with the exact number of bytes. The total SHALL count
an object that several caches hold only once, such as a zaaktype that is both in the cache of zaaktypes per UUID
and in the zaaktype list of its catalogus, so that the total is not larger than the memory the caches really use. Because measuring
walks through every cached object, only this page SHALL measure the memory usage: the pages
`/rest/admin/util/cache` and `/rest/admin/util/cache/zhps` show the number of entries but no memory usage. Like the
other admin util pages, the page SHALL require the 'beheren' permission.

#### Scenario: A beheerder opens the ZTC cache page
- **WHEN** a user with the 'beheren' permission opens `/rest/admin/util/cache/ztc`
- **THEN** the page shows the total estimated memory usage of all ZTC caches
- **AND** for each ZTC cache its estimated number of entries and its estimated memory usage

#### Scenario: Caches that hold the same objects
- **WHEN** the same zaaktypes are both in the cache of zaaktypes per UUID and in the zaaktype list of their
  catalogus
- **THEN** the total estimated memory usage counts these zaaktypes once, so it is less than the sum of the
  estimated memory usage per cache

#### Scenario: A beheerder opens the page with all caches
- **WHEN** a user with the 'beheren' permission opens `/rest/admin/util/cache`
- **THEN** the page shows the number of entries of each cache
- **AND** the system does not measure the memory usage of the caches

#### Scenario: A user without the 'beheren' permission
- **WHEN** a user without the 'beheren' permission opens `/rest/admin/util/cache/ztc`
- **THEN** the system refuses the request and does not measure the memory usage of the caches
