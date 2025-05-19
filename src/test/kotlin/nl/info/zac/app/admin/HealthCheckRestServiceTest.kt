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
import net.atos.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.healthcheck.createBuildInformation
import nl.info.zac.healthcheck.createZaaktypeInrichtingscheck
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID

class HealthCheckRestServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val configuratieService = mockk<ConfiguratieService>()
    val healthCheckService = mockk<HealthCheckService>()
    val policyService = mockk<PolicyService>()
    val healthCheckRestService = HealthCheckRestService(
        ztcClientService = ztcClientService,
        configuratieService = configuratieService,
        healthCheckService = healthCheckService,
        policyService = policyService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A valid and an invalid zaaktype") {
        val catalogusURI = URI("https://example.com/catalogs/${UUID.randomUUID()}")
        val zaaktypen = listOf(createZaakType(), createZaakType())
        val zaaktypeInrichtingschecks = listOf(
            // valid zaaktype
            createZaaktypeInrichtingscheck(zaaktype = zaaktypen[0]),
            // invalid zaaktype
            createZaaktypeInrichtingscheck(zaaktype = zaaktypen[1], statustypeIntakeAanwezig = false)
        )
        every { policyService.readOverigeRechten().beheren } returns true
        every { configuratieService.readDefaultCatalogusURI() } returns catalogusURI
        every { ztcClientService.listZaaktypen(catalogusURI) } returns zaaktypen
        zaaktypen.forEachIndexed { index, zaaktype ->
            every { healthCheckService.controleerZaaktype(zaaktypen[index].url) } returns zaaktypeInrichtingschecks[index]
        }

        When("listZaaktypeInrichtingschecks is called") {
            val result = healthCheckRestService.listZaaktypeInrichtingschecks()

            Then(
                "it should return a list of RESTZaaktypeInrichtingscheck objects where the first is valid and the second is invalid"
            ) {
                result.size shouldBe 2
                with(result[0]) {
                    zaaktype.identificatie shouldBe zaaktypen[0].identificatie
                    zaaktype.omschrijving shouldBe zaaktypen[0].omschrijving
                    zaaktype.uuid shouldBe zaaktypen[0].url.extractUuid()
                    valide = true
                }
                with(result[1]) {
                    zaaktype.identificatie shouldBe zaaktypen[1].identificatie
                    zaaktype.omschrijving shouldBe zaaktypen[1].omschrijving
                    zaaktype.uuid shouldBe zaaktypen[1].url.extractUuid()
                    valide = false
                }
            }
        }
    }

    Given("No zaaktypes are available") {
        val catalogusURI = URI("https://example.com/catalogs/${UUID.randomUUID()}")
        every { policyService.readOverigeRechten().beheren } returns true
        every { configuratieService.readDefaultCatalogusURI() } returns catalogusURI
        every { ztcClientService.listZaaktypen(catalogusURI) } returns emptyList()

        When("listZaaktypeInrichtingschecks is called") {
            val result = healthCheckRestService.listZaaktypeInrichtingschecks()

            Then("it should return an empty list") {
                result shouldBe emptyList()
            }
        }
    }

    Given("Communicatiekanaal EFormulier exists") {
        every { policyService.readOverigeRechten().beheren } returns true
        every { healthCheckService.bestaatCommunicatiekanaalEformulier() } returns true

        When("the check for the existence of the EFormulier communication channel is performed") {
            val result = healthCheckRestService.readBestaatCommunicatiekanaalEformulier()

            Then("it should return true") {
                result shouldBe true
            }
        }
    }

    Given("The user has the required 'beheren' permissions") {
        val now = ZonedDateTime.now()
        every { policyService.readOverigeRechten().beheren } returns true
        every { ztcClientService.resetCacheTimeToNow() } returns now
        every { ztcClientService.clearCacheTime() } returns "fakeResult"
        every { ztcClientService.clearZaaktypeCache() } returns "fakeResult"
        every { ztcClientService.clearStatustypeCache() } returns "fakeResult"
        every { ztcClientService.clearResultaattypeCache() } returns "fakeResult"
        every { ztcClientService.clearInformatieobjecttypeCache() } returns "fakeResult"
        every { ztcClientService.clearZaaktypeInformatieobjecttypeCache() } returns "fakeResult"
        every { ztcClientService.clearRoltypeCache() } returns "fakeResult"
        every { ztcClientService.clearBesluittypeCache() } returns "fakeResult"

        When("clearZTCCaches is called") {
            val result = healthCheckRestService.clearZTCCaches()

            Then("it should clear all ZTC caches and return the current cache reset time") {
                verify(exactly = 1) {
                    ztcClientService.clearZaaktypeCache()
                    ztcClientService.clearStatustypeCache()
                    ztcClientService.clearResultaattypeCache()
                    ztcClientService.clearInformatieobjecttypeCache()
                    ztcClientService.clearZaaktypeInformatieobjecttypeCache()
                    ztcClientService.clearBesluittypeCache()
                    ztcClientService.clearRoltypeCache()
                    ztcClientService.clearCacheTime()
                    ztcClientService.resetCacheTimeToNow()
                }
                result shouldBe now
            }
        }
    }

    Given("the user has the required permissions to manage rights") {
        val now = ZonedDateTime.now()
        every { policyService.readOverigeRechten().beheren } returns true
        every { ztcClientService.resetCacheTimeToNow() } returns now

        When("readZTCCacheTime is called") {
            val result = healthCheckRestService.readZTCCacheTime()

            Then("it should return the current cache reset time") {
                result shouldBe now
            }
        }
    }

    Given("build information is available") {
        val now = ZonedDateTime.now()
        val buildInformation = createBuildInformation(
            commit = "fakeCommit",
            buildId = "fakeCommit",
            buildDatumTijd = now,
            versienummer = "fakeVersionNumber"
        )
        every { healthCheckService.readBuildInformatie() } returns buildInformation

        When("readBuildInformatie is called") {
            val result = healthCheckRestService.readBuildInformatie()

            Then("it should return the correct build information object") {
                result.commit shouldBe "fakeCommit"
                result.buildId shouldBe "fakeCommit"
                result.buildDatumTijd shouldBe now
                result.versienummer shouldBe "fakeVersionNumber"
            }
        }
    }

    Given("The user does not have the required 'beheren' permissions") {
        every { policyService.readOverigeRechten().beheren } returns false

        When("listZaaktypeInrichtingschecks is called") {
            val exception = shouldThrow<PolicyException> {
                healthCheckRestService.listZaaktypeInrichtingschecks()
            }

            Then("it should throw a PolicyException") {
                exception shouldNotBe null
            }
        }

        When("clearZTCCaches is called") {
            val exception = shouldThrow<PolicyException> {
                healthCheckRestService.clearZTCCaches()
            }

            Then("it should throw a PolicyException") {
                exception shouldNotBe null
            }
        }

        When("readZTCCacheTime is called") {
            val exception = shouldThrow<PolicyException> {
                healthCheckRestService.readZTCCacheTime()
            }
            Then("it should throw a PolicyException") {
                exception shouldNotBe null
            }
        }
    }
})
