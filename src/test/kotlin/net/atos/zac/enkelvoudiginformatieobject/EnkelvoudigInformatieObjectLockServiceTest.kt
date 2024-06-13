package net.atos.zac.enkelvoudiginformatieobject

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObject
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.zac.event.EventingService
import net.atos.zac.websocket.event.ScreenEventType
import java.util.UUID

class EnkelvoudigInformatieObjectLockServiceTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerTest

    beforeContainer { testCase ->
        // only run before Given
        if (testCase.parent == null) {
            clearAllMocks()
        }
    }

    Given("an information object") {
        val drcClientService = mockk<DrcClientService>()
        val zrcClientService = mockk<ZRCClientService>()

        val informationObjectUUID = UUID.randomUUID()
        val unlockedEnkelvoudigInformatieObject = createEnkelvoudigInformatieObject(locked = false)
        val lockedEnkelvoudigInformatieObject = createEnkelvoudigInformatieObject(locked = true)

        val eventingService = mockk<EventingService>()
        val screenEvent = ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT.updated(informationObjectUUID)
        every { eventingService.send(screenEvent) } just runs

        val changeEventConfiguration = mockk<EnkelvoudigInformatieObjectLockServiceChangeEventConfiguration>()
        val numRetries = 5
        every { changeEventConfiguration.waitTimeoutMillis } returns 2L
        every { changeEventConfiguration.retries } returns numRetries

        val enkelvoudigInformatieObjectLockService = EnkelvoudigInformatieObjectLockService(
            drcClientService,
            zrcClientService,
            eventingService,
            changeEventConfiguration
        )

        When("a request to send change event for info object that's locked is initiated") {
            every {
                drcClientService.readEnkelvoudigInformatieobject(informationObjectUUID)
            } returnsMany listOf(
                unlockedEnkelvoudigInformatieObject,
                unlockedEnkelvoudigInformatieObject,
                lockedEnkelvoudigInformatieObject
            )
            enkelvoudigInformatieObjectLockService.sendChangeEvent(informationObjectUUID, true)

            Then("DRC is polled") {
                verify(exactly = 3) { drcClientService.readEnkelvoudigInformatieobject(informationObjectUUID) }
            }

            Then("event is sent") {
                verify(exactly = 1) { eventingService.send(screenEvent) }
            }
        }

        When("a request to send change event for info object that's unlocked is initiated") {
            every {
                drcClientService.readEnkelvoudigInformatieobject(informationObjectUUID)
            } returnsMany listOf(
                lockedEnkelvoudigInformatieObject,
                lockedEnkelvoudigInformatieObject,
                lockedEnkelvoudigInformatieObject,
                unlockedEnkelvoudigInformatieObject
            )
            enkelvoudigInformatieObjectLockService.sendChangeEvent(informationObjectUUID, false)

            Then("DRC is polled") {
                verify(exactly = 4) { drcClientService.readEnkelvoudigInformatieobject(informationObjectUUID) }
            }

            Then("event is sent") {
                verify(exactly = 1) { eventingService.send(screenEvent) }
            }
        }

        When("a request to send change event times out") {
            every {
                drcClientService.readEnkelvoudigInformatieobject(informationObjectUUID)
            } returnsMany List(numRetries) {
                lockedEnkelvoudigInformatieObject
            }
            enkelvoudigInformatieObjectLockService.sendChangeEvent(informationObjectUUID, false)

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
