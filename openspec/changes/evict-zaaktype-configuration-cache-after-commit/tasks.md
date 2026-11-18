## 1. ADR

- [x] 1.1 Add an ADR that records clearing an application-wide cache after the transaction completes, with a
      synchronous CDI event and an `AFTER_COMPLETION` observer (design D6), add it to the index, and run
      `python3 scripts/adr/check-adrs.py --base origin/main`.

## 2. Backend

- [x] 2.1 Add `ZaaktypeConfigurationCacheEvictionEvent` with the zaaktype UUID.
- [x] 2.2 Inject `Event<ZaaktypeConfigurationCacheEvictionEvent>` in `ZaaktypeConfigurationService`, let `evict`
      fire it, and add the `AFTER_COMPLETION` observer that clears both caches (design D1-D3). Keep
      `clearManagedCache` and `clearListCache` immediate (design D5).

## 3. Tests

- [x] 3.1 In `ZaaktypeConfigurationServiceTest`, test that `evict` fires the event and does not clear the caches
      itself, that the observer clears both caches, and that `clearManagedCache` and `clearListCache` clear at once.
- [x] 3.2 Check that `ZaaktypeConfigurationBeheerServiceTest` and `MailTemplateKoppelingenServiceTest` pass
      unchanged.
- [x] 3.3 In the itest `ZaaktypeConfigurationRestServiceBpmnMailTest`, remove the mailtemplate koppeling after the
      mail template of the zaak was read, and check that the default mail template is proposed.

## 4. Checks

- [x] 4.1 Run `./gradlew spotlessApply detektApply`, `./gradlew detekt` and `./gradlew compileKotlin`.
- [x] 4.2 Run `./gradlew test --tests "nl.info.zac.admin.*" --tests "net.atos.zac.admin.*"`.
- [ ] 4.3 Run `KIND_PORT_OFFSET=2000 ./gradlew itest --tests "nl.info.zac.itest.bpmn.ZaaktypeConfigurationRestServiceBpmnMailTest"`.
