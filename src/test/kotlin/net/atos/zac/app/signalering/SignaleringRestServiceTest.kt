/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.signalering

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import net.atos.zac.app.shared.RestPageParameters
import net.atos.zac.app.signalering.converter.RestSignaleringInstellingenConverter
import net.atos.zac.app.signalering.exception.SignaleringException
import net.atos.zac.app.zaak.model.createRestZaakOverzicht
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.identity.IdentityService
import net.atos.zac.signalering.SignaleringService
import net.atos.zac.signalering.model.SignaleringType

class SignaleringRestServiceTest : BehaviorSpec({
    val signaleringService = mockk<SignaleringService>()
    val identityService = mockk<IdentityService>()
    val restSignaleringInstellingenConverter = mockk<RestSignaleringInstellingenConverter>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()

    val signaleringRestService = SignaleringRestService(
        signaleringService,
        identityService,
        restSignaleringInstellingenConverter,
        loggedInUserInstance
    )

    Given("zaken signaleringen for ZAAK_OP_NAAM") {
        val signaleringType = SignaleringType.Type.ZAAK_OP_NAAM
        val pageNumber = 0
        val pageSize = 5
        val numberOfElements = 11
        val restZaakOverzichtList = List(numberOfElements) { createRestZaakOverzicht() }
        val restPageParameters = RestPageParameters(pageNumber, pageSize)

        every { signaleringService.countZakenSignaleringen(signaleringType) } returns numberOfElements.toLong()
        every {
            signaleringService.listZakenSignaleringenPage(signaleringType, restPageParameters)
        } returns restZaakOverzichtList

        When("listing zaken signaleringen with proper page parameters") {
            val restResultaat = signaleringRestService.listZakenSignaleringen(signaleringType, restPageParameters)

            Then("correct response is returned") {
                restResultaat.totaal shouldBe numberOfElements
                restResultaat.resultaten shouldBe restZaakOverzichtList
            }
        }

        When("listing zaken signaleringen with incorrect page parameters") {
            val exception = shouldThrow<SignaleringException> {
                signaleringRestService.listZakenSignaleringen(signaleringType, RestPageParameters(123, 456))
            }

            Then("exception is thrown") {
                exception.message shouldBe "Requested page 123 must be <= 1"
            }
        }
    }
})
