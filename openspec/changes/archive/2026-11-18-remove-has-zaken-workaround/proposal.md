## Why

In [PZ-13372](https://dimpact.atlassian.net/browse/PZ-13372) `ZaaktypeConfigurationRestService` asks Open Zaak whether a zaaktype version has zaken before it stores the configuration, and passes the answer to `ZaaktypeConfigurationBeheerService.checkProcessBinding` as a parameter `hasZaken: Boolean`. That was done to keep the Open Zaak call out of the window between the cache eviction and the commit of the store. Since [PZ-13546](https://dimpact.atlassian.net/browse/PZ-13546) the cache is also cleared after the transaction completes, so that window no longer matters, and the workaround only makes the code harder to follow ([PZ-13551](https://dimpact.atlassian.net/browse/PZ-13551)).

The call was in fact never inside that window: the REST service runs without a transaction, and the cache is cleared inside the transaction of `storeConfiguration`, after the call. This change is therefore safe on its own; it is stacked on PZ-13546 only because the ticket orders it after that one.

## What Changes

- `checkProcessBinding` becomes `checkProcessBindingAndReadChangeability`. It asks Open Zaak itself, once, whether the zaaktype version has zaken, and returns whether the process definition stays changeable once the configuration is bound to it. The name says that it returns a value.
- `isProcessDefinitionChangeable` and `checkProcessBindingAndReadChangeability` share one private rule: a process definition is changeable unless the configuration is bound and its zaaktype version has zaken.
- `ZaaktypeConfigurationRestService.createOrUpdateZaaktypeConfiguration` no longer calls `hasZaken`, drops the comment about the order, and fills `isProcessDefinitionChangeable` from the result of `checkProcessBindingAndReadChangeability`.
- A save still asks Open Zaak once.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- none: the behaviour of changing the process definition of a zaaktype version does not change.

## Impact

- Backend: `ZaaktypeConfigurationBeheerService` and `ZaaktypeConfigurationRestService`.
- Unit tests `ZaaktypeConfigurationBeheerServiceTest` and `ZaaktypeConfigurationRestServiceTest` follow the new signature. The scenarios about the process binding move to a new `ZaaktypeConfigurationBeheerServiceProcessBindingTest`, because the first test class grows past detekt's `LargeClass` limit.
- No REST, OpenAPI, database or frontend change; the test-data scripts do not change.
