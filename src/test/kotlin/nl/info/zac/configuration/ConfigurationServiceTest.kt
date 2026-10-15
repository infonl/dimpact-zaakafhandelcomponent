/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.CatalogusListParameters
import nl.info.client.zgw.ztc.model.createCatalogus
import nl.info.zac.authentication.LoggedInUserProvider

class ConfigurationServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val configurationService = ConfigurationService(
        brpConfiguration = mockk<BrpConfigurationProvider>(relaxed = true),
        entityManager = mockk<EntityManager>(),
        ztcClientService = ztcClientService,
        bronOrganisatie = "123443210",
        catalogusDomein = "fakeCatalogusDomein",
        contextUrl = "https://example.com/zac",
        gemeenteCode = "fakeGemeenteCode",
        gemeenteNaam = "fakeGemeenteNaam",
        gemeenteMail = "fake@example.com",
        verantwoordelijkeOrganisatie = "123443210",
        zgwApiClientMpRestUrl = "https://example.com/zgw"
    )

    afterEach { checkUnnecessaryStub() }

    context("Starting up ZAC") {
        given("a catalogus for the configured catalogus domein, outside any user session") {
            val catalogus = createCatalogus()
            val runsAsSystemUser = mutableListOf<Boolean>()
            every { ztcClientService.readCatalogus(any<CatalogusListParameters>()) } answers {
                runsAsSystemUser += LoggedInUserProvider.systemUser.get()
                catalogus
            }

            `when`("onStartup is called") {
                configurationService.onStartup(Any())

                then("the catalogus is read as the system user instead of falling back to the functionele gebruiker") {
                    runsAsSystemUser shouldBe listOf(true)
                }

                and("its URL becomes the default catalogus URI") {
                    configurationService.readDefaultCatalogusURI() shouldBe catalogus.url
                }
            }
        }
    }
})
