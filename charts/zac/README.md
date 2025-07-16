# zaakafhandelcomponent

![Version: 1.0.93](https://img.shields.io/badge/Version-1.0.93-informational?style=flat-square) ![AppVersion: 3.6](https://img.shields.io/badge/AppVersion-3.6-informational?style=flat-square)

A Helm chart for installing Zaakafhandelcomponent

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Team Dimpact, INFO | <teamdimpact@info.nl> | <https://github.com/infonl/dimpact-zaakafhandelcomponent/discussions> |

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| @opentelemetry | opentelemetry-collector | 0.129.0 |
| @solr | solr-operator | 0.9.1 |

## Usage

Make sure you have helm installed. Add the required repositories as follows:
```
helm repo add opentelemetry https://open-telemetry.github.io/opentelemetry-helm-charts
helm repo add solr https://solr.apache.org/charts
```

If you had already added this repo earlier, run `helm repo update` to retrieve
the latest versions of the packages

Now add the ZAC repo:
```
helm repo add zac https://infonl.github.io/dimpact-zaakafhandelcomponent
```

And install zac:
```
helm install my-release zac/zaakafhandelcomponent
```

## Changes to the helm chart

The Github workflow will perform helm-linting and will bump the version if needed. This `README.md` file is generated automatically as well.

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| additionalAllowedFileTypes | string | `nil` | An optional list of additional file extensions that can be uploaded |
| affinity | object | `{}` | set affinity parameters |
| auth.clientId | string | `""` | Client ID and secret as defined in the Keycloak ZAC realm |
| auth.realm | string | `""` |  |
| auth.secret | string | `""` |  |
| auth.server | string | `""` |  |
| auth.sslRequired | string | `""` | Whether communication with the Keycloak OpenID provider should be over HTTPS. Valid values are: "all" - to always require HTTPS, "external" - to only require HTTPS for external requests, "none" - if HTTPS is not required. This should be set to "all" in production environments. |
| autoscaling.enabled | bool | `false` |  |
| autoscaling.maxReplicas | int | `100` |  |
| autoscaling.minReplicas | int | `1` |  |
| autoscaling.targetCPUUtilizationPercentage | int | `80` |  |
| backendConfig.enabled | bool | `false` |  |
| bagApi.apiKey | string | `""` |  |
| bagApi.url | string | `""` |  |
| brpApi.apiKey | string | `""` |  |
| brpApi.protocollering.doelbinding.raadpleegmet | string | `"BRPACT-Totaal"` |  |
| brpApi.protocollering.doelbinding.zoekmet | string | `"BRPACT-ZoekenAlgemeen"` |  |
| brpApi.protocollering.originOin | string | `""` |  |
| brpApi.url | string | `""` |  |
| catalogusDomein | string | `"ALG"` | ZAC OpenZaak Catalogus Domein |
| contextUrl | string | `""` | External URL to the zaakafhandelcomponent. (https://zaakafhandelcomponent.example.com) |
| db.host | string | `""` | database.internal or 1.2.3.4 |
| db.name | string | `""` |  |
| db.password | string | `""` |  |
| db.user | string | `""` |  |
| extraDeploy | list | `[]` | Extra objects to deploy (value evaluated as a template) |
| featureFlags.bpmnSupport | bool | `false` | turns BPMN support on or off; defaults to false |
| fullnameOverride | string | `""` | fullname to use |
| gemeente.code | string | `""` |  |
| gemeente.mail | string | `""` |  |
| gemeente.naam | string | `""` |  |
| image.pullPolicy | string | `"IfNotPresent"` |  |
| image.repository | string | `"ghcr.io/infonl/zaakafhandelcomponent"` |  |
| image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion. |
| imagePullSecrets | list | `[]` | specifies image pull secrets |
| ingress.annotations | object | `{}` |  |
| ingress.className | string | `""` |  |
| ingress.enabled | bool | `false` |  |
| ingress.hosts[0].host | string | `"chart-example.local"` |  |
| ingress.hosts[0].paths[0].path | string | `"/"` |  |
| ingress.hosts[0].paths[0].pathType | string | `"ImplementationSpecific"` |  |
| ingress.tls | list | `[]` |  |
| initContainer.enabled | bool | `true` |  |
| initContainer.image.repository | string | `"curlimages/curl"` |  |
| initContainer.image.tag | string | `"8.14.1@sha256:9a1ed35addb45476afa911696297f8e115993df459278ed036182dd2cd22b67b"` |  |
| javaOptions | string | `""` | JVM startup options. defaults to "-Xmx1024m -Xms1024m -Xlog:gc::time,uptime" |
| keycloak.adminClient.id | string | `""` | Keycloak ZAC admin client name |
| keycloak.adminClient.secret | string | `""` | Keycloak ZAC admin client secret |
| klantinteractiesApi.token | string | `""` |  |
| klantinteractiesApi.url | string | `""` |  |
| kvkApi.apiKey | string | `""` |  |
| kvkApi.url | string | `""` |  |
| mail.smtp.password | string | `""` | SMTP server password if authentication is required. Optional |
| mail.smtp.port | string | `"587"` | SMTP server port: 587 for TLS, port 25 for relaying. Required |
| mail.smtp.server | string | `""` | SMTP server host (for example, localhost or in-v3.mailjet.com). Required |
| mail.smtp.username | string | `""` | SMTP server username if authentication is required. Optional |
| maxFileSizeMB | int | `80` | Maximum size (in Mega Bytes) of files that can be uploaded. |
| nameOverride | string | `""` | name to use |
| nginx.allowedHosts | string | `""` |  |
| nginx.api_proxy.bag.apikey_header_name | string | `"apikey"` |  |
| nginx.api_proxy.bag.apikey_value | string | `"test"` |  |
| nginx.api_proxy.bag.client_secret | string | `"bag_client"` |  |
| nginx.api_proxy.bag.enabled | bool | `true` |  |
| nginx.api_proxy.bag.host | string | `"bag.nl"` |  |
| nginx.api_proxy.bag.key_secret | string | `"bag_key"` |  |
| nginx.api_proxy.bag.path | string | `"/lvbag/individuelebevragingen/v2/"` |  |
| nginx.api_proxy.bag.proxy_path | string | `"/lvbag/individuelebevragingen/v2/"` |  |
| nginx.api_proxy.bag.server_secret | string | `"bag_server"` |  |
| nginx.api_proxy.bag.ssl_verify | bool | `false` |  |
| nginx.api_proxy.brp.apikey_header_name | string | `""` |  |
| nginx.api_proxy.brp.apikey_value | string | `""` |  |
| nginx.api_proxy.brp.client_secret | string | `"brp_client"` |  |
| nginx.api_proxy.brp.enabled | bool | `true` |  |
| nginx.api_proxy.brp.host | string | `"brp.nl"` |  |
| nginx.api_proxy.brp.key_secret | string | `"brp_key"` |  |
| nginx.api_proxy.brp.path | string | `"/haalcentraal/api/brp"` |  |
| nginx.api_proxy.brp.proxy_path | string | `"/haalcentraal/api/brp"` |  |
| nginx.api_proxy.brp.server_secret | string | `"brp_server"` |  |
| nginx.api_proxy.brp.ssl_verify | bool | `false` |  |
| nginx.api_proxy.brp.x_doelbinding | string | `"test"` |  |
| nginx.api_proxy.certificate_secret | string | `"nginx-certs"` |  |
| nginx.api_proxy.enabled | bool | `false` |  |
| nginx.api_proxy.kvk.basisprofiel.apikey_header_name | string | `"X-Api-Key"` |  |
| nginx.api_proxy.kvk.basisprofiel.apikey_value | string | `"test"` |  |
| nginx.api_proxy.kvk.basisprofiel.client_secret | string | `"kvk_client"` |  |
| nginx.api_proxy.kvk.basisprofiel.enabled | bool | `true` |  |
| nginx.api_proxy.kvk.basisprofiel.host | string | `"kvk.nl"` |  |
| nginx.api_proxy.kvk.basisprofiel.key_secret | string | `"kvk_key"` |  |
| nginx.api_proxy.kvk.basisprofiel.path | string | `"/test"` |  |
| nginx.api_proxy.kvk.basisprofiel.proxy_path | string | `"/test"` |  |
| nginx.api_proxy.kvk.basisprofiel.server_secret | string | `"kvk_server"` |  |
| nginx.api_proxy.kvk.basisprofiel.ssl_verify | bool | `false` |  |
| nginx.api_proxy.kvk.vestigingsprofiel.apikey_header_name | string | `"X-Api-Key"` |  |
| nginx.api_proxy.kvk.vestigingsprofiel.apikey_value | string | `"test"` |  |
| nginx.api_proxy.kvk.vestigingsprofiel.client_secret | string | `"kvk_client"` |  |
| nginx.api_proxy.kvk.vestigingsprofiel.enabled | bool | `true` |  |
| nginx.api_proxy.kvk.vestigingsprofiel.host | string | `"kvk.nl"` |  |
| nginx.api_proxy.kvk.vestigingsprofiel.key_secret | string | `"kvk_key"` |  |
| nginx.api_proxy.kvk.vestigingsprofiel.path | string | `"/test"` |  |
| nginx.api_proxy.kvk.vestigingsprofiel.proxy_path | string | `"/test"` |  |
| nginx.api_proxy.kvk.vestigingsprofiel.server_secret | string | `"kvk_server"` |  |
| nginx.api_proxy.kvk.vestigingsprofiel.ssl_verify | bool | `false` |  |
| nginx.api_proxy.kvk.zoeken.apikey_header_name | string | `"X-Api-Key"` |  |
| nginx.api_proxy.kvk.zoeken.apikey_value | string | `"test"` |  |
| nginx.api_proxy.kvk.zoeken.client_secret | string | `"kvk_client"` |  |
| nginx.api_proxy.kvk.zoeken.enabled | bool | `true` |  |
| nginx.api_proxy.kvk.zoeken.host | string | `"kvk.nl"` |  |
| nginx.api_proxy.kvk.zoeken.key_secret | string | `"kvk_key"` |  |
| nginx.api_proxy.kvk.zoeken.path | string | `"/test"` |  |
| nginx.api_proxy.kvk.zoeken.proxy_path | string | `"/test"` |  |
| nginx.api_proxy.kvk.zoeken.server_secret | string | `"kvk_server"` |  |
| nginx.api_proxy.kvk.zoeken.ssl_verify | bool | `false` |  |
| nginx.autoscaling.enabled | bool | `false` |  |
| nginx.client_max_body_size | string | `"120M"` |  |
| nginx.enabled | bool | `false` |  |
| nginx.existingConfigmap | string | `nil` | mount existing nginx vhost config |
| nginx.image.pullPolicy | string | `"IfNotPresent"` |  |
| nginx.image.repository | string | `"nginxinc/nginx-unprivileged"` |  |
| nginx.image.tag | string | `"1.29.0@sha256:5957d8d004517de8a53af6812bdb1b779d7c6746a611569af59b22660925f47b"` |  |
| nginx.livenessProbe.failureThreshold | int | `3` |  |
| nginx.livenessProbe.initialDelaySeconds | int | `60` |  |
| nginx.livenessProbe.periodSeconds | int | `10` |  |
| nginx.livenessProbe.successThreshold | int | `1` |  |
| nginx.livenessProbe.timeoutSeconds | int | `5` |  |
| nginx.podLabels | object | `{}` |  |
| nginx.readinessProbe.failureThreshold | int | `3` |  |
| nginx.readinessProbe.initialDelaySeconds | int | `30` |  |
| nginx.readinessProbe.periodSeconds | int | `10` |  |
| nginx.readinessProbe.successThreshold | int | `1` |  |
| nginx.readinessProbe.timeoutSeconds | int | `5` |  |
| nginx.replicaCount | int | `1` |  |
| nginx.resources | object | `{}` |  |
| nginx.securityContext.capabilities.drop[0] | string | `"ALL"` |  |
| nginx.securityContext.readOnlyRootFilesystem | bool | `false` |  |
| nginx.securityContext.runAsNonRoot | bool | `true` |  |
| nginx.securityContext.runAsUser | int | `101` |  |
| nginx.service.annotations | object | `{}` |  |
| nginx.service.port | int | `80` |  |
| nginx.service.type | string | `"ClusterIP"` |  |
| nginx.useXForwardedHost | bool | `false` |  |
| nodeSelector | object | `{}` | set node selector parameters |
| notificationsSecretKey | string | `""` | API key for the ZGW Notificaties Consumer API integration; also needs to be configured in Open Notificaties |
| objectenApi.token | string | `""` |  |
| objectenApi.url | string | `""` |  |
| office_converter.affinity | object | `{}` |  |
| office_converter.enabled | bool | `true` |  |
| office_converter.image.pullPolicy | string | `"IfNotPresent"` |  |
| office_converter.image.repository | string | `"ghcr.io/eugenmayer/kontextwork-converter"` |  |
| office_converter.image.tag | string | `"1.8.0@sha256:48da70902307f27ad92a27ddf5875310464fd4d4a2f53ce53e1a6f9b3b4c3355"` |  |
| office_converter.imagePullSecrets | list | `[]` |  |
| office_converter.name | string | `"office-converter"` |  |
| office_converter.nodeSelector | object | `{}` |  |
| office_converter.podAnnotations | object | `{}` |  |
| office_converter.podSecurityContext | object | `{}` |  |
| office_converter.replicas | int | `1` |  |
| office_converter.resources.requests.cpu | string | `"100m"` |  |
| office_converter.resources.requests.memory | string | `"512Mi"` |  |
| office_converter.securityContext | object | `{}` |  |
| office_converter.service.annotations | object | `{}` |  |
| office_converter.service.port | int | `80` |  |
| office_converter.service.type | string | `"ClusterIP"` |  |
| office_converter.tolerations | list | `[]` |  |
| opa.affinity | object | `{}` |  |
| opa.autoscaling.enabled | bool | `false` |  |
| opa.enabled | bool | `true` |  |
| opa.image.pullPolicy | string | `"IfNotPresent"` |  |
| opa.image.repository | string | `"openpolicyagent/opa"` |  |
| opa.image.tag | string | `"1.6.0-static@sha256:3e5a77e73b42c4911ff2a9286f9ba280b273afc17784f7e5d8ba69db22a1e1c0"` |  |
| opa.imagePullSecrets | list | `[]` |  |
| opa.name | string | `"opa"` |  |
| opa.nodeSelector | object | `{}` |  |
| opa.podAnnotations | object | `{}` |  |
| opa.podSecurityContext | object | `{}` |  |
| opa.replicas | int | `1` |  |
| opa.resources.requests.cpu | string | `"10m"` |  |
| opa.resources.requests.memory | string | `"20Mi"` |  |
| opa.securityContext | object | `{}` |  |
| opa.service.annotations | object | `{}` |  |
| opa.service.port | int | `8181` |  |
| opa.service.type | string | `"ClusterIP"` |  |
| opa.sidecar | bool | `false` | set sidecar to true to run the opa service together with the zac pod |
| opa.tolerations | list | `[]` |  |
| openForms.url | string | `""` | Not used at the moment. |
| opentelemetry-collector.config.receivers.jaeger | object | `{}` |  |
| opentelemetry-collector.config.receivers.prometheus | object | `{}` |  |
| opentelemetry-collector.config.receivers.zipkin | object | `{}` |  |
| opentelemetry-collector.config.service.pipelines.logs | object | `{}` |  |
| opentelemetry-collector.config.service.pipelines.metrics | object | `{}` |  |
| opentelemetry-collector.config.service.pipelines.traces.receivers[0] | string | `"otlp"` |  |
| opentelemetry-collector.enabled | bool | `false` |  |
| opentelemetry-collector.image.pullPolicy | string | `"IfNotPresent"` |  |
| opentelemetry-collector.image.repository | string | `"otel/opentelemetry-collector-contrib"` |  |
| opentelemetry-collector.image.tag | string | `"0.130.0@sha256:867d1074c2f750936fb9358ec9eefa009308053cf156b2c7ca1761ba5ef78452"` |  |
| opentelemetry-collector.mode | string | `"deployment"` |  |
| opentelemetry-collector.ports.jaeger-compact.enabled | bool | `false` |  |
| opentelemetry-collector.ports.jaeger-grpc.enabled | bool | `false` |  |
| opentelemetry-collector.ports.jaeger-thrift.enabled | bool | `false` |  |
| opentelemetry-collector.ports.zipkin.enabled | bool | `false` |  |
| opentelemetry-collector.presets.clusterMetrics.enabled | bool | `false` |  |
| opentelemetry-collector.replicaCount | int | `1` |  |
| opentelemetry_zaakafhandelcomponent.disabled | string | `"-true"` | Enables or disables the ZAC OpenTelemetry integration. Disabled by default. |
| opentelemetry_zaakafhandelcomponent.endpoint | string | `""` | OpenTelemetry collector endpoint URL |
| organizations.bron.rsin | string | `""` | The RSIN of the Non-natural person - the organization that created the zaak. Must be a valid RSIN of 9 numbers and comply with https://nl.wikipedia.org/wiki/Burgerservicenummer#11-proef |
| organizations.verantwoordelijke.rsin | string | `""` | The RSIN of the Non-natural person - the organization that is ultimately responsible for handling a zaak or establishing a decision. Must be a valid RSIN of 9 numbers and comply with https://nl.wikipedia.org/wiki/Burgerservicenummer#11-proef |
| podAnnotations | object | `{}` | pod specific annotations |
| podSecurityContext | object | `{}` | pod specific security context |
| remoteDebug | bool | `false` | Enable Java remote debugging |
| replicaCount | int | `1` | The number of replicas to run |
| resources.requests.cpu | string | `"100m"` |  |
| resources.requests.memory | string | `"1Gi"` |  |
| securityContext | object | `{}` | generic security context |
| service.annotations | object | `{}` |  |
| service.port | int | `80` |  |
| service.type | string | `"ClusterIP"` |  |
| serviceAccount.annotations | object | `{}` | Annotations to add to the service account |
| serviceAccount.create | bool | `true` | Specifies whether a service account should be created |
| serviceAccount.name | string | `""` | The name of the service account to use. If not set and create is true, a name is generated using the fullname template |
| signaleringen.affinity | object | `{}` |  |
| signaleringen.concurrencyPolicy | string | `"Forbid"` |  |
| signaleringen.deleteOldSignaleringenSchedule | string | `"0 3 * * *"` | Schedule of the 'delete old signaleringen' send job in CRON job format |
| signaleringen.deleteOlderThanDays | string | `"14"` | Delete any signaleringen older than this number of days when the corresponding admin endpoint is called. |
| signaleringen.failedJobsHistoryLimit | int | `3` |  |
| signaleringen.image.pullPolicy | string | `"IfNotPresent"` |  |
| signaleringen.image.repository | string | `"curlimages/curl"` |  |
| signaleringen.image.tag | string | `"8.14.1@sha256:9a1ed35addb45476afa911696297f8e115993df459278ed036182dd2cd22b67b"` |  |
| signaleringen.imagePullSecrets | list | `[]` |  |
| signaleringen.nodeSelector | object | `{}` |  |
| signaleringen.podSecurityContext | object | `{}` |  |
| signaleringen.resources | object | `{}` |  |
| signaleringen.restartPolicy | string | `"Never"` |  |
| signaleringen.securityContext | object | `{}` |  |
| signaleringen.sendZaakSignaleringenSchedule | string | `"0 2 * * *"` | Schedule of the signaleringen send zaken job in CRON job format |
| signaleringen.successfulJobsHistoryLimit | int | `1` | k8s settings for the signaleren jobs |
| signaleringen.tolerations | list | `[]` |  |
| smartDocuments.authentication | string | `""` | Authentication token |
| smartDocuments.enabled | bool | `false` | Enable SmartDocuments integration for creating a new document |
| smartDocuments.fixedUserName | string | `""` | If set this overrides the sending of the username of the user that is logged in to ZAC to SmartDocuments with a fixed value. This username is sent to SmartDocuments when creating a new document as an HTTP header. For most target environments, this should not be set, assuming that all users that are available in ZAC are also available in the SmartDocuments environment with the same username. If this setting is set, then templates in SmartDocuments cannot use user-specific values. |
| smartDocuments.url | string | `""` | URL to SmartDocuments instance. For example: https://partners.smartdocuments.com |
| smartDocuments.wizardAuthEnabled | bool | `true` | [OPTIONAL] Normal attended wizard flow started with user; when set to false no user added to the request and a special no_auth SmartDocuments URL is used |
| solr-operator.affinity | object | `{}` | affinity for solr-operator |
| solr-operator.annotations | object | `{}` | annotations for solr-operator |
| solr-operator.enabled | bool | `false` | set enabled to actually use the solr-operator helm chart |
| solr-operator.fullnameOverride | string | `"solr-operator"` | set fullname for solr-operator |
| solr-operator.image.pullPolicy | string | `"IfNotPresent"` | solr-operator imagePullPolicy |
| solr-operator.image.repository | string | `"apache/solr-operator"` | solr-operator repository |
| solr-operator.image.tag | string | `"v0.9.1@sha256:4db34508137f185d3cad03c7cf7c2b5d6533fb590822effcde9125cff5a90aa2"` | solr-operator tag |
| solr-operator.metrics.enabled | bool | `true` | enable to have solr-operator metric endpoints |
| solr-operator.nodeSelector | object | `{}` | nodeSelector for solr-operator |
| solr-operator.solr.affinity | object | `{}` | affinity for solr in solrcloud |
| solr-operator.solr.annotations | object | `{}` | annotations for solr in solrcloud |
| solr-operator.solr.busyBoxImage.pullPolicy | string | `"IfNotPresent"` | solr busybox image imagePullPolicy |
| solr-operator.solr.busyBoxImage.repository | string | `"library/busybox"` | solr busybox image reposity |
| solr-operator.solr.busyBoxImage.tag | string | `"1.37.0-glibc@sha256:bd606c263abed91a141187b92fdb54b87bbc39cfb9068f96ad84196a36963103"` | solr busybox image tag |
| solr-operator.solr.enabled | bool | `true` | enable configuration of a solrcloud |
| solr-operator.solr.image.pullPolicy | string | `"IfNotPresent"` | solr imagePullPolicy |
| solr-operator.solr.image.repository | string | `"library/solr"` | solr image repository |
| solr-operator.solr.image.tag | string | `"9.8.1@sha256:436be8c80a51cfbd077afade3d0563afe08a7f26556894e197cab44feec56b6f"` | solr image tag |
| solr-operator.solr.javaMem | string | `"-Xms512m -Xmx768m"` | solr memory settings |
| solr-operator.solr.jobs.affinity | object | `{}` | affinity for jobs |
| solr-operator.solr.jobs.annotations | object | `{}` | annotations for jobs |
| solr-operator.solr.jobs.createZacCore | bool | `true` | enable createZacCore to have a curl statement generate the zac core in the provided solrcloud if it does not exist yet |
| solr-operator.solr.jobs.image.pullPolicy | string | `"IfNotPresent"` | solr jobs imagePullPolicy |
| solr-operator.solr.jobs.image.repository | string | `"curlimages/curl"` | solr jobs repository |
| solr-operator.solr.jobs.image.tag | string | `"8.14.1@sha256:9a1ed35addb45476afa911696297f8e115993df459278ed036182dd2cd22b67b"` | solr jobs tag |
| solr-operator.solr.jobs.nodeSelector | object | `{}` | nodeSelector for jobs |
| solr-operator.solr.jobs.tolerations | list | `[]` | tolerations for jobs |
| solr-operator.solr.logLevel | string | `"INFO"` | solr loglevel |
| solr-operator.solr.nodeSelector | object | `{}` | nodeSelector for solr in solrcloud |
| solr-operator.solr.replicas | int | `3` | replicas for solr in solrcloud, should be an odd number |
| solr-operator.solr.storage.reclaimPolicy | string | `"Delete"` | solr storage reclaimPolicy |
| solr-operator.solr.storage.size | string | `"1Gi"` | solr storage size |
| solr-operator.solr.storage.storageClassName | string | `"managed-csi"` | solr storage storageClassName |
| solr-operator.solr.tolerations | list | `[]` | tolerations for solr in solrcloud |
| solr-operator.tolerations | list | `[]` | tolerations for solr-operator |
| solr-operator.watchNamespaces | string | `"default"` | a comma-seperated list of namespaces to watch, watches all namespaces if empty |
| solr-operator.zookeeper-operator.affinity | object | `{}` | affinity for zookeeper-operator |
| solr-operator.zookeeper-operator.annotations | object | `{}` | annotations for zookeeper-operator |
| solr-operator.zookeeper-operator.fullnameOverride | string | `"zookeeper-operator"` | set fullname for zookeeper-operator |
| solr-operator.zookeeper-operator.hooks.image.pullPolicy | string | `"IfNotPresent"` | zookeeper-operator hooks imagePullPolicy |
| solr-operator.zookeeper-operator.hooks.image.repository | string | `"lachlanevenson/k8s-kubectl"` | zookeeper-operator hooks repository |
| solr-operator.zookeeper-operator.hooks.image.tag | string | `"v1.25.4@sha256:af5cea3f2e40138df90660c0c073d8b1506fb76c8602a9f48aceb5f4fb052ddc"` | zookeeper-operator hooks tag |
| solr-operator.zookeeper-operator.image.pullPolicy | string | `"IfNotPresent"` | zookeeper-operator imagePullPolicy |
| solr-operator.zookeeper-operator.image.repository | string | `"pravega/zookeeper-operator"` | zookeeper-operator image repository |
| solr-operator.zookeeper-operator.image.tag | string | `"0.2.15@sha256:b2bc4042fdd8fea6613b04f2f602ba4aff1201e79ba35cd0e2df9f3327111b0e"` | zookeeper-operator image tag |
| solr-operator.zookeeper-operator.nodeSelector | object | `{}` | nodeSelector for solr-operator |
| solr-operator.zookeeper-operator.tolerations | list | `[]` | tolerations for solr-operator |
| solr-operator.zookeeper-operator.watchNamespace | string | `"default"` | a comma-seperated list of namespaces to watch, watches all namespaces if empty |
| solr-operator.zookeeper-operator.zookeeper.affinity | object | `{}` | affinity for zookeeper |
| solr-operator.zookeeper-operator.zookeeper.annotations | object | `{}` | annotations for zookeeper |
| solr-operator.zookeeper-operator.zookeeper.image.pullPolicy | string | `"IfNotPresent"` | zookeeper imagePullPolicy |
| solr-operator.zookeeper-operator.zookeeper.image.repository | string | `"pravega/zookeeper"` | zookeeper image repository |
| solr-operator.zookeeper-operator.zookeeper.image.tag | string | `"0.2.15@sha256:c498ebfb76a66f038075e2fa6148528d74d31ca1664f3257fdf82ee779eec9c8"` | zookeeper image tag |
| solr-operator.zookeeper-operator.zookeeper.nodeSelector | object | `{}` | nodeSelector for zookeeper |
| solr-operator.zookeeper-operator.zookeeper.replicas | int | `3` | replicas for zookeeper, should be an odd number |
| solr-operator.zookeeper-operator.zookeeper.storage.reclaimPolicy | string | `"Delete"` | zookeeper storage reclaimPolicy |
| solr-operator.zookeeper-operator.zookeeper.storage.size | string | `"1Gi"` | zookeeper storage size |
| solr-operator.zookeeper-operator.zookeeper.storage.storageClassName | string | `"managed-csi"` | zookeeper storageClassName |
| solr-operator.zookeeper-operator.zookeeper.tolerations | list | `[]` | tolerations for zookeeper |
| solr.url | string | `""` | The location of an existing solr instance to be used by zac |
| tolerations | list | `[]` | set toleration parameters |
| zacInternalEndpointsApiKey | string | `""` | API key for authentication of internal ZAC endpoints |
| zgwApis.clientId | string | `""` |  |
| zgwApis.secret | string | `""` |  |
| zgwApis.url | string | `""` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.14.2](https://github.com/norwoodj/helm-docs/releases/v1.14.2)

