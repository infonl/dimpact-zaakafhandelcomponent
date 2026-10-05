## Why

Since [PZ-10200](https://dimpact.atlassian.net/browse/PZ-10200) and
[PZ-10202](https://dimpact.atlassian.net/browse/PZ-10202), a zaakspecifiek geautoriseerde zaak can only be
reached by three groups:
- employees holding `zaakspecifiek_geautoriseerd` for the zaaktype;
- its current zaakbehandelaar;
- medewerkers holding the *Zaakspecifiek geautoriseerde medewerker* rol, which a previous zaakbehandelaar
  receives on handover.

A **taakbehandelaar** belongs to none of these. So an employee who gets a taak of such a zaak cannot open
that taak or its zaak, and cannot find either of them in a werkvoorraad or in zoekresultaten.

[PZ-12035](https://dimpact.atlassian.net/browse/PZ-12035) closes that gap. Whoever is, or has been,
taakbehandelaar of a taak of a zaakspecifiek geautoriseerde zaak gets access to that zaak and keeps it.

## What Changes

- **A taakbehandelaar gets the *Zaakspecifiek geautoriseerde medewerker* rol as soon as they are assigned.**
  When a taak of a zaakspecifiek geautoriseerde zaak gets a medewerker, ZAC adds that rol for the medewerker
  to the zaak in Open Zaak. Open Zaak is the only record of who has access. Policies and the search index
  keep reading it exactly as they do today, and they do not read Flowable.
- **Every way a medewerker assigns a taak is covered**, for CMMN human tasks and BPMN user tasks alike:
  - starting a human task plan item with a medewerker;
  - toekennen and toekennen aan mij, from the taakdetailpagina and from the takenwerkvoorraad;
  - verdelen from the takenwerkvoorraad;
  - the implicit assignment to the logged-in user when an unassigned taak is completed.
- **Reassigning, releasing or completing removes nothing.** The previous taakbehandelaar already has the rol
  and keeps it. ZAC never removes a *Zaakspecifiek geautoriseerde medewerker* rol because of a taak.
- **No duplicate rollen.** A medewerker who already holds the rol does not get a second one. This applies
  whether the rol came from an earlier taak, a previous zaakbehandeling or a manual addition. The rollen
  cannot be told apart. The current zaakbehandelaar does not get the rol either, because their *Behandelaar*
  rol already gives access. That is the existing rule of the zaak handover flow.
- **Taakbehandelaars are not betrokkenen.** The rol has `omschrijvingGeneriek = behandelaar`, which the
  betrokkenen tab already filters out.
- **The taakhistorie shows the new rol.** Next to the existing *behandelaar* line, a *Zaakspecifiek
  geautoriseerde medewerker* line appears. Its toelichting says the medewerker was granted zaakspecifieke
  autorisatie for the zaak.
- **Werkvoorraden and zoekresultaten follow** through the existing reindex after a rol is added.
- **Assignment fails closed when the zaaktype lacks the roltype.** The employee gets the existing
  `ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER_ROLTYPE_NOT_FOUND` message. Bulk verdelen skips such a
  taak.
- **BPMN models need nothing extra.** The BPMN guide documents two ways to set a user task's assignee:
  - `${var:get(zaakBehandelaar)}`, which gives the taak to the zaakbehandelaar;
  - `${taken:behandelaar('…')}`, which gives it to the behandelaar of another taak of the same zaak.

  Both resolve to someone who already has access. BPMN processes cannot assign a taak in any other way;
  `UpdateZaakAssignmentDelegate` only assigns the zaak.
- **Zaken that are not zaakspecifiek geautoriseerd are unaffected.**

## Capabilities

### New Capabilities

- `zaakspecifieke-autorisatie-taakbehandelaar`: the *Zaakspecifiek geautoriseerde medewerker* rol a
  taakbehandelaar of a zaakspecifiek geautoriseerde zaak receives on assignment and keeps. It covers:
  - all assignment paths for CMMN and BPMN;
  - no duplicates;
  - the betrokkenen tab, the taakhistorie, werkvoorraden and zoekresultaten;
  - the fail-closed behaviour.

### Modified Capabilities

None. The rol already feeds the OPA policies (`zaakspecifieke-autorisatie-toegang`) and the search index
(`zaakspecifiek-geautoriseerde-zoekindex`) through `ZaakToewijzing.geautoriseerdeMedewerkerIds`.

## Impact

- **Backend**:
  - `ZaakspecifiekeAutorisatieService`, whose existing `grantZaakspecifiekeAutorisatie`, roltype check and
    reindexing are reused.
  - `TaskService` and `TaskRestService`: toekennen, verdelen, and the implicit assignment in complete.
  - `PlanItemsRestService` and `CMMNService`: the fail-closed check before starting a taak, and the grant to
    the selected medewerker after it is started.
  - `RestTaskHistoryConverter`, plus a new Flowable history entry.
- **No Java file is changed.** Converting `ZacCreateUserTaskInterceptor` and `FlowableTaskService` to
  Kotlin is a follow-up PR.
- **Open Zaak**: one *Zaakspecifiek geautoriseerde medewerker* rol per medewerker who has ever been
  taakbehandelaar of a marked zaak. No catalogus change is needed beyond what PZ-10202 requires.
- **Solr and OPA**: no change.
- **Frontend**: no change. The new history line uses the roltype omschrijving as its label, like the
  zaakhistorie.
- **Out of scope**:
  - refactoring the zaak and taak assignment flows into one shared flow;
  - reporting skipped taken in the takenwerkvoorraad dialogs (later iteration);
  - taken that end without being assigned or completed (to be discussed later);
  - deactivation (PZ-12022) and manually added medewerkers (PZ-12023).
