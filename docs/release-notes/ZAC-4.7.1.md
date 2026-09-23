# ZAC 4.7.1

| **Release** | https://dimpact.atlassian.net/projects/PZ/versions/12657/tab/release-report-all-issues |
| --- | --- |
| **Date** | <!-- TODO: vul releasedatum in --> |
| **Version** | ZAC 4.7.1 |
| **Description** | |
| **Contributors** | <!-- TODO: vul contributors in --> |

## 📇 Samenvatting

Deze release bevat een groot aantal verbeteringen aan de ZAC applicatie. De belangrijkste wijzigingen zijn:

* Uitgebreide BPMN-verbeteringen, waaronder een visuele procesflow-weergave met zoom- en navigatiefunctionaliteit, ondertekening van documenten in een BPMN-proces, het starten van BPMN-zaken vanuit Open Formulieren, en het automatisch toevoegen van initiator en betrokkenen uit productaanvragen. Zie Nieuwe functionaliteit.
* SmartDocuments-integratie in BPMN zaakafhandelparameters (ZAPs), inclusief ondersteuning voor template-velden in formulieren. Zie Nieuwe functionaliteit.
* Automatische ontvangstbevestiging is nu beschikbaar in BPMN-processen, met ondersteuning voor aanvraagspecifieke e-mailadressen en zaakdata in mailsjablonen. Zie Nieuwe functionaliteit.
* Aanvraagspecifieke contactgegevens worden nu getoond op de zaakdetailpagina. Zie Nieuwe functionaliteit.
* IAM-verbeteringen, waaronder de mogelijkheid om groepen te deactiveren en ondersteuning voor de functionele rol 'Geen enkel entiteitstype'. Zie Nieuwe functionaliteit.
* Office Converter (kontextwork-converter) is vervangen door Gotenberg vanwege kritieke beveiligingsproblemen. Zie Security verbeteringen.
* Niet meer onderhouden frontend-bibliotheken zijn vervangen. Zie Security verbeteringen.
* Verbeterde stabiliteit door structured JSON logging, WildFly-transactieconfiguratie en een verbeterd versiebeheersysteem. Zie Stabiliteit verbeteringen.
* Documentatie-updates voor zaakdata in e-mails, SmartDocuments template-velden en automatische ontvangstbevestiging. Zie Documentatie.
* 3 bug fixes vanuit Jira, diverse bug fixes uit commits, library updates en code stewardship-items waaronder een groot aantal Angular v19 standalone migraties en Kotlin-conversies.

## 😁 Nieuwe functionaliteit

### [[BPMN] Procesflow en actuele processtatus/-stap inclusief de versie van het proces](https://dimpact.atlassian.net/browse/PZ-9366)

Bij een BPMN-zaak is nu een visuele procesflow beschikbaar in een zijpaneel op de zaakdetailpagina. De procesflow toont de volledige BPMN-processtructuur met daarin de actuele processtatus en -stap. Het zijpaneel bevat zoom-functionaliteit en toetsenbordnavigatie om door grotere processen te navigeren. De titel van het dialoog bevat de procesdefinitienaam en het versienummer, zodat na een update van het procesmodel zichtbaar is met welke versie de zaak is gestart.

### [[BPMN] Taak voor het ondertekenen van documenten](https://dimpact.atlassian.net/browse/PZ-7712)

Het is nu mogelijk om documenten te ondertekenen binnen een BPMN-proces. Via een specifieke delegate in het procesmodel kunnen documenten automatisch ter ondertekening worden aangeboden als onderdeel van de processtappen.

### [[BPMN] Starten BPMN zaak vanuit een Open formulier](https://dimpact.atlassian.net/browse/PZ-10316)

Het starten van een BPMN-zaak vanuit een Open Formulieren-inzending is mogelijk gemaakt. Wanneer een productaanvraag binnenkomt via Open Formulieren, kan nu automatisch een BPMN-zaak worden gestart in plaats van alleen een CMMN-zaak.

### [[BPMN] Initiator en andere betrokkenen aan de BPMN zaak toevoegen uit de productaanvraag](https://dimpact.atlassian.net/browse/PZ-10765)

Bij het starten van een BPMN-zaak vanuit een productaanvraag worden de initiator en andere betrokkenen nu automatisch aan de zaak toegevoegd. Dit zorgt ervoor dat de relevante persoonsgegevens direct beschikbaar zijn in het zaakdossier.

### [[BPMN] Inhoud mailsjabloon in taakscherm kunnen tonen en zaakdata meenemen in te verzenden mail](https://dimpact.atlassian.net/browse/PZ-8151)

Mailsjablonen kunnen nu worden getoond en bewerkt in BPMN-taakformulieren. Daarnaast kunnen zaakdata als variabelen worden gebruikt in e-mailsjablonen, waardoor dynamische en contextspecifieke e-mails vanuit taken kunnen worden verstuurd. De taakformulieren voor Extern Advies Mail en Extern Advies Vastleggen zijn gemigreerd naar Angular-formulieren.

### [[BPMN] Procesdefinitie - Formdefinities uniek maken per procesdefinitie](https://dimpact.atlassian.net/browse/PZ-10156)

Formulierdefinities zijn nu uniek per procesdefinitie. Dit voorkomt conflicten wanneer meerdere procesdefinities dezelfde formuliernamen gebruiken en maakt het mogelijk om formulieren per proces onafhankelijk te configureren.

### [BPMN] SmartDocuments-integratie in BPMN

SmartDocuments is nu beschikbaar in BPMN zaakafhandelparameters (ZAPs). Hiermee kunnen documenten worden gegenereerd vanuit SmartDocuments-sjablonen als onderdeel van een BPMN-proces. SmartDocuments-configuraties worden automatisch opgeslagen bij het aanmaken van een nieuwe versie van een zaaktype. Daarnaast zijn SmartDocuments template-velden beschikbaar in formulieren (formio).

### [BPMN] Automatische ontvangstbevestiging in BPMN-proces

Het automatisch versturen van een ontvangstbevestiging is nu mogelijk gemaakt binnen BPMN-processen. Wanneer een productaanvraag binnenkomt, kan het BPMN-proces automatisch een bevestigingsmail versturen naar de indiener.

### [Aanvraagspecifieke contactgegevens tonen op zaakdetailpagina](https://dimpact.atlassian.net/browse/PZ-10533)

Op de zaakdetailpagina worden nu zaakspecifieke contactgegevens getoond die afkomstig zijn uit de productaanvraag. Dit maakt het voor behandelaars eenvoudiger om de juiste contactgegevens te vinden voor communicatie met de aanvrager.

### [Gebruik voorkeurs e-mailadres bij versturen automatische bevestiging](https://dimpact.atlassian.net/browse/PZ-10505)

Bij het versturen van een automatische bevestiging wordt nu het voorkeurs e-mailadres uit de contactgegevens van de persoon of het bedrijf gebruikt. In alle e-mailformulieren prevaleert het contactgegevens-e-mailadres boven het initiator-e-mailadres.

### [Aanvraagspecifiek e-mailadres gebruiken bij versturen automatische ontvangstbevestiging](https://dimpact.atlassian.net/browse/PZ-10634)

Bij het versturen van een automatische ontvangstbevestiging voor een productaanvraag wordt nu het aanvraagspecifieke e-mailadres gebruikt in plaats van een generiek adres. Dit zorgt ervoor dat de bevestiging wordt verstuurd vanaf het juiste e-mailadres dat bij de betreffende aanvraag hoort.

### [IAM] Groepen deactiveren

Het is nu mogelijk om groepen te deactiveren in ZAC. Gedeactiveerde groepen worden visueel gemarkeerd met een "(inactief)" label en worden niet meer getoond in lijsten waar alleen actieve groepen relevant zijn, zoals bij het toewijzen van taken of zaken. De groepen blijven wel zichtbaar bij historische gegevens waar ze aan gekoppeld zijn.

### [[IAM] Support 'Geen enkel entiteitstype' functionele rol](https://dimpact.atlassian.net/browse/PZ-9811)

Ondersteuning is toegevoegd voor de functionele rol 'Geen enkel entiteitstype' in de nieuwe IAM-architectuur. Dit maakt het mogelijk om gebruikers te configureren zonder specifiek entiteitstype.

### Documenten verwijderen via documentdetailpagina

Het is nu mogelijk om een enkelvoudig informatieobject direct te verwijderen via de documentdetailpagina wanneer het document niet aan een zaak is gekoppeld. Bij het verwijderen van een document wordt ook het bijbehorende inboxdocument verwijderd indien dit bestaat.

## 🙂 Verbeteringen aan bestaande functionaliteit

### [[BPMN] Procesdefinitie - Verbeteren opvoeren en beheren van de BPMN procesdefinitiebestanden](https://dimpact.atlassian.net/browse/PZ-8907)

De gebruikersinterface voor het opvoeren en beheren van BPMN-procesdefinities is verbeterd. Dit omvat een verbeterd visueel ontwerp, automatisch uitklappen van een geüploade definitie op basis van de processleutel uit de XML-inhoud, en diverse UX-verbeteringen op basis van gebruikersfeedback.

### [[BPMN] Foutmelding voor verplichte velden vertalen naar Nederlands](https://dimpact.atlassian.net/browse/PZ-6706)

Foutmeldingen voor verplichte velden in BPMN-formulieren zijn vertaald naar het Nederlands, zodat gebruikers een consistente Nederlandstalige ervaring hebben.

### [[Automatische ontvangstbevestiging] Aanhef in mailtemplate toont leeg bij onbekende initiator](https://dimpact.atlassian.net/browse/PZ-9213)

Wanneer bij het gebruik van de variabele \[ZAAK_INITIATOR\] in een mailsjabloon geen initiator aan de zaak is gekoppeld, wordt nu een leeg veld getoond in plaats van het woord "ONBEKEND". Dit resulteert in een professionelere uitstraling van de verstuurde e-mails.

### Dashboard kaarthoogtes gelijkgetrokken

De hoogtes van dashboard-kaarten zijn nu gelijkgetrokken per rij, zodat het dashboard er visueel consistenter uitziet.

### Prestatie verbeteringen

Diverse Angular-modules worden nu lazy-loaded (documenten, productaanvragen, signaleringen, BAG, admin-routes en formulieren), wat resulteert in snellere initiële laadtijden van de applicatie. Daarnaast zijn immutable cache headers toegevoegd voor gehashte assets via een StaticCacheFilter.

### Taak details - Inactieve groep indicator

Bij het wijzigen van de groepstoewijzing van een taak wordt nu een "(inactief)" label getoond voor groepen die gedeactiveerd zijn, zodat behandelaars duidelijk kunnen zien welke groepen niet meer actief zijn.

### Advies External Email - Standaard e-mailadres laden

Bij het Extern Advies e-mailformulier wordt nu het standaard e-mailadres automatisch geladen wanneer dit beschikbaar is.

## 😎 Stabiliteit verbeteringen

### Structured JSON logging

Structured JSON logging is toegevoegd aan WildFly. Dit maakt het mogelijk om logberichten in een gestructureerd JSON-formaat te genereren, wat het eenvoudiger maakt om logs te verwerken met tools als Elasticsearch of Grafana Loki. Een script is beschikbaar om de structured JSON logs te lezen.

### WildFly transactie configuratie

De WFLYTX0013 waarschuwingen bij het opstarten van WildFly zijn opgelost door een unieke node-identifier te configureren voor WildFly-transacties. De overbodige node-identifier configuratie is vervolgens opgeruimd.

### Versioning verbeterd

Het versiebeheersysteem is overgehaald naar rolling dev pre-releases en proper hotfix patch-versies. Dit maakt het eenvoudiger om hotfixes te releasen en dev-versies te traceren.

## 📘 Documentatie

Documentatie is bijgewerkt om het up-to-date te houden, te verbeteren en verduidelijken, of toe te voegen waar deze nog niet volledig was.

### Zaakdata gebruik in e-mails

Documentatie is toegevoegd over het gebruik van zaakdata als variabelen in e-mailsjablonen. Zie hiervoor de ontwikkeldocumentatie in de repository.

### SmartDocuments template-velden in formio

Documentatie is toegevoegd over het gebruik van SmartDocuments template-velden in formio-formulieren. Dit beschrijft hoe template-velden geconfigureerd en gebruikt kunnen worden.

### Automatische ontvangstbevestiging delegate

Documentatie is aangemaakt voor de nieuwe sendConfirmationEmailDelegate die gebruikt wordt voor het automatisch versturen van ontvangstbevestigingen in BPMN-processen.

### BPMN delegates - waarschuwing naamwijzigingen

Waarschuwingen zijn toegevoegd aan de documentatie over het hernoemen van BPMN-delegates om breaking changes te voorkomen bij bestaande processen.

### Deserializatie fix voor oudere documenten

Documentatie is bijgewerkt met informatie over de deserializatie-fix voor het ophalen van oudere inbox- en ontkoppelde documenten uit Open Zaak.

### IAM setup documentatie

Een fout in de IAM setup-documentatie is gecorrigeerd.

## 🔐 Security verbeteringen

### [Office Converter Security: Critical Multimedia Library Vulnerabilities](https://dimpact.atlassian.net/browse/PZ-8635)

De kontextwork-converter (Office Converter) is vervangen door Gotenberg vanwege kritieke beveiligingsproblemen in de multimedia-bibliotheken die door de converter werden gebruikt. Gotenberg biedt dezelfde functionaliteit voor documentconversie maar zonder de bekende beveiligingsrisico's.

### [[SECURITY] Frontend Angular Web App - Vervangen van niet meer onderhouden bibliotheken](https://dimpact.atlassian.net/browse/PZ-10664)

Niet meer onderhouden en deprecated NPM-packages in de frontend zijn vervangen door actief onderhouden alternatieven. Dit vermindert het risico op onopgeloste beveiligingsproblemen in de frontend.

### Supply chain attack bescherming

Best practices zijn geïmplementeerd voor betere bescherming tegen supply chain attacks. Dit omvat maatregelen om de integriteit van dependencies en het buildproces te waarborgen.

## 🐞 Bug fixes

### [Oudere documenten openen in Inbox documenten en Ontkoppelde documenten geeft foutmelding](https://dimpact.atlassian.net/browse/PZ-10865)

Bij het openen van oudere documenten in Inbox documenten en Ontkoppelde documenten verscheen een deserializatie-foutmelding ("Unable to deserialize property 'wijzigingen'"). Dit was veroorzaakt door een probleem bij het ophalen van de auditlog van het document uit Open Zaak. Dit is opgelost.

### [Taakgegevens in ZAP's worden niet ingevuld bij CMMN taken](https://dimpact.atlassian.net/browse/PZ-10376)

Na het invullen van taakgegevens (groep en doorlooptijd) in de zaakafhandelparameters (ZAP's) voor de taak "Aanvullende informatie" werden deze niet vooringevuld bij het aanmaken van de taak. Zowel de groep als de datum bleven leeg. Dit is opgelost zodat de geconfigureerde waarden nu correct worden overgenomen.

### [Verwijderen van een inboxdocument of ontkoppeld document via de documentdetailpagina geeft foutmelding](https://dimpact.atlassian.net/browse/PZ-10108)

Bij het verwijderen van een inboxdocument of ontkoppeld document via de documentdetailpagina verscheen de foutmelding "zaakUuid must not be null". Dit kwam doordat de verwijderfunctie altijd een zaak-UUID verwachtte, ook voor documenten die niet aan een zaak zijn gekoppeld. Dit is opgelost.

### Dashboard sortering

De sortering van de dashboard-kaarten "Aan mij toegewezen zaken/taken" is gecorrigeerd.

### Contact details incorrect getoond

Contactgegevens bij een zaak werden in bepaalde gevallen onjuist getoond als zaakspecifieke contactgegevens. Dit is gecorrigeerd.

### Grotere bestanden uploaden lokaal

Het was niet mogelijk om grotere bestanden lokaal te uploaden. De bestandsgroottelimiet is verhoogd zodat grotere bestanden nu succesvol geüpload kunnen worden.

### IllegalStateException bij ongeldige sessie

Een IllegalStateException trad op wanneer de code probeerde de ingelogde gebruiker op te halen met een geïnvalideerde sessie. Dit is opgelost met betere foutafhandeling.

### Informatie object historie deserialisatie

Een deserialisatie-probleem bij het ophalen van de historie van informatieobjecten is opgelost.

### ExternAdviesMail serialisatie

Een serialisatiefout bij de verzender van de ExternAdviesMail is opgelost en de taakformulier-specificaties zijn aangescherpt.

### Icon kleuren regressie na Angular migratie

Na de Angular v19-migratie waren de icoonkleuren in document- en taaklijsten en de taken-kaart incorrect. Dit is gecorrigeerd.

### PABC container versienummer

Het versienummer voor PABC-containers was incorrect en is gecorrigeerd.

## 🔧 Library updates

<!-- TODO: Vul de library updates tabel in op basis van Renovate PRs in de commit range v4.5.0..v4.7.1 -->

| **Context** | **Library** | **Version** | **Pull Request** |
| --- | --- | --- | --- |
| | | | |

## 🔒 Security updates

<!-- TODO: Vul de security updates tabel in op basis van Dependabot/Snyk alerts -->

| **Severity** | **Package** | **CVE** | **GHSA** | **Resolution** |
| --- | --- | --- | --- | --- |
| | | | | |

## ⛑️ Code Stewardship

Code Stewardship is het principe van onderhoud aan code die indirect waarde levert. Het zijn bijvoorbeeld wijzigingen aan code die het onderhoud daarvan vergemakkelijkt, of wijzigingen aan workflows voor het bouwen van de code, en andere kleine verbeteringen aan code. Zie ook https://www.mediawiki.org/wiki/Code_Stewardship voor meer details en uitleg.

### Jira tickets

| Ticket |
| --- |
| https://dimpact.atlassian.net/browse/PZ-8296 |
| https://dimpact.atlassian.net/browse/PZ-10860 |
| https://dimpact.atlassian.net/browse/PZ-10782 |
| https://dimpact.atlassian.net/browse/PZ-10590 |
| https://dimpact.atlassian.net/browse/PZ-10577 |
| https://dimpact.atlassian.net/browse/PZ-10569 |
| https://dimpact.atlassian.net/browse/PZ-10568 |
| https://dimpact.atlassian.net/browse/PZ-10544 |
| https://dimpact.atlassian.net/browse/PZ-10535 |
| https://dimpact.atlassian.net/browse/PZ-10525 |
| https://dimpact.atlassian.net/browse/PZ-10521 |
| https://dimpact.atlassian.net/browse/PZ-10499 |
| https://dimpact.atlassian.net/browse/PZ-10435 |
| https://dimpact.atlassian.net/browse/PZ-10341 |

### Angular v19 standalone migratie

Een groot aantal Angular-componenten is gemigreerd naar standalone componenten als onderdeel van de Angular v19-migratie. Dit betreft onder andere:

* ZoekObject cluster, MultiFacetFilterComponent, KlantZoekComponent, PersoonZoekComponent, BedrijfZoekComponent
* InboxDocumentenList, OntkoppeldeDocumentenList, InboxProductaanvragenList, ColumnPicker, Dialog, Edit, TakenCard
* TaakEditComponent, KlantContactmomentenTabelComponent, ZaakBetrokkeneFilterComponent
* EnhanceMatErrorDirective, ZacRadio, ZacCheckbox, ZacToggle, ZacTextarea, ZacDate, ZacSelect, ZacInput, ZacAutoComplete
* BAG-componenten, IdentityComponent, ZoekopdrachtComponent, SignaleringenSettingsComponent, FoutAfhandelingComponent
* ViewComponent, AdminComponent, SideNavComponent, GroepSignaleringenComponent, ReferentieTabellenComponent
* DateRangeFilterComponent, ParametersComponent, ParametersEditBpmnComponent, ParametersEditCmmnComponent
* NotificationDialogComponent, TekstFilterComponent, ConfirmDialogComponent, FoutDialogComponent, ActieOnmogelijkDialogComponent
* Mailtemplates, Inrichtingscheck, ReferentieTabel, EditInput
* ZaakVerkort, ZaakOntkoppelenDialog, ZakenVerdelenDialog, ZakenVrijgevenDialog
* PersoonView, BedrijfView, KlantZakenTabel, ZaakInitiator, KlantZoekDialog, Zaakdata, ZakenAfgehandeld, ZakenMijn, TakenMijn
* WerklijstComponent, BetrokkeneLinkComponent, ZaakOpschortenDialogComponent, ZaakVerlengenDialogComponent
* InformatieObjecten-componenten, TakenVerdelenDialogComponent, NotitiesComponent
* TakenWerkvoorraad, ZakenWerkvoorraad, Zoek
* BesluitCreateComponent, ZaakCreateComponent, CaseDetailsEditComponent, ZaakDocumentenComponent, ZaakLinkComponent
* ZaakViewComponent decompositie met ZaakTakenComponent extractie
* SignaleringenModule lazy-loading
* Lazy-loading van /admin routes en Form.io (TakenModule), Documenten en Productaanvragen modules

### Kotlin-conversies

Diverse Java-packages zijn geconverteerd naar Kotlin voor betere onderhoudbaarheid:

* WebDAV-code
* DRC client
* Inbox document package
* Detached document package en service
* Informatieobjecten package
* Productaanvraag package (opsplitsing in kleinere delen)

### Overige code stewardship

* Backend Kotlin-code verplaatst van `net.atos` naar `nl.info` package
* Verwijdering van alle formulierdefinities-code en database-tabellen
* Verwijdering van formio-tabel uit database
* Refactoring van zaak-afsluiten backend-logica
* Refactoring van zaak opschorten/hervatten backend-code
* Introductie van inbox document en detached document repository classes
* Verbeterde backend-code voor inbox documenten service
* Refactoring van contact details code
* Taakformulier classes hernoemd naar `*TaskForm` conventie
* SmartDocuments-tabellen hernoemd (CMMN-prefix verwijderd)
* Verbeterde OR client exception classes
* Verbeterde BPMN task not found error handling
* Introductie van `nowildcardimports` Detekt formatting-regel
* WildFly Galleon en Cloud Galleon Pack upgrades
* Verbetering van Helm chart resource definitions voor initcontainers en Solr
* Diverse CI/CD-verbeteringen (merge queue workflow, stale branches job, Snyk/CodeQL, Slack action)
* E2E-testverbeteringen voor BPMN en Open Forms flows
* Load-test scripts voor zaak-aanmaak en documentupload
* Cucumber HTML reporter vervangen
