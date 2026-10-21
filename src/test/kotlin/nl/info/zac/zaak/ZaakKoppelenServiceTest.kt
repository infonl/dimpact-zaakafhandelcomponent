/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.GerelateerdeZaak
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.event.EventingService
import nl.info.zac.search.IndexingService
import nl.info.zac.websocket.event.ScreenEvent
import nl.info.zac.websocket.event.ScreenEventType
import java.util.UUID

class ZaakKoppelenServiceTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerTest
    val eventingService = mockk<EventingService>()
    val indexingService = mockk<IndexingService>()
    val zrcClientService = mockk<ZrcClientService>()
    val zaakKoppelenService = ZaakKoppelenService(
        eventingService = eventingService,
        indexingService = indexingService,
        zrcClientService = zrcClientService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Adding a gerelateerde zaak") {
        val gerelateerdeZaak = createZaak()
        val patchZaakSlot = slot<Zaak>()

        given("a zaak without gerelateerde zaken") {
            val zaak = createZaak()
            every { zrcClientService.patchZaak(zaak.uuid, capture(patchZaakSlot), "fakeReden") } returns zaak

            `when`("a gerelateerde zaak is added with an explanation") {
                zaakKoppelenService.addGerelateerdeZaak(zaak, gerelateerdeZaak, "fakeReden")

                then("the zaak is patched with only the gerelateerde zaak, with the explanation") {
                    verify(exactly = 1) { zrcClientService.patchZaak(zaak.uuid, any(), "fakeReden") }
                    patchZaakSlot.captured.gerelateerdeZaken.map { it.url } shouldBe listOf(gerelateerdeZaak.url)
                }
            }
        }

        given("a zaak that is related to another zaak") {
            val otherZaak = createZaak()
            val zaak = createZaak().apply {
                addGerelateerdeZakenItem(GerelateerdeZaak().apply { url = otherZaak.url })
            }
            every { zrcClientService.patchZaak(zaak.uuid, capture(patchZaakSlot), null) } returns zaak

            `when`("a gerelateerde zaak is added") {
                zaakKoppelenService.addGerelateerdeZaak(zaak, gerelateerdeZaak, null)

                then("the zaak keeps the other zaak and gets the gerelateerde zaak") {
                    patchZaakSlot.captured.gerelateerdeZaken.map { it.url } shouldBe
                        listOf(otherZaak.url, gerelateerdeZaak.url)
                }
            }
        }

        given("a zaak that is already related to the gerelateerde zaak") {
            val zaak = createZaak().apply {
                addGerelateerdeZakenItem(GerelateerdeZaak().apply { url = gerelateerdeZaak.url })
            }
            every { zrcClientService.patchZaak(zaak.uuid, capture(patchZaakSlot), null) } returns zaak

            `when`("the gerelateerde zaak is added again") {
                zaakKoppelenService.addGerelateerdeZaak(zaak, gerelateerdeZaak, null)

                then("the gerelateerde zaak is listed once") {
                    patchZaakSlot.captured.gerelateerdeZaken.map { it.url } shouldBe listOf(gerelateerdeZaak.url)
                }
            }
        }
    }

    context("Linking a hoofdzaak and a deelzaak") {
        given("two zaken") {
            val hoofdzaak = createZaak()
            val deelzaak = createZaak()
            val patchZaakSlot = slot<Zaak>()
            val screenEventSlot = slot<ScreenEvent>()
            every { zrcClientService.patchZaak(deelzaak.uuid, capture(patchZaakSlot)) } returns deelzaak
            every { indexingService.addOrUpdateZaakOrThrow(hoofdzaak.uuid, false) } just runs
            every { eventingService.send(capture(screenEventSlot)) } just runs

            `when`("they are linked") {
                zaakKoppelenService.linkHoofdzaakAndDeelzaak(hoofdzaak = hoofdzaak, deelzaak = deelzaak)

                then("the hoofdzaak is set on the deelzaak") {
                    patchZaakSlot.captured.hoofdzaak shouldBe hoofdzaak.url
                }

                and("the hoofdzaak is reindexed and a screen event is sent for it") {
                    verify(exactly = 1) { indexingService.addOrUpdateZaakOrThrow(hoofdzaak.uuid, false) }
                    screenEventSlot.captured shouldBe ScreenEventType.ZAAK.updated(hoofdzaak.uuid)
                }
            }
        }
    }

    context("Updating a hoofdzaak") {
        given("a hoofdzaak") {
            val hoofdzaakUuid = UUID.randomUUID()
            val screenEventSlot = slot<ScreenEvent>()
            every { indexingService.addOrUpdateZaakOrThrow(hoofdzaakUuid, false) } just runs
            every { eventingService.send(capture(screenEventSlot)) } just runs

            `when`("the hoofdzaak is updated") {
                zaakKoppelenService.updateHoofdzaak(hoofdzaakUuid)

                then("the hoofdzaak is reindexed and a screen event is sent for it") {
                    verify(exactly = 1) { indexingService.addOrUpdateZaakOrThrow(hoofdzaakUuid, false) }
                    screenEventSlot.captured shouldBe ScreenEventType.ZAAK.updated(hoofdzaakUuid)
                }
            }
        }
    }
})
