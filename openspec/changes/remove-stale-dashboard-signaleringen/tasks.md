## 1. Deletion helper

- [x] 1.1 In `SignaleringService`, extract the bulk delete by id and the grouped screen events of
  `deleteSignaleringen` into a private function that takes a list of signaleringen; verify the existing
  `SignaleringServiceTest` cases for `deleteSignaleringen` still pass
- [x] 1.2 Add a `SignaleringService` function that deletes the user signaleringen of given types for a zaak or
  taak subject whose target differs from a given user (all of them when the user is `null`), and returns the
  number deleted; verify with `SignaleringServiceTest` cases for "other user deleted", "kept user kept",
  "group target kept" and "null deletes all user signaleringen"

## 2. Cleanup on ROL DESTROY

- [x] 2.1 In `NotificationReceiver.handleSignaleringen`, on a `zaken` ROL DESTROY notification, call a new
  `SignaleringService.deleteZaakSignaleringenOfFormerBehandelaren` that reads the zaak, finds its behandelaar
  medewerker and deletes the `ZAAK_OP_NAAM` and `ZAAK_DOCUMENT_TOEGEVOEGD` signaleringen of other users through
  1.2, logging the count at `INFO`; verify with `SignaleringServiceTest` cases for "reassigned" and "released"
  (the current behandelaar is kept, so deleting a non-behandelaar rol keeps it too) and a `NotificationReceiverTest`
  case for the delegation
- [x] 2.2 Skip the cleanup at `FINE` when the zaak returns 404, without breaking the rest of the notification
  handling; verify with a `NotificationReceiverTest` case
- [x] 2.3 Add an integration test that gives user A a `ZAAK_OP_NAAM` signalering for a zaak, removes A as
  behandelaar through the ZAC API, sends the ROL DESTROY notification and asserts that A has no signalering left
  (`/signaleringen/latest`) and that A's dashboard listing no longer contains the zaak. The test does not assign
  the zaak to a second user, because the only other user of the group is `BEHANDELAAR_1`, whose signaleringen
  `SignaleringRestServiceTest` counts; verify with `./gradlew itest --tests` on the new test

## 3. Cleanup on taak assign and release

- [x] 3.1 In `TaskService.assignTask` and `releaseTask`, delete the `TAAK_OP_NAAM` signaleringen of the taak of
  every user other than the new assignee through 1.2 (all on release); verify with `TaskServiceTest` cases for
  "reassigned to another user", "released" and "assigned to the same user"

## 4. Validation in the zaken listing

- [x] 4.1 Validate each zaak signalering in `SignaleringService.listZakenSignaleringenPage`: keep it only when
  the logged-in user is the behandelaar of the zaak and the zaak rechten give `canLezen`, evaluating the rechten
  once per zaak (replace `RestZaakOverzichtConverter.convertForDisplay` by `convertForDisplayIfReadable`, which
  returns `null` when the user cannot read the zaak); verify with `SignaleringServiceTest` cases for "other behandelaar", "no read rights" and
  "valid"
- [x] 4.2 Delete the invalid signaleringen by id and log a `WARNING` per row with signalering id, type, zaak
  UUID and reason, without the username; verify the deletion and that no username is in the message in
  `SignaleringServiceTest`
- [x] 4.3 Validate the page once on the `SIGNALERING_TIJDSTIP` path and all signaleringen before sorting and
  slicing on the in-memory sort path; verify with `SignaleringServiceTest` cases on both paths that the stale
  rows are left out of the result and that their deletion sends a `SIGNALERINGEN` screen event
- [x] 4.4 In `SignaleringRestService.listZakenSignaleringen`, keep the `page > maxPages` check on the count
  before listing and return a count made after listing; verify with a `SignaleringRestServiceTest` case where
  the returned total excludes the rows deleted while listing

## 5. Validation in the taken listing

- [x] 5.1 Validate each taak signalering in `SignaleringService.listTakenSignaleringenPage`: keep it only when
  the logged-in user is the assignee and `policyService.readTaakRechten(task).canLezen` is true; delete the rest
  by id and log a `WARNING` per row with signalering id, type, taak ID and reason, without the username; verify
  with `SignaleringServiceTest` cases for "other assignee", "unassigned", "no read rights" and "valid"

## 6. Mijn binnenkort verlopende zaken

- [x] 6.1 In `ZaakRestService.listZaakWarnings`, keep only zaken whose behandelaar medewerker rol belongs to the
  logged-in user and whose zaak rechten give `canLezen`, evaluating the rechten once per zaak; verify with
  `ZaakRestServiceTest` cases for "only zaakspecifiek geautoriseerde medewerker rol", "behandelaar without read
  rights" and "behandelaar with read rights"

## 7. Finish

- [x] 7.1 Update `docs/development/logging.md` if it lists the log statements of `SignaleringService` or
  `NotificationReceiver`; verify the new warnings are described there, or that the file has no such list
- [x] 7.2 Run `./gradlew spotlessApply detektApply`, then `./gradlew detekt` and
  `./gradlew test --tests "nl.info.zac.signalering.*" --tests "nl.info.zac.notification.*" --tests "nl.info.zac.task.*"
  --tests "nl.info.zac.app.signalering.*" --tests "nl.info.zac.app.zaak.ZaakRestServiceTest"`; verify all pass
- [x] 7.3 Run `openspec validate remove-stale-dashboard-signaleringen --strict`; verify it reports no errors
