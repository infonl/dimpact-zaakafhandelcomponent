/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import nl.info.client.or.`object`.model.createORObject
import nl.info.client.or.`object`.model.createObjectRecord
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.flowable.ProcessStartData
import nl.info.zac.flowable.ZaakProcessService
import nl.info.zac.productaanvraag.model.createBetrokkene
import nl.info.zac.productaanvraag.model.createProductaanvraagDimpact

class ProductaanvraagBpmnServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zaakProcessService = mockk<ZaakProcessService>()
    val productaanvraagZaakService = mockk<ProductaanvraagZaakService>()
    val productaanvraagClaimRepository = mockk<ProductaanvraagClaimRepository>()
    val productaanvraagBpmnService = ProductaanvraagBpmnService(
        ztcClientService = ztcClientService,
        zaakProcessService = zaakProcessService,
        productaanvraagZaakService = productaanvraagZaakService,
        productaanvraagClaimRepository = productaanvraagClaimRepository
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Creating and starting a zaak for a productaanvraag with a BPMN zaaktype") {
        val zaaktypeConfiguration = createZaaktypeBpmnConfiguration(
            groupId = "fakeGroupId",
            defaultBehandelaarId = "fakeBehandelaarId"
        )
        val zaaktype = createZaakType()
        val zaak = createZaak().apply { communicatiekanaalNaam = "fakeCommunicatiekanaal" }
        val productaanvraagDimpact = createProductaanvraagDimpact()
        val productaanvraagObject = createORObject(
            record = createObjectRecord(
                data = mapOf("aanvraaggegevens" to mapOf("fakeFormStep" to mapOf("fakeKey" to "fakeValue")))
            )
        )
        val productaanvraagInitiator = ProductaanvraagInitiator(
            betrokkene = createBetrokkene(),
            klantcontactContactDetails = null
        )

        given("a zaak can be created, set up from the productaanvraag and have its BPMN process started") {
            clearAllMocks()
            every { ztcClientService.readZaaktype(zaaktypeConfiguration.zaaktypeUuid) } returns zaaktype
            every {
                productaanvraagZaakService.createZaak(zaaktype, productaanvraagDimpact, productaanvraagObject)
            } returns zaak
            every {
                productaanvraagZaakService.findValidDefaultBehandelaarId(zaaktypeConfiguration, zaak)
            } returns "fakeBehandelaarId"
            every {
                productaanvraagZaakService.setUpZaakFromProductaanvraag(
                    zaak = zaak,
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    behandelaarId = "fakeBehandelaarId",
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )
            } returns productaanvraagInitiator
            every {
                zaakProcessService.start(
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    zaak = zaak,
                    zaaktype = zaaktype,
                    processStartData = any()
                )
            } just runs
            every { productaanvraagClaimRepository.markDone(productaanvraagObject.uuid) } just runs

            `when`("the zaak is created and started") {
                productaanvraagBpmnService.createAndStartZaak(
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )

                then(
                    """
                    the zaak is created, the zaak is set up from the productaanvraag before the BPMN process is
                    started with the default group and behandelaar, so that the process can use the initiator and
                    the klantcontact of the zaak, and the productaanvraag is marked as done once the process is
                    started
                    """
                ) {
                    verifyOrder {
                        productaanvraagZaakService.createZaak(zaaktype, productaanvraagDimpact, productaanvraagObject)
                        productaanvraagZaakService.setUpZaakFromProductaanvraag(
                            zaak = zaak,
                            zaaktypeConfiguration = zaaktypeConfiguration,
                            behandelaarId = "fakeBehandelaarId",
                            productaanvraagDimpact = productaanvraagDimpact,
                            productaanvraagObject = productaanvraagObject
                        )
                        zaakProcessService.start(
                            zaaktypeConfiguration = zaaktypeConfiguration,
                            zaak = zaak,
                            zaaktype = zaaktype,
                            processStartData = ProcessStartData(
                                zaakData = mapOf("fakeKey" to "fakeValue"),
                                groupId = "fakeGroupId",
                                behandelaarId = "fakeBehandelaarId",
                                communicatiekanaal = "fakeCommunicatiekanaal"
                            )
                        )
                        productaanvraagClaimRepository.markDone(productaanvraagObject.uuid)
                    }
                }
            }
        }

        given("a zaak whose BPMN process cannot be started") {
            clearAllMocks()
            every { ztcClientService.readZaaktype(zaaktypeConfiguration.zaaktypeUuid) } returns zaaktype
            every {
                productaanvraagZaakService.createZaak(zaaktype, productaanvraagDimpact, productaanvraagObject)
            } returns zaak
            every {
                productaanvraagZaakService.findValidDefaultBehandelaarId(zaaktypeConfiguration, zaak)
            } returns "fakeBehandelaarId"
            every {
                productaanvraagZaakService.setUpZaakFromProductaanvraag(
                    zaak = zaak,
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    behandelaarId = "fakeBehandelaarId",
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )
            } returns productaanvraagInitiator
            every {
                zaakProcessService.start(
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    zaak = zaak,
                    zaaktype = zaaktype,
                    processStartData = any()
                )
            } throws IllegalStateException("fakeProcessStartFailure")

            `when`("the zaak is created and started") {
                val illegalStateException = shouldThrow<IllegalStateException> {
                    productaanvraagBpmnService.createAndStartZaak(
                        zaaktypeConfiguration = zaaktypeConfiguration,
                        productaanvraagDimpact = productaanvraagDimpact,
                        productaanvraagObject = productaanvraagObject
                    )
                }

                then("the productaanvraag is not marked as done") {
                    illegalStateException.message shouldBe "fakeProcessStartFailure"
                    verify(exactly = 0) {
                        productaanvraagClaimRepository.markDone(any())
                    }
                }
            }
        }
    }
})
