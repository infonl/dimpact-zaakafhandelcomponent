package net.atos.zac.app.signalering

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import net.atos.zac.app.informatieobjecten.model.createRestEnkelvoudigInformatieobject
import net.atos.zac.app.signalering.converter.RestSignaleringInstellingenConverter
import net.atos.zac.app.signalering.model.createRestSignaleringTaskSummary
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
        val pageNumber = 0
        val pageSize = 5
        val numberOfElements = 11
        val restZaakOverzichtList = List(numberOfElements) { createRESTZaakOverzicht() }

        every { signaleringService.countZakenSignaleringen(signaleringType) } returns numberOfElements.toLong()
        every {
            signaleringService.listZakenSignaleringenPage(signaleringType, pageNumber, pageSize)
        } returns restZaakOverzichtList

        When("listing zaken signaleringen with proper page parameters") {
            val response = signaleringRestService.listZakenSignaleringen(signaleringType, pageNumber, pageSize)

            Then("correct response is returned") {
                response.status shouldBe 200
                response.headers["X-Total-Count"] shouldBe listOf(numberOfElements)
                response.entity shouldBe restZaakOverzichtList
            }
        }

        When("listing zaken signaleringen with incorrect page parameters") {
            val response = signaleringRestService.listZakenSignaleringen(signaleringType, 123, 456)

            Then("404 response is returned") {
                response.status shouldBe 404
                response.entity shouldBe null
            }
        }
    }

    Given("taken signaleringen for TAAK_OP_NAAM") {
        val signaleringType = SignaleringType.Type.TAAK_OP_NAAM
        val pageNumber = 0
        val pageSize = 5
        val numberOfElements = 11
        val restSignaleringTaskSummaryList = List(numberOfElements) { createRestSignaleringTaskSummary() }

        every { signaleringService.countTakenSignaleringen(signaleringType) } returns numberOfElements.toLong()
        every {
            signaleringService.listTakenSignaleringenPage(signaleringType, pageNumber, pageSize)
        } returns restSignaleringTaskSummaryList

        When("listing taken signaleringen with proper page parameters") {
            val response = signaleringRestService.listTakenSignaleringen(signaleringType, pageNumber, pageSize)

            Then("correct response is returned") {
                response.status shouldBe 200
                response.headers["X-Total-Count"] shouldBe listOf(numberOfElements)
                response.entity shouldBe restSignaleringTaskSummaryList
            }
        }

        When("listing taken signaleringen with incorrect page parameters") {
            val response = signaleringRestService.listTakenSignaleringen(signaleringType, 123, 456)

            Then("404 response is returned") {
                response.status shouldBe 404
                response.entity shouldBe null
            }
        }
    }

    Given("information object signaleringen for ZAAK_DOCUMENT_TOEGEVOEGD") {
        val signaleringType = SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD
        val pageNumber = 0
        val pageSize = 5
        val numberOfElements = 11
        val restEnkelvoudigInformatieobjectList = List(numberOfElements) { createRestEnkelvoudigInformatieobject() }

        every {
            signaleringService.countInformatieobjectenSignaleringen(signaleringType)
        } returns numberOfElements.toLong()
        every {
            signaleringService.listInformatieobjectenSignaleringen(signaleringType, pageNumber, pageSize)
        } returns restEnkelvoudigInformatieobjectList

        When("listing taken signaleringen with proper page parameters") {
            val response = signaleringRestService.listInformatieobjectenSignaleringen(
                signaleringType,
                pageNumber,
                pageSize
            )

            Then("correct response is returned") {
                response.status shouldBe 200
                response.headers["X-Total-Count"] shouldBe listOf(numberOfElements)
                response.entity shouldBe restEnkelvoudigInformatieobjectList
            }
        }

        When("listing taken signaleringen with incorrect page parameters") {
            val response = signaleringRestService.listInformatieobjectenSignaleringen(signaleringType, 123, 456)

            Then("404 response is returned") {
                response.status shouldBe 404
                response.entity shouldBe null
            }
        }
    }
})
