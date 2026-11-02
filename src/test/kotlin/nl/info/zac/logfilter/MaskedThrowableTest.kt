/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.logfilter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.io.PrintWriter
import java.io.StringWriter

class MaskedThrowableTest : BehaviorSpec({
    context("Masking the personal data in a throwable") {
        given("an exception with a BSN in its message, in the message of its cause and in a suppressed exception") {
            val bsn = "999993896"
            val illegalStateException = IllegalStateException("Geen persoon gevonden voor BSN '$bsn'", RuntimeException("fakeCause $bsn"))
                .apply { addSuppressed(IllegalArgumentException("fakeSuppressed $bsn")) }

            `when`("the personal data is masked") {
                val maskedThrowable = illegalStateException.maskPersonalData()

                then("the BSN is masked in the messages, which start with the name of the original class") {
                    maskedThrowable.message shouldBe "java.lang.IllegalStateException: Geen persoon gevonden voor BSN '******896'"
                    maskedThrowable.cause?.message shouldBe "java.lang.RuntimeException: fakeCause ******896"
                    maskedThrowable.suppressed.single().message shouldBe "java.lang.IllegalArgumentException: fakeSuppressed ******896"
                }

                and("the stack trace stays the same, and the printed stack trace looks like the original apart from the BSN") {
                    maskedThrowable.stackTrace shouldBe illegalStateException.stackTrace
                    with(StringWriter().also { maskedThrowable.printStackTrace(PrintWriter(it)) }.toString()) {
                        this shouldContain "java.lang.IllegalStateException: Geen persoon gevonden voor BSN '******896'"
                        this shouldContain "Caused by: java.lang.RuntimeException: fakeCause ******896"
                        this shouldNotContain bsn
                    }
                }
            }
        }

        given("an exception without personal data in its messages") {
            val illegalStateException = IllegalStateException("fakeMessage", RuntimeException("fakeCause"))

            `when`("the personal data is masked") {
                val maskedThrowable = illegalStateException.maskPersonalData()

                then("the original exception is kept, so that its class stays visible in the logging") {
                    maskedThrowable shouldBeSameInstanceAs illegalStateException
                }
            }
        }

        given("an exception that has a BSN only in the message of its cause") {
            val illegalStateException = IllegalStateException("fakeMessage", RuntimeException("fakeCause 999993896"))

            `when`("the personal data is masked") {
                val maskedThrowable = illegalStateException.maskPersonalData()

                then("the exception is copied as well, so that it can refer to the masked cause") {
                    maskedThrowable.message shouldBe "java.lang.IllegalStateException: fakeMessage"
                    maskedThrowable.cause?.message shouldBe "java.lang.RuntimeException: fakeCause ******896"
                }
            }
        }

        given("an exception without a message that has a BSN in the message of its suppressed exception") {
            val illegalStateException = IllegalStateException().apply { addSuppressed(RuntimeException("fakeSuppressed 999993896")) }

            `when`("the personal data is masked") {
                val maskedThrowable = illegalStateException.maskPersonalData()

                then("the copy has only the name of the original class as message") {
                    maskedThrowable.message shouldBe "java.lang.IllegalStateException"
                    maskedThrowable.toString() shouldBe "java.lang.IllegalStateException"
                }
            }
        }

        given("an exception whose personal data is masked already") {
            val maskedThrowable = IllegalStateException("BSN 999993896").maskPersonalData()

            `when`("the personal data is masked again") {
                val maskedAgainThrowable = maskedThrowable.maskPersonalData()

                then("the masked exception is kept, so that a log record can pass more than one log handler") {
                    maskedAgainThrowable shouldBeSameInstanceAs maskedThrowable
                }
            }
        }

        given("an exception that is both the cause of another exception and suppressed by it") {
            val runtimeException = RuntimeException("fakeShared 999993896")
            val illegalStateException = IllegalStateException("fakeMessage", runtimeException).apply { addSuppressed(runtimeException) }

            `when`("the personal data is masked") {
                val maskedThrowable = illegalStateException.maskPersonalData()

                then("the shared exception is masked once and used for both, like in the original") {
                    (maskedThrowable.cause === maskedThrowable.suppressed.single()) shouldBe true
                }
            }
        }
    }
})
