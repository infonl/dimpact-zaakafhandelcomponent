# zaakafhandelcomponent

![Version: 1.0.27](https://img.shields.io/badge/Version-1.0.27-informational?style=flat-square) ![AppVersion: 3.0](https://img.shields.io/badge/AppVersion-3.0-informational?style=flat-square)

A Helm chart for installing Zaakafhandelcomponent

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Team Dimpact, INFO | <teamdimpact@info.nl> | <https://github.com/infonl/dimpact-zaakafhandelcomponent/discussions> |

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| @bitnami | solr | 9.6.0 |
| @opentelemetry | opentelemetry-collector | 0.104.0 |
| @solr | solr-operator | 0.9.1 |

## Usage

Make sure you have helm installed. Add the required repositories as follows:
```
helm repo add opentelemetry https://open-telemetry.github.io/opentelemetry-helm-charts
helm repo add bitnami https://charts.bitnami.com/bitnami
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

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| affinity | object | `{}` | set affinity parameters |
| auth.clientId | string | `""` | Client ID and secret as defined in the realm |
| auth.realm | string | `""` |  |
| auth.secret | string | `""` |  |
| auth.server | string | `""` |  |
| autoscaling.enabled | bool | `false` |  |
| autoscaling.maxReplicas | int | `100` |  |
| autoscaling.minReplicas | int | `1` |  |
| autoscaling.targetCPUUtilizationPercentage | int | `80` |  |
| backendConfig.enabled | bool | `false` |  |
| bagApi.apiKey | string | `""` |  |
| bagApi.url | string | `""` |  |
| brpApi.apiKey | string | `""` |  |
| brpApi.protocollering.doelbinding | string | `"BRPACT-Totaal"` | Doelbinding for BRP Protocollering |
| brpApi.protocollering.originOin | string | `""` | If specified, enables the BRP Protocollering |
| brpApi.protocollering.verwerking | string | `"zaakafhandelcomponent"` | Verwerking for BRP Protocollering |
| brpApi.url | string | `""` |  |
| catalogusDomein | string | `"ALG"` | OpenZaak Catalogus Domein |
| contextUrl | string | `""` | External URL to the zaakafhandelcomponent. (https://zaakafhandelcomponent.example.com) |
| db.host | string | `""` |  |
| db.name | string | `""` |  |
| db.password | string | `""` |  |
| db.user | string | `""` |  |
| extraDeploy | list | `[]` | Extra objects to deploy (value evaluated as a template) |
| featureFlags.bpmnSupport | bool | `false` |  |
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
| initContainer | object | `{"enabled":true,"image":{"repository":"curlimages/curl","tag":"8.12.1@sha256:94e9e444bcba979c2ea12e27ae39bee4cd10bc7041a472c4727a558e213744e6"}}` | set initContainer parameters |
| keycloak.adminClient.id | string | `""` | Keycloak ZAC admin client name |
| keycloak.adminClient.secret | string | `""` | Keycloak ZAC admin client secret |
| klantinteractiesApi.token | string | `""` |  |
| klantinteractiesApi.url | string | `""` |  |
| kvkApi.apiKey | string | `""` |  |
| kvkApi.url | string | `""` |  |
| mail | object | `{"smtp":{"password":"","port":"587","server":"","username":""}}` | Email sending connection. SPF record needs to be properly setup in DNS |
| mail.smtp.password | string | `""` | SMTP server password if authentication is required. Optional |
| mail.smtp.port | string | `"587"` | SMTP server port: 587 for TLS, port 25 for relaying. Required |
| mail.smtp.server | string | `""` | SMTP server host (for example localhost or in-v3.mailjet.com). Required |
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
| nginx.image.tag | string | `"1.27.4@sha256:840f33319fb642e32a15a1772400e017e1175891c98afdff3a47871c925cb0e9"` |  |
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
| notificationsSecretKey | string | `""` | Configuration of the notifications receiving endpoint. |
| objectenApi.token | string | `""` |  |
| objectenApi.url | string | `""` |  |
| office_converter.affinity | object | `{}` |  |
| office_converter.enabled | bool | `true` |  |
| office_converter.image.pullPolicy | string | `"IfNotPresent"` |  |
| office_converter.image.repository | string | `"ghcr.io/eugenmayer/kontextwork-converter"` |  |
| office_converter.image.tag | string | `"1.7.2@sha256:475b52be29912d5cf3c2751003833f4fe70f44167275aed9b401c768ff91f093"` |  |
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
| opa.image.tag | string | `"1.3.0-static@sha256:44f0f4b1c09260eaf5e24fc3931fe10f80cffd13054ef3ef62cef775d5cbd272"` |  |
| opa.imagePullSecrets | list | `[]` |  |
| opa.name | string | `"opa"` | set url if the opa url cannot be automatically determined and is not run as a sidecar. the opa url should be the url the openpolicyagent can be reached on from ZAC ( for example: http://release-opa.default.svc.cluster.local:8181 ) url: "" |
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
| openForms.url | string | `""` |  |
| opentelemetry-collector.config.receivers.jaeger | object | `{}` |  |
| opentelemetry-collector.config.receivers.prometheus | object | `{}` |  |
| opentelemetry-collector.config.receivers.zipkin | object | `{}` |  |
| opentelemetry-collector.config.service.pipelines.logs | object | `{}` |  |
| opentelemetry-collector.config.service.pipelines.metrics | object | `{}` |  |
| opentelemetry-collector.config.service.pipelines.traces.receivers[0] | string | `"otlp"` |  |
| opentelemetry-collector.enabled | bool | `false` |  |
| opentelemetry-collector.image.repository | string | `"otel/opentelemetry-collector-contrib"` |  |
| opentelemetry-collector.mode | string | `"deployment"` |  |
| opentelemetry-collector.ports.jaeger-compact.enabled | bool | `false` |  |
| opentelemetry-collector.ports.jaeger-grpc.enabled | bool | `false` |  |
| opentelemetry-collector.ports.jaeger-thrift.enabled | bool | `false` |  |
| opentelemetry-collector.ports.zipkin.enabled | bool | `false` |  |
| opentelemetry-collector.presets.clusterMetrics.enabled | bool | `false` |  |
| opentelemetry-collector.replicaCount | int | `1` |  |
| organizations.bron.rsin | string | `""` | The RSIN of the Non-natural person - the organization that created the zaak. Must be a valid RSIN of 9 numbers and comply with https://nl.wikipedia.org/wiki/Burgerservicenummer#11-proef |
| organizations.verantwoordelijke.rsin | string | `""` | The RSIN of the Non-natural person - the organization that is ultimately responsible for handling a zaak or establishing a decision. Must be a valid RSIN of 9 numbers and comply with https://nl.wikipedia.org/wiki/Burgerservicenummer#11-proef |
| podAnnotations | object | `{}` | pod specific annotations |
| podSecurityContext | object | `{}` | pod specific security context |
| remoteDebug | bool | `false` | Enable Java remote debugging |
| replicaCount | int | `1` | the number of replicas to run |
| resources.requests.cpu | string | `"100m"` |  |
| resources.requests.memory | string | `"1Gi"` |  |
| securityContext | object | `{}` |  |
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
| signaleringen.image.tag | string | `"8.12.1@sha256:94e9e444bcba979c2ea12e27ae39bee4cd10bc7041a472c4727a558e213744e6"` |  |
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
| smartDocuments.fixedUserName | string | `""` | Fixed username for authentication |
| smartDocuments.url | string | `""` | URL to SmartDocuments instance. For example: https://partners.smartdocuments.com |
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
| solr-operator.solr.busyBoxImage.tag | string | `"1.37.0-glibc@sha256:04c3917ae1ad16d8be9702176a1e1ecd3cfe6b374a274bd52382c001b4ecd088"` | solr busybox image tag |
| solr-operator.solr.enabled | bool | `true` | enable configuration of a solrcloud |
| solr-operator.solr.image.pullPolicy | string | `"IfNotPresent"` | solr imagePullPolicy |
| solr-operator.solr.image.repository | string | `"library/solr"` | solr image repository |
| solr-operator.solr.image.tag | string | `"9.8.1@sha256:16983468366aaf62417bb6a2a4b703b486b199b8461192df131455071c263916"` | solr image tag |
| solr-operator.solr.javaMem | string | `"-Xms512m -Xmx768m"` | solr memory settings |
| solr-operator.solr.jobs.affinity | object | `{}` | affinity for jobs |
| solr-operator.solr.jobs.annotations | object | `{}` | annotations for jobs |
| solr-operator.solr.jobs.createZacCore | bool | `true` | enable createZacCore to have a curl statement generate the zac core in the provided solrcloud if it does not exist yet |
| solr-operator.solr.jobs.image.pullPolicy | string | `"IfNotPresent"` | solr jobs imagePullPolicy |
| solr-operator.solr.jobs.image.repository | string | `"curlimages/curl"` | solr jobs repository |
| solr-operator.solr.jobs.image.tag | string | `"8.12.1@sha256:94e9e444bcba979c2ea12e27ae39bee4cd10bc7041a472c4727a558e213744e6"` | solr jobs tag |
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
| solr.auth.enabled | bool | `false` |  |
| solr.cloudBootstrap | bool | `false` |  |
| solr.cloudEnabled | bool | `false` |  |
| solr.collectionReplicas | int | `1` |  |
| solr.coreNames[0] | string | `"zac"` |  |
| solr.customLivenessProbe.failureThreshold | int | `6` |  |
| solr.customLivenessProbe.httpGet.path | string | `"/solr/zac/admin/ping"` |  |
| solr.customLivenessProbe.httpGet.port | string | `"http"` |  |
| solr.customLivenessProbe.initialDelaySeconds | int | `40` |  |
| solr.customLivenessProbe.periodSeconds | int | `10` |  |
| solr.customLivenessProbe.successThreshold | int | `1` |  |
| solr.customLivenessProbe.timeoutSeconds | int | `15` |  |
| solr.customReadinessProbe.failureThreshold | int | `6` |  |
| solr.customReadinessProbe.httpGet.path | string | `"/solr/zac/admin/ping"` |  |
| solr.customReadinessProbe.httpGet.port | string | `"http"` |  |
| solr.customReadinessProbe.initialDelaySeconds | int | `60` |  |
| solr.customReadinessProbe.periodSeconds | int | `10` |  |
| solr.customReadinessProbe.successThreshold | int | `1` |  |
| solr.customReadinessProbe.timeoutSeconds | int | `15` |  |
| solr.enabled | bool | `false` | set enabled to true to provision bitnami solr version with the zac core |
| solr.extraEnvVars[0].name | string | `"ZK_CREATE_CHROOT"` |  |
| solr.extraEnvVars[0].value | string | `"true"` |  |
| solr.persistence.size | string | `"1Gi"` |  |
| solr.replicaCount | int | `1` |  |
| solr.service.ports.http | int | `80` |  |
| solr.zookeeper.enabled | bool | `false` |  |
| tolerations | list | `[]` | set toleration parameters |
| zgwApis.clientId | string | `""` |  |
| zgwApis.secret | string | `""` |  |
| zgwApis.url | string | `""` |  |
| zgwApis.urlExtern | string | `""` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.14.2](https://github.com/norwoodj/helm-docs/releases/v1.14.2)

