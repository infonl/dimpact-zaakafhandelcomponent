/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.besluit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.model.createBesluit
import nl.info.client.zgw.brc.model.createNillableDatesBesluitPatch
import nl.info.client.zgw.brc.model.generated.BesluitInformatieObject
import nl.info.client.zgw.brc.model.generated.VervalredenEnum
import nl.info.client.zgw.brc.model.generated.createBesluitInformatieObject
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createBesluitType
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BESLUIT_RESPONSE_DATE_MISSING_TYPE
import java.time.LocalDate
import java.util.UUID

class BesluitServiceTest : BehaviorSpec({
    val brcClientService = mockk<BrcClientService>()
    val drcClientService = mockk<DrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()

    val besluitService = BesluitService(
        brcClientService,
        drcClientService,
        ztcClientService
    )

    val reactionPeriodDays = 2L

    val besluitType = createBesluitType(
        publicationPeriod = "P1D",
        reactionPeriod = "P${reactionPeriodDays}D"
    )
    val besluit = createBesluit(besluittypeUri = besluitType.url)
    val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
    val besluitInformatieObject = createBesluitInformatieObject()

    afterEach {
        checkUnnecessaryStub()
    }

    context("Reading a besluit to withdraw") {
        given("a besluit") {
            val besluitUuid = UUID.randomUUID()
            val besluitToWithdraw = createBesluit()
            val vervaldatum = LocalDate.of(2026, 1, 1)
            every { brcClientService.readBesluit(besluitUuid) } returns besluitToWithdraw

            `when`("the besluit is read with a vervaldatum and vervalreden") {
                val readBesluit = besluitService.readBesluit(
                    besluitUuid = besluitUuid,
                    vervaldatum = vervaldatum,
                    vervalreden = VervalredenEnum.INGETROKKEN_OVERHEID
                )

                then("the besluit has the given vervaldatum and vervalreden") {
                    readBesluit.vervaldatum shouldBe vervaldatum
                    readBesluit.vervalreden shouldBe VervalredenEnum.INGETROKKEN_OVERHEID
                }
            }
        }
    }

    context("Creating a besluit") {
        given("a besluit with publication and response dates and an informatieobject") {
            besluitType.publicatieIndicatie(true)
            val besluitToCreate = createBesluit(
                besluittypeUri = besluitType.url,
                publicationDate = LocalDate.now(),
                reactionDate = LocalDate.now().plusDays(3)
            )
            val informatieobjectUuid = UUID.randomUUID()

            every { ztcClientService.readBesluittype(besluitType.url.extractUuid()) } returns besluitType
            every { brcClientService.createBesluit(besluitToCreate) } returns besluitToCreate
            every {
                drcClientService.readEnkelvoudigInformatieobject(informatieobjectUuid)
            } returns enkelvoudigInformatieObject
            every {
                brcClientService.createBesluitInformatieobject(any<BesluitInformatieObject>(), "Aanmaken besluit")
            } returns besluitInformatieObject

            `when`("Besluit creation is triggered") {
                val createdBesluit = besluitService.createBesluit(besluitToCreate, listOf(informatieobjectUuid))

                then("it creates besluit and information object") {
                    createdBesluit shouldBe besluitToCreate
                    verify(exactly = 1) {
                        ztcClientService.readBesluittype(besluitType.url.extractUuid())
                        brcClientService.createBesluit(besluitToCreate)
                        drcClientService.readEnkelvoudigInformatieobject(informatieobjectUuid)
                    }
                }
            }
        }

        given("a besluit without publication and response dates and an informatieobject") {
            besluitType.publicatieIndicatie(true)
            val besluitToCreate = createBesluit(besluittypeUri = besluitType.url)
            val informatieobjectUuid = UUID.randomUUID()

            every { ztcClientService.readBesluittype(besluitType.url.extractUuid()) } returns besluitType
            every { brcClientService.createBesluit(besluitToCreate) } returns besluitToCreate
            every {
                drcClientService.readEnkelvoudigInformatieobject(informatieobjectUuid)
            } returns enkelvoudigInformatieObject
            every {
                brcClientService.createBesluitInformatieobject(any<BesluitInformatieObject>(), "Aanmaken besluit")
            } returns besluitInformatieObject

            `when`("Besluit creation is requested") {
                val createdBesluit = besluitService.createBesluit(besluitToCreate, listOf(informatieobjectUuid))

                then("it creates besluit and information object") {
                    createdBesluit shouldBe besluitToCreate
                    verify(exactly = 1) {
                        ztcClientService.readBesluittype(besluitType.url.extractUuid())
                        brcClientService.createBesluit(besluitToCreate)
                        drcClientService.readEnkelvoudigInformatieobject(informatieobjectUuid)
                    }
                }
            }
        }

        given("a besluit type with disabled publication and a besluit with a publication date") {
            besluitType.publicatieIndicatie(false)
            val besluitToCreate = createBesluit(
                besluittypeUri = besluitType.url,
                publicationDate = LocalDate.now(),
                reactionDate = LocalDate.now().plusDays(3)
            )

            every { ztcClientService.readBesluittype(besluitType.url.extractUuid()) } returns besluitType

            `when`("Besluit creation is requested") {
                val exception = shouldThrow<BesluitPublicationDisabledException> {
                    besluitService.createBesluit(besluitToCreate, null)
                }

                then("it throws exception") {
                    exception.message shouldBe "Besluit type with UUID '${besluitType.url.extractUuid()}' " +
                        "and name '${besluitType.omschrijving}' cannot have publication or response dates"
                }
            }
        }

        given("a besluit type with enabled publication and a besluit with only a publication date") {
            besluitType.publicatieIndicatie(true)
            val besluitToCreate = createBesluit(besluittypeUri = besluitType.url, publicationDate = LocalDate.now())

            every { ztcClientService.readBesluittype(besluitType.url.extractUuid()) } returns besluitType

            `when`("Besluit creation is requested") {
                val exception = shouldThrow<BesluitResponseDateMissingException> {
                    besluitService.createBesluit(besluitToCreate, null)
                }

                then("it throws exception") {
                    exception.errorCode shouldBe ERROR_CODE_BESLUIT_RESPONSE_DATE_MISSING_TYPE
                    exception.message shouldBe null
                }
            }
        }

        given("a besluit type with enabled publication and a besluit with only a response date") {
            besluitType.publicatieIndicatie(true)
            val besluitToCreate = createBesluit(besluittypeUri = besluitType.url, reactionDate = LocalDate.now())

            every { ztcClientService.readBesluittype(besluitType.url.extractUuid()) } returns besluitType

            `when`("Besluit creation is requested") {
                val exception = shouldThrow<BesluitPublicationDateMissingException> {
                    besluitService.createBesluit(besluitToCreate, null)
                }

                then("it throws exception") {
                    exception.errorCode shouldBe ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE
                    exception.message shouldBe null
                }
            }
        }

        given("enabled publication and a besluit with a response date before the calculated response date") {
            besluitType.publicatieIndicatie(true)
            val publicationDate = LocalDate.now()
            val responseDate = LocalDate.now().plusDays(1)
            val besluitToCreate = createBesluit(
                besluittypeUri = besluitType.url,
                publicationDate = publicationDate,
                reactionDate = responseDate
            )

            every { ztcClientService.readBesluittype(besluitType.url.extractUuid()) } returns besluitType

            `when`("Besluit creation is requested") {
                val exception = shouldThrow<BesluitResponseDateInvalidException> {
                    besluitService.createBesluit(besluitToCreate, null)
                }

                then("it throws exception") {
                    exception.message shouldBe "Response date $responseDate" +
                        " is before calculated response date " +
                        "${publicationDate.plusDays(reactionPeriodDays)}"
                }
            }
        }
    }

    context("Updating a besluit") {
        given("a besluit and a patch with publication and response dates and an informatieobject") {
            besluitType.publicatieIndicatie(true)
            val besluitPatch = createNillableDatesBesluitPatch(
                publicatiedatum = LocalDate.now(),
                uiterlijkeReactiedatum = LocalDate.now().plusDays(3)
            )
            val informatieobjectUuid = UUID.randomUUID()

            every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType
            every {
                brcClientService.patchBesluit(besluit.url.extractUuid(), besluitPatch, "fakeReason")
            } returns besluit
            every { brcClientService.listBesluitInformatieobjecten(besluit.url) } returns listOf(besluitInformatieObject)
            every { brcClientService.deleteBesluitinformatieobject(any<UUID>()) } returns besluitInformatieObject
            every {
                drcClientService.readEnkelvoudigInformatieobject(informatieobjectUuid)
            } returns enkelvoudigInformatieObject
            every {
                brcClientService.createBesluitInformatieobject(any<BesluitInformatieObject>(), "Wijzigen besluit")
            } returns besluitInformatieObject

            `when`("update is requested") {
                besluitService.updateBesluit(
                    besluit = besluit,
                    besluitPatch = besluitPatch,
                    informatieobjectUuids = listOf(informatieobjectUuid),
                    reason = "fakeReason"
                )

                then("the besluit is patched with the patch and its informatieobjecten are replaced") {
                    verify(exactly = 1) {
                        brcClientService.patchBesluit(besluit.url.extractUuid(), besluitPatch, "fakeReason")
                        brcClientService.deleteBesluitinformatieobject(besluitInformatieObject.url.extractUuid())
                        brcClientService.createBesluitInformatieobject(
                            any<BesluitInformatieObject>(),
                            "Wijzigen besluit"
                        )
                    }
                }
            }
        }

        given("a besluit and a patch without publication and response dates and without informatieobjecten") {
            besluitType.publicatieIndicatie(true)
            val besluitPatch = createNillableDatesBesluitPatch()

            every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType
            every {
                brcClientService.patchBesluit(besluit.url.extractUuid(), besluitPatch, "fakeReason")
            } returns besluit

            `when`("update is requested") {
                besluitService.updateBesluit(
                    besluit = besluit,
                    besluitPatch = besluitPatch,
                    informatieobjectUuids = null,
                    reason = "fakeReason"
                )

                then("the besluit is patched and its informatieobjecten are left as they are") {
                    verify(exactly = 1) {
                        brcClientService.patchBesluit(besluit.url.extractUuid(), besluitPatch, "fakeReason")
                    }
                    verify(exactly = 0) {
                        brcClientService.listBesluitInformatieobjecten(any())
                    }
                }
            }
        }

        given("a besluit type that cannot have publications and a patch with a publication date") {
            besluitType.publicatieIndicatie(false)
            val besluitPatch = createNillableDatesBesluitPatch(
                publicatiedatum = LocalDate.now(),
                uiterlijkeReactiedatum = LocalDate.now().plusDays(3)
            )

            every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType

            `when`("Besluit update is requested") {
                val exception = shouldThrow<BesluitPublicationDisabledException> {
                    besluitService.updateBesluit(
                    besluit = besluit,
                    besluitPatch = besluitPatch,
                    informatieobjectUuids = null,
                    reason = "fakeReason"
                )
                }

                then("it throws exception") {
                    exception.message shouldBe "Besluit type with UUID '${besluitType.url.extractUuid()}' " +
                        "and name '${besluitType.omschrijving}' cannot have publication or response dates"
                }
            }
        }

        given("a besluit type with enabled publications and a patch without a publication date") {
            besluitType.publicatieIndicatie(true)
            val besluitPatch = createNillableDatesBesluitPatch(uiterlijkeReactiedatum = LocalDate.now())

            every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType

            `when`("Besluit update is requested") {
                val exception = shouldThrow<BesluitPublicationDateMissingException> {
                    besluitService.updateBesluit(
                    besluit = besluit,
                    besluitPatch = besluitPatch,
                    informatieobjectUuids = null,
                    reason = "fakeReason"
                )
                }

                then("it throws exception") {
                    exception.errorCode shouldBe ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE
                    exception.message shouldBe null
                }
            }
        }

        given("a besluit type with enabled publications and a patch without a response date") {
            besluitType.publicatieIndicatie(true)
            val besluitPatch = createNillableDatesBesluitPatch(publicatiedatum = LocalDate.now())

            every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType

            `when`("Besluit update is requested") {
                val exception = shouldThrow<BesluitResponseDateMissingException> {
                    besluitService.updateBesluit(
                    besluit = besluit,
                    besluitPatch = besluitPatch,
                    informatieobjectUuids = null,
                    reason = "fakeReason"
                )
                }

                then("it throws exception") {
                    exception.errorCode shouldBe ERROR_CODE_BESLUIT_RESPONSE_DATE_MISSING_TYPE
                    exception.message shouldBe null
                }
            }
        }

        given("enabled publications and a patch with a response date before the calculated response date") {
            besluitType.publicatieIndicatie(true)
            val publicationDate = LocalDate.now().plusDays(1)
            val responseDate = LocalDate.now().plusDays(1)
            val besluitPatch = createNillableDatesBesluitPatch(
                publicatiedatum = publicationDate,
                uiterlijkeReactiedatum = responseDate
            )

            every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType

            `when`("Besluit update is requested") {
                val exception = shouldThrow<BesluitResponseDateInvalidException> {
                    besluitService.updateBesluit(
                    besluit = besluit,
                    besluitPatch = besluitPatch,
                    informatieobjectUuids = null,
                    reason = "fakeReason"
                )
                }

                then("it throws exception") {
                    exception.message shouldBe "Response date $responseDate" +
                        " is before calculated response date " +
                        "${publicationDate.plusDays(reactionPeriodDays)}"
                }
            }
        }
    }
})
