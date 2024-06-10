package net.atos.zac.signalering

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.mockk
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.zac.app.zaken.converter.RESTZaakOverzichtConverter
import net.atos.zac.event.EventingService
import net.atos.zac.mail.MailService

class SignaleringServiceTest : BehaviorSpec({
    val eventingService = mockk<EventingService>()
    val mailService = mockk<MailService>()
    val signaleringenMailHelper = mockk<SignaleringMailHelper>()
    val signaleringZACHelper = mockk<SignaleringZACHelper>()
    val signaleringPredicateHelper = mockk<SignaleringPredicateHelper>()
    val zrcClientService = mockk<ZRCClientService>()
    val restZaakOverzichtConverter = mockk<RESTZaakOverzichtConverter>()

    val signaleringService = SignaleringenService(
        eventingService = eventingService,
        mailService = mailService,
        signaleringenMailHelper = signaleringenMailHelper,
        signaleringZACHelper = signaleringZACHelper,
        signaleringPredicateHelper = signaleringPredicateHelper,
        zrcClientService = zrcClientService,
        restZaakOverzichtConverter = restZaakOverzichtConverter
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    beforeSpec {
        clearAllMocks()
    }

    Given("two signaleringen, one of which has a creation date older than 1 day") {
        val deleteOlderThanDays = 1L

        When("signaleringen older than 1 day are deleted") {

            signaleringService.deleteOldSignaleringen(deleteOlderThanDays)

            Then("one signalering is deleted but the other is not") {
            }
        }
    }
})
