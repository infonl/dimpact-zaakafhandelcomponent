## 1. Backend (PR 1)

- [ ] 1.1 Add `listZaaktypeUuidsLinkedTo(mailTemplateId)` to `MailTemplateKoppelingenService` (design D1).
- [ ] 1.2 In `MailTemplateService`, inject `MailTemplateKoppelingenService`; let `updateMailtemplate` call
      `zaaktypeConfigurationService.evict(uuid)` for each linked zaaktype instead of `clearManagedCache()`, and drop
      the clear in `delete` and in the create branch of `storeMailtemplate` (design D1, D2).

## 2. Tests (PR 1)

- [ ] 2.1 In `MailTemplateKoppelingenServiceTest`, test that `listZaaktypeUuidsLinkedTo` queries the koppelingen of
      the given template.
- [ ] 2.2 In `MailTemplateServiceTest`, verify that `updateMailtemplate` calls `evict` for each linked zaaktype and never
      `clearManagedCache()`, and that creating or deleting a template evicts nothing.
- [ ] 2.3 In the itest `ZaaktypeConfigurationRestServiceBpmnMailTest`, after the linked template was read, change its
      onderwerp via `PUT /beheer/mailtemplates/{id}` and check that the next read of the mail template of the zaak
      returns the new onderwerp. This also passes on main, so it guards against a regression; it cannot prove the race
      fix (design, Risks).

## 3. Checks (PR 1)

- [ ] 3.1 Update the SPDX headers of the touched files and run `./gradlew spotlessApply detektApply`,
      `./gradlew detekt` and `./gradlew compileKotlin`.
- [ ] 3.2 Run `./gradlew test --tests "nl.info.zac.mailtemplates.*" --tests "net.atos.zac.admin.*"`.
- [ ] 3.3 Run `KIND_PORT_OFFSET=2000 ./gradlew itest --tests "nl.info.zac.itest.bpmn.ZaaktypeConfigurationRestServiceBpmnMailTest"`.
- [ ] 3.4 In the local kind stack as a beheerder: change the onderwerp of a mail template linked to a zaaktype, then
      as a behandelaar open the mail form of a zaak of that zaaktype and check the new onderwerp; press "cache legen"
      on the admin page and check it still reports the cleared cache.
