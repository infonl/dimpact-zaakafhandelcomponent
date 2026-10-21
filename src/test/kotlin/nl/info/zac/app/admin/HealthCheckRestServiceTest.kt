/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.healthcheck.createBuildInformation
import nl.info.zac.healthcheck.createZaaktypeInrichtingscheck
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID

class HealthCheckRestServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val configurationService = mockk<ConfigurationService>()
    val healthCheckService = mockk<HealthCheckService>()
    val policyService = mockk<PolicyService>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val healthCheckRestService = HealthCheckRestService(
        ztcClientService = ztcClientService,
        configurationService = configurationService,
        healthCheckService = healthCheckService,
        policyService = policyService,
        zaaktypeConfigurationService = zaaktypeConfigurationService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("A valid and an invalid zaaktype") {
        val catalogusURI = URI("https://example.com/catalogs/${UUID.randomUUID()}")
        val zaaktypen = listOf(createZaakType(), createZaakType())
        val zaaktypeInrichtingschecks = listOf(
            // valid zaaktype
            createZaaktypeInrichtingscheck(zaaktype = zaaktypen[0]),
            // invalid zaaktype
            createZaaktypeInrichtingscheck(zaaktype = zaaktypen[1], statustypeIntakeAanwezig = false)
        )
        every { policyService.readOverigeRechten().canBeheren } returns true
        every { configurationService.readDefaultCatalogusURI() } returns catalogusURI
        every { zaaktypeConfigurationService.findConfiguration(any()) } returns null
        every { ztcClientService.listZaaktypen(catalogusURI) } returns zaaktypen
        zaaktypen.forEachIndexed { index, zaaktype ->
            every { healthCheckService.controleerZaaktype(zaaktypen[index].url) } returns zaaktypeInrichtingschecks[index]
        }

        `when`("listZaaktypeInrichtingschecks is called") {
            val result = healthCheckRestService.listZaaktypeInrichtingschecks()

            then(
                "it should return a list of RestZaaktypeInrichtingscheck objects where the first is valid and the second is invalid"
            ) {
                result.size shouldBe 2
                with(result[0]) {
                    zaaktype.identificatie shouldBe zaaktypen[0].identificatie
                    zaaktype.omschrijving shouldBe zaaktypen[0].omschrijving
                    zaaktype.uuid shouldBe zaaktypen[0].url.extractUuid()
                    isValide shouldBe true
                }
                with(result[1]) {
                    zaaktype.identificatie shouldBe zaaktypen[1].identificatie
                    zaaktype.omschrijving shouldBe zaaktypen[1].omschrijving
                    zaaktype.uuid shouldBe zaaktypen[1].url.extractUuid()
                    isValide shouldBe false
                }
            }
        }
    }

    given("A zaaktype for which only the zaakspecifieke autorisatie eigenschap is configured") {
        val catalogusURI = URI("https://example.com/catalogs/${UUID.randomUUID()}")
        val zaaktype = createZaakType()
        every { policyService.readOverigeRechten().canBeheren } returns true
        every { configurationService.readDefaultCatalogusURI() } returns catalogusURI
        every { zaaktypeConfigurationService.findConfiguration(any()) } returns null
        every { ztcClientService.listZaaktypen(catalogusURI) } returns listOf(zaaktype)
        every { healthCheckService.controleerZaaktype(zaaktype.url) } returns createZaaktypeInrichtingscheck(
            zaaktype = zaaktype,
            zaakspecifiekeAutorisatieEigenschapAanwezig = true,
            zaakspecifiekeAutorisatieRoltypeAanwezig = false
        )

        `when`("listZaaktypeInrichtingschecks is called") {
            val result = healthCheckRestService.listZaaktypeInrichtingschecks()

            then("both zaakspecifieke autorisatie flags are mapped to the REST model") {
                with(result.single()) {
                    isZaakspecifiekeAutorisatieEigenschapAanwezig shouldBe true
                    isZaakspecifiekeAutorisatieRoltypeAanwezig shouldBe false
                }
            }
            and("the warning is mapped to the REST model") {
                result.single().hasWaarschuwingen shouldBe true
            }
            and("the zaaktype is still reported as valid") {
                result.single().isValide shouldBe true
            }
        }
    }

    given("A zaaktype bound to BPMN and a zaaktype bound to CMMN") {
        val catalogusURI = URI("https://example.com/catalogs/${UUID.randomUUID()}")
        val bpmnZaaktype = createZaakType()
        val cmmnZaaktype = createZaakType()
        every { policyService.readOverigeRechten().canBeheren } returns true
        every { configurationService.readDefaultCatalogusURI() } returns catalogusURI
        every {
            zaaktypeConfigurationService.findConfiguration(bpmnZaaktype.url.extractUuid())
        } returns createZaaktypeBpmnConfiguration(zaaktypeUUID = bpmnZaaktype.url.extractUuid())
        every {
            zaaktypeConfigurationService.findConfiguration(cmmnZaaktype.url.extractUuid())
        } returns createZaaktypeCmmnConfiguration(zaaktypeUUID = cmmnZaaktype.url.extractUuid())
        every { ztcClientService.listZaaktypen(catalogusURI) } returns listOf(bpmnZaaktype, cmmnZaaktype)
        every { healthCheckService.controleerZaaktype(cmmnZaaktype.url) } returns createZaaktypeInrichtingscheck(
            zaaktype = cmmnZaaktype
        )

        `when`("listZaaktypeInrichtingschecks is called") {
            val result = healthCheckRestService.listZaaktypeInrichtingschecks()

            then("only the CMMN zaaktype is returned") {
                result.single().zaaktype.uuid shouldBe cmmnZaaktype.url.extractUuid()
            }
            and("the BPMN zaaktype is not checked") {
                verify(exactly = 0) { healthCheckService.controleerZaaktype(bpmnZaaktype.url) }
            }
        }
    }

    given("No zaaktypes are available") {
        val catalogusURI = URI("https://example.com/catalogs/${UUID.randomUUID()}")
        every { policyService.readOverigeRechten().canBeheren } returns true
        every { configurationService.readDefaultCatalogusURI() } returns catalogusURI
        every { ztcClientService.listZaaktypen(catalogusURI) } returns emptyList()

        `when`("listZaaktypeInrichtingschecks is called") {
            val result = healthCheckRestService.listZaaktypeInrichtingschecks()

            then("it should return an empty list") {
                result shouldBe emptyList()
            }
        }
    }

    given("Communicatiekanaal EFormulier exists") {
        every { policyService.readOverigeRechten().canBeheren } returns true
        every { healthCheckService.bestaatCommunicatiekanaalEformulier() } returns true

        `when`("the check for the existence of the EFormulier communication channel is performed") {
            val isCommunicatiekanaalEformulierAanwezig = healthCheckRestService.readBestaatCommunicatiekanaalEformulier()

            then("it should return true") {
                isCommunicatiekanaalEformulierAanwezig shouldBe true
            }
        }
    }

    given("build information is available") {
        val now = ZonedDateTime.now()
        val buildInformation = createBuildInformation(
            commit = "fakeCommit",
            buildId = "fakeCommit",
            buildDatumTijd = now,
            versienummer = "fakeVersionNumber"
        )
        every { healthCheckService.readBuildInformatie() } returns buildInformation

        `when`("readBuildInformatie is called") {
            val result = healthCheckRestService.readBuildInformatie()

            then("it should return the correct build information object") {
                result.commit shouldBe "fakeCommit"
                result.buildId shouldBe "fakeCommit"
                result.buildDatumTijd shouldBe now
                result.versienummer shouldBe "fakeVersionNumber"
            }
        }
    }

    given("The user does not have the required 'beheren' permissions") {
        every { policyService.readOverigeRechten().canBeheren } returns false

        `when`("listZaaktypeInrichtingschecks is called") {
            val exception = shouldThrow<PolicyException> {
                healthCheckRestService.listZaaktypeInrichtingschecks()
            }

            then("it should throw a PolicyException") {
                exception shouldNotBe null
            }
        }
    }
})
