# ZAC solution architecture

These pages describe the solution architecture of ZAC.

The ZAC architecture is documented using the [C4 Model](https://c4model.com/).
Architecture diagrams are created and rendered using [Mermaid](https://mermaid.js.org/).

## ZAC system context

The following System Context diagram illustrates the architectural landscape of ZAC:

```mermaid
C4Context
    title ZAC System Context diagram

    Person(Citizen, "Citizen", "A citizen within a municipality")
    Person(Employee, "Employee", "An employee of a municipality")

    Enterprise_Boundary(b0, "ZAC and related Common Ground components") {
        System(OpenFormulieren, "Open Formulieren")
        System(ZAC, "ZAC", "Zaakafhandelcomponent")

        System_Boundary(CentralizedServices, "Centralized Services") {
            System(OpenNotificaties, "Open Notificaties")
        }

        System_Boundary(registers, "Registers") {
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
    Rel(SmartDocuments, OpenZaak, "Uses", "ZGW Documenten en Zaken API")

    UpdateElementStyle(ZAC, $bgColor="red", $borderColor="red")
    UpdateElementStyle(BAG, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(BRP, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(KVK, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(VNGReferentielijsten, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(Mailjet, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(SmartDocuments, $bgColor="grey", $borderColor="grey")

    UpdateLayoutConfig($c4ShapeInRow="5", $c4BoundaryInRow="2")
```

The following components are part of the ZAC system context:

| Component         | Description                                                                                                                               | ZAC usage                                                                                                      |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| Objecten          | Manages objects. Implements the ZGW Objecten API.                                                                                         | For example 'productaanvragen' which are created from Open Formulieren and used by ZAC to create a new 'zaak'. |
| Objecttypen       | Object types. Required for Objecten. Implements the ZGW Objecttypen API.                                                                  | ZAC uses a specific 'productaanvraag' type for Open Formulieren data used to create a new 'zaak'               |
| Open Formulieren  | Manages and renders forms. In this context a citizen can submit a so-called 'zaakstartformulier' which is used to create a new 'zaak'.    | Citizens can start a new zaak by submitting a 'zaakstartformulier'.                                            |
| Open Klant        | Manages 'customers' (= citizens in our context) and customer 'contact moments'. Implements both the ZWG Klanten and Contactmomenten APIs. | Retrieve customer and customer contact data (e.g. email address) of a citizen linked to a zaak.                |
| Open Notificaties | The central messaging / system notification component. Implements the ZWG Notificaties APIs.                                              | ZAC needs to get notified of changes in related to zaken from various other components.                        |
| Open Zaak         | Manages zaken, zaaktypes, and all related items. Also stores documents.                                                                   | Used by ZAC to store and retrieve zaken, documents and related data.                                           |

Furthermore, ZAC integrates with the following external services:

| Service               | Description                                                            | ZAC Usage                                                                                                                           |
|-----------------------|------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| BAG                   | Centralized address and location data service by the Dutch government. | Retrieve address and location data related to a zaak.                                                                               |
| BRP                   | Centralized personal data service by the Dutch government.             | Retrieve personal data for citizens related to a zaak (the initiator of a zaak).                                                    |
| KVK                   | Centralized company data service.                                      | Retrieve company data for companies related to a zaak.                                                                              |
| Mailjet               | Email service.                                                         | Send emails to employees.                                                                                                           |
| SmartDocuments        | Document creation service.                                             | Start a document creation 'wizard' with pre-filled zaak data where the resulting document is stored by SmartDocuments in Open Zaak. |
| VNG Referentielijsten | Centralized reference data service.                                    | Retrieve reference data such as 'communication channels'.                                                                           |

## Zaak creation flow

The most standard flow for creating a new 'zaak' is done by a citizen filling in a specific 'product aanvraag' form in Open Formulieren.

Upon submitting the form, the following happens:

1. The content of the form is saved in a structured format (JSON) as _Product Request_ in Miscellaneous Registrations.
2. The BSN obtained via DigiD from the citizen is stored in the Product Request.
3. The completed form is saved as a PDF document in Open Case.

Creating the Product Request in Miscellaneous Registrations ensures that a notification is sent to Open Notifications. "Zaak afhandel component" has a subscription to notifications, which means that Open Notifications forwards the notification to "Zaak afhandel component".
After receiving the notification of creating a new Product Request in Miscellaneous Registrations, the following happens in "Zaak afhandel component":

1. The Product Request is retrieved from Miscellaneous Registrations.
2. Based on the type of Product Request, the Case Type is determined, and a Case is created.
3. The Product Request is linked to the Case.
4. The existing PDF document of the completed form is linked to the Case.
5. The BSN or Chamber of Commerce number from the Product Request is used to link a Role of the type Applicant to the Case. The BSN or establishment number is stored with the Role.
6. A CMMN Case is started for the case. The started CMMN Case is derived from the case type and can be configured in "Zaak afhandel component" using case handling parameters.

This flow is visualised in the following sequence diagram:

![ZAC Open Formulieren sequence diagram](attachments/images/zac-open-formulieren-sequence-diagram.png)

## Identity and Access Management (IAM)

The IAM architecture of ZAC is shown in the following diagram:

![Identity and Policy](attachments/images/zac-iam-architecture.jpg)







