/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.ZaakListParameters
import nl.info.client.zgw.zrc.model.ZaakUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.createZaaktypeConfigurationsUnderTest
import nl.info.zac.exception.ErrorCode.ERROR_CODE_CMMN_PROCESS_DEFINITION_CANNOT_CHANGE
import nl.info.zac.exception.ErrorCode.ERROR_CODE_PROCESS_DEFINITION_CANNOT_CHANGE
import nl.info.zac.exception.ErrorCode.ERROR_CODE_PROCESS_ENGINE_CANNOT_CHANGE
import nl.info.zac.exception.ErrorCode.ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID

class ZaaktypeConfigurationBeheerServiceTest : BehaviorSpec({
    val zaaktypeConfigurationRepository = mockk<ZaaktypeConfigurationRepository>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val smartDocumentsTemplatesService = mockk<SmartDocumentsTemplatesService>()
    val zaaktypeConfigurationVersioning = mockk<ZaaktypeConfigurationVersioning>()
    val zaaktypeConfigurationBeheerService = ZaaktypeConfigurationBeheerService(
        zaaktypeConfigurationRepository = zaaktypeConfigurationRepository,
        zaaktypeConfigurationService = zaaktypeConfigurationService,
        ztcClientService = ztcClientService,
        zrcClientService = zrcClientService,
        smartDocumentsTemplatesService = smartDocumentsTemplatesService,
        zaaktypeConfigurationVersioning = zaaktypeConfigurationVersioning
    )

    afterEach {
        checkUnnecessaryStub()
    }

    fun clearZtcCachesJustRuns() {
        every { ztcClientService.invalidateZaaktype(any()) } just runs
    }

    createZaaktypeConfigurationsUnderTest().forEach { (configurationType, createZaaktypeConfiguration) ->
        context("storing a $configurationType configuration") {
            given("a $configurationType configuration for a zaaktype version that already has a configuration") {
                val zaaktypeConfiguration = createZaaktypeConfiguration("fakeNietOntvankelijkResultaattype").apply { id = 999L }
                val storedZaaktypeConfiguration = createZaaktypeConfiguration("fakeNietOntvankelijkResultaattype").apply { id = 42L }
                every {
                    zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeConfiguration.zaaktypeUuid)
                } returns storedZaaktypeConfiguration
                every { zaaktypeConfigurationRepository.store(zaaktypeConfiguration) } returns zaaktypeConfiguration
                every { zaaktypeConfigurationService.evict(zaaktypeConfiguration.zaaktypeUuid) } just runs

                `when`("it is stored with an id that does not exist") {
                    zaaktypeConfigurationBeheerService.storeConfiguration(zaaktypeConfiguration)

                    then("the existing configuration of the zaaktype version is updated, and the cache is evicted") {
                        zaaktypeConfiguration.id shouldBe 42L
                        verifyOrder {
                            zaaktypeConfigurationRepository.store(zaaktypeConfiguration)
                            zaaktypeConfigurationService.evict(zaaktypeConfiguration.zaaktypeUuid)
                        }
                    }
                }
            }
        }

        context("handling a notification for a new version of a $configurationType zaaktype") {
            given("a $configurationType configuration of the previous version of the zaaktype") {
                val previousZaaktypeConfiguration = createZaaktypeConfiguration("fakeNietOntvankelijkResultaattype")
                val newZaaktypeUuid = UUID.randomUUID()
                val newZaaktype = createZaakType(
                    uri = URI("https://example.com/zaaktypes/$newZaaktypeUuid"),
                    omschrijving = previousZaaktypeConfiguration.zaaktypeOmschrijving
                )
                val nextZaaktypeConfiguration = createZaaktypeConfiguration("fakeNietOntvankelijkResultaattype").apply {
                    id = null
                    zaaktypeUuid = newZaaktypeUuid
                    groepID = "fakeCopiedGroup"
                }
                clearZtcCachesJustRuns()
                every { ztcClientService.readZaaktype(newZaaktype.url) } returns newZaaktype
                every { zaaktypeConfigurationRepository.findByZaaktypeUuid(newZaaktypeUuid) } returns null
                every {
                    zaaktypeConfigurationRepository.findCurrentByZaaktypeOmschrijving(newZaaktype.omschrijving)
                } returns previousZaaktypeConfiguration
                every {
                    zaaktypeConfigurationVersioning.createNextVersion(previousZaaktypeConfiguration, newZaaktype)
                } returns nextZaaktypeConfiguration
                every { zaaktypeConfigurationRepository.store(nextZaaktypeConfiguration) } returns nextZaaktypeConfiguration
                every { zaaktypeConfigurationService.evict(newZaaktypeUuid) } just runs
                every {
                    smartDocumentsTemplatesService.copySmartDocumentsTemplateMappings(
                        previousZaaktypeConfiguration.zaaktypeUuid,
                        newZaaktypeUuid
                    )
                } just runs

                `when`("the notification is handled") {
                    zaaktypeConfigurationBeheerService.updateZaaktypeConfiguration(newZaaktype.url)

                    then("the next version of the previous configuration is stored") {
                        verify(exactly = 1) { zaaktypeConfigurationRepository.store(nextZaaktypeConfiguration) }
                    }

                    and("the SmartDocuments template mappings are copied onto the new version") {
                        verify(exactly = 1) {
                            smartDocumentsTemplatesService.copySmartDocumentsTemplateMappings(
                                previousZaaktypeConfiguration.zaaktypeUuid,
                                newZaaktypeUuid
                            )
                        }
                    }
                }
            }

            given("an existing $configurationType configuration of the zaaktype version itself") {
                val existingZaaktypeConfiguration = createZaaktypeConfiguration("fakeNietOntvankelijkResultaattype").apply {
                    zaaktypeOmschrijving = "fakeExistingZaaktype$configurationType"
                }
                val zaaktype = createZaakType(
                    uri = URI("https://example.com/zaaktypes/${existingZaaktypeConfiguration.zaaktypeUuid}"),
                    omschrijving = existingZaaktypeConfiguration.zaaktypeOmschrijving
                )
                clearZtcCachesJustRuns()
                every { ztcClientService.readZaaktype(zaaktype.url) } returns zaaktype
                every {
                    zaaktypeConfigurationRepository.findByZaaktypeUuid(existingZaaktypeConfiguration.zaaktypeUuid)
                } returns existingZaaktypeConfiguration
                every {
                    zaaktypeConfigurationRepository.store(existingZaaktypeConfiguration)
                } returns existingZaaktypeConfiguration
                every { zaaktypeConfigurationService.evict(existingZaaktypeConfiguration.zaaktypeUuid) } just runs

                `when`("a notification for that zaaktype version is handled") {
                    zaaktypeConfigurationBeheerService.updateZaaktypeConfiguration(zaaktype.url)

                    then(
                        "only that configuration is stored, without a next version or SmartDocuments template mappings"
                    ) {
                        verify(exactly = 1) {
                            zaaktypeConfigurationRepository.store(existingZaaktypeConfiguration)
                        }
                        verify(exactly = 0) {
                            zaaktypeConfigurationRepository.findCurrentByZaaktypeOmschrijving(
                                existingZaaktypeConfiguration.zaaktypeOmschrijving
                            )
                            zaaktypeConfigurationVersioning.createNextVersion(any(), any())
                            smartDocumentsTemplatesService.copySmartDocumentsTemplateMappings(
                                any(),
                                existingZaaktypeConfiguration.zaaktypeUuid
                            )
                        }
                    }

                    and("that zaaktype is removed from the ZTC caches before it is read") {
                        verifyOrder {
                            ztcClientService.invalidateZaaktype(zaaktype.url)
                            ztcClientService.readZaaktype(zaaktype.url)
                        }
                    }
                }
            }

            given("$configurationType configurations of an older and a newer version of the same zaaktype") {
                val olderZaaktypeConfiguration = createZaaktypeConfiguration("fakeNietOntvankelijkResultaattype").apply {
                    groepID = "fakeOlderGroup"
                }
                val newerZaaktypeConfiguration = createZaaktypeConfiguration("fakeNietOntvankelijkResultaattype").apply {
                    groepID = "fakeNewerGroup"
                }
                val olderZaaktype = createZaakType(
                    uri = URI("https://example.com/zaaktypes/${olderZaaktypeConfiguration.zaaktypeUuid}"),
                    omschrijving = olderZaaktypeConfiguration.zaaktypeOmschrijving
                )
                clearZtcCachesJustRuns()
                every { ztcClientService.readZaaktype(olderZaaktype.url) } returns olderZaaktype
                every {
                    zaaktypeConfigurationRepository.findByZaaktypeUuid(olderZaaktypeConfiguration.zaaktypeUuid)
                } returns olderZaaktypeConfiguration
                every {
                    zaaktypeConfigurationRepository.store(olderZaaktypeConfiguration)
                } returns olderZaaktypeConfiguration
                every { zaaktypeConfigurationService.evict(olderZaaktypeConfiguration.zaaktypeUuid) } just runs

                `when`("a notification for the older version is handled") {
                    zaaktypeConfigurationBeheerService.updateZaaktypeConfiguration(olderZaaktype.url)

                    then("the configuration of the newer version is neither stored nor replaced by a next version") {
                        newerZaaktypeConfiguration.groepID shouldBe "fakeNewerGroup"
                        verify(exactly = 0) {
                            zaaktypeConfigurationRepository.store(newerZaaktypeConfiguration)
                            zaaktypeConfigurationVersioning.createNextVersion(any(), any())
                        }
                    }
                }
            }
        }

        context("checking the productaanvraagtype of a $configurationType configuration") {
            given("a productaanvraagtype that the current configuration of another zaaktype uses") {
                val otherZaaktypeConfiguration = createZaaktypeConfiguration("fakeNietOntvankelijkResultaattype").apply {
                    zaaktypeOmschrijving = "fakeOtherZaaktype"
                }
                every {
                    zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype("fakeProductaanvraagtype")
                } returns listOf(otherZaaktypeConfiguration)

                `when`("a configuration of a zaaktype with another omschrijving uses it") {
                    val exception = shouldThrow<InputValidationFailedException> {
                        zaaktypeConfigurationBeheerService.checkProductaanvraagtypeIsNotInUse(
                            productaanvraagtype = "fakeProductaanvraagtype",
                            zaaktypeOmschrijving = "fakeZaaktype"
                        )
                    }

                    then("it is rejected as already in use") {
                        exception.errorCode shouldBe ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE
                    }
                }
            }

            given("a productaanvraagtype that the current configuration of the same zaaktype uses") {
                val previousVersionConfiguration = createZaaktypeConfiguration("fakeNietOntvankelijkResultaattype").apply {
                    zaaktypeOmschrijving = "fakeZaaktype"
                }
                every {
                    zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype("fakeProductaanvraagtype")
                } returns listOf(previousVersionConfiguration)

                `when`("a configuration of a new version of that zaaktype uses it") {
                    then("it is accepted") {
                        shouldNotThrowAny {
                            zaaktypeConfigurationBeheerService.checkProductaanvraagtypeIsNotInUse(
                                productaanvraagtype = "fakeProductaanvraagtype",
                                zaaktypeOmschrijving = "fakeZaaktype"
                            )
                        }
                    }
                }
            }
        }
    }

    context("handling a zaaktype notification") {
        given("a concept zaaktype") {
            val zaaktype = createZaakType(concept = true)
            clearZtcCachesJustRuns()
            every { ztcClientService.readZaaktype(zaaktype.url) } returns zaaktype

            `when`("the notification is handled") {
                zaaktypeConfigurationBeheerService.updateZaaktypeConfiguration(zaaktype.url)

                then("no configuration is read") {
                    verify(exactly = 0) {
                        zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktype.url.extractUuid())
                    }
                }
            }
        }

        given("a zaaktype whose omschrijving has no configuration") {
            val zaaktypeUuid = UUID.randomUUID()
            val zaaktype = createZaakType(
                uri = URI("https://example.com/zaaktypes/$zaaktypeUuid"),
                omschrijving = "fakeZaaktypeWithoutConfiguration"
            )
            clearZtcCachesJustRuns()
            every { ztcClientService.readZaaktype(zaaktype.url) } returns zaaktype
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid) } returns null
            every { zaaktypeConfigurationRepository.findCurrentByZaaktypeOmschrijving(zaaktype.omschrijving) } returns null

            `when`("the notification is handled") {
                zaaktypeConfigurationBeheerService.updateZaaktypeConfiguration(zaaktype.url)

                then("no configuration is created") {
                    verify(exactly = 0) {
                        zaaktypeConfigurationRepository.store(match { it.zaaktypeUuid == zaaktypeUuid })
                    }
                }
            }
        }
    }

    context("finding a configuration") {
        given("a stored configuration") {
            val zaaktypeUuid = UUID.randomUUID()
            val zaaktypeConfiguration = ZaaktypeConfiguration().apply { this.zaaktypeUuid = zaaktypeUuid }
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid) } returns zaaktypeConfiguration

            `when`("the configuration is looked up") {
                val foundConfiguration = zaaktypeConfigurationBeheerService.findConfiguration(zaaktypeUuid)

                then("the stored configuration is returned") {
                    foundConfiguration shouldBe zaaktypeConfiguration
                }
            }
        }

        given("no stored configuration") {
            val zaaktypeUuid = UUID.randomUUID()
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid) } returns null

            `when`("the configuration is looked up") {
                val foundConfiguration = zaaktypeConfigurationBeheerService.findConfiguration(zaaktypeUuid)

                then("null is returned") {
                    foundConfiguration shouldBe null
                }
            }
        }
    }

    context("upserting existing configuration without servicenorm") {
        given("an existing configuration with deadline warning and updated zaaktype without servicenorm") {
            val zaaktypeUuid = UUID.randomUUID()
            val existingConfig = ZaaktypeConfiguration().apply {
                this.zaaktypeUuid = zaaktypeUuid
                zaaktypeOmschrijving = "fakeOriginalOmschrijving"
                einddatumGeplandWaarschuwing = 5
                creatiedatum = ZonedDateTime.now()
            }
            val updatedZaaktype = createZaakType(
                uri = URI("https://example.com/zaaktypes/$zaaktypeUuid"),
                omschrijving = "fakeUpdatedOmschrijving",
                servicenorm = null
            )
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid) } returns existingConfig
            every { zaaktypeConfigurationRepository.store(existingConfig) } returns existingConfig
            every { zaaktypeConfigurationService.evict(zaaktypeUuid) } just runs

            `when`("upsertConfiguration is called") {
                zaaktypeConfigurationBeheerService.upsertConfiguration(updatedZaaktype)

                then("einddatumGeplandWaarschuwing is cleared and omschrijving updated") {
                    existingConfig.zaaktypeOmschrijving shouldBe "fakeUpdatedOmschrijving"
                    existingConfig.einddatumGeplandWaarschuwing shouldBe null
                    verify(exactly = 1) { zaaktypeConfigurationRepository.store(existingConfig) }
                }
            }
        }
    }

    fun stubZakenOfZaaktypeVersion(zaaktypeUuid: UUID, zakenCount: Int) {
        val zaaktype = createZaakType(uri = URI("https://example.com/zaaktypen/$zaaktypeUuid"))
        every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaaktype
        every {
            zrcClientService.listZakenUuids(match<ZaakListParameters> { it.zaaktype == zaaktype.url })
        } returns Results(List(zakenCount) { ZaakUuid(UUID.randomUUID()) }, zakenCount)
    }

    context("whether the process definition of a configuration may change") {
        given("a configuration that is not bound to a process definition") {
            val zaaktypeConfiguration = ZaaktypeConfiguration().apply { zaaktypeUuid = UUID.randomUUID() }

            `when`("it is checked") {
                val isChangeable = zaaktypeConfigurationBeheerService.isProcessDefinitionChangeable(zaaktypeConfiguration)

                then("it may change, so that a beheerder can bind it for the first time") {
                    isChangeable shouldBe true
                }
            }
        }

        given("a BPMN configuration of a zaaktype version without zaken") {
            val zaaktypeConfiguration = createZaaktypeBpmnConfiguration()
            stubZakenOfZaaktypeVersion(zaaktypeConfiguration.zaaktypeUuid, zakenCount = 0)

            `when`("it is checked") {
                val isChangeable = zaaktypeConfigurationBeheerService.isProcessDefinitionChangeable(zaaktypeConfiguration)

                then("it may change, so that the first zaken of the zaaktype version start the chosen process") {
                    isChangeable shouldBe true
                }
            }
        }

        given("a BPMN configuration of a zaaktype version with a zaak") {
            val zaaktypeConfiguration = createZaaktypeBpmnConfiguration()
            stubZakenOfZaaktypeVersion(zaaktypeConfiguration.zaaktypeUuid, zakenCount = 1)

            `when`("it is checked") {
                val isChangeable = zaaktypeConfigurationBeheerService.isProcessDefinitionChangeable(zaaktypeConfiguration)

                then("it may not change, so that all zaken of a zaaktype version follow the same process") {
                    isChangeable shouldBe false
                }
            }
        }

        given("a CMMN configuration") {
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration()

            `when`("it is checked") {
                val isChangeable = zaaktypeConfigurationBeheerService.isProcessDefinitionChangeable(zaaktypeConfiguration)

                then("it may not change, without asking Open Zaak for zaken") {
                    isChangeable shouldBe false
                    verify(exactly = 0) { zrcClientService.listZakenUuids(any()) }
                }
            }
        }
    }

    context("binding a configuration to a process definition") {
        given("a BPMN configuration of a zaaktype version with a zaak") {
            val zaaktypeConfiguration = createZaaktypeBpmnConfiguration(bpmnProcessDefinitionKey = "fakeProcessA")
            val zaaktypeUuid = zaaktypeConfiguration.zaaktypeUuid
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid) } returns zaaktypeConfiguration

            `when`("it is bound to the process definition it already has") {
                then("the binding is accepted without asking Open Zaak for zaken") {
                    shouldNotThrowAny {
                        zaaktypeConfigurationBeheerService.checkProcessDefinitionMayChange(
                            zaaktypeUuid,
                            ProcessEngine.BPMN,
                            "fakeProcessA"
                        )
                    }
                    verify(exactly = 0) { zrcClientService.listZakenUuids(any()) }
                }
            }

            `when`("it is bound to another BPMN process definition") {
                stubZakenOfZaaktypeVersion(zaaktypeUuid, zakenCount = 1)

                val exception = shouldThrow<InputValidationFailedException> {
                    zaaktypeConfigurationBeheerService.checkProcessDefinitionMayChange(
                        zaaktypeUuid,
                        ProcessEngine.BPMN,
                        "fakeProcessB"
                    )
                }

                then("the binding is rejected with an error code that the beheerder sees explained") {
                    exception.errorCode shouldBe ERROR_CODE_PROCESS_DEFINITION_CANNOT_CHANGE
                    exception.message shouldBe "The process definition of the configuration of zaaktype " +
                        "'$zaaktypeUuid' cannot change from BPMN 'fakeProcessA' to BPMN 'fakeProcessB'"
                }
            }
        }

        given("a BPMN configuration of a zaaktype version without zaken") {
            val zaaktypeConfiguration = createZaaktypeBpmnConfiguration(bpmnProcessDefinitionKey = "fakeProcessA")
            val zaaktypeUuid = zaaktypeConfiguration.zaaktypeUuid
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid) } returns zaaktypeConfiguration
            stubZakenOfZaaktypeVersion(zaaktypeUuid, zakenCount = 0)

            `when`("it is bound to another BPMN process definition") {
                then("the binding is accepted") {
                    shouldNotThrowAny {
                        zaaktypeConfigurationBeheerService.checkProcessDefinitionMayChange(
                            zaaktypeUuid,
                            ProcessEngine.BPMN,
                            "fakeProcessB"
                        )
                    }
                }
            }
        }

        given("a CMMN configuration") {
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration()
            val zaaktypeUuid = zaaktypeConfiguration.zaaktypeUuid
            val caseDefinitionKey = zaaktypeConfiguration.processBinding!!.definitionKey
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid) } returns zaaktypeConfiguration

            `when`("it is bound to another CMMN case definition") {
                val exception = shouldThrow<InputValidationFailedException> {
                    zaaktypeConfigurationBeheerService.checkProcessDefinitionMayChange(
                        zaaktypeUuid,
                        ProcessEngine.CMMN,
                        "fakeOtherCaseDefinition"
                    )
                }

                then("the binding is rejected because a CMMN case definition never changes") {
                    exception.errorCode shouldBe ERROR_CODE_CMMN_PROCESS_DEFINITION_CANNOT_CHANGE
                    exception.message shouldBe "The process definition of the configuration of zaaktype " +
                        "'$zaaktypeUuid' cannot change from CMMN '$caseDefinitionKey' to CMMN 'fakeOtherCaseDefinition'"
                }
            }

            `when`("it is bound to a BPMN process definition") {
                val exception = shouldThrow<InputValidationFailedException> {
                    zaaktypeConfigurationBeheerService.checkProcessDefinitionMayChange(
                        zaaktypeUuid,
                        ProcessEngine.BPMN,
                        "fakeProcessB"
                    )
                }

                then("the binding is rejected because the engine never changes") {
                    exception.errorCode shouldBe ERROR_CODE_PROCESS_ENGINE_CANNOT_CHANGE
                }
            }
        }

        given("a BPMN configuration of a zaaktype version without zaken, bound to a CMMN case definition") {
            val zaaktypeConfiguration = createZaaktypeBpmnConfiguration(bpmnProcessDefinitionKey = "fakeProcessA")
            val zaaktypeUuid = zaaktypeConfiguration.zaaktypeUuid
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid) } returns zaaktypeConfiguration

            `when`("the binding is checked") {
                val exception = shouldThrow<InputValidationFailedException> {
                    zaaktypeConfigurationBeheerService.checkProcessDefinitionMayChange(
                        zaaktypeUuid,
                        ProcessEngine.CMMN,
                        "fakeCaseDefinition"
                    )
                }

                then("it is rejected because the engine never changes, without asking Open Zaak for zaken") {
                    exception.errorCode shouldBe ERROR_CODE_PROCESS_ENGINE_CANNOT_CHANGE
                    verify(exactly = 0) { ztcClientService.readZaaktype(zaaktypeUuid) }
                }
            }
        }

        given("a zaaktype version without a configuration") {
            val zaaktypeUuid = UUID.randomUUID()
            every { zaaktypeConfigurationRepository.findByZaaktypeUuid(zaaktypeUuid) } returns null

            `when`("it is bound to a process definition") {
                then("the binding is accepted") {
                    shouldNotThrowAny {
                        zaaktypeConfigurationBeheerService.checkProcessDefinitionMayChange(
                            zaaktypeUuid,
                            ProcessEngine.BPMN,
                            "fakeProcessB"
                        )
                    }
                }
            }
        }
    }
})
