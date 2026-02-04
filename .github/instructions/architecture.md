---
path:
  - 'src/**/*.kt'
---
# ZAC Architecture Pattern and Code Organization

This is the architectural guidelines and code organization conventions for this project. This document focuses on the Kotlin code even though some concepts could be applicable to the Angular typescript frontend as well.

## Current State Analysis

The ZAC codebase currently follows a **feature-based modular architecture** with some layering conventions, but without strict enforcement of architectural boundaries. The code is organized primarily by business domain (zaak, task, admin, etc.) rather than by technical layers.

### Current Package Structure

```
nl.info.zac/
├── app/                    # REST API layer (presentation)
│   ├── admin/
│   ├── zaak/
│   ├── task/
│   └── ...
├── admin/                  # Business logic for admin features
├── zaak/                   # Business logic for zaak features
├── task/                   # Business logic for task features
├── identity/               # Business logic for identity
├── policy/                 # Business logic for policy
├── search/                 # Business logic for search
├── authentication/         # Cross-cutting concern
├── configuratie/           # Cross-cutting concern
└── ...

nl.info.client/            # External API clients (infrastructure)
├── zgw/
├── brp/
├── kvk/
└── ...
```

## Target Architecture: Clean Architecture (Hexagonal-inspired)

We adopt **Clean Architecture** principles with a hexagonal/ports-and-adapters inspiration. This provides:
- Clear separation of concerns
- Testability through dependency inversion
- Independence from frameworks and external systems
- Business logic isolation

### Architecture Layers (Inside-Out)

```
┌─────────────────────────────────────────────────────────┐
│  Presentation Layer (Adapters - Inbound)                │
│  - REST Controllers                                     │
│  - WebSocket endpoints                                  │
│  - Request/Response DTOs                                │
│  - Coarse-grained authorization                         │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│  Application Layer (Use Cases / Application Services)   │
│  - Orchestrate domain logic                             │
│  - Transaction boundaries                               │
│  - Application-specific workflows                       │
│  - Fine-grained authorization                           │
│  - Business rule enforcement                            │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│  Domain Layer (Business Logic)                          │
│  - Domain entities and aggregates                       │
│  - Domain services                                      │
│  - Business rules and validation                        │
│  - Domain events                                        │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│  Infrastructure Layer (Adapters - Outbound)             │
│  - External API clients                                 │
│  - Database repositories                                │
│  - Message brokers                                      │
│  - File systems                                         │
└─────────────────────────────────────────────────────────┘
```

### Ports and Adapters (External System Abstraction)

**Core Principle:** Application layer depends on abstractions (ports), not concrete implementations (adapters).

#### Architecture Pattern

```
Application Layer (ZaakService)
        ↓ depends on (interface/port)
Infrastructure Service Interface (abstraction)
        ↑ implemented by (adapter)
Client Service Implementation (nl.info.client.*)
        ↓ calls (HTTP/REST)
External System (Open Zaak, BRP, KvK, etc.)
```

#### Implementation Example

```kotlin
// ═══════════════════════════════════════════════════════════
// Infrastructure Layer: Client Service (Adapter)
// Package: nl.info.client.brp
// ═══════════════════════════════════════════════════════════
@ApplicationScoped
class BrpClientService @Inject constructor(
    @RestClient private val personenApi: PersonenApi,  // MicroProfile REST client
    private val brpConfiguration: BrpConfiguration,
    private val zaaktypeCmmnConfigurationService: ZaaktypeCmmnConfigurationService
) {
    /**
     * Retrieves a person by BSN from the BRP Personen API.
     * 
     * Handles:
     * - Protocol configuration (protocollering enabled/disabled)
     * - Header management (doelbinding, verwerking, API keys)
     * - Error handling and logging
     * - Request/response mapping
     */
    fun retrievePersoon(
        burgerservicenummer: String,
        zaaktypeUuid: UUID? = null,
        userName: String? = null
    ): Persoon? {
        val query = createRaadpleegMetBurgerservicenummerQuery(burgerservicenummer)
        
        return try {
            if (brpConfiguration.isBrpProtocolleringEnabled()) {
                personenApi.personen(
                    personenQuery = query,
                    doelbinding = resolveDoelbinding(zaaktypeUuid),
                    verwerking = resolveVerwerkingregister(zaaktypeUuid),
                    gebruikersnaam = userName
                )
            } else {
                personenApi.personen(query)
            }.personen?.firstOrNull()
        } catch (e: WebApplicationException) {
            when (e.response.status) {
                404 -> null  // Not found is expected
                else -> throw BrpRuntimeException("BRP API error", e)
            }
        }
    }
    
    // Private helper methods for configuration resolution
    private fun resolveDoelbinding(zaaktypeUuid: UUID?): String? { /* ... */ }
    private fun resolveVerwerkingregister(zaaktypeUuid: UUID?): String? { /* ... */ }
}

// ═══════════════════════════════════════════════════════════
// Application Layer: Business Service
// Package: nl.info.zac.zaak
// ═══════════════════════════════════════════════════════════
@ApplicationScoped
class InitiatorService @Inject constructor(
    private val brpClientService: BrpClientService,  // Uses client abstraction
    private val zgwApiService: ZgwApiService
) {
    /**
     * Links a person as initiator to a zaak.
     * 
     * Business logic:
     * - Validates BSN
     * - Retrieves person data from BRP
     * - Creates initiator role in ZGW
     * - Handles business errors
     */
    @Transactional
    fun addInitiatorToZaak(zaak: Zaak, bsn: Bsn, userName: String): Rol<*> {
        // Business validation
        require(!zaak.isClosed) { "Cannot add initiator to closed zaak" }
        
        // External system call (abstracted through client service)
        val persoon = brpClientService.retrievePersoon(
            burgerservicenummer = bsn.value,
            zaaktypeUuid = zaak.zaaktypeUuid,
            userName = userName
        ) ?: throw BrpPersonNotFoundException(
            "Person with BSN ${bsn.toMaskedString()} not found"
        )
        
        // Business logic orchestration
        return zgwApiService.createInitiatorRole(zaak, persoon)
    }
}
```

#### Client Service Responsibilities

✅ **Client services SHOULD handle:**
- HTTP/REST client configuration
- Request/response mapping (external API ↔ internal models)
- Header management (authentication, API keys, correlation IDs)
- Technical error handling (timeouts, connection errors, HTTP status codes)
- Retry logic and circuit breakers
- Protocol-specific concerns (pagination, rate limiting)
- Logging external API calls
- Request/response transformation

❌ **Client services SHOULD NOT handle:**
- Business validation (e.g., "zaak must be open")
- Business rule enforcement
- Authorization (beyond API authentication)
- Transaction management
- Cross-service orchestration
- Domain events

#### Benefits of This Pattern

1. **Testability**
   ```kotlin
   // Easy to mock external services in tests
   class InitiatorServiceTest : FunSpec({
       val brpClient = mockk<BrpClientService>()
       val zgwApi = mockk<ZgwApiService>()
       val service = InitiatorService(brpClient, zgwApi)
       
       test("should add initiator when person exists") {
           every { brpClient.retrievePersoon(any(), any(), any()) } returns mockPersoon
           every { zgwApi.createInitiatorRole(any(), any()) } returns mockRole
           
           // Test business logic without real BRP API calls
           val result = service.addInitiatorToZaak(mockZaak, mockBsn, "user")
           
           result shouldNotBe null
       }
   })
   ```

2. **Configuration Isolation**
   - Client services encapsulate API-specific configuration
   - Business services focus purely on domain logic
   - Easy to change external API versions or providers
   - Configuration changes don't affect business layer

3. **Error Handling Separation**
   ```kotlin
   // Client service: Technical error handling
   fun retrievePersoon(bsn: String): Persoon? {
       return try {
           personenApi.personen(query).personen?.firstOrNull()
       } catch (e: WebApplicationException) {
           when (e.response.status) {
               404 -> null  // Not found is expected
               503 -> throw BrpRuntimeException("BRP temporarily unavailable", e)
               else -> throw BrpRuntimeException("BRP API error", e)
           }
       }
   }
   
   // Business service: Business error handling
   fun addInitiator(zaak: Zaak, bsn: Bsn): Rol<*> {
       val persoon = brpClient.retrievePersoon(bsn.value)
           ?: throw BusinessRuleViolationException(
               errorCode = ErrorCode.INITIATOR_NOT_FOUND_IN_BRP,
               message = "Cannot add initiator: person not found in BRP register"
           )
       // Business logic continues...
   }
   ```

4. **Resilience Patterns**
   ```kotlin
   // Can add resilience patterns at client service level
   @ApplicationScoped
   class BrpClientService @Inject constructor(
       @RestClient private val personenApi: PersonenApi
   ) {
       @Retry(maxRetries = 3, delay = 1000)
       @Timeout(5000)  // 5 second timeout
       @Fallback(fallbackMethod = "retrievePersoonFallback")
       fun retrievePersoon(bsn: String): Persoon? {
           return personenApi.personen(createQuery(bsn)).personen?.firstOrNull()
       }
       
       private fun retrievePersoonFallback(bsn: String): Persoon? {
           LOG.warning("BRP API unavailable, using fallback for BSN ${bsn}")
           return null  // Or cache, or alternative service
       }
   }
   ```

## Design Principles

These principles guide how we structure and implement code across all layers.

### 1. Composition Over Inheritance

**Prefer composition to inheritance for code reuse and flexibility.**

#### Why Composition?
- **Flexibility**: Change behavior at runtime by swapping dependencies
- **Testability**: Easy to mock/stub composed dependencies
- **Loose Coupling**: Reduces tight coupling between classes
- **Single Responsibility**: Each composed object has one clear purpose
- **Avoid Fragile Base Class Problem**: Changes to base classes don't cascade

#### Examples

```kotlin
// ❌ AVOID: Inheritance for code reuse
abstract class BaseService(
    protected val zrcClientService: ZrcClientService,
    protected val ztcClientService: ZtcClientService
) {
    protected fun getZaak(uuid: UUID): Zaak = zrcClientService.readZaak(uuid)
    protected fun getZaaktype(uri: URI): Zaaktype = ztcClientService.readZaaktype(uri)
}

// Problems:
// - Subclasses inherit ALL methods whether needed or not
// - Changes to BaseService affect all subclasses
// - Cannot choose different implementations
// - Tight coupling to specific implementations
class ZaakService : BaseService(zrcClient, ztcClient) {
    fun updateZaak(uuid: UUID) {
        val zaak = getZaak(uuid)  // Inherited
        // ...
    }
}

// ✅ PREFER: Composition with dependency injection
@ApplicationScoped
class ZgwApiService @Inject constructor(
    private val zrcClientService: ZrcClientService,  // Composed
    private val ztcClientService: ZtcClientService,  // Composed
    private val drcClientService: DrcClientService   // Composed
) {
    fun createZaak(zaak: Zaak): Zaak {
        calculateDoorlooptijden(zaak)
        return zrcClientService.createZaak(zaak)
    }
    
    fun closeZaak(zaak: Zaak, resultaatTypeUUID: UUID, toelichting: String?) {
        val resultaatType = ztcClientService.readResultaattype(resultaatTypeUUID)
        // Uses multiple composed services flexibly
        // ...
    }
}

// Benefits:
// ✅ Each service is independently replaceable
// ✅ Clear dependency graph
// ✅ Easy to test with mocks
// ✅ No hidden inherited state
```

#### When Inheritance IS Appropriate

✅ **DO use inheritance for:**

1. **Type Hierarchies** (polymorphism)
   ```kotlin
   // ✅ Sealed classes for type safety
   sealed class Notification {
       abstract val resource: Resource
       abstract val action: Action
   }
   
   data class ZaakNotification(
       override val resource: Resource,
       override val action: Action,
       val zaakId: UUID
   ) : Notification()
   
   data class TaakNotification(
       override val resource: Resource,
       override val action: Action,
       val taakId: UUID
   ) : Notification()
   ```

2. **Abstract Base Classes with Template Methods**
   ```kotlin
   // ✅ Template method pattern for workflow
   abstract class AbstractZoekObjectConverter<T> {
       // Template method
       fun convert(data: T): ZoekObject {
           val baseObject = createBaseObject(data)
           addSpecificFields(baseObject, data)
           return baseObject
       }
       
       protected abstract fun createBaseObject(data: T): ZoekObject
       protected abstract fun addSpecificFields(obj: ZoekObject, data: T)
   }
   ```

3. **Framework Requirements** (e.g., exceptions, delegates)
   ```kotlin
   // ✅ Exception hierarchies
   open class ZgwRuntimeException(message: String, cause: Throwable? = null) 
       : RuntimeException(message, cause)
   
   class ResultTypeNotFoundException(message: String) 
       : ZgwRuntimeException(message)
   ```

### 2. Single Responsibility Principle (SRP)

**Each class should have one, and only one, reason to change.**

#### Guidelines

```kotlin
// ❌ AVOID: God classes with multiple responsibilities
class ZaakManager {
    fun createZaak() { /* ... */ }            // Zaak creation
    fun sendEmail() { /* ... */ }             // Email sending
    fun validateBSN() { /* ... */ }           // Business rule validation
    fun indexInSolr() { /* ... */ }           // Search indexing
    fun checkAuthorization() { /* ... */ }    // Authorization
    fun logActivity() { /* ... */ }           // Audit logging
}
// Problems: 6 reasons to change this class!

// ✅ PREFER: Separate services with single responsibilities

@ApplicationScoped
class ZaakService @Inject constructor(
    private val zgwApiService: ZgwApiService,
    private val searchService: SearchService
) {
    // Responsibility: Zaak business logic orchestration
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
    // Responsibility: Notification sending
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
    // Responsibility: Authorization policy evaluation
    fun readZaakRechten(zaak: Zaak): ZaakRechten {
        return opaClient.readZaakRechten(zaak.uuid)
    }
}
```

#### Service Sizing Guidelines

**Small, Focused Services:**
- ✅ 100-300 lines typical
- ✅ 3-10 public methods
- ✅ One clear domain concept
- ✅ Dependencies: 2-6 injected services

**Warning Signs:**
- ⚠️ More than 500 lines
- ⚠️ More than 15 public methods
- ⚠️ More than 8 dependencies
- ⚠️ Name contains "Manager", "Helper", "Util" without clear focus

### 3. Factory Pattern

**Use factories for complex object creation, but prefer constructor injection for services.**

#### When to Use Factories

✅ **DO use factories for:**
1. Complex object construction with multiple steps
2. Objects requiring runtime data to construct
3. HTTP header factories (existing pattern in codebase)
4. Converting between DTOs and domain objects

```kotlin
// ═════════════════════════════════════════════════════════════
// Example 1: HTTP Header Factory (existing pattern)
// ═════════════════════════════════════════════════════════════
class BrpClientHeadersFactory @Inject constructor(
    private val brpConfiguration: BrpConfiguration,
    private val loggedInUserInstance: Instance<LoggedInUser>
) : ClientHeadersFactory {
    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> {
        return if (brpConfiguration.isBrpProtocolleringEnabled()) {
            clientOutgoingHeaders.apply {
                add("X-API-KEY", brpConfiguration.apiKey.orElse(null))
                add("X-ORIGIN-OIN", brpConfiguration.originOIN.orElse(null))
                add("X-GEBRUIKER", resolveUser())
            }
        } else {
            clientOutgoingHeaders
        }
    }
    
    private fun resolveUser(): String = /* ... */
}

// ═════════════════════════════════════════════════════════════
// Example 2: Value Object Factory
// ═════════════════════════════════════════════════════════════
object ZaakIdentificatieFactory {
    private const val PREFIX = "ZAAK"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    
    fun generate(gemeenteCode: String, sequence: Int): ZaakIdentificatie {
        val date = LocalDate.now().format(dateFormatter)
        return ZaakIdentificatie(
            "$PREFIX-$gemeenteCode-$date-${sequence.toString().padStart(5, '0')}"
        )
    }
    
    fun fromString(value: String): ZaakIdentificatie {
        // Validation and parsing logic
        return ZaakIdentificatie(value)
    }
}

// ═════════════════════════════════════════════════════════════
// Example 3: DTO to Domain Converter (Factory Methods)
// ═════════════════════════════════════════════════════════════

// ✅ Extension function as factory method
fun RestZaakCreate.toZaak(): Zaak {
    return Zaak(
        identificatie = ZaakIdentificatie(this.identificatie),
        omschrijving = this.omschrijving,
        initiatorBsn = this.initiatorBsn?.let { Bsn(it) },
        contactEmail = this.contactEmail?.let { EmailAdres(it) }
    )
}

// ✅ Companion object factory method
data class PersonenQuery(
    val type: String,
    val fields: List<String>
) {
    companion object {
        fun raadpleegMetBurgerservicenummer(bsn: Bsn): PersonenQuery {
            return PersonenQuery(
                type = "RaadpleegMetBurgerservicenummer",
                fields = listOf("burgerservicenummer", "naam", "geboorte")
            ).apply {
                addBurgerservicenummer(bsn.value)
            }
        }
    }
}
```

❌ **DON'T use factories for:**
- Simple service instantiation (use DI instead)
- Objects with no construction logic
- Bypassing dependency injection

```kotlin
// ❌ AVOID: Factory for simple services
interface ZaakServiceFactory {
    fun createZaakService(): ZaakService  // Just use @Inject!
}

// ✅ PREFER: Direct dependency injection
@ApplicationScoped
class MyService @Inject constructor(
    private val zaakService: ZaakService  // Simple!
)
```

### 4. Additional Best Practices

#### Immutability
```kotlin
// ✅ PREFER: Immutable data classes
data class Zaak(
    val identificatie: ZaakIdentificatie,
    val omschrijving: String,
    val status: ZaakStatus
) {
    // Use copy() for "updates"
    fun withNewStatus(newStatus: ZaakStatus): Zaak = copy(status = newStatus)
}

// ❌ AVOID: Mutable state
data class Zaak(
    val identificatie: ZaakIdentificatie,
    var omschrijving: String,  // Mutable!
    var status: ZaakStatus      // Mutable!
)
```

#### Null Safety
```kotlin
// ✅ Use nullable types explicitly
fun findZaak(uuid: UUID): Zaak?  // Clear: might not exist

// ✅ Use Elvis operator for defaults
val name = zaak.naam ?: "Onbekend"

// ✅ Use safe call operator
val email = zaak.initiator?.emailAdres?.value

// ❌ AVOID: Force unwrapping
val name = zaak.naam!!  // Can crash!
```

#### Method Naming
```kotlin
// ✅ Clear intent
fun findZaakByIdentificatie(id: ZaakIdentificatie): Zaak?
fun createZaak(data: ZaakCreateData): Zaak
fun updateZaakStatus(zaakId: UUID, status: ZaakStatus): Zaak
fun validateBsn(bsn: Bsn): Boolean

// ❌ AVOID: Vague names
fun doStuff(data: Any): Any
fun process(input: String): String
fun handle(request: Request): Response
```

## Package Structure Convention

### For New Kotlin Code

```
nl.info.zac/
├── {domain}/                           # e.g. zaak, task, admin, note
│   ├── api/                            # Presentation Layer (REST)
│   │   ├── {Feature}RestService.kt
│   │   ├── model/
│   │   │   ├── Rest{Entity}.kt
│   │   │   └── Rest{Entity}Converter.kt
│   │   └── validation/
│   ├── application/                    # Application Layer
│   │   ├── {Feature}Service.kt
│   │   ├── {Feature}CommandHandler.kt
│   │   └── {Feature}QueryHandler.kt
│   ├── domain/                         # Domain Layer
│   │   ├── model/
│   │   │   ├── {Entity}.kt            # Domain entities
│   │   │   └── {ValueObject}.kt       # Value objects
│   │   ├── service/
│   │   │   └── {Domain}Service.kt     # Domain services
│   │   ├── repository/
│   │   │   └── {Entity}Repository.kt  # Port (interface)
│   │   ├── event/
│   │   │   └── {Domain}Event.kt
│   │   └── exception/
│   │       └── {Domain}Exception.kt
│   └── infrastructure/                 # Infrastructure Layer
│       ├── persistence/
│       │   └── {Entity}RepositoryImpl.kt
│       ├── client/
│       │   └── {External}Client.kt
│       └── messaging/
│
├── shared/                             # Shared kernel
│   ├── domain/
│   │   └── model/                     # Shared value objects
│   ├── infrastructure/
│   │   └── config/
│   └── util/
│
└── {crosscutting}/                    # Cross-cutting concerns
    ├── authentication/
    ├── policy/                         # Authorization
    ├── logging/
    └── metrics/
```

### Migration Strategy for Existing Code

Given the existing structure, we use a **gradual migration** approach:

1. **Keep current package names** for now to avoid massive refactoring
2. **Apply layer separation within existing packages**:

```
nl.info.zac/
├── app/                               # PRESENTATION LAYER
│   ├── {domain}/
│   │   ├── {Feature}RestService.kt   # REST endpoints
│   │   ├── model/                     # REST DTOs
│   │   └── converter/                 # DTO converters
│
├── {domain}/                          # APPLICATION + DOMAIN LAYER
│   ├── {Feature}Service.kt           # Application service (use case orchestrator)
│   ├── model/                         # Domain entities
│   ├── exception/                     # Domain exceptions
│   └── util/                          # Domain utilities
│
└── {domain}.infrastructure/           # INFRASTRUCTURE LAYER (new)
    ├── persistence/
    └── client/
```

## Layer Responsibilities and Rules

### 1. Presentation Layer (`app/`)

**Responsibilities:**
- Handle HTTP requests/responses
- Input validation (format, not business rules)
- DTO conversion to/from domain models
- **Coarse-grained authorization** (endpoint access control)
- Error handling and HTTP status mapping

**Rules:**
- ✅ CAN depend on: Application layer, shared utilities
- ❌ CANNOT depend on: Infrastructure layer directly
- ❌ CANNOT contain: Business logic, direct database access, external API calls
- ✅ MUST: Use constructor injection
- ✅ MUST: Document with KDoc
- ✅ SHOULD: Perform coarse-grained authorization checks

**Naming Conventions:**
- Services: `{Feature}RestService`
- Models: `Rest{Entity}`, `Rest{Entity}Update`, `Rest{Entity}Create`
- Converters: `Rest{Entity}Converter` or extension functions `to{Entity}()`, `toRest{Entity}()`

**Authorization in Presentation Layer:**

```kotlin
@Path("referentietabellen")
class ReferenceTableRestService @Inject constructor(
    private val referenceTableService: ReferenceTableService,
    private val policyService: PolicyService
) {
    @GET
    fun listReferenceTables(): List<RestReferenceTable> {
        // ✅ COARSE-GRAINED: Can user access admin functionality?
        assertPolicy(policyService.readOverigeRechten().beheren)
        
        // Service handles fine-grained authorization
        return referenceTableService.listReferenceTables()
            .map { it.toRestReferenceTable(false) }
    }
    
    @POST
    fun createReferenceTable(restReferenceTable: RestReferenceTable): RestReferenceTable {
        // ✅ COARSE-GRAINED: Role-based access control
        assertPolicy(policyService.readOverigeRechten().beheren)
        
        return referenceTableService.createReferenceTable(
            restReferenceTable.toReferenceTable()
        ).toRestReferenceTable(true)
    }
}
```

### 2. Application Layer (Service classes in domain packages)

**Responsibilities:**
- Orchestrate business workflows (use cases)
- Define transaction boundaries
- Coordinate between domain services
- Handle cross-aggregate operations
- **Fine-grained authorization** (instance and state-based)
- **Business rule enforcement**
- Trigger domain events

**Rules:**
- ✅ CAN depend on: Domain layer, infrastructure ports (interfaces), PolicyService
- ❌ CANNOT depend on: Presentation layer, infrastructure implementations
- ❌ CANNOT contain: HTTP/REST concerns, DTO mapping
- ✅ MUST: Be transactional where appropriate
- ✅ MUST: Use constructor injection
- ✅ MUST: Perform fine-grained authorization for instance-specific operations

**Naming Conventions:**
- Services: `{Feature}Service`
- Example: `ZaakService`, `TaskService`, `ReferenceTableService`

**Authorization in Application Layer:**

```kotlin
@ApplicationScoped
class ZaakService @Inject constructor(
    private val zrcClient: ZrcClient,
    private val policyService: PolicyService,
    private val ztcClientService: ZtcClientService
) {
    @Transactional
    fun updateZaak(uuid: UUID, updates: ZaakUpdates): Zaak {
        val zaak = zrcClient.readZaak(uuid)
        
        // ✅ FINE-GRAINED: Can user modify THIS SPECIFIC zaak?
        val zaakRechten = policyService.readZaakRechten(zaak)
        assertPolicy(zaakRechten.wijzigen)
        
        // ✅ BUSINESS RULE: State validation
        if (!zaak.isOpen) {
            throw BusinessRuleException("Cannot modify closed zaak")
        }
        
        // Apply updates...
        return zrcClient.updateZaak(uuid, updates)
    }
    
    @Transactional
    fun closeZaak(zaakId: UUID, resultaat: ZaakResultaat) {
        val zaak = zrcClient.readZaak(zaakId)
        
        // ✅ FINE-GRAINED: Instance-specific authorization
        val zaakRechten = policyService.readZaakRechten(zaak)
        assertPolicy(zaakRechten.afhandelen)
        
        // ✅ BUSINESS RULE: Validate state transition
        if (!zaak.isOpen) {
            throw BusinessRuleException("Zaak is already closed")
        }
        
        // Close zaak...
    }
}
```

**Complex Authorization Example:**

```kotlin
@ApplicationScoped
class ReferenceTableAdminService @Inject constructor(
    private val entityManager: EntityManager,
    private val referenceTableService: ReferenceTableService
) {
    @Transactional
    fun deleteReferenceTable(id: Long) {
        val referenceTable = referenceTableService.readReferenceTable(id)
        
        // ✅ BUSINESS RULE + AUTHORIZATION: System tables cannot be deleted
        // This is both a business constraint and a security rule
        if (referenceTable.isSystemReferenceTable) {
            throw InputValidationFailedException(
                ERROR_CODE_SYSTEM_REFERENCE_TABLE_CANNOT_BE_DELETED
            )
        }
        
        // ✅ BUSINESS RULE: Check if table is in use
        if (isInUse(referenceTable)) {
            throw InputValidationFailedException(
                ERROR_CODE_REFERENCE_TABLE_IS_IN_USE
            )
        }
        
        entityManager.remove(referenceTable)
    }
}
```

### 3. Domain Layer (`model/` subdirectories)

**Responsibilities:**
- Contain business logic and rules
- Define domain entities and value objects
- Define domain services for complex logic
- Define repository interfaces (ports)
- Raise domain events

**Rules:**
- ✅ CAN depend on: Nothing (pure domain)
- ❌ CANNOT depend on: Any other layer
- ❌ CANNOT contain: Framework annotations (except JPA where unavoidable), authorization logic
- ✅ MUST: Be framework-agnostic where possible
- ✅ PREFER: Immutable value objects
- ✅ PREFER: Data classes for entities
- ✅ **STRONGLY PREFER: Value objects over primitive types for domain concepts**

**Naming Conventions:**
- Entities: `{Entity}` (e.g. `ReferenceTable`, `Zaak`)
- Value Objects: `{Name}` (e.g. `EmailAddress`, `ZaakIdentificatie`, `Bsn`)
- Repositories: `{Entity}Repository` (interface)
- Domain Services: `{Domain}DomainService` (if needed to distinguish)

#### Value Objects for Type Safety

**Use value objects instead of primitive types for domain concepts:**

```kotlin
// ❌ AVOID: Primitive obsession - easy to mix up parameters
fun createZaak(
    identificatie: String,  // What kind of string?
    omschrijving: String,   // Easy to swap these!
    bsn: String,            // No validation
    email: String           // Could be any string
): Zaak

// ✅ PREFER: Value objects - type-safe, self-validating
fun createZaak(
    identificatie: ZaakIdentificatie,  // Clear intent
    omschrijving: Omschrijving,        // Cannot be swapped
    bsn: Bsn,                          // Validated in constructor
    email: EmailAdres                  // Guaranteed valid
): Zaak
```

**Implementing Value Objects:**

```kotlin
package nl.info.zac.zaak.model

import jakarta.validation.constraints.Pattern

// ═════════════════════════════════════════════════════════════
// Simple Value Object (inline class for performance)
// ═════════════════════════════════════════════════════════════
@JvmInline
value class ZaakIdentificatie(val value: String) {
    init {
        require(value.isNotBlank()) { "Zaak identificatie cannot be blank" }
        require(value.length <= 40) { "Zaak identificatie cannot exceed 40 characters" }
    }
    
    override fun toString(): String = value
}

// ═════════════════════════════════════════════════════════════
// Value Object with validation and business logic
// ═════════════════════════════════════════════════════════════
@JvmInline
value class Bsn(val value: String) {
    init {
        require(value.matches(Regex("^\\d{9}$"))) { 
            "BSN must be exactly 9 digits" 
        }
        require(isValid11ProefCijfer(value)) { 
            "BSN has invalid check digit" 
        }
    }
    
    private fun isValid11ProefCijfer(bsn: String): Boolean {
        val digits = bsn.map { it.digitToInt() }
        val sum = digits.take(8).mapIndexed { index, digit -> 
            digit * (9 - index) 
        }.sum() - digits[8]
        return sum % 11 == 0
    }
    
    // ✅ Can add domain-specific behavior
    fun isMasked(): Boolean = value == "000000000"
    
    // ✅ Safe conversion for logging (masked)
    fun toMaskedString(): String = "***${value.takeLast(3)}"
    
    override fun toString(): String = value
}

// ═════════════════════════════════════════════════════════════
// Email Value Object
// ═════════════════════════════════════════════════════════════
@JvmInline
value class EmailAdres(val value: String) {
    init {
        require(value.isNotBlank()) { "Email address cannot be blank" }
        require(EMAIL_PATTERN.matches(value)) { 
            "Invalid email address format" 
        }
    }
    
    companion object {
        private val EMAIL_PATTERN = Regex(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )
    }
    
    fun domain(): String = value.substringAfter('@')
    
    override fun toString(): String = value
}

// ═════════════════════════════════════════════════════════════
// Complex Value Object (data class)
// ═════════════════════════════════════════════════════════════
data class Bedrag(
    val waarde: BigDecimal,
    val valuta: Valuta = Valuta.EUR
) {
    init {
        require(waarde >= BigDecimal.ZERO) { 
            "Amount cannot be negative" 
        }
    }
    
    // ✅ Value objects can have business logic
    operator fun plus(other: Bedrag): Bedrag {
        require(this.valuta == other.valuta) { 
            "Cannot add amounts with different currencies" 
        }
        return Bedrag(this.waarde + other.waarde, this.valuta)
    }
    
    fun isZero(): Boolean = waarde == BigDecimal.ZERO
    
    override fun toString(): String = "$valuta $waarde"
}

enum class Valuta { EUR, USD, GBP }

// ═════════════════════════════════════════════════════════════
// Identifier Value Object (UUID wrapper)
// ═════════════════════════════════════════════════════════════
@JvmInline
value class ZaakId(val value: UUID) {
    companion object {
        fun random() = ZaakId(UUID.randomUUID())
        fun fromString(value: String) = ZaakId(UUID.fromString(value))
    }
    
    override fun toString(): String = value.toString()
}
```

**Using Value Objects in Domain Entities:**

```kotlin
// ✅ Entity with value objects
@Entity
class Zaak(
    @Embedded
    val identificatie: ZaakIdentificatie,
    
    val omschrijving: String,  // Simple string is OK here
    
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "initiator_bsn"))
    val initiatorBsn: Bsn?,
    
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "contact_email"))
    val contactEmail: EmailAdres?
) {
    @Id
    val id: ZaakId = ZaakId.random()
    
    // ✅ Type-safe method signatures
    fun updateContactEmail(newEmail: EmailAdres) {
        // Cannot accidentally pass BSN or other string here!
        this.contactEmail = newEmail
    }
}
```

**REST Layer Conversion:**

```kotlin
package nl.info.zac.app.zaak.model

// REST DTO uses primitives for JSON serialization
data class RestZaakCreate(
    @field:NotBlank
    @field:Size(max = 40)
    val identificatie: String,
    
    @field:Pattern(regexp = "^\\d{9}$")
    val initiatorBsn: String?,
    
    @field:Email
    val contactEmail: String?
)

// Converter transforms to domain value objects
fun RestZaakCreate.toZaak(): Zaak {
    return Zaak(
        identificatie = ZaakIdentificatie(this.identificatie),
        omschrijving = this.omschrijving,
        initiatorBsn = this.initiatorBsn?.let { Bsn(it) },  // ✅ Validated here
        contactEmail = this.contactEmail?.let { EmailAdres(it) }
    )
}

// Converter from domain to REST
fun Zaak.toRest(): RestZaak {
    return RestZaak(
        identificatie = this.identificatie.value,  // ✅ Extract primitive
        initiatorBsn = this.initiatorBsn?.value,
        contactEmail = this.contactEmail?.value
    )
}
```

**Benefits of Value Objects:**

1. **Type Safety**
   ```kotlin
   // ❌ Can accidentally swap parameters
   createZaak(email, bsn)  // Compiles but wrong!
   
   // ✅ Compiler catches mistakes
   createZaak(EmailAdres(email), Bsn(bsn))  // Type error if swapped
   ```

2. **Single Validation Point**
   ```kotlin
   // ❌ Validation scattered everywhere
   fun createZaak(bsn: String) {
       require(bsn.matches(Regex("^\\d{9}$")))
       // ... validation duplicated in many places
   }
   
   // ✅ Validation in one place
   val bsn = Bsn("123456782")  // Validated in constructor
   createZaak(bsn)  // No validation needed - guaranteed valid
   ```

3. **Self-Documenting Code**
   ```kotlin
   // ❌ What kind of string?
   fun notify(recipient: String, message: String)
   
   // ✅ Crystal clear intent
   fun notify(recipient: EmailAdres, message: String)
   ```

4. **Encapsulated Business Logic**
   ```kotlin
   val total = Bedrag(100.00) + Bedrag(50.00)  // Type-safe operations
   if (total.isZero()) { /* ... */ }
   ```

5. **Impossible to Represent Invalid State**
   ```kotlin
   // ❌ Can create invalid BSN
   val bsn: String = "abc123"  // Compiles!
   
   // ✅ Cannot create invalid BSN
   val bsn = Bsn("abc123")  // Throws IllegalArgumentException
   ```

**When to Use Value Objects:**

✅ **DO create value objects for:**
- Identifiers (BSN, KvK number, zaak nummer)
- Email addresses, phone numbers
- Money/currency amounts
- Dates/periods with business meaning
- Quantities with units (weight, distance)
- Codes with validation rules
- Anything with format constraints

❌ **DON'T create value objects for:**
- Simple descriptions/text without constraints
- Generic strings that are just data
- When primitive type is clear enough (e.g., `age: Int`)

**Kotlin Inline Classes for Performance:**

```kotlin
// ✅ Inline class (no runtime overhead)
@JvmInline
value class ZaakIdentificatie(val value: String)

// At runtime, this is equivalent to using String directly
// but with compile-time type safety!
```

### 4. Infrastructure Layer (`nl.info.client/` and new `infrastructure/`)

**Responsibilities:**
- Implement repository interfaces
- External API clients
- Database access
- File system operations
- Message broker connections

**Rules:**
- ✅ CAN depend on: Domain layer (interfaces only)
- ❌ CANNOT depend on: Presentation layer, Application layer
- ✅ MUST: Implement ports defined in domain
- ✅ MUST: Use constructor injection

**Naming Conventions:**
- Repository Implementations: `{Entity}RepositoryImpl` or `Jpa{Entity}Repository`
- Clients: `{System}Client` (e.g. `ZgwClient`, `BrpClient`)

## Authorization Architecture

### Two-Layer Authorization Strategy

ZAC implements a **defense-in-depth** authorization strategy with two layers:

```
┌─────────────────────────────────────────────────────────┐
│  REST Controller (Presentation Layer)                   │
│  ✓ Coarse-grained authorization                         │
│    - Endpoint access control                            │
│    - Role-based access (RBAC)                           │
│    - Feature flags                                      │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│  Service (Application Layer)                            │
│  ✓ Fine-grained authorization                           │
│    - Instance-specific permissions                      │
│    - State-dependent authorization                      │
│    - Business rule enforcement                          │
└─────────────────────────────────────────────────────────┘
```

### Authorization Decision Matrix

| Authorization Type | Location | Example | Rationale |
|-------------------|----------|---------|-----------|
| **Endpoint access** | Presentation Layer | `readOverigeRechten().beheren` | Fast fail, clear security boundary |
| **Feature capability** | Presentation Layer | `readWerklijstRechten().documentCreeren` | General permission checking |
| **Instance-specific** | Application Layer | `readZaakRechten(zaak).wijzigen` | Requires loading entity first |
| **State-dependent** | Application Layer | `if (zaak.isOpen && rechten.afhandelen)` | Business logic + authorization |
| **Business rules** | Application/Domain Layer | `if (referenceTable.isSystemTable)` | Core business constraint |

### Authorization Examples

#### ✅ Coarse-Grained (Presentation Layer)

```kotlin
@Path("zaken")
class ZaakRestService @Inject constructor(
    private val zaakService: ZaakService,
    private val policyService: PolicyService
) {
    @GET
    @Path("werkvoorraad")
    fun listWerkvoorraad(): List<RestZaak> {
        // ✅ Can user access work queue feature?
        assertPolicy(policyService.readWerklijstRechten().inbox)
        
        return zaakService.listWerkvoorraadZaken()
            .map { it.toRest() }
    }
    
    @POST
    fun createZaak(restZaak: RestZaakCreate): RestZaak {
        // ✅ Can user create zaken in general?
        assertPolicy(policyService.readWerklijstRechten().zakenCreeren)
        
        return zaakService.createZaak(restZaak.toDomain())
            .toRest()
    }
}
```

#### ✅ Fine-Grained (Application Layer)

```kotlin
@ApplicationScoped
class ZaakService @Inject constructor(
    private val zrcClient: ZrcClient,
    private val policyService: PolicyService
) {
    @Transactional
    fun assignZaak(zaakId: UUID, userId: String) {
        val zaak = zrcClient.readZaak(zaakId)
        
        // ✅ Can user modify THIS zaak?
        val zaakRechten = policyService.readZaakRechten(zaak)
        assertPolicy(zaakRechten.toekennen)
        
        // ✅ Business rule
        if (!zaak.isOpen) {
            throw BusinessRuleException("Cannot assign closed zaak")
        }
        
        // Assign logic...
    }
    
    @Transactional
    fun deleteZaak(zaakId: UUID) {
        val zaak = zrcClient.readZaak(zaakId)
        
        // ✅ Instance-specific permission
        val zaakRechten = policyService.readZaakRechten(zaak)
        assertPolicy(zaakRechten.verwijderen)
        
        // ✅ Business constraint
        if (zaak.hasDocuments()) {
            throw BusinessRuleException("Cannot delete zaak with documents")
        }
        
        zrcClient.deleteZaak(zaakId)
    }
}
```

#### ✅ Combined Authorization (Best Practice)

```kotlin
// PRESENTATION LAYER
@PATCH
@Path("{uuid}")
fun updateZaak(
    @PathParam("uuid") uuid: UUID,
    updates: RestZaakUpdates
): RestZaak {
    // ✅ LAYER 1: Coarse-grained - fast fail
    assertPolicy(policyService.readWerklijstRechten().zakenWijzigen)
    
    // ✅ LAYER 2: Fine-grained - in service
    return zaakService.updateZaak(uuid, updates.toDomain())
        .toRest()
}

// APPLICATION LAYER
@Transactional
fun updateZaak(uuid: UUID, updates: ZaakUpdates): Zaak {
    val zaak = zrcClient.readZaak(uuid)
    
    // ✅ LAYER 2: Fine-grained authorization
    val zaakRechten = policyService.readZaakRechten(zaak)
    assertPolicy(zaakRechten.wijzigen)
    
    // ✅ Business rules
    validateZaakUpdates(zaak, updates)
    
    return zrcClient.updateZaak(uuid, updates)
}
```

### When NOT to Duplicate Authorization

Some endpoints don't need coarse-grained checks if:
1. They're read-only and publicly accessible
2. All authorization is instance-specific
3. The endpoint is used by internal services

```kotlin
@GET
@Path("code/{code}")
fun readReferenceTableByCode(@PathParam("code") code: String) =
    // ✅ No coarse-grained check: publicly accessible for BPMN forms
    // Note: Consider proper authorization with PABC in future
    referenceTableService.readReferenceTable(code).toRestReferenceTable(true)
```

### Authorization Anti-Patterns

#### ❌ Business Logic in Controller

```kotlin
// DON'T DO THIS
@DELETE
@Path("{id}")
fun deleteReferenceTable(@PathParam("id") id: Long) {
    assertPolicy(policyService.readOverigeRechten().beheren)
    
    // ❌ Business logic belongs in service
    val table = referenceTableService.readReferenceTable(id)
    if (table.isSystemReferenceTable) {
        throw ValidationException("Cannot delete system table")
    }
    
    referenceTableService.deleteReferenceTable(id)
}
```

#### ❌ Loading Entity Twice

```kotlin
// DON'T DO THIS
@PATCH
@Path("{uuid}")
fun updateZaak(@PathParam("uuid") uuid: UUID, updates: RestZaakUpdates): RestZaak {
    // ❌ Loading zaak for authorization check
    val zaak = zaakService.findZaak(uuid)
    val zaakRechten = policyService.readZaakRechten(zaak)
    assertPolicy(zaakRechten.wijzigen)
    
    // ❌ Loading zaak again in service
    return zaakService.updateZaak(uuid, updates.toDomain())
        .toRest()
}

// DO THIS INSTEAD: Let service handle both loading and authorization
@PATCH
@Path("{uuid}")
fun updateZaak(@PathParam("uuid") uuid: UUID, updates: RestZaakUpdates): RestZaak {
    assertPolicy(policyService.readWerklijstRechten().zakenWijzigen)
    return zaakService.updateZaak(uuid, updates.toDomain()).toRest()
}
```

#### ❌ No Authorization in Service (Reusability Issue)

```kotlin
// DON'T DO THIS
@ApplicationScoped
class ZaakService {
    // ❌ No authorization - relies on controller to check
    fun deleteZaak(zaakId: UUID) {
        val zaak = zrcClient.readZaak(zaakId)
        zrcClient.deleteZaak(zaakId)
    }
}

// Problem: If another service calls this, authorization is bypassed!
class BatchService {
    fun cleanupOldZaken() {
        oldZaken.forEach { zaakService.deleteZaak(it.id) }  // ❌ No auth check!
    }
}
```

## Exception Handling Architecture

### Exception Hierarchy Strategy

ZAC uses a **three-tier exception hierarchy** that balances specificity with reusability:

```kotlin
┌─────────────────────────────────────────────────────────┐
│  Tier 1: Base Exceptions                               │
│  - Generic categories (shared package)                 │
│  - BusinessRuleViolationException                      │
│  - ValidationException                                 │
│  - ResourceNotFoundException                           │
│  - PolicyException                                     │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│  Tier 2: Domain-Specific Exceptions                    │
│  - Extend base exceptions                              │
│  - Located in domain exception packages                │
│  - ZaakClosedException                                 │
│  - SystemReferenceTableException                       │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│  Tier 3: Operation-Specific (when needed)              │
│  - Very specific use cases                             │
│  - Contains additional context/behavior                │
│  - BetrokkeneAlreadyAddedException                     │
└─────────────────────────────────────────────────────────┘
```

### Base Exception Classes

**Define in `nl.info.zac.exception` package:**

```kotlin
// Business rule violations (domain constraints)
open class BusinessRuleViolationException(
    val errorCode: ErrorCode,
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

// Input validation failures
open class ValidationException(
    val errorCode: ErrorCode,
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

// Resource not found (404)
open class ResourceNotFoundException(
    val resourceType: String,
    val identifier: String,
    message: String? = "$resourceType not found: $identifier"
) : RuntimeException(message)

// Authorization denied (403) - already exists
class PolicyException(
    message: String? = "Authorization denied"
) : RuntimeException(message)
```

### Domain-Specific Exceptions

**Define in domain exception packages (e.g., `nl.info.zac.zaak.exception`):**

```kotlin
// Zaak domain
package nl.info.zac.zaak.exception

class ZaakNotFoundException(zaakId: UUID) : 
    ResourceNotFoundException("Zaak", zaakId.toString())

class ZaakClosedException(val zaakId: UUID) : 
    BusinessRuleViolationException(
        errorCode = ErrorCode.ERROR_CODE_ZAAK_IS_CLOSED,
        message = "Zaak $zaakId is already closed"
    )

class ZaakHasOpenTasksException(
    val zaakId: UUID,
    val openTasks: List<Task>
) : BusinessRuleViolationException(
    errorCode = ErrorCode.ERROR_CODE_ZAAK_HAS_OPEN_TASKS,
    message = "Cannot close zaak $zaakId: has ${openTasks.size} open tasks"
) {
    fun getOpenTaskIds() = openTasks.map { it.id }
}

// Reference table domain
package nl.info.zac.admin.exception

class ReferenceTableNotFoundException(id: Long) : 
    ResourceNotFoundException("ReferenceTable", id.toString())

class SystemReferenceTableException(val code: String) :
    BusinessRuleViolationException(
        errorCode = ErrorCode.ERROR_CODE_SYSTEM_REFERENCE_TABLE_CANNOT_BE_DELETED,
        message = "System reference table '$code' cannot be modified"
    )

class ReferenceTableInUseException(
    val id: Long,
    val usedBy: List<String>
) : BusinessRuleViolationException(
    errorCode = ErrorCode.ERROR_CODE_REFERENCE_TABLE_IS_IN_USE,
    message = "Reference table $id is used by: ${usedBy.joinToString()}"
)
```

### When to Create Specific Exception Classes

#### ✅ Create Specific Exception When:

1. **Represents a distinct business rule**
   ```kotlin
   class ZaakAlreadyClosedException(zaakId: UUID) : BusinessRuleViolationException(...)
   class DocumentAlreadySignedException(documentId: UUID) : BusinessRuleViolationException(...)
   ```

2. **Requires specific handling in calling code**
   ```kotlin
   try {
       zaakService.closeZaak(zaakId)
   } catch (e: ZaakAlreadyClosedException) {
       // OK - zaak is already closed, return current state
       return zaak.toRest()
   } catch (e: ZaakHasOpenTasksException) {
       // Show user which tasks are open
       return Response.status(CONFLICT)
           .entity(mapOf("openTasks" to e.openTasks))
           .build()
   }
   ```

3. **Contains domain-specific data or behavior**
   ```kotlin
   class ZaakHasOpenTasksException(
       val zaakId: UUID,
       val openTasks: List<Task>
   ) : BusinessRuleViolationException(...) {
       fun canAutoCloseAnyTasks() = openTasks.any { it.isAutoCloseable }
       fun getCriticalTasks() = openTasks.filter { it.isCritical }
   }
   ```

4. **Needs different HTTP status mapping**
   ```kotlin
   ResourceNotFoundException        // → 404 Not Found
   BusinessRuleViolationException  // → 400 Bad Request
   ConcurrentModificationException // → 409 Conflict
   PolicyException                 // → 403 Forbidden
   ```

#### ❌ Use Generic Exception When:

1. **Simple validation with error code is sufficient**
   ```kotlin
   // ✅ Good use of generic exception
   if (referenceTable.code.isBlank()) {
       throw ValidationException(ERROR_CODE_REFERENCE_TABLE_CODE_REQUIRED)
   }
   ```

2. **No special handling needed beyond standard error response**
3. **No domain-specific behavior or additional context required**
4. **One-off validation that won't be reused**

### Exception Naming Conventions

```kotlin
// ✅ Good names (describe the problem)
class ZaakClosedException           // State violation
class DocumentLockedException        // Resource state
class UserNotAuthorizedException     // Authorization failure
class InvalidEmailFormatException    // Validation failure
class ReferenceTableInUseException  // Constraint violation

// ❌ Avoid (too generic or unclear)
class ZaakException                  // What went wrong?
class ErrorException                 // Redundant
class Exception123                   // Meaningless
class GeneralException               // Too vague
```

**Naming Pattern:** `{Subject}{Problem}Exception`
- Subject: The entity or concept (Zaak, Document, User)
- Problem: What's wrong (Closed, Locked, NotFound, Invalid)

### Type Aliases: When NOT to Use

**❌ DON'T use type aliases as exception replacements:**

```kotlin
// ❌ DON'T: Loses semantic meaning and can't be caught separately
typealias ZaakClosedException = BusinessRuleViolationException

// ✅ DO: Create proper exception class
class ZaakClosedException(zaakId: UUID) : 
    BusinessRuleViolationException(...)

// ✅ OK: Type alias for complex result types
typealias ValidationResult<T> = Result<T, ValidationException>
```

**Rationale:**
- Type aliases don't create new types (just aliases)
- Can't be caught separately in try-catch blocks
- Lose domain-specific constructors and behavior
- Less discoverable in IDE autocomplete
- Can't add methods or properties

### Exception Usage Examples

#### Application Layer Example

```kotlin
@ApplicationScoped
class ReferenceTableAdminService @Inject constructor(
    private val entityManager: EntityManager,
    private val referenceTableService: ReferenceTableService
) {
    @Transactional
    fun deleteReferenceTable(id: Long) {
        val referenceTable = referenceTableService.readReferenceTable(id)
        
        // ✅ Specific, self-documenting exception
        if (referenceTable.isSystemReferenceTable) {
            throw SystemReferenceTableException(referenceTable.code)
        }
        
        // ✅ Exception with additional context
        val usedBy = findUsages(referenceTable)
        if (usedBy.isNotEmpty()) {
            throw ReferenceTableInUseException(
                id = referenceTable.id!!,
                usedBy = usedBy
            )
        }
        
        entityManager.remove(referenceTable)
    }
}

@ApplicationScoped
class ZaakService @Inject constructor(
    private val zrcClient: ZrcClient,
    private val taskService: TaskService
) {
    @Transactional
    fun closeZaak(zaakId: UUID) {
        val zaak = zrcClient.readZaak(zaakId)
            ?: throw ZaakNotFoundException(zaakId)
        
        // ✅ State validation with specific exception
        if (!zaak.isOpen) {
            throw ZaakClosedException(zaakId)
        }
        
        // ✅ Business rule with context
        val openTasks = taskService.findOpenTasksForZaak(zaakId)
        if (openTasks.isNotEmpty()) {
            throw ZaakHasOpenTasksException(zaakId, openTasks)
        }
        
        // Close zaak...
    }
}
```

#### Presentation Layer Exception Handling

```kotlin
@Path("referentietabellen")
class ReferenceTableRestService @Inject constructor(
    private val referenceTableService: ReferenceTableService,
    private val referenceTableAdminService: ReferenceTableAdminService
) {
    @DELETE
    @Path("{id}")
    fun deleteReferenceTable(@PathParam("id") id: Long): Response {
        return try {
            referenceTableAdminService.deleteReferenceTable(id)
            Response.noContent().build()
        } catch (e: SystemReferenceTableException) {
            // ✅ Specific handling: provide helpful message
            Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf(
                    "message" to "System reference tables cannot be deleted",
                    "code" to e.code
                ))
                .build()
        } catch (e: ReferenceTableInUseException) {
            // ✅ Specific handling: show where it's used
            Response.status(Response.Status.CONFLICT)
                .entity(mapOf(
                    "message" to "Reference table is in use",
                    "usedBy" to e.usedBy
                ))
                .build()
        }
        // Note: In practice, RestExceptionMapper handles most exceptions
    }
}
```

### Exception Mapper Integration

The `RestExceptionMapper` automatically maps exceptions to HTTP responses:

```kotlin
@Provider
class RestExceptionMapper : ExceptionMapper<Exception> {
    override fun toResponse(exception: Exception): Response =
        when (exception) {
            // Specific exceptions
            is PolicyException -> generateResponse(
                responseStatus = Response.Status.FORBIDDEN,
                errorCode = ERROR_CODE_FORBIDDEN,
                exception = exception
            )
            
            // Base exception with error code
            is BusinessRuleViolationException -> generateResponse(
                responseStatus = Response.Status.BAD_REQUEST,
                errorCode = exception.errorCode,
                exception = exception
            )
            
            is ValidationException -> generateResponse(
                responseStatus = Response.Status.BAD_REQUEST,
                errorCode = exception.errorCode,
                exception = exception
            )
            
            is ResourceNotFoundException -> generateResponse(
                responseStatus = Response.Status.NOT_FOUND,
                errorCode = ERROR_CODE_RESOURCE_NOT_FOUND,
                exception = exception
            )
            
            // ... other mappings
        }
}
```

### Testing with Specific Exceptions

```kotlin
class ReferenceTableAdminServiceTest : FreeSpec({
    "deleteReferenceTable" - {
        "should throw SystemReferenceTableException when deleting system table" {
            // ✅ Specific assertion
            val exception = shouldThrow<SystemReferenceTableException> {
                service.deleteReferenceTable(systemTableId)
            }
            exception.code shouldBe "ADVIES"
        }
        
        "should throw ReferenceTableInUseException with usage details" {
            // ✅ Can assert on exception properties
            val exception = shouldThrow<ReferenceTableInUseException> {
                service.deleteReferenceTable(tableInUseId)
            }
            exception.usedBy shouldContain "ZaaktypeConfiguration:uuid-123"
        }
    }
})
```

### Benefits of Specific Exceptions

1. **Self-Documenting Code**
   - `throw SystemReferenceTableException(code)` is clearer than `throw InputValidationFailedException(ERROR_CODE_123)`
   
2. **Type-Safe Catching**
   - Can catch specific exceptions without checking error codes
   - Compiler ensures all catch blocks are valid
   
3. **Better IDE Support**
   - Autocomplete shows available exceptions
   - "Find usages" tracks exception flows
   - Refactoring tools work properly
   
4. **Domain Context Preservation**
   - Exceptions can carry domain-specific data
   - Can add domain-specific methods
   
5. **Improved Testing**
   - Can assert on specific exception types
   - Can verify exception properties
   - More maintainable test code

### Exception Strategy Decision Matrix

| Scenario | Exception Type | Example |
|----------|----------------|----------|
| Resource not found | Specific `ResourceNotFoundException` subclass | `ReferenceTableNotFoundException(id)` |
| Business rule violation | Specific `BusinessRuleViolationException` subclass | `SystemReferenceTableException(code)` |
| State constraint | Specific domain exception | `ZaakClosedException(zaakId)` |
| Simple validation | Generic with error code | `ValidationException(ERROR_CODE)` |
| Authorization denied | `PolicyException` | Already exists |
| Concurrent modification | Specific exception | `OptimisticLockException(entity)` |
| External API failure | Infrastructure exception | `ZgwRuntimeException` (existing) |

## Input Validation Architecture

### Current Validation Status

ZAC **partially implements** Jakarta Bean Validation (Hibernate Validator) with:
- ✅ Jakarta Validation API (`@Valid`, `@NotNull`, `@NotBlank`, etc.)
- ✅ Hibernate Validator (version 8.0.3.Final - provided by WildFly)
- ✅ Custom validators for complex business rules
- ⚠️ **Inconsistent usage** across REST endpoints
- ❌ **Not leveraging Kotlin-specific validation features**

### Validation Strategy: Three-Layer Approach

```kotlin
┌─────────────────────────────────────────────────────────┐
│  Layer 1: Format Validation (Presentation)            │
│  - Bean Validation annotations (@NotNull, @Size)       │
│  - Format constraints (@Email, @Pattern)               │
│  - Type safety (handled by Kotlin)                     │
│  - Custom validators for complex formats               │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│  Layer 2: Business Rules (Application)                │
│  - State validation                                    │
│  - Cross-field validation                              │
│  - Business constraints                                │
│  - Authorization checks                                │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│  Layer 3: Domain Invariants (Domain)                   │
│  - Entity integrity rules                              │
│  - Value object validation                             │
│  - Domain constraints                                  │
└─────────────────────────────────────────────────────────┘
```

### Bean Validation in Presentation Layer

#### Standard Jakarta Validation Annotations

```kotlin
package nl.info.zac.app.admin.model

import jakarta.validation.Valid
import jakarta.validation.constraints.*

data class RestReferenceTableCreate(
    // ✅ Format validation: not blank
    @field:NotBlank(message = "Reference table code is required")
    @field:Size(min = 2, max = 50, message = "Code must be between 2 and 50 characters")
    @field:Pattern(regexp = "^[A-Z_]+$", message = "Code must be uppercase with underscores")
    val code: String,
    
    // ✅ Format validation: not blank
    @field:NotBlank(message = "Reference table name is required")
    @field:Size(max = 100, message = "Name must not exceed 100 characters")
    val naam: String,
    
    // ✅ Nested validation
    @field:Valid
    @field:NotEmpty(message = "At least one value is required")
    val waarden: List<RestReferenceTableValue>
)

data class RestReferenceTableValue(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
    
    @field:Min(1)
    @field:Max(9999)
    val sortOrder: Int = 1
)
```

#### Enabling Validation in REST Endpoints

```kotlin
@Path("referentietabellen")
@AllOpen  // Required for @Valid to work with Kotlin
class ReferenceTableRestService @Inject constructor(
    private val referenceTableService: ReferenceTableService
) {
    @POST
    fun createReferenceTable(
        @Valid restReferenceTable: RestReferenceTableCreate  // ✅ Triggers validation
    ): RestReferenceTable {
        return referenceTableService
            .createReferenceTable(restReferenceTable.toDomain())
            .toRest()
    }
    
    @PUT
    @Path("{id}")
    fun updateReferenceTable(
        @PathParam("id") id: Long,
        @Valid @NotNull restUpdate: RestReferenceTableUpdate  // ✅ Multiple annotations
    ): RestReferenceTable {
        return referenceTableService
            .updateReferenceTable(id, restUpdate.toDomain())
            .toRest()
    }
}
```

### Common Validation Annotations

| Annotation | Purpose | Example |
|------------|---------|----------|
| `@NotNull` | Value must not be null | `@NotNull val id: Long?` |
| `@NotBlank` | String must not be null/empty/whitespace | `@NotBlank val code: String` |
| `@NotEmpty` | Collection/String must not be null/empty | `@NotEmpty val values: List<String>` |
| `@Size(min, max)` | String/Collection size | `@Size(min=2, max=50) val code: String` |
| `@Min(value)` | Number minimum | `@Min(1) val count: Int` |
| `@Max(value)` | Number maximum | `@Max(100) val percent: Int` |
| `@Email` | Valid email format | `@Email val email: String` |
| `@Pattern(regexp)` | Regex match | `@Pattern(regexp="^[A-Z]+$") val code: String` |
| `@Past` | Date in the past | `@Past val birthDate: LocalDate` |
| `@Future` | Date in the future | `@Future val deadline: LocalDate` |
| `@Valid` | Cascade validation | `@Valid val address: Address` |

### Custom Validators for Complex Rules

#### Creating Custom Validators

```kotlin
package nl.info.zac.app.zaak.model

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

// ═══════════════════════════════════════════════════════════════
// 1. Define the annotation
// ═══════════════════════════════════════════════════════════════
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [BetrokkeneIdentificatieValidator::class])
annotation class ValidBetrokkeneIdentificatie(
    val message: String = "Invalid betrokkene identificatie",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

// ═══════════════════════════════════════════════════════════════
// 2. Implement the validator
// ═══════════════════════════════════════════════════════════════
class BetrokkeneIdentificatieValidator : 
    ConstraintValidator<ValidBetrokkeneIdentificatie, BetrokkeneIdentificatie> {
    
    override fun isValid(
        value: BetrokkeneIdentificatie?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (value == null) return true  // Use @NotNull separately for null checks
        
        // ✅ Complex cross-field validation
        return when (value.type) {
            IdentificatieType.BSN -> {
                !value.bsnNummer.isNullOrBlank() &&
                value.kvkNummer.isNullOrBlank() &&
                value.vestigingsnummer.isNullOrBlank()
            }
            IdentificatieType.VN -> {
                !value.kvkNummer.isNullOrBlank() &&
                !value.vestigingsnummer.isNullOrBlank() &&
                value.bsnNummer.isNullOrBlank()
            }
            IdentificatieType.RSIN -> {
                !value.kvkNummer.isNullOrBlank() &&
                value.bsnNummer.isNullOrBlank() &&
                value.vestigingsnummer.isNullOrBlank()
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 3. Apply the annotation
// ═══════════════════════════════════════════════════════════════
@ValidBetrokkeneIdentificatie  // ✅ Class-level validation
data class BetrokkeneIdentificatie(
    @field:NotNull
    val type: IdentificatieType,
    val bsnNummer: String? = null,
    val kvkNummer: String? = null,
    val rsin: String? = null,
    val vestigingsnummer: String? = null
)
```

#### Custom Validator with Custom Error Messages

```kotlin
class ZaakIdentificatieValidator : 
    ConstraintValidator<ValidZaakIdentificatie, RestZaakCreate> {
    
    override fun isValid(
        value: RestZaakCreate?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (value == null) return true
        
        // Disable default message
        context.disableDefaultConstraintViolation()
        
        // ✅ Specific error messages for different violations
        if (value.zaaktypeUuid == null) {
            context.buildConstraintViolationWithTemplate(
                "Zaaktype is required"
            )
            .addPropertyNode("zaaktypeUuid")
            .addConstraintViolation()
            return false
        }
        
        if (value.startdatum?.isAfter(value.einddatumGepland) == true) {
            context.buildConstraintViolationWithTemplate(
                "Start date must be before planned end date"
            )
            .addPropertyNode("einddatumGepland")
            .addConstraintViolation()
            return false
        }
        
        return true
    }
}
```

### Kotlin-Specific Validation Considerations

#### Nullable Types and Validation

```kotlin
// ✅ CORRECT: Validate nullable Kotlin properties
data class RestZaakUpdate(
    // Nullable but when provided, must not be blank
    @field:NotBlank
    val reden: String? = null,
    
    // Not nullable, so automatically required (Kotlin null-safety)
    @field:Size(max = 100)
    val omschrijving: String,
    
    // Nullable list that when provided must not be empty
    @field:NotEmpty
    @field:Valid
    val documenten: List<RestDocument>? = null
)

// ❌ AVOID: Making everything nullable just for validation
data class RestZaakUpdate(
    val reden: String?,  // ❌ Unclear if required or not
    val omschrijving: String?,  // ❌ Loses Kotlin null-safety benefits
)
```

#### AllOpen Plugin for Validation

**Required for Jakarta Validation with Kotlin:**

```kotlin
// ✅ Required: Classes must be open for validation proxies
@AllOpen
@NoArgConstructor
data class RestReferenceTableUpdate(
    @field:NotBlank
    var naam: String,
    
    @field:Valid
    var waarden: List<RestReferenceTableValue> = emptyList()
)

// ❌ Without @AllOpen, validation may not work correctly
data class RestReferenceTableUpdate(  // Final class - proxy issues!
    @field:NotBlank
    val naam: String
)
```

### Validation Groups (Advanced)

```kotlin
// Define validation groups
interface CreateValidation
interface UpdateValidation

data class RestZaak(
    // Only required on create
    @field:NotNull(groups = [CreateValidation::class])
    val zaaktypeUuid: UUID?,
    
    // Only required on update
    @field:NotNull(groups = [UpdateValidation::class])
    val id: UUID?,
    
    // Required in both cases
    @field:NotBlank(groups = [CreateValidation::class, UpdateValidation::class])
    val omschrijving: String
)

// Use groups in REST endpoints
@POST
fun createZaak(
    @Valid(groups = [CreateValidation::class]) zaak: RestZaak
): RestZaak {
    // Only CreateValidation constraints are checked
}

@PUT
@Path("{id}")
fun updateZaak(
    @PathParam("id") id: UUID,
    @Valid(groups = [UpdateValidation::class]) zaak: RestZaak
): RestZaak {
    // Only UpdateValidation constraints are checked
}
```

### Business Rule Validation (Application Layer)

**Format validation belongs in REST models, business rules in services:**

```kotlin
@ApplicationScoped
class ReferenceTableService @Inject constructor(
    private val entityManager: EntityManager
) {
    fun createReferenceTable(referenceTable: ReferenceTable): ReferenceTable {
        // ✅ Business rule: check for duplicate codes
        findReferenceTableByCode(referenceTable.code)?.let {
            throw ReferenceTableAlreadyExistsException(referenceTable.code)
        }
        
        // ✅ Business rule: validate value sort orders are unique
        val sortOrders = referenceTable.values.map { it.sortOrder }
        if (sortOrders.size != sortOrders.toSet().size) {
            throw ValidationException(
                ERROR_CODE_REFERENCE_TABLE_DUPLICATE_SORT_ORDERS,
                "Sort orders must be unique"
            )
        }
        
        return entityManager.persist(referenceTable)
    }
}
```

### Validation Error Handling

Jakarta Validation errors are automatically caught and mapped to HTTP 400:

```kotlin
// RestExceptionMapper already handles validation exceptions
@Provider
class RestExceptionMapper : ExceptionMapper<Exception> {
    override fun toResponse(exception: Exception): Response =
        when (exception) {
            // ✅ Automatic handling of Bean Validation errors
            is ConstraintViolationException -> {
                val violations = exception.constraintViolations.map {
                    mapOf(
                        "field" to it.propertyPath.toString(),
                        "message" to it.message
                    )
                }
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(mapOf("violations" to violations))
                    .build()
            }
            // ... other exceptions
        }
}
```

### Migration to Better Validation

#### Before (Manual Validation)

```kotlin
// ❌ Manual validation in REST endpoint
@POST
fun createReferenceTable(restReferenceTable: RestReferenceTable): RestReferenceTable {
    // Manual checks - error prone, verbose
    if (restReferenceTable.code.isBlank()) {
        throw ValidationException(ERROR_CODE_EMPTY_CODE)
    }
    if (restReferenceTable.code.length > 50) {
        throw ValidationException(ERROR_CODE_CODE_TOO_LONG)
    }
    if (!restReferenceTable.code.matches(Regex("^[A-Z_]+$"))) {
        throw ValidationException(ERROR_CODE_INVALID_CODE_FORMAT)
    }
    
    return referenceTableService.createReferenceTable(
        restReferenceTable.toReferenceTable()
    ).toRestReferenceTable()
}
```

#### After (Bean Validation)

```kotlin
// ✅ Declarative validation in model
data class RestReferenceTableCreate(
    @field:NotBlank(message = "Code is required")
    @field:Size(max = 50, message = "Code must not exceed 50 characters")
    @field:Pattern(regexp = "^[A-Z_]+$", message = "Code must be uppercase with underscores")
    val code: String,
    
    @field:NotBlank
    val naam: String
)

// ✅ Clean REST endpoint
@POST
fun createReferenceTable(
    @Valid restReferenceTable: RestReferenceTableCreate  // Validation happens automatically
): RestReferenceTable {
    return referenceTableService
        .createReferenceTable(restReferenceTable.toDomain())
        .toRest()
}
```

### Best Practices Summary

#### ✅ DO:

1. **Use `@Valid` on REST endpoint parameters**
   ```kotlin
   fun createEntity(@Valid entity: RestEntityCreate): RestEntity
   ```

2. **Apply `@AllOpen` to validated data classes**
   ```kotlin
   @AllOpen
   data class RestEntity(...)
   ```

3. **Use `@field:` prefix for Kotlin properties**
   ```kotlin
   @field:NotBlank  // ✅
   val name: String
   ```

4. **Separate format validation (REST) from business rules (Service)**
   - Format: `@NotBlank`, `@Size`, `@Pattern` in REST models
   - Business: State checks, duplicates, constraints in services

5. **Create custom validators for complex cross-field validation**
   ```kotlin
   @ValidBetrokkeneIdentificatie
   data class BetrokkeneIdentificatie(...)
   ```

6. **Use Kotlin null-safety instead of `@NotNull` where possible**
   ```kotlin
   val required: String  // ✅ Non-nullable = required
   val optional: String? = null  // ✅ Nullable = optional
   ```

#### ❌ DON'T:

1. **Don't do format validation in service layer**
   ```kotlin
   // ❌ Belongs in REST model validation
   if (code.isBlank()) throw ValidationException(...)
   ```

2. **Don't forget `@Valid` on REST parameters**
   ```kotlin
   // ❌ Validation won't happen
   fun create(entity: RestEntity): RestEntity
   ```

3. **Don't use validation annotations without `@AllOpen`**
   ```kotlin
   // ❌ May not work correctly
   data class RestEntity(@field:NotBlank val name: String)
   ```

4. **Don't validate business rules with Bean Validation**
   ```kotlin
   // ❌ Business logic doesn't belong in validators
   @Constraint(validatedBy = [CheckDatabaseForDuplicateValidator::class])
   ```

### Validation Checklist

When adding/updating REST endpoints:

- [ ] Add `@Valid` to REST endpoint parameters
- [ ] Add `@AllOpen` and `@NoArgConstructor` to validated classes
- [ ] Use `@field:` prefix on Kotlin validation annotations
- [ ] Apply format validation annotations (`@NotBlank`, `@Size`, etc.)
- [ ] Create custom validators for complex cross-field rules
- [ ] Keep business rule validation in service layer
- [ ] Leverage Kotlin null-safety (avoid nullable types when not needed)
- [ ] Add meaningful error messages to validation annotations
- [ ] Test validation with invalid inputs
- [ ] Document validation rules in KDoc

## Input Sanitization and Security

### Current Security Posture

ZAC has **strong security foundations** for preventing common vulnerabilities:

#### ✅ SQL Injection Protection
- **JPA Criteria API**: All database queries use type-safe Criteria API
- **Parameterized Queries**: When using JPQL, parameters are properly bound
- **EntityManager**: No raw SQL or string concatenation in queries
- **❌ No Native Queries**: Native SQL should be avoided

#### ✅ Framework-Level Security
- **Jakarta EE Security**: Built-in security features
- **WildFly Security**: Container-managed security
- **Keycloak Integration**: OAuth2/OIDC authentication
- **Open Policy Agent**: Policy-based authorization

#### ⚠️ Areas Requiring Attention
- **XSS Protection**: Frontend Angular handles most, but backend validation needed
- **Path Traversal**: File operations need validation
- **SSRF Prevention**: External URL calls should be validated
- **Content Type Validation**: File uploads need strict checking

### SQL Injection Prevention

#### ✅ ALWAYS Use JPA Criteria API or Parameterized Queries

```kotlin
// ✅ CORRECT: JPA Criteria API (type-safe, injection-proof)
@ApplicationScoped
class ReferenceTableService @Inject constructor(
    private val entityManager: EntityManager
) {
    fun findReferenceTable(code: String): ReferenceTable? {
        val criteriaBuilder = entityManager.criteriaBuilder
        val query = criteriaBuilder.createQuery(ReferenceTable::class.java)
        val root = query.from(ReferenceTable::class.java)
        
        // ✅ Parameters are type-safe and properly escaped
        val predicate = criteriaBuilder.equal(
            root.get<String>("code"),
            code.uppercase()
        )
        
        query.select(root).where(predicate)
        return entityManager.createQuery(query).resultList.firstOrNull()
    }
}

// ✅ CORRECT: JPQL with named parameters
fun findByCode(code: String): ReferenceTable? {
    return entityManager.createQuery(
        "SELECT r FROM ReferenceTable r WHERE r.code = :code",
        ReferenceTable::class.java
    )
    .setParameter("code", code)  // ✅ Parameterized, safe from injection
    .resultList
    .firstOrNull()
}

// ❌ NEVER DO THIS: String concatenation (SQL injection vulnerability!)
fun findByCodeUNSAFE(code: String): ReferenceTable? {
    // ❌ DANGEROUS! User input directly in query string
    val query = "SELECT r FROM ReferenceTable r WHERE r.code = '$code'"
    return entityManager.createQuery(query, ReferenceTable::class.java)
        .resultList
        .firstOrNull()
}

// ❌ NEVER DO THIS: Native SQL with string concatenation
fun findByCodeNativeUNSAFE(code: String): ReferenceTable? {
    // ❌ EXTREMELY DANGEROUS!
    val sql = "SELECT * FROM referentie_tabel WHERE code = '$code'"
    return entityManager.createNativeQuery(sql, ReferenceTable::class.java)
        .resultList
        .firstOrNull()
}
```

#### JPA Criteria API Best Practices

```kotlin
// ✅ Complex queries with multiple parameters
fun searchZaken(
    omschrijving: String?,
    status: String?,
    startDatum: LocalDate?
): List<Zaak> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Zaak::class.java)
    val root = query.from(Zaak::class.java)
    
    val predicates = mutableListOf<Predicate>()
    
    // ✅ Each parameter is safely handled
    omschrijving?.let {
        predicates.add(cb.like(
            cb.lower(root.get("omschrijving")),
            "%${it.lowercase()}%"  // Still safe with Criteria API
        ))
    }
    
    status?.let {
        predicates.add(cb.equal(root.get<String>("status"), it))
    }
    
    startDatum?.let {
        predicates.add(cb.greaterThanOrEqualTo(
            root.get("startdatum"),
            it
        ))
    }
    
    query.select(root).where(*predicates.toTypedArray())
    return entityManager.createQuery(query).resultList
}
```

### XSS (Cross-Site Scripting) Prevention

#### Frontend Protection (Angular)

Angular automatically sanitizes data in templates:

```typescript
// ✅ Angular automatically escapes HTML
<div>{{ zaak.omschrijving }}</div>  <!-- Safe, auto-escaped -->

// ⚠️ Bypassing sanitization (use with extreme caution)
<div [innerHTML]="zaak.omschrijving"></div>  <!-- Potentially unsafe -->
```

#### Backend Validation

**Validate and sanitize rich text input:**

```kotlin
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.SimpleHtmlSerializer

// ✅ Sanitize HTML input to allow only safe tags
fun sanitizeHtml(input: String): String {
    val cleaner = HtmlCleaner()
    val props = cleaner.properties.apply {
        // Allow only safe HTML tags
        allowedHtmlTags = setOf("p", "br", "strong", "em", "ul", "ol", "li")
    }
    
    val rootNode = cleaner.clean(input)
    return SimpleHtmlSerializer(props).getAsString(rootNode)
}

// Use in validation
@ValidateHtmlContent
data class RestNoteCreate(
    @field:NotBlank
    val tekst: String  // Will be sanitized before storage
)
```

#### Content Security Policy (CSP)

**Configure in application headers:**

```kotlin
// In REST response filters or security configuration
@Provider
class SecurityHeadersFilter : ContainerResponseFilter {
    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext
    ) {
        responseContext.headers.apply {
            // ✅ Prevent XSS attacks
            add("Content-Security-Policy", 
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'")
            add("X-Content-Type-Options", "nosniff")
            add("X-Frame-Options", "DENY")
            add("X-XSS-Protection", "1; mode=block")
        }
    }
}
```

### Path Traversal Prevention

```kotlin
import java.nio.file.Paths
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.startsWith

// ✅ Validate file paths to prevent directory traversal
class FileService {
    private val allowedBaseDir = Paths.get("/var/zac/uploads").toAbsolutePath()
    
    fun readFile(fileName: String): ByteArray {
        // ✅ Validate and sanitize file path
        val requestedPath = allowedBaseDir.resolve(fileName).normalize().toAbsolutePath()
        
        // ✅ Ensure path is within allowed directory
        if (!requestedPath.startsWith(allowedBaseDir)) {
            throw SecurityException("Path traversal attempt detected: $fileName")
        }
        
        // ✅ Additional validation: no suspicious patterns
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw ValidationException(
                ERROR_CODE_INVALID_FILENAME,
                "Invalid filename: $fileName"
            )
        }
        
        return requestedPath.toFile().readBytes()
    }
}

// ❌ NEVER DO THIS: Direct path concatenation
fun readFileUNSAFE(fileName: String): ByteArray {
    // ❌ DANGEROUS! User could provide "../../etc/passwd"
    val path = "/var/zac/uploads/$fileName"
    return File(path).readBytes()
}
```

### File Upload Security

```kotlin
data class FileUploadValidator(
    private val maxFileSize: Long = 10 * 1024 * 1024, // 10MB
    private val allowedContentTypes: Set<String> = setOf(
        "application/pdf",
        "image/jpeg",
        "image/png",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    )
) {
    fun validateFileUpload(file: InputStream, filename: String, contentType: String) {
        // ✅ Validate file size
        if (file.available() > maxFileSize) {
            throw ValidationException(
                ERROR_CODE_FILE_TOO_LARGE,
                "File exceeds maximum size of ${maxFileSize / 1024 / 1024}MB"
            )
        }
        
        // ✅ Validate content type
        if (contentType !in allowedContentTypes) {
            throw ValidationException(
                ERROR_CODE_INVALID_FILE_TYPE,
                "File type '$contentType' is not allowed"
            )
        }
        
        // ✅ Validate file extension
        val extension = filename.substringAfterLast('.', "")
        val allowedExtensions = setOf("pdf", "jpg", "jpeg", "png", "doc", "docx")
        if (extension.lowercase() !in allowedExtensions) {
            throw ValidationException(
                ERROR_CODE_INVALID_FILE_EXTENSION,
                "File extension '$extension' is not allowed"
            )
        }
        
        // ✅ Validate filename doesn't contain path separators
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw ValidationException(
                ERROR_CODE_INVALID_FILENAME,
                "Invalid filename"
            )
        }
        
        // ✅ ADVANCED: Validate actual file content matches declared type
        // This prevents attackers from renaming malicious files
        val actualContentType = detectContentType(file)
        if (actualContentType != contentType) {
            throw ValidationException(
                ERROR_CODE_FILE_TYPE_MISMATCH,
                "File content does not match declared type"
            )
        }
    }
    
    private fun detectContentType(file: InputStream): String {
        // Use Apache Tika or similar to detect actual content type
        return Files.probeContentType(file)
    }
}
```

### SSRF (Server-Side Request Forgery) Prevention

```kotlin
import java.net.URL
import java.net.InetAddress

// ✅ Validate external URLs before making requests
class UrlValidator {
    private val allowedSchemes = setOf("http", "https")
    private val blockedNetworks = setOf(
        "127.0.0.0/8",      // Localhost
        "10.0.0.0/8",       // Private network
        "172.16.0.0/12",    // Private network
        "192.168.0.0/16",   // Private network
        "169.254.0.0/16",   // Link-local
        "::1/128",          // IPv6 localhost
        "fc00::/7"          // IPv6 private
    )
    
    fun validateUrl(urlString: String) {
        val url = try {
            URL(urlString)
        } catch (e: Exception) {
            throw ValidationException(ERROR_CODE_INVALID_URL, "Invalid URL format")
        }
        
        // ✅ Validate protocol
        if (url.protocol !in allowedSchemes) {
            throw ValidationException(
                ERROR_CODE_INVALID_URL_SCHEME,
                "URL scheme '${url.protocol}' is not allowed"
            )
        }
        
        // ✅ Resolve hostname and check against blocked networks
        val address = InetAddress.getByName(url.host)
        if (isBlockedAddress(address)) {
            throw ValidationException(
                ERROR_CODE_BLOCKED_URL,
                "Access to internal network addresses is not allowed"
            )
        }
    }
    
    private fun isBlockedAddress(address: InetAddress): Boolean {
        // Check if address is in any blocked network
        return address.isLoopbackAddress || 
               address.isLinkLocalAddress ||
               address.isSiteLocalAddress
    }
}

// Usage in external API calls
@ApplicationScoped
class ExternalServiceClient {
    fun fetchFromExternalService(url: String): String {
        // ✅ Validate URL before making request
        UrlValidator().validateUrl(url)
        
        // Make the request
        return httpClient.get(url)
    }
}
```

### Input Validation for Special Characters

```kotlin
// ✅ Validate and sanitize input that will be used in various contexts
object InputSanitizer {
    
    // For code/identifiers (alphanumeric + underscore only)
    fun sanitizeCode(input: String): String {
        return input.replace(Regex("[^A-Za-z0-9_]"), "")
    }
    
    // For email addresses
    fun sanitizeEmail(email: String): String {
        return email.trim().lowercase()
    }
    
    // For search queries (remove special SQL/Solr characters)
    fun sanitizeSearchQuery(query: String): String {
        return query
            .replace(Regex("[&|!(){}\\[\\]^\"~*?:]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
    
    // For file names
    fun sanitizeFilename(filename: String): String {
        return filename
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(255)  // Maximum filename length
    }
}
```

### Security Headers for REST Responses

```kotlin
@Provider
class SecurityHeadersFilter : ContainerResponseFilter {
    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext
    ) {
        responseContext.headers.apply {
            // ✅ Prevent MIME type sniffing
            add("X-Content-Type-Options", "nosniff")
            
            // ✅ Prevent clickjacking
            add("X-Frame-Options", "DENY")
            
            // ✅ Enable XSS protection
            add("X-XSS-Protection", "1; mode=block")
            
            // ✅ Referrer policy
            add("Referrer-Policy", "strict-origin-when-cross-origin")
            
            // ✅ Content Security Policy
            add("Content-Security-Policy", "default-src 'self'")
            
            // ✅ Permissions policy
            add("Permissions-Policy", "geolocation=(), microphone=(), camera=()")
            
            // ✅ HSTS (if using HTTPS)
            add("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        }
    }
}
```

### Security Best Practices Summary

#### ✅ DO:

1. **Always use JPA Criteria API or parameterized queries**
   - Never concatenate user input into SQL strings
   
2. **Validate all file operations**
   - Check paths for traversal attempts
   - Validate file types and sizes
   - Verify actual content matches declared type
   
3. **Sanitize HTML input**
   - Use HtmlCleaner or similar for rich text
   - Configure allowlist of safe HTML tags
   
4. **Validate external URLs**
   - Block internal/private network access
   - Whitelist allowed protocols
   
5. **Add security headers**
   - CSP, X-Frame-Options, X-Content-Type-Options, HSTS
   
6. **Use Bean Validation**
   - Validate input format at API boundary
   
7. **Implement rate limiting**
   - Prevent brute force attacks (consider for public endpoints)
   
8. **Sanitize special characters**
   - Especially for search queries, file names, codes

#### ❌ DON'T:

1. **Don't use string concatenation for queries**
   ```kotlin
   // ❌ NEVER
   val query = "SELECT * FROM table WHERE id = $id"
   ```
   
2. **Don't trust file extensions**
   ```kotlin
   // ❌ Check actual content type, not just extension
   ```
   
3. **Don't bypass sanitization**
   ```kotlin
   // ❌ In Angular: [innerHTML] without sanitization
   ```
   
4. **Don't allow arbitrary file uploads**
   ```kotlin
   // ❌ Must validate type, size, and content
   ```
   
5. **Don't trust user input in file paths**
   ```kotlin
   // ❌ Always validate and normalize paths
   ```
   
6. **Don't make requests to untrusted URLs**
   ```kotlin
   // ❌ Validate URLs to prevent SSRF
   ```

### Security Checklist

When implementing features:

- [ ] Database queries use Criteria API or parameterized JPQL (never string concatenation)
- [ ] File paths validated against traversal attacks
- [ ] File uploads validated for type, size, and actual content
- [ ] External URLs validated against SSRF (no internal network access)
- [ ] Rich text input sanitized for XSS
- [ ] Security headers configured (CSP, X-Frame-Options, etc.)
- [ ] Input validation with Bean Validation
- [ ] Authorization checks implemented (coarse + fine-grained)
- [ ] Error messages don't leak sensitive information
- [ ] Rate limiting considered for public endpoints
- [ ] Special characters sanitized for context (search, filenames, etc.)

## Dependency Rules (The Dependency Rule)

```
Presentation → Application → Domain ← Infrastructure
     ↓              ↓           ↑           ↑
     └──────────────┴───────────┴───────────┘
          All depend on Domain (via interfaces)
```

**Key Principle:** Dependencies point inward. The domain has no outward dependencies.

## Special Packages

### Cross-Cutting Concerns
Located at root level, these are exceptions to layer rules:
- `authentication/` - Authentication filters and context
- `policy/` - Authorization logic (PolicyService, OPA evaluation)
- `configuratie/` - Configuration services
- `exception/` - Global exception handling
- `util/` - Utility classes

### Shared Kernel
- `shared/` - Code shared across bounded contexts
  - Keep minimal
  - Changes require coordination across teams

## Testing Strategy per Layer

### Presentation Layer Tests
- Unit tests with mocked application services
- Integration tests with TestContainers
- Test REST contracts, DTOs, validation
- Test coarse-grained authorization

### Application Layer Tests
- Unit tests with mocked infrastructure
- Test use case orchestration
- Test transaction boundaries
- Test fine-grained authorization logic
- Test business rule enforcement

### Domain Layer Tests
- Pure unit tests (no mocks needed)
- Test business rules in isolation
- Fast, no framework dependencies

### Infrastructure Layer Tests
- Integration tests with real dependencies
- TestContainers for databases
- WireMock for external APIs

## Refactoring Existing Code

When working with existing code that doesn't yet follow these architectural guidelines, refer to the **[Migration Guide](./migration-guide.md)** for:

- **Practical refactoring checklists** covering all aspects (layer separation, validation, security, etc.)
- **Common refactoring patterns** with before/after code examples
- **Step-by-step workflow** for safe refactoring
- **When NOT to refactor** guidelines

**Quick checklist summary:**
1. Layer Separation - Extract business logic from REST controllers
2. Exception Handling - Replace generic exceptions with domain-specific ones
3. Input Validation - Add Bean Validation to REST models
4. Value Objects - Replace primitives with type-safe value objects
5. Code Quality - Constructor injection, immutability, single responsibility
6. Security - SQL injection, XSS, SSRF, file upload validation
7. Testing - Match tests to layer responsibilities

## Architecture Decision Records (ADRs)

For significant architectural decisions, create ADRs in `docs/architecture/adr/`:
- ADR format: `NNNN-title-of-decision.md`
- Include: Context, Decision, Consequences
- Keep history: Don't delete superseded ADRs

## References

- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Defense in Depth - OWASP](https://owasp.org/www-community/Defense_in_depth)
