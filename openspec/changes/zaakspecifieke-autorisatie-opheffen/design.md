## Context

A zaak is zaakspecifiek geautoriseerd when it has the `ZAAK_GEAUTORISEERD` zaakeigenschap with value `true`.
Medewerkers get individual access through the behandelaar rol or a *Zaakspecifiek geautoriseerde medewerker*
(ZGM) rol. `ZaakspecifiekeAutorisatieService.shouldMarkZaakspecifiekGeautoriseerd` now refuses a request to
unmark a marked zaak with `ZaakspecifiekeAutorisatieCannotBeLiftedException`, and the edit form disables the
checkbox once the zaak is marked.

Everything that depends on the marking reads it live: `PolicyService` for the rechten, the werkvoorraad and the
release and handover guards. The search index holds a copy, so it needs a reindex, as when marking.

## Goals / Non-Goals

**Goals:** every requirement in `specs/zaakspecifieke-autorisatie-activatie/spec.md`.

**Non-Goals:** a reden in the confirmation dialog (the story drops the one in the Figma design); a separate
policy right for lifting.

## Decisions

### Lifting is part of the zaak update, like marking

The edit form sends `isZaakspecifiekGeautoriseerd: false`, as it sends `true` to mark. The update already
asserts `wijzigen` and requires a reden, so lifting needs no new endpoint. `shouldMarkZaakspecifiekGeautoriseerd`
no longer throws; a new `shouldLiftZaakspecifiekeAutorisatie` is true only for `false` on a marked zaak.

### Who may lift

Every employee with `wijzigen` on the zaak, as the story says ("iedereen die bij de zaak mag"). On a marked zaak
only the employees with access to it have `wijzigen`. Marking stays limited to the behandelaar and holders of the
`zaakspecifiek_geautoriseerd` application role.

### The rollen go first, the zaakeigenschap last

Under the per-zaak lock, ZAC deletes every ZGM rol of the zaak and then every `ZAAK_GEAUTORISEERD`
zaakeigenschap, each with the reden as audit toelichting; then it reindexes the zaak, its taken and its
documenten, and commits Solr. If a call fails halfway, the zaak stays marked and the employee can save again.
The other order could leave an unmarked zaak with ZGM rollen that would grant access again once the zaak is
marked anew, which the story rules out.

Deleting the eigenschap, rather than setting it to `false`, keeps the zaakregister as it was before the
marking, so marking again simply creates a new one.

### Lifting happens before a behandelaar change in the same update

The update lifts the marking before it assigns the zaak. A handover in the same save then gives the previous
behandelaar no ZGM rol that would be deleted at once, and releasing the zaak in the same save is allowed, since
the release guard reads the marking live.

### Confirmation without a reden

The edit form opens a dialog *Zaakspecifieke autorisatie opheffen* on submit when the checkbox went from ticked
to unticked, with the buttons *Opheffen* and *Sluiten*. *Sluiten* leaves the form open and saves nothing.

## Risks / Trade-offs

- [A medewerker is added, or a taak assigned, while the marking is lifted] → Adding a medewerker and granting a
  taakbehandelaar access read the rollen under the same per-zaak lock, so on one pod they either run before the
  lifting, and their rol is deleted, or after it. Granting a taakbehandelaar checks the marking before it takes
  the lock, so it can still add a ZGM rol right after the lifting; that rol grants nothing on an unmarked zaak,
  but it would return if the zaak were marked again. Across pods the lock does not serialise. Both need two
  employees acting on the same zaak at the same moment and are accepted, like the same trade-off for adding
  and ontkoppelen.
- [Reindexing fails after the lifting] → The request still succeeds and the failure is logged, as for marking.
  The rechten follow the zaakregister at once; only the search results lag until the next reindex.

## Migration Plan

None.

## Open Questions

None.
