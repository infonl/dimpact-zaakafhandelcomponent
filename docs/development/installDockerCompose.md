# Podman Compose setup

The ZAC Podman Compose setup runs various services required by ZAC and optionally can also run ZAC itself as a container.
It was created to be able to run ZAC locally for development and testing purposes.
For general ZAC installation instructions please see the [INSTALL.md](INSTALL.md) file.

The setup consists of a [docker-compose.yaml](../../docker-compose.yaml) file (the filename is kept for compose tooling
auto-discovery compatibility) as well as various data import scripts.

This setup was initially based on https://github.com/generiekzaakafhandelcomponent/gzac-docker-compose and credits go out to Valtimo for this.
It was extended and made specific for the needs of ZAC.

## Prerequisites

- [Podman](https://podman.io/docs/installation), our supported container runtime. Docker with `docker compose` may still work as an unsupported fallback. Validated with Podman 6.0.2.
- The `podman compose` command (bundled with recent Podman versions).
- [1Password CLI extensions](https://developer.1password.com/docs/cli/) (optional)

### macOS / Windows (`podman machine`)

The default `podman machine` allocation (2GiB RAM) is far too small for this stack (Solr, Keycloak, four PostgreSQL
instances, Open Zaak, Open Klant, PABC, etc.). 8GiB is enough to bring the default profile up at idle, but running
the full integration test suite under load needs more — **16GiB RAM / 8 CPU is the validated minimum**:

```shell
podman machine set --memory 16384 --cpus 8
```

Rootless Podman also defaults to disallowing privileged host ports (<1024), which the `itest` profile's `greenmail`
service needs (SMTP port 25). Lower the unprivileged port floor inside the machine and persist it across restarts:

```shell
podman machine ssh sudo sysctl -w net.ipv4.ip_unprivileged_port_start=25
podman machine ssh "echo 'net.ipv4.ip_unprivileged_port_start=25' | sudo tee /etc/sysctl.d/99-podman-unprivileged-ports.conf"
```

On Linux (no `podman machine`), apply the same sysctl directly on the host instead.

### WSL2
Make sure you clone the repository to the WSL filesystem itself

### Linux with iptables
- Run [setup-linux.sh script](../../scripts/docker-compose/setup-linux.sh)

### Linux with nftables

The following is only relevant if you are using the unsupported Docker fallback; Podman's default
(netavark) network backend does not require this manual nftables/iptables configuration.

- Run [setup-linux.sh script](../../scripts/docker-compose/setup-linux.sh)
- Uninstall docker.io (on Debian and derivatives): `sudo apt remove $(dpkg --get-selections docker.io docker-compose docker-doc podman-docker containerd runc | cut -f1)`
- Install docker-ce, minimum version >= 29.0.0 (check: https://docs.docker.com/engine/install/debian/#install-using-the-repository)
- Create or update `/etc/docker/daemon.json` to contain at least:
```json
{
   "firewall-backend": "nftables"
}
```
- Ensure the nftables configuration, most likely `/etc/nftables.conf`, contains at least:
```nft
#!/usr/sbin/nft -f

# Clean
flush ruleset

# IPv4 filtering
table filter {
    chain input {
        type filter hook input priority 0;
        ct state invalid counter drop;
        ct state { established, related } counter accept;
        iif lo accept;
        # PUT YOUR FIREWALL RULES HERE
    }
    chain forward {
        type filter hook forward priority filter;
        policy drop;
        ct state invalid counter drop;
        ct state { established, related } counter accept;
        iifname "docker0" oifname "docker0" counter accept comment "Docker default bridge ICC";
        iifname "br-*" oifname "br-*" counter accept comment "Docker Compose / user-defined bridge ICC";
    }
}
```
- The docker service should start after nftables and restart on nftables restart:
```bash
mkdir systemd/system/docker.service.d
# If your distro is not Debian: check that the names in After and PartOf are correct !!
cat > systemd/system/docker.service.d/override.conf << EOF
[Unit]
After=network-online.target nss-lookup.target docker.socket nftables.service containerd.service time-set.target
PartOf=nftables.service
EOF
systemctl daemon-reload
systemctl restart nftables
systemctl restart docker
```

## Starting Podman Compose

### Running all required services but not ZAC itself

This starts up all required services (like Keycloak, Open Zaak, etc) but does not start ZAC itself.
From the root folder of this repository execute the following command:

```
./start-docker-compose.sh
```

This will run Podman Compose (using `podman compose up -d`) and uses the 1Password CLI extensions
to retrieve certain environment variables from 1Password.

Please see the [Compose file](../../docker-compose.yaml) for the services that will be started.
Note that it may take a while for all services to start up completely.
You can check the logs of the various containers if you want to see the status.

### Other options

Please consult the help of the `start-docker-compose.sh` script for more options including the option to also
start up ZAC or even build the ZAC container image first beforehand:

```
./start-docker-compose.sh -h
```

### Using arm64 containers on a Mac

If you want to run arm64 containers, you can set the environment variable `DOCKER_USE_ARM64_CONTAINERS=true`
before starting the shell script:

```
DOCKER_USE_ARM64_CONTAINERS=true ./start-docker-compose.sh

```

You can also add this environment variable to your shell for convenience by adding to your
local `~/.zshrc`:

```
# ZAC
export DOCKER_USE_ARM64_CONTAINERS=true
```

### Notes

#### Using the latest version of ZAC

Currently, our ZAC Compose file contains a reference to a specific version of the ZAC container image.
In order to use the latest ZAC image you can specify a ZAC container image by setting the `ZAC_DOCKER_IMAGE`
environment variable.
You can find the latest released version of the ZAC container image on:
https://github.com/infonl/dimpact-zaakafhandelcomponent/pkgs/container/zaakafhandelcomponent

#### ZAC container logs

Note that it takes some time for ZAC to start up completely. You can see progress by checking the ZAC container logs:

```
podman logs -f zac
```

## The various containers

This section contains some specific information about some of the containers used in our Compose setup.

### PostgreSQL ZAC database

ZAC requires a PostgreSQL database with two database schemas. This is automatically created by the Compose file.
If you need to manually insert or change data in the ZAC database:

1. Using a PostgreSQL database client connect to the ZAC database using `jdbc:postgresql://localhost:54320/zac`
2. Log in using the database admin credentials that can be found in the Compose file.
3. You should see the following database schemas:
    - `zaakafhandelcomponent`
    - `flowable`
4. After ZAC has started up the first time it should have created the required database tables and initial data.

### Keycloak

The Keycloak configuration required by ZAC is automatically imported using the included JSON realm file.

To log in to the Keycloak Admin Console:

1. Go to: http://localhost:8081/auth/admin
2. Log in with the Keycloak admin credentials that can be found in the Docker Compose file.

ZAC uses the imported `zaakafhandelcomponent` Keycloak realm.

After making changes in Keycloak you can make a new realm export thereby overriding the existing `zaakafhandelcomponent-realm.json` file to
be automatically imported. Because we include our test users in the realm this can unfortunately only be done using a 
stand-alone Keycloak instance on your computer (which you downloaded and installed locally) and the Keycloak command line.
For example to export the ZAC Keycloak realm including the users do the following:

```
<KEYCLOAK_INSTALL_DIR>/bin/kc.sh export --dir <ZAC_GIT_REPO_DIR>/scripts/docker-compose/imports/keycloak --users realm_file --realm zaakafhandelcomponent
```

Please see https://www.keycloak.org/server/importExport for details.

When you do so beware of the following:

1. After you have exported the realm JSON file make the following manual changes in the file:
   1. Set the value of the `secret` attribute in the `zaakafhandelcomponent` client configuration to: `keycloakZaakafhandelcomponentClientSecret`.

#### Roles

All required roles are already included in the Keycloak realm. No need to create them manually.

#### Test users

Test users are imported into Keycloak on startup using the `zaakafhandelcomponent-realm.json` file.

For a ZAC admin the following user roles are required:
 - `raadpleger`
 - `behandelaar`
 - `coordinator`
 - `recordmanager`
 - `beheerder`
 - `domein_elk_zaaktype`

### Open Klant

Basic configuration required by ZAC is automatically imported into the Open Klant database from the Compose file.
Also, a superuser account for the Open Klant UI on http://localhost:8002 is created automatically with username 'admin' and password 'admin'.

### Open Formulieren

To test the productaanvraag flow end-to-end locally (form submission in Open Formulieren → Objecten API → Open Zaak → Open Notificaties → ZAC), start the stack with the `-f` flag:

```
./start-docker-compose.sh -f
```

This automatically also starts the Objecten API and Open Notificaties services (they are required for the flow and are included automatically via the `openformulieren` profile).

The Open Formulieren admin UI is available at http://localhost:8007/admin/ (username: `admin`, password: `admin`).
Direct access to the Open Formulieren web container is also available at http://localhost:8009/admin/.

The `openformulieren-init` container automatically registers all backend services (Open Zaak APIs, Objecten API, Open Klant) in Open Formulieren on first start.
After the stack is up, you will still need to configure the following in the Open Formulieren admin UI before you can test the productaanvraag flow:
- Create an **Objects API group** at http://localhost:8007/admin/registrations_objects_api/objectsapigroupconfig/add/ — select the pre-configured services and fill in the catalogue domain and RSIN from your local Open Zaak instance.
- Create a **ZGW API group** at http://localhost:8007/admin/zgw_apis/zgwapigroupconfig/add/ — same catalogue values.
- Create a form with an **Objects API** or **ZGW** registration backend pointing to the group you just configured.

## Stopping

1. Stop ZAC (only if you are running ZAC separately and not as part of the Podman Compose setup)
2. Stop all containers by executing the command: `./stop-docker-compose.sh` from the root folder of this project.

## Cleaning up

We use Podman volumes to persist data between restarts of certain containers in order to speed up
subsequent startups.

Sometimes it is needed to clean up these volumes to start with a clean slate.
To do so run the Podman Compose start script with the `-d` option:

```
./start-docker-compose.sh -d
```
