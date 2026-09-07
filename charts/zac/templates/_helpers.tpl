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
We truncate at 25 chars in order to provide space for the suffixes set by the solr-operator and zookeeper
*/}}
{{- define "zaakafhandelcomponent.solrcloud.fullname" -}}
{{ include "zaakafhandelcomponent.fullname" . | trunc 25 | trimSuffix "-" }}-solr
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
We truncate at 46 chars in order to provide space for the suffix
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

{{/*
Maximum heap size in MB, taken from the -Xmx option in .Values.javaOptions.
Falls back to the 1024m of the default javaOptions when no -Xmx is given.
*/}}
{{- define "zaakafhandelcomponent.maxHeapSizeMB" -}}
{{- $javaOptions := .Values.javaOptions | default "-Xmx1024m -Xms1024m -Xlog:gc::time,uptime" -}}
{{- $match := regexFind "-Xmx[0-9]+[kKmMgG]?" $javaOptions -}}
{{- if not $match -}}
1024
{{- else -}}
{{- $value := regexFind "[0-9]+" $match | int64 -}}
{{- $unit := regexFind "[kKmMgG]?$" $match | lower -}}
{{- if eq $unit "g" -}}
{{- mul $value 1024 -}}
{{- else if eq $unit "k" -}}
{{- div $value 1024 -}}
{{- else -}}
{{- $value -}}
{{- end -}}
{{- end -}}
{{- end }}

{{/*
Fails the release when the configured file size limits cannot be served by the configured heap.
ZAC performs the same check on startup; doing it here as well turns a crash loop into a failed
install with an actionable message.
*/}}
{{- define "zaakafhandelcomponent.validateFileSizeLimits" -}}
{{- $maxFileSizeMB := .Values.maxFileSizeMB | default 80 | int64 -}}
{{- $maxInMemoryFileSizeMB := .Values.maxInMemoryFileSizeMB | default 80 | int64 -}}
{{- if or (le $maxFileSizeMB 0) (le $maxInMemoryFileSizeMB 0) -}}
{{- fail "maxFileSizeMB and maxInMemoryFileSizeMB must both be greater than zero" -}}
{{- end -}}
{{- if gt $maxFileSizeMB 2047 -}}
{{- fail (printf "maxFileSizeMB (%d) cannot be larger than 2047, because the documents registry expresses the size of a document as a 32 bit integer number of bytes" $maxFileSizeMB) -}}
{{- end -}}
{{- if gt $maxInMemoryFileSizeMB $maxFileSizeMB -}}
{{- fail (printf "maxInMemoryFileSizeMB (%d) cannot be larger than maxFileSizeMB (%d)" $maxInMemoryFileSizeMB $maxFileSizeMB) -}}
{{- end -}}
{{- $maxHeapSizeMB := include "zaakafhandelcomponent.maxHeapSizeMB" . | int64 -}}
{{- $availableHeapMB := div $maxHeapSizeMB 2 -}}
{{- $requiredHeapMB := mul $maxInMemoryFileSizeMB 3 -}}
{{- if gt $requiredHeapMB $availableHeapMB -}}
{{- fail (printf "maxInMemoryFileSizeMB (%d) requires at least %d MB of heap but only %d MB of the %d MB heap is available for it. Either lower maxInMemoryFileSizeMB to at most %d or raise -Xmx in javaOptions." $maxInMemoryFileSizeMB $requiredHeapMB $availableHeapMB $maxHeapSizeMB (div $availableHeapMB 3)) -}}
{{- end -}}
{{- end }}
