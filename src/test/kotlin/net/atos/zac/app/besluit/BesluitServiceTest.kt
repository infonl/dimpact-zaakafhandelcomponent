/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.besluit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.brc.model.createBesluit
import net.atos.client.zgw.brc.model.generated.Besluit
import net.atos.client.zgw.brc.model.generated.BesluitInformatieObject
import net.atos.client.zgw.brc.model.generated.createBesluitInformatieObject
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObject
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.util.extractUuid
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createBesluitType
import net.atos.zac.app.zaak.converter.RestBesluitConverter
import net.atos.zac.app.zaak.model.createRestBesluitVastleggenGegevens
import net.atos.zac.app.zaak.model.createRestBesluitWijzigenGegevens
import java.time.LocalDate
import java.util.UUID

class BesluitServiceTest : BehaviorSpec({
    val brcClientService = mockk<BrcClientService>()
    val drcClientService = mockk<DrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val zgwApiService = mockk<ZGWApiService>()
    val restBesluitConverter = mockk<RestBesluitConverter>()

    val besluitService = BesluitService(
        brcClientService,
        drcClientService,
        ztcClientService,
        zrcClientService,
        zgwApiService,
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

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Zaak, besluit creation data with publication and response dates") {
        besluitType.publicatieIndicatie(true)
        val restBesluitVastleggenGegevens = createRestBesluitVastleggenGegevens(
            publicationDate = LocalDate.now(),
            lastResponseDate = LocalDate.now().plusDays(3)
        )

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType
        every { restBesluitConverter.convertToBesluit(zaak, restBesluitVastleggenGegevens) } returns besluit
        every {
            zgwApiService.createResultaatForZaak(zaak, restBesluitVastleggenGegevens.resultaattypeUuid, null)
        } just runs
        every { brcClientService.createBesluit(besluit) } returns besluit
        every {
            drcClientService.readEnkelvoudigInformatieobject(
                restBesluitVastleggenGegevens.informatieobjecten!!.first()
            )
        } returns enkelvoudigInformatieObject
        every {
            brcClientService.createBesluitInformatieobject(any<BesluitInformatieObject>(), "Aanmaken besluit")
        } returns besluitInformatieObject

        When("Besluit creation is triggered") {
            val besluit = besluitService.createBesluit(zaak, restBesluitVastleggenGegevens)

            Then("it creates besluit and information object") {
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

    Given("Zaak, besluit creation data without publication and response dates") {
        besluitType.publicatieIndicatie(true)
        val restBesluitVastleggenGegevens = createRestBesluitVastleggenGegevens()

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType
        every { restBesluitConverter.convertToBesluit(zaak, restBesluitVastleggenGegevens) } returns besluit
        every {
            zgwApiService.createResultaatForZaak(zaak, restBesluitVastleggenGegevens.resultaattypeUuid, null)
        } just runs
        every { brcClientService.createBesluit(besluit) } returns besluit
        every {
            drcClientService.readEnkelvoudigInformatieobject(
                restBesluitVastleggenGegevens.informatieobjecten!!.first()
            )
        } returns enkelvoudigInformatieObject
        every {
            brcClientService.createBesluitInformatieobject(any<BesluitInformatieObject>(), "Aanmaken besluit")
        } returns besluitInformatieObject

        When("Besluit creation is requested") {
            val besluit = besluitService.createBesluit(zaak, restBesluitVastleggenGegevens)

            Then("it creates besluit and information object") {
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

    Given("Zaak, besluit type with disabled publication and creation data with publication date") {
        besluitType.publicatieIndicatie(false)
        val restBesluitVastleggenGegevens = createRestBesluitVastleggenGegevens(
            publicationDate = LocalDate.now(),
            lastResponseDate = LocalDate.now().plusDays(3)
        )

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType

        When("Besluit creation is requested") {
            val exception = shouldThrow<BesluitPublicationDisabledException> {
                besluitService.createBesluit(zaak, restBesluitVastleggenGegevens)
            }

            Then("it throws exception") {
                exception.message shouldBe "Besluit type with UUID '${besluitType.url.extractUuid()}' " +
                        "and name '${besluitType.omschrijving}' cannot have publication or response dates"
            }
        }
    }

    Given("Zaak, besluit type with enabled publication and creation data with only publication date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitVastleggenGegevens = createRestBesluitVastleggenGegevens(
            publicationDate = LocalDate.now()
        )

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType

        When("Besluit creation is requested") {
            val exception = shouldThrow<BesluitResponseDateMissingException> {
                besluitService.createBesluit(zaak, restBesluitVastleggenGegevens)
            }

            Then("it throws exception") {
                exception.message shouldBe "Missing response date"
            }
        }
    }

    Given("Zaak, besluit type with enabled publication and creation data with only response date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitVastleggenGegevens = createRestBesluitVastleggenGegevens(
            lastResponseDate = LocalDate.now()
        )

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType

        When("Besluit creation is requested") {
            val exception = shouldThrow<BesluitPublicationDateMissingException> {
                besluitService.createBesluit(zaak, restBesluitVastleggenGegevens)
            }

            Then("it throws exception") {
                exception.message shouldBe "Missing publication date"
            }
        }
    }

    Given("Zaak, enabled publication and response date before calculated response date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitVastleggenGegevens = createRestBesluitVastleggenGegevens(
            publicationDate = LocalDate.now(),
            lastResponseDate = LocalDate.now().plusDays(1)
        )

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType

        When("Besluit creation is requested") {
            val exception = shouldThrow<BesluitResponseDateInvalidException> {
                besluitService.createBesluit(zaak, restBesluitVastleggenGegevens)
            }

            Then("it throws exception") {
                exception.message shouldBe "Response date ${restBesluitVastleggenGegevens.lastResponseDate}" +
                    " is before calculated response date " +
                    "${restBesluitVastleggenGegevens.publicationDate!!.plusDays(reactionPeriodDays)}"
            }
        }
    }

    Given("Zaak, besluit and creation data with publication and response dates") {
        val restBesluitWijzigenGegevens = createRestBesluitWijzigenGegevens(
            publicationDate = LocalDate.now(),
            lastResponseDate = LocalDate.now().plusDays(3)
        )

        every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType
        every { brcClientService.updateBesluit(any<Besluit>(), restBesluitWijzigenGegevens.reden) } returns besluit
        every { brcClientService.listBesluitInformatieobjecten(besluit.url) } returns listOf(besluitInformatieObject)
        every { brcClientService.deleteBesluitinformatieobject(any<UUID>()) } returns besluitInformatieObject
        every {
            drcClientService.readEnkelvoudigInformatieobject(restBesluitWijzigenGegevens.informatieobjecten!!.first())
        } returns enkelvoudigInformatieObject
        every {
            brcClientService.createBesluitInformatieobject(any<BesluitInformatieObject>(), "Wijzigen besluit")
        } returns besluitInformatieObject

        When("update is requested") {
            besluitType.publicatieIndicatie(true)
            besluitService.updateBesluit(zaak, besluit, restBesluitWijzigenGegevens)

            Then("update is executed correctly") {
                besluit shouldBe besluit
                with(besluit) {
                    publicatiedatum shouldBe restBesluitWijzigenGegevens.publicationDate
                    uiterlijkeReactiedatum shouldBe restBesluitWijzigenGegevens.lastResponseDate
                }
            }
        }
    }

    Given("Zaak, besluit and creation data without publication and response dates") {
        besluitType.publicatieIndicatie(true)
        val restBesluitWijzigenGegevens = createRestBesluitWijzigenGegevens()

        every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType
        every { brcClientService.updateBesluit(any<Besluit>(), restBesluitWijzigenGegevens.reden) } returns besluit
        every { brcClientService.listBesluitInformatieobjecten(besluit.url) } returns listOf(besluitInformatieObject)
        every { brcClientService.deleteBesluitinformatieobject(any<UUID>()) } returns besluitInformatieObject
        every {
            drcClientService.readEnkelvoudigInformatieobject(restBesluitWijzigenGegevens.informatieobjecten!!.first())
        } returns enkelvoudigInformatieObject
        every {
            brcClientService.createBesluitInformatieobject(any<BesluitInformatieObject>(), "Wijzigen besluit")
        } returns besluitInformatieObject

        When("update is requested") {
            besluitService.updateBesluit(zaak, besluit, restBesluitWijzigenGegevens)

            Then("update is executed correctly") {
                besluit shouldBe besluit
                with(besluit) {
                    publicatiedatum shouldBe restBesluitWijzigenGegevens.publicationDate
                    uiterlijkeReactiedatum shouldBe restBesluitWijzigenGegevens.lastResponseDate
                }
            }
        }
    }

    Given("Zaak, besluit and type that cannot have publications, but publication date is supplied") {
        besluitType.publicatieIndicatie(false)
        val restBesluitWijzigenGegevens = createRestBesluitWijzigenGegevens()

        every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType

        When("Besluit update is requested") {
            val exception = shouldThrow<BesluitPublicationDisabledException> {
                besluitService.updateBesluit(zaak, besluit, restBesluitWijzigenGegevens)
            }

            Then("it throws exception") {
                exception.message shouldBe "Besluit type with UUID '${besluitType.url.extractUuid()}' " +
                        "and name '${besluitType.omschrijving}' cannot have publication or response dates"
            }
        }
    }

    Given("Zaak, besluit and type with enabled publications, but without publication date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitWijzigenGegevens = createRestBesluitWijzigenGegevens(
            lastResponseDate = LocalDate.now()
        )

        every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType

        When("Besluit update is requested") {
            besluitType.publicatieIndicatie(true)
            restBesluitWijzigenGegevens.publicationDate = null

            val exception = shouldThrow<BesluitPublicationDateMissingException> {
                besluitService.updateBesluit(zaak, besluit, restBesluitWijzigenGegevens)
            }

            Then("it throws exception") {
                exception.message shouldBe "Missing publication date"
            }
        }
    }

    Given("Zaak, besluit and type with enabled publications, but without response date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitWijzigenGegevens = createRestBesluitWijzigenGegevens(
            publicationDate = LocalDate.now()
        )

        every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType

        When("Besluit update is requested") {
            besluitType.publicatieIndicatie(true)
            restBesluitWijzigenGegevens.publicationDate = null

            val exception = shouldThrow<BesluitPublicationDateMissingException> {
                besluitService.updateBesluit(zaak, besluit, restBesluitWijzigenGegevens)
            }

            Then("it throws exception") {
                exception.message shouldBe "Missing publication date"
            }
        }
    }

    Given("Zaak, besluit and type with enabled publications, response date before calculated response date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitWijzigenGegevens = createRestBesluitWijzigenGegevens(
            publicationDate = LocalDate.now().plusDays(1),
            lastResponseDate = LocalDate.now().plusDays(1)
        )

        every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType

        When("Besluit update is requested") {
            val exception = shouldThrow<BesluitResponseDateInvalidException> {
                besluitService.updateBesluit(zaak, besluit, restBesluitWijzigenGegevens)
            }

            Then("it throws exception") {
                exception.message shouldBe "Response date ${restBesluitWijzigenGegevens.lastResponseDate}" +
                    " is before calculated response date " +
                    "${restBesluitWijzigenGegevens.publicationDate!!.plusDays(reactionPeriodDays)}"
            }
        }
    }
})
