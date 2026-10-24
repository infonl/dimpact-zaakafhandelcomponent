{{/*
Pod security context for the containers of this chart.
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
Init container that waits until Postgres accepts TCP connections, which it only does once its init scripts with
the seed data have run. Without it, Keycloak and the PABC migrations fail on their first start and Kubernetes
retries them with a growing back-off.
*/}}
{{- define "zac-dev.waitForPostgres" -}}
- name: wait-for-postgres
  image: {{ .Values.images.postgres }}
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
Environment variable with one of the generated passwords. Usage: include "zac-dev.passwordEnv" (list . "NAME" "key")
*/}}
{{- define "zac-dev.passwordEnv" -}}
{{- $root := index . 0 }}
- name: {{ index . 1 }}
  valueFrom:
    secretKeyRef:
      name: {{ $root.Values.passwordsSecret }}
      key: {{ index . 2 }}
{{- end }}
