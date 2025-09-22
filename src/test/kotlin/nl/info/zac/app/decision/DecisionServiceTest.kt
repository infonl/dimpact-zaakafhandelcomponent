/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.decision

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.atos.client.zgw.drc.DrcClientService
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.model.createBesluit
import nl.info.client.zgw.brc.model.generated.Besluit
import nl.info.client.zgw.brc.model.generated.BesluitInformatieObject
import nl.info.client.zgw.brc.model.generated.createBesluitInformatieObject
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createBesluitType
import nl.info.zac.app.zaak.converter.RestDecisionConverter
import nl.info.zac.app.zaak.model.createRestDecisionChangeData
import nl.info.zac.app.zaak.model.createRestDecisionCreateData
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BESLUIT_RESPONSE_DATE_MISSING_TYPE
import java.time.LocalDate
import java.util.UUID

class DecisionServiceTest : BehaviorSpec({
    val brcClientService = mockk<BrcClientService>()
    val drcClientService = mockk<DrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val zgwApiService = mockk<ZGWApiService>()
    val restDecisionConverter = mockk<RestDecisionConverter>()

    val decisionService = DecisionService(
        brcClientService,
        drcClientService,
        ztcClientService,
        zrcClientService,
        zgwApiService,
        restDecisionConverter
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
        val restBesluitVastleggenGegevens = createRestDecisionCreateData(
            publicationDate = LocalDate.now(),
            lastResponseDate = LocalDate.now().plusDays(3)
        )

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType
        every { restDecisionConverter.convertToBesluit(zaak, restBesluitVastleggenGegevens) } returns besluit
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
            val besluit = decisionService.createDecision(zaak, restBesluitVastleggenGegevens)

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
        val restBesluitVastleggenGegevens = createRestDecisionCreateData()

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType
        every { restDecisionConverter.convertToBesluit(zaak, restBesluitVastleggenGegevens) } returns besluit
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
            val besluit = decisionService.createDecision(zaak, restBesluitVastleggenGegevens)

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
        val restBesluitVastleggenGegevens = createRestDecisionCreateData(
            publicationDate = LocalDate.now(),
            lastResponseDate = LocalDate.now().plusDays(3)
        )

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType

        When("Besluit creation is requested") {
            val exception = shouldThrow<DecisionPublicationDisabledException> {
                decisionService.createDecision(zaak, restBesluitVastleggenGegevens)
            }

            Then("it throws exception") {
                exception.message shouldBe "Besluit type with UUID '${besluitType.url.extractUuid()}' " +
                    "and name '${besluitType.omschrijving}' cannot have publication or response dates"
            }
        }
    }

    Given("Zaak, besluit type with enabled publication and creation data with only publication date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitVastleggenGegevens = createRestDecisionCreateData(
            publicationDate = LocalDate.now()
        )

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType

        When("Besluit creation is requested") {
            val exception = shouldThrow<DecisionResponseDateMissingException> {
                decisionService.createDecision(zaak, restBesluitVastleggenGegevens)
            }

            Then("it throws exception") {
                exception.errorCode shouldBe ERROR_CODE_BESLUIT_RESPONSE_DATE_MISSING_TYPE
                exception.message shouldBe null
            }
        }
    }

    Given("Zaak, besluit type with enabled publication and creation data with only response date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitVastleggenGegevens = createRestDecisionCreateData(
            lastResponseDate = LocalDate.now()
        )

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType

        When("Besluit creation is requested") {
            val exception = shouldThrow<DecisionPublicationDateMissingException> {
                decisionService.createDecision(zaak, restBesluitVastleggenGegevens)
            }

            Then("it throws exception") {
                exception.errorCode shouldBe ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE
                exception.message shouldBe null
            }
        }
    }

    Given("Zaak, enabled publication and response date before calculated response date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitVastleggenGegevens = createRestDecisionCreateData(
            publicationDate = LocalDate.now(),
            lastResponseDate = LocalDate.now().plusDays(1)
        )

        every {
            ztcClientService.readBesluittype(restBesluitVastleggenGegevens.besluittypeUuid)
        } returns besluitType

        When("Besluit creation is requested") {
            val exception = shouldThrow<DecisionResponseDateInvalidException> {
                decisionService.createDecision(zaak, restBesluitVastleggenGegevens)
            }

            Then("it throws exception") {
                exception.message shouldBe "Response date ${restBesluitVastleggenGegevens.lastResponseDate}" +
                    " is before calculated response date " +
                    "${restBesluitVastleggenGegevens.publicationDate!!.plusDays(reactionPeriodDays)}"
            }
        }
    }

    Given("Zaak, besluit and creation data with publication and response dates") {
        val restBesluitWijzigenGegevens = createRestDecisionChangeData(
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
            decisionService.updateDecision(zaak, besluit, restBesluitWijzigenGegevens)

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
        val restBesluitWijzigenGegevens = createRestDecisionChangeData()

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
            decisionService.updateDecision(zaak, besluit, restBesluitWijzigenGegevens)

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
        val restBesluitWijzigenGegevens = createRestDecisionChangeData()

        every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType

        When("Besluit update is requested") {
            val exception = shouldThrow<DecisionPublicationDisabledException> {
                decisionService.updateDecision(zaak, besluit, restBesluitWijzigenGegevens)
            }

            Then("it throws exception") {
                exception.message shouldBe "Besluit type with UUID '${besluitType.url.extractUuid()}' " +
                    "and name '${besluitType.omschrijving}' cannot have publication or response dates"
            }
        }
    }

    Given("Zaak, besluit and type with enabled publications, but without publication date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitWijzigenGegevens = createRestDecisionChangeData(
            lastResponseDate = LocalDate.now()
        )

        every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType

        When("Besluit update is requested") {
            besluitType.publicatieIndicatie(true)
            restBesluitWijzigenGegevens.publicationDate = null

            val exception = shouldThrow<DecisionPublicationDateMissingException> {
                decisionService.updateDecision(zaak, besluit, restBesluitWijzigenGegevens)
            }

            Then("it throws exception") {
                exception.errorCode shouldBe ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE
                exception.message shouldBe null
            }
        }
    }

    Given("Zaak, besluit and type with enabled publications, but without response date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitWijzigenGegevens = createRestDecisionChangeData(
            publicationDate = LocalDate.now()
        )

        every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType

        When("Besluit update is requested") {
            besluitType.publicatieIndicatie(true)
            restBesluitWijzigenGegevens.publicationDate = null

            val exception = shouldThrow<DecisionPublicationDateMissingException> {
                decisionService.updateDecision(zaak, besluit, restBesluitWijzigenGegevens)
            }

            Then("it throws exception") {
                exception.errorCode shouldBe ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE
                exception.message shouldBe null
            }
        }
    }

    Given("Zaak, besluit and type with enabled publications, response date before calculated response date") {
        besluitType.publicatieIndicatie(true)
        val restBesluitWijzigenGegevens = createRestDecisionChangeData(
            publicationDate = LocalDate.now().plusDays(1),
            lastResponseDate = LocalDate.now().plusDays(1)
        )

        every { ztcClientService.readBesluittype(besluit.besluittype.extractUuid()) } returns besluitType

        When("Besluit update is requested") {
            val exception = shouldThrow<DecisionResponseDateInvalidException> {
                decisionService.updateDecision(zaak, besluit, restBesluitWijzigenGegevens)
            }

            Then("it throws exception") {
                exception.message shouldBe "Response date ${restBesluitWijzigenGegevens.lastResponseDate}" +
                    " is before calculated response date " +
                    "${restBesluitWijzigenGegevens.publicationDate!!.plusDays(reactionPeriodDays)}"
            }
        }
    }
})
