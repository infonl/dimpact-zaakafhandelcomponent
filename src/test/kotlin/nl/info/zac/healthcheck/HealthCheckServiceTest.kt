/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.healthcheck

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.admin.ZaakafhandelParameterService
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
import nl.info.zac.admin.model.ReferenceTable.Systeem
import nl.info.zac.admin.model.createReferenceTable
import nl.info.zac.admin.model.createZaakafhandelParameters
import nl.info.zac.configuratie.ConfiguratieService
import java.net.URI
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID

class HealthCheckServiceTest : BehaviorSpec({

    @Suppress("UNCHECKED_CAST")
    Given("A zaaktype with two initiator roles") {
        val branchName = Optional.of("dev") as Optional<String?>
        val commitHash = Optional.of("hash") as Optional<String?>
        val versionNumber = Optional.of("0.0.0") as Optional<String?>
        val zaaktypeUuid = UUID.randomUUID()
        val zaaktypeUri = URI("https://example.com/zaaktype/$zaaktypeUuid")

        val referenceTableService = mockk<ReferenceTableService>()
        val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
        val ztcClientService = mockk<ZtcClientService>()

        val healthCheckService = HealthCheckService(
            branchName,
            commitHash,
            versionNumber,
            referenceTableService,
            zaakafhandelParameterService,
            ztcClientService
        )

        every { ztcClientService.resetCacheTimeToNow() } returns ZonedDateTime.now()
        every { ztcClientService.readZaaktype(zaaktypeUri) } returns createZaakType(zaaktypeUri)
        every {
            zaakafhandelParameterService.readZaakafhandelParameters(zaaktypeUuid)
        } returns createZaakafhandelParameters(groupId = "fakeGroupId")
        every {
            ztcClientService.readStatustypen(zaaktypeUri)
        } returns listOf(
            createStatusType(volgnummer = 1, omschrijving = ConfiguratieService.STATUSTYPE_OMSCHRIJVING_INTAKE),
            createStatusType(volgnummer = 2, omschrijving = ConfiguratieService.STATUSTYPE_OMSCHRIJVING_IN_BEHANDELING),
            createStatusType(volgnummer = 3, omschrijving = ConfiguratieService.STATUSTYPE_OMSCHRIJVING_HEROPEND),
            createStatusType(
                volgnummer = 4,
                omschrijving = ConfiguratieService.STATUSTYPE_OMSCHRIJVING_AANVULLENDE_INFORMATIE
            ),
            createStatusType(volgnummer = 5, omschrijving = ConfiguratieService.STATUSTYPE_OMSCHRIJVING_AFGEROND),
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
            createInformatieObjectType(omschrijving = ConfiguratieService.INFORMATIEOBJECTTYPE_OMSCHRIJVING_EMAIL)
        )
        every {
            referenceTableService.readReferenceTable(Systeem.BRP_DOELBINDING_ZOEK_WAARDE.name)
        } returns createReferenceTable()
        every {
            referenceTableService.readReferenceTable(Systeem.BRP_DOELBINDING_RAADPLEEG_WAARDE.name)
        } returns createReferenceTable()

        When("controleerZaaktype is called") {
            val zaaktypeInrichtingscheck = healthCheckService.controleerZaaktype(zaaktypeUri)

            Then("The zaaktypeInrichtingscheck is invalid") {
                zaaktypeInrichtingscheck.isValide shouldBe false
            }

            And("the number of initiator roles is detected") {
                zaaktypeInrichtingscheck.aantalInitiatorrollen shouldBe 2
            }
        }
    }
})
