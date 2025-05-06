/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.exception.PolicyException
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.healthcheck.createZaaktypeInrichtingscheck
import java.net.URI
import java.util.UUID

class HealthCheckRestServiceTest: BehaviorSpec ({
     val ztcClientService = mockk<ZtcClientService>()
     val configuratieService = mockk<ConfiguratieService>()
     val healthCheckService= mockk< HealthCheckService>()
     val policyService= mockk<PolicyService>()
    val healthCheckRestService = HealthCheckRestService(
        ztcClientService = ztcClientService,
        configuratieService = configuratieService,
        healthCheckService = healthCheckService,
        policyService = policyService
    )

    Given("Two configured zaaktypen") {
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

            Then("it should return a list of RESTZaaktypeInrichtingscheck objects") {
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

    Given("the user does not have the required 'beheren' permissions") {
        every { policyService.readOverigeRechten().beheren } returns false

        When("listZaaktypeInrichtingschecks is called") {
            val exception = shouldThrow<PolicyException> {
                healthCheckRestService.listZaaktypeInrichtingschecks()
            }

            Then("it should throw a PolicyException") {
                exception shouldNotBe null
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
        every { healthCheckService.bestaatCommunicatiekanaalEformulier() } returns true

        When("the check for the existence of the EFormulier communication channel is performed") {
            val result = healthCheckRestService.readBestaatCommunicatiekanaalEformulier()

            Then("it should return true") {
                result shouldBe true
            }
        }
    }
})
