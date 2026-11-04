# Running ZAC locally on Kubernetes (kind)

You can run ZAC and the services it needs in a local Kubernetes cluster with [kind](https://kind.sigs.k8s.io/).
ZAC itself is installed with the [ZAC Helm chart](../../charts/zac), as in production.

Everything that the kind stack needs is in [scripts/kind](../../scripts/kind):

| Path                                                                         | What it holds                                                                                   |
|------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| [charts/zac-dev/values.yaml](../../scripts/kind/charts/zac-dev/values.yaml)  | The images of every service and the versions of the upstream Helm charts, which Renovate updates |
| [values/arm64.yaml](../../scripts/kind/values/arm64.yaml)                    | The arm64 images that replace those images on an arm64 host                                     |
| [values](../../scripts/kind/values)                                          | The settings of ZAC and of the components that have an upstream Helm chart                      |
| [charts/zac-dev](../../scripts/kind/charts/zac-dev)                          | A Helm chart for the services without an upstream chart, the seed Jobs and the configuration    |
| [seed](../../scripts/kind/seed)                                              | The seed data: the Keycloak realm, the Open Zaak database, the WireMock mappings and so on      |
| [ports.json](../../scripts/kind/ports.json)                                  | The host ports of the services                                                                  |

The components with a [Maykin Media Helm chart](https://github.com/maykinmedia/charts), such as Open Zaak and Open
Klant, are installed with that chart.

## Prerequisites

- [Docker Desktop](https://docs.docker.com/desktop/) or Docker Engine. Give Docker at least 12 GiB of memory.
- [kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Helm](https://helm.sh/docs/intro/install/)
- Python 3
- Optional: the [1Password CLI](https://developer.1password.com/docs/cli/) (`op`), with access to the 1Password vault
  `Dimpact`

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

Add these options for the optional services:

| Option | What it adds                                                                                                 |
|--------|--------------------------------------------------------------------------------------------------------------|
| `-o`   | Open Object, for example for the Dimpact productaanvraag flow.                                               |
| `-n`   | Open Notificaties. Open Zaak and Open Object then send their notifications to it, and it sends them to ZAC.  |
| `-a`   | Open Archiefbeheer.                                                                                          |
| `-f`   | Open Formulieren, with Open Object and Open Notificaties.                                                    |
| `-v`   | A local VNG Selectielijst API, with the Selectielijst 2020 data. Open Zaak and Open Archiefbeheer keep using the public Selectielijst API. |
| `-t`   | The test doubles of the integration tests: WireMock for SmartDocuments, KVK and BAG, GreenMail and Open Object. |
| `-d`   | Deletes the cluster and its data first, so that everything starts from the seed data again.                  |
| `-e`   | Does not use the 1Password CLI; see [Secrets from 1Password](#secrets-from-1password).                       |

The first start creates a kind cluster named `zac` and takes about 5 minutes, most of it to pull the images.
You can run the script again at any time, for example to switch between the modes, to add or remove optional
services or to start a newly built ZAC image. It updates the running cluster and keeps its data, and it removes the
optional services of the options that you leave out.

When ZAC is in the cluster, it is at http://localhost:8080. Log in with one of the
[test users](testing.md#local-test-users).

### Running ZAC from your IDE

Without `-z`, `-l` or `-b`, ZAC does not run in the cluster and the host ports 8080 and 9990 stay free. Start ZAC
from your IDE as described in [IDE configuration](ideConfig.md), with the environment variables of
[.env.example](../../.env.example). The stack then also runs the OPA and office converter services that ZAC needs
on the host ports 8181 and 8083; when ZAC runs in the cluster, it has its own, as in production.

Open Notificaties sends its notifications to a ZAC in your IDE at `http://host.docker.internal:8080`.

## Stopping the stack

| Command            | What it does                                                                    |
|--------------------|---------------------------------------------------------------------------------|
| `./stop-kind.sh`   | Stops the cluster and keeps its data. `./start-kind.sh` starts it again.        |
| `./stop-kind.sh -d`| Deletes the cluster and its data.                                               |

## Settings and secrets

### Settings of ZAC

[values/zac.yaml](../../scripts/kind/values/zac.yaml) holds the settings of ZAC in the cluster. These environment
variables override them, from your `.env` file or from the environment, with the environment first:

`AUTH_SSL_REQUIRED`, `BAG_API_CLIENT_MP_REST_URL`, `BAG_API_KEY`, `BRON_ORGANISATIE_RSIN`, `BRP_API_KEY`,
`BRP_API_KEY_HEADER`, `BRP_DOELBINDING_HEADER`, `BRP_DOELBINDING_PER_ZAAKTYPE`, `BRP_DOELBINDING_RAADPLEEGMET`,
`BRP_DOELBINDING_ZOEKMET`, `BRP_GEBRUIKER_HEADER`, `BRP_ORIGIN_OIN`, `BRP_ORIGIN_OIN_HEADER`,
`BRP_PROTOCOLLERING_ENABLED`, `BRP_TOEPASSING`, `BRP_TOEPASSING_HEADER`, `BRP_VERWERKINGSREGISTER`,
`BRP_VERWERKINGSREGISTER_EXT_WITH_ZAAKTYPE`, `BRP_VERWERKING_HEADER`, `GEMEENTE_CODE`, `GEMEENTE_MAIL`,
`GEMEENTE_NAAM`, `KVK_API_CLIENT_MP_REST_URL`, `KVK_API_KEY`, `MAX_FILE_SIZE_MB`, `MAX_IN_MEMORY_FILE_SIZE_MB`,
`SIGNALERINGEN_DELETE_OLDER_THAN_DAYS`, `SMARTDOCUMENTS_AUTHENTICATION`, `SMARTDOCUMENTS_CLIENT_MP_REST_URL`,
`SMARTDOCUMENTS_ENABLED`, `SMARTDOCUMENTS_FIXED_USER_NAME`, `SMTP_PASSWORD`, `SMTP_PORT`, `SMTP_SERVER`,
`SMTP_USERNAME`, `VERANTWOORDELIJKE_ORGANISATIE_RSIN` and `ZAC_INTERNAL_ENDPOINTS_API_KEY`.

The other settings of ZAC, such as the URLs of Open Zaak and Keycloak, point at the services in the cluster and
stay as they are. The `DOCKER_COMPOSE_<NAME>_EMAIL_ADDRESS` variables set the e-mail addresses of the test users and
groups in Keycloak. The settings of the other services are in [scripts/kind](../../scripts/kind) only.

### Generated passwords

The script generates the passwords of the stack when it creates the cluster, and keeps them in the Secret
`zac-dev-generated` of the cluster. They stay the same when you stop and start the cluster, and `-d` generates new
ones. These are:

- the passwords of the databases;
- the secret keys of Open Zaak, VNG Selectielijst and the other Django components;
- the password of the `admin` user of the Keycloak admin console (http://keycloak.localhost:8081/admin) and of the admin pages
  of Open Zaak, Open Klant, Open Object, Open Notificaties, Open Archiefbeheer and Open Formulieren. The script
  prints it when the stack is ready;
- the passwords of Solr and the office converter.

`DB_PASSWORD`, `SOLR_PASSWORD` and `OFFICE_CONVERTER_PASSWORD` in your `.env` file or in the environment replace the
generated password of the ZAC database, Solr and the office converter, so that a ZAC in your IDE with the settings of
`.env` can connect. When you run ZAC from your IDE and one of them is not set, the script prints the generated
password to use instead.

To read a generated password, for example that of the ZAC database:

```shell
kubectl --context kind-zac -n zac get secret zac-dev-generated -o jsonpath='{.data.db-password-zac}' | base64 -d
```

The client secrets, tokens and API keys in the seed data, such as those of the Keycloak realm and of the Open Zaak
applications, are fixed test values.

### Secrets from 1Password

`start-kind.sh` uses the 1Password CLI when it is installed and you have access to the
vault `Dimpact`: it runs with the environment variables of [.env.tpl](../../.env.tpl), with the secrets from
1Password, for example those of SmartDocuments and the SMTP server. Use `-e` to skip this and only use your `.env`
file and environment.

## Host ports

A small proxy container named `zac-ports` publishes the services of the cluster on these host ports, on
`127.0.0.1` only. The services of an option are only published with that option.

| Service                                       | Host port | Environment variable                   |
|-----------------------------------------------|-----------|----------------------------------------|
| ZAC (only when ZAC runs in the cluster)        | 8080      | `KIND_PORT_ZAC`                        |
| ZAC management and health (only with ZAC in the cluster)| 9990      | `KIND_PORT_ZAC_MANAGEMENT`             |
| Keycloak                                       | 8081      | `KIND_PORT_KEYCLOAK`                   |
| Keycloak management and health                 | 9001      | `KIND_PORT_KEYCLOAK_MANAGEMENT`        |
| Open Zaak                                      | 8001      | `KIND_PORT_OPENZAAK`                   |
| Open Klant                                     | 8002      | `KIND_PORT_OPENKLANT`                  |
| Open Notificaties (`-n`, `-f`)                 | 8003      | `KIND_PORT_OPENNOTIFICATIES`           |
| Open Archiefbeheer API and UI (`-a`)           | 8004, 8005 | `KIND_PORT_OPENARCHIEFBEHEER`, `KIND_PORT_OPENARCHIEFBEHEER_UI` |
| PABC                                           | 8006      | `KIND_PORT_PABC`                       |
| Open Formulieren, through nginx and directly (`-f`) | 8007, 8009 | `KIND_PORT_OPENFORMULIEREN`, `KIND_PORT_OPENFORMULIEREN_WEB` |
| Open Object (`-o`, `-t`, `-f`)                 | 8010      | `KIND_PORT_OPEN_OBJECT`                |
| VNG Selectielijst API, at `/api/v1/` (`-v`)    | 8011      | `KIND_PORT_VNG_SELECTIELIJST`          |
| Office converter                               | 8083      | `KIND_PORT_OFFICE_CONVERTER`           |
| OPA (only when ZAC runs outside the cluster)   | 8181      | `KIND_PORT_OPA`                        |
| Solr                                           | 8983      | `KIND_PORT_SOLR`                       |
| BRP personen mock and its WireMock             | 5010, 18084 | `KIND_PORT_BRP_PERSONEN_MOCK`, `KIND_PORT_BRP_PERSONEN_WIREMOCK` |
| SmartDocuments, KVK and BAG WireMock (`-t`)    | 18080, 18081, 18082 | `KIND_PORT_SMARTDOCUMENTS_WIREMOCK`, `KIND_PORT_KVK_WIREMOCK`, `KIND_PORT_BAG_WIREMOCK` |
| GreenMail API and SMTP (`-t`)                  | 18083, 2525 | `KIND_PORT_GREENMAIL_API`, `KIND_PORT_GREENMAIL_SMTP` |
| PostgreSQL databases of ZAC, Open Zaak, Open Object, Open Klant, Keycloak, Open Notificaties, Open Archiefbeheer, PABC, Open Formulieren and VNG Selectielijst | 54320, 54322, 54323, 54325, 54326, 54327, 54328, 54329, 54330, 54331 | `KIND_PORT_ZAC_DATABASE`, `KIND_PORT_OPENZAAK_DATABASE`, `KIND_PORT_OPEN_OBJECT_DATABASE`, `KIND_PORT_OPENKLANT_DATABASE`, `KIND_PORT_KEYCLOAK_DATABASE`, `KIND_PORT_OPENNOTIFICATIES_DATABASE`, `KIND_PORT_OPENARCHIEFBEHEER_DATABASE`, `KIND_PORT_PABC_DATABASE`, `KIND_PORT_OPENFORMULIEREN_DATABASE`, `KIND_PORT_VNG_SELECTIELIJST_DATABASE` |

All databases are on one PostgreSQL server. Each database port connects to that server, so the database name, user
and password select the database. The users and databases are listed in
[charts/zac-dev/values.yaml](../../scripts/kind/charts/zac-dev/values.yaml); the passwords are
[generated](#generated-passwords).

The script stops before it creates anything when one of these ports is already in use, and lists the ports. You can
then choose other ports:

- `KIND_PORT_<NAME>=<port>` moves one service, for example `KIND_PORT_ZAC=18888`.
- `KIND_PORT_<NAME>=0` does not publish that service, for example `KIND_PORT_GREENMAIL_SMTP=0`.
- `KIND_PORT_OFFSET=<offset>` adds the offset to every host port. Use an offset of 11205 or less, because the
  database ports must stay below 65536.

For example, to run a second cluster next to the first one:

```shell
KIND_CLUSTER_NAME=zac-2 KIND_PORT_OFFSET=11000 ./start-kind.sh -l
```

ZAC is then at http://localhost:19080 and Keycloak at http://keycloak.localhost:19081.

### Keycloak in the browser

Keycloak is at http://keycloak.localhost:8081, or at `keycloak.localhost` with the Keycloak host port that you chose.
Its admin console is at http://keycloak.localhost:8081/admin.

Keycloak puts this URL in its tokens and in the endpoints of its OpenID configuration, so your browser, the pods in
the cluster and a ZAC in your IDE must all reach Keycloak at the same URL. ZAC uses one of these endpoints to log
you out of Keycloak, from the server and not from your browser. A name under `localhost` makes this possible:

- Browsers, and the resolvers of macOS and of most Linux distributions, resolve every name under `localhost` to
  your own computer. You do not need an entry in `/etc/hosts`.
- In the cluster, CoreDNS resolves `keycloak.localhost` to the Keycloak Service, and that Service also listens on the
  Keycloak host port.

ZAC and PABC in the cluster call Keycloak at `http://keycloak:8080`, and a ZAC in your IDE can keep using
`AUTH_SERVER=http://host.docker.internal:8081` from `.env.example`, or use `http://keycloak.localhost:8081`.

If your computer does not resolve `keycloak.localhost`, for example on Linux without `nss-myhostname` or
`systemd-resolved`, add this line to `/etc/hosts`:

```
127.0.0.1 keycloak.localhost
```

Set `KEYCLOAK_HOSTNAME` to use another URL. The pods in the cluster only reach that URL when its host name contains a
dot.

## Memory and arm64

The stack without ZAC and without optional services takes about 4 GiB of memory, and ZAC adds up to 4 GiB.

On an arm64 host, such as a Mac with Apple Silicon, the stack uses the arm64 images of
[values/arm64.yaml](../../scripts/kind/values/arm64.yaml). Set `DOCKER_USE_ARM64_CONTAINERS=false` to use the
default images. Open Formulieren has no working arm64 image and runs emulated. The ZAC image in the registry is only
built for amd64, so on an arm64 host `-l` and `-b` start ZAC faster than `-z`.

## Inspecting the stack

The cluster has the kubectl context `kind-zac` and runs everything in the namespace `zac`.
The script makes `zac` the default namespace of that context:

```shell
kubectl --context kind-zac get pods
kubectl --context kind-zac logs deploy/zac --follow
```

The script writes the files that it generates, such as the Helm values, to `build/kind/<cluster name>`.

When a service does not become ready in time, the script stops and shows the pods and the latest warning events.
A cluster that an earlier version of the script made can keep data that no longer fits, for example in the Postgres
volume. Start again with `-d` to delete the cluster first.
