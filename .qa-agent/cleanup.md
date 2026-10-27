# Cleanup

ZAC can't delete a zaak, so abort ("afbreken") every open zaak whose omschrijving starts with "QA Agent:".
They then leave the werkvoorraden and the dashboard. Use the REST API with `page.request`, after logging
in as user 1 (`QA_USERNAME`).

1. Find them: `PUT /rest/zoeken/list` with the JSON body
   `{ "page": 0, "rows": 200, "type": "ZAAK", "zoeken": { "ZAAK_OMSCHRIJVING": "QA Agent:" }, "alleenOpenstaandeZaken": true }`.
   The zaken are in `resultaten`; keep those whose `omschrijving` starts with "QA Agent:" and whose
   `isAfgehandeld` is false. `id` is the zaak UUID, `identificatie` the zaaknummer.
2. For each zaak:
   - `GET /rest/zaken/zaak/<id>`; its `zaaktype.uuid` is needed next. Search results don't contain it.
   - `GET /rest/zaakafhandelparameters/zaakbeeindigredenen/<zaaktype uuid>` gives a list of
     `{ id, naam }`. Prefer a reason whose `id` isn't `ZAAK_NIET_ONTVANKELIJK`; otherwise take the first.
   - `PATCH /rest/zaken/zaak/<id>/afbreken` with the body `{ "zaakbeeindigRedenId": "<reason id>" }`.
3. Check again with the search from step 1. The search index updates a few seconds after an abort, so
   retry for up to a minute before you report zaken as still open.
