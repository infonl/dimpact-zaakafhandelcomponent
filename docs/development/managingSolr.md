# Managing the Solr search engine

ZAC uses Solr as the search engine. It is used both for search functionality and for lists like the 
various 'werkvoorraden' in ZAC. It is run as a separate Docker container in the local ZAC Docker Compose setup
and as a separate service in the Kubernetes setup.

The ZAC Solr index contains the following ZAC object types:
- zaken
- tasks
- documents

## Solr authentication

Solr rejects unauthenticated requests. It uses the
[Solr basic authentication plugin](https://solr.apache.org/guide/solr/latest/deployment-guide/basic-authentication-plugin.html),
configured through a `security.json` file, and ZAC authenticates every request it makes to Solr with the
credentials from the `SOLR_USERNAME` and `SOLR_PASSWORD` environment variables.
ZAC fails to start when these are not set.

### Local Docker Compose setup

The local Solr container installs [security.json](../../scripts/docker-compose/imports/solr/security.json)
on startup. It contains the developer credentials `zac` / `fakeSolrPassword`, which are also the defaults for
`SOLR_USERNAME` and `SOLR_PASSWORD` in [docker-compose.yaml](../../docker-compose.yaml).
Use the same credentials to log in to the Solr admin UI on http://localhost:8983.

To use different credentials, set `SOLR_USERNAME` and `SOLR_PASSWORD` in your `.env` file and replace the
`credentials` and `user-role` entries in `security.json`. Solr does not store the password itself but
`base64(sha256(sha256(salt + password)))` followed by a space and `base64(salt)`, which you can generate with:

```shell
python3 -c "
import base64, hashlib, os
salt = os.urandom(32)
password = b'yourPassword'
print(base64.b64encode(hashlib.sha256(hashlib.sha256(salt + password).digest()).digest()).decode() + ' ' + base64.b64encode(salt).decode())
"
```

### Kubernetes setup

Set `solr.username` and `solr.password` in the Helm values. They end up in the ZAC Kubernetes secret,
and ZAC authenticates every Solr request with them.

For an external Solr instance (`solr.url` in the Helm values), configure the matching user in the
`security.json` of that Solr instance yourself.

The Solr the chart deploys through the Solr operator does not require authentication yet, so it
ignores the credentials ZAC sends. That Solr is only reachable from inside the cluster; it has no
ingress and no external addressability. Turning authentication on there means giving ZAC an account
that may read and edit the schema and update the index, which the operator's own generated account
may not, so it needs a `security.json` of our own rather than the one the operator bootstraps.

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

## Trigger a reindex on a running environment, without a restart

A Solr schema update that adds fields but deliberately reindexes nothing at startup (an empty
`teHerindexerenZoekObjectTypes`, as used by `SolrSchemaV8` and `SolrSchemaV9`) leaves the new fields empty on
existing documents until a reindex is run by hand. That reindex needs no extra tooling:
`IndexingAdminRestService` exposes it as an internal endpoint.

- `GET /rest/internal/indexeren/herindexeren/{type}` reindexes one `ZoekObjectType` (`ZAAK`, `TAAK` or
  `DOCUMENT`)
- `GET /rest/internal/indexeren/herindexeren` reindexes everything

Both run asynchronously and answer `202 Accepted` immediately, or `409 Conflict` when a reindex of that type
is already running. They are `@InternalEndpoint`s: they are not reachable from the frontend and require the
API key checked by `ZacApiKeyAuthFilter`.

Reindex per type rather than all at once, in the order `ZAAK`, `TAAK`, `DOCUMENT`, so that a long-running
document reindex does not delay the zaak rows, which are the ones users notice first.

### Deployment prerequisites for zaakspecifieke autorisatie

`SolrSchemaV9` adds the medewerkers individually authorised for a zaak to the zaak, taak and document rows,
which is what keeps a zaakspecifiek geautoriseerde zaak visible to its own behandelaar in werklijsten and
zoekresultaten. Until the reindex above has run, existing rows carry none, which the filter query reads as
"not authorised" - it fails closed, never open.

Separately, `recordmanager` and `beheerder` reach zaakspecifiek geautoriseerde zaken only by being granted the
`zaakspecifiek_geautoriseerd` application role for the zaaktype in PABC; there is no rule for those roles in
the OPA policies. Configuring that mapping per environment is a prerequisite, not a nicety: without it a
flagged zaak whose behandelaar is removed outside ZAC is reachable by nobody.
