/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jakarta.ws.rs.NotFoundException
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.admin.ZaaktypeCmmnConfigurationBeheerService
import nl.info.zac.admin.exception.MultipleZaaktypeConfigurationsFoundException
import nl.info.zac.admin.model.createZaakbeeindigReden
import nl.info.zac.admin.model.createZaaktypeCompletionParameters
import nl.info.zac.app.admin.converter.RestZaakbeeindigParameterConverter
import nl.info.zac.app.admin.model.RestSmartDocuments
import nl.info.zac.app.admin.model.createRestResultaattype
import nl.info.zac.app.admin.model.createRestZaakbeeindigParameter
import nl.info.zac.app.admin.model.createRestZaaktypeBpmnConfiguration
import nl.info.zac.app.zaak.model.isBesluitVerplicht
import nl.info.zac.app.zaak.model.isDatumKenmerkVerplicht
import nl.info.zac.app.zaak.model.isVervaldatumBesluitVerplicht
import nl.info.zac.app.zaak.model.toRestResultaatType
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import nl.info.zac.policy.PolicyService
import nl.info.zac.smartdocuments.SmartDocumentsService
import java.util.UUID

class ZaaktypeBpmnConfigurationRestServiceTest : BehaviorSpec({
    val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration()
    val zaaktypeBpmnConfigurationBeheerService = mockk<ZaaktypeBpmnConfigurationBeheerService>()
    val zaaktypeBpmnConfigurationService = mockk<ZaaktypeBpmnConfigurationService>()
    val policyService = mockk<PolicyService>()
    val zaaktypeCmmnConfigurationBeheerService = mockk<ZaaktypeCmmnConfigurationBeheerService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaakbeeindigParameterConverter = mockk<RestZaakbeeindigParameterConverter>()
    val smartDocumentsService = mockk<SmartDocumentsService>()
    val zaaktypeBpmnConfigurationRestService =
        ZaaktypeBpmnConfigurationRestService(
            zaaktypeBpmnConfigurationService,
            zaaktypeBpmnConfigurationBeheerService,
            zaaktypeCmmnConfigurationBeheerService,
            policyService,
            ztcClientService,
            zaakbeeindigParameterConverter,
            smartDocumentsService
        )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Reading BPMN zaaktypes") {
        given("BPMN zaaktype process definition is set-up") {
            val resultaatType = createResultaatType()
            val restResultType = resultaatType.toRestResultaatType()
            val restZaakbeeindigParameter = createRestZaakbeeindigParameter(resultaattype = restResultType)
            every { policyService.readOverigeRechten().startenZaak } returns true
            every {
                zaaktypeBpmnConfigurationBeheerService.listConfigurations()
            } returns listOf(zaaktypeBpmnProcessDefinition)
            every {
                zaakbeeindigParameterConverter.convertZaakbeeindigParameters(any())
            } returns listOf(restZaakbeeindigParameter)
            every { ztcClientService.readResultaattype(any<UUID>()) } returns createResultaatType()
            every { smartDocumentsService.isEnabled() } returns true

            `when`("reading BPMN zaaktypes") {
                val result = zaaktypeBpmnConfigurationRestService.getZaaktypeBpmnConfiguration(
                    zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                )

                then("it should return a list of BPMN zaaktypes") {
                    with(result) {
                        id shouldBe zaaktypeBpmnProcessDefinition.id
                        zaaktypeUuid shouldBe zaaktypeBpmnProcessDefinition.zaaktypeUuid
                        zaaktypeOmschrijving shouldBe zaaktypeBpmnProcessDefinition.zaaktypeOmschrijving
                        bpmnProcessDefinitionKey shouldBe zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                        productaanvraagtype shouldBe zaaktypeBpmnProcessDefinition.productaanvraagtype
                        groepNaam shouldBe zaaktypeBpmnProcessDefinition.groepID
                        smartDocuments?.enabledGlobally shouldBe true
                        smartDocuments?.enabledForZaaktype shouldBe zaaktypeBpmnProcessDefinition.smartDocumentsEnabled
                    }
                }
            }
        }

        given("No BPMN zaaktype process definition is set-up") {
            every { policyService.readOverigeRechten().startenZaak } returns true
            every {
                zaaktypeBpmnConfigurationBeheerService.listConfigurations()
            } returns emptyList()

            `when`("reading BPMN zaaktypes") {
                val exception = shouldThrow<NotFoundException> {
                    zaaktypeBpmnConfigurationRestService.getZaaktypeBpmnConfiguration(
                        zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                    )
                }

                then("it should return a list of BPMN zaaktypes") {
                    exception.message shouldContain zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                }
            }
        }

        given("Multiple zaaktypes mapped to one process definition") {
            every { policyService.readOverigeRechten().startenZaak } returns true
            every {
                zaaktypeBpmnConfigurationBeheerService.listConfigurations()
            } returns listOf(zaaktypeBpmnProcessDefinition, zaaktypeBpmnProcessDefinition)

            `when`("reading BPMN zaaktypes") {
                val exception = shouldThrow<MultipleZaaktypeConfigurationsFoundException> {
                    zaaktypeBpmnConfigurationRestService.getZaaktypeBpmnConfiguration(
                        zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                    )
                }

                then("it should return a list of BPMN zaaktypes") {
                    exception.message shouldContain zaaktypeBpmnProcessDefinition.bpmnProcessDefinitionKey
                }
            }
        }
    }

    context("Creating or updating BPMN zaaktypes") {
        given("A valid REST zaaktype BPMN configuration for a new zaaktype") {
            val restZaaktypeBpmnConfiguration = createRestZaaktypeBpmnConfiguration(
                groepNaam = "testGroep",
                productaanvraagtype = "testProductaanvraag"
            )
            val savedConfiguration = createZaaktypeBpmnConfiguration()
            every { policyService.readOverigeRechten().beheren } returns true
            every {
                zaaktypeCmmnConfigurationBeheerService.checkIfProductaanvraagtypeIsNotAlreadyInUse(any(), any())
            } returns Unit
            every {
                zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(any(), any())
            } returns Unit
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfiguration(any<UUID>())
            } returns null
            every {
                zaaktypeBpmnConfigurationBeheerService.storeConfiguration(any())
            } returns savedConfiguration
            every { ztcClientService.readResultaattype(any<UUID>()) } returns createResultaatType()
            every { zaakbeeindigParameterConverter.convertZaakbeeindigParameters(any()) } returns emptyList()
            every { smartDocumentsService.isEnabled() } returns true

            `when`("creating a new zaaktype BPMN configuration") {
                val result = zaaktypeBpmnConfigurationRestService.createOrUpdateZaaktypeBpmnConfiguration(
                    restZaaktypeBpmnConfiguration
                )

                then("it should return the created configuration") {
                    result.zaaktypeUuid shouldBe savedConfiguration.zaaktypeUuid
                    result.bpmnProcessDefinitionKey shouldBe savedConfiguration.bpmnProcessDefinitionKey
                }
            }
        }

        given("A valid REST zaaktype BPMN configuration for an existing zaaktype") {
            val existingZaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration()
            val updatedZaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration(
                zaaktypeCompletionParameters = setOf(
                    createZaaktypeCompletionParameters(
                        id = 1L,
                        zaakbeeindigReden = createZaakbeeindigReden(
                            id = 1234L,
                            name = "fakeZaakbeeindigName1"
                        ),
                        resultaattype = UUID.randomUUID()
                    ),
                    createZaaktypeCompletionParameters(
                        id = 2L,
                        zaakbeeindigReden = createZaakbeeindigReden(
                            id = 1235L,
                            name = "fakeZaakbeeindigName2"
                        ),
                        resultaattype = UUID.randomUUID()
                    )
                )
            )
            val restResultaattype = createRestResultaattype()
            val resultaatType = createResultaatType()
            val restZaaktypeBpmnConfiguration = createRestZaaktypeBpmnConfiguration(
                id = existingZaaktypeBpmnConfiguration.id!!,
                zaaktypeUuid = existingZaaktypeBpmnConfiguration.zaaktypeUuid,
                groepNaam = "updatedGroep",
                productaanvraagtype = "updatedProductaanvraag",
                zaakNietOntvankelijkResultaattype = restResultaattype
            )
            every { policyService.readOverigeRechten().beheren } returns true
            every {
                zaaktypeCmmnConfigurationBeheerService.checkIfProductaanvraagtypeIsNotAlreadyInUse(any(), any())
            } returns Unit
            every {
                zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(any(), any())
            } returns Unit
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfiguration(restZaaktypeBpmnConfiguration.zaaktypeUuid)
            } returns existingZaaktypeBpmnConfiguration
            every {
                zaaktypeBpmnConfigurationBeheerService.storeConfiguration(any())
            } returns updatedZaaktypeBpmnConfiguration
            every { zaakbeeindigParameterConverter.convertZaakbeeindigParameters(any()) } returns emptyList()
            every { ztcClientService.readResultaattype(any<UUID>()) } returns resultaatType
            every { smartDocumentsService.isEnabled() } returns true

            `when`("updating an existing zaaktype BPMN configuration") {
                val updatedRestZaaktypeBpmnConfiguration = zaaktypeBpmnConfigurationRestService.createOrUpdateZaaktypeBpmnConfiguration(
                    restZaaktypeBpmnConfiguration
                )

                then("it should return the updated configuration") {
                    with(updatedRestZaaktypeBpmnConfiguration) {
                        id shouldBe updatedZaaktypeBpmnConfiguration.id
                        zaaktypeUuid shouldBe updatedZaaktypeBpmnConfiguration.zaaktypeUuid
                        bpmnProcessDefinitionKey shouldBe updatedZaaktypeBpmnConfiguration.bpmnProcessDefinitionKey
                        groepNaam shouldBe updatedZaaktypeBpmnConfiguration.groepID
                        productaanvraagtype shouldBe updatedZaaktypeBpmnConfiguration.productaanvraagtype
                        with(zaakNietOntvankelijkResultaattype!!) {
                            id shouldBe resultaatType.url.extractUuid()
                            naam shouldBe resultaatType.omschrijving
                            naamGeneriek shouldBe resultaatType.omschrijvingGeneriek
                            toelichting shouldBe resultaatType.toelichting
                            archiefNominatie shouldBe resultaatType.archiefnominatie.name
                            bronArchiefprocedure shouldBe resultaatType.brondatumArchiefprocedure
                            besluitVerplicht shouldBe resultaatType.isBesluitVerplicht()
                            vervaldatumBesluitVerplicht shouldBe resultaatType.isVervaldatumBesluitVerplicht()
                            datumKenmerkVerplicht shouldBe resultaatType.isDatumKenmerkVerplicht()
                        }
                        with(zaakbeeindigParameters) {
                            this.size shouldBe 0
                        }
                    }
                }
            }
        }

        given("A REST zaaktype BPMN configuration with smartDocuments enabled for zaaktype") {
            val existingConfiguration = createZaaktypeBpmnConfiguration()
            val savedConfiguration = createZaaktypeBpmnConfiguration()
            val restZaaktypeBpmnConfiguration = createRestZaaktypeBpmnConfiguration(
                zaaktypeUuid = existingConfiguration.zaaktypeUuid,
                smartDocuments = RestSmartDocuments(enabledGlobally = true, enabledForZaaktype = true)
            )
            val capturedConfiguration = slot<nl.info.zac.admin.model.ZaaktypeBpmnConfiguration>()
            every { policyService.readOverigeRechten().beheren } returns true
            every {
                zaaktypeBpmnConfigurationBeheerService.findConfiguration(restZaaktypeBpmnConfiguration.zaaktypeUuid)
            } returns existingConfiguration
            every {
                zaaktypeBpmnConfigurationBeheerService.storeConfiguration(capture(capturedConfiguration))
            } returns savedConfiguration
            every { ztcClientService.readResultaattype(any<UUID>()) } returns createResultaatType()
            every { zaakbeeindigParameterConverter.convertZaakbeeindigParameters(any()) } returns emptyList()
            every { smartDocumentsService.isEnabled() } returns true

            `when`("updating the configuration") {
                zaaktypeBpmnConfigurationRestService.createOrUpdateZaaktypeBpmnConfiguration(
                    restZaaktypeBpmnConfiguration
                )

                then("smartDocumentsEnabled is persisted as true") {
                    capturedConfiguration.captured.smartDocumentsEnabled shouldBe true
                }
            }
        }

        given("A REST zaaktype BPMN configuration without a group name") {
            val restZaaktypeBpmnConfiguration = createRestZaaktypeBpmnConfiguration(
                groepNaam = null
            )
            every { policyService.readOverigeRechten().beheren } returns true

            `when`("creating a zaaktype BPMN configuration") {
                val exception = shouldThrow<IllegalStateException> {
                    zaaktypeBpmnConfigurationRestService.createOrUpdateZaaktypeBpmnConfiguration(
                        restZaaktypeBpmnConfiguration
                    )
                }

                then("it should throw an exception") {
                    exception.message shouldContain "groepNaam must not be null"
                }
            }
        }
    }
})
