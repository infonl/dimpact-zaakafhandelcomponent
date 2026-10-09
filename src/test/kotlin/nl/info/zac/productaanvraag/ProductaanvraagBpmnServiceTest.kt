/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
    val productaanvraagBpmnService = ProductaanvraagBpmnService(
        ztcClientService = ztcClientService,
        zaakProcessService = zaakProcessService,
        productaanvraagZaakService = productaanvraagZaakService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Creating and starting a zaak for a productaanvraag with a BPMN zaaktype") {
        given("a zaak can be created, set up from the productaanvraag and have its BPMN process started") {
            clearAllMocks()
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
            } returns ProductaanvraagInitiator(
                betrokkene = createBetrokkene(),
                klantcontactContactDetails = null
            )
            every {
                zaakProcessService.start(
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    zaak = zaak,
                    zaaktype = zaaktype,
                    processStartData = any()
                )
            } just runs

            `when`("the zaak is created and started") {
                productaanvraagBpmnService.createAndStartZaak(
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )

                then(
                    """
                    the zaak is created, and the zaak is set up from the productaanvraag before the BPMN process is
                    started with the default group and behandelaar
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
                    }
                }
            }
        }
    }
})
