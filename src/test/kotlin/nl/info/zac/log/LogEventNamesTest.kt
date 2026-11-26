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

private fun Class<*>.namesInProgressiveForm() =
    listOfNotNull(simpleName.takeIf { it.endsWith("ing") }) +
        declaredClasses
            .filter { it.isEnum && it.simpleName == "Type" }
            .flatMap { type -> type.enumConstants.map { (it as Enum<*>).name } }
            .filter { it.endsWith("ING") }
            .map { "$simpleName.Type.$it" }

class LogEventNamesTest : BehaviorSpec({
    context("The names of all log events") {
        given("all log events in the production code of ZAC") {
            val logEventClasses = ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("nl.info", "net.atos")
                .filter { it.isAssignableTo(LogEvent::class.java) && it.reflect() != LogEvent::class.java }
                .map { it.reflect() }

            `when`("their names and type constants are checked") {
                val namesInProgressiveForm = logEventClasses.flatMap { it.namesInProgressiveForm() }

                then(
                    "no event and no type constant ends in -ing, because an event names what happened, such as " +
                        "ZaakSuspensionStarted, not an activity that is going on"
                ) {
                    namesInProgressiveForm.shouldBeEmpty()
                }
            }
        }

        given("an event and a type constant whose names end in -ing") {
            `when`("their names are checked") {
                val namesInProgressiveForm = FakeZaakSuspending::class.java.namesInProgressiveForm()

                then("the event and the type constant are reported") {
                    namesInProgressiveForm shouldContainExactly listOf(
                        "FakeZaakSuspending",
                        "FakeZaakSuspending.Type.ZAAK_SUSPENDING"
                    )
                }
            }
        }
    }
})
