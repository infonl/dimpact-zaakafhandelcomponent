## 1. Backend

- [x] 1.1 Extract the single-document delete and link logic into functions reused by the single and the bulk path, without changing the existing endpoints' behaviour.
- [x] 1.2 Add `RestDetachedDocumentBulkRequest`/`RestDetachedDocumentBulkLinkRequest` and `RestDetachedDocumentBulkResult` (own files, validated, max 100 ids).
- [x] 1.3 Add `PUT /rest/ontkoppeldedocumenten/lijst/verwijderen` and `PUT /rest/ontkoppeldedocumenten/lijst/koppelen`: continue on failure, return per-document result with `foutcode`, enforce lock rule and policies.
- [x] 1.4 Regenerate the OpenAPI spec and frontend types.
- [x] 1.5 Unit tests (all succeed, one fails and others proceed, locked, already linked, missing in Open Zaak, no right, too many ids).
- [x] 1.6 Integration test: bulk delete and bulk link with one failing document.

## 2. Frontend

- [x] 2.1 `OntkoppeldeDocumentenService`: mutations for both bulk endpoints that invalidate the list query.
- [x] 2.2 List component: `select` column, master toggle, selection cleared on page, sort and filter change.
- [x] 2.3 *Verwijderen* and *Koppelen* buttons with badges; confirm dialog; bulk mode in `InformatieObjectLinkComponent`.
- [x] 2.4 Snackbar for the result and a dialog listing failed documents.
- [x] 2.5 Translations in `nl.json` and `en.json`.
- [x] 2.6 Specs for the component, service and link component; an e2e step if the list is covered today.

## 3. Wrap-up

- [x] 3.1 Update the gebruikershandleiding.
- [x] 3.2 Run the linters and tests.
