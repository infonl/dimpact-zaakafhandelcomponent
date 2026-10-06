## 1. Shared building blocks in `ZaakspecifiekeAutorisatieService`

- [x] 1.1 Move the per-zaak lock from `ZaakService.lockForZaak` into `ZaakspecifiekeAutorisatieService`, or a
      small shared bean. `ZaakService.assignZaak` must keep using the same lock instance.
- [x] 1.2 Add `grantZaakspecifiekeAutorisatieToTaakbehandelaar(zaak, medewerkerId): Boolean`. It returns `false`
      when the zaak is not marked or the medewerker is the zaakbehandelaar. Otherwise, under the lock, it calls the
      existing `grantZaakspecifiekeAutorisatie`, which checks the roltype and skips an existing holder, with the
      audit toelichting "Zaakspecifiek geautoriseerd medewerker van zaak {zaaknummer}". When a rol was added it
      reindexes. The callers write the taakhistorie entry (4.1).

## 2. REST assignment paths

- [x] 2.1 Add `TaskService.grantZaakspecifiekeAutorisatieToNewAssignee(task, assignee)`. When the assignee changes,
      it grants (1.2) and writes the history entry (4.1). Every assignment path calls it before writing to Flowable.
      In bulk verdelen it is called before the groep changes, so a refusal leaves the taak unchanged.
- [x] 2.2 `TaskRestService.completeTask` does not grant. The completer already has access, because completing
      requires the `wijzigen` right on the taak.
- [x] 2.3 In `PlanItemsRestService.doHumanTaskplanItem`, grant (1.2) to the selected medewerker before the
      opschorting, the mail and the creation of the taak, so that a missing roltype stops the request before
      anything happens.
- [x] 2.4 In bulk `TaskService.assignTasks`, catch `ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException`
      per taak: log a warning, send `ScreenEventType.TAAK.skipped(task)`, and continue. Single-taak paths let
      it propagate.
- [x] 2.5 Add a short list of the entry points to the `TaskService` KDoc.

## 3. Taak creation and marking

- [x] 3.1 After `cmmnService.startHumanTaskPlanItem`, when a rol was added, look the new taak up with
      `CMMNService.readOpenTaskForPlanItem` and write the history entry (4.1). `ZacCreateHumanTaskInterceptor` and
      `FlowableHelper` stay unchanged.
- [x] 3.2 In `ZaakspecifiekeAutorisatieService.markZaakspecifiekGeautoriseerd`, before the existing reindex, grant
      (1.2) to every distinct assignee of the open taken (`FlowableTaskService.listOpenTasksForZaak`, used read-only),
      and write the history entry on that taak when a rol was added.

## 4. Taakhistorie

- [x] 4.1 Add `TaskHistoryService.addZaakspecifiekGeautoriseerdeMedewerkerAddedEntry(task, zaak, medewerkerId)`. It writes
      the custom type `USER_TASK_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER_ADDED` through Flowable's task log entry
      builder, as `ValueChangeData` with the medewerker's full name and the fixed toelichting. The Java
      `FlowableTaskService` is not edited.
- [x] 4.2 Extend `RestTaskHistoryConverter` to render it:
      - gegeven: *Zaakspecifiek geautoriseerde medewerker*;
      - oude waarde: empty;
      - nieuwe waarde: the medewerker's full name;
      - toelichting: the fixed, untranslated text "Zaakspecifiek geautoriseerd medewerker van zaak {zaaknummer}".
- [x] 4.3 Use `ZgwApiService.ROLTYPE_OMSCHRIJVING_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER` as the gegeven label, the
      same text the zaakhistorie shows for this rol. Add no i18n key: the taakhistorie component's `translate` pipe
      shows an unknown key as-is.

## 5. Tests (separate phase: start only after the developer's explicit OK)

- [ ] 5.1 `ZaakspecifiekeAutorisatieServiceTest`: unit tests for 1.2, covering:
      - the zaak is not marked;
      - the medewerker is the zaakbehandelaar;
      - the medewerker already holds the rol;
      - a rol is added, including the reindex;
      - the roltype is missing.
- [ ] 5.2 `TaskServiceTest`:
      - the grant happens before the Flowable assignment;
      - an unchanged assignee writes nothing;
      - a zaak that is not marked writes nothing;
      - bulk verdelen with one skipped taak;
      - release writes nothing.
- [ ] 5.3 `TaskRestServiceTest`: complete adds no rol and is not refused on a missing roltype. `PlanItemsRestServiceTest`: the start is
      refused on a zaaktype without the roltype.
- [ ] 5.4 `PlanItemsRestServiceTest`: starting a taak with a medewerker grants; without a medewerker writes nothing.
      `CMMNServiceTest`: `readOpenTaskForPlanItem` finds the taak, or throws `TaskNotFoundException`.
- [ ] 5.5 `ZaakspecifiekeAutorisatieServiceTest` for 3.2:
      - marking grants the rol to the assignees of the open taken;
      - it skips the zaakbehandelaar and existing holders;
      - groep-only taken add nothing.
- [ ] 5.6 `RestTaskHistoryConverterTest`: renders the new history entry.
- [ ] 5.7 Integration test on a marked zaak with BEHANDELAAR_1 and BEHANDELAAR_2, neither of whom holds the
      flag. The steps are:
      1. Start a taak with BEHANDELAAR_1. Check that the rol exists, that BEHANDELAAR_1 can read and edit the
         zaak, and that BEHANDELAAR_1 finds it in the zoekresultaten.
      2. Reassign the taak to BEHANDELAAR_2. Check that both users hold the rol, and that the taakhistorie
         shows the line.
      3. Release the taak, then complete a taak. Check that both users still hold the rol.
      4. Reassign the taak back to BEHANDELAAR_1. Check that there is still exactly one rol per user.
      5. Check that neither user appears in the betrokkenen tab.
- [ ] 5.8 Integration test: reassign a BPMN user task of a marked zaak through `taken/toekennen` and check that the
      new assignee gets the rol.
- [ ] 5.9 Integration test: mark a zaak that has an open taak assigned to BEHANDELAAR_1. Check that
      BEHANDELAAR_1 gets the rol and can still open the taak.
- [ ] 5.10 Integration test on a marked zaak whose zaaktype lacks the roltype:
      - assigning a single taak is refused with the error code;
      - a bulk verdelen skips that taak and assigns the rest.

## 6. Wrap-up

- [ ] 6.1 Ask the developer to run `./gradlew spotlessApply detektApply`, `./gradlew detekt`, the unit tests
      and the integration tests, and report back.
- [x] 6.2 Update `docs/solution-architecture/accessControlPolicies.md`: a taakbehandelaar of a marked zaak
      receives the *Zaakspecifiek geautoriseerde medewerker* rol on assignment and keeps it.
- [x] 6.3 Run `openspec validate zaakspecifieke-autorisatie-taakbehandelaar` and fix any findings.
