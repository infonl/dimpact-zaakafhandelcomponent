/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.logfilter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class PersonalDataMaskingTest : BehaviorSpec({
    context("Masking personal data in a text") {
        given("a text with a BSN") {
            val text = "Geen persoon gevonden voor BSN '999993896'"

            `when`("the personal data is masked") {
                val maskedText = text.maskPersonalData()

                then("all digits of the BSN except the last three are masked") {
                    maskedText shouldBe "Geen persoon gevonden voor BSN '******896'"
                }
            }
        }

        given("a text with two BSNs in a list") {
            val text = "burgerservicenummer: [999993896, 999992958]"

            `when`("the personal data is masked") {
                val maskedText = text.maskPersonalData()

                then("both BSNs are masked") {
                    maskedText shouldBe "burgerservicenummer: [******896, ******958]"
                }
            }
        }

        given("a text with a RSIN, which has the same format as a BSN") {
            val text = "bronorganisatie: 002564440"

            `when`("the personal data is masked") {
                val maskedText = text.maskPersonalData()

                then("the RSIN is masked as well") {
                    maskedText shouldBe "bronorganisatie: ******440"
                }
            }
        }

        given("a text with an IBAN without and with spaces") {
            val text = "rekeningen: NL91ABNA0417164300 en NL91 ABNA 0417 1643 00"

            `when`("the personal data is masked") {
                val maskedText = text.maskPersonalData()

                then("both IBANs are masked completely") {
                    maskedText shouldBe "rekeningen: $MASKED_VALUE en $MASKED_VALUE"
                }
            }
        }

        given("a text with a UUID, a zaak identificatie, a date and numbers that are shorter or longer than a BSN") {
            val text = "zaak 4f8c1a2e-1d3b-4c5a-9e7f-123456789abc ZAAK-2026-0000000001 1980-05-17 12345678 1234567890"

            `when`("the personal data is masked") {
                val maskedText = text.maskPersonalData()

                then("nothing is masked") {
                    maskedText shouldBe text
                }
            }
        }

        given("a text in which the personal data is already masked") {
            val text = "BSN ******896 en IBAN $MASKED_VALUE"

            `when`("the personal data is masked again") {
                val maskedText = text.maskPersonalData()

                then("the text does not change, so that a log record can pass more than one log handler") {
                    maskedText shouldBe text
                }
            }
        }
    }
})
