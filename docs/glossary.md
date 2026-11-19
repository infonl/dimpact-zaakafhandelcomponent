<!--
  ~ SPDX-FileCopyrightText: 2026 INFO.nl
  ~ SPDX-License-Identifier: EUPL-1.2+
  -->

# Glossary

This glossary is here to promote [Ubiquitous Language](https://martinfowler.com/bliki/UbiquitousLanguage.html), so that the Team can communicate with the Business more effectively. It contains a list of terms that are used in the business domain and a list of terms used by the team with their agreed-upon definition.

This file is the source of the glossary. Make changes here, not in Confluence.
It started as a copy of the
[Glossary page in the Dimpact Confluence](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/4522123/Glossary).

Also see the overall PodiumD glossary: PodiumD Glossary in the Dimpact Confluence (there is some overlap).

## Domain Specific Terms

| Abbreviation/term | Meaning | Description |
|---|---|---|
| [ABC](https://open-archiefbeheer.readthedocs.io/en/latest/manual/index.html) | Archief Beheer Component | PodiumD Zaak component for archiving zaak data.<br/><br/>Archiving in this context includes destruction of data but also includes other types of archiving. |
| [AD FS](https://learn.microsoft.com/en-us/windows-server/identity/ad-fs/ad-fs-overview) | Active Directory Federation Services | Federated Identity and Access Management solution for Microsoft Active Directory. Used by most municipalities and serves as the integration point between the IAM solution for municipalities for PodiumD. |
| [BAG](https://www.kadaster.nl/zakelijk/registraties/basisregistraties/bag) | Basisregistratie Adressen en Gebouwen | ZAC integrates with the BAG API. |
| Behandelaar | Case Manager | This is a person (council employee) that has been tasked with handling cases. A case manager can only deal with cases of the zaaktypen that they are authorised for through their functional roles. The rights that a behandelaar should have is available [in the ZAC repository](https://github.com/infonl/dimpact-zaakafhandelcomponent/blob/main/docs/solution-architecture/accessControlPolicies.md). |
| BPM | Business Process Management | |
| [BPMN](https://en.wikipedia.org/wiki/BPMN) | Business Process Model and Notation | An open standard for business process modeling that provides a graphical notation for specifying business processes in a Business Process Diagram (BPD).<br/><br/>ZAC biedt een zeker mate van ondersteuning voor BPMN processen maar focust veel meer op een standaard CMMN model. |
| [BRP](https://www.rijksoverheid.nl/onderwerpen/privacy-en-persoonsgegevens/basisregistratie-personen-brp) | Basisregistratie Personen | “De Basisregistratie Personen (BRP) bevat persoonsgegevens van inwoners van Nederland (ingezetenen) en van personen die Nederland hebben verlaten (niet-ingezetenen).”<br/><br/>ZAC integrates with the BRP API. |
| [CG](https://commonground.nl/) | Common Ground | Data and IT architecture and best practises set up by the VNG with data-at-rest being one of the founding principles. PodiumD and ZAC is based on Common Ground. |
| [CMMN](https://en.wikipedia.org/wiki/CMMN) | Case Management Model and Notation | An open standard consisting of a representation for graphically expressing a Case, as well as an interchange format for exchanging Case models among different tools.<br/><br/>Het generieke zaakmodel in ZAC is een CMMN model (en niet een BPMN proces), maar ZAC biedt ook een zeker mate van ondersteuning voor BPMN processen. |
| Contact | PodiumD Contact is the call-center / customer service component within PodiumD. It is implemented by the KISS application created by ICATT. | KISS - Klant Interactie Service Systeem - is an application that can be used by councils as their customer service application. It has functionality for the call-center employees to help callers with questions. It interacts with other PodiumD components to retrieve information (about Cases for instance) and stores information about certain Internal Tasks. |
| Domein | Domain | A grouping of entity types, such as ‘zaaktypen’, in the PABC. Domains are a PABC-internal concept only and do not necessarily represent departments within the council organization. ZAC and Keycloak have no knowledge of these domains. |
| eSuite | ATOS eSuite | De ATOS eSuite is een voor Dimpact ontwikkelt monolitisch software pakket wat grofweg dezelfde functionaliteit biedt die ook zal worden geboden door PodiumD.<br/><br/>Veel Dimpact gemeenten gebruiken op dit moment de eSuite en zullen de komende jaren (geleidelijk) gaan migreren naar PodiumD. |
| HIB | Hosting, Integratie en Beheer | Project within Beyond 23 program that is responsible for setting up a shared hosting environment, operations and service desk. |
| IAM | Identity and Access Management | For the PodiumD IAM architecture see: [Zero Trust IAM Architectuur voor PodiumD](https://drive.google.com/drive/folders/1_o4YdwDZT2trFv9_RnHqlL6Vah49VBlk) |
| IGA | Identity Governance and Administration | Identity and access management at the organization (municipality) level. Part of IAM that is outside of PodiumD but instead is part of the existing municipality IT landscape. |
| ImZTC | Informatiemodel Zaaktypen | Gericht op het kunnen specificeren van zaaktypen. Zaaktypen worden gebruikt om de behandeling van zaken aan te sturen. |
| KCC | Klant Contact Centrum | |
| KING | Kwaliteitsinstituut Nederlandse Gemeenten | Nu VNG Realisatie |
| KISS | Klant Interactie Service Systeem | Zie: [https://www.icatt.nl/cases/kiss](https://www.icatt.nl/cases/kiss) |
| [KVK](https://www.kvk.nl/) | Kamer van Koophandel | ZAC integrates with the KVK API. |
| MDTO | Metadata Duurzame Toegankelijkheid Overheid | Used for archiving etc purposes, partly in other components like the ABC. |
| MOR | Melding Openbare Ruimte | An example of a zaaktype in ZGW. |
| Notificaties | Notifications. In the context of ZAC a purely technical concept where ZAC is notified of certain technical events from the Open Notificaties component. | ZAC is configured in Open Notificaties to be notified of certain events (e.g. a new zaak has been created in Open Zaak). These notifications are technically HTTP REST requests from Open Notificaties to the ZAC backend API ('notificaties' endpoint). Some of these notifications can result in signaleringen to the end-user. |
| OIP | Open Inwoner Platform | Zie: [https://www.maykinmedia.nl/blog/2023/feb/21/maykin-het-open-inwoner-platform/](https://www.maykinmedia.nl/blog/2023/feb/21/maykin-het-open-inwoner-platform/) |
| OK | Open Klant | Zie [https://github.com/maykinmedia/open-klant](https://github.com/maykinmedia/open-klant) |
| [PDC](https://iplo.nl/digitaal-stelsel/vergunningaanvragen-ontvangen-samenwerken/zaaktypecatalogus-ztc-producten-dienstencatalogus/) | Producten- en Diensten Catalogus | |
| POD | PodiumD | PodiumD is de Dimpact PaaS oplossing waaring de verschillede componenten worden gekoppeld en per gemeente wordt opgezet. |
| [RBAC](https://en.wikipedia.org/wiki/Role-based_access_control) | Role-based Access Control | “Role-based access control is a policy-neutral access-control mechanism defined around roles and privileges.”<br/><br/>Onderdeel van de [PodiumD IAM architectuur](https://drive.google.com/drive/folders/1_o4YdwDZT2trFv9_RnHqlL6Vah49VBlk). |
| RGBZ | Referentiemodel Gemeentelijke Basisgegevens Zaken, ofwel Informatiemodel Zaken. | Gericht op de informatievoorziening voor het behandelen en (achteraf) verantwoorden van zaken. |
| RSGB | Referentiemodel Stelsel van Gemeentelijke Basisgegevens, ofwel Informatiemodel Basis- en Kerngegevens. | Dimpact geeft aan dat dit ouderwets is. |
| [RSIN](https://www.kvk.nl/over-het-handelsregister/rsin-nummer/) | Rechtspersonen en Samenwerkingsverbanden Informatienummer | An RSIN (translated as “Legal Entities and Partnerships Information Number”) refers to an organization or legal entity, rather than an individual person. It’s used for data exchange between (governmental) organizations, such as the Netherlands Chamber of Commerce (KVK) and the Tax Authority (Belastingdienst). Note that sole proprietorships (eenmanszaken) do not have an RSIN. |
| [SAM](https://www.dimpact.nl/news/dimpact-introduceert-nieuwe-samenwerkingsomgeving-voor-haar-leden) | | Samenwerkingsomgeving Dimpact in Sharepoint |
| SDG | Single Digital Gateway | |
| Sdu | | [https://www.sdu.nl/over-sdu](https://www.sdu.nl/over-sdu) |
| [SSC](https://www.ssc-ict.nl/) / SSC-IT | Shared Service Center | Hosting partner for the Dimpact PodiumD environments. Specifically SSC Enschede. |
| Signaleringen | Functionality in ZAC where the user is ‘signalled’ of certain events (e.g. tasks or zaken assigned to them), as a card on the dashboard, as an e-mail, or both. | See the [ZAC user manual](https://github.com/infonl/dimpact-zaakafhandelcomponent/tree/main/docs/manuals) for details. |
| StUF | Standaard Uitwisseling Formaat | Oude standaard voor gegevensuitwisseling gemeentes / overheid. Deels een voorloper van de ZGW. Nog steeds in gebruik bij veel gemeentes. |
| StUF-ZKN | Stuf Zaken | De voorloper van ZGW en de ZGW APIs. Nog steeds in gebruik bij veel gemeentes. |
| TSA | Taak Specifieke Applicatie | Een applicatie ontwikkeld voor een specifiek type zaak. Bijvoorbeeld iBurgerzaken. Deze applicaties hebben soms een geïntegreerd zaaksysteem of hebben een koppeling met een zaaksysteem.<br/>Bijv een parkeervergunning applicatie bij een specifieke gemeente. |
| VNG | Vereniging Nederlandse Gemeentes | VNG maintains the Common Ground standard amongst many other things shared between Dutch municipalities. |
| VRL | [VNG Referentielijsten API](https://vng-realisatie.github.io/gemma-zaken/ontwikkelaars/) | Is used in the Atos ZAC source code. |
| VT&H | [https://vng.nl/rubrieken/onderwerpen/vergunningverlening-toezicht-en-handhaving](https://vng.nl/rubrieken/onderwerpen/vergunningverlening-toezicht-en-handhaving) | |
| WOO | [https://www.rijksoverheid.nl/onderwerpen/wet-open-overheid-woo](https://www.rijksoverheid.nl/onderwerpen/wet-open-overheid-woo) | The Wet open overheid (WOO) requires Dutch councils to actively publish certain types of information, such as laws, decisions, and research reports. Councils must respond to information requests within 28 days, with a possible 14-day extension. The law aims to enhance transparency and accessibility of public information. |
| [Zaak](https://www.gemmaonline.nl/index.php/Thema-architectuur_Zaakgericht_werken) | ‘Case' as used in 'zaakgericht werken’, a Dutch approach to case management for municipalities and other Dutch public institutions. | Een samenhangende hoeveelheid werk met een duidelijke aanleiding en een duidelijk eindresultaat, waarvan kwaliteit en doorlooptijd bewaakt moeten worden.<br/>Not to be confused with the term `Case` in CMMN. A CMMN zaakprocess is modeled using a CMMN case, but a zaak is a more generic term, if only because a BPMN zaakprocess is modeled by a BPMN process instance. |
| ZAP / ZAPs | Zaak Afhandel Parameter / Zaak Afhandel Parameters | Zaaktype configuration managed by beheerders in the ZAC UI used to configure parts of the zaak handling process (CMMN or BPMN) process in ZAC. |
| ZGW | Zaakgericht werken | Onderdeel hiervan zijn de [ZGW API’s](https://vng.nl/projecten/zaakgericht-werken-api) die gebruikt worden binnen PodiumD Zaak. |
| ZRC | Zaakregistratiecomponent | Open Zaak is een voorbeeld van een zaakregistratiecomponent (maar Open Zaak is nog meer dan dat). |
| ZTC | Zaaktypecatalogus | Open Zaak is onder meer ook een zaaktypecatalogus. |
