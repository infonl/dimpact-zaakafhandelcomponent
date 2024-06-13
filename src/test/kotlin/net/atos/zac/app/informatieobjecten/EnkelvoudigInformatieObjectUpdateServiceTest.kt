package net.atos.zac.app.informatieobjecten

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObject
import net.atos.client.zgw.drc.model.generated.Ondertekening
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import net.atos.zac.event.EventingService
import net.atos.zac.websocket.event.ScreenEventType
import java.time.LocalDate
import java.util.UUID

class EnkelvoudigInformatieObjectUpdateServiceTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerTest

    beforeContainer { testCase ->
        // only run before Given
        if (testCase.parent == null) {
            clearAllMocks()
        }
    }

    Given("an information object") {
        val enkelvoudigInformatieObjectLockService = mockk<EnkelvoudigInformatieObjectLockService>()
        val drcClientService = mockk<DrcClientService>()
        val loggedInUserInstance = mockk<Instance<LoggedInUser>>()

        val informationObjectUUID = UUID.randomUUID()
        val unsignedEnkelvoudigInformatieObject = createEnkelvoudigInformatieObject().apply {
            ondertekening = Ondertekening()
        }
        val signedEnkelvoudigInformatieObject = createEnkelvoudigInformatieObject().apply {
            ondertekening = Ondertekening().apply {
                datum = LocalDate.now()
            }
        }

        val eventingService = mockk<EventingService>()
        val screenEvent = ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT.updated(informationObjectUUID)
        every { eventingService.send(screenEvent) } just runs

        val changeEventConfiguration = mockk<EnkelvoudigInformatieObjectUpdateServiceChangeEventConfiguration>()
        val numRetries = 5
        every { changeEventConfiguration.waitTimeoutMillis } returns 2L
        every { changeEventConfiguration.retries } returns numRetries

        val enkelvoudigInformatieObjectUpdateService = EnkelvoudigInformatieObjectUpdateService(
            enkelvoudigInformatieObjectLockService,
            drcClientService,
            loggedInUserInstance,
            eventingService,
            changeEventConfiguration
        )

        When("a request to send change event for info object that's locked is initiated") {
            every {
                drcClientService.readEnkelvoudigInformatieobject(informationObjectUUID)
            } returnsMany listOf(
                unsignedEnkelvoudigInformatieObject,
                unsignedEnkelvoudigInformatieObject,
                signedEnkelvoudigInformatieObject
            )
            enkelvoudigInformatieObjectUpdateService.sendChangeEvent(informationObjectUUID)

            Then("DRC is polled") {
                verify(exactly = 3) { drcClientService.readEnkelvoudigInformatieobject(informationObjectUUID) }
            }

            Then("event is sent") {
                verify(exactly = 1) { eventingService.send(screenEvent) }
            }
        }

        When("a request to send change event times out") {
            every {
                drcClientService.readEnkelvoudigInformatieobject(informationObjectUUID)
            } returnsMany List(numRetries) {
                unsignedEnkelvoudigInformatieObject
            }
            enkelvoudigInformatieObjectUpdateService.sendChangeEvent(informationObjectUUID)

            Then("DRC is polled") {
                verify(exactly = numRetries) {
                    drcClientService.readEnkelvoudigInformatieobject(informationObjectUUID)
                }
            }

            Then("event is not sent") {
                verify(exactly = 0) { eventingService.send(screenEvent) }
            }
        }
    }
})
