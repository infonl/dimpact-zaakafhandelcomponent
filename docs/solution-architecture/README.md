# ZAC solution architecture

These pages describe the solution architecture of ZAC.

The ZAC architecture is documented using the [C4 Model](https://c4model.com/).
Architecture diagrams are created and rendered using [Mermaid](https://mermaid.js.org/).

The architecture is documented in more detail on the following pages:
- [System Context](systemContext.md) - The system context of ZAC including the surrounding technical landscape.
- [IAM Architecture](iamArchitecture.md) - The identity and access management (IAM) architecture of ZAC.
- [Open Formulieren Integration](openFormulierenIntegration.md) - The integration of ZAC with Open Formulieren.
- [SmartDocuments Integration](smartDocumentsIntegration.md) - The integration of ZAC with SmartDocuments.
- [Solr Architecture](solrArchitecture.md) - The architecture of the Solr search engine in ZAC.

## Common Ground

PodiumD and therefore also ZAC is set up according to the principles of Common Ground.
A few important Common Ground principles that therefore apply to ZAC include:

| Principle               | Implication for ZAC                                                                                                                                                                                                  |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Component based         | <ul><li>ZAC is a component for zaakgericht werken with a clear purpose and bounded context.</li><li>Functionality which is not directly related to its purpose should (eventually) be moved outside of ZAC.</li></ul> |
| Open and open source    | <ul><li>The ZAC source code is open source. We use the EUPL 1.2 open source license.</li><li>Also our way of working, documentation, user stories and issues are all open to the public.</li></ul>                   |
| 'Eenmalige vastlegging' | <ul><li>Data is kept at the source as much as possible.</li><li>The main deviance from this principle is that ZAC stores certain external data mainly Open Zaak (in the ZAC cache and the ZAC Solr search engine) for performance optimisation reasons. ZAC however always treats Open Zaak as the single source of truth for this data.</li></ul>|
| Standards               | <ul></li>ZAC runs on Kubernetes and provides a Kubernetes Helm Chart for deployment to a Kubernetes cluster.</li><br/><li>ZAC is currently not compliant with the Common Ground NLX and Haven standards.</li><li>ZAC does not offer an API to be used for other components. The ZAC backend API is solely meant to be used by the ZAC frontend and can therefore be seen as a ‘backend for the frontend’. Security guidelines obviously still apply.</li></ul>|







