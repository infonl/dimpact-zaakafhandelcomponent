/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import nl.info.client.klant.KlantClientService
import nl.info.client.or.`object`.model.createORObject
import nl.info.client.or.`object`.model.createObjectRecord
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.identity.IdentityService
import nl.info.zac.productaanvraag.model.createBron
import nl.info.zac.productaanvraag.model.createProductaanvraagDimpact
import nl.info.zac.zaak.ZaakService

class ProductaanvraagZaakServiceTest : BehaviorSpec({
    val zgwApiService = mockk<ZgwApiService>()
    val configurationService = mockk<ConfigurationService>()
    val productaanvraagClaimRepository = mockk<ProductaanvraagClaimRepository>()
    val productaanvraagZaakService = ProductaanvraagZaakService(
        zgwApiService = zgwApiService,
        zaakService = mockk<ZaakService>(),
        identityService = mockk<IdentityService>(),
        configurationService = configurationService,
        klantClientService = mockk<KlantClientService>(),
        productaanvraagBetrokkeneService = mockk<ProductaanvraagBetrokkeneService>(),
        productaanvraagDocumentService = mockk<ProductaanvraagDocumentService>(),
        productaanvraagClaimRepository = productaanvraagClaimRepository
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Creating a zaak for a productaanvraag") {
        val zaaktype = createZaakType()
        val productaanvraagDimpact = createProductaanvraagDimpact().apply { bron = createBron() }
        val productaanvraagObject = createORObject(record = createObjectRecord())

        given("a zaak can be created in the ZRC") {
            clearAllMocks()
            val zaak = createZaak()
            every { configurationService.readBronOrganisatie() } returns "fakeBronOrganisatie"
            every { zgwApiService.createZaak(any()) } returns zaak
            every { productaanvraagClaimRepository.markDone(productaanvraagObject.uuid) } just runs

            `when`("the zaak is created") {
                val createdZaak = productaanvraagZaakService.createZaak(
                    zaaktype = zaaktype,
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )

                then(
                    """
                    the created zaak is returned and the productaanvraag is marked as done right after the zaak is
                    created, so that a failure later on never makes a retry create a second zaak
                    """
                ) {
                    createdZaak shouldBe zaak
                    verifyOrder {
                        zgwApiService.createZaak(any())
                        productaanvraagClaimRepository.markDone(productaanvraagObject.uuid)
                    }
                }
            }
        }

        given("a zaak that cannot be created in the ZRC") {
            clearAllMocks()
            every { configurationService.readBronOrganisatie() } returns "fakeBronOrganisatie"
            every { zgwApiService.createZaak(any()) } throws IllegalStateException("fakeZaakCreationFailure")

            `when`("the zaak is created") {
                val illegalStateException = shouldThrow<IllegalStateException> {
                    productaanvraagZaakService.createZaak(
                        zaaktype = zaaktype,
                        productaanvraagDimpact = productaanvraagDimpact,
                        productaanvraagObject = productaanvraagObject
                    )
                }

                then(
                    """
                    the productaanvraag is not marked as done, so that it is handled again after its claim times out
                    """
                ) {
                    illegalStateException.message shouldBe "fakeZaakCreationFailure"
                    verify(exactly = 0) {
                        productaanvraagClaimRepository.markDone(any())
                    }
                }
            }
        }
    }
})
