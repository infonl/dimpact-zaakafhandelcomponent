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

class LogFieldNamesTest : BehaviorSpec({
    val synonymsOfStandardFieldNames = setOf(
        "zaakId", "zaakUUID", "zaakID", "zaakIdentification", "uuid", "user", "username", "userId", "userName",
        "medewerker", "behandelaarId", "groepId", "groupID", "taskId", "informatieobjectUUID", "duration",
        "durationMillis", "exceptionType", "traceID", "spanID", "event", "requestID", "correlationId"
    )

    fun Class<*>.fieldsNamedAfterASynonym() =
        declaredFields
            .filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) && it.name in synonymsOfStandardFieldNames }
            .map { "${name}.${it.name}" }

    context("The field names of all log events") {
        given("all log events in the production code of ZAC") {
            val logEventClasses = ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("nl.info", "net.atos")
                .filter { it.isAssignableTo(LogEvent::class.java) && it.reflect() != LogEvent::class.java }
                .map { it.reflect() }

            `when`("their fields are checked against the synonyms of the standard field names") {
                val fieldsNamedAfterASynonym = logEventClasses.flatMap { it.fieldsNamedAfterASynonym() }

                then("no event names a field after a synonym") {
                    fieldsNamedAfterASynonym.shouldBeEmpty()
                }
            }
        }

        given("an event with a field named after a synonym of a standard field name") {
            class FakeEventWithSynonym(val zaakId: String, val groupId: String) : LogEvent("Fake event with synonym")

            `when`("its fields are checked against the synonyms of the standard field names") {
                val fieldsNamedAfterASynonym = FakeEventWithSynonym::class.java.fieldsNamedAfterASynonym()

                then("the field named after the synonym is reported with its event") {
                    fieldsNamedAfterASynonym shouldContainExactly listOf(
                        "${FakeEventWithSynonym::class.java.name}.zaakId"
                    )
                }
            }
        }
    }
})
