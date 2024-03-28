package net.atos.zac.app.signaleringen

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.mockk
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.zac.app.zaken.converter.RESTZaakOverzichtConverter
import net.atos.zac.app.zaken.model.createRESTZaakOverzicht
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.signalering.SignaleringenService
import net.atos.zac.signalering.model.Signalering
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.signalering.model.createSignalering
import java.util.UUID
import kotlin.random.Random

class SignaleringenRestServiceTest : BehaviorSpec() {
    private val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    private val signaleringenService = mockk<SignaleringenService>()
    private val restZaakOverzichtConverter = mockk<RESTZaakOverzichtConverter>()
    private val zrcClientService = mockk<ZRCClientService>()
    private val loggedInUser = createLoggedInUser()

    @InjectMockKs
    lateinit var signaleringenRestService: SignaleringenRestService

    override suspend fun beforeContainer(testCase: TestCase) {
        super.beforeContainer(testCase)

        // Only run before Given
        if (testCase.parent != null) return

        MockKAnnotations.init(this)
        clearAllMocks()

        every { loggedInUserInstance.get() } returns loggedInUser
    }

    init {
        Given("there are multiple ZAAK_OP_NAAM signals ") {
            val numberOfSignals = 100
            val maxDelay = 15L

            val zaakList = MutableList(numberOfSignals) { createZaak() }
            val signals: List<Signalering> =
                MutableList(numberOfSignals) { index -> createSignalering(zaakList[index]) }
            every { signaleringenService.listSignaleringen(any()) } returns signals
            zaakList.forEach { zaak ->
                every { zrcClientService.readZaak(zaak.uuid) } answers {
                    Thread.sleep(Random.nextLong(10, maxDelay))
                    zaak
                }
                every { restZaakOverzichtConverter.convert(zaak) } returns createRESTZaakOverzicht(uuid = zaak.uuid)
            }

            When("zaak signals with type ZAAK_OP_NAAM are requested") {
                val list = signaleringenRestService.listZakenSignaleringen(SignaleringType.Type.ZAAK_OP_NAAM)

                Then("an overview for all signals is generated") {
                    list shouldHaveSize numberOfSignals
                    list.forEachIndexed { index, restZaakOverzicht ->
                        restZaakOverzicht.uuid shouldBe zaakList[index].uuid
                    }
                    verify(exactly = numberOfSignals) {
                        zrcClientService.readZaak(any<UUID>())
                    }
                }
            }
        }
    }
}
