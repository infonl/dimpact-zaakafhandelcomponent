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
Common labels
*/}}
{{- define "zaakafhandelcomponent.labels" -}}
helm.sh/chart: {{ include "zaakafhandelcomponent.chart" . }}
{{ include "zaakafhandelcomponent.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "office-converter.labels" -}}
helm.sh/chart: {{ include "zaakafhandelcomponent.chart" . }}
{{ include "office-converter.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "opa.labels" -}}
helm.sh/chart: {{ include "zaakafhandelcomponent.chart" . }}
{{ include "opa.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "zaakafhandelcomponent.selectorLabels" -}}
app.kubernetes.io/name: {{ include "zaakafhandelcomponent.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "office-converter.selectorLabels" -}}
app.kubernetes.io/name: office-converter
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "opa.selectorLabels" -}}
app.kubernetes.io/name: opa
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
