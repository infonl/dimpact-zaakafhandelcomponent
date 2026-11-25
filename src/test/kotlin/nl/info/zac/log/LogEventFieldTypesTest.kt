/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

private val ALLOWED_VALUE_TYPES = setOf(
    String::class.java,
    Int::class.java, Int::class.javaObjectType,
    Long::class.java, Long::class.javaObjectType,
    Boolean::class.java, Boolean::class.javaObjectType,
    UUID::class.java,
    URI::class.java,
    LocalDate::class.java,
    ZonedDateTime::class.java,
    Employee::class.java
)

private val ALLOWED_CONTAINER_TYPES = setOf(List::class.java, Set::class.java, Collection::class.java, Map::class.java)

// The declared generic type keeps the type arguments that erasure removes from the value, so the elements of a
// collection and the keys and values of a map are checked too.
private fun Type.isSupported(): Boolean =
    when (this) {
        is Class<*> -> isEnum || this in ALLOWED_VALUE_TYPES
        is ParameterizedType -> rawType in ALLOWED_CONTAINER_TYPES && actualTypeArguments.all { it.isSupported() }
        is WildcardType -> upperBounds.all { it.isSupported() }
        else -> false
    }

private fun Class<*>.fieldsOfAnUnsupportedType() =
    declaredFields
        .filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }
        .filterNot { it.genericType.isSupported() }
        .map { "$name.${it.name}" }

class LogEventFieldTypesTest : BehaviorSpec({
    context("The field types of all log events") {
        given("all log events in the production code of ZAC") {
            val logEventClasses = ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("nl.info", "net.atos")
                .filter { it.isAssignableTo(LogEvent::class.java) && it.reflect() != LogEvent::class.java }
                .map { it.reflect() }

            `when`("their field types are checked") {
                val fieldsOfAnUnsupportedType = logEventClasses.flatMap { it.fieldsOfAnUnsupportedType() }

                then(
                    "every field has a simple type, so that no object is written with its toString(), which would " +
                        "bypass the @Redacted fields inside it"
                ) {
                    fieldsOfAnUnsupportedType.shouldBeEmpty()
                }
            }
        }

        given("an event with fields that hold an object, directly or in a collection or map") {
            class FakeKlant(val naam: String)
            @Suppress("LongParameterList")
            class FakeEventWithObject(
                val klant: FakeKlant,
                val klanten: List<FakeKlant>,
                val klantenPerId: Map<String, FakeKlant>,
                val zaakUuid: UUID,
                val namen: List<String>,
                val outcomesPerZaaktype: Map<String, Set<Outcome>>
            ) : LogEvent("Fake event with object")

            `when`("its field types are checked") {
                val fieldsOfAnUnsupportedType = FakeEventWithObject::class.java.fieldsOfAnUnsupportedType()

                then(
                    "the fields that hold the object are reported with their event, and the collections and maps of " +
                        "simple types are not"
                ) {
                    fieldsOfAnUnsupportedType shouldContainExactly listOf(
                        "${FakeEventWithObject::class.java.name}.klant",
                        "${FakeEventWithObject::class.java.name}.klanten",
                        "${FakeEventWithObject::class.java.name}.klantenPerId"
                    )
                }
            }
        }
    }
})
