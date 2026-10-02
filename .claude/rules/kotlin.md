---
paths:
  - "src/**/*.kt"
  - "src/main/java/**/*.java"
  - "build.gradle.kts"
---

# Kotlin conventions

## Omit return types when they are obvious
When a Kotlin function has an expression body (`=`) and its right-hand side already makes the return type
unambiguous — constructing a specific type via `Foo().apply { ... }`, or delegating to another function whose
own return type is already clear — omit the explicit return type declaration. This applies even to public API
functions; this project deliberately diverges from the general Kotlin style guide's recommendation to always
declare return types on public declarations.
```kotlin
// Before
fun convert(mailTemplate: MailTemplate): RESTMailtemplate =
    RESTMailtemplate().apply { ... }
// After
fun convert(mailTemplate: MailTemplate) =
    RESTMailtemplate().apply { ... }
```
Keep the explicit return type when the body is a block (`{ ... return ... }`, where Kotlin requires it anyway)
or when omitting it would genuinely obscure what the function returns.

## Use variable names that are the same as their type where possible
When you see a variable declaration where the variable name is different from its type, rename the variable to match the type. For example, write `val user: User` instead of `val u: User` or `val usr: User`. This improves readability and makes it clear what the variable represents.
This includes exceptions.
For example `catch (e: IOException)` should be `catch (ioException: IOException)`.

## Follow the Kotlin Coding Conventions
Follow the official Kotlin coding conventions for naming, formatting, and structuring code: https://kotlinlang.org/docs/coding-conventions.html
Place `companion object` at the **top** of a class body, before any functions or properties.
This includes using camelCase for function and variable names, PascalCase for class names, and consistent indentation and spacing.
Rename existing classes to comply with the following Kotlin code convention:
When using an acronym as part of a declaration name, follow these rules:
— For two-letter acronyms, use uppercase for both letters. For example, IOStream.
— For acronyms longer than two letters, capitalize only the first letter. For example, XmlFormatter or HttpInputStream.

## Name boolean properties with an `is`/`has` prefix
Follow the [Kotlin convention for booleans](https://kotlinlang.org/docs/coding-conventions.html#names-for-test-methods):
name boolean properties and variables with an `is`, `has`, or similar prefix, e.g. `isInformatieobjectDeleted`
rather than `informatieobjectDeleted`. This applies to REST model classes as well as regular code.
For a boolean property on a class serialized with JSON-B (e.g. a `RestXxx` model), add
`@get:JsonbProperty("isXxx")` above the property so the JSON field name matches the Kotlin property
name exactly:
```kotlin
// Before
var informatieobjectDeleted: Boolean = true
// After
@get:JsonbProperty("isInformatieobjectDeleted")
var isInformatieobjectDeleted: Boolean = true
```

## Prefer Kotlin data classes for simple data holders
When you encounter a class that is primarily used to hold data (i.e., it has properties and no significant behavior), for example for classes used as arguments or responses in REST services,
use a Kotlin `data class`.
When used by dependency injection frameworks, such as is the case in REST services, ensure that the data class has the following annotations:
```
@NoArgConstructor
@AllOpen
```

## Use named parameters in Kotlin
When calling a Kotlin function that has multiple parameters, especially if they are of the same type, use named parameters to improve readability. For example:
```kotlin
// Before
val user = createUser("John", "Doe", 30)
// After
val user = createUser(firstName = "John", lastName = "Doe", age = 30)
```
This makes it clear what each argument represents and reduces the chance of accidentally swapping parameters.

## Prefer concise lambda syntax in Kotlin
When you have a lambda function that can be simplified to a single expression, use the concise syntax. For example:

    // Before
    val sum = numbers.map { number -> number * 2 }.sum()
    // After
    val sum = numbers.map { it * 2 }.sum()
This makes the code more concise and easier to read.

## Use method references in Kotlin
When you have a lambda function that simply calls another function, use a method reference to make the code more concise. For example:
```kotlin// Before
val param.map { someFunction(it) }
// After
val param.map(::someFunction)
```

## Use .apply for object configuration in Kotlin
When you need to configure an object after creating it, use the `.apply` scope function to make the code more concise and readable. For example:
```kotlin// Before
val user = User()
user.firstName = "John"
user.lastName = "Doe"
// After
val user = User().apply {
    firstName = "John"
    lastName = "Doe"
}
```
This allows you to initialize the object in a more fluent way.

Prefer `.apply` over `.also` for this even inside an extension function, where the extension receiver and the
object being configured are two different values in scope at once. Qualify references to the extension receiver
with `this@functionName` so every unqualified assignment inside the `apply` block unambiguously targets the new
object:
```kotlin
// Before (.also, only needed because of the two receivers)
fun MailTemplate.toRestMailtemplate() = RESTMailtemplate().also {
    it.mailTemplateNaam = mailTemplateNaam
}
// After (.apply, receiver disambiguated explicitly)
fun MailTemplate.toRestMailtemplate() = RESTMailtemplate().apply {
    mailTemplateNaam = this@toRestMailtemplate.mailTemplateNaam
}
```
`.also`'s `it`/named parameter is for side effects on an existing value; configuring a freshly constructed object's
fields is not a side effect, so reach for `.apply` regardless of how many receivers are in scope.

## Distinguish between `findXxx` and `readXxx` functions in low-level Kotlin CRUD services
Use the following convention:

`findXxx(itemId)` function: returns `null` if the item in question could not be found
`readXXX(itemId)` - throws 'Item not found' exception when the item in question could not be found

For example:
```kotlin
  fun findReferenceTable(code: String): ReferenceTable? =
    entityManager.criteriaBuilder.let { criteriaBuilder ->
        criteriaBuilder.createQuery(ReferenceTable::class.java).let { query ->
            query.from(ReferenceTable::class.java).let { root ->
                criteriaBuilder.equal(root.get<Any>("code"), code.uppercase()).let { predicate ->
                    query.select(root).where(predicate)
                }
            }
            entityManager.createQuery(query).resultList
        }
    }.firstOrNull()
```

and:
```kotlin
fun readReferenceTable(code: String): ReferenceTable =
        findReferenceTable(code) ?: run {
            throw ReferenceTableNotFoundException("No reference table found with code '$code'")
        }
```

## Kotlin repository entity classes must have the @AllOpen annotation
Kotlin repository entity classes must have the @AllOpen annotation.

```kotlin// Before
@Entity
@Table(schema = SCHEMA, name = "inbox_document")
@SequenceGenerator(schema = SCHEMA, name = "sq_inbox_document", sequenceName = "sq_inbox_document", allocationSize = 1)
class InboxDocument
// After
@Entity
@Table(schema = SCHEMA, name = "inbox_document")
@SequenceGenerator(schema = SCHEMA, name = "sq_inbox_document", sequenceName = "sq_inbox_document", allocationSize = 1)
@AllOpen
class InboxDocument
```

## In Kotlin repository entity classes use `lateinit var` for variables that are nullable
In Kotlin repository entity classes use `lateinit var` for variables that are nullable instead of a nullable variable.

```kotlin// Before
@NotNull
@Column(name = "creatiedatum", nullable = false)
var creatiedatum: LocalDate? = null
// After
@NotNull
@Column(name = "creatiedatum", nullable = false)
lateinit var creatiedatum: LocalDate
```

## Use `XxxRepository` naming convention for Kotlin repository classes
Name Kotlin classes that perform logic on the ZAC database using JPA `XxxRepository`

```kotlin// Before
class DetachedDocumentService
// After
class DetachedDocumentRepository
```

## Use proper Transaction annotations in Kotlin service classes
Use proper Transaction annotations in Kotlin service classes.
Follow these rules:
- Use `@Transactional(SUPPORTS)` at class level when a service class contains functions that update data in the database.
- Use `@Transactional(REQUIRED)` at function level for functions that update data in the database (create, update, delete)
- Read functions do not need a `@Transactional` annotation.
- Do not use any transactional annotations at function level for functions that only read from the database.
  For these functions, the transactional annotation at class level is used.
