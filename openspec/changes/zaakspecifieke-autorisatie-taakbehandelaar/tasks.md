## 1. Shared building blocks in `ZaakspecifiekeAutorisatieService`

- [x] 1.1 Move the per-zaak lock from `ZaakService.lockForZaak` into `ZaakspecifiekeAutorisatieService`, or a
      small shared bean. `ZaakService.assignZaak` must keep using the same lock instance.
- [x] 1.2 Add `grantZaakspecifiekeAutorisatieToTaakbehandelaar(zaak, medewerkerId): Boolean`. It returns `false`
      when the zaak is not marked or the medewerker is the zaakbehandelaar. Otherwise, under the lock, it calls the
      existing `grantZaakspecifiekeAutorisatie`, which checks the roltype and skips an existing holder, with the
      audit toelichting "Zaakspecifiek geautoriseerd medewerker van zaak {zaaknummer}". When a rol was added it
      reindexes. The callers write the taakhistorie entry (4.1).

## 2. REST assignment paths

- [x] 2.1 `TaskService.assignTaskToUser` first grants the zaakspecifieke autorisatie (1.2) to a new assignee. It
      writes the history entry (4.1) when a rol was added, and only then assigns in Flowable. Verdelen and toekennen
      (`assignTaskAndOptionallyReleaseFromAssignee`) set or release the behandelaar before changing the groep, the
      same order the zaak assignment uses (`ZaakService.changeBehandelaar` before `assignGroup`). So a refusal leaves both the groep and
      the behandelaar unchanged.
- [x] 2.2 `TaskRestService.completeTask` does not grant. The completer already has access, because completing
      requires the `wijzigen` right on the taak.
- [x] 2.3 In `PlanItemsRestService.doHumanTaskplanItem`, grant (1.2) to the selected medewerker before the
      opschorting, the mail and the creation of the taak, so that a missing roltype stops the request before
      anything happens.
- [x] 2.4 In bulk `TaskService.assignTasks`, catch `ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException`
      per taak: log a warning, send `ScreenEventType.TAAK.skipped(task)`, and continue. Single-taak paths let
      it propagate.
- [x] 2.4a Let `TaskService.assignOrReleaseTask` (single `PATCH /taken/toekennen`) assign the taak directly instead
      of through the bulk loop, so that the bulk catch of 2.4 does not swallow the missing roltype there.
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

## 4a. Frontend: message for skipped taken

- [x] 4a.1 In `taken-werkvoorraad.component.ts`, count the `SKIPPED` screen events on `TAAK` while verdelen runs.
      When the batch is finished and the count is not zero, show a snackbar in the style of
      `showSkippedZakenMessage` in the zakenwerkvoorraad: singular and plural text, `{{aantal}}`, 8 seconds.
- [x] 4a.2 Add the `msg.taken.verdelen.overgeslagen.enkelvoud` and `.meervoud` texts to `nl.json` and `en.json`.

## 5. Tests (separate phase: start only after the developer's explicit OK)

- [x] 5.1 `ZaakspecifiekeAutorisatieServiceTest`: unit tests for 1.2, covering:
      - the zaak is not marked;
      - the medewerker is the zaakbehandelaar;
      - the medewerker already holds the rol;
      - a rol is added, including the reindex;
      - the roltype is missing.
- [x] 5.2 `TaskServiceTest`:
      - the grant happens before the Flowable assignment;
      - an unchanged assignee writes nothing;
      - a zaak that is not marked writes nothing;
      - bulk verdelen with one skipped taak;
      - release writes nothing.
- [x] 5.3 `TaskRestServiceTest`: complete adds no rol and is not refused on a missing roltype. `PlanItemsRestServiceTest`: the start is
      refused on a zaaktype without the roltype.
- [x] 5.4 `PlanItemsRestServiceTest`: starting a taak with a medewerker grants; without a medewerker writes nothing.
      `CMMNServiceTest`: `readOpenTaskForPlanItem` finds the taak, or throws `TaskNotFoundException`.
- [x] 5.5 `ZaakspecifiekeAutorisatieServiceTest` for 3.2:
      - marking grants the rol to the assignees of the open taken;
      - it skips the zaakbehandelaar and existing holders;
      - groep-only taken add nothing.
- [x] 5.6 `RestTaskHistoryConverterTest`: renders the new history entry.
- [x] 5.7 Integration test (`TaskRestServiceTaakbehandelaarZaakspecifiekAutorisatieTest`) on a marked zaak. It uses
      BEHANDELAAR_1_EN_BRP_ZOEKER_2 and BEHANDELAAR_LONG_NAME_TEST, neither of whom holds the flag. BEHANDELAAR_2 has
      no rights on this zaaktype. The steps are:
      1. Start a taak with the first user. Check that the rol exists, that the user can read the zaak and the taak
         and finds the zaak, and that the zaakbehandelaar gets no rol.
      2. Check that the taakhistorie shows the line and that the betrokkenen do not list the user.
      3. Reassign the taak to the second user, release it, and assign it back. Check that there is exactly one rol
         per user and that the released user can still read the zaak.

      Completing is not tested here: it writes nothing, see the decision in design.md.
- [x] 5.8 *Dropped by the developer.* No separate BPMN integration test. Reassigning a BPMN user task goes through
      the same `TaskService` code as a CMMN taak, and 5.7 covers that.
- [x] 5.9 Integration test: mark a zaak that has an open taak assigned to BEHANDELAAR_1. Check that
      BEHANDELAAR_1 gets the rol and can still open the taak.
- [x] 5.10 *Dropped by the developer.* No integration test for a zaaktype without the roltype. The itest data has no
      such zaaktype, and the zaak handover (PZ-10202) has no such itest either. Each link is covered by unit tests:
      - `ZaakspecifiekeAutorisatieServiceTest` (the exception);
      - `TaskServiceTest` (bulk skip);
      - `PlanItemsRestServiceTest` (starting is refused);
      - `ZaakRestServiceTest` and `ZaakServiceTest` (zaak handover and verdelen);
      - `RestExceptionMapperTest` (HTTP 400 with the error code).
- [x] 5.11 Integration test: distribute the taken of three zaken from the takenwerkvoorraad (`PUT taken/lijst/verdelen`, as
      COORDINATOR_1) to one behandelaar. Only the third zaak is marked. Check that all three taken are assigned, and that
      only the marked zaak gets a *Zaakspecifiek geautoriseerde medewerker* rol for that behandelaar.
- [x] 5.12 Frontend spec `taken-werkvoorraad.component.spec.ts`: after verdelen, one `SKIPPED` taak shows the
      singular message, two show the plural message, and none shows no message.
- [x] 5.13 `TaskServiceTest`: `assignOrReleaseTask` on a zaak without the roltype throws the roltype exception,
      sends no `SKIPPED` event and leaves the taak unchanged.

## 6. Wrap-up

- [ ] 6.1 Ask the developer to run `./gradlew spotlessApply detektApply`, `./gradlew detekt`, the unit tests
      and the integration tests, and report back.
- [x] 6.2 Update `docs/solution-architecture/accessControlPolicies.md`: a taakbehandelaar of a marked zaak
      receives the *Zaakspecifiek geautoriseerde medewerker* rol on assignment and keeps it.
- [x] 6.3 Run `openspec validate zaakspecifieke-autorisatie-taakbehandelaar` and fix any findings.
