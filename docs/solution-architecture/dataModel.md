# ZAC data model

The ZAC data model consists of the following data stores:

- PostgreSQL relational database
- File system
- In-memory data

## PostgreSQL relational database

ZAC uses the following database schemas:

| Schema name                | Description                                                                                                                       |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| zaakafhandelcomponent       | The main ZAC application schema. Contains the main ZAC data model tables.                                                         |
| flowable                    | The Flowable process engine schema. Contains the Flowable process engine tables used for CMMN and BPMN process automation in ZAC. |

Both schemas need to exist before ZAC can start up.
The database tables for both schemas are created automatically by ZAC when it starts up for the first time, 
and are updated automatically when ZAC is updated to a new version.
ZAC uses [Flyway](https://flywaydb.org/) for database schema versioning and updates.
These two database schemas are [configured as data sources in the WildFly application server](../../src/main/resources/wildfly/configure-wildfly.cli) used by ZAC.

## File system

- ZAC itself does not use a persistent file system.
- [The ZAC Solr search engine](solrArchitecture.md) (part of the overall ZAC application but a separate runtime) stores its search index on the file system.

ZAC provides an internal endpoint to recreate the Solr index from the source data (both external ZGW data and
internal ZAC) when needed. Be aware that this will take considerable time depending on the amount of data to be indexed.

## In-memory data

The following data is stored in memory in ZAC:

| Data type         | Description                                                                                                                                                                           |
|-------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Session data      | Data related to a logged-in user session. When a user logs in (using OIDC (OpenID Connect)) a session is created and session data is stored in memory for performance reasons. Should session data get lost, for whatever reason, it is automatically recreated.|
| Cached data       | Certain data, mostly specific external ZGW data, is cached for performance optimization. Caching this data is needed in order to achieve an acceptable level of user experience in ZAC. |

Caching is implemented using the Infinispan caching framework as provided by the WildFly application server used by ZAC. 
When this cache is empty, the first user using related functionality in ZAC will have a slower user experience.
There is currently no way to automatically fill the cache.

ZAC provides an internal endpoint to clear specific ZAC caches when needed.

