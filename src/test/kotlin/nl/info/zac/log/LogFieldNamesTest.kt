/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import java.lang.reflect.Modifier

private val STANDARD_FIELD_NAMES: Set<String> = LogFieldNames::class.java.declaredFields
    .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
    .map { it.get(null) as String }
    .toSet()

private val SYNONYMS_OF_STANDARD_FIELD_NAMES = setOf(
    "uuid", "user", "username", "userId", "userName", "medewerker", "behandelaarId", "duration", "durationMillis",
    "exceptionType", "event", "correlationId"
)

private val ZAC_CONCEPTS = listOf("zaaktype", "zaak", "taak", "task", "informatieobject", "groep", "group", "besluittype", "rol")
private val IDENTIFYING_SUFFIXES = listOf("id", "uuid", "uri", "url", "identificatie", "identification", "nummer", "nr")
private val CONCEPT_WITH_IDENTIFYING_SUFFIX = Regex(
    "(${ZAC_CONCEPTS.joinToString("|")})(${IDENTIFYING_SUFFIXES.joinToString("|")})",
    RegexOption.IGNORE_CASE
)

private fun String.normalized() = lowercase().replace("_", "")

/**
 * The reason why [fieldName] is not allowed, or `null` when it is.
 */
private fun violationOf(fieldName: String): String? {
    val standardNameWithOtherSpelling = STANDARD_FIELD_NAMES.find {
        it != fieldName && it.normalized() == fieldName.normalized()
    }
    return when {
        standardNameWithOtherSpelling != null -> "spell it as '$standardNameWithOtherSpelling'"
        fieldName in SYNONYMS_OF_STANDARD_FIELD_NAMES -> "it is a synonym of a standard field name in LogFieldNames"
        CONCEPT_WITH_IDENTIFYING_SUFFIX.matches(fieldName) && fieldName !in STANDARD_FIELD_NAMES ->
            "it identifies a ZAC concept, so use a name from LogFieldNames or add one there"
        else -> null
    }
}

private fun Class<*>.fieldsWithANonStandardName() =
    declaredFields
        .filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }
        .mapNotNull { field -> violationOf(field.name)?.let { "$name.${field.name}: $it" } }

class LogFieldNamesTest : BehaviorSpec({
    context("The field names of all log events") {
        given("all log events in the production code of ZAC") {
            val logEventClasses = ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("nl.info", "net.atos")
                .filter { it.isAssignableTo(LogEvent::class.java) && it.reflect() != LogEvent::class.java }
                .map { it.reflect() }

            `when`("their field names are checked against the standard field names") {
                val fieldsWithANonStandardName = logEventClasses.flatMap { it.fieldsWithANonStandardName() }

                then("every field that names a shared concept uses its standard name from LogFieldNames") {
                    fieldsWithANonStandardName.shouldBeEmpty()
                }
            }
        }

        given("an event with a standard name in another spelling, a synonym and other names for a ZAC concept") {
            @Suppress("LongParameterList")
            class FakeEventWithNonStandardNames(
                val zaakidentificatie: String,
                val zaakuuid: String,
                val username: String,
                val zaakID: String,
                val zaakNr: String,
                val zaakUrl: String,
                val taskId: String,
                val groepId: String,
                val zaakIdentificatie: String,
                val zaakCount: Int,
                val taskName: String
            ) : LogEvent("Fake event with non-standard names")

            `when`("its field names are checked against the standard field names") {
                val fieldsWithANonStandardName = FakeEventWithNonStandardNames::class.java.fieldsWithANonStandardName()

                then("every non-standard name is reported with the reason, and the standard and specific names are not") {
                    val eventName = FakeEventWithNonStandardNames::class.java.name
                    val conceptMessage = "it identifies a ZAC concept, so use a name from LogFieldNames or add one there"
                    fieldsWithANonStandardName shouldContainExactlyInAnyOrder listOf(
                        "$eventName.zaakidentificatie: spell it as 'zaakIdentificatie'",
                        "$eventName.zaakuuid: spell it as 'zaakUuid'",
                        "$eventName.username: it is a synonym of a standard field name in LogFieldNames",
                        "$eventName.zaakID: $conceptMessage",
                        "$eventName.zaakNr: $conceptMessage",
                        "$eventName.zaakUrl: $conceptMessage",
                        "$eventName.taskId: $conceptMessage",
                        "$eventName.groepId: $conceptMessage"
                    )
                }
            }
        }
    }
})
