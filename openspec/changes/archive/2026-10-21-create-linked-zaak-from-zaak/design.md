## Context

See proposal.md for the motivation. The current state:

- *Zaak koppelen* is the standalone, signal-based `zac-zaak-link` component (`src/main/app/src/app/zaken/zaak-link/`).
  `zaak-view` opens it in the `actionsSidenav` drawer when `activeAction === "actie.zaak.koppelen"`; the menu item
  is built in `zaak-view-menu.builder.ts` and shows only with `zaak.rechten.canWijzigen`. It has one form, no tabs.
- *Zaak aanmaken* is the routed page `zaken/create` (`zac-zaak-create`). It has no inputs, wraps itself in its own
  `mat-sidenav-container` for the initiator and BAG pickers, and navigates to the new zaak after `POST /rest/zaken/zaak`.
- `ZaakRestService.createZaak` creates, assigns and starts a zaak; `RestZaakAanmaakGegevens` has no link field, and
  `RestZaakCreateData.toZaak()` ignores its `gerelateerdeZaken`. The endpoint returns only the identificatie
  (spec `zaak-creation-rest-endpoint`).
- `ZaakKoppelenRestService.linkZaak` and `ZaakLinkData` hold the link rules. A gerelateerde zaak link patches only the
  source zaak; Open Zaak then adds the source zaak to the gerelateerde zaken of the other zaak itself, without an audit
  line on that zaak, so both zaken show it. A hoofd/deelzaak link patches `hoofdzaak` on the deelzaak; the hoofdzaak
  shows it through the read-only `deelzaken` of Open Zaak.
- `RestZaak.zaaktype.zaaktypeRelaties` already lists the deelzaaktypen of the zaaktype; the frontend does not use it yet.
- The zaakhistorie comes from the Open Zaak audit trail only.
- All backend code involved is Kotlin; no Java conversion is needed.

## Goals / Non-Goals

**Goals:**

- One request creates and links the new zaak, and all rights and relation checks run on the server before the zaak
  exists.
- Reuse the existing *Zaak aanmaken* form instead of building a second one.

**Non-Goals:**

- Choosing another aanvrager for the new zaak (PZ-11342).
- Creating a related zaak automatically from a BPMN process (PZ-8841).
- Creating a new zaak as *hoofdzaak* of the current zaak.
- Any link between the archiving (vernietiging) of the two zaken: each zaak keeps the archive rules of its own
  zaaktype, which is already the case.

## Decisions

### Extend `POST /rest/zaken/zaak` instead of create-then-link from the frontend

`RestZaakAanmaakGegevens` gets an optional `koppeling: RestZaakAanmaakKoppeling(zaakUuid: UUID, relatieType:
RelatieType)`. `relatieType` means the role of the new zaak, the same as `RestZaakLinkData.relatieType` means the role
of the zaak to link: `DEELZAAK` makes the new zaak a deelzaak of `zaakUuid`.

Alternative: the frontend calls `createZaak`, reads the new zaak by identificatie, then calls `PATCH /zaak/koppel`.
Rejected: three calls, a zaak without link when the last call fails, and a rights check after the zaak exists.

### Checks reuse `ZaakLinkData`

Before `zgwApiService.createZaak`, `createZaak` reads the zaak to link and its rechten and checks, in this order:
`canLezen` and `canKoppelen` (else `PolicyException`), the zaak is open, and for `DEELZAAK` the zaak is not a deelzaak
and the new zaaktype is in `getDeelzaaktypenSet()` of its zaaktype (else a validation error). Any other `relatieType`
is a validation error. The checks are a small function next to the existing rules in `ZaakLinkData.kt`, so that
`linkZaak` and `createZaak` cannot drift apart.

### Set the relation when the zaak is created, and leave the other side to Open Zaak

- `DEELZAAK`: the new zaak is created with `hoofdzaak` set. Afterwards the hoofdzaak is reindexed and gets a
  `ScreenEvent`, as `koppelHoofdEnDeelzaak` does, because Open Zaak notifies only for the deelzaak.
- `GERELATEERD`: the new zaak is created with the current zaak in `gerelateerdeZaken`. Open Zaak adds the new zaak to
  the gerelateerde zaken of the current zaak itself, so the relation is two-sided, as the item asks.

Alternative: patch the current zaak as well, with the explanation *Aangemaakt als gerelateerde zaak*, to give it a
history line. Rejected: by then Open Zaak has already added the new zaak, so the patch changes nothing, writes no audit
line, and only copies logic that belongs to Open Zaak.

The hoofdzaak notification moves from `ZaakKoppelenRestService` to `ZaakKoppelenService`, which both REST services use,
so the logic is not copied. That service also holds the checks and the link of a new zaak.

### Initiator is copied by the frontend

The tab passes the initiator of the current zaak into the form; the request carries it in `initiatorIdentificatie` as
today, so `assertCanAddBetrokkene` keeps guarding it. When the selected zaaktype does not allow that kind of
initiator, the form does not send it. Copying on the server was rejected because PZ-11342 will let the employee change
the aanvrager in the same form.

### Make `zac-zaak-create` embeddable

`zac-zaak-create` gets signal inputs and an output; the route keeps using it without inputs:

- `koppeling = input<{ zaak: RestZaak; relatieType: "GERELATEERD" | "DEELZAAK" } | null>(null)`. When set, the
  component filters the zaaktypen (deelzaaktypen from `zaak.zaaktype.zaaktypeRelaties` for `DEELZAAK`), shows the
  initiator read-only, sends `koppeling` in the request, hides the page title, and shows *Annuleren*.
- `zaakCreated = output<string>()` with the identificatie, and `cancelled = output<void>()`. When `koppeling` is set,
  the component emits instead of navigating.

Its inner `mat-sidenav-container` stays, so the BAG picker opens inside the drawer, over the form.

Alternative: extract a separate `zac-zaak-create-form`. Rejected for now: it moves the whole 335-line component and its
617-line spec for no behaviour gain.

### Tabs in `zac-zaak-link`

`zac-zaak-link` wraps its current form and table in a `mat-tab-group` tab *Bestaande zaak koppelen*. The second tab
*Nieuwe zaak aanmaken* holds a required `zac-select` *Soort koppeling* and, below it, `<zac-zaak-create [koppeling]>`.
`relationTypeUnavailableReason` is reused to disable *Deelzaak* on a deelzaak. On `zaakCreated` the component shows
`msg.zaak.aangemaakt-en-gekoppeld`, emits `zaakLinked` (so `zaak-view` resets the sidebar and reloads the zaak) and
closes. The tab is only rendered when the zaak is open, `zaak.rechten.koppelen` is true, and
`listZaaktypesForCreation` returns at least one zaaktype. `RestZaakRechten` does not expose `koppelen` today; the
backend layer adds it from `ZaakRechten.canKoppelen`.

## Design

Figma: [Zaak detailpagina - Zaak koppelen](https://www.figma.com/design/hwqjMzbwU1gqPIWeOqg8hb/Dimpact---Confidential?node-id=6232-133512)
(node 6232:133512).

The picture shows the zaak detail page with the action menu on the left (*Zaak koppelen* highlighted under
*Koppeling*) and, on the right, the *Zaak koppelen* sidebar over the page, with the account-tree icon and a close
button in the header.

- **Tabs** under the header: *Bestaande zaak koppelen* and *Nieuwe zaak aanmaken*. The picture shows the second tab
  selected. The first tab is the screen of today.
- **Intro text**: "Koppel zaak 'ZAAK-…' aan een andere zaak".
- **Soort koppeling\*** (select), showing *Gerelateerde zaak*. The other option is *Deelzaak*; the picture does not
  show the open list.
- **Section *Zaak aanmaken***, with the text "Deze zaak wordt automatisch gekoppeld aan zaak 'ZAAK-…' als gerelateerde
  zaak".
  - *Zaaktype\** (search field) with the hint "De groep en eventueel medewerker plus vertrouwelijkheidaanduiding worden
    gezet na het kiezen van het zaaktype".
  - *Initiator* (greyed out, read-only) showing the initiator of the current zaak, with a person icon.
  - *Startdatum\** (date picker, filled), *BAG objecten* (with a locate icon).
- **Section *Zaaktoewijzing***: *Zaak toekennen aan groep\** ("- Kies een groep -") and *Zaak toekennen aan
  medewerker\** ("- Geen behandelaar -", disabled until a groep is chosen).
- **Section *Overige gegevens***: *Communicatiekanaal\**, *Vertrouwelijkheidaanduiding\**, *Omschrijving\**,
  *Toelichting* (text area).
- **Buttons**: a primary button, disabled while the form is invalid, and *Annuleren*.

What differs from today: the sidebar gets tabs; the *Zaak aanmaken* form, now only a full page, also shows inside the
sidebar under a relation choice; the initiator is prefilled and read-only.

Not in the picture: the *Deelzaak* text, the empty zaaktype list, the loading state, the error state and the
confirmation after creation. These follow the existing ZAC patterns (see Assumptions).

## Assumptions

- The primary button in the picture reads *Zoeken*; that is a copy of the first tab. The button reads *Aanmaken*
  (`actie.aanmaken`), as on the *Zaak aanmaken* page.
- The *Deelzaak* text reads "Deze zaak wordt automatisch gekoppeld aan zaak '{{zaak}}' als deelzaak".
- For *Gerelateerde zaak* every zaaktype the employee may start is offered; the ZTC `gerelateerdeZaaktypen` are not
  used to limit the list, because few catalogi fill them and the item does not ask for it.
- *Deelzaak* means the new zaak is a deelzaak of the current zaak. Creating a new hoofdzaak is not offered: a zaak that
  already exists rarely needs a new parent, and the item lists only gerelateerde zaak and deelzaak.
- The tab is hidden, not disabled, when the employee may not use it, like other actions in the menu.
- The current zaak must be open; the existing link rules also refuse an open-closed pair.
- When the current zaak has no initiator, the initiator field is empty and read-only.
- The *Toelichting* of the new zaak is not prefilled.
- The employee stays on the current zaak after creation and opens the new zaak from *Gerelateerde zaken*; the item
  asks to be able to navigate, not to be taken there.
- Loading uses the existing spinner of `zac-zaak-create`; errors use the existing HTTP error handling, and the sidebar
  stays open with the entered data.
- History labels: `hoofdzaak` and `gerelateerdeZaken` get i18n labels (*Hoofdzaak*, *Gerelateerde zaken*) in the
  history, because they show as raw attribute names today.

## Risks / Trade-offs

- [Open Zaak writes no audit line on the hoofdzaak when a deelzaak is created] → The hoofdzaak shows the deelzaak
  under *Gerelateerde zaken*, and the deelzaak's history shows the hoofdzaak. Note this in the PR so the product owner
  can decide on a follow-up; it is the same for the existing *Bestaande zaak koppelen* flow.
- [Open Zaak writes no audit line on the current zaak when a gerelateerde zaak is created] → The current zaak shows
  the new zaak under *Gerelateerde zaken*, and the history of the new zaak shows the current zaak. This is the same as
  for a deelzaak and for the existing *Bestaande zaak koppelen* flow.
- [Nested `mat-sidenav-container` in the drawer] → Angular Material supports it; check the BAG picker manually in the
  drawer and cover it in the component spec.
