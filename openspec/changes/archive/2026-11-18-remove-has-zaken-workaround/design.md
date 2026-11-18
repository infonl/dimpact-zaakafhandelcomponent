## Context

`createOrUpdateZaaktypeConfiguration` calls `hasZaken` before `storeConfiguration`, passes the answer to `checkProcessBinding`, and sets `isProcessDefinitionChangeable = !hasZaken` on the response. Since PZ-13546 `ZaaktypeConfigurationService.evict` also clears the cache after the transaction completes.

## Goals / Non-Goals

**Goals:**
- The check whether a zaaktype version has zaken lives in the beheer service again.
- A save asks Open Zaak once, as now.
- One rule decides whether a process definition is changeable.

**Non-Goals:**
- Any change in behaviour, REST payload or error code.

## Decisions

### D1. The check asks Open Zaak first and returns whether the process definition stays changeable

`checkProcessBindingAndReadChangeability` calls `hasZaken` once, before it reads the configuration and compares the binding, and returns whether the process definition stays changeable once the configuration is bound. The REST service needs that answer for `isProcessDefinitionChangeable` in the response, also when the binding does not change; asking only when the binding changes would need a second call for the response.

The function both validates and returns a value, so its name says both. Keeping `checkProcessBinding` as a command and letting the REST service call `isProcessDefinitionChangeable` after the store would ask Open Zaak twice when the binding changes, so that is not chosen.

### D2. A failure of Open Zaak still stores nothing

`hasZaken` runs before the store, as the first step of the check, so a failure of Open Zaak still rejects the save before the configuration is read or stored.

### D3. One rule for "changeable"

A private `isProcessDefinitionChangeable(isBound, hasZaken)` holds the rule: changeable unless bound and with zaken. `isProcessDefinitionChangeable(zaaktypeConfiguration)` passes the current binding and asks Open Zaak lazily, so an unbound configuration costs no call. The check passes `isBound = true`, because after the save the configuration is bound to the process definition it was checked for.
