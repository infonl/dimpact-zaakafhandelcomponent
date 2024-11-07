package net.atos.zac.app.signalering

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import net.atos.zac.app.signalering.converter.RestSignaleringInstellingenConverter
import net.atos.zac.app.zaak.model.createRESTZaakOverzicht
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
        val numberOfElements = 11
        val restZaakOverzichtList = List(numberOfElements) { createRESTZaakOverzicht() }

        every { signaleringService.countZakenSignaleringen(signaleringType) } returns numberOfElements.toLong()

        When("listing zaken signaleringen with proper page parameters") {
            val pageNumber = 0
            val pageSize = 5

            every {
                signaleringService.listZakenSignaleringenPage(signaleringType, pageNumber, pageSize)
            } returns restZaakOverzichtList

            val response = signaleringRestService.listZakenSignaleringen(signaleringType, pageNumber, pageSize)

            Then("correct response is returned") {
                response.status shouldBe 200
                response.headers["X-Total-Count"] shouldBe listOf(numberOfElements)
                response.entity shouldBe restZaakOverzichtList
            }
        }

        When("listing zaken signaleringen with incorrect page parameters") {
            val pageNumber = 123
            val pageSize = 456

            every {
                signaleringService.listZakenSignaleringenPage(signaleringType, pageNumber, pageSize)
            } returns emptyList()

            val response = signaleringRestService.listZakenSignaleringen(signaleringType, pageNumber, pageSize)

            Then("404 response is returned") {
                response.status shouldBe 404
                response.entity shouldBe null
            }
        }
    }
})
