/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.authentication

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import nl.info.zac.app.zaak.model.ZAAK_TYPE_1_OMSCHRIJVING
import nl.info.zac.app.zaak.model.ZAAK_TYPE_2_OMSCHRIJVING

class LoggedInUserAuthorisationTest : BehaviorSpec({

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("PABC integration enabled -> use application roles per zaaktype") {
        val userAuthorizedWithPabc = createLoggedInUser(
            pabcMappings = mapOf(
                ZAAK_TYPE_1_OMSCHRIJVING to setOf("applicationRole1"),
                ZAAK_TYPE_2_OMSCHRIJVING to emptySet()
            )
        )

        When("authorisation is checked for various zaaktypen") {

            Then("authorised for ZAAK_TYPE_1_OMSCHRIJVING (mapped with roles)") {
                userAuthorizedWithPabc.isAuthorisedForZaaktype(ZAAK_TYPE_1_OMSCHRIJVING, true) shouldBe true
            }

            Then("not authorised for ZAAK_TYPE_2_OMSCHRIJVING (mapped but no roles)") {
                userAuthorizedWithPabc.isAuthorisedForZaaktype(ZAAK_TYPE_2_OMSCHRIJVING, true) shouldBe false
            }

            Then("not authorised for zaaktype not present in mappings") {
                userAuthorizedWithPabc.isAuthorisedForZaaktype("zaaktype3", true) shouldBe false
            }
        }
    }

    Given("PABC integration is disabled -> functional roles are used") {
        val userAuthorizedForAllWithPabcDisabled = createLoggedInUser(
            zaakTypes = null,
            pabcMappings = emptyMap()
        )

        val userAuthorizedWithPabcDisabled = createLoggedInUser(
            zaakTypes = setOf(ZAAK_TYPE_1_OMSCHRIJVING, ZAAK_TYPE_2_OMSCHRIJVING),
            pabcMappings = emptyMap()
        )

        When("authorisation is evaluated without PABC mappings") {

            Then("all zaaktypen: authorised for any zaaktype") {
                userAuthorizedForAllWithPabcDisabled.isAuthorisedForZaaktype(ZAAK_TYPE_1_OMSCHRIJVING, false) shouldBe true
                userAuthorizedForAllWithPabcDisabled.isAuthorisedForZaaktype("zaaktype3", false) shouldBe true
            }

            Then("only authorised for configured zaaktypen") {
                userAuthorizedWithPabcDisabled.isAuthorisedForZaaktype(ZAAK_TYPE_1_OMSCHRIJVING, false) shouldBe true
                userAuthorizedWithPabcDisabled.isAuthorisedForZaaktype(ZAAK_TYPE_2_OMSCHRIJVING, false) shouldBe true
                userAuthorizedWithPabcDisabled.isAuthorisedForZaaktype("zaaktype3", false) shouldBe false
            }
        }
    }
})
