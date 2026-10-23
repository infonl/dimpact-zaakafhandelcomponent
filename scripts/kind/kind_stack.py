#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
"""Helpers for scripts/kind/up.sh.

  ports  <ports.json> <zac|ide> <output dir> <node host name> <proxy container name>
         Resolves the host ports, fails when one of them is taken, and writes the nginx configuration and the
         'docker run' port arguments of the container that publishes the NodePorts on the host.
  values <compose config json> <output dir> <zac|ide>
         Writes the Helm values and the Secrets of the stack, taken from the resolved docker-compose.yaml so that
         the kind stack and the Docker Compose stack share one source for images, credentials and settings.
  wait   <kube context> <namespace> <timeout seconds> <node start time> [<deployment>...]
         Waits until the pods of the deployments, or of all deployments, are ready, ignoring a readiness from
         before the node started:
         right after a stopped node starts again, its pods still report the readiness they had when it stopped.
"""
import json
import os
import re
import socket
import subprocess
import sys
import time
from datetime import datetime

MAXIMUM_PORT = 65535
REALM_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "docker-compose", "imports", "keycloak",
                          "realms", "zaakafhandelcomponent-realm.json")


def environment_name(port_name):
    return "KIND_PORT_" + port_name.upper().replace("-", "_")


def is_published_for_mode(port, mode):
    only = port.get("only")
    return only is None or (only == "zacInCluster") == (mode == "zac")


def ports_of_container(container_name):
    result = subprocess.run(["docker", "port", container_name], capture_output=True, text=True, check=False)
    return {int(match) for match in re.findall(r":(\d+)$", result.stdout, re.M)}


def is_port_in_use(host_port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as connection:
        connection.settimeout(0.5)
        if connection.connect_ex(("127.0.0.1", host_port)) == 0:
            return True
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as listener:
        listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            listener.bind(("127.0.0.1", host_port))
        except OSError:
            return True
    return False


def resolve_ports(ports_file, mode, output_dir, node_host, proxy_container):
    ports = json.load(open(ports_file))["ports"]
    offset = int(os.environ.get("KIND_PORT_OFFSET", "0"))
    ports_of_running_proxy = ports_of_container(proxy_container)
    published, errors = [], []
    for port in ports:
        if not is_published_for_mode(port, mode):
            continue
        variable = environment_name(port["name"])
        host_port = int(os.environ.get(variable, port["hostPort"] + offset))
        if host_port == 0:
            continue
        if not 0 < host_port <= MAXIMUM_PORT:
            errors.append(f"{port['name']}: host port {host_port} is out of range ({variable})")
        elif host_port not in ports_of_running_proxy and is_port_in_use(host_port):
            errors.append(f"{port['name']}: host port {host_port} is already in use ({variable})")
        published.append({**port, "hostPort": host_port})
    if len({port["hostPort"] for port in published}) != len(published):
        errors.append("two services have the same host port; check the KIND_PORT_* variables")
    if errors:
        print("Cannot publish the ports of the kind stack:", *errors, sep="\n  ", file=sys.stderr)
        print("Free these ports, or choose others: KIND_PORT_<NAME>=<port> for one service, KIND_PORT_<NAME>=0 to "
              "leave one out, or KIND_PORT_OFFSET=<offset> to move all of them.", file=sys.stderr)
        sys.exit(1)

    servers = "".join(
        f"    server {{\n"
        f"        listen {port['nodePort']};\n"
        f"        set $upstream {node_host}:{port['nodePort']};\n"
        f"        proxy_pass $upstream;\n"
        f"    }}\n"
        for port in published)
    with open(os.path.join(output_dir, "nginx.conf"), "w") as nginx_configuration:
        nginx_configuration.write(
            "worker_processes 1;\n"
            "pid /tmp/nginx.pid;\n"
            "events {\n    worker_connections 1024;\n}\n"
            "stream {\n"
            "    resolver 127.0.0.11 valid=10s;\n"
            f"{servers}"
            "}\n")
    with open(os.path.join(output_dir, "publish-arguments"), "w") as publish_arguments:
        for port in published:
            publish_arguments.write(f"--publish=127.0.0.1:{port['hostPort']}:{port['nodePort']}\n")
    with open(os.path.join(output_dir, "ports.env"), "w") as ports_environment:
        for port in published:
            ports_environment.write(f"HOST_PORT_{port['name'].upper().replace('-', '_')}={port['hostPort']}\n")
    for port in published:
        print(f"  {port['name']:<26} localhost:{port['hostPort']}")


def split_image(image):
    name, _, digest = image.partition("@")
    repository, separator, tag = name.rpartition(":")
    if not separator or "/" in tag:
        repository, tag = name, "latest"
    return {"repository": repository, "tag": f"{tag}@{digest}" if digest else tag}


def as_boolean(value):
    return str(value).strip().lower() in ("true", "1", "yes")


def set_path(values, path, value):
    *parents, leaf = path.split(".")
    for parent in parents:
        values = values.setdefault(parent, {})
    values[leaf] = value


ZAC_VALUES_FROM_COMPOSE = [
    ("AUTH_ENABLE_PKCE", "auth.enablePkce", as_boolean),
    ("AUTH_REALM", "auth.realm", str),
    ("AUTH_RESOURCE", "auth.clientId", str),
    ("AUTH_SECRET", "auth.secret", str),
    ("AUTH_SERVER", "auth.server", str),
    ("AUTH_SSL_REQUIRED", "auth.sslRequired", str),
    ("BAG_API_CLIENT_MP_REST_URL", "bagApi.url", str),
    ("BAG_API_KEY", "bagApi.apiKey", str),
    ("BRON_ORGANISATIE_RSIN", "organizations.bron.rsin", str),
    ("BRP_API_CLIENT_MP_REST_URL", "brpApi.url", str),
    ("BRP_API_KEY", "brpApi.apiKey.value", str),
    ("BRP_API_KEY_HEADER", "brpApi.apiKey.header", str),
    ("BRP_DOELBINDING_HEADER", "brpApi.protocollering.doelbinding.header", str),
    ("BRP_DOELBINDING_PER_ZAAKTYPE", "brpApi.protocollering.doelbinding.perZaaktype", as_boolean),
    ("BRP_DOELBINDING_RAADPLEEGMET", "brpApi.protocollering.doelbinding.raadpleegmet", str),
    ("BRP_DOELBINDING_ZOEKMET", "brpApi.protocollering.doelbinding.zoekmet", str),
    ("BRP_GEBRUIKER_HEADER", "brpApi.protocollering.gebruiker.header", str),
    ("BRP_LOG_LEVEL", "brpApi.logLevel", str),
    ("BRP_ORIGIN_OIN", "brpApi.protocollering.originOin.oin", str),
    ("BRP_ORIGIN_OIN_HEADER", "brpApi.protocollering.originOin.header", str),
    ("BRP_PROTOCOLLERING_ENABLED", "brpApi.protocollering.enabled", as_boolean),
    ("BRP_SYSTEM_USER", "brpApi.protocollering.systemUser", str),
    ("BRP_TOEPASSING", "brpApi.protocollering.toepassing.value", str),
    ("BRP_TOEPASSING_HEADER", "brpApi.protocollering.toepassing.header", str),
    ("BRP_VERWERKINGSREGISTER", "brpApi.protocollering.verwerking.register", str),
    ("BRP_VERWERKINGSREGISTER_EXT_WITH_ZAAKTYPE", "brpApi.protocollering.verwerking.extendWithZaaktype", str),
    ("BRP_VERWERKING_HEADER", "brpApi.protocollering.verwerking.header", str),
    ("DB_HOST", "db.host", str),
    ("DB_NAME", "db.name", str),
    ("DB_PASSWORD", "db.password", str),
    ("DB_USER", "db.user", str),
    ("GEMEENTE_CODE", "gemeente.code", str),
    ("GEMEENTE_MAIL", "gemeente.mail", str),
    ("GEMEENTE_NAAM", "gemeente.naam", str),
    ("KEYCLOAK_ADMIN_CLIENT_ID", "keycloak.adminClient.id", str),
    ("KEYCLOAK_ADMIN_CLIENT_SECRET", "keycloak.adminClient.secret", str),
    ("KLANTINTERACTIES_API_CLIENT_MP_REST_URL", "klantinteractiesApi.url", str),
    ("KLANTINTERACTIES_API_TOKEN", "klantinteractiesApi.token", str),
    ("KVK_API_CLIENT_MP_REST_URL", "kvkApi.url", str),
    ("KVK_API_KEY", "kvkApi.apiKey", str),
    ("MAX_FILE_SIZE_MB", "maxFileSizeMB", int),
    ("MAX_IN_MEMORY_FILE_SIZE_MB", "maxInMemoryFileSizeMB", int),
    ("OBJECTS_API_CLIENT_MP_REST_URL", "objectenApi.url", str),
    ("OBJECTS_API_TOKEN", "objectenApi.token", str),
    ("OFFICE_CONVERTER_PASSWORD", "office_converter.password", str),
    ("OFFICE_CONVERTER_USERNAME", "office_converter.username", str),
    ("OPEN_FORMS_URL", "openForms.url", str),
    ("OPEN_NOTIFICATIONS_API_SECRET_KEY", "notificationsSecretKey", str),
    ("SIGNALERINGEN_DELETE_OLDER_THAN_DAYS", "signaleringen.deleteOlderThanDays", str),
    ("SMARTDOCUMENTS_AUTHENTICATION", "smartDocuments.authentication", str),
    ("SMARTDOCUMENTS_CLIENT_MP_REST_URL", "smartDocuments.url", str),
    ("SMARTDOCUMENTS_ENABLED", "smartDocuments.enabled", as_boolean),
    ("SMARTDOCUMENTS_FIXED_USER_NAME", "smartDocuments.fixedUserName", str),
    ("SMTP_PASSWORD", "mail.smtp.password", str),
    ("SMTP_PORT", "mail.smtp.port", str),
    ("SMTP_SERVER", "mail.smtp.server", str),
    ("SMTP_USERNAME", "mail.smtp.username", str),
    ("SOLR_PASSWORD", "solr.password", str),
    ("SOLR_URL", "solr.url", str),
    ("SOLR_USERNAME", "solr.username", str),
    ("VERANTWOORDELIJKE_ORGANISATIE_RSIN", "organizations.verantwoordelijke.rsin", str),
    ("ZAC_INTERNAL_ENDPOINTS_API_KEY", "zacInternalEndpointsApiKey", str),
    ("ZGW_API_CLIENT_MP_REST_URL", "zgwApis.url", str),
    ("ZGW_API_CLIENTID", "zgwApis.clientId", str),
    ("ZGW_API_SECRET", "zgwApis.secret", str),
]

DATABASE_SERVICES = ["zac-database", "openzaak-database", "open-object-database", "openklant-database",
                     "keycloak-database", "pabc-database"]

SERVICES_WITH_ENVIRONMENT_SECRET = ["keycloak", "pabc-api", "pabc-migrations", "office-converter",
                                    "brp-personen-mock", "greenmail"]


def secret(name, string_data):
    return {"apiVersion": "v1", "kind": "Secret", "metadata": {"name": name}, "type": "Opaque",
            "stringData": string_data}


def sql_literal(value):
    return "'" + value.replace("'", "''") + "'"


def sql_identifier(value):
    return '"' + value.replace('"', '""') + '"'


def write_json(path, content):
    with open(path, "w") as output:
        json.dump(content, output, indent=2)


def write_values(compose_file, output_dir, mode):
    services = json.load(open(compose_file))["services"]

    def environment(service):
        return {key: "" if value is None else str(value)
                for key, value in (services[service].get("environment") or {}).items()}

    def image(service):
        return services[service]["image"]

    host_ports = dict(line.strip().split("=", 1) for line in open(os.path.join(output_dir, "ports.env")) if "=" in line)
    zac_port = host_ports.get("HOST_PORT_ZAC", "8080")
    keycloak_port = host_ports.get("HOST_PORT_KEYCLOAK", "8081")
    pabc_port = host_ports.get("HOST_PORT_PABC", "8006")

    roles_and_databases = "".join(
        f"CREATE ROLE {sql_identifier(environment(service)['POSTGRES_USER'])} LOGIN SUPERUSER PASSWORD "
        f"{sql_literal(environment(service)['POSTGRES_PASSWORD'])};\n"
        f"CREATE DATABASE {sql_identifier(environment(service)['POSTGRES_DB'])} "
        f"OWNER {sql_identifier(environment(service)['POSTGRES_USER'])};\n"
        for service in DATABASE_SERVICES)
    openzaak_database = environment("openzaak-database")
    openklant_database = environment("openklant-database")
    secrets = [secret(f"{service}-compose-env", environment(service)) for service in SERVICES_WITH_ENVIRONMENT_SECRET]
    secrets += [
        secret("postgres-initdb-roles", {"00-roles-and-databases.sql": roles_and_databases}),
        secret("openzaak-seed-env", {key: openzaak_database.get(key, "")
                                     for key in ("BAG_API_CLIENT_MP_REST_URL", "BAG_API_KEY")}),
        secret("openklant-database-access", {"PGHOST": "openklant-database",
                                             "PGUSER": openklant_database["POSTGRES_USER"],
                                             "PGPASSWORD": openklant_database["POSTGRES_PASSWORD"],
                                             "PGDATABASE": openklant_database["POSTGRES_DB"]}),
    ]
    write_json(os.path.join(output_dir, "secrets.json"), {"apiVersion": "v1", "kind": "List", "items": secrets})

    write_json(os.path.join(output_dir, "zac-dev.json"), {
        "zacOutsideCluster": mode == "ide",
        "keycloak": {"hostname": os.environ.get("KEYCLOAK_HOSTNAME") or f"http://localhost:{keycloak_port}"},
        "images": {
            "postgres": image("openzaak-database"),
            "redis": image("redis"),
            "keycloak": image("keycloak"),
            "pabcApi": image("pabc-api"),
            "pabcMigrations": image("pabc-migrations"),
            "brpPersonenMock": image("brp-personen-mock"),
            "wiremock": image("smartdocuments-wiremock"),
            "greenmail": image("greenmail"),
            "solr": image("solr"),
            "openObject": image("open-object.local"),
            "opa": image("opa"),
            "officeConverter": image("office-converter"),
        },
        "officeConverter": {"args": services["office-converter"].get("command") or []},
    })

    for release, database_service, web_service in [("openzaak", "openzaak-database", "openzaak-app.local"),
                                                   ("openklant", "openklant-database", "openklant.local"),
                                                   ("openobject", "open-object-database", "open-object.local")]:
        database = environment(database_service)
        values = {
            "image": split_image(image(web_service)),
            "settings": {
                "secretKey": environment(web_service)["SECRET_KEY"],
                "database": {"host": database_service, "name": database["POSTGRES_DB"],
                             "username": database["POSTGRES_USER"], "password": database["POSTGRES_PASSWORD"]},
            },
        }
        if release == "openzaak":
            web = environment(web_service)
            values["configuration"] = {"superuser": {"username": web["OPENZAAK_SUPERUSER_USERNAME"],
                                                     "email": web["OPENZAAK_SUPERUSER_EMAIL"],
                                                     "password": web["DJANGO_SUPERUSER_PASSWORD"]}}
            values["nginx"] = {"image": split_image(image("openzaak.local"))}
        write_json(os.path.join(output_dir, f"{release}.json"), values)

    zac_environment = environment("zac")
    zac_values = {}
    for variable, path, convert in ZAC_VALUES_FROM_COMPOSE:
        if variable in zac_environment:
            set_path(zac_values, path, convert(zac_environment[variable]))
    set_path(zac_values, "contextUrl", f"http://localhost:{zac_port}")
    set_path(zac_values, "pabcApi.url", "http://pabc-api:8000")
    set_path(zac_values, "pabcApi.apiKey", zac_environment.get("PABC_API_KEY") or environment("pabc-api")["API_KEY__0"])
    write_json(os.path.join(output_dir, "zac.json"), zac_values)

    realm = json.load(open(REALM_FILE))
    for client in realm["clients"]:
        port = {"zaakafhandelcomponent": zac_port, "pabc": pabc_port}.get(client["clientId"])
        if port:
            origin = f"http://localhost:{port}"
            if f"{origin}/*" not in client.setdefault("redirectUris", []):
                client["redirectUris"].append(f"{origin}/*")
            if origin not in client.setdefault("webOrigins", []):
                client["webOrigins"].append(origin)
    os.makedirs(os.path.join(output_dir, "realm"), exist_ok=True)
    write_json(os.path.join(output_dir, "realm", os.path.basename(REALM_FILE)), realm)


def parse_time(value):
    return datetime.fromisoformat(re.sub(r"(\.\d{6})\d*", r"\1", value).replace("Z", "+00:00"))


def wait_until_ready(context, namespace, timeout, node_started_at, *deployments):
    deadline = time.time() + int(timeout)
    started_at = parse_time(node_started_at)

    def kubectl_json(*arguments):
        return json.loads(subprocess.run(["kubectl", "--context", context, "-n", namespace, *arguments, "-o", "json"],
                                         capture_output=True, text=True, check=True).stdout)

    def is_ready(deployment):
        specification = kubectl_json("get", "deployment", deployment)["spec"]
        if specification.get("replicas", 1) == 0:
            return True
        labels = specification["selector"]["matchLabels"]
        selector = ",".join(f"{key}={value}" for key, value in labels.items())
        pods = kubectl_json("get", "pods", "--selector", selector)["items"]
        ready_conditions = [condition for pod in pods for condition in pod.get("status", {}).get("conditions", [])
                            if condition["type"] == "Ready"]
        return bool(ready_conditions) and len(ready_conditions) == len(pods) and all(
            condition["status"] == "True" and parse_time(condition["lastTransitionTime"]) >= started_at
            for condition in ready_conditions)

    waiting = list(deployments) or [item["metadata"]["name"] for item in kubectl_json("get", "deployments")["items"]]
    while waiting:
        still_waiting = [deployment for deployment in waiting if not is_ready(deployment)]
        for deployment in waiting:
            if deployment not in still_waiting:
                print(f"  ready: {deployment}", flush=True)
        waiting = still_waiting
        if waiting and time.time() > deadline:
            print(f"Timed out waiting for {', '.join(waiting)}", file=sys.stderr)
            sys.exit(1)
        if waiting:
            time.sleep(2)


if __name__ == "__main__":
    if len(sys.argv) == 7 and sys.argv[1] == "ports":
        resolve_ports(*sys.argv[2:])
    elif len(sys.argv) == 5 and sys.argv[1] == "values":
        write_values(*sys.argv[2:])
    elif len(sys.argv) >= 6 and sys.argv[1] == "wait":
        wait_until_ready(*sys.argv[2:])
    else:
        print(__doc__, file=sys.stderr)
        sys.exit(2)
