## 1. Backend

- [ ] 1.1 `ZrcClient.zaakeigenschapDelete` and `ZrcClientService.deleteZaakeigenschap(zaakUUID, uuid, auditExplanation)`.
- [ ] 1.2 `shouldMarkZaakspecifiekGeautoriseerd` no longer throws. Remove `ZaakspecifiekeAutorisatieCannotBeLiftedException` and its error code.
- [ ] 1.3 `ZaakspecifiekeAutorisatieService.liftZaakspecifiekeAutorisatie(zaak, reden)`: under the lock, delete
      every ZGM rol, then every `ZAAK_GEAUTORISEERD` zaakeigenschap, with the reden; reindex the zaak, its
      taken and its documenten, and commit Solr.
- [ ] 1.4 `ZaakRestService.updateZaak` lifts the marking, for `false` on a marked zaak, before it assigns the zaak,
      and does not require the ZGM roltype for a handover in that update.
- [ ] 1.5 Adding a medewerker and granting a taakbehandelaar read the marking again under the lock, and add no
      rol to a zaak that is no longer marked.
- [ ] 1.6 Unit tests for 1.1-1.5.
- [ ] 1.7 Integration test: lift the marking and check the rollen and zaakeigenschap are gone, another
      behandelaar of the zaaktype finds and opens the zaak, the zaakhistorie line with the reden, and that
      marking again does not bring the extra medewerker back. Replace the test that expects the refusal.

## 2. Frontend

- [ ] 2.1 Keep *Zaak zaakspecifiek autoriseren* enabled on a marked zaak when the employee has `wijzigen`; send
      `false` only when the box went from ticked to unticked.
- [ ] 2.2 Confirmation dialog *Zaakspecifieke autorisatie opheffen* without a reden, opened on submit.
- [ ] 2.3 i18n texts in `nl.json` and `en.json`: dialog, updated hint, removed error code.
- [ ] 2.4 Specs for the form and the dialog.

## 3. Wrap-up

- [ ] 3.1 Update the gebruikershandleiding.
- [ ] 3.2 Run the linters and tests.
