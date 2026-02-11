# Solr indexing in ZAC

ZAC uses the Solr search engine to be able to quickly retrieve and search in zaak-related information.
The Solr index is used in multiple places within ZAC, most notably for the various 'workload' ('werkvoorraad') screens.

The Solr index contains both data from Open Zaak (zaak data) as well as 'ZAC-internal' data (such as task data).

## ZAC startup

When ZAC starts up, it will check if the ZAC Solr index is available. If not, it will try to create the Solr index.
ZAC requires that the `zac` Solr core is available. If it is not, ZAC will fail to start.

## Solr indexing flow

The standard flow for Solr indexing in ZAC is as follows:

1. ZAC either receives a specific notification from Open Notifications for data
which is part of the Solr index (e.g. a new zaak is created) or ZAC internally identified that certain
data in the Solr index is stale and needs to be re-indexed (e.g. a task is created).
2. ZAC indexes the relevant data in Solr.

This is illustrated in the following sequence diagram showing the example of a new zaak being created:

```mermaid
sequenceDiagram
    participant OpenNotificaties
    box ZAC landscape
    participant ZAC
    participant Solr
    end

    OpenNotificaties->>+ZAC: New zaak created
    ZAC->>+Solr: Index zaak data
```

## Reindexing

Besides the standard flow for Solr indexing, ZAC also has a 'reindexing' feature, which can be used to (re)build Solr index data per 'ZAC Solr data type' (zaken, taken, documents). 
By running this for all supported data types, you effectively reindex all data in the `zac` Solr core.
This can be useful in case of issues with the Solr index, or when the Solr schema has been updated and all data for one or more types needs to be reindexed to be compatible with the new schema.

When the reindexing feature is triggered for a specific data type, ZAC will first delete all data in the Solr index that belong to that type and then reindex the corresponding data from Open Zaak and ZAC-internal data.
Be aware that this can take considerable time, depending on the amount of data that needs to be indexed for the selected type(s).

Reindexing can be started by using the [reindexing script](../../scripts/solr/reindex-zac-solr-data.sh) or by calling the internal ZAC reindex API endpoint directly. 
The script invokes this endpoint once per data type and the endpoint is protected with an `X-API-KEY` header, which the script also requires and forwards.
