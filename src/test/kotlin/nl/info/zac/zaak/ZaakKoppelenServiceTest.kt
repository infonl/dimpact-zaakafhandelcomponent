/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.zrc.model.generated.GerelateerdeZaak
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.event.EventingService
import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.ZaakRechten
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.search.IndexingService
import nl.info.zac.websocket.event.ScreenEvent
import nl.info.zac.websocket.event.ScreenEventType
import java.net.URI
import java.util.UUID

class ZaakKoppelenServiceTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerTest
    val eventingService = mockk<EventingService>()
    val indexingService = mockk<IndexingService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val zaakService = mockk<ZaakService>()
    val zrcClientService = mockk<ZrcClientService>()
    val zaakKoppelenService = ZaakKoppelenService(
        eventingService = eventingService,
        indexingService = indexingService,
        loggedInUserInstance = loggedInUserInstance,
        policyService = policyService,
        zaakService = zaakService,
        zrcClientService = zrcClientService
    )

    fun stubZaakToLinkTo(zaakToLinkTo: Zaak, zaakToLinkToType: ZaakType, zaakRechten: ZaakRechten = createZaakRechten()) {
        val loggedInUser = createLoggedInUser()
        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            zaakService.readZaakAndZaakTypeByZaakUUID(zaakToLinkTo.uuid)
        } returns Pair(zaakToLinkTo, zaakToLinkToType)
        every {
            policyService.readZaakRechten(zaak = zaakToLinkTo, zaaktype = zaakToLinkToType, loggedInUser = loggedInUser)
        } returns zaakRechten
    }

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

    context("Reading the zaak to link a new zaak to") {
        val newZaaktypeUuid = UUID.randomUUID()
        val newZaakType = createZaakType(uri = URI("https://example.com/zaaktypes/$newZaaktypeUuid"))

        given("an open zaak on which the user may link zaken") {
            val zaakToLinkToType = createZaakType()
            val zaakToLinkTo = createZaak(zaaktypeUri = zaakToLinkToType.url)
            stubZaakToLinkTo(zaakToLinkTo, zaakToLinkToType)

            `when`("it is read to link a new zaak to as gerelateerde zaak") {
                val zaak = zaakKoppelenService.readZaakToLinkNewZaakTo(
                    zaakUuid = zaakToLinkTo.uuid,
                    relatieType = RelatieType.GERELATEERD,
                    newZaaktypeUuid = newZaaktypeUuid
                )

                then("that zaak is returned") {
                    zaak shouldBe zaakToLinkTo
                }
            }
        }

        given("an open zaak on which the user may link zaken and whose zaaktype lists the new zaaktype as deelzaaktype") {
            val zaakToLinkToType = createZaakType().apply { deelzaaktypen = listOf(newZaakType.url) }
            val zaakToLinkTo = createZaak(zaaktypeUri = zaakToLinkToType.url)
            stubZaakToLinkTo(zaakToLinkTo, zaakToLinkToType)

            `when`("it is read to link a new zaak to as deelzaak") {
                val zaak = zaakKoppelenService.readZaakToLinkNewZaakTo(
                    zaakUuid = zaakToLinkTo.uuid,
                    relatieType = RelatieType.DEELZAAK,
                    newZaaktypeUuid = newZaaktypeUuid
                )

                then("that zaak is returned") {
                    zaak shouldBe zaakToLinkTo
                }
            }
        }

        given("an open zaak on which the user may not link zaken") {
            val zaakToLinkToType = createZaakType()
            val zaakToLinkTo = createZaak(zaaktypeUri = zaakToLinkToType.url)
            stubZaakToLinkTo(zaakToLinkTo, zaakToLinkToType, createZaakRechten(koppelen = false))

            `when`("it is read to link a new zaak to as gerelateerde zaak") {
                val policyException = shouldThrow<PolicyException> {
                    zaakKoppelenService.readZaakToLinkNewZaakTo(
                        zaakUuid = zaakToLinkTo.uuid,
                        relatieType = RelatieType.GERELATEERD,
                        newZaaktypeUuid = newZaaktypeUuid
                    )
                }

                then("the request is refused as not authorised") {
                    policyException shouldNotBe null
                }
            }
        }

        given("an open zaak that the user may not read") {
            val zaakToLinkToType = createZaakType()
            val zaakToLinkTo = createZaak(zaaktypeUri = zaakToLinkToType.url)
            stubZaakToLinkTo(zaakToLinkTo, zaakToLinkToType, createZaakRechten(lezen = false))

            `when`("it is read to link a new zaak to as gerelateerde zaak") {
                val policyException = shouldThrow<PolicyException> {
                    zaakKoppelenService.readZaakToLinkNewZaakTo(
                        zaakUuid = zaakToLinkTo.uuid,
                        relatieType = RelatieType.GERELATEERD,
                        newZaaktypeUuid = newZaaktypeUuid
                    )
                }

                then("the request is refused as not authorised") {
                    policyException shouldNotBe null
                }
            }
        }

        given("an open zaak on which the user may link zaken and whose zaaktype has another deelzaaktype") {
            val zaakToLinkToType = createZaakType().apply { deelzaaktypen = listOf(createZaakType().url) }
            val zaakToLinkTo = createZaak(zaaktypeUri = zaakToLinkToType.url)
            stubZaakToLinkTo(zaakToLinkTo, zaakToLinkToType)

            `when`("it is read to link a new zaak to as deelzaak") {
                val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                    zaakKoppelenService.readZaakToLinkNewZaakTo(
                        zaakUuid = zaakToLinkTo.uuid,
                        relatieType = RelatieType.DEELZAAK,
                        newZaaktypeUuid = newZaaktypeUuid
                    )
                }

                then("the request is refused with a validation error") {
                    inputValidationFailedException.errorCode shouldBe ErrorCode.ERROR_CODE_VALIDATION_GENERIC
                    inputValidationFailedException.message shouldBe
                        "A new zaak cannot be linked to zaak '${zaakToLinkTo.identificatie}' as DEELZAAK: " +
                        "ZAAKTYPE_DOES_NOT_ALLOW_DEELZAAK"
                }
            }
        }

        given("an open deelzaak on which the user may link zaken and whose zaaktype lists the new zaaktype") {
            val zaakToLinkToType = createZaakType().apply { deelzaaktypen = listOf(newZaakType.url) }
            val zaakToLinkTo = createZaak(zaaktypeUri = zaakToLinkToType.url, hoofdzaakUri = createZaak().url)
            stubZaakToLinkTo(zaakToLinkTo, zaakToLinkToType)

            `when`("it is read to link a new zaak to as deelzaak") {
                val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                    zaakKoppelenService.readZaakToLinkNewZaakTo(
                        zaakUuid = zaakToLinkTo.uuid,
                        relatieType = RelatieType.DEELZAAK,
                        newZaaktypeUuid = newZaaktypeUuid
                    )
                }

                then("the request is refused with a validation error") {
                    inputValidationFailedException.message shouldBe
                        "A new zaak cannot be linked to zaak '${zaakToLinkTo.identificatie}' as DEELZAAK: IS_DEELZAAK"
                }
            }
        }

        given("a closed zaak on which the user may link zaken") {
            val zaakToLinkToType = createZaakType()
            val zaakToLinkTo = createZaak(
                zaaktypeUri = zaakToLinkToType.url,
                archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN
            )
            stubZaakToLinkTo(zaakToLinkTo, zaakToLinkToType)

            `when`("it is read to link a new zaak to as gerelateerde zaak") {
                val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                    zaakKoppelenService.readZaakToLinkNewZaakTo(
                        zaakUuid = zaakToLinkTo.uuid,
                        relatieType = RelatieType.GERELATEERD,
                        newZaaktypeUuid = newZaaktypeUuid
                    )
                }

                then("the request is refused with a validation error") {
                    inputValidationFailedException.message shouldBe
                        "A new zaak cannot be linked to zaak '${zaakToLinkTo.identificatie}' as GERELATEERD: AFGEHANDELD"
                }
            }
        }

        given("a relatie type that cannot be used to link a new zaak") {
            `when`("a zaak is read to link a new zaak to as its hoofdzaak") {
                val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                    zaakKoppelenService.readZaakToLinkNewZaakTo(
                        zaakUuid = UUID.randomUUID(),
                        relatieType = RelatieType.HOOFDZAAK,
                        newZaaktypeUuid = newZaaktypeUuid
                    )
                }

                then("the request is refused with a validation error, without reading the zaak") {
                    inputValidationFailedException.errorCode shouldBe ErrorCode.ERROR_CODE_VALIDATION_GENERIC
                    inputValidationFailedException.message shouldBe
                        "RelatieType HOOFDZAAK cannot be used to link a new zaak"
                    verify(exactly = 0) { zaakService.readZaakAndZaakTypeByZaakUUID(any()) }
                }
            }
        }
    }

    context("Adding the link to a new zaak") {
        val zaakToLinkTo = createZaak()

        given("a new zaak that is linked as gerelateerde zaak") {
            val newZaak = createZaak()

            `when`("the link is added") {
                zaakKoppelenService.addLinkToNewZaak(newZaak, zaakToLinkTo, RelatieType.GERELATEERD)

                then("the zaak to link to is its only gerelateerde zaak and it has no hoofdzaak") {
                    newZaak.gerelateerdeZaken.map { it.url } shouldBe listOf(zaakToLinkTo.url)
                    newZaak.hoofdzaak shouldBe null
                }
            }
        }

        given("a new zaak that is linked as deelzaak") {
            val newZaak = createZaak()

            `when`("the link is added") {
                zaakKoppelenService.addLinkToNewZaak(newZaak, zaakToLinkTo, RelatieType.DEELZAAK)

                then("the zaak to link to is its hoofdzaak and it has no gerelateerde zaken") {
                    newZaak.hoofdzaak shouldBe zaakToLinkTo.url
                    newZaak.gerelateerdeZaken.isNullOrEmpty() shouldBe true
                }
            }
        }
    }

    context("Completing the link of a new zaak") {
        val newZaak = createZaak()
        val zaakToLinkTo = createZaak()

        given("a new zaak that was created as gerelateerde zaak") {
            val patchZaakSlot = slot<Zaak>()
            every {
                zrcClientService.patchZaak(zaakToLinkTo.uuid, capture(patchZaakSlot), "Aangemaakt als gerelateerde zaak")
            } returns zaakToLinkTo

            `when`("the link is completed") {
                zaakKoppelenService.completeLinkOfNewZaak(newZaak, zaakToLinkTo, RelatieType.GERELATEERD)

                then("the new zaak is added to the gerelateerde zaken of the zaak it was linked to") {
                    patchZaakSlot.captured.gerelateerdeZaken.map { it.url } shouldBe listOf(newZaak.url)
                }
            }
        }

        given("a new zaak that was created as deelzaak") {
            val screenEventSlot = slot<ScreenEvent>()
            every { indexingService.addOrUpdateZaakOrThrow(zaakToLinkTo.uuid, false) } just runs
            every { eventingService.send(capture(screenEventSlot)) } just runs

            `when`("the link is completed") {
                zaakKoppelenService.completeLinkOfNewZaak(newZaak, zaakToLinkTo, RelatieType.DEELZAAK)

                then("the hoofdzaak is reindexed and a screen event is sent for it, without patching it") {
                    verify(exactly = 1) { indexingService.addOrUpdateZaakOrThrow(zaakToLinkTo.uuid, false) }
                    screenEventSlot.captured shouldBe ScreenEventType.ZAAK.updated(zaakToLinkTo.uuid)
                    verify(exactly = 0) { zrcClientService.patchZaak(any(), any(), any()) }
                }
            }
        }
    }
})
