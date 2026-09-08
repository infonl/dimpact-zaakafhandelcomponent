## Context

See proposal.md - Why. The relevant existing machinery:

- `ZrcClientService.isZaakspecifiekGeautoriseerd(zaakUUID)` (in `zrc/util/ZaakspecifiekGeautoriseerd.kt`)
  reads the `ZAAK_GEAUTORISEERD` zaakeigenschap. There is no write counterpart yet.
- `ZtcClientService.ZAAK_GEAUTORISEERD_EIGENSCHAP_NAAM` plus `findEigenschap(zaaktypeUrl, naam)` already back
  `RestZaaktypeConfiguration.zaakspecifiekAutoriseerbaar`.
- The three rego policies share one guard shape:
  ```rego
  default zaak_allowed := false
  zaak_allowed if { not zaak.zaakspecifiekGeautoriseerd }
  zaak_allowed if { zaakspecifiekGeautoriseerd.rol in user.rollen }
  ```
  Every permission rule already conjoins `zaak_allowed`, so a third disjunct propagates everywhere for free.
- `SearchService.getZaakspecifiekGeautoriseerdFilterQuery()` builds a *negative* Solr filter
  (`-(zaaktype:X AND zaakspecifiekGeautoriseerd:true OR ...)`) over the zaaktypen for which the user lacks
  the flag.
- `SolrSchemaV8` established the pattern for this epic: per-type field plus a `copyField` into a shared
  `ZoekObject`-level field, with `teHerindexerenZoekObjectTypes = emptySet()` and a manual reindex.
- `TaakZoekObjectConverter.convert(id, isZaakspecifiekGeautoriseerd)` already takes the per-zaak lookup as a
  function parameter so `IndexingService` can memoize it across the taken of one zaak.

## Goals / Non-Goals

**Goals:**

- One place decides "may this employee reach this zaakspecifiek geautoriseerde zaak" — the rego guard — with
  Solr filtering derived from the same rule rather than re-deriving it.
- The write path fails closed: any precondition that does not hold leaves the zaak completely untouched,
  including its zaakgegevens.
- No behandelaar can lock themselves out of a zaak by flagging it.

**Non-Goals:**

- Deactivation (PZ-12022), extra manually authorised employees (PZ-12023), reassignment with carry-over of
  the previous behandelaar (PZ-10202), and taakbehandelaars (PZ-12035). This change deliberately makes
  reassignment impossible rather than partially correct.
- An automated Solr reindex. As with `SolrSchemaV8`, the reindex is manual per environment.
- Frontend prevention of behandelaar changes on a flagged zaak; PZ-10200 explicitly scopes that check to the
  backend.

## Decisions

### The activation flag travels on the existing zaak update, not on a dedicated endpoint

`RestZaakCreateData` gains `isZaakspecifiekGeautoriseerd: Boolean?`, handled inside `updateZaak`.

*Why:* the Figma design puts the control in the zaakgegevens edit form, and the "must have a behandelaar"
precondition has to be evaluated against the *resulting* state — the same form can assign a behandelaar and
flag the zaak in one submit. A separate endpoint would need the frontend to sequence two calls and would
make that combined case racy.

*Alternative considered:* `PATCH /rest/zaken/zaak/{uuid}/zaakspecifieke-autorisatie`. Rejected for the
above; revisit if PZ-12022 and PZ-12023 turn this into a richer sub-resource.

Nullable, tri-state semantics: `null` means "not mentioned, leave alone", `true` means activate (idempotent
if already active), `false` on an already-flagged zaak is the rejected case. A non-nullable Boolean would
make every existing client that omits the field look like a deactivation request.

### Order of operations: patch the zaakgegevens first, create the zaakeigenschap last

There is no transaction across two Open Zaak calls. Doing `patchZaak` first and `createZaakeigenschap` last
means a failure of the second call leaves an edited-but-unflagged zaak, which the employee can simply retry.
The reverse order can leave a flagged zaak whose behandelaar was never assigned — i.e. a zaak nobody can
reach. All four preconditions are checked *before* either call.

### `zaak_allowed` gains an identity term, fed from a new policy input

```rego
zaak_allowed if { zaak.behandelaarIsLoggedInUser }
```

`ZaakData`, `TaakData` and `DocumentData` each gain a `behandelaarIsLoggedInUser: Boolean`, computed in
`PolicyService` from the zaak's behandelaar-medewerker rol (via the existing
`ZgwApiService.findBehandelaarMedewerkerRoleForZaak`) compared against the logged-in user's id.

*Why a pre-computed boolean rather than passing `zaak.behandelaarId` and comparing in rego:* the comparison
needs the same identity notion `ZaakService.assignZaak` uses, and keeping it in Kotlin keeps the rego free
of null-handling for a zaak without a behandelaar. It also matches how `zaakspecifiekGeautoriseerd` is
already pre-resolved rather than passed as raw zaakeigenschappen.

*Cost:* `readZaakRechten` may now need the zaak's rollen. `PolicyService` already calls
`isZaakspecifiekGeautoriseerd` (a `listZaakeigenschappen` call) per rechten evaluation, so this is a second
ZGW call on the same path. Short-circuit it: only resolve the behandelaar when the zaak is actually
zaakspecifiek geautoriseerd — for every other zaak the first disjunct already makes the guard true, so the
value is irrelevant.

### The zaak's behandelaar is denormalized onto all three zoekobjecten with a shared copyField

New `SolrSchemaV9`, mirroring `SolrSchemaV8` exactly: `zaak_zaakBehandelaarGebruikersnaam`,
`taak_zaakBehandelaarGebruikersnaam`, `informatieobject_zaakBehandelaarGebruikersnaam`, each `copyField`-ed
into a shared `zaakBehandelaarGebruikersnaam`. The filter query then becomes, per zaaktype without the flag:

```
-(zaaktype:X AND zaakspecifiekGeautoriseerd:true AND -zaakBehandelaarGebruikersnaam:"<user>")
```

*Why a separate field on `ZaakZoekObject` rather than reusing `zaak_behandelaarGebruikersnaam`:* the shared
`copyField` target needs one consistent name across all three types, and `TaakZoekObject` already uses
`taak_behandelaarGebruikersnaam` for the *taak's* behandelaar. Overloading that name across the two meanings
is exactly the kind of confusion that produces a silent authorization hole. The zaak-level field on
`ZaakZoekObject` is redundant with `zaak_behandelaarGebruikersnaam` but keeps the copyField uniform.

`TaakZoekObjectConverter` and `DocumentZoekObjectConverter` resolve the zaak's behandelaar through the same
memoized-lookup-function parameter that `isZaakspecifiekGeautoriseerd` already uses, so indexing all taken
of one zaak still costs one rollen lookup.

*Alternative considered:* leave the index alone and post-filter results in `SearchService`. Rejected: it
breaks paging and result counts, the exact reason PZ-11954 filtered in Solr in the first place.

### Recordmanagers and beheerders are handled by configuration, not by a policy branch

Per PZ-10203's own conclusion. The seed data in `scripts/docker-compose/imports/` maps the recordmanager and
beheerder functional roles to `zaakspecifiek_geautoriseerd` as well. This keeps `zaak_allowed` at three
disjuncts and keeps the permission matrix free of a second special case.

*Trade-off:* PZ-10200's acceptance criteria name recordmanagers and beheerders explicitly. If an environment
forgets the PABC mapping, they lose access to flagged zaken and the cause is invisible from ZAC. The
integration tests must therefore assert the seeded mapping, not merely the policy behaviour.

### Batch werkvoorraad operations skip flagged zaken, and the dialog names the reason

`assignFromList` and `releaseZakenFromList` run in a detached coroutine after the HTTP response has been
sent, so they cannot throw a user-visible error. `releaseZaken` already has the shape for this: it filters
out non-open zaken and emits `ScreenEventType.ZAAK_ROLLEN.skipped(zaak)`. Zaakspecifiek geautoriseerde zaken
join that filter — that skip is the authoritative guard and stays server-side.

*Why not reject the whole batch up front:* the selection is often large and mostly valid; refusing all of it
because one zaak is flagged is worse for the user than skipping that one. Single-zaak endpoints
(`assignZaak`, `assignZaakToLoggedInUser`, `assignZaakToLoggedInUserFromList`, `updateZaak`) are synchronous
and do throw a distinct `InputValidationFailedException` carrying its own `ErrorCode`.

### Refusals cross the wire as error codes, never as message text

Every new refusal gets an `ErrorCode` enum entry whose `value` is an i18n key (`msg.error.zaak....`), thrown
as an `InputValidationFailedException` and rendered by `RestExceptionMapper` as `{"message": "<key>"}`. The
frontend resolves the key against `nl.json`/`en.json`. This is the established ZAC contract —
`ErrorCode`'s own KDoc states "These should be translated by the frontend to human-readable messages" — so
the requirement is to follow it, not to invent anything.

*Consequence for the acceptance criteria:* PZ-10200 asks for "een foutmelding met een heldere
(Nederlandstalige) tekst". That Dutch text is authored in `nl.json`, not in Kotlin. A reviewer checking the
backend for Dutch strings is checking the wrong layer; the backend assertion is that the right *code* comes
back, and the frontend assertion is that the code resolves to the right sentence in both translation files.

**The verdelen and vrijgeven dialogs distinguish the two skip reasons** ("zaakspecifiek geautoriseerd"
versus "reeds afgehandeld") rather than showing one combined message. Note that today *neither* reason
reaches the user at all: `batch-process.service.ts` subscribes only to `Opcode.UPDATED`, and nothing in the
frontend inspects `Opcode.SKIPPED`, so a skipped zaak simply completes the batch unchanged and silently.

The distinction is made by **partitioning the selection in the frontend before dispatching the batch**,
not by adding a reason to the `SKIPPED` websocket payload. `zaken-werkvoorraad.component.ts` already
pre-filters this way for release (it drops zaken with no behandelaar from the selection), and the row model
already carries `afgehandeld`; it only needs `isZaakspecifiekGeautoriseerd` exposed on the werklijst row as
well — a value that is already indexed in Solr, so exposing it costs nothing but a field on
`RestZaakOverzicht`/the zoekobject REST model.

*Alternative considered:* extend `ScreenEventType.skipped(zaak)` to carry a reason and have the frontend
render it on arrival. Rejected: it changes the websocket contract for every consumer of `ZAAK_ROLLEN`, and
the message would arrive per-zaak and asynchronously, which is a poor fit for a single summary line in a
dialog the user has already dismissed.

*Trade-off — a stale index:* a zaak flagged since the last reindex looks distributable to the frontend, so
the backend skips it and the user gets no reason for that particular zaak. Activation reindexes directly
(see below), which makes this window small, and the outcome degrades to today's behaviour rather than to a
wrong message. The server-side skip is never bypassed, so this is a messaging gap, not an authorization one.

### The release and reassignment restrictions are scoped to the marking, not to the zaak

Both restrictions are expressed as "while the zaak is zaakspecifiek geautoriseerd", evaluated per request
against the zaak's current state, rather than as a permanent property acquired at activation. Confirmed with
the PO: once PZ-12022 lifts the marking, the zaak behaves like a normal zaak again and becomes releasable
and reassignable. Scoping the guard this way means PZ-12022 gets that behaviour for free and cannot forget
to unwind a latched flag.

### Reindex directly on activation rather than relying on the notificatie

`NotificationReceiver` already handles the `zaakeigenschap` notificatie (added by
`2026-09-01-zaakspecifieke-autorisatie-werklijsten-zoekresultaten`), so Open Zaak will eventually push the
change back. But the activating employee sees the zaak's own worklists immediately after the request, and
notificatie delivery is asynchronous and best-effort. Activation therefore triggers the same reindex fan-out
(zaak, its taken, its documenten) directly, and the notificatie path stays as the backstop for out-of-band
changes.

## Risks / Trade-offs

- **A flagged zaak whose behandelaar is removed out-of-band loses its identity-based access.** ZAC blocks
  release and reassignment, but nothing stops an administrator from deleting the behandelaar rol directly in
  Open Zaak — the exact practice PZ-10200's background section warns against. → **The recordmanager is the
  recovery path, and it works by construction:** because this change gives recordmanagers and beheerders the
  `zaakspecifiek_geautoriseerd` role through PABC (see the decision above), they keep access to every flagged
  zaak of the zaaktype whether or not it still has a behandelaar, and can assign a new one — which lifts the
  zaak back into the behandelaar exception. The zaak is therefore not unreachable in a correctly configured
  environment. The residual risk is narrow and specific: an environment where the PABC mapping is missing has
  no recordmanager access either, and a flagged zaak stripped of its behandelaar there really is reachable by
  nobody until the mapping is added. That makes the mapping a deployment prerequisite rather than a nicety,
  which is why it is asserted by integration test (task 6.5) rather than only documented.
- **Existing flagged zaken stay wrongly visible until the manual reindex runs.** Same accepted gap as
  `SolrSchemaV8`, now in the other direction: until `SolrSchemaV9`'s reindex runs, no row carries a
  zaak-behandelaar, so a behandelaar without the flag will *not* see their own flagged zaak in worklists even
  though they can open it. → Since the only way to create a flagged zaak from ZAC is this change itself, and
  activation reindexes directly, the gap affects only zaken flagged by hand in Open Zaak before deploy.
- **A recordmanager or beheerder without the PABC mapping loses access to flagged zaken entirely.** They may
  flag a zaak only if they hold the flag or are the behandelaar, so the "flag then immediately lose access"
  case cannot arise for them; the real failure is quieter — in an under-configured environment they simply
  cannot reach any flagged zaak, and nothing in ZAC explains why. Combined with the risk above, this is also
  what turns a behandelaar-less flagged zaak into an unreachable one. → Asserted by integration tests
  (task 6.5) covering both the configured and the unconfigured case, and called out in the deployment notes
  as a prerequisite.
- **Two ZGW reads per rechten evaluation on a flagged zaak.** `listZaakeigenschappen` plus `listRollen`. →
  Short-circuited to flagged zaken only, so unflagged zaken (the overwhelming majority) pay nothing extra.
- **`CsvService` reflects over every `ZoekObject` bean property**, so the new zaak-behandelaar field
  automatically gains a column in the zaken/taken/documenten CSV export and shifts every column after it,
  exactly as `zaakspecifiekGeautoriseerd` did. → Intended and consistent with the previous change; call it
  out in the release notes.

## Migration Plan

1. Deploy. `SolrSchemaV9` adds the fields without reindexing; the shared `zaakBehandelaarGebruikersnaam`
   field is simply absent on existing documents, which the negative filter query treats as "not the
   behandelaar" — fail-closed, never fail-open.
2. Update the PABC configuration on Docker Compose and the INFO test environment to map recordmanager and
   beheerder functional roles to `zaakspecifiek_geautoriseerd`.
3. Trigger the reindex manually, per environment, at a quiet moment — together with `SolrSchemaV8`'s still
   outstanding reindex if that has not yet been run there. **No new tooling is needed:**
   `IndexingAdminRestService` already exposes `GET /rest/internal/indexeren/herindexeren` for everything and
   `GET /rest/internal/indexeren/herindexeren/{type}` for a single `ZoekObjectType`. Both run asynchronously
   on `IndexingService`'s own coroutine scope and return `202 Accepted` (`409 Conflict` if a reindex of that
   type is already running). They are `@InternalEndpoint`s, so they are not reachable from the frontend and
   require the API key checked by `ZacApiKeyAuthFilter`. Reindex per type (`ZAAK`, then `TAAK`, then
   `DOCUMENT`) rather than all at once, so a long-running document reindex does not delay the zaak rows that
   matter most here.
4. Rollback: reverting the code leaves the two Solr fields in place (harmless, unread) and leaves any zaak
   already flagged in Open Zaak flagged. Access reverts to flag-holders only, which is stricter, not looser.

## Open Questions

None. Both questions raised while drafting this design have been answered by the PO and folded into the
Decisions above: the werkvoorraad dialogs distinguish the two skip reasons, and lifting the marking in
PZ-12022 makes the zaak releasable and reassignable again. The verdelen dialog's wording is not covered by
the Figma design (only the vrijgeven pop-up is), so its copy follows the vrijgeven pop-up's tone.
