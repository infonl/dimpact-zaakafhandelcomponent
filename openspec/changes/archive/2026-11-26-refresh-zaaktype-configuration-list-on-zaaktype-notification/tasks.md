## 1. Backend

- [x] 1.1 Add `ZAAKTYPE_CONFIGURATIES` to `ScreenEventType`, without an object of its own (design D1).
- [x] 1.2 In `NotificationReceiver.handleZaaktype`, send `ScreenEventType.ZAAKTYPE_CONFIGURATIES.updated(FixedScreenEventResourceId.ZAAKTYPE_CONFIGURATIES)` after `updateZaaktypeConfiguration` returns, for `create` and `update` only (design D2).
- [x] 1.3 In `NotificationReceiverTest`, extend the existing `create` and `update` zaaktype tests: stub `eventingService.send(any<ScreenEvent>())` and check with `verifyOrder` that the event is sent after `updateZaaktypeConfiguration`. Without the stub these tests would keep passing while `handleZaaktype` catches the `MockKException`. Add tests that a `destroy` notification, a failing `updateZaaktypeConfiguration` and a notification about another resource send no `ZAAKTYPE_CONFIGURATIES` event (design D4).
- [x] 1.4 Regenerate the OpenAPI spec (`./gradlew generateOpenApiSpec`).

## 2. Frontend

- [x] 2.1 Regenerate the frontend types from the OpenAPI spec.
- [x] 2.2 In `ParametersComponent`, add a websocket listener for `UPDATED` / `ZAAKTYPE_CONFIGURATIES` / `zaaktype-configuraties` that invalidates the list query, and remove it when the component is destroyed (design D3).
- [x] 2.3 Remove `refetchOnWindowFocus: true` from the list query; keep `staleTime: StaleTimes.Instant`.

## 3. Tests

- [x] 3.1 In `parameters.component.spec.ts`, replace the spec that reads the list again on tab focus with one that checks that the `ZAAKTYPE_CONFIGURATIES` screen event reads the list again and that the row then shows the procesmodel. The spec fails without the listener.
- [x] 3.2 Check that the page does not read its list again when its tab gets the focus.
- [x] 3.3 Check that the listener is removed when the page is destroyed.
- [x] 3.4 In `NotificationZaaktypeCompletionParametersTest`, subscribe a websocket to `UPDATED` / `ZAAKTYPE_CONFIGURATIES` / `zaaktype-configuraties`, as `NotificationZaakUpdateWebSocketListenerTest` does, and check that the zaaktype notification of that test reaches it with that opcode, type and id (design D4).

## 4. Checks

- [x] 4.1 Run `./gradlew spotlessApply detektApply detekt` and `./gradlew test --tests "nl.info.zac.notification.NotificationReceiverTest"`.
- [x] 4.2 In `src/main/app`, run `npm run lint`, which also type-checks the specs with `ngc` (Jest does not type-check), and `npx ng test --test-path-patterns=src/app/admin/parameters/parameters.component.spec.ts`.
- [x] 4.3 Run `./gradlew itest --tests "nl.info.zac.itest.NotificationZaaktypeCompletionParametersTest"`.
- [x] 4.4 In the local kind stack, keep the Zaakafhandelparameters page open, publish a new version of a configured zaaktype on the publish page of Open Zaak (the "Publiceren" button of the zaaktype) in another window, and check that the page shows the new version with its procesmodel without a reload or a focus change.
