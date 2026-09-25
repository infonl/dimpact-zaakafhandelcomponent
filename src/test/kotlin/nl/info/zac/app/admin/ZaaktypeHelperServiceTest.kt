/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.date.shouldNotBeBefore
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.ZaaktypeHelperService
import nl.info.zac.admin.model.ZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.createBetrokkeneKoppelingen
import nl.info.zac.admin.model.createHumanTaskParameters
import nl.info.zac.admin.model.createHumanTaskReferentieTabel
import nl.info.zac.admin.model.createReferenceTable
import nl.info.zac.admin.model.createReferenceTableValue
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.createZaaktypeBrpParameters
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import java.net.URI
import java.util.UUID

class ZaaktypeHelperServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeHelperService = ZaaktypeHelperService(ztcClientService)

    afterEach {
        checkUnnecessaryStub()
    }

    fun resultaattypeUri(uuid: UUID) = URI("https://example.com/resultaattype/$uuid")

    fun zaakbeeindigReden(id: Long, naam: String) = ZaakbeeindigReden().apply {
        this.id = id
        this.naam = naam
    }

    fun completionParameter(
        zaaktypeConfiguration: ZaaktypeConfiguration,
        zaakbeeindigReden: ZaakbeeindigReden,
        resultaattype: UUID
    ) = ZaaktypeCompletionParameters().apply {
        this.zaaktypeConfiguration = zaaktypeConfiguration
        this.zaakbeeindigReden = zaakbeeindigReden
        this.resultaattype = resultaattype
    }

    data class ZaaktypeConfigurationUnderTest(
        val configurationType: String,
        val create: (UUID) -> ZaaktypeConfiguration
    )

    listOf(
        ZaaktypeConfigurationUnderTest("CMMN") {
            createZaaktypeCmmnConfiguration(nietOntvankelijkResultaattype = it)
        },
        ZaaktypeConfigurationUnderTest("BPMN") {
            createZaaktypeBpmnConfiguration(
                nietOntvankelijkResultaattype = it,
                bpmnProcessDefinitionKey = "fakeBpmnProcessDefinitionKey"
            )
        }
    ).forEach { (configurationType, createZaaktypeConfiguration) ->
        context("mapZaakbeeindigGegevens of a $configurationType zaaktype configuration") {
            given("a previous configuration whose resultaattypen are not the first ones of the new zaaktype") {
                val previousAfgebrokenUuid = UUID.randomUUID()
                val previousToegekendUuid = UUID.randomUUID()
                val previousNietOntvankelijkUuid = UUID.randomUUID()

                val newVerlengdUuid = UUID.randomUUID()
                val newToegekendUuid = UUID.randomUUID()
                val newNietOntvankelijkUuid = UUID.randomUUID()
                val newAfgebrokenUuid = UUID.randomUUID()

                val newZaaktype = createZaakType(
                    resultTypes = listOf(
                        resultaattypeUri(newVerlengdUuid),
                        resultaattypeUri(newToegekendUuid),
                        resultaattypeUri(newNietOntvankelijkUuid),
                        resultaattypeUri(newAfgebrokenUuid)
                    )
                )

                every { ztcClientService.readResultaattype(resultaattypeUri(newVerlengdUuid)) } returns
                    createResultaatType(url = resultaattypeUri(newVerlengdUuid), omschrijving = "Verlengd")
                every { ztcClientService.readResultaattype(resultaattypeUri(newToegekendUuid)) } returns
                    createResultaatType(url = resultaattypeUri(newToegekendUuid), omschrijving = "Toegekend")
                every { ztcClientService.readResultaattype(resultaattypeUri(newNietOntvankelijkUuid)) } returns
                    createResultaatType(
                        url = resultaattypeUri(newNietOntvankelijkUuid),
                        omschrijving = "Niet ontvankelijk"
                    )
                every { ztcClientService.readResultaattype(resultaattypeUri(newAfgebrokenUuid)) } returns
                    createResultaatType(url = resultaattypeUri(newAfgebrokenUuid), omschrijving = "Afgebroken")
                every { ztcClientService.readResultaattype(previousAfgebrokenUuid) } returns
                    createResultaatType(url = resultaattypeUri(previousAfgebrokenUuid), omschrijving = "Afgebroken")
                every { ztcClientService.readResultaattype(previousToegekendUuid) } returns
                    createResultaatType(url = resultaattypeUri(previousToegekendUuid), omschrijving = "Toegekend")
                every { ztcClientService.readResultaattype(previousNietOntvankelijkUuid) } returns
                    createResultaatType(
                        url = resultaattypeUri(previousNietOntvankelijkUuid),
                        omschrijving = "Niet ontvankelijk"
                    )

                val previousZaaktypeConfiguration = createZaaktypeConfiguration(previousNietOntvankelijkUuid)
                previousZaaktypeConfiguration.setZaakbeeindigParameters(
                    listOf(
                        completionParameter(
                            previousZaaktypeConfiguration,
                            zaakbeeindigReden(id = 1L, naam = "Zaak is afgebroken"),
                            previousAfgebrokenUuid
                        ),
                        completionParameter(
                            previousZaaktypeConfiguration,
                            zaakbeeindigReden(id = 2L, naam = "Zaak is toegekend"),
                            previousToegekendUuid
                        )
                    )
                )
                val newZaaktypeConfiguration = createZaaktypeConfiguration(UUID.randomUUID())

                `when`("the zaakbeeindig gegevens are mapped to the new configuration") {
                    zaaktypeHelperService.mapZaakbeeindigGegevens(
                        previousZaaktypeConfiguration,
                        newZaaktypeConfiguration,
                        newZaaktype
                    )

                    then("every parameter is matched on omschrijving instead of on position") {
                        val zaakbeeindigParameters = newZaaktypeConfiguration.getZaakbeeindigParameters()
                        zaakbeeindigParameters shouldHaveSize 2
                        zaakbeeindigParameters.first {
                            it.zaakbeeindigReden.id == 1L
                        }.resultaattype shouldBe newAfgebrokenUuid
                        zaakbeeindigParameters.first {
                            it.zaakbeeindigReden.id == 2L
                        }.resultaattype shouldBe newToegekendUuid
                    }

                    and("the niet-ontvankelijk resultaattype is matched on omschrijving as well") {
                        newZaaktypeConfiguration.nietOntvankelijkResultaattype shouldBe newNietOntvankelijkUuid
                    }
                }
            }

            given("a previous configuration with a resultaattype that no longer exists in the new zaaktype") {
                val previousRemovedUuid = UUID.randomUUID()
                val previousNietOntvankelijkUuid = UUID.randomUUID()
                val newToegekendUuid = UUID.randomUUID()

                val newZaaktype = createZaakType(resultTypes = listOf(resultaattypeUri(newToegekendUuid)))

                every { ztcClientService.readResultaattype(resultaattypeUri(newToegekendUuid)) } returns
                    createResultaatType(url = resultaattypeUri(newToegekendUuid), omschrijving = "Toegekend")
                every { ztcClientService.readResultaattype(previousRemovedUuid) } returns
                    createResultaatType(url = resultaattypeUri(previousRemovedUuid), omschrijving = "Verwijderd")
                every { ztcClientService.readResultaattype(previousNietOntvankelijkUuid) } returns
                    createResultaatType(
                        url = resultaattypeUri(previousNietOntvankelijkUuid),
                        omschrijving = "Niet ontvankelijk"
                    )

                val previousZaaktypeConfiguration = createZaaktypeConfiguration(previousNietOntvankelijkUuid)
                previousZaaktypeConfiguration.setZaakbeeindigParameters(
                    listOf(
                        completionParameter(
                            previousZaaktypeConfiguration,
                            zaakbeeindigReden(id = 1L, naam = "Zaak is verwijderd"),
                            previousRemovedUuid
                        )
                    )
                )
                val newZaaktypeConfiguration = createZaaktypeConfiguration(UUID.randomUUID())

                `when`("the zaakbeeindig gegevens are mapped to the new configuration") {
                    zaaktypeHelperService.mapZaakbeeindigGegevens(
                        previousZaaktypeConfiguration,
                        newZaaktypeConfiguration,
                        newZaaktype
                    )

                    then("the unmatched parameters are not copied instead of being mapped to an arbitrary resultaattype") {
                        newZaaktypeConfiguration.getZaakbeeindigParameters() shouldHaveSize 0
                        newZaaktypeConfiguration.nietOntvankelijkResultaattype.shouldBeNull()
                    }
                }
            }

            given("a previous configuration without any zaak beeindigen gegevens") {
                val newToegekendUuid = UUID.randomUUID()
                val newZaaktype = createZaakType(resultTypes = listOf(resultaattypeUri(newToegekendUuid)))

                every { ztcClientService.readResultaattype(resultaattypeUri(newToegekendUuid)) } returns
                    createResultaatType(url = resultaattypeUri(newToegekendUuid), omschrijving = "Toegekend")

                val previousZaaktypeConfiguration = createZaaktypeConfiguration(UUID.randomUUID()).apply {
                    nietOntvankelijkResultaattype = null
                }
                val newZaaktypeConfiguration = createZaaktypeConfiguration(UUID.randomUUID())

                `when`("the zaakbeeindig gegevens are mapped to the new configuration") {
                    zaaktypeHelperService.mapZaakbeeindigGegevens(
                        previousZaaktypeConfiguration,
                        newZaaktypeConfiguration,
                        newZaaktype
                    )

                    then("the new configuration keeps an empty collection of parameters instead of a null one") {
                        newZaaktypeConfiguration.zaaktypeCompletionParameters.shouldNotBeNull().shouldBeEmpty()
                    }

                    and("the niet-ontvankelijk resultaattype of the new configuration is cleared") {
                        newZaaktypeConfiguration.nietOntvankelijkResultaattype.shouldBeNull()
                    }
                }
            }
        }

        context("updateZaakbeeindigGegevens of a $configurationType zaaktype configuration") {
            given("an existing configuration whose zaaktype got new resultaattype UUIDs") {
                val previousToegekendUuid = UUID.randomUUID()
                val previousNietOntvankelijkUuid = UUID.randomUUID()
                val newAfgebrokenUuid = UUID.randomUUID()
                val newToegekendUuid = UUID.randomUUID()
                val newNietOntvankelijkUuid = UUID.randomUUID()

                val newZaaktype = createZaakType(
                    resultTypes = listOf(
                        resultaattypeUri(newAfgebrokenUuid),
                        resultaattypeUri(newToegekendUuid),
                        resultaattypeUri(newNietOntvankelijkUuid)
                    )
                )

                every { ztcClientService.readResultaattype(resultaattypeUri(newAfgebrokenUuid)) } returns
                    createResultaatType(url = resultaattypeUri(newAfgebrokenUuid), omschrijving = "Afgebroken")
                every { ztcClientService.readResultaattype(resultaattypeUri(newToegekendUuid)) } returns
                    createResultaatType(url = resultaattypeUri(newToegekendUuid), omschrijving = "Toegekend")
                every { ztcClientService.readResultaattype(resultaattypeUri(newNietOntvankelijkUuid)) } returns
                    createResultaatType(
                        url = resultaattypeUri(newNietOntvankelijkUuid),
                        omschrijving = "Niet ontvankelijk"
                    )
                every { ztcClientService.readResultaattype(previousToegekendUuid) } returns
                    createResultaatType(url = resultaattypeUri(previousToegekendUuid), omschrijving = "Toegekend")
                every { ztcClientService.readResultaattype(previousNietOntvankelijkUuid) } returns
                    createResultaatType(
                        url = resultaattypeUri(previousNietOntvankelijkUuid),
                        omschrijving = "Niet ontvankelijk"
                    )

                val zaaktypeConfiguration = createZaaktypeConfiguration(previousNietOntvankelijkUuid)
                zaaktypeConfiguration.setZaakbeeindigParameters(
                    listOf(
                        completionParameter(
                            zaaktypeConfiguration,
                            zaakbeeindigReden(id = 2L, naam = "Zaak is toegekend"),
                            previousToegekendUuid
                        )
                    )
                )

                `when`("the zaakbeeindig gegevens are updated in place") {
                    zaaktypeHelperService.updateZaakbeeindigGegevens(zaaktypeConfiguration, newZaaktype)

                    then("the stored resultaattypen are updated instead of being silently discarded") {
                        val zaakbeeindigParameters = zaaktypeConfiguration.getZaakbeeindigParameters()
                        zaakbeeindigParameters shouldHaveSize 1
                        zaakbeeindigParameters.first().resultaattype shouldBe newToegekendUuid
                        zaaktypeConfiguration.nietOntvankelijkResultaattype shouldBe newNietOntvankelijkUuid
                    }
                }
            }

            given("a configuration that has already been remapped onto the resultaattypen of its own zaaktype") {
                val toegekendUuid = UUID.randomUUID()
                val newZaaktype = createZaakType(resultTypes = listOf(resultaattypeUri(toegekendUuid)))

                every { ztcClientService.readResultaattype(resultaattypeUri(toegekendUuid)) } returns
                    createResultaatType(url = resultaattypeUri(toegekendUuid), omschrijving = "Toegekend")
                every { ztcClientService.readResultaattype(toegekendUuid) } returns
                    createResultaatType(url = resultaattypeUri(toegekendUuid), omschrijving = "Toegekend")

                val existingZaaktypeConfiguration = createZaaktypeConfiguration(toegekendUuid)
                existingZaaktypeConfiguration.setZaakbeeindigParameters(
                    listOf(
                        completionParameter(
                            existingZaaktypeConfiguration,
                            zaakbeeindigReden(id = 2L, naam = "Zaak is toegekend"),
                            toegekendUuid
                        )
                    )
                )
                val persistedParameter = existingZaaktypeConfiguration.getZaakbeeindigParameters().first().apply {
                    id = 1234L
                }

                `when`("the same zaaktype notification is handled twice") {
                    zaaktypeHelperService.updateZaakbeeindigGegevens(existingZaaktypeConfiguration, newZaaktype)
                    zaaktypeHelperService.updateZaakbeeindigGegevens(existingZaaktypeConfiguration, newZaaktype)

                    then("the persisted parameter is updated in place instead of being replaced by a new one") {
                        val zaakbeeindigParameters = existingZaaktypeConfiguration.getZaakbeeindigParameters()
                        zaakbeeindigParameters shouldHaveSize 1
                        with(zaakbeeindigParameters.first()) {
                            this shouldBeSameInstanceAs persistedParameter
                            id shouldBe 1234L
                            resultaattype shouldBe toegekendUuid
                        }
                    }
                }
            }
        }

        context("copyConfigurationData of a $configurationType zaaktype configuration") {
            given("a previous configuration with the settings that both configuration types have in common") {
                val previousToegekendUuid = UUID.randomUUID()
                val previousNietOntvankelijkUuid = UUID.randomUUID()
                val newToegekendUuid = UUID.randomUUID()
                val newNietOntvankelijkUuid = UUID.randomUUID()

                val newZaaktype = createZaakType(
                    resultTypes = listOf(
                        resultaattypeUri(newToegekendUuid),
                        resultaattypeUri(newNietOntvankelijkUuid)
                    )
                )

                every { ztcClientService.readResultaattype(resultaattypeUri(newToegekendUuid)) } returns
                    createResultaatType(url = resultaattypeUri(newToegekendUuid), omschrijving = "Toegekend")
                every { ztcClientService.readResultaattype(resultaattypeUri(newNietOntvankelijkUuid)) } returns
                    createResultaatType(
                        url = resultaattypeUri(newNietOntvankelijkUuid),
                        omschrijving = "Niet ontvankelijk"
                    )
                every { ztcClientService.readResultaattype(previousToegekendUuid) } returns
                    createResultaatType(url = resultaattypeUri(previousToegekendUuid), omschrijving = "Toegekend")
                every { ztcClientService.readResultaattype(previousNietOntvankelijkUuid) } returns
                    createResultaatType(
                        url = resultaattypeUri(previousNietOntvankelijkUuid),
                        omschrijving = "Niet ontvankelijk"
                    )

                val previousZaaktypeConfiguration = createZaaktypeConfiguration(previousNietOntvankelijkUuid).apply {
                    groepID = "fakeGroupId"
                    defaultBehandelaarId = "fakeDefaultBehandelaarId"
                    productaanvraagtype = "fakeProductaanvraagtype"
                    smartDocumentsEnabled = true
                    zaaktypeBetrokkeneParameters = createBetrokkeneKoppelingen(brpKoppelen = false)
                    zaaktypeBrpParameters = createZaaktypeBrpParameters(raadpleegWaarde = "fakeRaadpleegWaarde")
                    setZaakbeeindigParameters(
                        listOf(
                            completionParameter(
                                this,
                                zaakbeeindigReden(id = 2L, naam = "Zaak is toegekend"),
                                previousToegekendUuid
                            )
                        )
                    )
                }
                val newZaaktypeConfiguration = createZaaktypeConfiguration(UUID.randomUUID())

                `when`("the shared configuration data is copied onto the new configuration") {
                    zaaktypeHelperService.copyConfigurationData(
                        previousZaaktypeConfiguration,
                        newZaaktypeConfiguration,
                        newZaaktype
                    )

                    then("the zaakbeeindig gegevens are matched onto the resultaattypen of the new zaaktype") {
                        newZaaktypeConfiguration.nietOntvankelijkResultaattype shouldBe newNietOntvankelijkUuid
                        newZaaktypeConfiguration.getZaakbeeindigParameters().map {
                            it.resultaattype
                        } shouldBe listOf(newToegekendUuid)
                    }

                    and("the group, behandelaar, productaanvraagtype and SmartDocuments settings are copied") {
                        newZaaktypeConfiguration.groepID shouldBe "fakeGroupId"
                        newZaaktypeConfiguration.defaultBehandelaarId shouldBe "fakeDefaultBehandelaarId"
                        newZaaktypeConfiguration.productaanvraagtype shouldBe "fakeProductaanvraagtype"
                        newZaaktypeConfiguration.smartDocumentsEnabled shouldBe true
                    }

                    and("the betrokkene koppelingen and BRP doelbindingen are copied onto the new configuration") {
                        with(newZaaktypeConfiguration.getBetrokkeneParameters()) {
                            brpKoppelen shouldBe false
                            kvkKoppelen shouldBe true
                            zaaktypeConfiguration shouldBe newZaaktypeConfiguration
                        }
                        with(newZaaktypeConfiguration.getBrpParameters()) {
                            raadpleegWaarde shouldBe "fakeRaadpleegWaarde"
                            zaaktypeConfiguration shouldBe newZaaktypeConfiguration
                        }
                    }

                    and("the new configuration is dated at the moment it was copied") {
                        newZaaktypeConfiguration.creatiedatum!! shouldNotBeBefore
                            previousZaaktypeConfiguration.creatiedatum!!
                    }
                }
            }
        }
    }

    context("copyConfigurationData of a CMMN zaaktype configuration") {
        given("a previous configuration whose human task is coupled to the ADVIES reference table") {
            val newZaaktype = createZaakType(resultTypes = emptyList())
            val adviesReferenceTable = createReferenceTable(
                code = "ADVIES",
                name = "Advies",
                isSystemReferenceTable = true,
                values = mutableListOf(
                    createReferenceTableValue(id = 1L, name = "Positief"),
                    createReferenceTableValue(id = 2L, name = "Negatief")
                )
            )
            val previousZaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                caseDefinitionId = "fakeCaseDefinitionId"
            ).apply {
                nietOntvankelijkResultaattype = null
                setHumanTaskParametersCollection(
                    setOf(
                        createHumanTaskParameters(
                            zaaktypeCmmnConfiguration = this,
                            formulierDefinitieID = "ADVIES",
                            planItemDefinitionID = "ADVIES",
                            referenceTables = listOf(
                                createHumanTaskReferentieTabel(
                                    referenceTable = adviesReferenceTable,
                                    field = "ADVIES"
                                )
                            )
                        )
                    )
                )
            }
            val previousHumanTaskParameters =
                previousZaaktypeConfiguration.getHumanTaskParametersCollection().single()
            val previousReferentieTabel = previousHumanTaskParameters.getReferentieTabellen().single()
            val newZaaktypeConfiguration = createZaaktypeCmmnConfiguration()

            `when`("the configuration data is copied onto the configuration of the new zaaktype version") {
                zaaktypeHelperService.copyConfigurationData(
                    previousZaaktypeConfiguration,
                    newZaaktypeConfiguration,
                    newZaaktype
                )

                then("the new configuration is coupled to the same reference table") {
                    with(newZaaktypeConfiguration.getHumanTaskParametersCollection().single()) {
                        planItemDefinitionID shouldBe "ADVIES"
                        with(getReferentieTabellen().single()) {
                            veld shouldBe "ADVIES"
                            tabel shouldBeSameInstanceAs adviesReferenceTable
                        }
                    }
                }

                and("the previous configuration keeps its own coupling, so its zaken keep their advies options") {
                    previousHumanTaskParameters.getReferentieTabellen()
                        .single() shouldBeSameInstanceAs previousReferentieTabel
                    previousReferentieTabel.humantask shouldBeSameInstanceAs previousHumanTaskParameters
                }

                and("the coupling of the new configuration is a new, unsaved record of its own human task") {
                    val newHumanTaskParameters = newZaaktypeConfiguration.getHumanTaskParametersCollection().single()
                    with(newHumanTaskParameters.getReferentieTabellen().single()) {
                        this shouldNotBeSameInstanceAs previousReferentieTabel
                        id.shouldBeNull()
                        humantask shouldBeSameInstanceAs newHumanTaskParameters
                    }
                }
            }
        }
    }

    context("copyConfigurationData of a BPMN zaaktype configuration") {
        given("a previous configuration with a BPMN process definition key") {
            val newZaaktype = createZaakType(resultTypes = emptyList())
            val previousZaaktypeConfiguration = createZaaktypeBpmnConfiguration(
                bpmnProcessDefinitionKey = "fakeBpmnProcessDefinitionKey"
            ).apply { nietOntvankelijkResultaattype = null }
            val newZaaktypeConfiguration = createZaaktypeBpmnConfiguration()

            `when`("the configuration data is copied onto the configuration of the new zaaktype version") {
                zaaktypeHelperService.copyConfigurationData(
                    previousZaaktypeConfiguration,
                    newZaaktypeConfiguration,
                    newZaaktype
                )

                then("the BPMN process definition key is carried over") {
                    newZaaktypeConfiguration.bpmnProcessDefinitionKey shouldBe "fakeBpmnProcessDefinitionKey"
                }
            }
        }
    }

    context("copyConfigurationData onto a configuration of a different type") {
        given("a CMMN previous configuration and a BPMN new configuration") {
            val newZaaktype = createZaakType(resultTypes = emptyList())
            val previousZaaktypeConfiguration =
                createZaaktypeCmmnConfiguration().apply { nietOntvankelijkResultaattype = null }
            val newZaaktypeConfiguration = createZaaktypeBpmnConfiguration()

            `when`("the configuration data is copied") {
                val illegalArgumentException = shouldThrow<IllegalArgumentException> {
                    zaaktypeHelperService.copyConfigurationData(
                        previousZaaktypeConfiguration,
                        newZaaktypeConfiguration,
                        newZaaktype
                    )
                }

                then("the copy is refused") {
                    illegalArgumentException.message shouldBe
                        "Cannot copy a CMMN zaaktype configuration onto a BPMN zaaktype configuration"
                }
            }
        }
    }
})
