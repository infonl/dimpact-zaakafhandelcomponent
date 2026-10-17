# Managing the Solr search engine

ZAC uses Solr as the search engine. It is used both for search functionality and for lists like the 
various 'werkvoorraden' in ZAC. It is run as a separate Docker container in the local ZAC Docker Compose setup
and as a separate service in the Kubernetes setup.

The ZAC Solr index contains the following ZAC object types:
- zaken
- tasks
- documents

## Update the Solr search index manually

When running ZAC locally (and not in Kubernetes) the ZAC Solr search index is not automatically regularly updated.

In order to see content in e.g. the 'werklijsten' in ZAC you will need to update the Solr search index manually whenever you have changed relevant content.

You can use the [Solr indexing script](../../scripts/solr/reindex-zac-solr-data.sh) to reindex data in the Solr index.

## Trigger ZAC to reindex the Solr search index on startup

For some code changes in ZAC the Solr index needs to be reindexed for a certain object type.
This is the case for example when the ZAC Solr schema it updated.

In order to have ZAC reindex the Solr index on startup do the following:
1. Add a new ZAC 'Solr schema update' class to the `net.atos.zac.solr.schema` package.
2. Implement the `getTeHerindexerenZoekObjectTypes` method in this class so that it updates the 
correct object type.
3. If required add unit and/or integration tests.

Now when ZAC is deployed it will check on startup in Solr the current version of the ZAC Solr schema 
and if it is behind (which it is in this case) it will automatically update it to the latest version
by running the Solr schema update which was created.
