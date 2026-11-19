## 1. Backend

- [x] 1.1 Rename `ZaaktypeConfigurationBeheerService.checkProcessBinding` to `checkProcessBindingAndReadChangeability`, let it ask `hasZaken` itself, once, and return whether the process definition stays changeable (design D1, D2).
- [x] 1.2 Let `isProcessDefinitionChangeable` and the check share one private rule (design D3).
- [x] 1.3 Remove the `hasZaken` call, the `hasZaken` argument and the comment about the order from `ZaaktypeConfigurationRestService.createOrUpdateZaaktypeConfiguration`, and fill `isProcessDefinitionChangeable` from the result of the check.

## 2. Tests

- [x] 2.1 Move the scenarios about the process binding and the zaken in Open Zaak from `ZaaktypeConfigurationBeheerServiceTest` to a new `ZaaktypeConfigurationBeheerServiceProcessBindingTest`, which detekt's `LargeClass` rule requires. In it, stub the zaken in Open Zaak instead of passing `hasZaken`, call the check in `when` and assert the returned value in every accepted scenario, check that Open Zaak is asked once for an unchanged binding, and that a failure of Open Zaak to list the zaken is passed on before the configuration is read.
- [x] 2.2 In `ZaaktypeConfigurationRestServiceTest`, stub the result of the check, check that the response follows it, and that a failing check stores nothing. Remove the scenario of a failing Open Zaak call, which now only repeats the failing check.

## 3. Checks

- [x] 3.1 Run `./gradlew spotlessApply detektApply`, `./gradlew detekt` and `./gradlew compileKotlin`.
- [x] 3.2 Run `./gradlew test --tests "nl.info.zac.admin.*" --tests "nl.info.zac.app.admin.*"`.
