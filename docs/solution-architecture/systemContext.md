# ZAC system context

The following System Context diagram illustrates the architectural landscape of ZAC:

```mermaid
C4Context
    title ZAC System Context diagram

    Person(Employee, "Employee", "An employee of a municipality")
    Person(Citizen, "Citizen", "A citizen within a municipality")

    Enterprise_Boundary(b0, "ZAC context") {
        System_Boundary(ZAC, "ZAC components") {
            System(ZAC, "Zaakafhandelcomponent")
            System(OfficeConverter, "OfficeConverter")
            System(OpenPolicyAgent, "OPA")
        }

        System_Boundary(components, "Other components") {
            System(OpenFormulieren, "Open Formulieren")
            System(OpenNotificaties, "Open Notificaties")
            System(Objecten, "Objecten")
            System(Objecttypen, "Objecttypen")
            System(OpenZaak, "Open Zaak")
            System(OpenKlant, "Open Klant")
        }
    }

    Enterprise_Boundary(b1, "External services") {
        System(BAG, "BAG")
        System(BRP, "BRP")
        System(KVK, "KVK")
        System(VNGReferentielijsten, "VNG Referentielijsten")
        System(Mailjet, "Mailjet")
        System(SmartDocuments, "SmartDocuments")
    }

    Rel(Citizen, OpenFormulieren, "Submits case forms")
    Rel(Employee, ZAC, "Handles cases")

    Rel(OpenFormulieren, OpenNotificaties, "Uses", "ZGW Notificaties API")
    Rel(OpenKlant, OpenNotificaties, "Uses", "ZGW Notificaties API")
    Rel(Objecten, OpenNotificaties, "Uses", "ZGW Notificaties API")
    Rel(OpenZaak, OpenNotificaties, "Uses", "ZGW Notificaties API")
    Rel(OpenNotificaties, ZAC, "Uses", "HTTPS")

    Rel(ZAC, Objecten, "Uses", "ZGW Objecten API")
    Rel(ZAC, Objecttypen, "Uses", "ZGW Objecttypen API")
    Rel(ZAC, OpenZaak, "Uses", "ZGW Autorisaties, Besluiten, Catalogi, Documenten, en Zaken API")
    Rel(ZAC, OpenKlant, "Uses", "Klanten API")
    Rel(ZAC, OpenKlant, "Uses", "Contactmomenten API")
    Rel(ZAC, BAG, "Uses", "HaalCentraal BAG Bevragen API")
    Rel(ZAC, BRP, "Uses", "HaalCentraal BRP Bevragen API")
    Rel(ZAC, KVK, "Uses", "KVK Zoeken en Vestigingsprofielen API")
    Rel(ZAC, VNGReferentielijsten, "Uses", "VNG Referentielijsten API")
    Rel(ZAC, SmartDocuments, "Uses", "SmartDocuments API")
    Rel(ZAC, Mailjet, "Uses", "Mailjet API")
    Rel(ZAC, OfficeConverter, "Uses", "OfficeConverter API")
    Rel(ZAC, OpenPolicyAgent, "Uses", "OPA API")
    Rel(SmartDocuments, OpenZaak, "Uses", "ZGW Documenten en Zaken API")

    UpdateElementStyle(ZAC, $bgColor="red", $borderColor="red")
    UpdateElementStyle(BAG, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(BRP, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(KVK, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(VNGReferentielijsten, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(Mailjet, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(SmartDocuments, $bgColor="grey", $borderColor="grey")

    UpdateLayoutConfig($c4ShapeInRow="5", $c4BoundaryInRow="4")
```

## Components

The following components are part of the 'ZAC subsystem':

| Component                                                         | Description                                                                      | ZAC usage                                                          | API(s) used                                      |
|-------------------------------------------------------------------|----------------------------------------------------------------------------------|--------------------------------------------------------------------|--------------------------------------------------|
| [ZAC](https://github.com/maykinmedia/objects-api/)                | The Zaakafhandelcomponent. Consists of both the ZAC backend as well as frontend. | -                                                                  | -                                                |
| [OfficeConverter](https://github.com/EugenMayer/officeconverter)  | Document conversion service.                                                     | Convert office documents (like .docx) to PDF for preview purposes. | <ul><li>OfficeConverter REST API 1.5.0</li></ul> |
| [Open Policy Agent (OPA)](https://www.openpolicyagent.org//)      | Open Policy Agent server                                                         | See [ZAC IAM architecture](iamArchitecture.md).                    | <ul><li>OPA REST API </li></ul>                  |

The following components are part of the broader context of ZAC (='PodiumD Zaak' in the context of Dimpact).

| Component                                                                  | Description                                                                                                                               | ZAC usage                                                                                                      | API(s) used                                                                                                            |
|----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| [Objecten](https://github.com/maykinmedia/objects-api/)                    | Manages objects. Implements the ZGW Objecten API.                                                                                         | For example 'productaanvragen' which are created from Open Formulieren and used by ZAC to create a new 'zaak'. | <ul><li>Objects API 2.1.1</li></ul>                                                                                    |
| [Objecttypen](https://github.com/maykinmedia/objecttypes-api)              | Object types. Required for Objecten. Implements the ZGW Objecttypen API.                                                                  | ZAC uses a specific 'productaanvraag' type for Open Formulieren data used to create a new 'zaak'               | <ul><li>Objecttypen API 2.1.0</li></ul>                                                                                |
| [Open Forms / Open Formulieren](https://github.com/maykinmedia/open-forms) | Manages and renders forms. In this context a citizen can submit a so-called 'zaakstartformulier' which is used to create a new 'zaak'.    | Citizens can start a new zaak by submitting a 'zaakstartformulier'.                                            | <ul><li>n/a (see below)</li></ul>                                                                                      |
| [Open Klant](https://github.com/maykinmedia/open-klant)                    | Manages 'customers' (= citizens in our context) and customer 'contact moments'. Implements both the ZWG Klanten and Contactmomenten APIs. | Retrieve customer and customer contact data (e.g. email address) of a citizen linked to a zaak.                | <ul><li>Klanten API 1.0.0</li><li>Contactmomenten API 1.0.0</li></ul>                                                  |
| [Open Notificaties](https://github.com/open-zaak/open-notificaties)        | The central messaging / system notification component. Implements the ZWG Notificaties APIs.                                              | ZAC needs to get notified of changes in related to zaken from various other components.                        | <ul><li>n/a (see below)</li></ul>                                                                                      |
| [Open Zaak](https://github.com/open-zaak/open-zaak)                        | Manages zaken, zaaktypes, and all related items. Also stores documents.                                                                   | Used by ZAC to store and retrieve zaken, documents and related data.                                           | <ul><li>Besluiten API 1.1.0</li><li>Documenten API 1.3.0</li><li>Zaken API 1.24.0</li><li>Catalogi API 1.2.1</li></ul> |

The 'APIs used' column indicates which APIs offered by the various components is used by ZAC to integrate with each component including which version of the API is used.
Most APIs are defined using [OpenAPI](https://www.openapis.org/) definitions as part of the [Zaakgerichtwerken (ZGW) API specifications](https://vng-realisatie.github.io/gemma-zaken/standaard/).

Notes:
- ZAC does not integrate with Open Formulieren. For details please see: [ZAC integration with Open Formulieren](openFormulierenIntegration.md).
- ZAC does not integrate with Open Notificaties using an API. Rather Open Notificaties performs callback HTTP requests to ZAC when events have been received to which has subscribed.
- Components belonging to the [IAM architecture](iamArchitecture.md) such as Keycloak and OpenLDAP are not listed here to keep things relatively simple.

## External services

Furthermore, ZAC integrates with the following external services:

| Service                                                                              | Description                                                                        | ZAC Usage                                                                                                                           | API(s) used                                                                                     |
|--------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| [Haal Centraal BAG](https://vng-realisatie.github.io/Haal-Centraal-BAG-bevragen/)    | Centralized address and location data service by the Dutch government.             | Retrieve address and location data related to a zaak.                                                                               | <ul><li>IMBAG API 2.6.0</li></ul>                                                               |
| [Haal Centraal BRP](https://github.com/BRP-API/Haal-Centraal-BRP-bevragen)           | Centralized personal data service by the Dutch government.                         | Retrieve personal data for citizens related to a zaak (the initiator of a zaak).                                                    | <ul><li>BRP Personen Bevragen API 2.0.0</li></ul>                                               |
| [KVK](https://developers.kvk.nl/)                                                    | Centralized company data service.                                                  | Retrieve company data for companies related to a zaak.                                                                              | <ul><li>Basisprofiel API 1.3</li><li>Vestigingsprofiel API 1.4</li><li>Zoeken API 1.3</li></ul> |
| [Mailjet](https://www.mailjet.com/)                                                  | Email service.                                                                     | Send emails to employees. Only used for sending e-mails, not for managing e-mail templates (this is done in ZAC itself).            | <ul><li>Mailjet Email API (latest version)</li></ul>                                            |
| [SmartDocuments](https://www.smartdocuments.eu/)                                     | Document creation service. Maybe used to create documents in your own look & feel. | Start a document creation 'wizard' with pre-filled zaak data where the resulting document is stored by SmartDocuments in Open Zaak. | <ul><li>SmartDocuments REST API (latest version)</li></ul>                                      |
| [VNG Referentielijsten](https://vng-realisatie.github.io/gemma-zaken/ontwikkelaars/) | Centralized reference data service.                                                | Retrieve reference data such as 'communication channels'.                                                                           | <ul><li>Referentielijsten & Selectielijst API 0.6.0 (see below)</li></ul>                       |



Notes:
- For the VNG Referentielijsten API we follow the API version convention as used by https://vng-realisatie.github.io/gemma-zaken/standaard/ (and not the one used by https://github.com/VNG-Realisatie/referentielijsten-api).

For some of these external services ZAC uses mocks when running ZAC locally (please see: [development](../development/)) and (optionally) also when running ZAC on a test environment.
