package net.atos.zac.zaken

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.createRolType
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.zac.event.EventingService
import net.atos.zac.event.Opcode
import net.atos.zac.identity.model.createGroup
import net.atos.zac.identity.model.createUser
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zoeken.IndexeerService
import net.atos.zac.zoeken.model.index.ZoekObjectType

@MockKExtension.CheckUnnecessaryStub
class ZakenServiceTest : BehaviorSpec({
    val eventingService = mockk<EventingService>()
    val indexeerService = mockk<IndexeerService>()
    val zrcClientService = mockk<ZRCClientService>()
    val ztcClientService = mockk<ZTCClientService>()
    val zakenService = ZakenService(
        eventingService = eventingService,
        indexeerService = indexeerService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService
    )

    Given("A list of zaken") {
        val explanation = "dummyExplanation"
        val screenEventResourceId = "dummyResourceId"
        val zaken = listOf(
            createZaak(),
            createZaak()
        )
        val group = createGroup()
        val user = createUser()
        val rolTypeBehandelaar = createRolType(
            omschrijvingGeneriek = RolType.OmschrijvingGeneriekEnum.BEHANDELAAR
        )
        val screenEventSlot = slot<ScreenEvent>()
        zaken.map {
            every { zrcClientService.readZaak(it.uuid) } returns it
            every {
                ztcClientService.readRoltype(
                    RolType.OmschrijvingGeneriekEnum.BEHANDELAAR,
                    it.zaaktype
                )
            } returns rolTypeBehandelaar
            every { zrcClientService.updateRol(it, any(), explanation) } just Runs
            every { indexeerService.indexeerDirect(it.uuid.toString(), ZoekObjectType.ZAAK) } just Runs
            every { eventingService.send(capture(screenEventSlot)) } just Runs
        }
        When(
            "the assign zaken async function is called with a group, a user " +
                "and a screen event resource id"
        ) {
            val coroutine = zakenService.assignZakenAsync(
                zaakUUIDs = zaken.map { it.uuid },
                explanation = explanation,
                group = group,
                user = user,
                screenEventResourceId = screenEventResourceId
            )
            Then(
                "for both zaken the group and user roles " +
                    "and the search index should be updated and " +
                    "a screen event of type 'zaken verdelen' should sent"
            ) {
                coroutine.join()
                verify(exactly = zaken.size) {
                    zaken.map {
                        zrcClientService.updateRol(it, any(), explanation)
                    }
                }
                with(screenEventSlot.captured) {
                    opcode shouldBe Opcode.UPDATED
                    objectType shouldBe ScreenEventType.ZAKEN_VERDELEN
                    objectId.resource shouldBe screenEventResourceId
                }
            }
        }
    }
})
