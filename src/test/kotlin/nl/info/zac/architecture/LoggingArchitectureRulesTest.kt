/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchRule
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.architecture.fixtures.JulLoggerFixture
import nl.info.zac.architecture.fixtures.PrintStackTraceFixture
import nl.info.zac.architecture.fixtures.PrintlnFixture
import nl.info.zac.architecture.fixtures.Slf4jLoggerFixture
import kotlin.reflect.KClass

class LoggingArchitectureRulesTest : BehaviorSpec({
    fun KClass<*>.isRejectedBy(rule: ArchRule) =
        rule.evaluate(ClassFileImporter().importClasses(java)).hasViolation()

    context("The logging rules") {
        given("a class that logs through a java.util.logging Logger") {
            `when`("the rules are checked") {
                then("the rule on other logging APIs rejects it") {
                    JulLoggerFixture::class.isRejectedBy(LoggingArchitectureRules.noOtherLoggingApi) shouldBe true
                }
            }
        }

        given("a class that logs through SLF4J") {
            `when`("the rules are checked") {
                then("the rule on other logging APIs rejects it") {
                    Slf4jLoggerFixture::class.isRejectedBy(LoggingArchitectureRules.noOtherLoggingApi) shouldBe true
                }
            }
        }

        given("a class that prints to the console") {
            `when`("the rules are checked") {
                then("the rule on console output rejects it") {
                    PrintlnFixture::class.isRejectedBy(LoggingArchitectureRules.noConsoleOutput) shouldBe true
                }
            }
        }

        given("a class that prints a stack trace") {
            `when`("the rules are checked") {
                then("the rule on console output rejects it") {
                    PrintStackTraceFixture::class.isRejectedBy(LoggingArchitectureRules.noConsoleOutput) shouldBe true
                }
            }
        }
    }
})
