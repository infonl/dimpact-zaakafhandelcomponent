## Context

The zaken werkvoorraad already knows per zaak whether it is zaakspecifiek geautoriseerd, and so does the zaak
edit form. The backend refuses to release a marked zaak or to mark a zaak without a behandelaar, so the change
is frontend only.

## Goals / Non-Goals

**Goals:** every requirement in `specs/zaakspecifieke-autorisatie-activatie/spec.md`.

**Non-Goals:** a new design pattern for notices; changing the backend refusals.

## Decisions

### The notice is a hint under the field

The story asks for a notice that a behandelaar is required. Both screens show it as a hint under the
behandelaar field, the pattern the edit form already uses under the *Zaak zaakspecifiek autoriseren* checkbox.
No Figma design exists for this story.

### The edit form follows the checkbox, not the stored marking

The behandelaar is required while the checkbox is ticked. Ticking it on an unmarked zaak needs a behandelaar
too, as the backend requires; unticking it lifts the requirement.

### The werkvoorraad no longer counts skipped zaken for verdelen

With a medewerker required, verdelen cannot leave a zaakspecifiek geautoriseerde zaak out, so the werkvoorraad
drops that message and its texts. Vrijgeven keeps its message.

The backend still skips a handover when the zaaktype lacks the *Zaakspecifiek geautoriseerde medewerker*
roltype. The dropped message never covered that case (it only counted zaken without a chosen medewerker), so no
feedback is lost. The inrichtingscheck already reports the missing roltype.
