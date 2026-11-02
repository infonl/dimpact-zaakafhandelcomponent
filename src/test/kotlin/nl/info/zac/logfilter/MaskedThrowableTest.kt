/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.logfilter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.PrintWriter
import java.io.StringWriter

class MaskedThrowableTest : BehaviorSpec({
    context("Masking a throwable") {
        given("an exception with a BSN in its message, in the message of its cause and in a suppressed exception") {
            val bsn = "999993896"
            val exception = IllegalStateException("Geen persoon gevonden voor BSN '$bsn'", RuntimeException("fakeCause $bsn"))
                .apply { addSuppressed(IllegalArgumentException("fakeSuppressed $bsn")) }

            `when`("the exception is masked") {
                val maskedThrowable = exception.toMaskedThrowable()

                then("the BSN is masked in the messages, while the class names and the stack trace stay the same") {
                    maskedThrowable.toString() shouldBe "java.lang.IllegalStateException: Geen persoon gevonden voor BSN '******896'"
                    maskedThrowable.stackTrace shouldBe exception.stackTrace
                    maskedThrowable.cause.toString() shouldBe "java.lang.RuntimeException: fakeCause ******896"
                    maskedThrowable.suppressed.single().toString() shouldBe "java.lang.IllegalArgumentException: fakeSuppressed ******896"
                }

                and("the printed stack trace does not contain the BSN") {
                    with(StringWriter().also { maskedThrowable.printStackTrace(PrintWriter(it)) }.toString()) {
                        this shouldContain "******896"
                        this shouldNotContain bsn
                    }
                }
            }
        }

        given("an exception without a message") {
            val exception = IllegalStateException()

            `when`("the exception is masked") {
                val maskedThrowable = exception.toMaskedThrowable()

                then("only the class name of the exception is printed") {
                    maskedThrowable.toString() shouldBe "java.lang.IllegalStateException"
                }
            }
        }

        given("an exception that is both the cause of another exception and suppressed by it") {
            val sharedException = RuntimeException("fakeShared 999993896")
            val exception = IllegalStateException("fakeMessage", sharedException).apply { addSuppressed(sharedException) }

            `when`("the exception is masked") {
                val maskedThrowable = exception.toMaskedThrowable()

                then("the shared exception is masked once and used for both, like in the original") {
                    (maskedThrowable.cause === maskedThrowable.suppressed.single()) shouldBe true
                }
            }
        }
    }
})
