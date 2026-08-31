# Test data scripts

This directory contains scripts to generate test data in a local Docker Compose ZAC stack: zaaktypes,
zaakafhandelparameters, zaken and documents. They are used for manual testing and for load/performance testing.

`zac_client.py`, `zac_testdata.py` and `zac_reporting.py` are shared modules (HTTP/auth, the zaaktype/document
catalogue, and reporting helpers) used by the scripts below; they are not run directly.

## create-zaaktypes.py

Creates a batch of test zaaktypes directly in the local Open Zaak PostgreSQL database, by rendering
[`open-zaak/zaaktype-template.sql`](open-zaak/zaaktype-template.sql) once per zaaktype and executing the result.
Each rendered zaaktype gets its own resultaattypen, statustypen, roltypen and zaaktype-informatieobjecttype, all
with freshly generated UUIDs.

For each zaaktype it also:
1. registers a matching entity type in the local PABC PostgreSQL database, by rendering
   [`pabc/add-zaaktype-template.sql`](pabc/add-zaaktype-template.sql) and executing the result;
2. configures the zaaktype's zaakafhandelparameters in ZAC, by calling
   [`configure-zaakafhandelparameters.py`](configure-zaakafhandelparameters.py) (unless `--skip-config` is passed).

When `--add-extra-versions N` is given, each zaaktype is preceded by `N` additional (superseded) versions: for
every extra version, a zaaktype is created in Open Zaak just like the current one, and then immediately marked as
an older version by rendering
[`open-zaak/zaaktype-version-update-template.sql`](open-zaak/zaaktype-version-update-template.sql). Extra versions
are not registered in PABC and are not configured in ZAC.

### Prerequisites
* Python 3.10+
* `psql` on `PATH`
* the Open Zaak and PABC Docker Compose services running (and ZAC + Keycloak too, unless `--skip-config` is used)

### Usage
```shell
./create-zaaktypes.py [--count N] [--start-number N]
                       [--host HOST] [--port PORT] [--dbname DBNAME] [--user USER] [--password PASSWORD]
                       [--template PATH] [--add-extra-versions N] [--version-update-template PATH]
                       [--pabc-host HOST] [--pabc-port PORT] [--pabc-dbname DBNAME] [--pabc-user USER]
                       [--pabc-password PASSWORD] [--pabc-template PATH]
                       [--zac-url URL] [--keycloak-url URL] [--skip-config]
```

| Option                      | Default                                           | Description                                                                           |
|------------------------------|---------------------------------------------------|----------------------------------------------------------------------------------------|
| `--count N`                  | `10`                                              | number of zaaktypes to create                                                         |
| `--start-number N`           | `1`                                               | 1-based ZAAKTYPE_NUMBER to start numbering from                                       |
| `--host`                     | `localhost`                                       | Open Zaak database host                                                               |
| `--port`                     | `54322`                                           | Open Zaak database port                                                               |
| `--dbname`                   | `openzaak`                                        | Open Zaak database name                                                               |
| `--user`                     | `openzaak`                                        | Open Zaak database user                                                               |
| `--password`                 | `openzaak`                                        | Open Zaak database password                                                           |
| `--template`                 | `open-zaak/zaaktype-template.sql`                 | path to the Open Zaak SQL template                                                    |
| `--add-extra-versions N`     | off                                               | for each zaaktype, also create N (1-10) extra superseded versions in Open Zaak first  |
| `--version-update-template`  | `open-zaak/zaaktype-version-update-template.sql`  | path to the zaaktype-version-update SQL template                                      |
| `--pabc-host`                | `localhost`                                       | PABC database host                                                                    |
| `--pabc-port`                | `54329`                                           | PABC database port                                                                    |
| `--pabc-dbname`              | `Pabc`                                            | PABC database name                                                                    |
| `--pabc-user`                | `pabc`                                            | PABC database user                                                                    |
| `--pabc-password`            | `pabc`                                            | PABC database password                                                                |
| `--pabc-template`            | `pabc/add-zaaktype-template.sql`                  | path to the PABC SQL template                                                         |
| `--zac-url`                  | `http://localhost:8080`                           | ZAC base URL, passed through to `configure-zaakafhandelparameters.py`                 |
| `--keycloak-url`             | `http://localhost:8081`                           | Keycloak base URL, passed through to `configure-zaakafhandelparameters.py`            |
| `--skip-config`              | off                                               | skip configuring zaakafhandelparameters in ZAC after creating each zaaktype           |

The database and PABC connection options only need to be overridden when the local stack deviates from the
defaults in `docker-compose.yaml`.

### Examples

Create the default number of zaaktypes (10):
```shell
./create-zaaktypes.py
```

Create 500 zaaktypes, for load testing:
```shell
./create-zaaktypes.py --count 500
```

Note that each of the 500 zaaktypes is created, registered in PABC and configured in ZAC one at a time, so a run
like this takes a while — expect it to run for several minutes.

Create zaaktypes in Open Zaak and PABC only, without touching ZAC (e.g. because ZAC isn't running yet):
```shell
./create-zaaktypes.py --count 500 --skip-config
```

Create zaaktypes that each have 3 extra superseded versions in Open Zaak (only the current version is registered
in PABC and configured in ZAC):
```shell
./create-zaaktypes.py --add-extra-versions 3
```

## Other scripts

* [`create-zaak.py`](create-zaak.py) — creates zaak(en) of an existing CMMN or BPMN test zaaktype, optionally
  uploading test documents to each.
* [`create-load.py`](create-load.py) — end-to-end local load/performance test: uploads a BPMN process definition,
  configures zaakafhandelparameters for the 7 built-in test zaaktypes, then creates many zaken concurrently.
* [`configure-zaakafhandelparameters.py`](configure-zaakafhandelparameters.py) — configures a single zaaktype's
  zaakafhandelparameters in ZAC (default group, CMMN case type, zaakNietOntvankelijkResultaattype, zaakAfzenders
  and humanTaskParameters). Used by `create-zaaktypes.py` above, and can also be run standalone.

Run any script with `--help` for its full set of options.
