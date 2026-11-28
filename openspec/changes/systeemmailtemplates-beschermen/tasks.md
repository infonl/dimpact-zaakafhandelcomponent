## 1. Backend

- [ ] 1.1 Add `ERROR_CODE_SYSTEM_MAIL_TEMPLATE_CANNOT_BE_DELETED`, `ERROR_CODE_SYSTEM_MAIL_TEMPLATE_CANNOT_BE_CONVERTED`
      and `ERROR_CODE_MAIL_TEMPLATE_IS_IN_USE_BY_ZAAKTYPE_CONFIGURATION` to `ErrorCode` (design D6).
- [ ] 1.2 In `MailTemplateService.delete`, before the template is removed, throw `InputValidationFailedException` with
      the delete error code when the stored template is a system mail template, and otherwise with the in-use error
      code when `mailTemplateKoppelingenService.listZaaktypeUuidsLinkedTo(id)` is not empty (design D1, D2, D5).
- [ ] 1.3 In `MailTemplateService.updateMailtemplate`, throw `InputValidationFailedException` with the convert error
      code, before the request is copied onto the entity, when the flag of the request differs from the stored flag,
      or when the stored template is a system mail template and the `mail` of the request differs (design D2, D4).
- [ ] 1.4 In `MailTemplateService.createMailtemplate`, throw `InputValidationFailedException` with the convert error
      code when the request has `isDefaultMailtemplate = true` (design D4).

## 2. Frontend

- [ ] 2.1 Add `msg.error.system.mailtemplate.cannot.be.deleted`, `msg.error.system.mailtemplate.cannot.be.converted`
      and `msg.error.mailtemplate.is.in.use.by.zaaktype-configuration` to `src/main/app/src/assets/i18n/nl.json` and
      `en.json`.

## 3. Tests

- [ ] 3.1 In `MailTemplateServiceTest`, test that:
      - deleting a system mail template throws with the delete error code and removes nothing, also when it is linked;
      - deleting a normal mail template that a zaaktype configuration links throws with the in-use error code and
        removes nothing;
      - deleting a normal mail template that nothing links removes it (the existing test, with a normal template);
      - storing a system mail template with the flag off, or with another `mail`, throws with the convert error code
        and merges nothing;
      - storing a system mail template with a changed name, subject and body merges those changes;
      - storing a normal mail template with the flag on throws with the convert error code and merges nothing;
      - creating a mail template with the flag on throws with the convert error code and persists nothing.
- [ ] 3.2 In the itest `MailtemplateBeheerRestServiceTest`, read the id of the system mail template of
      `SIGNALERING_TAAK_OP_NAAM` from the list, and test that:
      - `DELETE` answers 400 with the delete error code, and the template is still in the list;
      - `PUT` with `defaultMailtemplate: false`, and `PUT` with `mail: ZAAK_ALGEMEEN`, answer 400 with the convert
        error code, and `GET` returns the template unchanged;
      - `PUT` with a changed name, subject and body answers 200 with those changes; restore the original values
        afterwards;
      - `POST` with `defaultMailtemplate: true` answers 400 with the convert error code.
- [ ] 3.3 In the same itest, create a normal mail template and link it to a zaaktype by storing the configuration of
      that zaaktype with the template in `mailtemplateKoppelingen`, with `ZacClient.createZaaktypeBpmnConfiguration`
      as in `ZaaktypeConfigurationRestServiceBpmnMailTest`; `PUT beheer/mailtemplatekoppeling` cannot create a link,
      because it leaves the zaaktype configuration unset. Check that the link is stored, and test that `DELETE`
      answers 400 with the in-use error code and the template and the link still exist. Then store the configuration
      again with its original links, and test that `DELETE` answers 204. Keep the existing create-read-update-delete
      test of a normal template unchanged.

## 4. Checks

- [ ] 4.1 Run `./gradlew spotlessApply detektApply`, `./gradlew detekt` and `./gradlew compileKotlin`.
- [ ] 4.2 Run `./gradlew test --tests "nl.info.zac.mailtemplates.*" --tests "nl.info.zac.app.admin.*"`.
- [ ] 4.3 Run `cd src/main/app && npm run lint` and the Jest spec `backend-constant-translations.spec.ts`.
- [ ] 4.4 Run `./gradlew itest --tests "nl.info.zac.itest.MailtemplateBeheerRestServiceTest"`.
- [ ] 4.5 On the local kind stack, check on the admin screen that a system mail template has no delete button, that
      its name, subject and body can still be stored, and that a new normal template can be created and deleted.
