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
import java.util.UUID

class ConfiguratieServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val ztcClientService = mockk<ZtcClientService>()
    val catalogus = mockk<Catalogus>()
    val zgwApiClientMpRestUrl = "https://example.com:1111"
    val contextUrl = "https://example.com:2222"
    val gemeenteCode = "gemeenteCode"
    val gemeenteNaam = "Gemeente Name"
    val gemeenteMail = "gemeente@example.com"

    afterEach {
        checkUnnecessaryStub()
    }

    given("A valid configuration with BPMN") {
        val catalogusUri = "https://example.com/catalogus"
        every { catalogus.url } returns URI(catalogusUri)
        every { ztcClientService.readCatalogus(any<CatalogusListParameters>()) } returns catalogus
        val brpConfiguration = createBrpConfiguration()
        val bronOrganisatie = "123443210"
        val verantwoordelijkeOrganisatie = "316245124"
        val catalogusDomein = "ALG"
        val configurationService = ConfigurationService(
            entityManager = entityManager,
            ztcClientService = ztcClientService,
            zgwApiClientMpRestUrl = zgwApiClientMpRestUrl,
            contextUrl = contextUrl,
            gemeenteCode = gemeenteCode,
            gemeenteNaam = gemeenteNaam,
            gemeenteMail = gemeenteMail,
            bronOrganisatie = bronOrganisatie,
            verantwoordelijkeOrganisatie = verantwoordelijkeOrganisatie,
            catalogusDomein = catalogusDomein,
            brpConfiguration = brpConfiguration
        )

        `when`("zaak tonen URL is requested") {
            configurationService.onStartup(Any())
            val zaakTonenUrl = configurationService.zaakTonenUrl(zaakIdentificatie = "id")

            then("correct url is built") {
                zaakTonenUrl.toString() shouldBe "$contextUrl/zaken/id"
            }
        }

        `when`("taak tonen URL is requested") {
            val taakTonenUrl = configurationService.taakTonenUrl(taakId = "id")

            then("correct url is built") {
                taakTonenUrl.toString() shouldBe "$contextUrl/taken/id"
            }
        }

        `when`("informatieobject tonen url is requested") {
            val uuid = UUID.randomUUID()
            val informatieobjectTonenUrl = configurationService.informatieobjectTonenUrl(uuid)

            then("Correct url is built") {
                informatieobjectTonenUrl.toString() shouldBe "$contextUrl/informatie-objecten/$uuid"
            }
        }
    }

    given("An invalid bron organisatie BSN") {
        val bronOrganisatie = "123456789"
        val verantwoordelijkeOrganisatie = "316245124"
        val catalogusDomein = "ALG"
        val brpConfiguration = createBrpConfiguration()

        `when`("configuration service is initialized") {
            then("BSN is validated") {
                shouldThrow<IllegalArgumentException> {
                    ConfigurationService(
                        entityManager = entityManager,
                        ztcClientService = ztcClientService,
                        zgwApiClientMpRestUrl = zgwApiClientMpRestUrl,
                        contextUrl = contextUrl,
                        gemeenteCode = gemeenteCode,
                        gemeenteNaam = gemeenteNaam,
                        gemeenteMail = gemeenteMail,
                        bronOrganisatie = bronOrganisatie,
                        verantwoordelijkeOrganisatie = verantwoordelijkeOrganisatie,
                        catalogusDomein = catalogusDomein,
                        brpConfiguration = brpConfiguration
                    ).onStartup(Any())
                }
            }
        }
    }
})
