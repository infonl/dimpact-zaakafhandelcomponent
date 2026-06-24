## Context

Hibernate Validator 8.x deprecated placing `@Valid` on a container type (e.g. `List<T>`) and requires it on the type argument instead (`List<@Valid T>`). Two REST model classes in the admin module use the old form, causing `HV000271` warnings in the ZAC server logs.

## Goals / Non-Goals

**Goals:**
- Eliminate the `HV000271` deprecation warning
- Preserve identical cascade validation behaviour

**Non-Goals:**
- Upgrading Hibernate Validator
- Changing validation rules or constraints on `RestReferenceTableValue`

## Decisions

**Move annotation to type argument**

Change:
```kotlin
@field:Valid
var waarden: List<RestReferenceTableValue> = emptyList()
```
to:
```kotlin
var waarden: List<@Valid RestReferenceTableValue> = emptyList()
```

This is the Hibernate Validator 8+ recommended form. The `@field:` use-site target is no longer needed since the annotation now targets the type argument directly. Validation semantics are unchanged — each element in the list is still cascade-validated.

## Risks / Trade-offs

No risks. This is a one-line annotation placement change per file with no behavioral difference.
