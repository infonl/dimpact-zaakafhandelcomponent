#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
"""Helpers for scripts/kind/up.sh.

  ports  <ports.json> <zac|ide> <profiles> <output dir> <node host name> <proxy container name>
         Resolves the host ports, fails when one of them is taken, and writes the nginx configuration and the
         'docker run' port arguments of the container that publishes the NodePorts on the host.
  values <stack.json> <output dir> <zac|ide> <profiles> <Secret with the generated passwords, as JSON, or empty>
         Writes the Helm values of the releases, the Secret with the generated passwords and the SQL that gives the
         database roles their passwords. Settings of ZAC in .env or in the environment override the defaults.
  wait   <kube context> <namespace> <timeout seconds> <node start time> [<deployment>...]
         Waits until the pods of the deployments, or of all deployments, are ready, ignoring a readiness from
         before the node started:
         right after a stopped node starts again, its pods still report the readiness they had when it stopped.

<profiles> is a comma-separated list of the optional services: objecten, itest, opennotificaties,
openarchiefbeheer and openformulieren.
"""
import base64
import hashlib
import json
import os
import re
import secrets
import socket
import subprocess
import sys
import time
from datetime import datetime

MAXIMUM_PORT = 65535
KIND_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(KIND_DIR))
REALM_FILE = os.path.join(KIND_DIR, "seed", "keycloak", "zaakafhandelcomponent-realm.json")
GENERATED_SECRET = "zac-dev-generated"
# the host name of Keycloak in the browser and in the cluster: browsers and the resolvers of macOS and Linux resolve
# every name under 'localhost' to the host itself, and up.sh rewrites it to the Keycloak Service in the cluster
KEYCLOAK_HOST = "keycloak.localhost"
DJANGO_ADMIN_HASH_ITERATIONS = 600000


def environment_name(port_name):
    return "KIND_PORT_" + port_name.upper().replace("-", "_")


def effective_profiles(profiles):
    active = {profile for profile in profiles.split(",") if profile}
    if active & {"itest", "openformulieren"}:
        active.add("objecten")
    if "openformulieren" in active:
        active.add("opennotificaties")
    return active


def is_published(port, mode, profiles):
    only = port.get("only")
    if only is not None and (only == "zacInCluster") != (mode == "zac"):
        return False
    return port.get("profile") is None or port["profile"] in profiles


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


def resolve_ports(ports_file, mode, profiles, output_dir, node_host, proxy_container):
    ports = json.load(open(ports_file))["ports"]
    active_profiles = effective_profiles(profiles)
    offset = int(os.environ.get("KIND_PORT_OFFSET", "0"))
    ports_of_running_proxy = ports_of_container(proxy_container)
    published, errors = [], []
    for port in ports:
        if not is_published(port, mode, active_profiles):
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
        print(f"  {port['name']:<28} localhost:{port['hostPort']}")


def read_dotenv(path):
    variables = {}
    if not os.path.exists(path):
        return variables
    for line in open(path):
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = re.sub(r"^export\s+", "", key.strip())
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        else:
            value = re.sub(r"\s+#.*$", "", value)
        variables[key] = value
    return variables


def load_environment():
    """The variables of .env with those of the environment on top."""
    return {**read_dotenv(os.path.join(ROOT, ".env")), **os.environ}


def as_boolean(value):
    return str(value).strip().lower() in ("true", "1", "yes")


# The settings of ZAC that a developer can override in .env or in the environment, with their place in the values of
# charts/zac. The other settings of ZAC point at the services of the stack and stay as scripts/kind/values/zac.yaml
# sets them.
ZAC_SETTINGS_FROM_ENVIRONMENT = [
    ("AUTH_SSL_REQUIRED", "auth.sslRequired", str),
    ("BAG_API_CLIENT_MP_REST_URL", "bagApi.url", str),
    ("BAG_API_KEY", "bagApi.apiKey", str),
    ("BRON_ORGANISATIE_RSIN", "organizations.bron.rsin", str),
    ("BRP_API_KEY", "brpApi.apiKey.value", str),
    ("BRP_API_KEY_HEADER", "brpApi.apiKey.header", str),
    ("BRP_DOELBINDING_HEADER", "brpApi.protocollering.doelbinding.header", str),
    ("BRP_DOELBINDING_PER_ZAAKTYPE", "brpApi.protocollering.doelbinding.perZaaktype", as_boolean),
    ("BRP_DOELBINDING_RAADPLEEGMET", "brpApi.protocollering.doelbinding.raadpleegmet", str),
    ("BRP_DOELBINDING_ZOEKMET", "brpApi.protocollering.doelbinding.zoekmet", str),
    ("BRP_GEBRUIKER_HEADER", "brpApi.protocollering.gebruiker.header", str),
    ("BRP_ORIGIN_OIN", "brpApi.protocollering.originOin.oin", str),
    ("BRP_ORIGIN_OIN_HEADER", "brpApi.protocollering.originOin.header", str),
    ("BRP_PROTOCOLLERING_ENABLED", "brpApi.protocollering.enabled", as_boolean),
    ("BRP_TOEPASSING", "brpApi.protocollering.toepassing.value", str),
    ("BRP_TOEPASSING_HEADER", "brpApi.protocollering.toepassing.header", str),
    ("BRP_VERWERKINGSREGISTER", "brpApi.protocollering.verwerking.register", str),
    ("BRP_VERWERKINGSREGISTER_EXT_WITH_ZAAKTYPE", "brpApi.protocollering.verwerking.extendWithZaaktype", str),
    ("BRP_VERWERKING_HEADER", "brpApi.protocollering.verwerking.header", str),
    ("GEMEENTE_CODE", "gemeente.code", str),
    ("GEMEENTE_MAIL", "gemeente.mail", str),
    ("GEMEENTE_NAAM", "gemeente.naam", str),
    ("KVK_API_CLIENT_MP_REST_URL", "kvkApi.url", str),
    ("KVK_API_KEY", "kvkApi.apiKey", str),
    ("MAX_FILE_SIZE_MB", "maxFileSizeMB", int),
    ("MAX_IN_MEMORY_FILE_SIZE_MB", "maxInMemoryFileSizeMB", int),
    ("SIGNALERINGEN_DELETE_OLDER_THAN_DAYS", "signaleringen.deleteOlderThanDays", str),
    ("SMARTDOCUMENTS_AUTHENTICATION", "smartDocuments.authentication", str),
    ("SMARTDOCUMENTS_CLIENT_MP_REST_URL", "smartDocuments.url", str),
    ("SMARTDOCUMENTS_ENABLED", "smartDocuments.enabled", as_boolean),
    ("SMARTDOCUMENTS_FIXED_USER_NAME", "smartDocuments.fixedUserName", str),
    ("SMTP_PASSWORD", "mail.smtp.password", str),
    ("SMTP_PORT", "mail.smtp.port", str),
    ("SMTP_SERVER", "mail.smtp.server", str),
    ("SMTP_USERNAME", "mail.smtp.username", str),
    ("VERANTWOORDELIJKE_ORGANISATIE_RSIN", "organizations.verantwoordelijke.rsin", str),
    ("ZAC_INTERNAL_ENDPOINTS_API_KEY", "zacInternalEndpointsApiKey", str),
]

# The generated passwords that a variable of .env or of the environment overrides, so that a ZAC in an IDE with the
# settings of .env can connect to the stack
GENERATED_FROM_ENVIRONMENT = {
    "db-password-zac": "DB_PASSWORD",
    "solr-password": "SOLR_PASSWORD",
    "office-converter-password": "OFFICE_CONVERTER_PASSWORD",
}

# The releases of the upstream charts, with their database and the host name of that database
DJANGO_RELEASES = {
    "openzaak": ("openzaak", "openzaak-database"),
    "openklant": ("openklant", "openklant-database"),
    "openobject": ("objects", "open-object-database"),
    "opennotificaties": ("opennotificaties", "opennotificaties-database"),
    "openarchiefbeheer": ("openarchiefbeheer", "openarchiefbeheer-database"),
    "openforms": ("openformulieren", "openformulieren-database"),
}


def set_path(values, path, value):
    *parents, leaf = path.split(".")
    for parent in parents:
        values = values.setdefault(parent, {})
    values[leaf] = value


def write_json(path, content):
    with open(path, "w") as output:
        json.dump(content, output, indent=2)


def split_image(image):
    name, _, digest = image.partition("@")
    repository, separator, tag = name.rpartition(":")
    if not separator or "/" in tag:
        repository, tag = name, "latest"
    return {"repository": repository, "tag": f"{tag}@{digest}" if digest else tag}


def django_password_hash(password):
    salt = secrets.token_hex(11)
    digest = hashlib.pbkdf2_hmac("sha256", password.encode(), salt.encode(), DJANGO_ADMIN_HASH_ITERATIONS)
    return f"pbkdf2_sha256${DJANGO_ADMIN_HASH_ITERATIONS}${salt}${base64.b64encode(digest).decode()}"


def solr_security(username, password):
    """The security.json of Solr with basic authentication for one user, as Solr's Sha256AuthenticationProvider
    hashes the password."""
    salt = secrets.token_bytes(32)
    digest = hashlib.sha256(hashlib.sha256(salt + password.encode()).digest()).digest()
    return json.dumps({
        "authentication": {
            "class": "solr.BasicAuthPlugin",
            "blockUnknown": True,
            "realm": "ZAC Solr",
            "forwardCredentials": False,
            "credentials": {username: f"{base64.b64encode(digest).decode()} {base64.b64encode(salt).decode()}"},
        },
        "authorization": {
            "class": "solr.RuleBasedAuthorizationPlugin",
            "permissions": [{"name": "all", "role": "admin"}],
            "user-role": {username: "admin"},
        },
    }, indent=2)


def generated_passwords(existing_secret_file, database_owners, environment):
    """The generated passwords of the cluster: those of the Secret of an earlier start, new ones for the rest."""
    passwords = {}
    if existing_secret_file and os.path.getsize(existing_secret_file) > 0:
        passwords = {key: base64.b64decode(value).decode()
                     for key, value in json.load(open(existing_secret_file)).get("data", {}).items()}
    keys = (["postgres-password", "admin-password", "solr-password", "office-converter-password"]
            + [f"db-password-{owner}" for owner in database_owners]
            + [f"secret-key-{release}" for release in DJANGO_RELEASES])
    for key in keys:
        override = environment.get(GENERATED_FROM_ENVIRONMENT.get(key, ""), "")
        if override:
            passwords[key] = override
        elif not passwords.get(key):
            passwords[key] = secrets.token_hex(20)
    if not passwords.get("admin-password-hash") or passwords.get("admin-password-of-hash") != passwords["admin-password"]:
        passwords["admin-password-hash"] = django_password_hash(passwords["admin-password"])
        passwords["admin-password-of-hash"] = passwords["admin-password"]
    solr_credentials = f"{environment.get('SOLR_USERNAME') or 'zac'}:{passwords['solr-password']}"
    if passwords.get("solr-credentials") != solr_credentials:
        passwords["solr-security.json"] = solr_security(*solr_credentials.split(":", 1))
        passwords["solr-credentials"] = solr_credentials
    return passwords


def sql_literal(value):
    return "'" + value.replace("'", "''") + "'"


def sql_identifier(value):
    return '"' + value.replace('"', '""') + '"'


def roles_sql(databases, passwords):
    """Creates the missing roles and databases and gives each role the password of the Secret."""
    statements = []
    for database in databases:
        owner, name = database["owner"], database["name"]
        password = sql_literal(passwords[f"db-password-{owner}"])
        statements.append(
            f"SELECT 'CREATE ROLE {sql_identifier(owner)} LOGIN SUPERUSER' "
            f"WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = {sql_literal(owner)}) \\gexec\n"
            f"ALTER ROLE {sql_identifier(owner)} PASSWORD {password};\n"
            f"SELECT 'CREATE DATABASE {sql_identifier(name)} OWNER {sql_identifier(owner)}' "
            f"WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = {sql_literal(name)}) \\gexec\n")
    return "".join(statements)


def write_values(stack_file, output_dir, mode, profiles, existing_secret_file):
    stack = json.load(open(stack_file))
    active_profiles = effective_profiles(profiles)
    environment = load_environment()
    host_ports = dict(line.strip().split("=", 1) for line in open(os.path.join(output_dir, "ports.env")) if "=" in line)

    def host_url(port_name, default):
        return f"http://localhost:{host_ports.get('HOST_PORT_' + port_name, default)}"

    databases = stack["databases"]
    passwords = generated_passwords(existing_secret_file, [database["owner"] for database in databases], environment)
    write_json(os.path.join(output_dir, "generated", "secret.json"), {
        "apiVersion": "v1", "kind": "Secret", "metadata": {"name": GENERATED_SECRET}, "type": "Opaque",
        "stringData": passwords})
    with open(os.path.join(output_dir, "generated", "roles.sql"), "w") as roles:
        roles.write(roles_sql(databases, passwords))
    with open(os.path.join(output_dir, "generated", "openzaak-seed-env.json"), "w") as seed_environment:
        json.dump({"apiVersion": "v1", "kind": "Secret", "metadata": {"name": "openzaak-seed-env"}, "type": "Opaque",
                   "stringData": {key: environment.get(key, default) for key, default in [
                       ("BAG_API_CLIENT_MP_REST_URL", "https://api.bag.kadaster.nl/lvbag/individuelebevragingen/v2/"),
                       ("BAG_API_KEY", "please-request-a-bag-api-key")]}}, seed_environment)

    keycloak_url = (environment.get("KEYCLOAK_HOSTNAME")
                    or f"http://{KEYCLOAK_HOST}:{host_ports.get('HOST_PORT_KEYCLOAK', 8081)}")
    email_addresses = {
        name.replace("DOCKER_COMPOSE_", "ZAC_", 1): value for name, value in environment.items()
        if re.fullmatch(r"DOCKER_COMPOSE_[A-Z0-9_]+_EMAIL_ADDRESS", name) and value}
    write_json(os.path.join(output_dir, "zac-dev.json"), {
        "zacOutsideCluster": mode == "ide",
        "profiles": {profile: profile in profiles.split(",") for profile in
                     ["objecten", "itest", "opennotificaties", "openarchiefbeheer", "openformulieren"]},
        "keycloak": {"hostname": keycloak_url, "emailAddresses": email_addresses},
        "officeConverter": {"username": environment.get("OFFICE_CONVERTER_USERNAME") or stack["officeConverterUsername"]},
    })

    opennotificaties = "opennotificaties" in active_profiles
    for release, (database, database_host) in DJANGO_RELEASES.items():
        values = {
            "image": split_image(stack["images"][release]),
            "settings": {
                "secretKey": passwords[f"secret-key-{release}"],
                "database": {"host": database_host, "name": database, "username": database,
                             "password": passwords[f"db-password-{database}"]},
            },
        }
        if release in ("openzaak", "openarchiefbeheer", "openforms"):
            values["nginx"] = {"image": split_image(stack["images"]["nginx"])}
        if release in ("openzaak", "opennotificaties"):
            values["configuration"] = {"superuser": {"username": "admin", "email": "admin@example.com",
                                                     "password": passwords["admin-password"]}}
        if release == "openzaak":
            values["settings"]["notificationsDisabled"] = not opennotificaties
            values["worker"] = {"replicaCount": 1 if opennotificaties else 0}
        if release == "openobject":
            values["worker"] = {"replicaCount": 1 if opennotificaties else 0}
        if release == "openarchiefbeheer":
            api_url, frontend_url = host_url("OPENARCHIEFBEHEER", 8004), host_url("OPENARCHIEFBEHEER_UI", 8005)
            values["settings"].update({
                "frontend": {"apiUrl": api_url, "zaakUrlTemplate": host_url("ZAC", 8080) + "/zaken/{identificatie}"},
                "frontendUrl": frontend_url,
                "cors": {"allowedOrigins": f"{frontend_url},{api_url}"},
                "csrf": {"trustedOrigins": f"{frontend_url},{api_url}"},
            })
        if release == "openforms":
            origins = f"{host_url('OPENFORMULIEREN', 8007)},{host_url('OPENFORMULIEREN_WEB', 8009)}"
            values["settings"]["csrf"] = {"trustedOrigins": origins}
        write_json(os.path.join(output_dir, f"{release}.json"), values)

    zac_port = host_ports.get("HOST_PORT_ZAC", "8080")
    zac_values = {}
    for variable, path, convert in ZAC_SETTINGS_FROM_ENVIRONMENT:
        if environment.get(variable, "") != "":
            set_path(zac_values, path, convert(environment[variable]))
    set_path(zac_values, "contextUrl", f"http://localhost:{zac_port}")
    set_path(zac_values, "db.password", passwords["db-password-zac"])
    set_path(zac_values, "solr.username", environment.get("SOLR_USERNAME") or "zac")
    set_path(zac_values, "solr.password", passwords["solr-password"])
    set_path(zac_values, "office_converter.username",
             environment.get("OFFICE_CONVERTER_USERNAME") or stack["officeConverterUsername"])
    set_path(zac_values, "office_converter.password", passwords["office-converter-password"])
    write_json(os.path.join(output_dir, "zac.json"), zac_values)

    realm = json.load(open(REALM_FILE))
    for client in realm["clients"]:
        port = {"zaakafhandelcomponent": zac_port, "pabc": host_ports.get("HOST_PORT_PABC", "8006")}.get(
            client["clientId"])
        if port:
            origin = f"http://localhost:{port}"
            if f"{origin}/*" not in client.setdefault("redirectUris", []):
                client["redirectUris"].append(f"{origin}/*")
            if origin not in client.setdefault("webOrigins", []):
                client["webOrigins"].append(origin)
    os.makedirs(os.path.join(output_dir, "realm"), exist_ok=True)
    write_json(os.path.join(output_dir, "realm", os.path.basename(REALM_FILE)), realm)

    with open(os.path.join(output_dir, "generated", "summary.env"), "w") as summary:
        summary.write(f"ADMIN_PASSWORD={passwords['admin-password']}\n")
        if mode == "ide":
            for key, variable in GENERATED_FROM_ENVIRONMENT.items():
                if environment.get(variable, "") != passwords[key]:
                    summary.write(f"{variable}={passwords[key]}\n")


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
    if len(sys.argv) == 8 and sys.argv[1] == "ports":
        resolve_ports(*sys.argv[2:])
    elif len(sys.argv) == 7 and sys.argv[1] == "values":
        write_values(*sys.argv[2:])
    elif len(sys.argv) >= 6 and sys.argv[1] == "wait":
        wait_until_ready(*sys.argv[2:])
    else:
        print(__doc__, file=sys.stderr)
        sys.exit(2)
