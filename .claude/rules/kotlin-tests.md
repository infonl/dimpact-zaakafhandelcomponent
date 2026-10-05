---
paths:
  - "src/test/**/*.kt"
  - "src/itest/**/*.kt"
---

# Kotlin test conventions

## Kotest (Backend Tests)
Use BDD style with `context`/`given`/`` `when` ``/`then` blocks.
```kotlin
class MyServiceTest : BehaviorSpec({
    context("A function in the service under test") {
        given("some state") {
            `when`("action occurs") {
                then("expected result") { ... }
            }
        }
    }
})
```

Use the lowercase `and(...)` block for an additional assertion group after a `then(...)`, not the capitalized `And(...)`. Kotest's `BehaviorSpec` provides both, but mixing cases (lowercase `given`/`when`/`then` with capitalized `And`) is exactly what CodeQL's `java/confusing-method-name` query flags as confusing. A few older specs still use `And(...)`; new and touched specs use `and(...)`.
```kotlin
// Before
then("expected result") { ... }
And("an additional assertion") { ... }
// After
then("expected result") { ... }
and("an additional assertion") { ... }
```

## In Kotlin unit tests the 'shouldThrow' should be in the 'When' block
In Kotlin unit tests the 'shouldThrow' should be in the '`when`' and not in the 'then' block.
Also the exception message should be checked in the 'then' block.

```kotlin
// Before
`when`("calling the service") {
    then("should throw IllegalArgumentException") {
        shouldThrow<IllegalArgumentException> {
            service.add(1, 2)
        }
    }
}
// After
`when`("calling the service") {
    val illegalArgumentException = shouldThrow<IllegalArgumentException> {
        service.add(1, 2)
    }
    
    then("should throw IllegalArgumentException") {
        illegalArgumentException.message shouldBe "Expected exception message"
    }
}    
``` 

## Use `fakeXXX` for test values where possible

For example, instead of:
```kotlin
createRestUser(id = "user1", name = "User One")
```

use:
```kotlin
createRestUser(id = "fakeUserId1", name = "fakeUserName1")
```

## Let the `given`/`when`/`then` names carry the explanation, not comments
Tests must be self-documenting through their `context`/`given`/`` `when` ``/`then` descriptions and their variable
names. Do not add comments describing the scenario, the setup or the assertion — put that information in the block
description instead. This also applies to class-level KDoc summarising what a test class covers.

```kotlin
// Before
// the new zaaktype deliberately lists 'Verlengd' first so that a positional mapping produces the wrong result
val newZaaktype = createZaakType(resultTypes = listOf(...))
// After
given("a previous configuration whose resultaattypen are not the first ones of the new zaaktype") { ... }
```
