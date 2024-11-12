# Developer documentation

The following ZAC developer documentation is available:

- [CONTRIBUTING](../../CONTRIBUTING.md) - Instructions on how to contribute to ZAC.
- [Font caching procedures](fontCachingProcedures.md) - Instructions on font caching in ZAC.
- [IDE Configuration](ideConfig.md) - Instructions on how to configure your IDE for ZAC development.
- [Installation](INSTALL.md) - Instructions on how to build, run and test the software.
- [Running ZAC using Docker Compose](installDockerCompose.md) - Instructions on how to run the software using Docker Compose.
- [End-to-end type safety](endToEndTypeSafety.md) - Instructions on how to develop ZAC using end-to-end type safety.
- [Testing](testing.md) - Instructions on how to run and develop tests for ZAC.
- [Updating dependencies](updatingDependencies.md) - Instructions on how to update various types of dependencies in ZAC
and (OpenAPI) API specifications used by ZAC.
- [Managing Solr](managingSolr.md) - Instructions on how to manage the Solr search engine in ZAC. 
For example, how to reindex the Solr search index.
- [BPMN](bpmn.md) - components used to support BPMN standard flows. 
- [REST Paging](paging.md) - REST paging conventions. 

## Monitoring
ZAC exposes two monitoring endpoints through WildFly by default:

- http://localhost:9990/health
- http://localhost:9990/metrics

Note: These are not secured. To disable, remove the metrics layer from the install-wildfly.sh script.
