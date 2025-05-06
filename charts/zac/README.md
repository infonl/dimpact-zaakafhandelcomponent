# zaakafhandelcomponent

![Version: 1.0.59](https://img.shields.io/badge/Version-1.0.59-informational?style=flat-square) ![AppVersion: 3.0](https://img.shields.io/badge/AppVersion-3.0-informational?style=flat-square)

A Helm chart for installing Zaakafhandelcomponent

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Team Dimpact, INFO | <teamdimpact@info.nl> | <https://github.com/infonl/dimpact-zaakafhandelcomponent/discussions> |

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| @bitnami | solr | 9.6.1 |
| @opentelemetry | opentelemetry-collector | 0.122.5 |
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

## Changes to the helm chart

The Github workflow will perform helm-linting and will bump the version if needed. This `README.md` file is generated automatically as well.

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| additionalAllowedFileTypes | string | `nil` | An optional list of additional file extensions that can be uploaded |
| affinity | object | `{}` | set affinity parameters |
| auth | object | `{"clientId":"","realm":"","secret":"","server":""}` | Configuration for the Keycloak OpenID Connect integration |
| auth.clientId | string | `""` | Client ID and secret as defined in the Keycloak ZAC realm |
| autoscaling | object | `{"enabled":false,"maxReplicas":100,"minReplicas":1,"targetCPUUtilizationPercentage":80}` | autoscaling parameters |
| backendConfig | object | `{"enabled":false}` | currently not in use, specify backend in ingress instead |
| bagApi | object | `{"apiKey":"","url":""}` | Integration with the BAG API provider (Kadaster) |
| brpApi.apiKey | string | `""` |  |
| brpApi.protocollering.doelbinding | string | `"BRPACT-Totaal"` | Doelbinding for BRP Protocollering |
| brpApi.protocollering.originOin | string | `""` | If specified, enables the BRP Protocollering |
| brpApi.protocollering.verwerking | string | `"zaakafhandelcomponent"` | Verwerking for BRP Protocollering |
| brpApi.url | string | `""` |  |
| catalogusDomein | string | `"ALG"` | ZAC OpenZaak Catalogus Domein |
| contextUrl | string | `""` | External URL to the zaakafhandelcomponent. (https://zaakafhandelcomponent.example.com) |
| db | object | `{"host":"","name":"","password":"","user":""}` | Configuration of the ZAC database connection |
| db.host | string | `""` | database.internal or 1.2.3.4 |
| extraDeploy | list | `[]` | Extra objects to deploy (value evaluated as a template) |
| featureFlags | object | `{"bpmnSupport":false}` | Supported feature flags |
| featureFlags.bpmnSupport | bool | `false` | turns BPMN support on or off |
| fullnameOverride | string | `""` | fullname to use |
| gemeente | object | `{"code":"","mail":"","naam":""}` | Council specific configuration |
| image.pullPolicy | string | `"IfNotPresent"` |  |
| image.repository | string | `"ghcr.io/infonl/zaakafhandelcomponent"` |  |
| image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion. |
| imagePullSecrets | list | `[]` | specifies image pull secrets |
| ingress | object | `{"annotations":{},"className":"","enabled":false,"hosts":[{"host":"chart-example.local","paths":[{"path":"/","pathType":"ImplementationSpecific"}]}],"tls":[]}` | ingress specifications |
| initContainer | object | `{"enabled":true,"image":{"repository":"curlimages/curl","tag":"8.13.0@sha256:d43bdb28bae0be0998f3be83199bfb2b81e0a30b034b6d7586ce7e05de34c3fd"}}` | set initContainer parameters |
| keycloak | object | `{"adminClient":{"id":"","secret":""}}` | Configuration of the ZAC admin client in Keycloak |
| keycloak.adminClient.id | string | `""` | Keycloak ZAC admin client name |
| keycloak.adminClient.secret | string | `""` | Keycloak ZAC admin client secret |
| klantinteractiesApi | object | `{"token":"","url":""}` | Integration with the Klantinteracties API provider (OpenKlant) |
| kvkApi | object | `{"apiKey":"","url":""}` | Integration with the KVK API provider (KVK) |
| mail | object | `{"smtp":{"password":"","port":"587","server":"","username":""}}` | SMTP provider configuration. SPF record needs to be properly set up in DNS |
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
| nginx.image.tag | string | `"1.28.0@sha256:aa538e1dc81068827c28ad5855bbd721f0f17a3d303f4b5d6737ddc219d1c8c6"` |  |
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
| objectenApi | object | `{"token":"","url":""}` | Integration with the Objecten API provider |
| office_converter | object | `{"affinity":{},"enabled":true,"image":{"pullPolicy":"IfNotPresent","repository":"ghcr.io/eugenmayer/kontextwork-converter","tag":"1.8.0@sha256:48da70902307f27ad92a27ddf5875310464fd4d4a2f53ce53e1a6f9b3b4c3355"},"imagePullSecrets":[],"name":"office-converter","nodeSelector":{},"podAnnotations":{},"podSecurityContext":{},"replicas":1,"resources":{"requests":{"cpu":"100m","memory":"512Mi"}},"securityContext":{},"service":{"annotations":{},"port":80,"type":"ClusterIP"},"tolerations":[]}` | office_converter configuration. Prefilled resources values are the minimum recommended |
| opa | object | `{"affinity":{},"autoscaling":{"enabled":false},"enabled":true,"image":{"pullPolicy":"IfNotPresent","repository":"openpolicyagent/opa","tag":"1.4.0-static@sha256:8eb5ef478f757fabba76dfdafb58ab85667c151415b4f3689d9f05acc635d8ea"},"imagePullSecrets":[],"name":"opa","nodeSelector":{},"podAnnotations":{},"podSecurityContext":{},"replicas":1,"resources":{"requests":{"cpu":"10m","memory":"20Mi"}},"securityContext":{},"service":{"annotations":{},"port":8181,"type":"ClusterIP"},"sidecar":false,"tolerations":[]}` | Open Policy Agent (OPA) configuration, prefilled resources values are the minimum recommended |
| opa.name | string | `"opa"` | set url if the opa url cannot be automatically determined and is not run as a sidecar. the opa url should be the url the openpolicyagent can be reached on from ZAC ( for example: http://release-opa.default.svc.cluster.local:8181 ) url: "" |
| opa.sidecar | bool | `false` | set sidecar to true to run the opa service together with the zac pod |
| openForms | object | `{"url":""}` | Integration with Open Formulieren. Not used at the moment. |
| opentelemetry-collector | object | `{"config":{"receivers":{"jaeger":{},"prometheus":{},"zipkin":{}},"service":{"pipelines":{"logs":{},"metrics":{},"traces":{"receivers":["otlp"]}}}},"enabled":false,"image":{"pullPolicy":"IfNotPresent","repository":"otel/opentelemetry-collector-contrib","tag":"0.123.0@sha256:e39311df1f3d941923c00da79ac7ba6269124a870ee87e3c3ad24d60f8aee4d2"},"mode":"deployment","ports":{"jaeger-compact":{"enabled":false},"jaeger-grpc":{"enabled":false},"jaeger-thrift":{"enabled":false},"zipkin":{"enabled":false}},"presets":{"clusterMetrics":{"enabled":false}},"replicaCount":1}` | opentelemetry-collector enable to use the included helm chart and settings to work with zac |
| opentelemetry_zaakafhandelcomponent | object | `{"disabled":"-true","endpoint":""}` | OpenTelemetry configuration. Only read when opentelemetry-collector is enabled |
| opentelemetry_zaakafhandelcomponent.endpoint | string | `""` | OpenTelemetry collector URL. For example: http://opentelemetry-collector.default.svc.cluster.local:4317 |
| organizations.bron.rsin | string | `""` | The RSIN of the Non-natural person - the organization that created the zaak. Must be a valid RSIN of 9 numbers and comply with https://nl.wikipedia.org/wiki/Burgerservicenummer#11-proef |
| organizations.verantwoordelijke.rsin | string | `""` | The RSIN of the Non-natural person - the organization that is ultimately responsible for handling a zaak or establishing a decision. Must be a valid RSIN of 9 numbers and comply with https://nl.wikipedia.org/wiki/Burgerservicenummer#11-proef |
| podAnnotations | object | `{}` | pod specific annotations |
| podSecurityContext | object | `{}` | pod specific security context |
| remoteDebug | bool | `false` | Enable Java remote debugging |
| replicaCount | int | `1` | The number of replicas to run |
| resources | object | `{"requests":{"cpu":"100m","memory":"1Gi"}}` | specify resource limits and requests if needed, prefilled values are the minimum recommended |
| securityContext | object | `{}` | generic security context |
| service | object | `{"annotations":{},"port":80,"type":"ClusterIP"}` | service specifications |
| serviceAccount | object | `{"annotations":{},"create":true,"name":""}` | serviceAccount service account parameters |
| serviceAccount.annotations | object | `{}` | Annotations to add to the service account |
| serviceAccount.create | bool | `true` | Specifies whether a service account should be created |
| serviceAccount.name | string | `""` | The name of the service account to use. If not set and create is true, a name is generated using the fullname template |
| signaleringen | object | `{"affinity":{},"concurrencyPolicy":"Forbid","deleteOldSignaleringenSchedule":"0 3 * * *","deleteOlderThanDays":"14","failedJobsHistoryLimit":3,"image":{"pullPolicy":"IfNotPresent","repository":"curlimages/curl","tag":"8.13.0@sha256:d43bdb28bae0be0998f3be83199bfb2b81e0a30b034b6d7586ce7e05de34c3fd"},"imagePullSecrets":[],"nodeSelector":{},"podSecurityContext":{},"resources":{},"restartPolicy":"Never","securityContext":{},"sendZaakSignaleringenSchedule":"0 2 * * *","successfulJobsHistoryLimit":1,"tolerations":[]}` | Signaleringen cronjob configuration |
| signaleringen.deleteOldSignaleringenSchedule | string | `"0 3 * * *"` | Schedule of the 'delete old signaleringen' send job in CRON job format |
| signaleringen.deleteOlderThanDays | string | `"14"` | Delete any signaleringen older than this number of days when the corresponding admin endpoint is called. |
| signaleringen.sendZaakSignaleringenSchedule | string | `"0 2 * * *"` | Schedule of the signaleringen send zaken job in CRON job format |
| signaleringen.successfulJobsHistoryLimit | int | `1` | k8s settings for the signaleren jobs |
| smartDocuments.authentication | string | `""` | Authentication token |
| smartDocuments.enabled | bool | `false` | Enable SmartDocuments integration for creating a new document |
| smartDocuments.fixedUserName | string | `""` | If this setting is set, then templates in SmartDocuments cannot use user-specific values. |
| smartDocuments.url | string | `""` | URL to SmartDocuments instance. For example: https://partners.smartdocuments.com |
| solr | object | `{"auth":{"enabled":false},"cloudBootstrap":false,"cloudEnabled":false,"collectionReplicas":1,"coreNames":["zac"],"customLivenessProbe":{"failureThreshold":6,"httpGet":{"path":"/solr/zac/admin/ping","port":"http"},"initialDelaySeconds":40,"periodSeconds":10,"successThreshold":1,"timeoutSeconds":15},"customReadinessProbe":{"failureThreshold":6,"httpGet":{"path":"/solr/zac/admin/ping","port":"http"},"initialDelaySeconds":60,"periodSeconds":10,"successThreshold":1,"timeoutSeconds":15},"enabled":false,"extraEnvVars":[{"name":"ZK_CREATE_CHROOT","value":"true"}],"persistence":{"size":"1Gi"},"replicaCount":1,"service":{"ports":{"http":80}},"zookeeper":{"enabled":false}}` | Solr enable to use the included solr helm chart to provision a solr to use with zac |
| solr-operator | object | `{"affinity":{},"annotations":{},"enabled":false,"fullnameOverride":"solr-operator","image":{"pullPolicy":"IfNotPresent","repository":"apache/solr-operator","tag":"v0.9.1@sha256:4db34508137f185d3cad03c7cf7c2b5d6533fb590822effcde9125cff5a90aa2"},"metrics":{"enabled":true},"nodeSelector":{},"solr":{"affinity":{},"annotations":{},"busyBoxImage":{"pullPolicy":"IfNotPresent","repository":"library/busybox","tag":"1.37.0-glibc@sha256:47ac99f1ae0afb8d83d8cd8aac5461be8103cac932f2631b5acce9122236adb1"},"enabled":true,"image":{"pullPolicy":"IfNotPresent","repository":"library/solr","tag":"9.8.1@sha256:05332e172bd3335eb36f2477e22cecae99f1f8e3b1bc4d1c148cb5373f1abaae"},"javaMem":"-Xms512m -Xmx768m","jobs":{"affinity":{},"annotations":{},"createZacCore":true,"image":{"pullPolicy":"IfNotPresent","repository":"curlimages/curl","tag":"8.13.0@sha256:d43bdb28bae0be0998f3be83199bfb2b81e0a30b034b6d7586ce7e05de34c3fd"},"nodeSelector":{},"tolerations":[]},"logLevel":"INFO","nodeSelector":{},"replicas":3,"storage":{"reclaimPolicy":"Delete","size":"1Gi","storageClassName":"managed-csi"},"tolerations":[]},"tolerations":[],"watchNamespaces":"default","zookeeper-operator":{"affinity":{},"annotations":{},"fullnameOverride":"zookeeper-operator","hooks":{"image":{"pullPolicy":"IfNotPresent","repository":"lachlanevenson/k8s-kubectl","tag":"v1.25.4@sha256:af5cea3f2e40138df90660c0c073d8b1506fb76c8602a9f48aceb5f4fb052ddc"}},"image":{"pullPolicy":"IfNotPresent","repository":"pravega/zookeeper-operator","tag":"0.2.15@sha256:b2bc4042fdd8fea6613b04f2f602ba4aff1201e79ba35cd0e2df9f3327111b0e"},"nodeSelector":{},"tolerations":[],"watchNamespace":"default","zookeeper":{"affinity":{},"annotations":{},"image":{"pullPolicy":"IfNotPresent","repository":"pravega/zookeeper","tag":"0.2.15@sha256:c498ebfb76a66f038075e2fa6148528d74d31ca1664f3257fdf82ee779eec9c8"},"nodeSelector":{},"replicas":3,"storage":{"reclaimPolicy":"Delete","size":"1Gi","storageClassName":"managed-csi"},"tolerations":[]}}}` | requires the installation of the solr-operator crds: kubectl create -f https://solr.apache.org/operator/downloads/crds/v0.8.1/all-with-dependencies.yaml |
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
| solr-operator.solr.busyBoxImage.tag | string | `"1.37.0-glibc@sha256:47ac99f1ae0afb8d83d8cd8aac5461be8103cac932f2631b5acce9122236adb1"` | solr busybox image tag |
| solr-operator.solr.enabled | bool | `true` | enable configuration of a solrcloud |
| solr-operator.solr.image.pullPolicy | string | `"IfNotPresent"` | solr imagePullPolicy |
| solr-operator.solr.image.repository | string | `"library/solr"` | solr image repository |
| solr-operator.solr.image.tag | string | `"9.8.1@sha256:05332e172bd3335eb36f2477e22cecae99f1f8e3b1bc4d1c148cb5373f1abaae"` | solr image tag |
| solr-operator.solr.javaMem | string | `"-Xms512m -Xmx768m"` | solr memory settings |
| solr-operator.solr.jobs.affinity | object | `{}` | affinity for jobs |
| solr-operator.solr.jobs.annotations | object | `{}` | annotations for jobs |
| solr-operator.solr.jobs.createZacCore | bool | `true` | enable createZacCore to have a curl statement generate the zac core in the provided solrcloud if it does not exist yet |
| solr-operator.solr.jobs.image.pullPolicy | string | `"IfNotPresent"` | solr jobs imagePullPolicy |
| solr-operator.solr.jobs.image.repository | string | `"curlimages/curl"` | solr jobs repository |
| solr-operator.solr.jobs.image.tag | string | `"8.13.0@sha256:d43bdb28bae0be0998f3be83199bfb2b81e0a30b034b6d7586ce7e05de34c3fd"` | solr jobs tag |
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
| solr.enabled | bool | `false` | set enabled to true to provision bitnami solr version with the zac core |
| tolerations | list | `[]` | set toleration parameters |
| zacInternalEndpointsApiKey | string | `""` | API key for authentication of internal ZAC endpoints |
| zgwApis | object | `{"clientId":"","secret":"","url":"","urlExtern":""}` | ZGW API configuration for integration with the ZGW APIs provider (OpenZaak) |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.14.2](https://github.com/norwoodj/helm-docs/releases/v1.14.2)

