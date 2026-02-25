---
path:
  - 'src/**/*.kt'
---
# Kotlin Architecture Principles

These principles define mandatory rules for implementing Kotlin code in this project.

## Table of Contents
1. Composition Over Inheritance
2. Single Responsibility Principle (SRP)
3. Null Safety

## 1. Composition Over Inheritance

**Definition:** Composition means assembling objects by injecting dependencies rather than extending base classes to inherit behavior.

**Rule:** Always prefer composition over inheritance for code reuse and behavior sharing.

**Why:**
- Composition allows changing behavior at runtime by swapping dependencies
- Composed dependencies are easy to mock and test
- Composition reduces tight coupling between classes
- Each composed object has a single, clear responsibility
- Avoids the fragile base class problem where base class changes break subclasses

### How to use composition:

```kotlin
// ❌ NEVER do this: Inheritance for code reuse
abstract class BaseService(
   protected val zrcClientService: ZrcClientService,
   protected val ztcClientService: ZtcClientService
) {
   protected fun getZaak(uuid: UUID): Zaak = zrcClientService.readZaak(uuid)
}

class ZaakService : BaseService(zrcClient, ztcClient) {
   fun updateZaak(uuid: UUID) {
      val zaak = getZaak(uuid)  // Inherited - tight coupling
   }
}

// ✅ ALWAYS do this: Composition with dependency injection
@ApplicationScoped
class ZgwApiService @Inject constructor(
    private val zrcClientService: ZrcClientService,  // Injected dependency
    private val ztcClientService: ZtcClientService,  // Injected dependency
    private val drcClientService: DrcClientService   // Injected dependency
) {
    fun createZaak(zaak: Zaak): Zaak {
        calculateDoorlooptijden(zaak)
        return zrcClientService.createZaak(zaak)  // Uses composed service
    }
}
```

### Exceptions - When to Use Inheritance:

Only use inheritance in these specific cases:

1. **Sealed class hierarchies for type safety:**
   ```kotlin
   // ✅ ALWAYS use sealed classes for type hierarchies
   sealed class Notification {
       abstract val resource: Resource
       abstract val action: Action
   }
   
   data class ZaakNotification(
       override val resource: Resource,
       override val action: Action,
       val zaakId: UUID
   ) : Notification()
   ```

2. **Template method pattern for fixed workflows:**
   ```kotlin
   // ✅ Use abstract classes only for template methods
   abstract class AbstractZoekObjectConverter<T> {
       fun convert(data: T): ZoekObject {
           val baseObject = createBaseObject(data)  // Step 1
           addSpecificFields(baseObject, data)       // Step 2
           return baseObject
       }
       
       protected abstract fun createBaseObject(data: T): ZoekObject
       protected abstract fun addSpecificFields(obj: ZoekObject, data: T)
   }
   ```

3. **Framework requirements (exceptions only):**
   ```kotlin
   // ✅ Exception hierarchies are acceptable
   open class ZgwRuntimeException(message: String, cause: Throwable? = null) 
       : RuntimeException(message, cause)
   
   class ResultTypeNotFoundException(message: String) 
       : ZgwRuntimeException(message)
   ```

## 2. Single Responsibility Principle (SRP)

**Definition:** A class has a single responsibility if it has only one reason to change. Each class should do one thing and do it well.

**Rule:** Always design classes with exactly one responsibility. Never create classes that handle multiple unrelated concerns.

**Why:**
- Single-responsibility classes are easier to understand and maintain
- Changes to one feature don't affect unrelated functionality
- Classes with one responsibility are easier to test
- Reduces the risk of breaking unrelated features when making changes

### How to use single responsibility:

```kotlin
// ❌ NEVER do this: Multiple responsibilities in one class
class ZaakManager {
   fun createZaak() { /* Zaak creation */ }
   fun sendEmail() { /* Email sending */ }
   fun validateBSN() { /* Business validation */ }
   fun indexInSolr() { /* Search indexing */ }
   fun checkAuthorization() { /* Authorization */ }
   fun logActivity() { /* Audit logging */ }
}
// This class has 6 different reasons to change - violates SRP

// ✅ ALWAYS do this: Separate classes with single responsibilities
@ApplicationScoped
class ZaakService @Inject constructor(
   private val zgwApiService: ZgwApiService,
   private val searchService: SearchService
) {
   // Responsibility: Orchestrate zaak business logic only
   @Transactional
   fun createZaak(createData: ZaakCreateData): Zaak {
      val zaak = zgwApiService.createZaak(createData.toZaak())
      searchService.indexZaak(zaak)
      return zaak
   }
}

@ApplicationScoped
class NotificationService @Inject constructor(
   private val mailService: MailService
) {
   // Responsibility: Send notifications only
   fun notifyZaakCreated(zaak: Zaak, recipients: List<EmailAdres>) {
      recipients.forEach { recipient ->
         mailService.sendMail(
            to = recipient,
            subject = "Nieuwe zaak ${zaak.identificatie}",
            body = createBody(zaak)
         )
      }
   }
}

@ApplicationScoped
class PolicyService @Inject constructor(
   private val opaClient: OpaClient
) {
   // Responsibility: Evaluate authorization policies only
   fun readZaakRechten(zaak: Zaak): ZaakRechten {
      return opaClient.readZaakRechten(zaak.uuid)
   }
}
```

### Service Size Guidelines:

Follow these limits to maintain single responsibility:
- ✅ Keep services between 100-300 lines
- ✅ Limit to 3-10 public methods per service
- ✅ Focus on one clear domain concept per service
- ✅ Inject 2-6 dependencies maximum

**Warning Signs of SRP Violations:**
- ⚠️ Class exceeds 500 lines
- ⚠️ Class has more than 15 public methods
- ⚠️ Class has more than 8 injected dependencies
- ⚠️ Class name contains vague terms like "Manager", "Helper", "Util" without clear domain focus
- ⚠️ You cannot describe the class purpose in one sentence

## 3. Null Safety 

**Definition:** Null safety means explicitly declaring when values can be null using the `?` type modifier, and safely handling null values.

**Rule:** Always use nullable types (`Type?`) when values might be absent. Never use `!!` (force unwrap) except in tests.

**Why:**
- Nullable types make absence explicit in the type system
- Safe call operators prevent null pointer exceptions
- Elvis operator provides clean default values
- Force unwrap (`!!`) can crash at runtime

### How to use null safety:

```kotlin
// ✅ ALWAYS use nullable types explicitly
fun findZaak(uuid: UUID): Zaak?  // Clear: might return null

fun findZaakOrThrow(uuid: UUID): Zaak  // Clear: never returns null

// ✅ ALWAYS use safe call operator for nullable chains
val email: String? = zaak?.initiator?.emailAdres?.value

// ✅ ALWAYS use Elvis operator for defaults
val name: String = zaak?.naam ?: "Onbekend"
val count: Int = zaak?.documenten?.size ?: 0

// ✅ ALWAYS use let for nullable execution
zaak?.let { nonNullZaak ->
   processZaak(nonNullZaak)
}

// ✅ ALWAYS use early return for null checks
fun processZaak(zaakId: UUID?) {
   val id = zaakId ?: return  // Early return if null
   // Rest of function assumes id is non-null
   val zaak = findZaak(id)
   // ...
}

// ❌ NEVER use force unwrap in production code
val name = zaak.naam!!  // Can crash with NullPointerException!

// ❌ NEVER use platform types without null checks
// Platform types (from Java) have unknown nullability
fun processJavaResult(result: JavaType) {  // Might be null!
   result.value  // Dangerous if Java returns null
}

// ✅ ALWAYS add explicit null checks for platform types
fun processJavaResult(result: JavaType?) {  // Explicit nullable
   val value = result?.value ?: return
   // ...
}
```
