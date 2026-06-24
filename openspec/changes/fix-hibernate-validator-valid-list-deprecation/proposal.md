## Why

Hibernate Validator logs `HV000271` deprecation warnings at startup because `@Valid` is applied to the `List` container rather than the type argument. This creates noise in logs and will break in a future Hibernate Validator major version.

## What Changes

- Move `@field:Valid` from the `List` container to the type argument in `RestReferenceTable.waarden`
- Move `@field:Valid` from the `List` container to the type argument in `RestReferenceTableUpdate.waarden`

## Capabilities

### New Capabilities
<!-- None — this is a pure bug fix with no new capabilities -->

### Modified Capabilities
<!-- No spec-level requirement changes; validation semantics are identical -->

## Impact

- `src/main/kotlin/nl/info/zac/app/admin/model/RestReferenceTable.kt`
- `src/main/kotlin/nl/info/zac/app/admin/model/RestReferenceTableUpdate.kt`
- No API changes, no database changes, no dependency version changes
