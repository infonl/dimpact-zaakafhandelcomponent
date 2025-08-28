# ZAC system context

The following System Context diagram illustrates the architectural landscape of ZAC:

```mermaid
C4Context
    title ZAC System Context diagram

    Person(Citizen, "Citizen", "A citizen within a municipality")
    Person(Employee, "Employee", "An employee of a municipality")

    Enterprise_Boundary(b0, "ZAC context") {
        System_Boundary(components, "Other components") {
            System(OpenFormulieren, "Open Formulieren")
            System(OpenNotificaties, "Open Notificaties")
            System(Objecten, "Objecten")
            System(OpenZaak, "Open Zaak")
            System(OpenKlant, "Open Klant")
            System(OpenArchiefbeheer, "Open Archiefbeheer")
        }

        System_Boundary(ZAC, "ZAC components") {
            System(ZAC, "ZAC")
            System(OfficeConverter, "OfficeConverter")
            System(OpenPolicyAgent, "OPA")
            System(Solr, "Solr")
        }
    }

    Enterprise_Boundary(b1, "External services") {
        System(BAG, "BAG")
        System(BRP, "BRP")
        System(KVK, "KVK")
        System(SMTPServer, "SMTP Mail Server")
        System(SmartDocuments, "SmartDocuments")
    }

    Rel(Citizen, OpenFormulieren, "Submits case forms")
    Rel(Employee, ZAC, "Handles cases")

    Rel(OpenFormulieren, OpenNotificaties, "Uses", "ZGW Notificaties API")
    Rel(OpenKlant, OpenNotificaties, "Uses", "ZGW Notificaties API")
    Rel(Objecten, OpenNotificaties, "Uses", "ZGW Notificaties API")
    Rel(OpenZaak, OpenNotificaties, "Uses", "ZGW Notificaties API")

    Rel(OpenArchiefbeheer, OpenZaak, "Uses", "ZGW Documenten en Zaken API")

    Rel(OpenNotificaties, ZAC, "Uses", "ZGW Notificatie API for consumers")

    Rel(ZAC, OfficeConverter, "Uses", "OfficeConverter API")
    Rel(ZAC, OpenPolicyAgent, "Uses", "OPA API")
    Rel(ZAC, Solr, "Uses", "Solr API")

    Rel(ZAC, Objecten, "Uses", "ZGW Objecten API")
    Rel(ZAC, OpenZaak, "Uses", "ZGW Autorisaties, Besluiten, Catalogi, Documenten, en Zaken API")
    Rel(ZAC, OpenKlant, "Uses", "Klantinteracties API")
    Rel(ZAC, OpenKlant, "Uses", "Contactgegevens API")
    Rel(ZAC, BAG, "Uses", "HaalCentraal BAG Bevragen API")
    Rel(ZAC, BRP, "Uses", "HaalCentraal BRP Bevragen API")
    Rel(ZAC, KVK, "Uses", "KVK Zoeken en Vestigingsprofielen API")
    Rel(ZAC, SmartDocuments, "Uses", "SmartDocuments API")
    Rel(ZAC, SMTPServer, "Uses", "SMTP Mail Server")

    Rel(SmartDocuments, ZAC, "Uses", "ZAC SmartDocuments Callback API")

    UpdateElementStyle(ZAC, $bgColor="red", $borderColor="red")
    UpdateElementStyle(BAG, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(BRP, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(KVK, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(VNGReferentielijsten, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(SMTPServer, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(SmartDocuments, $bgColor="grey", $borderColor="grey")

    UpdateLayoutConfig($c4ShapeInRow="5", $c4BoundaryInRow="4")
```

## Components

The following components are part of the 'ZAC subsystem':

| Component                                                        | Description                                                                       | ZAC usage                                                          | API(s) used                                |
|------------------------------------------------------------------|-----------------------------------------------------------------------------------|--------------------------------------------------------------------|--------------------------------------------|
| [ZAC](https://github.com/infonl/dimpact-zaakafhandelcomponent)   | The Zaakafhandelcomponent. Consists of both the ZAC backend as well as frontend.  | -                                                                  | -                                          |
| [OfficeConverter](https://github.com/EugenMayer/officeconverter) | Document conversion service.                                                      | Convert office documents (like .docx) to PDF for preview purposes. | <ul><li>OfficeConverter REST API</li></ul> |
| [Open Policy Agent (OPA)](https://www.openpolicyagent.org//)     | Open Policy Agent server                                                          | See [ZAC IAM architecture](iamArchitecture.md).                    | <ul><li>OPA REST API </li></ul>            |
| [Solr](https://solr.apache.org/)                                 | Solr search engine                                                                | See [ZAC Solr architecture](solrArchitecture.md)                   | <ul><li>Solr REST API </li></ul>           |

The following components are part of the broader context of ZAC (='PodiumD Zaak' in the context of Dimpact).

| Component                                                                  | Description                                                                                                                                        | ZAC usage                                                                                                                  | API(s) used                                                                                                                                                                                                                                                                                                                               |
|----------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Objecten](https://github.com/maykinmedia/objects-api/)                    | Manages objects. Implements the ZGW Objecten API.                                                                                                  | For example 'productaanvragen' which are created from Open Formulieren and used by ZAC to create a new 'zaak'.             | <ul><li>[Objects API](../../src/main/resources/api-specs/or/objects-openapi.yaml)</li></ul>                                                                                                                                                                                                                                               |
| [Open Forms / Open Formulieren](https://github.com/maykinmedia/open-forms) | Manages and renders forms. In this context a citizen can submit a so-called 'zaakstartformulier' which is used to create a new 'zaak'.             | Citizens can start a new zaak by submitting a 'zaakstartformulier'.                                                        | <ul><li>n/a (see below)</li></ul>                                                                                                                                                                                                                                                                                                         |
| [Open Klant](https://github.com/maykinmedia/open-klant)                    | Manages 'customers' (= citizens in our context) and customer 'contact moments'. Implements both the ZWG Klantinteracties and Contactgegevens APIs. | Retrieve customer or company data and customer contact data (e.g. email address) of a citizen or company linked to a zaak. | <ul><li>[Klanten API](../../src/main/resources/api-specs/klanten/klanten-openapi.yaml)</li></ul>                                                                                                                                                                                                                                          |
| [Open Notificaties](https://github.com/open-zaak/open-notificaties)        | The central messaging / system notification component. Implements the ZWG Notificaties APIs.                                                       | ZAC needs to get notified of changes in related to zaken from various other components.                                    | <ul><li>[Notificaties API for consumers](https://vng-realisatie.github.io/gemma-zaken/standaard/notificaties-consumer/)</li></ul>                                                                                                                                                                                                         |
| [Open Zaak](https://github.com/open-zaak/open-zaak)                        | Manages zaken, zaaktypes, and all related items. Also stores documents.                                                                            | Used by ZAC to store and retrieve zaken, documents and related data.                                                       | <ul><li>[Besluiten API](../../src/main/resources/api-specs/zgw/brc-openapi.yaml)</li><li>[Documenten API](../../src/main/resources/api-specs/zgw/drc-openapi.yaml)</li><li>[Zaken API](../../src/main/resources/api-specs/zgw/zrc-openapi.yaml)</li><li>[Catalogi API](../../src/main/resources/api-specs/zgw/ztc-openapi.yaml)</li></ul> |
| [Open Archiefbeheer](https://github.com/maykinmedia/open-archiefbeheer)    | Manages archiving and record destruction.                                                                                                          | Will trigger record destruction in various registers, such as OpenZaak.                                                    |                                                                                                                                                                                                                                                                                                                                           |

The 'APIs used' column indicates which APIs offered by the various components is used by ZAC to integrate with each component including which version of the API is used.
Most APIs are defined using [OpenAPI](https://www.openapis.org/) definitions as part of the [Zaakgerichtwerken (ZGW) API specifications](https://vng-realisatie.github.io/gemma-zaken/standaard/).

Notes:
- ZAC does not integrate with Open Formulieren. For details please see: [ZAC integration with Open Formulieren](openFormulierenIntegration.md).
- Components belonging to the [IAM architecture](iamArchitecture.md) such as Keycloak are not listed here to keep things relatively simple.

## External services

Furthermore, ZAC integrates with the following external services:

| Service                                                                              | Description                                                                        | ZAC Usage                                                                                                                           | API(s) used                                                                                                                                                                                                                                                                                   |
|--------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Haal Centraal BAG](https://lvbag.github.io/BAG-Gemeentelijke-wensen-tav-BAG-Bevragingen)    | Centralized address and location data service by the Dutch government.             | Retrieve address and location data related to a zaak.                                                                               | <ul><li>[IMBAG API](../../src/main/resources/api-specs/bag/bag-openapi.yaml)</li></ul>                                                                                                                                                                                                        |
| [Haal Centraal BRP](https://github.com/BRP-API/Haal-Centraal-BRP-bevragen)           | Centralized personal data service by the Dutch government.                         | Retrieve personal data for citizens related to a zaak (the initiator of a zaak).                                                    | <ul><li>[BRP Personen Bevragen API](../../src/main/resources/api-specs/brp/brp-openapi.yaml)</li></ul>                                                                                                                                                                                        |
| [KVK](https://developers.kvk.nl/)                                                    | Centralized company data service.                                                  | Retrieve company data for companies related to a zaak.                                                                              | <ul><li>[Vestigingsprofiel API](../../src/main/resources/api-specs/kvk/vestigingsprofiel-openapi.yaml)</li><li>[Zoeken API](../../src/main/resources/api-specs/kvk/zoeken-openapi.yaml)</li></ul> |
| [SMTP Server](https://www.mailjet.com/)                                              | SMTP server. Services like MailJet can also be used.                               | Send emails to employees. Only used for sending e-mails, not for managing e-mail templates (this is done in ZAC itself).            | <ul><li>SMTP Protocol</li></ul>                                                                                                                                                                                                                                                               |
| [SmartDocuments](https://www.smartdocuments.eu/)                                     | Document creation service. Maybe used to create documents in your own look & feel. | Start a document creation 'wizard' with pre-filled zaak data where the resulting document is stored by SmartDocuments in Open Zaak. | <ul><li>SmartDocuments REST API (latest version)</li></ul>                                                                                                                                                                                                                                    |

For some of these external services ZAC uses mocks when running ZAC locally (please see: [development](../development/README.md)) and (optionally) also when running ZAC on a test environment.

### KVK integration

ZAC integrates with the Dutch Chamber of Commerce (KVK) to search for and retrieve information for organisations (businesses, legal entities and other organisations) 
that are related to a zaak.
This information is used to link a zaak to an organisation in the ZGW API zaakregister (e.g. Open Zaak) and to retrieve contact details for an organisation from the Klantinteracties API (e.g. Open Klant).

Notes:
- RSIN = Rechtspersonen Samenwerkingsverbanden Informatie Nummer
- The KVK APIs only support two main types of organisations: vestingen and rechtpersonen. Other types of organisations (like 'verenigingen', 'stichtingen', 'overheidsinstellingen', etc.) are all treated as rechtspersonen by the KVK APIs.

#### KVK organisation identifiers

ZAC supports the following KVK organisation identifiers both in the ZGW ZRC API to integrate with the zaakregister (e.g. Open Zaak):

| Organisation type                                      | Identifier type               | ZGW API read supported | ZGW API write supported | Remarks                                                                                                                                           |
|--------------------------------------------------------|-------------------------------|------------------------|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| Vestiging ('branch')                                   | KVK nummer + vestigingsnummer | yes                    | yes                     | Preferred option: only the combination of the two uniquely identifies a vestiging.                                                                |
| -                                                      | vestigingsnummer              | yes                    | no                      | ZAC supports this for legacy reasons only and shows a warning to the end-user. The vestigingsnummer alone does not uniquely identify a vestiging. |
| Rechtspersoon ('legal entity') and other organisations | KVK nummer                    | yes                    | yes                     | Preferred option: the KVK number uniquely identifies these types of organisations.                                                                |
| -                                                      | RSIN                          | yes                    | no                      | ZAC supports this for legacy reasons only and shows a warning to the end-user. ZAC prefers to use the KVK number instead.                         |

Notes:
- ZAC also provides functionality to search for organisations using the KVK Zoeken API. This is unrelated to how organisation data is written to and read from the ZGW API zaakregister.

#### KVK organisation identifiers used in the Klantinteracties API

ZAC supports the following KVK organisation identifiers in the Klantinteracties API to integrate with the customer register (e.g. Open Klant):

| Organisation type                                      | Identifier type               | Klantinteracties API read supported | Remarks                                                                                                       |
|--------------------------------------------------------|-------------------------------|-------------------------------------|---------------------------------------------------------------------------------------------------------------|
| Vestiging ('branch')                                   | KVK nummer + vestigingsnummer | yes                                 | Preferred option: only the combination of the two uniquely identifies a vestiging.                            |
| -                                                      | vestigingsnummer              | yes                                 | ZAC supports this for legacy reasons only. The vestigingsnummer alone does not uniquely identify a vestiging. |
| Rechtspersoon ('legal entity') and other organisations | KVK nummer                    | yes                                 | Preferred option: the KVK number uniquely identifies these types of organisations.                            |
| -                                                      | RSIN                          | yes                                 | ZAC supports this for legacy reasons only. ZAC prefers to use the KVK number instead.                         |

Notes:
- ZAC currently only reads data (like 'contact details') from the Klantinteracties API. It does not write any data to the Klantinteracties API.



