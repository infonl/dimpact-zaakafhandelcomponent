## Context

A medewerker reaches a zaakspecifiek geautoriseerde zaak through one of two routes:

1. **The flag.** They hold `zaakspecifiek_geautoriseerd` for the zaaktype.
2. **Being in `ZaakToewijzing.geautoriseerdeMedewerkerIds`.** That set is the *Behandelaar* medewerker rol
   plus every *Zaakspecifiek geautoriseerde medewerker* (ZGM) rol on the zaak in Open Zaak.
   `ZaakspecifiekeAutorisatieService.readZaakToewijzing` builds it. Two consumers read it:
   - `PolicyService`, for `loggedInUserIsGeautoriseerdeMedewerker`;
   - `ReindexSupportService.zaakAutorisatieGegevens`, for the Solr field `zaakGeautoriseerdeMedewerkers`.

The zaak handover flow (PZ-10202) writes these rollen in `ZaakService.assignZaak` → `changeBehandelaar`:

```
assignZaak (lock per zaak)
  readZaakToewijzing ─────────────► Open Zaak: rollen + ZAAK_GEAUTORISEERD eigenschap
  changeBehandelaar
    if marked: grantZaakspecifiekeAutorisatie(previous) ─► POST rol ZGM, unless already held
    delete old Behandelaar rol, create new one
    delete a ZGM rol held by the new behandelaar   ("at most one of both rollen")
  reindex zaak + reindexZaakspecifiekeAutorisatieDependents (taken, documenten)
```

These are all the ways a taak gets a medewerker today:

| Path | Code | Language |
|---|---|---|
| Start a human task plan item with a medewerker | `PlanItemsRestService.doHumanTaskplanItem` → `ZacCreateHumanTaskInterceptor` | Kotlin |
| `PATCH taken/toekennen`, `PUT taken/lijst/verdelen` | `TaskService.assignTasks` → `assignTaskToUser` | Kotlin |
| `PATCH taken/toekennen/mij`, `PATCH taken/lijst/toekennen/mij` | `TaskRestService.assignLoggedInUserToTask` → `TaskService.assignTaskToUser` | Kotlin |
| `PATCH taken/complete` on an unassigned taak | `TaskRestService.completeTask` → `FlowableTaskService.assignTaskToUser` directly | Kotlin caller |

Other facts:
- **The taakhistorie comes from Flowable.** `RestTaskHistoryConverter` converts `HistoricTaskLogEntry`s. A
  rol in Open Zaak only shows in the zaakhistorie (the Open Zaak audittrail).
- **The betrokkenen tab already hides ZGM rollen**, because their `omschrijvingGeneriek` is `behandelaar`.

## Goals / Non-Goals

**Goals:**

- Every requirement in `specs/zaakspecifieke-autorisatie-taakbehandelaar/spec.md`, across every path in the
  table.
- Open Zaak stays the single source of who has access. No Flowable reads in the policies or the index, and
  no change to OPA, Solr or `ZaakToewijzing`.
- Reuse the zaak handover building blocks as they are:
  - `grantZaakspecifiekeAutorisatie` and its duplicate check;
  - the roltype lookup and its error code;
  - `reindexZaakspecifiekeAutorisatieDependents`.

**Non-Goals:**

- Merging the zaak and taak assignment flows into one shared flow. That is a follow-up refactoring.
- Changing any Java file. Converting `ZacCreateUserTaskInterceptor` and `FlowableTaskService` to Kotlin is a
  follow-up PR.
- Reporting skipped taken in the takenwerkvoorraad verdelen dialog. It is not in the acceptance criteria and
  belongs to a later iteration.
- Taken that end without being assigned or completed (zaak afbreken, BPMN timer or boundary event). These
  are to be discussed later. With this design they are already harmless: the assignee got the rol at
  assignment, so they keep access anyway.
- Deactivation (PZ-12022) and manually added medewerkers (PZ-12023).

## Decisions

### Grant the rol on assignment; never remove it because of a taak

*Decided by the developer, 2026-10-05.*

When a taak of a marked zaak gets a medewerker, ZAC calls `grantZaakspecifiekeAutorisatie` for that
medewerker. Reassignment, release and completion write nothing, because the rol is already there.

Consequences:
- **Flowable is not needed for access.** The policies and the Solr index stay unchanged.
- **The earlier "previous taakbehandelaar" logic disappears.** That includes recording on release and
  complete, the "last open taak" rule, the A → B → A removal and the *ontkoppeld* history line.

*Alternative considered and dropped:* keep the current taakbehandelaar in Flowable only, and write the rol
when they leave the taak. That needed Flowable reads in `PolicyService` and in the index converters, plus
writes on every exit path. Exits without a ZAC code path, such as zaak afbreken or a BPMN timer, would have
silently lost access.

### One function in `ZaakspecifiekeAutorisatieService` for all paths

`grantZaakspecifiekeAutorisatieToTaakbehandelaar(zaak, medewerkerId): Boolean` is the only new function in
that service. It moves the existing per-zaak lock out of `ZaakService`, so that zaak and taak assignment
share one lock. It:
- returns `false` when the zaak is not marked;
- returns `false` when the medewerker is the zaakbehandelaar;
- otherwise calls the existing `grantZaakspecifiekeAutorisatie`. That function checks the roltype, which
  gives the existing error code, and skips an existing holder;
- reindexes when a rol was added.

The callers write the taakhistorie entry through `TaskHistoryService` when it returns `true`, because only
they know the taak. There is no separate roltype assertion: granting first is the check.

### REST paths grant before the Flowable write

In `TaskService.assignTaskToUser`, when the assignee changes on a taak of a marked zaak, the steps are:
1. grant, which also checks the roltype;
2. assign in Flowable.

`TaskRestService.completeTask` calls `TaskService.grantZaakspecifiekeAutorisatieToNewAssignee` before its
implicit assignment. It does not go through `assignTaskToUser`, because that sends an asynchronous "taak op
naam" signalering that would read a taak that has already been completed. In bulk verdelen, the grant
happens before the groep changes, so a refusal leaves the taak unchanged. The skipped screen event uses the
existing `ScreenEventType.skipped(String)` with the taak id, so no Java file changes. `PlanItemsRestService` asserts the roltype before starting the plan item.

*Why grant first:* Open Zaak and Flowable share no transaction.
- Grant first: if Flowable then fails, the medewerker has access without the taak. That is harmless, and a
  retry is idempotent.
- Assign first: if the grant then fails, the taakbehandelaar cannot open their own taak.

### CMMN grants in `PlanItemsRestService`; BPMN model assignees need no hook

- **CMMN.** All five CMMN human tasks with `flowable:assignee="${initiator}"` are manual plan items, and ZAC
  never sets an `initiator` case variable. So a CMMN taak only gets an assignee when a medewerker is selected
  in "taak starten". `PlanItemsRestService` grants to the selected medewerker before the opschorting, the
  mail and the creation of the taak, so a missing roltype stops the request with the UI error before
  anything happens. After `startHumanTaskPlanItem`, when a rol was added, it looks the new taak up through
  `CMMNService.readOpenTaskForPlanItem`, only to write the history entry. `ZacCreateHumanTaskInterceptor` and `FlowableHelper` stay
  unchanged, and no Open Zaak call runs inside the Flowable engine.
- **BPMN.** *Decided by the developer, 2026-10-05: no hook.*
  - A BPMN process cannot assign a taak through a service task; `UpdateZaakAssignmentDelegate` only assigns
    the zaak.
  - The only other mechanism is the `flowable:assignee` attribute of a `userTask`. The BPMN guide
    (`docs/manuals/bpmn-guide/README.md:615-652`) documents two forms. Both resolve to someone who already
    has access:
    - `${var:get(zaakBehandelaar)}` gives the taak to the zaakbehandelaar. They need no ZGM rol. A zaakbehandelaar
      who has since been replaced already got the rol from the zaak flow.
    - `${taken:behandelaar('…')}` gives it to the behandelaar of another taak of the same zaak, who got the
      rol when that taak was assigned, or is the zaakbehandelaar.
  - Literal users and `${initiator}` only appear in itest BPMN files and are not used in production.
  - So `UserTaskCompletionListener` is not changed, and no Java registration is needed. Reassigning, verdelen,
    "toekennen aan mij" and completing a BPMN user task go through the same `TaskService` code as CMMN.

### Marking a zaak grants the rol to the assignees of its open taken

*Decided by the developer, 2026-10-05.* A taak may already be assigned when its zaak becomes zaakspecifiek
geautoriseerd. Without a rol, its taakbehandelaar would lose access to their own taak at that moment. So
`ZaakspecifiekeAutorisatieService.markZaakspecifiekGeautoriseerd` reads the open taken of the zaak through
`FlowableTaskService.listOpenTasksForZaak`. That is the only Flowable read in this change, it happens at
marking time only, and the Java class itself is not changed. For every distinct assignee it calls the grant
from above: it skips the zaakbehandelaar and existing holders, and writes the taakhistorie line on that taak
when a rol was added.

The roltype cannot be missing at this point. Marking already requires `isZaakspecifiekAutoriseerbaar`, which
checks that the roltype exists. The grants happen before the reindex that `markZaakspecifiekGeautoriseerd`
already does, so the index picks them up in the same pass.

### The zaakbehandelaar gets no ZGM rol through a taak

This follows the existing rule of the zaak handover flow, "a medewerker holds at most one of both rollen",
and the acceptance criterion "Behandelaren worden niet dubbel opgeslagen als Zaakspecifiek geautoriseerde
medewerker". If the zaakbehandelaar later loses the zaak, `changeBehandelaar` grants the rol then.

### Fail closed on a missing roltype for REST paths

Single requests fail with the existing
`ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER_ROLTYPE_NOT_FOUND`. Bulk `assignTasks` catches it per
taak, logs a warning, sends `ScreenEventType.TAAK.skipped`, and continues. This mirrors
`ZaakService.assignZaakFromBatch`. Release and complete write nothing, so they need no check.

### Taakhistorie gets its own Flowable entry

Add a custom `HistoricTaskLogEntry` type, `USER_TASK_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER_ADDED`. It stores
`ValueChangeData` with the medewerker's full name and the fixed toelichting. Write it from Kotlin through Flowable's task log entry
builder, without editing the Java `FlowableTaskService`.

`RestTaskHistoryConverter` renders it as:
- gegeven: *Zaakspecifiek geautoriseerde medewerker*;
- oude waarde: empty;
- nieuwe waarde: the medewerker's name;
- toelichting: "Zaakspecifiek geautoriseerd medewerker van zaak {zaaknummer}".

Neither the label nor the toelichting is translated. The label is the roltype omschrijving
(`ZgwApiService.ROLTYPE_OMSCHRIJVING_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER`). The zaakhistorie shows
exactly that text for the same rol, straight from the Open Zaak catalogus. No i18n key is added.

The audit toelichting passed to `createRol`, which shows in the zaakhistorie, is the same fixed text. The
rol's own `roltoelichting` stays the constant the zaak handover uses, so the rollen cannot be told apart.

## Risks / Trade-offs

- [The list of ZGM rollen grows with every taakbehandelaar and never shrinks] → Intended. Access is kept
  after release and completion. Removing access belongs to PZ-12023.
- [A grant succeeds but the Flowable assignment or CMMN creation fails] → The medewerker has access without
  the taak. Accepted; retries are idempotent.
- [Bulk verdelen of many taken of one marked zaak reindexes that zaak once per granted rol] → Accept it for
  now. It is only paid when a rol is actually added.

## Migration Plan

- No Solr, OPA or catalogus change.
- Taken of zaken that were already marked before deployment get no rol from the deployment. *Decided by the
  developer, 2026-10-05: no migration.* Their taakbehandelaars already lack access today, so nothing gets
  worse. They get the rol on the next assignment of the taak.
- Rollback: rollen that were created stay in Open Zaak and keep granting access through the existing rol
  route.

## Open Questions

Decided during the grilling of 2026-10-05:
- No BPMN assignee hook: both documented `flowable:assignee` forms resolve to someone who already has access
  (see Decisions). This makes the earlier question about BPMN error behaviour moot.
- **The zaakbehandelaar gets no ZGM rol when they become taakbehandelaar** (confirmed by the developer). They
  already have access through the *Behandelaar* rol.
- Rol audit toelichting and taakhistorie toelichting: "Zaakspecifiek geautoriseerd medewerker van zaak {zaaknummer}", not translated.
- Out of scope:
  - assignee validation on REST paths: no group or role check exists for taken (`TaskRestService.kt:178-179`).
    A raadpleger assigned via the API would get read access. Follow-up ticket;
  - multi-pod concurrency;
  - bulk performance.

None remaining.
