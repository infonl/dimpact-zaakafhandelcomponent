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
import io.mockk.runs
import io.mockk.just
import io.mockk.slot
import jakarta.ws.rs.NotFoundException
import nl.info.client.zgw.util.extractUuid
import nl.info.zac.admin.ResultaattypeReferenceService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.zac.admin.ZaaktypeConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.exception.MultipleZaaktypeConfigurationsFoundException
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.createAutomaticEmailConfirmation
import nl.info.zac.admin.model.createMailTemplate
import nl.info.zac.admin.model.createMailtemplateKoppelingen
import nl.info.zac.admin.model.createZaakAfzender
import nl.info.zac.admin.model.createZaakbeeindigReden
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
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
import nl.info.zac.policy.PolicyService
import nl.info.zac.smartdocuments.SmartDocumentsService
import java.util.UUID

class ZaaktypeBpmnConfigurationRestServiceTest : BehaviorSpec({
    val zaaktypeBpmnProcessDefinition = createZaaktypeBpmnConfiguration(
        bpmnProcessDefinitionKey = "fakeBpmnProcessDefinitionKey"
    )
    val zaaktypeConfigurationBeheerService = mockk<ZaaktypeConfigurationBeheerService>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val policyService = mockk<PolicyService>()
    val resultaattypeReferenceService = mockk<ResultaattypeReferenceService>()
    val zaakbeeindigParameterConverter = mockk<RestZaakbeeindigParameterConverter>()
    val smartDocumentsService = mockk<SmartDocumentsService>()
    val zaaktypeBpmnConfigurationRestService =
        ZaaktypeBpmnConfigurationRestService(
            zaaktypeConfigurationService = zaaktypeConfigurationService,
            zaaktypeConfigurationBeheerService = zaaktypeConfigurationBeheerService,
            policyService = policyService,
            resultaattypeReferenceService = resultaattypeReferenceService,
            restZaakbeeindigParameterConverter = zaakbeeindigParameterConverter,
            smartDocumentsService = smartDocumentsService
        )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Reading BPMN zaaktypes") {
        given("BPMN zaaktype process definition is set-up") {
            val resultaatType = createResultaatType()
            val restResultType = resultaatType.toRestResultaatType()
            val restZaakbeeindigParameter = createRestZaakbeeindigParameter(resultaattype = restResultType)
            every { policyService.readOverigeRechten().canStartenZaak } returns true
            every {
                zaaktypeConfigurationService.listConfigurationsBoundTo(ProcessEngine.BPMN)
            } returns listOf(zaaktypeBpmnProcessDefinition)
            every {
                zaakbeeindigParameterConverter.convertZaakbeeindigParameters(any())
            } returns listOf(restZaakbeeindigParameter)
            every { resultaattypeReferenceService.findNietOntvankelijkResultaattype(any()) } returns createResultaatType()
            every { smartDocumentsService.isEnabled() } returns true

            `when`("reading BPMN zaaktypes") {
                val result = zaaktypeBpmnConfigurationRestService.getZaaktypeBpmnConfiguration(
                    "fakeBpmnProcessDefinitionKey"
                )

                then("it should return a list of BPMN zaaktypes") {
                    with(result) {
                        id shouldBe zaaktypeBpmnProcessDefinition.id
                        zaaktypeUuid shouldBe zaaktypeBpmnProcessDefinition.zaaktypeUuid
                        zaaktypeOmschrijving shouldBe zaaktypeBpmnProcessDefinition.zaaktypeOmschrijving
                        bpmnProcessDefinitionKey shouldBe "fakeBpmnProcessDefinitionKey"
                        productaanvraagtype shouldBe zaaktypeBpmnProcessDefinition.productaanvraagtype
                        groepNaam shouldBe zaaktypeBpmnProcessDefinition.groepID
                        smartDocuments?.isEnabledGlobally shouldBe true
                        smartDocuments?.isEnabledForZaaktype shouldBe zaaktypeBpmnProcessDefinition.isSmartDocumentsEnabled
                    }
                }
            }
        }

        given("No BPMN zaaktype process definition is set-up") {
            every { policyService.readOverigeRechten().canStartenZaak } returns true
            every {
                zaaktypeConfigurationService.listConfigurationsBoundTo(ProcessEngine.BPMN)
            } returns emptyList()

            `when`("reading BPMN zaaktypes") {
                val exception = shouldThrow<NotFoundException> {
                    zaaktypeBpmnConfigurationRestService.getZaaktypeBpmnConfiguration(
                        "fakeBpmnProcessDefinitionKey"
                    )
                }

                then("it should return a list of BPMN zaaktypes") {
                    exception.message shouldContain "fakeBpmnProcessDefinitionKey"
                }
            }
        }

        given("Multiple zaaktypes mapped to one process definition") {
            every { policyService.readOverigeRechten().canStartenZaak } returns true
            every {
                zaaktypeConfigurationService.listConfigurationsBoundTo(ProcessEngine.BPMN)
            } returns listOf(zaaktypeBpmnProcessDefinition, zaaktypeBpmnProcessDefinition)

            `when`("reading BPMN zaaktypes") {
                val exception = shouldThrow<MultipleZaaktypeConfigurationsFoundException> {
                    zaaktypeBpmnConfigurationRestService.getZaaktypeBpmnConfiguration(
                        "fakeBpmnProcessDefinitionKey"
                    )
                }

                then("it should return a list of BPMN zaaktypes") {
                    exception.message shouldContain "fakeBpmnProcessDefinitionKey"
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
            val savedConfiguration = createZaaktypeBpmnConfiguration(
                bpmnProcessDefinitionKey = "fakeBpmnProcessDefinitionKey"
            )
            every { policyService.readOverigeRechten().canBeheren } returns true
            every {
                zaaktypeConfigurationBeheerService.checkProductaanvraagtypeIsNotInUse(any(), any())
            } just runs
            every {
                zaaktypeConfigurationBeheerService.findConfiguration(any<UUID>())
            } returns null
            every {
                zaaktypeConfigurationBeheerService.storeConfiguration(any())
            } returns savedConfiguration
            every { resultaattypeReferenceService.findNietOntvankelijkResultaattype(any()) } returns createResultaatType()
            every { zaakbeeindigParameterConverter.convertZaakbeeindigParameters(any()) } returns emptyList()
            every { zaakbeeindigParameterConverter.toZaaktypeCompletionParameters(any()) } returns emptyList()
            every { smartDocumentsService.isEnabled() } returns true

            `when`("creating a new zaaktype BPMN configuration") {
                val result = zaaktypeBpmnConfigurationRestService.createOrUpdateZaaktypeBpmnConfiguration(
                    restZaaktypeBpmnConfiguration
                )

                then("it should return the created configuration") {
                    result.zaaktypeUuid shouldBe savedConfiguration.zaaktypeUuid
                    result.bpmnProcessDefinitionKey shouldBe savedConfiguration.processBinding?.definitionKey
                }
            }
        }

        given("A valid REST zaaktype BPMN configuration for an existing zaaktype") {
            val existingZaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration(
                bpmnProcessDefinitionKey = "fakeBpmnProcessDefinitionKey"
            )
            val updatedZaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration(
                bpmnProcessDefinitionKey = "fakeBpmnProcessDefinitionKey",
                zaaktypeCompletionParameters = setOf(
                    createZaaktypeCompletionParameters(
                        id = 1L,
                        zaakbeeindigReden = createZaakbeeindigReden(
                            id = 1234L,
                            name = "fakeZaakbeeindigName1"
                        ),
                        resultaattypeOmschrijving = "fakeResultaattype1"
                    ),
                    createZaaktypeCompletionParameters(
                        id = 2L,
                        zaakbeeindigReden = createZaakbeeindigReden(
                            id = 1235L,
                            name = "fakeZaakbeeindigName2"
                        ),
                        resultaattypeOmschrijving = "fakeResultaattype2"
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
            every { policyService.readOverigeRechten().canBeheren } returns true
            every {
                zaaktypeConfigurationBeheerService.checkProductaanvraagtypeIsNotInUse(any(), any())
            } just runs
            every {
                zaaktypeConfigurationBeheerService.findConfiguration(restZaaktypeBpmnConfiguration.zaaktypeUuid)
            } returns existingZaaktypeBpmnConfiguration
            every {
                zaaktypeConfigurationBeheerService.storeConfiguration(any())
            } returns updatedZaaktypeBpmnConfiguration
            every { zaakbeeindigParameterConverter.convertZaakbeeindigParameters(any()) } returns emptyList()
            every { zaakbeeindigParameterConverter.toZaaktypeCompletionParameters(any()) } returns emptyList()
            every { resultaattypeReferenceService.findNietOntvankelijkResultaattype(any()) } returns resultaatType
            every { resultaattypeReferenceService.readOmschrijving(restResultaattype.id) } returns "fakeNietOntvankelijk"
            every { smartDocumentsService.isEnabled() } returns true

            `when`("updating an existing zaaktype BPMN configuration") {
                val updatedRestZaaktypeBpmnConfiguration = zaaktypeBpmnConfigurationRestService.createOrUpdateZaaktypeBpmnConfiguration(
                    restZaaktypeBpmnConfiguration
                )

                then("it should return the updated configuration") {
                    with(updatedRestZaaktypeBpmnConfiguration) {
                        id shouldBe updatedZaaktypeBpmnConfiguration.id
                        zaaktypeUuid shouldBe updatedZaaktypeBpmnConfiguration.zaaktypeUuid
                        bpmnProcessDefinitionKey shouldBe updatedZaaktypeBpmnConfiguration.processBinding?.definitionKey
                        groepNaam shouldBe updatedZaaktypeBpmnConfiguration.groepID
                        productaanvraagtype shouldBe updatedZaaktypeBpmnConfiguration.productaanvraagtype
                        with(zaakNietOntvankelijkResultaattype!!) {
                            id shouldBe resultaatType.url.extractUuid()
                            naam shouldBe resultaatType.omschrijving
                            naamGeneriek shouldBe resultaatType.omschrijvingGeneriek
                            toelichting shouldBe resultaatType.toelichting
                            archiefNominatie shouldBe resultaatType.archiefnominatie.name
                            bronArchiefprocedure shouldBe resultaatType.brondatumArchiefprocedure
                            isBesluitVerplicht shouldBe resultaatType.isBesluitVerplicht()
                            isVervaldatumBesluitVerplicht shouldBe resultaatType.isVervaldatumBesluitVerplicht()
                            isDatumKenmerkVerplicht shouldBe resultaatType.isDatumKenmerkVerplicht()
                        }
                        with(zaakbeeindigParameters) {
                            this.size shouldBe 0
                        }
                    }
                }

                and("the stored configuration references the niet-ontvankelijk resultaattype by its omschrijving in ZTC") {
                    existingZaaktypeBpmnConfiguration.nietOntvankelijkResultaattypeOmschrijving shouldBe
                        "fakeNietOntvankelijk"
                }
            }
        }

        given(
            """an existing BPMN configuration with deadline warning windows, a confirmation email, a zaakafzender and
                a mailtemplate koppeling, which the BPMN REST payload does not carry"""
        ) {
            val existingZaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration(
                bpmnProcessDefinitionKey = "fakeBpmnProcessDefinitionKey"
            ).apply {
                einddatumGeplandWaarschuwing = 3
                uiterlijkeEinddatumAfdoeningWaarschuwing = 2
                zaaktypeEmailParameters = createAutomaticEmailConfirmation().also { it.zaaktypeConfiguration = this }
                setZaakAfzenders(setOf(createZaakAfzender(zaaktypeConfiguration = this)))
                setMailtemplateKoppelingen(
                    setOf(createMailtemplateKoppelingen(zaaktypeConfiguration = this, mailTemplate = createMailTemplate()))
                )
            }
            val restZaaktypeBpmnConfiguration = createRestZaaktypeBpmnConfiguration(
                id = existingZaaktypeBpmnConfiguration.id!!,
                zaaktypeUuid = existingZaaktypeBpmnConfiguration.zaaktypeUuid,
                groepNaam = "fakeChangedGroup"
            )
            val storedZaaktypeBpmnConfiguration = slot<ZaaktypeConfiguration>()
            every { policyService.readOverigeRechten().canBeheren } returns true
            every {
                zaaktypeConfigurationBeheerService.findConfiguration(restZaaktypeBpmnConfiguration.zaaktypeUuid)
            } returns existingZaaktypeBpmnConfiguration
            every {
                zaaktypeConfigurationBeheerService.storeConfiguration(capture(storedZaaktypeBpmnConfiguration))
            } answers { storedZaaktypeBpmnConfiguration.captured }
            every { zaakbeeindigParameterConverter.convertZaakbeeindigParameters(any()) } returns emptyList()
            every { zaakbeeindigParameterConverter.toZaaktypeCompletionParameters(any()) } returns emptyList()
            every { resultaattypeReferenceService.findNietOntvankelijkResultaattype(any()) } returns null
            every { smartDocumentsService.isEnabled() } returns true

            `when`("a beheerder changes its groep through the BPMN REST resource") {
                zaaktypeBpmnConfigurationRestService.createOrUpdateZaaktypeBpmnConfiguration(restZaaktypeBpmnConfiguration)

                then("the stored configuration has the new groep and keeps the settings that the payload does not carry") {
                    with(storedZaaktypeBpmnConfiguration.captured) {
                        groepID shouldBe "fakeChangedGroup"
                        einddatumGeplandWaarschuwing shouldBe 3
                        uiterlijkeEinddatumAfdoeningWaarschuwing shouldBe 2
                        zaaktypeEmailParameters shouldBe existingZaaktypeBpmnConfiguration.zaaktypeEmailParameters
                        getZaakAfzenders().size shouldBe 1
                        getMailtemplateKoppelingen().size shouldBe 1
                    }
                }
            }
        }

        given("A REST zaaktype BPMN configuration with smartDocuments enabled for zaaktype") {
            val existingConfiguration = createZaaktypeBpmnConfiguration(
                bpmnProcessDefinitionKey = "fakeBpmnProcessDefinitionKey"
            )
            val savedConfiguration = createZaaktypeBpmnConfiguration(
                bpmnProcessDefinitionKey = "fakeBpmnProcessDefinitionKey"
            )
            val restZaaktypeBpmnConfiguration = createRestZaaktypeBpmnConfiguration(
                zaaktypeUuid = existingConfiguration.zaaktypeUuid,
                smartDocuments = RestSmartDocuments(isEnabledGlobally = true, isEnabledForZaaktype = true)
            )
            val capturedConfiguration = slot<nl.info.zac.admin.model.ZaaktypeConfiguration>()
            every { policyService.readOverigeRechten().canBeheren } returns true
            every {
                zaaktypeConfigurationBeheerService.findConfiguration(restZaaktypeBpmnConfiguration.zaaktypeUuid)
            } returns existingConfiguration
            every {
                zaaktypeConfigurationBeheerService.storeConfiguration(capture(capturedConfiguration))
            } returns savedConfiguration
            every { resultaattypeReferenceService.findNietOntvankelijkResultaattype(any()) } returns createResultaatType()
            every { zaakbeeindigParameterConverter.convertZaakbeeindigParameters(any()) } returns emptyList()
            every { zaakbeeindigParameterConverter.toZaaktypeCompletionParameters(any()) } returns emptyList()
            every { smartDocumentsService.isEnabled() } returns true

            `when`("updating the configuration") {
                zaaktypeBpmnConfigurationRestService.createOrUpdateZaaktypeBpmnConfiguration(
                    restZaaktypeBpmnConfiguration
                )

                then("smartDocumentsEnabled is persisted as true") {
                    capturedConfiguration.captured.isSmartDocumentsEnabled shouldBe true
                }
            }
        }

        given("A REST zaaktype BPMN configuration without a group name") {
            val restZaaktypeBpmnConfiguration = createRestZaaktypeBpmnConfiguration(
                groepNaam = null
            )
            every { policyService.readOverigeRechten().canBeheren } returns true

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

    context("Listing BPMN configurations") {
        given("BPMN configurations bound in the system") {
            val bpmnConfig = createZaaktypeBpmnConfiguration(
                bpmnProcessDefinitionKey = "fakeBpmnProcessDefinitionKey"
            )
            every { policyService.readOverigeRechten().canStartenZaak } returns true
            every {
                zaaktypeConfigurationService.listConfigurationsBoundTo(ProcessEngine.BPMN)
            } returns listOf(bpmnConfig)
            every { resultaattypeReferenceService.findNietOntvankelijkResultaattype(any()) } returns createResultaatType()
            every { zaakbeeindigParameterConverter.convertZaakbeeindigParameters(any()) } returns emptyList()
            every { smartDocumentsService.isEnabled() } returns true

            `when`("listing BPMN configurations") {
                val list = zaaktypeBpmnConfigurationRestService.listZaaktypeBpmnConfigurations()

                then("it returns the mapped REST BPMN configurations") {
                    list.size shouldBe 1
                    list.first().bpmnProcessDefinitionKey shouldBe "fakeBpmnProcessDefinitionKey"
                }
            }
        }
    }
})
