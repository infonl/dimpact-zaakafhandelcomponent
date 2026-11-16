/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.admin.model.RestProcessDefinitionOverzicht
import nl.info.zac.app.admin.model.RestZaaktypeBeheerOverzicht
import nl.info.zac.app.admin.model.RestZaaktypeConfigurationOverzicht
import nl.info.zac.app.admin.model.createRestProcessDefinition
import nl.info.zac.smartdocuments.SmartDocumentsService
import nl.info.zac.zaak.ZaakspecifiekeAutorisatieService
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class RestZaaktypeConfigurationConverterTest : BehaviorSpec({
    val processDefinitionConverter = mockk<RestProcessDefinitionConverter>()
    val ztcClientService = mockk<ZtcClientService>()
    val smartDocumentsService = mockk<SmartDocumentsService>()
    val zaakspecifiekeAutorisatieService = mockk<ZaakspecifiekeAutorisatieService>()
    val restZaaktypeConfigurationConverter = RestZaaktypeConfigurationConverter(
        processDefinitionConverter = processDefinitionConverter,
        zaakbeeindigParameterConverter = mockk(),
        humanTaskParametersConverter = mockk(),
        ztcClientService = ztcClientService,
        resultaattypeReferenceService = mockk(),
        zaaktypeConfigurationBeheerService = mockk(),
        smartDocumentsService = smartDocumentsService,
        zaakspecifiekeAutorisatieService = zaakspecifiekeAutorisatieService
    )
    val restCaseDefinition = createRestProcessDefinition(key = "fakeCaseKey", naam = "fakeCaseName")

    afterEach {
        checkUnnecessaryStub()
    }

    context("Converting a zaaktype configuration to a row of the configuration list") {
        given("a zaaktype with a saved, valid CMMN configuration with a groep, SmartDocuments and BRP doelbindingen") {
            val zaaktype = createZaakType(
                identification = "fakeIdentificatie",
                omschrijving = "fakeOmschrijving",
                doel = "fakeDoel",
                beginGeldigheid = LocalDate.now().minusDays(1),
                eindeGeldigheid = LocalDate.now().plusDays(1)
            )
            val creatiedatum = ZonedDateTime.now().minusDays(2)
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaaktype.url.extractUuid(),
                creationDate = creatiedatum,
                groupId = "fakeGroepId",
                caseDefinitionId = "fakeCaseKey",
                smartDocumentsEnabled = true
            )
            every {
                processDefinitionConverter.toRestProcessDefinition(zaaktypeCmmnConfiguration.processBinding!!, false)
            } returns restCaseDefinition

            `when`("it is converted to a list row") {
                val restZaaktypeConfigurationOverzicht =
                    restZaaktypeConfigurationConverter.toRestZaaktypeConfigurationOverzicht(
                        zaaktypeConfiguration = zaaktypeCmmnConfiguration,
                        zaaktype = zaaktype
                    )

                then("the row has the zaaktype fields, the key and naam of its case definition, its creatiedatum and is valid") {
                    restZaaktypeConfigurationOverzicht shouldBe RestZaaktypeConfigurationOverzicht(
                        zaaktype = RestZaaktypeBeheerOverzicht(
                            uuid = zaaktype.url.extractUuid(),
                            identificatie = "fakeIdentificatie",
                            omschrijving = "fakeOmschrijving",
                            doel = "fakeDoel",
                            isNuGeldig = true,
                            beginGeldigheid = zaaktype.beginGeldigheid,
                            eindeGeldigheid = zaaktype.eindeGeldigheid
                        ),
                        processDefinition = RestProcessDefinitionOverzicht(key = "fakeCaseKey", naam = "fakeCaseName"),
                        creatiedatum = creatiedatum,
                        isValide = true
                    )
                }
                and("the zaaktype is not read again, and neither the zaakspecifieke autorisatie nor SmartDocuments is looked up") {
                    verify(exactly = 0) {
                        ztcClientService.readZaaktype(any<UUID>())
                        zaakspecifiekeAutorisatieService.isZaakspecifiekAutoriseerbaar(any())
                        smartDocumentsService.isEnabled()
                    }
                }
            }
        }

        given("a zaaktype with a saved BPMN configuration") {
            val zaaktype = createZaakType()
            val zaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration(
                zaaktypeUUID = zaaktype.url.extractUuid(),
                groupId = "fakeGroepId",
                bpmnProcessDefinitionKey = "fakeBpmnProcessDefinitionKey"
            )
            every {
                processDefinitionConverter.toRestProcessDefinition(zaaktypeBpmnConfiguration.processBinding!!, false)
            } returns createRestProcessDefinition(
                processEngine = ProcessEngine.BPMN,
                key = "fakeBpmnProcessDefinitionKey",
                naam = "fakeBpmnProcessDefinitionName"
            )

            `when`("it is converted to a list row") {
                val restZaaktypeConfigurationOverzicht =
                    restZaaktypeConfigurationConverter.toRestZaaktypeConfigurationOverzicht(
                        zaaktypeConfiguration = zaaktypeBpmnConfiguration,
                        zaaktype = zaaktype
                    )

                then("the row has the key and naam of the BPMN process definition and is valid") {
                    restZaaktypeConfigurationOverzicht.processDefinition shouldBe RestProcessDefinitionOverzicht(
                        key = "fakeBpmnProcessDefinitionKey",
                        naam = "fakeBpmnProcessDefinitionName"
                    )
                    restZaaktypeConfigurationOverzicht.isValide shouldBe true
                }
            }
        }

        given("a zaaktype without a saved configuration") {
            val zaaktype = createZaakType()
            val unsavedZaaktypeConfiguration = ZaaktypeConfiguration().apply {
                zaaktypeUuid = zaaktype.url.extractUuid()
            }

            `when`("it is converted to a list row") {
                val restZaaktypeConfigurationOverzicht =
                    restZaaktypeConfigurationConverter.toRestZaaktypeConfigurationOverzicht(
                        zaaktypeConfiguration = unsavedZaaktypeConfiguration,
                        zaaktype = zaaktype
                    )

                then("the row has no process definition and no creatiedatum, and is not valid") {
                    with(restZaaktypeConfigurationOverzicht) {
                        this.zaaktype.uuid shouldBe zaaktype.url.extractUuid()
                        processDefinition shouldBe null
                        creatiedatum shouldBe null
                        isValide shouldBe false
                    }
                }
                and("no process definition is read") {
                    verify(exactly = 0) { processDefinitionConverter.toRestProcessDefinition(any(), any()) }
                }
            }
        }

        given("a configuration without a process binding, which the health check reports as not valid") {
            val zaaktype = createZaakType()
            val zaaktypeConfiguration = ZaaktypeConfiguration().apply {
                zaaktypeUuid = zaaktype.url.extractUuid()
                groepID = "fakeGroepId"
                creatiedatum = ZonedDateTime.now()
            }

            `when`("it is converted to a list row") {
                val restZaaktypeConfigurationOverzicht =
                    restZaaktypeConfigurationConverter.toRestZaaktypeConfigurationOverzicht(
                        zaaktypeConfiguration = zaaktypeConfiguration,
                        zaaktype = zaaktype
                    )

                then("the row is not valid, as the configuration is not valid for zaak creation") {
                    zaaktypeConfiguration.isValidForZaakCreation() shouldBe false
                    restZaaktypeConfigurationOverzicht.isValide shouldBe false
                }
            }
        }
    }
})
