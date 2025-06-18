# Provisioning SOLR

***
> **Colofon** <br>
> Datum : 18-6-2025 <br>
> Versie :   1.0.0 <br>
> Verandering : provisioning solr <br>
> Toegangsrechten : Alleen lezen <br>
> Status : Definitief <br>
> Redacteur : John Bol <br>
> Auteur(s) : John Bol <br>
****

Versiegeschiedenis:

| 0.1   | Initiële versie                                                                                                                            |
|-------|--------------------------------------------------------------------------------------------------------------------------------------------|

<div style="page-break-after: always"></div>

# Inhoud

[*introduction*](#introduction)
[*requirements*](#requirements)
[*configuration*](#configuration)
[*provisioning*](#provisioning)

<div style="page-break-after: always"></div>

#  Introduction

originally solr was included in the helm chart of zaakafhandelcomponent. However, the current version of solr was not properly supported. Therefore the solr configuration part was deprecated in favor of provisioning solr seperately. This page describes how to configure solr for use by zaakafhandelcomponent.
Update: due to stability issues we have moved to solrcloud in k8s
Update: the zac helm charts can provision solrcloud for you but not the solr-operator crds. When used you will still have to create the zac solr core yourself.

#  Requirements

this requires a working helm installation in order to generate the required templates
the generated templates are too large to use kubectl apply

# Configuration

the zac core by default uses the standard solrconfig.xml provided by solr
this configuration file is managed by zookeeper and must be changed there if this is required. More information about that can be found here:
https://solr.apache.org/guide/6_6/using-zookeeper-to-manage-configuration-files.html

you can view the standard settings here:
https://solrcloud-common-zac-dev.dimpact.lifely.nl/solr/#/zac/files?file=solrconfig.xml

by default the UpdateHandler class and its attributes are set here with the following defaults:

•	autoCommit maxTime 15000 (15 seconds)
•	autoSoftCommit maxTime 3000 (3 seconds)

maxTime is defined as follows:
```
Maximum amount of time in ms that is allowed to pass
                   since a document was added before automatically
                   triggering a new commit
```

# Provisioning

generate solr-operator.yml
```
helm template solr-operator apache-solr/solr-operator --set metrics.enable=false > ~/solr-operator.yml
```
install solr operator
```
kubectl create -f https://solr.apache.org/operator/downloads/crds/v0.7.1/all-with-dependencies.yaml
kubectl apply -f solr-operator.yml
```
apply the (suggested) solrcloud template
```
apiVersion: solr.apache.org/v1beta1
kind: SolrCloud
metadata:
  name: zac-dev
spec:
  replicas: 3
  solrImage:
    tag: 9.3.0
  dataStorage:
    persistent:
      pvcTemplate:
         spec:
           resources:
             requests:
               storage: 1Gi
      reclaimPolicy: Delete
  zookeeperRef:
    provided:
      persistence:
        reclaimPolicy: Delete
        spec:
          accessModes:
          - ReadWriteOnce
          resources:
            requests:
              storage: 1Gi
```
create the collection zac
```
kubectl exec test-solrcloud-0 -ti -- /bin/bash
solr@test-solrcloud-0:/opt/solr-9.3.0$ solr create_collection -c zac -V -shards 2 -rf 2
WARNING: Using _default configset with data driven schema functionality. NOT RECOMMENDED for production use.
         To turn off: bin/solr config -c zac -p 8983 -action set-user-property -property update.autoCreateFields -value false
Connecting to ZooKeeper at test-solrcloud-zookeeper-0.test-solrcloud-zookeeper-headless.default.svc.cluster.local:2181,test-solrcloud-zookeeper-1.test-solrcloud-zookeeper-headless.default.svc.cluster.local:2181,test-solrcloud-zookeeper-2.test-solrcloud-zookeeper-headless.default.svc.cluster.local:2181/ ...
WARN  - 2023-10-13 15:55:40.905; org.apache.solr.common.cloud.SolrZkClient; Using default ZkCredentialsInjector. ZkCredentialsInjector is not secure, it creates an empty list of credentials which leads to 'OPEN_ACL_UNSAFE' ACLs to Zookeeper nodes
INFO  - 2023-10-13 15:55:40.986; org.apache.solr.common.cloud.ConnectionManager; Waiting up to 15000ms for client to connect to ZooKeeper
INFO  - 2023-10-13 15:55:41.027; org.apache.solr.common.cloud.ConnectionManager; zkClient has connected
INFO  - 2023-10-13 15:55:41.028; org.apache.solr.common.cloud.ConnectionManager; Client is connected to ZooKeeper
WARN  - 2023-10-13 15:55:41.028; org.apache.solr.common.cloud.SolrZkClient; Using default ZkACLProvider. DefaultZkACLProvider is not secure, it creates 'OPEN_ACL_UNSAFE' ACLs to Zookeeper nodes
INFO  - 2023-10-13 15:55:41.051; org.apache.solr.common.cloud.ZkStateReader; Updated live nodes from ZooKeeper... (0) -> (2)
INFO  - 2023-10-13 15:55:41.094; org.apache.solr.client.solrj.impl.ZkClientClusterStateProvider; Cluster at test-solrcloud-zookeeper-0.test-solrcloud-zookeeper-headless.default.svc.cluster.local:2181,test-solrcloud-zookeeper-1.test-solrcloud-zookeeper-headless.default.svc.cluster.local:2181,test-solrcloud-zookeeper-2.test-solrcloud-zookeeper-headless.default.svc.cluster.local:2181/ ready
Uploading /opt/solr-9.3.0/server/solr/configsets/_default/conf for config zac to ZooKeeper at test-solrcloud-zookeeper-0.test-solrcloud-zookeeper-headless.default.svc.cluster.local:2181,test-solrcloud-zookeeper-1.test-solrcloud-zookeeper-headless.default.svc.cluster.local:2181,test-solrcloud-zookeeper-2.test-solrcloud-zookeeper-headless.default.svc.cluster.local:2181/
Creating new collection 'zac' using CollectionAdminRequest
{
  "responseHeader":{
    "status":0,
    "QTime":13537},
  "success":{
    "test-solrcloud-0.test-solrcloud-headless.default:8983_solr":{
      "responseHeader":{
        "status":0,
        "QTime":11611},
      "core":"zac_shard2_replica_n1"},
    "test-solrcloud-1.test-solrcloud-headless.default:8983_solr":{
      "responseHeader":{
        "status":0,
        "QTime":11442},
      "core":"zac_shard2_replica_n2"},
    "test-solrcloud-1.test-solrcloud-headless.default:8983_solr":{
      "responseHeader":{
        "status":0,
        "QTime":11655},
      "core":"zac_shard1_replica_n6"},
    "test-solrcloud-0.test-solrcloud-headless.default:8983_solr":{
      "responseHeader":{
        "status":0,
        "QTime":12242},
      "core":"zac_shard1_replica_n4"}}}
```
test operation
```
tl exec zac-dev-solrcloud-0 -ti -- /bin/bash
solr@test-solrcloud-0:/opt/solr-9.3.0$ wget localhost:8983/solr/zac/admin/ping -O-
--2023-10-13 16:02:13--  http://localhost:8983/solr/zac/admin/ping
Resolving localhost (localhost)... ::1, 127.0.0.1
Connecting to localhost (localhost)|::1|:8983... connected.
HTTP request sent, awaiting response... 200 OK
Length: 253 [application/json]
Saving to: 'STDOUT'
-                                                0%[                                                                                                     ]       0  --.-KB/s               {
"responseHeader":{
"zkConnected":true,
"status":0,
"QTime":1,
"params":{
  "q":"{!lucene}*:*",
  "distrib":"false",
  "df":"_text_",
  "rows":"10",
  "echoParams":"all",
  "rid":"test-solrcloud-0.test-solrcloud-headless.default-1"
}
},
"status":"OK"
-                                              100%[====================================================================================================>]     253  --.-KB/s    in 0s
2023-10-13 16:02:13 (1.54 MB/s) - written to stdout [253/253]
```