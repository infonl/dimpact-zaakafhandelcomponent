{{/*
SPDX-FileCopyrightText: 2026 INFO.nl
SPDX-License-Identifier: EUPL-1.2+
*/}}
{{/*
Pod security context for the workloads of this chart.
*/}}
{{- define "zac-dev.podSecurityContext" -}}
seccompProfile:
  type: RuntimeDefault
{{- end }}

{{/*
Container security context for the images that run as root by default: no extra privileges and no capabilities.
*/}}
{{- define "zac-dev.containerSecurityContext" -}}
allowPrivilegeEscalation: false
capabilities:
  drop: ["ALL"]
{{- end }}

{{/*
Container security context for the images that run as a non-root user.
*/}}
{{- define "zac-dev.nonRootContainerSecurityContext" -}}
{{ include "zac-dev.containerSecurityContext" . }}
runAsNonRoot: true
{{- end }}

{{/*
An environment variable with a value from the Secret with the generated passwords.
Usage: include "zac-dev.generatedSecretEnv" (list . "POSTGRES_PASSWORD" "postgres-password")
*/}}
{{- define "zac-dev.generatedSecretEnv" -}}
{{- $root := index . 0 -}}
- name: {{ index . 1 }}
  valueFrom:
    secretKeyRef:
      name: {{ $root.Values.generatedSecret }}
      key: {{ index . 2 }}
{{- end }}

{{/*
Init container that waits until Postgres accepts TCP connections, which it only does once its init scripts with
the seed data have run.
*/}}
{{- define "zac-dev.waitForPostgres" -}}
- name: wait-for-postgres
  image: {{ .Values.postgres.image }}
  command: ["sh", "-c", "until pg_isready --host=postgres --port=5432 --quiet; do sleep 1; done"]
  securityContext:
    {{- include "zac-dev.nonRootContainerSecurityContext" . | nindent 4 }}
    runAsUser: 999
    readOnlyRootFilesystem: true
  resources:
    requests:
      cpu: 10m
      memory: 16Mi
    limits:
      cpu: 100m
      memory: 64Mi
{{- end }}

{{/*
Labels of the seed Jobs, which scripts/kind/up.sh waits for.
*/}}
{{- define "zac-dev.seedJobLabels" -}}
app.kubernetes.io/part-of: zac-dev
app.kubernetes.io/component: seed
{{- end }}

{{/*
Shell commands that wait until a TCP port accepts connections. Usage: include "zac-dev.waitForPort" (list "host" 8000)
*/}}
{{- define "zac-dev.waitForPort" -}}
until (exec 3<> "/dev/tcp/{{ index . 0 }}/{{ index . 1 }}") 2> /dev/null; do echo "waiting for {{ index . 0 }}:{{ index . 1 }}"; sleep 3; done
{{- end }}

{{/*
Whether Open Object runs: with -o, and with the integration test services and Open Formulieren that need it.
*/}}
{{- define "zac-dev.objecten" -}}
{{- if or .Values.profiles.objecten .Values.profiles.itest .Values.profiles.openformulieren }}true{{ end }}
{{- end }}

{{/*
Whether Open Notificaties runs: with -n, and with Open Formulieren that needs it.
*/}}
{{- define "zac-dev.opennotificaties" -}}
{{- if or .Values.profiles.opennotificaties .Values.profiles.openformulieren }}true{{ end }}
{{- end }}

{{/*
The database connection of PABC.
*/}}
{{- define "zac-dev.pabcDatabaseEnv" -}}
{{ include "zac-dev.generatedSecretEnv" (list . "PABC_DATABASE_PASSWORD" "db-password-pabc") }}
- name: ConnectionStrings__Pabc
  value: "Host=pabc-database;Port=5432;Username=pabc;Password=$(PABC_DATABASE_PASSWORD);Database=Pabc"
{{- end }}

{{/*
The URL at which the services in the cluster reach ZAC: in the cluster, or on the host for a ZAC in an IDE.
*/}}
{{- define "zac-dev.zacUrl" -}}
{{- if .Values.zacOutsideCluster }}http://host.docker.internal:8080{{ else }}http://zac{{ end }}
{{- end }}

{{/*
Shell command that creates the Django superuser 'admin' with the generated admin password in $ADMIN_PASSWORD,
unless it exists. Usage: include "zac-dev.createAdminUser" "<path of manage.py>"
*/}}
{{- define "zac-dev.createAdminUser" -}}
python {{ . }} shell -c "
import os
from django.contrib.auth import get_user_model
User = get_user_model()
if not User.objects.filter(username='admin').exists():
    User.objects.create_superuser('admin', 'admin@example.com', os.environ['ADMIN_PASSWORD'])
"
{{- end }}
