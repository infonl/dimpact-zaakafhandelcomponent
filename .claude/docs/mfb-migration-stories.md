# MFB-migratie — Jira Rovo story-voorbereiding

- Elk blok hieronder is één user story. Kopieer het als body naar Rovo of plak het als story-beschrijving.
- Parent is PZ-11246
- Subtaken
  - Ombouwen
  - Testen
  - Demo slide

---

## Story 1 — Taakaanpak opschonen

**Als** behandelaar
**Wil ik** dat het afhandelen van taken (`HumanTaskDoComponent`) geen gebruik meer maakt van de verouderde MFB-formulierlogica, maar uitsluitend van de nieuwe Angular-formuliercomponenten
**Zodat** er geen verouderde onderdelen meer actief zijn op de achtergrond die fouten of onverwacht gedrag kunnen veroorzaken

**Acceptatiecriteria:**

- Het formulier wordt getoond zodra de taakvelden geladen zijn
- Het formulier wordt niet getoond zolang de taakvelden nog laden (laad-indicator zichtbaar)
- Het opslaan van het formulier verstuurt de ingevulde taakvelden naar de backend
- Na succesvol opslaan sluit het formulierpaneel
- Bij een fout bij opslaan krijgt de behandelaar een foutmelding en blijft het paneel open

**Achtergrond:**
Zie [MFB-migratie op Confluence](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/edit-v2/1046609923?draftShareId=2650a600-b4c4-405f-afcb-8ec70223e00c)

---

## Story 2 — Inline bewerken loskoppelen van verouderd systeem

**Als** behandelaar
**Wil ik** dat het inline bewerken van velden (`EditComponent` + `EditInputComponent`) geen gebruik meer maakt van de verouderde MFB-formulierlogica, maar van een eenvoudig Angular Material invoerveld
**Zodat** het bewerken van losse velden snel en betrouwbaar werkt zonder afhankelijkheid van het verouderde formuliersysteem

**Acceptatiecriteria:**

- Een klik op het bewerkpictogram maakt een invoerveld zichtbaar op de plek van de waarde
- Het invoerveld toont de huidige waarde als beginwaarde
- Na het aanpassen en opslaan wordt de nieuwe waarde getoond
- Na het annuleren wordt de oorspronkelijke waarde getoond en is het invoerveld verdwenen
- Het invoerveld is toegankelijk via het toetsenbord (tab, enter, escape)

**Achtergrond:**
Zie [MFB-migratie op Confluence](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/edit-v2/1046609923?draftShareId=2650a600-b4c4-405f-afcb-8ec70223e00c)

---

## Story 3 — Documenten verzenden via vernieuwd formulier

**Als** behandelaar
**Wil ik** dat het verzendpaneel voor documenten (`InformatieObjectVerzendenComponent`) geen gebruik meer maakt van de verouderde MFB-formulierlogica, maar van een set expliciete formuliercomponenten: `zac-documents`, `zac-date` en `zac-textarea`
**Zodat** het verzenden van documenten betrouwbaar werkt via een helder, modern formulier

**Acceptatiecriteria:**

- Het paneel toont de lijst van beschikbare documenten voor de actieve zaak
- De behandelaar kan één of meerdere documenten selecteren
- Het verzenddatum-veld is verplicht; opslaan zonder datum is niet mogelijk
- Het toelichting-veld is optioneel
- Bij het wisselen van zaak toont het paneel de documenten van de nieuwe zaak
- Na succesvol versturen sluit het paneel en zijn de documenten als verstuurd gemarkeerd
- Bij een fout bij versturen krijgt de behandelaar een foutmelding en blijft het paneel open

**Achtergrond:**
Zie [MFB-migratie op Confluence](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/edit-v2/1046609923?draftShareId=2650a600-b4c4-405f-afcb-8ec70223e00c)

---

## Story 4a — Bevestigingsvensters vernieuwen (eerste vijf)

**Als** behandelaar
**Wil ik** dat de bevestigingsvensters voor zaakacties (`PromptDialogComponent`, batch a: afbreken, heropenen, hervatten, initiator wijzigen, document ontkoppelen) geen gebruik meer maken van de verouderde MFB-formulierlogica, maar van `zac-input`, `zac-textarea` of `zac-select`
**Zodat** foutmeldingen zichtbaar blijven en de behandelaar een actie opnieuw kan proberen zonder het venster te sluiten

**Acceptatiecriteria:**

- Het bevestigingsvenster opent met de juiste titel en (waar van toepassing) het juiste invoerveld
- De bevestigingsknop is uitgeschakeld zolang de verwerking bezig is
- Na succesvol bevestigen sluit het venster en is de zaakpagina bijgewerkt
- Bij een fout blijft het venster open, is de foutmelding zichtbaar en is de bevestigingsknop weer klikbaar
- De behandelaar kan de actie opnieuw proberen na een fout
- Het venster sluit bij annuleren zonder dat er iets is gewijzigd

**Achtergrond:**
Zie [MFB-migratie op Confluence](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/edit-v2/1046609923?draftShareId=2650a600-b4c4-405f-afcb-8ec70223e00c)

---

## Story 4b — Bevestigingsvensters vernieuwen (resterende vijf)

**Als** behandelaar
**Wil ik** dat de overige bevestigingsvensters (`PromptDialogComponent`, batch b: betrokkene ontkoppelen, BAG-object ontkoppelen, zaak-documenten document ontkoppelen, document verwijderen, besluit intrekken) geen gebruik meer maken van de verouderde MFB-formulierlogica, maar van `zac-input`, `zac-textarea` of `zac-select`
**Zodat** foutafhandeling in alle bevestigingsvensters consistent en duidelijk is

**Acceptatiecriteria:**

- Alle criteria uit Story 4a gelden ook voor deze vijf vensters
- Bij het verwijderen van een document zonder gekoppelde zaak toont het venster geen reden-invoerveld
- Bij het verwijderen van een document mét gekoppelde zaak toont het venster wel een reden-invoerveld
- Bij het intrekken van een besluit toont het venster een keuzelijst met intrekkingsredenen
- De intrekknop is pas klikbaar nadat een reden is gekozen

**Achtergrond:**
Zie [MFB-migratie op Confluence](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/edit-v2/1046609923?draftShareId=2650a600-b4c4-405f-afcb-8ec70223e00c)

---

## Story 5 — Besluit bewerken via vernieuwd formulier

**Als** behandelaar
**Wil ik** dat het bewerkpaneel voor besluiten (`BesluitEditComponent`) geen gebruik meer maakt van de verouderde MFB-formulierlogica, maar van een set expliciete formuliercomponenten: `zac-input`, `zac-date`, `zac-textarea` en `zac-documents`
**Zodat** het bewerken van besluiten betrouwbaar werkt via een helder, modern formulier

**Acceptatiecriteria:**

- Het paneel toont alle velden vooraf ingevuld met de huidige besluitwaarden
- Het besluittype-veld is alleen-lezen
- De ingangsdatum is verplicht; opslaan zonder ingangsdatum is niet mogelijk
- De vervaldatum kan niet eerder zijn dan de ingangsdatum
- De publicatievelden (publicatiedatum, uiterste reactiedatum) zijn alleen zichtbaar als het besluittype publicatie vereist
- De publicatievelden zijn niet zichtbaar als het besluittype geen publicatie vereist
- Na het wijzigen van het besluittype worden de bijbehorende documenten opnieuw geladen
- Het reden-veld is verplicht; opslaan zonder reden is niet mogelijk
- Na succesvol opslaan sluit het paneel en is het besluit bijgewerkt
- Bij een fout bij opslaan krijgt de behandelaar een foutmelding en blijft het paneel open

**Achtergrond:**
Zie [MFB-migratie op Confluence](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/edit-v2/1046609923?draftShareId=2650a600-b4c4-405f-afcb-8ec70223e00c)

---

## Story 6 — Besluit bekijken en intrekken via vernieuwd scherm

**Als** behandelaar
**Wil ik** dat het besluitdetailscherm (`BesluitViewComponent`) geen gebruik meer maakt van de verouderde MFB-formulierlogica, maar van statische weergavecomponenten en `zac-documents`
**Zodat** het raadplegen en intrekken van besluiten betrouwbaar werkt zonder verouderde onderdelen

**Acceptatiecriteria:**

- Alle besluitvelden zijn leesbaar weergegeven op het scherm
- De gekoppelde documenten per besluit zijn zichtbaar in een documentenlijst
- De documentenlijst is alleen-lezen (geen selectie of bewerking mogelijk)
- De intrekknop is zichtbaar en klikbaar
- Bij het klikken op intrekken opent een bevestigingsvenster met een reden-invoerveld
- Na succesvol intrekken is het besluit als ingetrokken gemarkeerd in het overzicht
- Bij een fout bij intrekken krijgt de behandelaar een foutmelding en blijft het venster open

**Achtergrond:**
Zie [MFB-migratie op Confluence](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/edit-v2/1046609923?draftShareId=2650a600-b4c4-405f-afcb-8ec70223e00c)

---

## Story 7 — Taakdetails bekijken via vernieuwd scherm

**Als** behandelaar
**Wil ik** dat het taakdetailscherm (`TaakViewComponent`) geen gebruik meer maakt van de verouderde MFB-formulierlogica, maar van `zac-auto-complete` voor groep en medewerker en de bestaande Angular-formuliercomponenten voor de taakvelden
**Zodat** het afhandelen van taken betrouwbaar werkt zonder verouderde onderdelen

**Acceptatiecriteria:**

- Het taakformulier toont de juiste velden voor de betreffende taaksoort
- Het groepsveld toont een zoekbare lijst van beschikbare groepen
- Het medewerkerveld toont een zoekbare lijst van medewerkers binnen de gekozen groep
- Bij het wisselen van groep wordt de medewerkerslijst opnieuw geladen
- Alle taakvelden zijn vooraf ingevuld met de huidige waarden
- Na het invullen en opslaan worden de taakvelden bijgewerkt in het zaakdossier
- Een webformulier-taak (Form.io) rendert het formulier correct zonder zichtbare wijziging voor de behandelaar
- Bij een fout bij opslaan krijgt de behandelaar een foutmelding en blijft het scherm open

**Achtergrond:**
Zie [MFB-migratie op Confluence](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/edit-v2/1046609923?draftShareId=2650a600-b4c4-405f-afcb-8ec70223e00c)

---

## Story 8 — Procestaak: beslissing nemen en uitvoeren

> ⚠️ Deze story kan pas worden ingepland nadat een product- en technische beslissing is genomen: de functionaliteit volledig bouwen, of bewust verwijderen.

**Als** producteigenaar
**Wil ik** een beslissing nemen of de procestaak-functionaliteit (`ProcessTaskDoComponent`) wordt gebouwd of verwijderd
**Zodat** er geen onzichtbaar gebroken functionaliteit meer in het systeem zit en het team weet wat er van hen verwacht wordt

**Acceptatiecriteria (bij keuze "verwijderen"):**

- Een behandelaar ziet geen lege of niet-reagerende schermen meer voor procestaken
- Er zijn geen verwijzingen naar procestaak-functionaliteit zichtbaar in de applicatie

**Acceptatiecriteria (bij keuze "bouwen"):**

- Een procestaak opent het juiste formulier met de benodigde velden
- Na invullen en bevestigen wordt de procestaak correct verwerkt in de backend
- Bij een fout krijgt de behandelaar een foutmelding en kan het opnieuw proberen
- De functionaliteit werkt via moderne formuliercomponenten zonder verouderde onderdelen

**Achtergrond:**
Zie [MFB-migratie op Confluence](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/edit-v2/1046609923?draftShareId=2650a600-b4c4-405f-afcb-8ec70223e00c)
