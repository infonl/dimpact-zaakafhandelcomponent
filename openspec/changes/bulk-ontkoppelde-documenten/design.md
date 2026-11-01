## Context

`OntkoppeldeDocumentenListComponent` is a plain `mat-table` with per-row delete (`DELETE
/rest/ontkoppeldedocumenten/{id}`) and link (`zac-informatie-object-link` drawer, `POST
/rest/informatieobjecten/informatieobject/verplaats` with `bron = ontkoppelde-documenten`). The zaken werkvoorraad
has the pattern to copy: a `select` column with `SelectionModel`, `masterToggle`, `checkboxLabel`, and bottom
buttons (`verdelen_button`, `vrijgeven_button`) with `matBadge` counts. ADR
[0008](../../../docs/solution-architecture/decisions/0008-update-the-ui-without-waiting-for-notifications-on-zac-initiated-changes.md)
(accepted) says ZAC-initiated changes update the UI on the external system's success response and report failed
items when the batch ends. This change follows it and does not conflict with any accepted ADR.

## Goals / Non-Goals

**Goals:** every requirement in `specs/ontkoppelde-documenten-bulk/spec.md`, reusing the existing UX pattern and
the single-document checks.

**Non-Goals:** selecting across pages, bulk actions on the *Inbox documenten* list, a date-range "ontkoppeld
van-tot" selection beyond the existing filters, progress through websocket screen events.

## Design

No Figma design is linked in the item. The UX is copied from the zaken werkvoorraad:

- **Table**: a first column with a header checkbox (`selectAll_checkbox`, aria-label from `actie.alles.selecteren`
  / `actie.alles.deselecteren`, indeterminate for a partial selection) and a checkbox per row. The filter row has
  an empty cell above it (`select_filter`). The column is not shown without the right to delete or link.
- **Actions**: a bar below the list shows, only while at least one document is selected, the buttons
  *Verwijderen* (`delete` icon, id `verwijderen_button`) and *Koppelen* (`link` icon, id `koppelen_button`), each
  with a badge of the selected count. Both are disabled while an action runs.
- **Verwijderen**: the existing confirm dialog with the text "Weet u dit zeker?" and the number of documents.
- **Koppelen**: the existing right-hand panel *selecteer zaak*, with an intro naming the number of documents.
- **Loading**: the table shows the existing loading shade while the action runs. **Empty**: unchanged.
- **Result**: a snackbar with the number processed. When some failed, a dialog lists each failed document with
  its reason; the failed documents remain in the list.
- Per-row delete and link icons stay as they are.

## Decisions

### Synchronous bulk endpoints with a per-document result

Both endpoints loop over the ids on the request thread and return `RestDetachedDocumentBulkResult` with
`verwerkt` (ids) and `mislukt` (id, titel, `foutcode`). They do not run asynchronously and do not send screen
events, because a selection is at most one page (the page size options of the list) and the ZGW calls per
document are few. The frontend invalidates the list query on the response. This is simpler than the zaken flow
and satisfies ADR 0008 without websocket events.

### Reuse the single-document logic per document

The delete and link logic is moved from the REST methods into `DetachedDocumentService` (or kept in the REST
service as private functions) and called by both the single and the bulk endpoints, so the checks are identical:
a document missing in Open Zaak is only removed from the table, a document already linked to a zaak fails, the
policy `canOntkoppeldeDocumentenVerwijderen` is asserted once for the request, and for linking
`canVerplaatsen` on the document and `canWijzigen` on the target zaak are checked per document. The existing
`isVergrendeld` rule (no delete for a locked document) is enforced per document in the backend as well.

### Notifications do not touch the list

The ontkoppelde-documenten list is not driven by screen events today, so no suppression is needed; the
notification handlers are unchanged.

## Risks / Trade-offs

- A long request for a large page → mitigated by a bounded page size; a request with more than 100 ids is refused
  with 400.
- A partial failure leaves the selection in an intermediate state → the failed documents stay in the list and
  the selection is cleared.

## Assumptions

- No design was linked; the UX, labels and positions follow the zaken werkvoorraad, as asked by the team.
- Selection is per page only (matches the "alle documenten op de pagina" criterion).
- "Selecteren van-tot" from the story is covered by the existing filter on *ontkoppeld op* plus select-all.
- *Koppelen* searches zaken by the informatieobjecttype of the first selected document (the search API needs
  one type); documents whose type is not allowed on the chosen zaak fail and are reported.
- The same right (`ontkoppelde_documenten_verwijderen`) gates the whole selection column for deleting; linking
  uses the existing per-document policies, so no new OPA rule is added.
- Maximum 100 ids per request.
- New texts are added in `nl.json` and `en.json`; Dutch wording is chosen to match existing messages.
