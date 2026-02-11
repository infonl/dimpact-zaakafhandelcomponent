/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.healthcheck

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createBesluitType
import nl.info.client.zgw.ztc.model.createBrondatumArchiefprocedure
import nl.info.client.zgw.ztc.model.createInformatieObjectType
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.zac.admin.ReferenceTableService
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeCmmnConfigurationBeheerService
import nl.info.zac.admin.model.ReferenceTable.SystemReferenceTable
import nl.info.zac.admin.model.createReferenceTable
import nl.info.zac.admin.model.createReferenceTableValue
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import java.net.URI
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID

class HealthCheckServiceTest : BehaviorSpec({

    @Suppress("UNCHECKED_CAST")
    Given("A zaaktype with CMMN configuration, two initiator role types and invalid BRP parameters") {
        val branchName = Optional.of("dev") as Optional<String?>
        val commitHash = Optional.of("hash") as Optional<String?>
        val versionNumber = Optional.of("0.0.0") as Optional<String?>
        val zaaktypeUuid = UUID.randomUUID()
        val zaaktypeUri = URI("https://example.com/zaaktype/$zaaktypeUuid")

        val referenceTableService = mockk<ReferenceTableService>()
        val zaaktypeCmmnConfigurationBeheerService = mockk<ZaaktypeCmmnConfigurationBeheerService>()
        val zaaktypeBpmnConfigurationBeheerService = mockk<ZaaktypeBpmnConfigurationBeheerService>()
        val ztcClientService = mockk<ZtcClientService>()

        val healthCheckService = HealthCheckService(
            branchName,
            commitHash,
            versionNumber,
            referenceTableService,
            zaaktypeCmmnConfigurationBeheerService,
            zaaktypeBpmnConfigurationBeheerService,
            ztcClientService
        )

        every { ztcClientService.resetCacheTimeToNow() } returns ZonedDateTime.now()
        every { ztcClientService.readZaaktype(zaaktypeUri) } returns createZaakType(zaaktypeUri)
        every {
            zaaktypeCmmnConfigurationBeheerService.readZaaktypeCmmnConfiguration(zaaktypeUuid)
        } returns createZaaktypeCmmnConfiguration(groupId = "fakeGroupId")
        every {
            ztcClientService.readStatustypen(zaaktypeUri)
        } returns listOf(
            createStatusType(volgnummer = 1, omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_INTAKE),
            createStatusType(volgnummer = 2, omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_IN_BEHANDELING),
            createStatusType(volgnummer = 3, omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_HEROPEND),
            createStatusType(
                volgnummer = 4,
                omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE
            ),
            createStatusType(volgnummer = 5, omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_AFGEROND),
        )
        every {
            ztcClientService.readResultaattypen(zaaktypeUri)
        } returns listOf(createResultaatType(brondatumArchiefprocedure = createBrondatumArchiefprocedure()))
        every {
            ztcClientService.readBesluittypen(zaaktypeUri)
        } returns listOf(createBesluitType())
        every {
            ztcClientService.listRoltypen(zaaktypeUri)
        } returns listOf(
            createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR),
            createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.ZAAKCOORDINATOR),
            // Two initiator roles
            createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR),
            createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
        )
        every {
            ztcClientService.readInformatieobjecttypen(zaaktypeUri)
        } returns listOf(
            createInformatieObjectType(omschrijving = ConfigurationService.INFORMATIEOBJECTTYPE_OMSCHRIJVING_EMAIL)
        )
        every {
            referenceTableService.readReferenceTable(SystemReferenceTable.BRP_DOELBINDING_ZOEK_WAARDE.name)
        } returns createReferenceTable()
        every {
            referenceTableService.readReferenceTable(SystemReferenceTable.BRP_DOELBINDING_RAADPLEEG_WAARDE.name)
        } returns createReferenceTable()
        every {
            referenceTableService.readReferenceTable(SystemReferenceTable.BRP_VERWERKINGSREGISTER_WAARDE.name)
        } returns createReferenceTable(
            values = mutableListOf(
                createReferenceTableValue(name = "Algemeen"),
                createReferenceTableValue(name = "Bíj́na")
            )
        )

        When("controleerZaaktype is called") {
            val zaaktypeInrichtingscheck = healthCheckService.controleerZaaktype(zaaktypeUri)

            Then("The zaaktypeInrichtingscheck is invalid") {
                zaaktypeInrichtingscheck.isValide shouldBe false
            }

            Then("The reason for the invalidity is reported") {
                with(zaaktypeInrichtingscheck) {
                    isStatustypeIntakeAanwezig shouldBe true
                    isStatustypeInBehandelingAanwezig shouldBe true
                    isStatustypeHeropendAanwezig shouldBe true
                    isStatustypeAanvullendeInformatieVereist shouldBe true
                    isStatustypeAfgerondAanwezig shouldBe true
                    isStatustypeAfgerondLaatsteVolgnummer shouldBe true
                    isResultaattypeAanwezig shouldBe true
                    aantalInitiatorroltypen shouldBe 2
                    aantalBehandelaarroltypen shouldBe 1
                    isRolOverigeAanwezig shouldBe true
                    isInformatieobjecttypeEmailAanwezig shouldBe true
                    isBesluittypeAanwezig shouldBe true
                    resultaattypesMetVerplichtBesluit shouldBe arrayOf("fakeOmschrijving")
                    isZaakafhandelParametersValide shouldBe true
                    isBrpInstellingenCorrect shouldBe false
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    Given("A valid zaaktype with BPMN configuration") {
        val branchName = Optional.of("dev") as Optional<String?>
        val commitHash = Optional.of("hash") as Optional<String?>
        val versionNumber = Optional.of("0.0.0") as Optional<String?>
        val zaaktypeUuid = UUID.randomUUID()
        val zaaktypeUri = URI("https://example.com/zaaktype/$zaaktypeUuid")

        val referenceTableService = mockk<ReferenceTableService>()
        val zaaktypeCmmnConfigurationBeheerService = mockk<ZaaktypeCmmnConfigurationBeheerService>()
        val zaaktypeBpmnConfigurationBeheerService = mockk<ZaaktypeBpmnConfigurationBeheerService>()
        val ztcClientService = mockk<ZtcClientService>()

        val healthCheckService = HealthCheckService(
            branchName,
            commitHash,
            versionNumber,
            referenceTableService,
            zaaktypeCmmnConfigurationBeheerService,
            zaaktypeBpmnConfigurationBeheerService,
            ztcClientService
        )

        every { ztcClientService.resetCacheTimeToNow() } returns ZonedDateTime.now()
        every { ztcClientService.readZaaktype(zaaktypeUri) } returns createZaakType(zaaktypeUri)
        every {
            zaaktypeCmmnConfigurationBeheerService.readZaaktypeCmmnConfiguration(zaaktypeUuid)
        } returns null
        every {
            zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeUuid)
        } returns createZaaktypeBpmnConfiguration()
        every {
            ztcClientService.readStatustypen(zaaktypeUri)
        } returns listOf(
            createStatusType(volgnummer = 1, omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_INTAKE),
            createStatusType(volgnummer = 2, omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_IN_BEHANDELING),
            createStatusType(volgnummer = 3, omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_HEROPEND),
            createStatusType(
                volgnummer = 4,
                omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE
            ),
            createStatusType(volgnummer = 5, omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_AFGEROND),
        )
        every {
            ztcClientService.readResultaattypen(zaaktypeUri)
        } returns listOf(createResultaatType(brondatumArchiefprocedure = createBrondatumArchiefprocedure()))
        every {
            ztcClientService.readBesluittypen(zaaktypeUri)
        } returns listOf(createBesluitType())
        every {
            ztcClientService.listRoltypen(zaaktypeUri)
        } returns listOf(
            createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR),
            createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.ZAAKCOORDINATOR),
            createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR),
        )
        every {
            ztcClientService.readInformatieobjecttypen(zaaktypeUri)
        } returns listOf(
            createInformatieObjectType(omschrijving = ConfigurationService.INFORMATIEOBJECTTYPE_OMSCHRIJVING_EMAIL)
        )
        every {
            referenceTableService.readReferenceTable(SystemReferenceTable.BRP_DOELBINDING_ZOEK_WAARDE.name)
        } returns createReferenceTable()
        every {
            referenceTableService.readReferenceTable(SystemReferenceTable.BRP_DOELBINDING_RAADPLEEG_WAARDE.name)
        } returns createReferenceTable()
        every {
            referenceTableService.readReferenceTable(SystemReferenceTable.BRP_VERWERKINGSREGISTER_WAARDE.name)
        } returns createReferenceTable(
            values = mutableListOf(
                createReferenceTableValue(name = "Algemeen"),
            )
        )

        When("controleerZaaktype is called") {
            val zaaktypeInrichtingscheck = healthCheckService.controleerZaaktype(zaaktypeUri)

            Then("The zaaktypeInrichtingscheck is valid") {
                zaaktypeInrichtingscheck.isValide shouldBe true
            }

            Then("All flags indicate a valid zaaktype") {
                with(zaaktypeInrichtingscheck) {
                    isStatustypeIntakeAanwezig shouldBe true
                    isStatustypeInBehandelingAanwezig shouldBe true
                    isStatustypeHeropendAanwezig shouldBe true
                    isStatustypeAanvullendeInformatieVereist shouldBe true
                    isStatustypeAfgerondAanwezig shouldBe true
                    isStatustypeAfgerondLaatsteVolgnummer shouldBe true
                    isResultaattypeAanwezig shouldBe true
                    aantalInitiatorroltypen shouldBe 1
                    aantalBehandelaarroltypen shouldBe 1
                    isRolOverigeAanwezig shouldBe true
                    isInformatieobjecttypeEmailAanwezig shouldBe true
                    isBesluittypeAanwezig shouldBe true
                    resultaattypesMetVerplichtBesluit shouldBe arrayOf("fakeOmschrijving")
                    isZaakafhandelParametersValide shouldBe true
                    isBrpInstellingenCorrect shouldBe true
                }
            }
        }
    }
})
