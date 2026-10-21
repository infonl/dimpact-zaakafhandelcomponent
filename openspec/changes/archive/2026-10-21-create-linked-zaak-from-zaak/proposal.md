## Why

A zaakbehandelaar who sees that a zaak needs a follow-up zaak or a deelzaak must now leave the zaak, start a new
zaak from the toolbar, fill in the initiator again, and come back to link the two zaken by hand. This takes many steps,
and the link is easy to forget. The expertisegroep calls the shortcut a must have (PZ-9841).

## What Changes

- The *Zaak koppelen* sidebar on the zaak detail page gets two tabs: *Bestaande zaak koppelen* (the current search and
  link form, unchanged) and *Nieuwe zaak aanmaken*.
- In *Nieuwe zaak aanmaken* the employee first picks the *Soort koppeling*: *Gerelateerde zaak* or *Deelzaak*. Then the
  standard *Zaak aanmaken* form shows in the sidebar.
- The zaaktypen in the form depend on the chosen relation. For *Deelzaak* only the deelzaaktypen of the current zaaktype
  show. For both relations only the zaaktypen the employee may start show.
- The initiator of the current zaak is copied to the new zaak.
- `POST /rest/zaken/zaak` accepts an optional zaak to link the new zaak to, with the relation type. ZAC checks the
  rights and the relation rules before it creates anything, and links the new zaak in the same call.
- A *Gerelateerde zaak* made this way is linked on both zaken, so both zaken show the relation and both histories show
  it. A *Deelzaak* is two-sided already, because the hoofdzaak shows its deelzaken.
- After the zaak is made, the sidebar closes, the current zaak reloads, and the new zaak shows under *Gerelateerde
  zaken*, from where the employee can open it.

## Capabilities

### New Capabilities

- `linked-zaak-creation`: creating a new zaak from an existing zaak, linked to it as gerelateerde zaak or deelzaak.

### Modified Capabilities

(none)

## Impact

- Backend: `ZaakRestService.createZaak`, `RestZaakAanmaakGegevens`, the link logic in `ZaakKoppelenRestService` and
  `ZaakLinkData`. The OpenAPI spec gets one optional field; existing clients are not affected.
- Frontend: `zaak-link` component (tabs), `zaak-create` component (usable in the sidebar), `zaak-view`, `nl.json` and
  `en.json`, and the generated types.
- Open Zaak: the new zaak is created with `hoofdzaak` or `gerelateerdeZaken` set; for a gerelateerde zaak the
  current zaak is patched as well. No new Open Zaak version is needed.
- Follow-up: PZ-11342 (an editable aanvrager for the new zaak) builds on this change.
