## Why

[PZ-10331](https://dimpact.atlassian.net/browse/PZ-10331): recordmanagers (request of gemeente Groningen) clean
up the *Ontkoppelde documenten* list in the Inbox. Today they delete or link each document one by one, which is
too much work for larger numbers. The case work queue already lets an employee select several zaken and
*Verdelen* or *Vrijgeven* them in bulk; the same pattern should apply here.

## What Changes

- **Selection on the *Ontkoppelde documenten* list**: a checkbox per row and a select-all checkbox in the header
  that selects all documents on the current page, as on the zaken werkvoorraad.
- **Bulk actions** *Verwijderen* and *Koppelen* appear as buttons with a count badge as soon as a document is
  selected. *Verwijderen* asks "Weet u dit zeker?"; *Koppelen* opens the existing *selecteer zaak* panel.
- **Two bulk REST endpoints**: `PUT /rest/ontkoppeldedocumenten/lijst/verwijderen` and
  `PUT /rest/ontkoppeldedocumenten/lijst/koppelen`. Each processes the documents one by one, continues after a
  failure and returns per document whether it succeeded or failed.
- **Failures are shown to the employee** after the action, per document; the other documents are still
  deleted or linked.
- **The list updates from the response**: a document counts as deleted or linked once Open Zaak confirmed it
  and ZAC removed it from its own table of ontkoppelde documenten; the list refreshes without waiting for
  notifications (ADR 0008). The notifications that follow are processed as before and do not update the list
  again.

## Capabilities

### New Capabilities
- `ontkoppelde-documenten-bulk`: selecting ontkoppelde documenten and deleting or linking them in bulk.

### Modified Capabilities

None.

## Impact

- Backend: `DetachedDocumentRestService`, `DetachedDocumentService`, new REST models for the bulk request and
  result; OpenAPI spec regenerated.
- Frontend: `OntkoppeldeDocumentenListComponent` and its service, `InformatieObjectLinkComponent` (bulk mode),
  translations.
- Test data scripts in `scripts/test-data/` do not call these endpoints; no update expected.
- No policy change: the existing `ontkoppelde_documenten_verwijderen` right and the document and zaak rights
  used by the single-document endpoints apply per document.
