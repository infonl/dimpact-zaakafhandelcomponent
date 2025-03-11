/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.configuratie

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import net.atos.client.zgw.ztc.model.generated.Catalogus
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.CatalogusListParameters
import java.net.URI
import java.util.UUID

class ConfiguratieServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val ztcClientService = mockk<ZtcClientService>()
    val catalogus = mockk<Catalogus>()

    val additionalAllowedFileTypes = ""
    val zgwApiClientMpRestUrl = "https://example.com:1111"
    val contextUrl = "https://example.com:2222"
    val gemeenteCode = "gemeenteCode"
    val gemeenteNaam = "Gemeente Name"
    val gemeenteMail = "gemeente@example.com"
    val bpmnSupport = false

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A valid configuration") {
        val catalogusUri = "https://example.com/catalogus"
        every { catalogus.url } returns URI(catalogusUri)
        every { ztcClientService.readCatalogus(any<CatalogusListParameters>()) } returns catalogus

        val bronOrganisatie = "123443210"
        val verantwoordelijkeOrganisatie = "316245124"
        val catalogusDomein = "ALG"

        val configurationService = ConfiguratieService(
            entityManager,
            ztcClientService,
            additionalAllowedFileTypes,
            zgwApiClientMpRestUrl,
            contextUrl,
            gemeenteCode,
            gemeenteNaam,
            gemeenteMail,
            bpmnSupport,
            bronOrganisatie,
            verantwoordelijkeOrganisatie,
            catalogusDomein
        )

        When("zaak tonen URL is requested") {
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
    }

    Given("An invalid bron organisatie BSN") {
        val bronOrganisatie = "123456789"

        val verantwoordelijkeOrganisatie = "316245124"
        val catalogusDomein = "ALG"

        When("configuration service is initialized") {
            Then("BSN is validated") {
                shouldThrow<IllegalArgumentException> {
                    ConfiguratieService(
                        entityManager,
                        ztcClientService,
                        additionalAllowedFileTypes,
                        zgwApiClientMpRestUrl,
                        contextUrl,
                        gemeenteCode,
                        gemeenteNaam,
                        gemeenteMail,
                        bpmnSupport,
                        bronOrganisatie,
                        verantwoordelijkeOrganisatie,
                        catalogusDomein
                    )
                }
            }
        }
    }
})
