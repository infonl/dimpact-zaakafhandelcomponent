/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuratie

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import nl.info.client.brp.util.createBrpConfiguration
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.CatalogusListParameters
import nl.info.client.zgw.ztc.model.generated.Catalogus
import nl.info.zac.configuration.ConfigurationService
import java.net.URI
import java.util.Optional
import java.util.UUID

class ConfiguratieServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val ztcClientService = mockk<ZtcClientService>()
    val catalogus = mockk<Catalogus>()
    val additionalAllowedFileTypes = Optional.of("fakeFileType1,fakeFileType2")
    val zgwApiClientMpRestUrl = "https://example.com:1111"
    val contextUrl = "https://example.com:2222"
    val gemeenteCode = "gemeenteCode"
    val gemeenteNaam = "Gemeente Name"
    val gemeenteMail = "gemeente@example.com"

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A valid configuration with BPMN and PABC feature flags set to true") {
        val catalogusUri = "https://example.com/catalogus"
        every { catalogus.url } returns URI(catalogusUri)
        every { ztcClientService.readCatalogus(any<CatalogusListParameters>()) } returns catalogus
        val pabcIntegration = true
        val brpConfiguration = createBrpConfiguration()
        val bronOrganisatie = "123443210"
        val verantwoordelijkeOrganisatie = "316245124"
        val catalogusDomein = "ALG"
        val configurationService = ConfigurationService(
            entityManager = entityManager,
            ztcClientService = ztcClientService,
            additionalAllowedFileTypes = additionalAllowedFileTypes,
            zgwApiClientMpRestUrl = zgwApiClientMpRestUrl,
            contextUrl = contextUrl,
            gemeenteCode = gemeenteCode,
            gemeenteNaam = gemeenteNaam,
            gemeenteMail = gemeenteMail,
            pabcIntegration = pabcIntegration,
            bronOrganisatie = bronOrganisatie,
            verantwoordelijkeOrganisatie = verantwoordelijkeOrganisatie,
            catalogusDomein = catalogusDomein,
            brpConfiguration = brpConfiguration
        )

        When("zaak tonen URL is requested") {
            configurationService.onStartup(Any())
            val zaakTonenUrl = configurationService.zaakTonenUrl(zaakIdentificatie = "id")

            Then("correct url is built") {
                zaakTonenUrl.toString() shouldBe "$contextUrl/zaken/id"
            }
        }

        When("taak tonen URL is requested") {
            val taakTonenUrl = configurationService.taakTonenUrl(taakId = "id")

            Then("correct url is built") {
                taakTonenUrl.toString() shouldBe "$contextUrl/taken/id"
            }
        }

        When("informatieobject tonen url is requested") {
            val uuid = UUID.randomUUID()
            val informatieobjectTonenUrl = configurationService.informatieobjectTonenUrl(uuid)

            Then("Correct url is built") {
                informatieobjectTonenUrl.toString() shouldBe "$contextUrl/informatie-objecten/$uuid"
            }
        }

        When("additional allowed file types are requested") {
            val fileTypes = configurationService.readAdditionalAllowedFileTypes()

            Then("Correct list is returned") {
                fileTypes shouldBe listOf("fakeFileType1", "fakeFileType2")
            }
        }

        When("feature flag PABC integration is requested") {
            val featureFlagPabcIntegration = configurationService.featureFlagPabcIntegration()

            Then("true is returned") {
                featureFlagPabcIntegration shouldBe true
            }
        }
    }

    Given("An invalid bron organisatie BSN") {
        val bronOrganisatie = "123456789"
        val verantwoordelijkeOrganisatie = "316245124"
        val catalogusDomein = "ALG"
        val pabcIntegration = false
        val brpConfiguration = createBrpConfiguration()

        When("configuration service is initialized") {
            Then("BSN is validated") {
                shouldThrow<IllegalArgumentException> {
                    ConfigurationService(
                        entityManager = entityManager,
                        ztcClientService = ztcClientService,
                        additionalAllowedFileTypes = additionalAllowedFileTypes,
                        zgwApiClientMpRestUrl = zgwApiClientMpRestUrl,
                        contextUrl = contextUrl,
                        gemeenteCode = gemeenteCode,
                        gemeenteNaam = gemeenteNaam,
                        gemeenteMail = gemeenteMail,
                        pabcIntegration = pabcIntegration,
                        bronOrganisatie = bronOrganisatie,
                        verantwoordelijkeOrganisatie = verantwoordelijkeOrganisatie,
                        catalogusDomein = catalogusDomein,
                        brpConfiguration = brpConfiguration
                    ).onStartup(Any())
                }
            }
        }
    }

    Given("An empty additional file list") {
        val catalogusUri = "https://example.com/catalogus"
        every { catalogus.url } returns URI(catalogusUri)
        every { ztcClientService.readCatalogus(any<CatalogusListParameters>()) } returns catalogus
        val bronOrganisatie = "123443210"
        val verantwoordelijkeOrganisatie = "316245124"
        val catalogusDomein = "ALG"
        val pabcIntegration = false
        val brpConfiguration = createBrpConfiguration()
        val configurationService = ConfigurationService(
            entityManager = entityManager,
            ztcClientService = ztcClientService,
            additionalAllowedFileTypes = Optional.empty(),
            zgwApiClientMpRestUrl = zgwApiClientMpRestUrl,
            contextUrl = contextUrl,
            gemeenteCode = gemeenteCode,
            gemeenteNaam = gemeenteNaam,
            gemeenteMail = gemeenteMail,
            pabcIntegration = pabcIntegration,
            bronOrganisatie = bronOrganisatie,
            verantwoordelijkeOrganisatie = verantwoordelijkeOrganisatie,
            catalogusDomein = catalogusDomein,
            brpConfiguration = brpConfiguration
        )

        When("a list of additional allowed file types are requested") {
            configurationService.onStartup(Any())
            val fileTypes = configurationService.readAdditionalAllowedFileTypes()

            Then("an empty list is returned") {
                fileTypes.size shouldBe 0
            }
        }
    }
})
