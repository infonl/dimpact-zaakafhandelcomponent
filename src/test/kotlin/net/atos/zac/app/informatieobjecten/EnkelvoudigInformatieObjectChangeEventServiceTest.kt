package net.atos.zac.app.informatieobjecten

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import net.atos.zac.event.EventingService
import net.atos.zac.websocket.event.ScreenEventType
import java.util.UUID

class EnkelvoudigInformatieObjectChangeEventServiceTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerTest

    beforeContainer { testCase ->
        // only run before Given
        if (testCase.parent == null) {
            clearAllMocks()
        }
    }

    Given("a change event for information object is requested to be sent") {
        val predicate: (UUID) -> Boolean = mockk()

        val informationObjectUUID = UUID.randomUUID()

        val eventingService = mockk<EventingService>()
        val screenEvent = ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT.updated(informationObjectUUID)
        every { eventingService.send(screenEvent) } just runs

        val changeEventConfiguration = mockk<EnkelvoudigInformatieObjectChangeEventServiceConfiguration>()
        val numRetries = 5
        every { changeEventConfiguration.waitTimeoutMillis } returns 2L
        every { changeEventConfiguration.retries } returns numRetries

        val enkelvoudigInformatieObjectChangeEventService = EnkelvoudigInformatieObjectChangeEventService(
            eventingService,
            changeEventConfiguration
        )

        When("a predicate succeeds") {
            every {
                predicate(informationObjectUUID)
            } returnsMany listOf(false, false, true)
            enkelvoudigInformatieObjectChangeEventService.sendChangeEvent(informationObjectUUID, predicate)

            Then("predicate is called") {
                verify(exactly = 3) { predicate(informationObjectUUID) }
            }

            Then("event is sent") {
                verify(exactly = 1) { eventingService.send(screenEvent) }
            }
        }

        When("the request times out") {
            every {
                predicate(informationObjectUUID)
            } returnsMany List(numRetries) { false }
            enkelvoudigInformatieObjectChangeEventService.sendChangeEvent(informationObjectUUID, predicate)

            Then("predicate is called") {
                verify(exactly = numRetries) { predicate(informationObjectUUID) }
            }

            Then("no event is sent") {
                verify(exactly = 0) { eventingService.send(screenEvent) }
            }
        }
    }
})
