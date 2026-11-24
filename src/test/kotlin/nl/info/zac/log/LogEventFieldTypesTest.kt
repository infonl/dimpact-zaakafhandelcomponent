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
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

private val ALLOWED_FIELD_TYPES = setOf(
    String::class.java,
    Int::class.java, Int::class.javaObjectType,
    Long::class.java, Long::class.javaObjectType,
    Boolean::class.java, Boolean::class.javaObjectType,
    UUID::class.java,
    URI::class.java,
    LocalDate::class.java,
    ZonedDateTime::class.java,
    Employee::class.java,
    List::class.java,
    Set::class.java,
    Collection::class.java,
    Map::class.java
)

private fun Class<*>.fieldsOfAnUnsupportedType() =
    declaredFields
        .filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }
        .filter { !it.type.isEnum && it.type !in ALLOWED_FIELD_TYPES }
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

        given("an event with a field that holds an object") {
            class FakeKlant(val naam: String)
            class FakeEventWithObject(val klant: FakeKlant, val zaakUuid: UUID) : LogEvent("Fake event with object")

            `when`("its field types are checked") {
                val fieldsOfAnUnsupportedType = FakeEventWithObject::class.java.fieldsOfAnUnsupportedType()

                then("the field that holds the object is reported with its event") {
                    fieldsOfAnUnsupportedType shouldContainExactly listOf(
                        "${FakeEventWithObject::class.java.name}.klant"
                    )
                }
            }
        }
    }
})
