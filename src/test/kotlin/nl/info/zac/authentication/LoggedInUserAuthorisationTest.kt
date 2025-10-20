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

    Given("PABC integration is disabled -> functional roles are used") {
        val userAuthorizedForAllWithPabcDisabled = createLoggedInUser(
            geautoriseerdeZaaktypen = null,
            applicationRolesPerZaaktype = emptyMap()
        )

        val userAuthorizedWithPabcDisabled = createLoggedInUser(
            geautoriseerdeZaaktypen = setOf(ZAAK_TYPE_1_OMSCHRIJVING, ZAAK_TYPE_2_OMSCHRIJVING),
            applicationRolesPerZaaktype = emptyMap()
        )

        When("authorisation is evaluated without PABC mappings") {

            Then("all zaaktypen: authorised for any zaaktype") {
                userAuthorizedForAllWithPabcDisabled.isAuthorisedForZaaktype(ZAAK_TYPE_1_OMSCHRIJVING) shouldBe true
                userAuthorizedForAllWithPabcDisabled.isAuthorisedForZaaktype("zaaktype3") shouldBe true
            }

            Then("only authorised for configured zaaktypen") {
                userAuthorizedWithPabcDisabled.isAuthorisedForZaaktype(ZAAK_TYPE_1_OMSCHRIJVING) shouldBe true
                userAuthorizedWithPabcDisabled.isAuthorisedForZaaktype(ZAAK_TYPE_2_OMSCHRIJVING) shouldBe true
                userAuthorizedWithPabcDisabled.isAuthorisedForZaaktype("zaaktype3") shouldBe false
            }
        }
    }
})
