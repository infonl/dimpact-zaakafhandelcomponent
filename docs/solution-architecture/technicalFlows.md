# ZAC Technical Flows

Some of the most important technical flows involving ZAC and other components and external service are documented
in this document.

## Zaak creation flow from Open Formulieren

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

## SmartDocuments document creation flow

```mermaid
sequenceDiagram
    actor Behandelaar
    box PodiumD
    participant ZAC
    participant OpenZaak
    participant OpenNotificaties
    end
    participant SmartDocuments


    Behandelaar->>+ZAC: Maak document aan voor een zaak
    ZAC->>+SmartDocuments: Maak document aan
    SmartDocuments->>SmartDocuments: Kies de gewenste sjabloon
    SmartDocuments->>+OpenZaak: Sla document op
    SmartDocuments->>+OpenZaak: Koppel document aan zaak
    OpenZaak->>-OpenNotificaties: Verstuur melding "document gekoppeld aan zaak"
    SmartDocuments->>-SmartDocuments: Sluit SmartDocuments
    OpenNotificaties--)ZAC: Ontvang melding "document gekoppeld aan zaak"
    ZAC->>-Behandelaar: Document is zichtbaar in de zaak
```

The ZAC - SmartDocuments flow is as follows:

1. User starts SmartDocument wizard from ZAC ('create document'). This can be done either from a case (zaak) or from a task.
2. ZAC sends a HTTPS request to start a document creation wizard to SmartDocuments and sends case- and other information in this request.
3. The document creation wizard is started in SmartDocuments. Case or task information is pre-filled.
4. When the user closes the document creation wizard, SmartDocuments does the following by sending two HTTPS requests:
   1. A Word document ('enkelvoudig informatie object') is created in OpenZaak.
   2. This document is linked to the case or task in question (by creating a ‘zaakinformatieobject’).
5. Open Zaak sends a notification to Open Notificaties that a new document has been linked to a case or task.
6. Open Notificaties sends a callback request to ZAC informing ZAC of this event.
7. Depending on which SmartDocuments environment is used, the user also has the option to download the document to their local computer.
However this part of the standard flow.
8. When the user returns to the ZAC browser tab, they will see the new document attached to the case or task.
