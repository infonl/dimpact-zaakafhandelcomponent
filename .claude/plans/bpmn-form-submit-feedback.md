# Terugkoppeling bij het versturen van een BPMN-taakformulier

Overdrachtsdocument bij de frontend-wijziging op deze branch. Het eerste deel is bedoeld
voor iedereen; de technische toelichting staat achteraan.

## Wat ging er mis?

Als het afronden van een taak mislukte, zag de gebruiker dat niet. De knop "Afronden"
werd groen met een vinkje — precies zoals bij een geslaagde afronding. Er kwam geen
foutmelding en geen waarschuwing.

Een harde serverfout was daardoor niet te onderscheiden van succes. De gebruiker ging
verder met ander werk terwijl de taak was blijven staan.

Daarnaast bleef het formulier tijdens het versturen invulbaar. Op een langzame
verbinding duurt dat enkele seconden, waarin de gebruiker velden kon aanpassen die al
onderweg waren, of nog een keer op Afronden kon klikken.

## Wat is er nu veranderd?

**Bij een mislukking krijgt de gebruiker het te zien.** De knop wordt rood in plaats van
groen, en er verschijnt de standaard foutmeldingsdialoog van ZAC — dus geen kort
verdwijnend melding-balkje onderin, maar hetzelfde venster als bij andere fouten in de
applicatie. Bij een serverfout staat daar
ook de technische toelichting in, zodat een melding bij support bruikbaar is.

**Het formulier is op slot tijdens het versturen.** Zolang de opdracht onderweg is, zijn
de velden niet aanpasbaar. Zodra het antwoord binnen is, gaat het slot er weer af.

**Na een mislukking kun je opnieuw proberen.** De velden komen weer vrij en de ingevulde
gegevens blijven staan. Dat is bewust: als het misging door bijvoorbeeld een korte
storing, moet de gebruiker het gewoon nog eens kunnen versturen zonder alles opnieuw in
te vullen.

**Elke fout van de server komt nu bij de gebruiker terecht, niet alleen deze ene.** De
wijziging is niet specifiek voor het probleem dat hem aan het licht bracht. Is er iets
mis met het formulier zelf, of gaat er verderop in het proces iets mis, dan wordt die
melding nu net zo goed getoond. Voorheen verdwenen al die fouten geruisloos achter een
groene knop met een vinkje.

Goed te zien bij een tweede poging om dezelfde taak af te ronden: de server meldt dan dat
de taak al afgerond is, en die melding komt nu daadwerkelijk in beeld.

**Na een geslaagde afronding is het formulier direct definitief.** Voorheen bleef het nog
een paar seconden invulbaar totdat ZAC de nieuwe status had opgehaald. Nu wordt de status
uit het antwoord van de server overgenomen, dus dat gat is weg.

Belangrijk daarbij: ZAC bepaalt zelf niet dat een taak afgerond is. Het neemt over wat de
server terugmeldt. Besluit het proces dat de taak open blijft, dan blijft het formulier
gewoon invulbaar. Dat is geen fout, maar het proces dat zijn werk doet.

## Wat kun je testen?

1. **Geslaagd afronden** — knop wordt groen, melding "De taak is afgerond", formulier
   wordt direct niet meer invulbaar.
2. **Mislukt afronden** — knop wordt rood, foutdialoog verschijnt met de melding van de
   server, velden komen weer vrij, ingevulde gegevens staan er nog.
3. **Tijdens het versturen** — velden zijn even niet aanpasbaar. Goed te zien met een
   langzame verbinding (in de browser te simuleren).

Een mislukking is af te dwingen door dezelfde taak twee keer af te ronden, of door het
verzoek in de browser te blokkeren.

---

## Technische toelichting

### Waarom een mislukking op succes leek

`FormioWrapperComponent` stuurde `submitDone` bij elke overgang van `submitPending`
`true` → `false`, ongeacht de uitkomst. Form.io kleurt de knop daarop groen
(`btn-success submit-success`; het vinkje komt uit CSS: `.submit-success::after`).
Omdat `isPending()` ook bij een fout naar `false` gaat, werd een 500 gemeld als succes.
De mutaties hadden bovendien geen `onError`, dus er kwam ook langs die weg geen melding.

### Hoe het nu werkt

- Nieuwe input `submitFailed`, gevuld uit `hasFailed()` in `TaakViewComponent`
  (`isError()` van beide mutaties).
- Bij een mislukking stuurt de wrapper `submissionError` in plaats van `submissionDone`.
  Dat is gebonden aan `<formio [error]>`, wat `@formio/angular` doorzet als Form.io's
  `submitError` — rode knop, en de knop wordt weer bruikbaar
  (`Button.js` zet in die handler zelf `disabled = false`).
- `onError` op beide mutaties gaat via `FoutAfhandelingService.foutAfhandelen(error)`,
  dezelfde route als `HumanTaskDoComponent`. Bij 5xx en 403 toont die dialoog ook de
  server-detailtekst. Het errortype is al `HttpErrorResponse` in `zacQueryClient`, dus er
  is geen cast nodig.
- `applySubmitPending()` zet `disabled` op de levende Form.io-componenten zolang het
  verzoek loopt. Bewust **zonder** `redraw()`: een redraw bouwt de verzendknop opnieuw op
  en gooit de spinner van het lopende verzoek weg.
- `onSuccess` roept `init(task, false)` aan met het antwoord van de server, zodat
  `isReadonly()` zich baseert op de teruggemelde status en rechten. Geen aanname over
  `AFGEROND` in de frontend.

### Aandachtspunten

- Een eigen foutboodschap meegeven aan de rode knop kan niet: `@formio/angular` verpakt
  de fout in een array en Form.io gebruikt alleen een boodschap als die een string is.
  De knop toont dus Form.io's eigen vertaalde tekst; de specifieke fout komt uit de
  dialoog. Het meegegeven object is daarom alleen een marker.
- Niets emitten bij een mislukking is geen optie: dan blijft de knop uitgeschakeld met
  een draaiende spinner staan, zonder mogelijkheid om opnieuw te proberen. Ook
  `silent: true` helpt niet — `@formio/angular` slaat dan het `submitError`-event over.
- `applySubmitPending()` zet `disabled` alleen door op `refs.input`. `Select` en `Tags`
  regelen hun eigen widget via hun `disabled`-setter, maar knoppen (`refs.button`) en de
  rijknoppen van een datagrid worden niet meegenomen. Voor de huidige formulieren is dat
  geen probleem; bij formulieren met datagrids is dit het eerste om te controleren.
- De partiële route (`updateTaakdataMutation`) is met de huidige formulieren
  onbereikbaar: geen enkel BPMN-formulier heeft een knop met `state: "draft"`. De
  foutafhandeling zit er wel op, maar is niet met een echt formulier te testen.

### Losse bevindingen, niet in deze wijziging opgelost

- Twee keer afronden geeft HTTP 500 in plaats van 409. `TaskNotFoundException` heeft geen
  eigen tak in `RestExceptionMapper` en valt door naar een serverfout. Met een 409 zou de
  frontend "deze taak is al afgerond" kunnen tonen in plaats van een technische dialoog.
- Er is een tweede oorzaak met hetzelfde symptoom: een fout in `SendEmailDelegate` /
  `MailService` tijdens `completeTask`. Losse bevinding, eigen ticket.
