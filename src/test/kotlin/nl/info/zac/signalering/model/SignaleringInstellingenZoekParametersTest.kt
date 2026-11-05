/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SignaleringInstellingenZoekParametersTest : BehaviorSpec({
    val mailOnlyInstellingen = createSignaleringInstellingen(
        type = createSignaleringType(type = SignaleringType.Type.TAAK_AFGEROND, subjecttype = SignaleringSubject.TAAK),
        isDashboard = false,
        isMail = true
    )

    context("Matching signalering instellingen against search parameters") {
        given("search parameters for an owner only") {
            val parameters = SignaleringInstellingenZoekParameters(SignaleringTarget.USER, "fakeMedewerker")

            `when`("instellingen are matched") {
                val isMatch = parameters.matches(mailOnlyInstellingen)

                then("they match") {
                    isMatch shouldBe true
                }
            }
        }

        given("search parameters for mail") {
            val parameters = SignaleringInstellingenZoekParameters(SignaleringTarget.USER, "fakeMedewerker").mail()

            `when`("instellingen with mail on are matched") {
                val isMatch = parameters.matches(mailOnlyInstellingen)

                then("they match") {
                    isMatch shouldBe true
                }
            }
        }

        given("search parameters for the dashboard") {
            val parameters = SignaleringInstellingenZoekParameters(SignaleringTarget.USER, "fakeMedewerker").dashboard()

            `when`("instellingen with the dashboard off are matched") {
                val isMatch = parameters.matches(mailOnlyInstellingen)

                then("they do not match") {
                    isMatch shouldBe false
                }
            }
        }
    }
})
