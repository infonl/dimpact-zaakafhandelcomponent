## 1. Backend (PR 1)

- [ ] 1.1 Add `ZaaktypeConfigurationsCacheEvictionEvent` as a `data object` in `nl.info.zac.admin`.
- [ ] 1.2 In `ZaaktypeConfigurationService`, inject `Event<ZaaktypeConfigurationsCacheEvictionEvent>`, add
      `evictAll()` and the `AFTER_COMPLETION` observer that clears all configurations again (design D1, D2). Leave
      `clearManagedCache()` unchanged.
- [ ] 1.3 In `MailTemplateService`, replace the three `clearManagedCache()` calls with `evictAll()` (design D3).

## 2. Tests (PR 1)

- [ ] 2.1 In `ZaaktypeConfigurationServiceTest`, test that `evictAll` clears all cached configurations at once and
      fires the event, that the observer clears them, and that `clearManagedCache` clears at once without firing an
      event.
- [ ] 2.2 In `MailTemplateServiceTest`, verify `evictAll()` instead of `clearManagedCache()` for `delete`,
      `updateMailtemplate` and `storeMailtemplate`.
- [ ] 2.3 In the itest `ZaaktypeConfigurationRestServiceBpmnMailTest`, after the linked template was read, change its
      onderwerp via `PUT /beheer/mailtemplates/{id}` and check that the next read of the mail template of the zaak
      returns the new onderwerp.

## 3. Checks (PR 1)

- [ ] 3.1 Update the SPDX headers of the touched files and run `./gradlew spotlessApply detektApply`,
      `./gradlew detekt` and `./gradlew compileKotlin`.
- [ ] 3.2 Run `./gradlew test --tests "nl.info.zac.admin.*" --tests "nl.info.zac.mailtemplates.*"`.
- [ ] 3.3 Run `KIND_PORT_OFFSET=2000 ./gradlew itest --tests "nl.info.zac.itest.bpmn.ZaaktypeConfigurationRestServiceBpmnMailTest"`.
- [ ] 3.4 In the local kind stack as a beheerder: change the onderwerp of a mail template linked to a zaaktype, then
      as a behandelaar open the mail form of a zaak of that zaaktype and check the new onderwerp; press "cache legen"
      on the admin page and check it still reports the cleared cache.
