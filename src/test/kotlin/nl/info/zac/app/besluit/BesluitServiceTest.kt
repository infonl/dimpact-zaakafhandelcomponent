/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.besluit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.model.createBesluit
import nl.info.client.zgw.brc.model.generated.Besluit
import nl.info.client.zgw.brc.model.generated.BesluitInformatieObject
import nl.info.client.zgw.brc.model.generated.createBesluitInformatieObject
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createBesluitType
import nl.info.zac.app.zaak.converter.RestBesluitConverter
import nl.info.zac.app.zaak.model.createRestBesluitChangeData
import nl.info.zac.app.zaak.model.createRestBesluitCreateData
import nl.info.zac.besluit.BesluitPublicationDateMissingException
import nl.info.zac.besluit.BesluitPublicationDisabledException
import nl.info.zac.besluit.BesluitResponseDateInvalidException
import nl.info.zac.besluit.BesluitResponseDateMissingException
import nl.info.zac.besluit.BesluitService
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BESLUIT_RESPONSE_DATE_MISSING_TYPE
import java.time.LocalDate
import java.util.UUID

class BesluitServiceTest : BehaviorSpec({
    val brcClientService = mockk<BrcClientService>()
    val drcClientService = mockk<DrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val restBesluitConverter = mockk<RestBesluitConverter>()

    val besluitService = BesluitService(
        brcClientService,
        drcClientService,
        ztcClientService,
        restBesluitConverter
    )

    val reactionPeriodDays = 2L

    val zaak = createZaak()
    val besluitType = createBesluitType(
        publicationPeriod = "P1D",
        reactionPeriod = "P${reactionPeriodDays}D"
    )
    val besluit = createBesluit()
    val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
    val besluitInformatieObject = createBesluitInformatieObject()

    afterEach {
        checkUnnecessaryStub()
    }

    given("Zaak, besluit creation data with publication and response dates") {
        besluitType.publicatieIndicatie(true)
        val restBesluitVastleggenGegevens = createRestBesluitCreateData(
            publicationDate = LocalDate.now(),
            lastResponseDate = LocalDate.now().plusDays(3)
        )

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType
        every { restBesluitConverter.convertToBesluit(zaak, restBesluitVastleggenGegevens) } returns besluit
        every { brcClientService.createBesluit(besluit) } returns besluit
        every {
            drcClientService.readEnkelvoudigInformatieobject(
                restBesluitVastleggenGegevens.informatieobjecten!!.first()
            )
        } returns enkelvoudigInformatieObject
        every {
            brcClientService.createBesluitInformatieobject(any<BesluitInformatieObject>(), "Aanmaken besluit")
        } returns besluitInformatieObject

        `when`("Besluit creation is triggered") {
            val besluit = besluitService.createBesluit(zaak, restBesluitVastleggenGegevens)

            then("it creates besluit and information object") {
                besluit shouldBe besluit
                verify(exactly = 1) {
                    ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
                    brcClientService.createBesluit(besluit)
                    drcClientService.readEnkelvoudigInformatieobject(
                        restBesluitVastleggenGegevens.informatieobjecten!!.first()
                    )
                }
            }
        }
    }

    given("Zaak, besluit creation data without publication and response dates") {
        besluitType.publicatieIndicatie(true)
        val restBesluitVastleggenGegevens = createRestBesluitCreateData()

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType
        every { restBesluitConverter.convertToBesluit(zaak, restBesluitVastleggenGegevens) } returns besluit
        every { brcClientService.createBesluit(besluit) } returns besluit
        every {
            drcClientService.readEnkelvoudigInformatieobject(
                restBesluitVastleggenGegevens.informatieobjecten!!.first()
            )
        } returns enkelvoudigInformatieObject
        every {
            brcClientService.createBesluitInformatieobject(any<BesluitInformatieObject>(), "Aanmaken besluit")
        } returns besluitInformatieObject

        `when`("Besluit creation is requested") {
            val besluit = besluitService.createBesluit(zaak, restBesluitVastleggenGegevens)

            then("it creates besluit and information object") {
                besluit shouldBe besluit
                verify(exactly = 1) {
                    ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
                    brcClientService.createBesluit(besluit)
                    drcClientService.readEnkelvoudigInformatieobject(
                        restBesluitVastleggenGegevens.informatieobjecten!!.first()
                    )
                }
            }
        }
    }

    given("Zaak, besluit type with disabled publication and creation data with publication date") {
        besluitType.publicatieIndicatie(false)
        val restBesluitVastleggenGegevens = createRestBesluitCreateData(
            publicationDate = LocalDate.now(),
            lastResponseDate = LocalDate.now().plusDays(3)
        )

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType

        `when`("Besluit creation is requested") {
            val exception = shouldThrow<BesluitPublicationDisabledException> {
                besluitService.createBesluit(zaak, restBesluitVastleggenGegevens)
            }

            then("it throws exception") {
                exception.message shouldBe "Besluit type with UUID '${besluitType.url.extractUuid()}' " +
                    "and name '${besluitType.omschrijving}' cannot have publication or response dates"
            }
        }
    }

    given("Zaak, besluit type with enabled publication and creation data with only publication date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitVastleggenGegevens = createRestBesluitCreateData(
            publicationDate = LocalDate.now()
        )

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType

        `when`("Besluit creation is requested") {
            val exception = shouldThrow<BesluitResponseDateMissingException> {
                besluitService.createBesluit(zaak, restBesluitVastleggenGegevens)
            }

            then("it throws exception") {
                exception.errorCode shouldBe ERROR_CODE_BESLUIT_RESPONSE_DATE_MISSING_TYPE
                exception.message shouldBe null
            }
        }
    }

    given("Zaak, besluit type with enabled publication and creation data with only response date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitVastleggenGegevens = createRestBesluitCreateData(
            lastResponseDate = LocalDate.now()
        )

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType

        `when`("Besluit creation is requested") {
            val exception = shouldThrow<BesluitPublicationDateMissingException> {
                besluitService.createBesluit(zaak, restBesluitVastleggenGegevens)
            }

            then("it throws exception") {
                exception.errorCode shouldBe ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE
                exception.message shouldBe null
            }
        }
    }

    given("Zaak, enabled publication and response date before calculated response date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitVastleggenGegevens = createRestBesluitCreateData(
            publicationDate = LocalDate.now(),
            lastResponseDate = LocalDate.now().plusDays(1)
        )

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType

        `when`("Besluit creation is requested") {
            val exception = shouldThrow<BesluitResponseDateInvalidException> {
                besluitService.createBesluit(zaak, restBesluitVastleggenGegevens)
            }

            then("it throws exception") {
                exception.message shouldBe "Response date ${restBesluitVastleggenGegevens.lastResponseDate}" +
                    " is before calculated response date " +
                    "${restBesluitVastleggenGegevens.publicationDate!!.plusDays(reactionPeriodDays)}"
            }
        }
    }

    given("Zaak, besluit and creation data with publication and response dates") {
        val restBesluitWijzigenGegevens = createRestBesluitChangeData(
            publicationDate = LocalDate.now(),
            lastResponseDate = LocalDate.now().plusDays(3)
        )
        val besluitPatchSlot = slot<Besluit>()

        every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType
        every {
            brcClientService.patchBesluit(
                restBesluitWijzigenGegevens.besluitUuid,
                capture(besluitPatchSlot),
                restBesluitWijzigenGegevens.reden
            )
        } returns besluit
        every { brcClientService.listBesluitInformatieobjecten(besluit.url) } returns listOf(besluitInformatieObject)
        every { brcClientService.deleteBesluitinformatieobject(any<UUID>()) } returns besluitInformatieObject
        every {
            drcClientService.readEnkelvoudigInformatieobject(restBesluitWijzigenGegevens.informatieobjecten!!.first())
        } returns enkelvoudigInformatieObject
        every {
            brcClientService.createBesluitInformatieobject(any<BesluitInformatieObject>(), "Wijzigen besluit")
        } returns besluitInformatieObject

        `when`("update is requested") {
            besluitType.publicatieIndicatie(true)
            besluitService.updateBesluit(besluit, restBesluitWijzigenGegevens)

            then("the besluit is patched with the supplied publication and response dates") {
                with(besluitPatchSlot.captured) {
                    publicatiedatum shouldBe restBesluitWijzigenGegevens.publicationDate
                    uiterlijkeReactiedatum shouldBe restBesluitWijzigenGegevens.lastResponseDate
                }
            }
        }
    }

    given("Zaak, besluit and creation data without publication and response dates") {
        besluitType.publicatieIndicatie(true)
        val restBesluitWijzigenGegevens = createRestBesluitChangeData()
        val besluitPatchSlot = slot<Besluit>()

        every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType
        every {
            brcClientService.patchBesluit(
                restBesluitWijzigenGegevens.besluitUuid,
                capture(besluitPatchSlot),
                restBesluitWijzigenGegevens.reden
            )
        } returns besluit
        every { brcClientService.listBesluitInformatieobjecten(besluit.url) } returns listOf(besluitInformatieObject)
        every { brcClientService.deleteBesluitinformatieobject(any<UUID>()) } returns besluitInformatieObject
        every {
            drcClientService.readEnkelvoudigInformatieobject(restBesluitWijzigenGegevens.informatieobjecten!!.first())
        } returns enkelvoudigInformatieObject
        every {
            brcClientService.createBesluitInformatieobject(any<BesluitInformatieObject>(), "Wijzigen besluit")
        } returns besluitInformatieObject

        `when`("update is requested") {
            besluitService.updateBesluit(besluit, restBesluitWijzigenGegevens)

            then("the besluit is patched with the supplied publication and response dates") {
                with(besluitPatchSlot.captured) {
                    publicatiedatum shouldBe restBesluitWijzigenGegevens.publicationDate
                    uiterlijkeReactiedatum shouldBe restBesluitWijzigenGegevens.lastResponseDate
                }
            }
        }
    }

    given("Zaak, besluit and type that cannot have publications, but publication date is supplied") {
        besluitType.publicatieIndicatie(false)
        val restBesluitWijzigenGegevens = createRestBesluitChangeData()

        every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType

        `when`("Besluit update is requested") {
            val exception = shouldThrow<BesluitPublicationDisabledException> {
                besluitService.updateBesluit(besluit, restBesluitWijzigenGegevens)
            }

            then("it throws exception") {
                exception.message shouldBe "Besluit type with UUID '${besluitType.url.extractUuid()}' " +
                    "and name '${besluitType.omschrijving}' cannot have publication or response dates"
            }
        }
    }

    given("Zaak, besluit and type with enabled publications, but without publication date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitWijzigenGegevens = createRestBesluitChangeData(
            lastResponseDate = LocalDate.now()
        )

        every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType

        `when`("Besluit update is requested") {
            besluitType.publicatieIndicatie(true)
            restBesluitWijzigenGegevens.publicationDate = null

            val exception = shouldThrow<BesluitPublicationDateMissingException> {
                besluitService.updateBesluit(besluit, restBesluitWijzigenGegevens)
            }

            then("it throws exception") {
                exception.errorCode shouldBe ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE
                exception.message shouldBe null
            }
        }
    }

    given("Zaak, besluit and type with enabled publications, but without response date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitWijzigenGegevens = createRestBesluitChangeData(
            publicationDate = LocalDate.now()
        )

        every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType

        `when`("Besluit update is requested") {
            besluitType.publicatieIndicatie(true)
            restBesluitWijzigenGegevens.publicationDate = null

            val exception = shouldThrow<BesluitPublicationDateMissingException> {
                besluitService.updateBesluit(besluit, restBesluitWijzigenGegevens)
            }

            then("it throws exception") {
                exception.errorCode shouldBe ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE
                exception.message shouldBe null
            }
        }
    }

    given("Zaak, besluit and type with enabled publications, response date before calculated response date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitWijzigenGegevens = createRestBesluitChangeData(
            publicationDate = LocalDate.now().plusDays(1),
            lastResponseDate = LocalDate.now().plusDays(1)
        )

        every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType

        `when`("Besluit update is requested") {
            val exception = shouldThrow<BesluitResponseDateInvalidException> {
                besluitService.updateBesluit(besluit, restBesluitWijzigenGegevens)
            }

            then("it throws exception") {
                exception.message shouldBe "Response date ${restBesluitWijzigenGegevens.lastResponseDate}" +
                    " is before calculated response date " +
                    "${restBesluitWijzigenGegevens.publicationDate!!.plusDays(reactionPeriodDays)}"
            }
        }
    }
})
