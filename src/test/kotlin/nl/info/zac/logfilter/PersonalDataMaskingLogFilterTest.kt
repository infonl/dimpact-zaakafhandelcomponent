/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.logfilter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jboss.logmanager.ExtLogRecord
import org.jboss.logmanager.formatters.PatternFormatter
import org.jboss.logmanager.formatters.XmlFormatter
import java.util.logging.Level
import java.util.logging.LogRecord

class PersonalDataMaskingLogFilterTest : BehaviorSpec({
    val personalDataMaskingLogFilter = PersonalDataMaskingLogFilter()

    context("Filtering a log record") {
        given("a WildFly log record with a BSN and an IBAN as message parameters") {
            val extLogRecord = ExtLogRecord(
                Level.INFO,
                "Persoon {0} met IBAN {1}",
                ExtLogRecord.FormatStyle.MESSAGE_FORMAT,
                "fakeLoggerClassName"
            ).apply { parameters = arrayOf("999993896", "NL91ABNA0417164300") }

            `when`("the log record is filtered") {
                val isLoggable = personalDataMaskingLogFilter.isLoggable(extLogRecord)

                then("the log record is logged with the BSN and the IBAN masked") {
                    isLoggable shouldBe true
                    extLogRecord.formattedMessage shouldBe "Persoon ******896 met IBAN $MASKED_VALUE"
                }
            }
        }

        given(
            "a log record with a BSN that is logged through the java.util.logging API, " +
                "as with a message supplier, and that WildFly wraps"
        ) {
            val originalLogRecord = LogRecord(Level.OFF, "PersonenApi.personen() >>>\nrequest = { burgerservicenummer: [999993896] }")
            val wrappedLogRecord = ExtLogRecord.wrap(originalLogRecord)

            `when`("the log record is filtered") {
                val isLoggable = personalDataMaskingLogFilter.isLoggable(wrappedLogRecord)

                then("the log record is logged with the BSN masked") {
                    isLoggable shouldBe true
                    wrappedLogRecord.formattedMessage shouldBe
                        "PersonenApi.personen() >>>\nrequest = { burgerservicenummer: [******896] }"
                }
            }
        }

        given("a WildFly log record with an exception that has a BSN in its message") {
            val extLogRecord = ExtLogRecord(Level.FINE, "fakeMessage", "fakeLoggerClassName").apply {
                thrown = IllegalStateException("Geen persoon gevonden voor BSN '999993896'")
            }

            `when`("the log record is filtered") {
                personalDataMaskingLogFilter.isLoggable(extLogRecord)

                then("the exception is logged with the BSN masked") {
                    extLogRecord.thrown.toString() shouldBe
                        "java.lang.IllegalStateException: Geen persoon gevonden voor BSN '******896'"
                }
            }
        }

        given("a WildFly log record with an exception without personal data, which a structured formatter writes") {
            val extLogRecord = ExtLogRecord(Level.SEVERE, "fakeMessage", "fakeLoggerClassName").apply {
                thrown = IllegalStateException("fakeExceptionMessage")
            }

            `when`("the log record is filtered and formatted like the JSON logging of WildFly does") {
                personalDataMaskingLogFilter.isLoggable(extLogRecord)
                val formattedLogRecord = XmlFormatter().format(extLogRecord)

                then("the type of the exception is still the class of the original exception") {
                    formattedLogRecord shouldContain "<exceptionType>java.lang.IllegalStateException</exceptionType>"
                }
            }
        }

        given("a WildFly log record with an exception with a BSN, which a structured formatter writes") {
            val extLogRecord = ExtLogRecord(Level.SEVERE, "fakeMessage", "fakeLoggerClassName").apply {
                thrown = IllegalStateException("Geen persoon gevonden voor BSN '999993896'")
            }

            `when`("the log record is filtered and formatted like the JSON logging of WildFly does") {
                personalDataMaskingLogFilter.isLoggable(extLogRecord)
                val formattedLogRecord = XmlFormatter().format(extLogRecord)

                then("the BSN is masked, and the class of the original exception is still visible in the message") {
                    formattedLogRecord shouldContain "java.lang.IllegalStateException: Geen persoon gevonden voor BSN '******896'"
                    formattedLogRecord shouldNotContain "999993896"
                }
            }
        }

        given("a WildFly log record with an exception with a BSN, which passes two log handlers with this filter") {
            val extLogRecord = ExtLogRecord(Level.SEVERE, "fakeMessage", "fakeLoggerClassName").apply {
                thrown = IllegalStateException("Geen persoon gevonden voor BSN '999993896'")
            }

            `when`("the log record is filtered twice and its exception is formatted as text") {
                personalDataMaskingLogFilter.isLoggable(extLogRecord)
                personalDataMaskingLogFilter.isLoggable(extLogRecord)
                val formattedException = PatternFormatter("%e").format(extLogRecord)

                then("the stack trace shows the class of the original exception and the masked BSN, not a masked copy of a masked copy") {
                    formattedException shouldContain "java.lang.IllegalStateException: Geen persoon gevonden voor BSN '******896'"
                    formattedException shouldNotContain MaskedThrowable::class.java.name
                }
            }
        }

        given("a WildFly log record with an exception, whose message cannot be formatted") {
            val extLogRecord = object : ExtLogRecord(Level.SEVERE, "fakeMessage 999993896", "fakeLoggerClassName") {
                override fun getFormattedMessage(): String = throw IllegalArgumentException("fakeFormattingError")
            }.apply { thrown = IllegalStateException("BSN 999993896") }

            `when`("the log record is filtered") {
                val isLoggable = personalDataMaskingLogFilter.isLoggable(extLogRecord)

                then("the log record is still logged, but without its message and exception, which could not be masked") {
                    isLoggable shouldBe true
                    extLogRecord.message shouldBe "The content of this log record is not logged, because masking its personal data " +
                        "failed with java.lang.IllegalArgumentException"
                    extLogRecord.thrown shouldBe null
                }
            }
        }

        given("a plain log record with a BSN, which WildFly does not create itself") {
            val logRecord = LogRecord(Level.INFO, "BSN 999993896")

            `when`("the log record is filtered") {
                val isLoggable = personalDataMaskingLogFilter.isLoggable(logRecord)

                then("the log record is logged with the BSN masked") {
                    isLoggable shouldBe true
                    logRecord.message shouldBe "BSN ******896"
                }
            }
        }

        given("a WildFly log record without a message") {
            val extLogRecord = ExtLogRecord(Level.INFO, null, "fakeLoggerClassName")

            `when`("the log record is filtered") {
                val isLoggable = personalDataMaskingLogFilter.isLoggable(extLogRecord)

                then("the log record is logged without a message") {
                    isLoggable shouldBe true
                    extLogRecord.message shouldBe null
                }
            }
        }
    }
})
