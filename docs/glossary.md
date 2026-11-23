<!--
  ~ SPDX-FileCopyrightText: 2026 INFO.nl
  ~ SPDX-License-Identifier: EUPL-1.2+
  -->

# Glossary

This glossary is here to promote [Ubiquitous Language](https://martinfowler.com/bliki/UbiquitousLanguage.html), so that the Team can communicate with the Business more effectively. It contains a list of terms that are used in the business domain and a list of terms used by the team with their agreed-upon definition.

This is the glossary of ZAC.

Also see the overall [PodiumD Glossary](https://dimpact.atlassian.net/wiki/spaces/POO/pages/284131416/PodiumD+Glossary)
in the Dimpact Confluence (there is some overlap).

## Domain Specific Terms

| Abbreviation/term | Meaning | Description |
|---|---|---|
| [ABC](https://open-archiefbeheer.readthedocs.io/en/latest/manual/index.html) | Archief Beheer Component | PodiumD Zaak component for archiving zaak data.<br/><br/>Archiving in this context includes destruction of data but also includes other types of archiving. |
| [AD FS](https://learn.microsoft.com/en-us/windows-server/identity/ad-fs/ad-fs-overview) | Active Directory Federation Services | Federated Identity and Access Management solution for Microsoft Active Directory. Used by most municipalities and serves as the integration point between the IAM solution for municipalities for PodiumD. |
| [BAG](https://www.kadaster.nl/zakelijk/registraties/basisregistraties/bag) | Basisregistratie Adressen en Gebouwen | ZAC integrates with the BAG API. |
| Behandelaar | Case Manager | This is a person (council employee) that has been tasked with handling cases. A case manager can only deal with cases of the zaaktypen that they are authorised for through their functional roles. The rights that a behandelaar should have is available [in the ZAC repository](https://github.com/infonl/dimpact-zaakafhandelcomponent/blob/main/docs/solution-architecture/accessControlPolicies.md). |
| BPM | Business Process Management | |
| [BPMN](https://en.wikipedia.org/wiki/BPMN) | Business Process Model and Notation | An open standard for business process modeling that provides a graphical notation for specifying business processes in a Business Process Diagram (BPD).<br/><br/>ZAC offers some support for BPMN processes, but focuses much more on a standard CMMN model. |
| [BRP](https://www.rijksoverheid.nl/onderwerpen/privacy-en-persoonsgegevens/basisregistratie-personen-brp) | Basisregistratie Personen | The Basisregistratie Personen (BRP) contains personal data of residents of the Netherlands (ingezetenen) and of persons who have left the Netherlands (niet-ingezetenen).<br/><br/>ZAC integrates with the BRP API. |
| [CG](https://commonground.nl/) | Common Ground | Data and IT architecture and best practises set up by the VNG with data-at-rest being one of the founding principles. PodiumD and ZAC is based on Common Ground. |
| [CMMN](https://en.wikipedia.org/wiki/CMMN) | Case Management Model and Notation | An open standard consisting of a representation for graphically expressing a Case, as well as an interchange format for exchanging Case models among different tools.<br/><br/>The generic zaak model in ZAC is a CMMN model (and not a BPMN process), but ZAC also offers some support for BPMN processes. |
| Contact | PodiumD Contact is the call-center / customer service component within PodiumD. It is implemented by the KISS application created by ICATT. | KISS - Klant Interactie Service Systeem - is an application that can be used by councils as their customer service application. It has functionality for the call-center employees to help callers with questions. It interacts with other PodiumD components to retrieve information (about Cases for instance) and stores information about certain Internal Tasks. |
| Domein | Domain | A grouping of entity types, such as ‘zaaktypen’, in the PABC. Domains are a PABC-internal concept only and do not necessarily represent departments within the council organization. ZAC and Keycloak have no knowledge of these domains. |
| eSuite | ATOS eSuite | The ATOS eSuite is a monolithic software package developed for Dimpact that offers roughly the same functionality that PodiumD will offer.<br/><br/>Many Dimpact municipalities currently use the eSuite and will (gradually) migrate to PodiumD in the coming years. |
| HIB | Hosting, Integratie en Beheer | Project within Beyond 23 program that is responsible for setting up a shared hosting environment, operations and service desk. |
| IAM | Identity and Access Management | For the PodiumD IAM architecture see: [Zero Trust IAM Architectuur voor PodiumD](https://drive.google.com/drive/folders/1_o4YdwDZT2trFv9_RnHqlL6Vah49VBlk) |
| IGA | Identity Governance and Administration | Identity and access management at the organization (municipality) level. Part of IAM that is outside of PodiumD but instead is part of the existing municipality IT landscape. |
| ImZTC | Informatiemodel Zaaktypen | Aimed at specifying zaaktypen. Zaaktypen are used to direct the handling of zaken. |
| KCC | Klant Contact Centrum | |
| KING | Kwaliteitsinstituut Nederlandse Gemeenten | Now VNG Realisatie. |
| KISS | Klant Interactie Service Systeem | See: [https://www.icatt.nl/cases/kiss](https://www.icatt.nl/cases/kiss) |
| [KVK](https://www.kvk.nl/) | Kamer van Koophandel | ZAC integrates with the KVK API. |
| MDTO | Metadata Duurzame Toegankelijkheid Overheid | Used for archiving etc purposes, partly in other components like the ABC. |
| MOR | Melding Openbare Ruimte | An example of a zaaktype in ZGW. |
| Notificaties | Notifications. In the context of ZAC a purely technical concept where ZAC is notified of certain technical events from the Open Notificaties component. | ZAC is configured in Open Notificaties to be notified of certain events (e.g. a new zaak has been created in Open Zaak). These notifications are technically HTTP REST requests from Open Notificaties to the ZAC backend API ('notificaties' endpoint). Some of these notifications can result in signaleringen to the end-user. |
| OIP | Open Inwoner Platform | See: [https://www.maykinmedia.nl/blog/2023/feb/21/maykin-het-open-inwoner-platform/](https://www.maykinmedia.nl/blog/2023/feb/21/maykin-het-open-inwoner-platform/) |
| OK | Open Klant | See: [https://github.com/maykinmedia/open-klant](https://github.com/maykinmedia/open-klant) |
| [PDC](https://iplo.nl/digitaal-stelsel/vergunningaanvragen-ontvangen-samenwerken/zaaktypecatalogus-ztc-producten-dienstencatalogus/) | Producten- en Diensten Catalogus | |
| POD | PodiumD | PodiumD is the Dimpact PaaS solution in which the various components are connected and which is set up per municipality. |
| [RBAC](https://en.wikipedia.org/wiki/Role-based_access_control) | Role-based Access Control | “Role-based access control is a policy-neutral access-control mechanism defined around roles and privileges.”<br/><br/>Part of the [PodiumD IAM architecture](https://drive.google.com/drive/folders/1_o4YdwDZT2trFv9_RnHqlL6Vah49VBlk). |
| RGBZ | Referentiemodel Gemeentelijke Basisgegevens Zaken, also known as Informatiemodel Zaken. | Aimed at the information supply for handling zaken and accounting for them (afterwards). |
| RSGB | Referentiemodel Stelsel van Gemeentelijke Basisgegevens, also known as Informatiemodel Basis- en Kerngegevens. | Dimpact considers this outdated. |
| [RSIN](https://www.kvk.nl/over-het-handelsregister/rsin-nummer/) | Rechtspersonen en Samenwerkingsverbanden Informatienummer | An RSIN (translated as “Legal Entities and Partnerships Information Number”) refers to an organization or legal entity, rather than an individual person. It’s used for data exchange between (governmental) organizations, such as the Netherlands Chamber of Commerce (KVK) and the Tax Authority (Belastingdienst). Note that sole proprietorships (eenmanszaken) do not have an RSIN. |
| [SAM](https://www.dimpact.nl/news/dimpact-introduceert-nieuwe-samenwerkingsomgeving-voor-haar-leden) | | Samenwerkingsomgeving Dimpact in Sharepoint |
| SDG | Single Digital Gateway | |
| Sdu | | [https://www.sdu.nl/over-sdu](https://www.sdu.nl/over-sdu) |
| [SSC](https://www.ssc-ict.nl/) / SSC-IT | Shared Service Center | Hosting partner for the Dimpact PodiumD environments. Specifically SSC Enschede. |
| Signaleringen | Functionality in ZAC where the user is ‘signalled’ of certain events (e.g. tasks or zaken assigned to them), as a card on the dashboard, as an e-mail, or both. | See the [ZAC user manual](https://github.com/infonl/dimpact-zaakafhandelcomponent/tree/main/docs/manuals) for details. |
| StUF | Standaard Uitwisseling Formaat | Old standard for data exchange between municipalities and the government. Partly a predecessor of ZGW. Still used by many municipalities. |
| StUF-ZKN | Stuf Zaken | The predecessor of ZGW and the ZGW APIs. Still used by many municipalities. |
| TSA | Taak Specifieke Applicatie | An application developed for a specific type of zaak, for example iBurgerzaken. These applications sometimes have an integrated zaak system or are connected to a zaak system.<br/>For example, a parking permit application of a specific municipality. |
| VNG | Vereniging Nederlandse Gemeentes | VNG maintains the Common Ground standard amongst many other things shared between Dutch municipalities. |
| VRL | [VNG Referentielijsten API](https://vng-realisatie.github.io/gemma-zaken/ontwikkelaars/) | Is used in the Atos ZAC source code. |
| VT&H | [https://vng.nl/rubrieken/onderwerpen/vergunningverlening-toezicht-en-handhaving](https://vng.nl/rubrieken/onderwerpen/vergunningverlening-toezicht-en-handhaving) | |
| WOO | [https://www.rijksoverheid.nl/onderwerpen/wet-open-overheid-woo](https://www.rijksoverheid.nl/onderwerpen/wet-open-overheid-woo) | The Wet open overheid (WOO) requires Dutch councils to actively publish certain types of information, such as laws, decisions, and research reports. Councils must respond to information requests within 28 days, with a possible 14-day extension. The law aims to enhance transparency and accessibility of public information. |
| [Zaak](https://www.gemmaonline.nl/index.php/Thema-architectuur_Zaakgericht_werken) | ‘Case' as used in 'zaakgericht werken’, a Dutch approach to case management for municipalities and other Dutch public institutions. | A coherent amount of work with a clear cause and a clear end result, of which the quality and lead time must be monitored.<br/>Not to be confused with the term `Case` in CMMN. A CMMN zaakprocess is modeled using a CMMN case, but a zaak is a more generic term, if only because a BPMN zaakprocess is modeled by a BPMN process instance. |
| Zaaktype | Case type | The type of a zaak, defined in a zaaktypecatalogus (ZTC) such as Open Zaak. A zaaktype sets, among other things, the statustypen, resultaattypen, roltypen and informatieobjecttypen of its zaken. In ZAC the handling of the zaken of a zaaktype is configured with ZAPs, and users are authorised per zaaktype. |
| ZAP / ZAPs | Zaak Afhandel Parameter / Zaak Afhandel Parameters | Zaaktype configuration managed by beheerders in the ZAC UI used to configure parts of the zaak handling process (CMMN or BPMN) process in ZAC. |
| ZGW | Zaakgericht werken | Part of this are the ZGW APIs, which are used within PodiumD Zaak. |
| ZRC | Zaakregistratiecomponent | Open Zaak is an example of a zaakregistratiecomponent (but Open Zaak is more than that). |
| ZTC | Zaaktypecatalogus | Open Zaak is, among other things, also a zaaktypecatalogus. |
