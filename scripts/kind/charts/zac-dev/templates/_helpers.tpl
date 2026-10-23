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
The image of a workload, failing when scripts/kind/up.sh did not set it. Usage: include "zac-dev.image" (list . "postgres")
*/}}
{{- define "zac-dev.image" -}}
{{- $root := index . 0 -}}
{{- $name := index . 1 -}}
{{- required (printf "images.%s is required; scripts/kind/up.sh takes it from docker-compose.yaml" $name) (index $root.Values.images $name) -}}
{{- end }}

{{/*
Init container that waits until Postgres accepts TCP connections, which it only does once its init scripts with
the seed data have run.
*/}}
{{- define "zac-dev.waitForPostgres" -}}
- name: wait-for-postgres
  image: {{ include "zac-dev.image" (list . "postgres") }}
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
