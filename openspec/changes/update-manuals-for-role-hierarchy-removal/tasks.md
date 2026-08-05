## 1. Update the user manual role descriptions

- [x] 1.1 In `docs/manuals/ZAC-gebruikershandleiding/ZAC-gebruikershandleiding.md`, correct the intro sentence of "Rollen en rechten" (currently "Er wordt momenteel gewerkt met vier rollen...") to reflect the actual 5 application roles described below it.
- [x] 1.2 Rewrite the `Coördinator` description to explicitly mention its document rights (lezen, downloaden) and notitie rights (lezen, wijzigen), cross-checked against `document-rechten.rego` and `notitie-rechten.rego`, instead of relying on the reader to infer them.
- [x] 1.3 Rewrite the `Recordmanager` description to state its rights directly (zaken/taken raadplegen, zaak heropenen, document rechten zoals wijzigen/verwijderen/ontgrendelen/nieuwe versie toevoegen/verplaatsen/ontkoppelen/downloaden/converteren) without the "aanvullende rechten" (additional-to-raadpleger) framing.
- [x] 1.4 Add a `Beheerder` role description (currently missing from this section entirely), cross-checked against `zaak-rechten.rego`, `document-rechten.rego`, `werklijst-rechten.rego`, `notitie-rechten.rego`, and `overige-rechten.rego` for its explicit grants.
- [x] 1.5 Re-read `Raadpleger` and `Behandelaar` descriptions (lines 172, 174) to confirm they already state direct rights with no inheritance language, and leave them unchanged if so.

## 2. Verify

- [x] 2.1 Re-run a full-text search for hierarchy/additive keywords ("aanvullende rechten", "erft", "overerving", "automatisch") across `docs/manuals/ZAC-gebruikershandleiding/ZAC-gebruikershandleiding.md` to confirm none remain.
- [x] 2.2 Confirm the rewritten section's role-by-role rights match the current `accessControlPolicies.md` permission table (no contradictions between the two documents).
- [x] 2.3 Run `./gradlew spotlessApply` if any non-markdown files were touched (not expected for this change, but confirm none were).
