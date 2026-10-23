# Running ZAC locally on Kubernetes (kind)

As an alternative to the [Docker Compose setup](installDockerCompose.md), you can run ZAC and the services it needs
in a local Kubernetes cluster with [kind](https://kind.sigs.k8s.io/). ZAC itself is installed with the
[ZAC Helm chart](../../charts/zac), as in production.

The kind stack uses [docker-compose.yaml](../../docker-compose.yaml) as its source of truth. When it starts, it reads
the images, credentials and settings of the Docker Compose services, including the values from your `.env` file and
`docker-compose.override.yml`, and loads the same seed data from
[scripts/docker-compose/imports](../../scripts/docker-compose/imports). It publishes the services on the same host
ports as Docker Compose. Your `.env` file, the frontend development server, Bruno and the
[test data scripts](../../scripts/test-data) work unchanged.

The scripts and the Helm chart for the services without an upstream chart are in [scripts/kind](../../scripts/kind).

## Prerequisites

- [Docker Desktop](https://docs.docker.com/desktop/) or Docker Engine with the Docker Compose plugin. Give Docker
  at least 12 GiB of memory.
- [kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Helm](https://helm.sh/docs/intro/install/)
- Python 3 and `openssl`

On macOS you can install the tools with Homebrew:

```shell
brew install kind kubectl helm
```

## Starting the stack

From the root folder of this repository:

| Command                | What starts                                                                    |
|------------------------|--------------------------------------------------------------------------------|
| `./start-kind.sh`      | All services that ZAC needs, but not ZAC. Run ZAC from your IDE.               |
| `./start-kind.sh -z`   | The services and ZAC from the registry (`ghcr.io/infonl/zaakafhandelcomponent:latest`, or `$ZAC_DOCKER_IMAGE`). |
| `./start-kind.sh -l`   | The services and the ZAC image that you built locally (`ghcr.io/infonl/zaakafhandelcomponent:dev`). |
| `./start-kind.sh -b`   | Builds the ZAC image with `./gradlew buildDockerImage`, then does the same as `-l`. |
| `./start-kind.sh -d …` | Deletes the cluster and its data first, so that everything starts from the seed data again. |

The first start creates a kind cluster named `zac` and takes about 5 minutes, most of it to pull the images.
You can run the script again at any time, for example to switch between the modes or to start a newly built ZAC
image: it updates the running cluster and keeps its data.

When ZAC is in the cluster, it is at http://localhost:8080. Log in with one of the
[test users](testing.md#local-test-users).

### Running ZAC from your IDE

Without `-z`, `-l` or `-b`, ZAC does not run in the cluster and the host ports 8080 and 9990 stay free. Start ZAC
from your IDE as described in [IDE configuration](ideConfig.md), with the environment variables of
[.env.example](../../.env.example). The stack then also runs the OPA and office converter services that ZAC needs
on the host ports 8181 and 8083; when ZAC runs in the cluster, it has its own, as in production.

## Stopping the stack

| Command            | What it does                                                                    |
|--------------------|---------------------------------------------------------------------------------|
| `./stop-kind.sh`   | Stops the cluster and keeps its data. `./start-kind.sh` starts it again.        |
| `./stop-kind.sh -d`| Deletes the cluster and its data.                                               |

## Host ports

A small proxy container named `zac-ports` publishes the services of the cluster on these host ports, on
`127.0.0.1` only. These are the host ports of Docker Compose.

| Service                                       | Host port | Environment variable                   |
|-----------------------------------------------|-----------|----------------------------------------|
| ZAC (only when ZAC runs in the cluster)        | 8080      | `KIND_PORT_ZAC`                        |
| ZAC management and health (only with ZAC in the cluster)| 9990      | `KIND_PORT_ZAC_MANAGEMENT`             |
| Keycloak                                       | 8081      | `KIND_PORT_KEYCLOAK`                   |
| Keycloak management and health                 | 9001      | `KIND_PORT_KEYCLOAK_MANAGEMENT`        |
| Open Zaak                                      | 8001      | `KIND_PORT_OPENZAAK`                   |
| Open Klant                                     | 8002      | `KIND_PORT_OPENKLANT`                  |
| PABC                                           | 8006      | `KIND_PORT_PABC`                       |
| Open Object                                    | 8010      | `KIND_PORT_OPEN_OBJECT`                |
| Office converter                               | 8083      | `KIND_PORT_OFFICE_CONVERTER`           |
| OPA (only when ZAC runs outside the cluster)   | 8181      | `KIND_PORT_OPA`                        |
| Solr                                           | 8983      | `KIND_PORT_SOLR`                       |
| BRP personen mock                              | 5010      | `KIND_PORT_BRP_PERSONEN_MOCK`          |
| SmartDocuments, KVK, BAG and BRP WireMock      | 18080, 18081, 18082, 18084 | `KIND_PORT_SMARTDOCUMENTS_WIREMOCK`, `KIND_PORT_KVK_WIREMOCK`, `KIND_PORT_BAG_WIREMOCK`, `KIND_PORT_BRP_PERSONEN_WIREMOCK` |
| GreenMail API and SMTP                         | 18083, 25 | `KIND_PORT_GREENMAIL_API`, `KIND_PORT_GREENMAIL_SMTP` |
| PostgreSQL databases of ZAC, Open Zaak, Open Object, Open Klant, Keycloak and PABC | 54320, 54322, 54323, 54325, 54326, 54329 | `KIND_PORT_ZAC_DATABASE`, `KIND_PORT_OPENZAAK_DATABASE`, `KIND_PORT_OPEN_OBJECT_DATABASE`, `KIND_PORT_OPENKLANT_DATABASE`, `KIND_PORT_KEYCLOAK_DATABASE`, `KIND_PORT_PABC_DATABASE` |

All databases are on one PostgreSQL server. Each database port connects to that server, so the database name, user
and password of [docker-compose.yaml](../../docker-compose.yaml) select the database.

The script stops before it creates anything when one of these ports is already in use, and lists the ports. You can
then choose other ports:

- `KIND_PORT_<NAME>=<port>` moves one service, for example `KIND_PORT_ZAC=18888`.
- `KIND_PORT_<NAME>=0` does not publish that service, for example `KIND_PORT_GREENMAIL_SMTP=0`.
- `KIND_PORT_OFFSET=<offset>` adds the offset to every host port. Use an offset of 11206 or less, because the
  database ports must stay below 65536.

For example, to run the kind stack next to a running Docker Compose stack:

```shell
KIND_CLUSTER_NAME=zac-kind KIND_PORT_OFFSET=11000 ./start-kind.sh -l
```

ZAC is then at http://localhost:19080 and Keycloak at http://localhost:19081. Keycloak accepts the login
redirect to the ZAC port that the cluster had when it was created; after you change the ZAC port, delete the
cluster with `-d`.

### Keycloak in the browser

Keycloak issues its tokens for `http://localhost:<Keycloak host port>`, the URL that your browser uses. ZAC and
PABC in the cluster call Keycloak at `http://keycloak:8080`, and a ZAC in your IDE can keep using
`AUTH_SERVER=http://host.docker.internal:8081` from `.env.example`. Unlike Docker Compose, you do not need a
`host.docker.internal` entry in `/etc/hosts` to log in. Set `KEYCLOAK_HOSTNAME` to use another URL.

## Credentials

The credentials are those of [docker-compose.yaml](../../docker-compose.yaml), with the values from your `.env`
file. For example, the Keycloak admin console at http://localhost:8081/admin, the Open Zaak admin at
http://localhost:8001/admin and the Open Klant admin at http://localhost:8002/admin use the same users as in Docker
Compose. The only generated password is that of the `postgres` superuser, which nothing outside the cluster uses.

## Memory and arm64

The stack without ZAC takes about 5 GiB of memory, and ZAC adds up to 4 GiB.

On an arm64 host, such as a Mac with Apple Silicon, the stack uses the arm64 images of
[docker-compose.arm64-override.yaml](../../docker-compose.arm64-override.yaml). Set
`DOCKER_USE_ARM64_CONTAINERS=false` to use the default images. The ZAC image in the registry is only built for
amd64, so on an arm64 host `-l` and `-b` start ZAC faster than `-z`.

## Inspecting the stack

The cluster has the kubectl context `kind-zac` and runs everything in the namespace `zac`:

```shell
kubectl --context kind-zac -n zac get pods
kubectl --context kind-zac -n zac logs deploy/zac --follow
```

The script writes the files that it generates, such as the Helm values, to `build/kind/<cluster name>`.

## Differences with Docker Compose

- Only the services that ZAC needs run, with Open Object and the test doubles always included. The Docker Compose
  options for metrics (`-m`), Open Notificaties (`-n`), Open Archiefbeheer (`-a`) and Open Formulieren (`-f`) have
  no kind equivalent yet.
- One PostgreSQL server holds all databases.
- `start-kind.sh` does not use the 1Password CLI. To use secrets from 1Password, start it with
  `op run --env-file=.env.tpl -- ./start-kind.sh`.
