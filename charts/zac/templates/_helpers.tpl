{{/*
Expand the name of the chart.
*/}}
{{- define "zaakafhandelcomponent.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "zaakafhandelcomponent.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "zaakafhandelcomponent.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a name for NGINX
We truncate at 57 chars in order to provide space for the suffix
*/}}
{{- define "zaakafhandelcomponent.nginx.name" -}}
{{ include "zaakafhandelcomponent.name" . | trunc 57 | trimSuffix "-" }}-nginx
{{- end }}

{{/*
Create a default fully qualified name for NGINX.
We truncate at 57 chars in order to provide space for the suffix
*/}}
{{- define "zaakafhandelcomponent.nginx.fullname" -}}
{{ include "zaakafhandelcomponent.fullname" . | trunc 57 | trimSuffix "-" }}-nginx
{{- end }}

{{/*
Create a default fully qualified name for solrcloud.
We truncate at 57 chars in order to provide space for the suffix
*/}}
{{- define "zaakafhandelcomponent.solrcloud.fullname" -}}
{{ include "zaakafhandelcomponent.fullname" . | trunc 57 | trimSuffix "-" }}-solr
{{- end }}

{{/*
Create a default fully qualified name for opa.
We truncate at 57 chars in order to provide space for the "-nginx" suffix
*/}}
{{- define "zaakafhandelcomponent.opa.fullname" -}}
{{ include "zaakafhandelcomponent.fullname" . | trunc 57 | trimSuffix "-" }}-opa
{{- end }}

{{/*
Create a default fully qualified name for office-converter.
We truncate at 57 chars in order to provide space for the suffix
*/}}
{{- define "zaakafhandelcomponent.office-converter.fullname" -}}
{{ include "zaakafhandelcomponent.fullname" . | trunc 46 | trimSuffix "-" }}-office-converter
{{- end }}

{{/*
Common labels
*/}}
{{- define "zaakafhandelcomponent.all.labels" -}}
helm.sh/chart: {{ include "zaakafhandelcomponent.chart" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "zaakafhandelcomponent.labels" -}}
{{ include "zaakafhandelcomponent.all.labels" . }}
{{ include "zaakafhandelcomponent.selectorLabels" . }}
{{- end }}

{{- define "zaakafhandelcomponent.office-converter.labels" -}}
{{ include "zaakafhandelcomponent.all.labels" . }}
{{ include "zaakafhandelcomponent.office-converter.selectorLabels" . }}
{{- end }}

{{- define "zaakafhandelcomponent.opa.labels" -}}
{{ include "zaakafhandelcomponent.all.labels" . }}
{{ include "zaakafhandelcomponent.opa.selectorLabels" . }}
{{- end }}

{{- define "zaakafhandelcomponent.nginx.labels" -}}
{{ include "zaakafhandelcomponent.all.labels" . }}
{{ include "zaakafhandelcomponent.nginx.selectorLabels" . }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "zaakafhandelcomponent.selectorLabels" -}}
app.kubernetes.io/name: {{ include "zaakafhandelcomponent.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "zaakafhandelcomponent.office-converter.selectorLabels" -}}
app.kubernetes.io/name: office-converter
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "zaakafhandelcomponent.opa.selectorLabels" -}}
app.kubernetes.io/name: opa
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "zaakafhandelcomponent.nginx.selectorLabels" -}}
app.kubernetes.io/name: nginx
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "zaakafhandelcomponent.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "zaakafhandelcomponent.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
