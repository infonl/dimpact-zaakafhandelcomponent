## Why

A zaakspecifiek geautoriseerde zaak always needs a behandelaar; the backend refuses to distribute one to a group
alone. The werkvoorraad verdelen dialog still lets the employee choose only a group, and afterwards tells them some
zaken were skipped. The zaak edit form lets them save a marked zaak without a behandelaar. Both give the impression
that this is allowed. [PZ-12793](https://dimpact.atlassian.net/browse/PZ-12793) makes the behandelaar required in
the user interface instead.

## What Changes

- **Verdelen dialog**: when the selection holds one or more zaakspecifiek geautoriseerde zaken, the *Medewerker*
  field is marked required, a notice explains why, and the dialog lists those zaken with the lock icon. *Verdelen*
  stays disabled until a medewerker is chosen.
- **Zaak edit form**: while *Zaak zaakspecifiek autoriseren* is ticked, the *Behandelaar* field is marked required
  with a notice. *Opslaan* stays disabled until a behandelaar is chosen.
- **The "not distributed" message after verdelen goes**: the dialog prevents that case. The message after
  vrijgeven stays.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `zaakspecifieke-autorisatie-activatie`: the verdelen dialog and the edit form require a behandelaar for a
  zaakspecifiek geautoriseerde zaak; verdelen no longer leaves such a zaak out. The outdated requirement that
  forbids handing such a zaak over is replaced by the handover behaviour that exists since PZ-10202.

## Impact

- **Frontend**: verdelen dialog, zaken werkvoorraad, zaak edit form, i18n texts.
- **Backend**: none. Its refusals stay as a safety net.
- **Docs**: gebruikershandleiding.
