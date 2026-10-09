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
import nl.info.client.klant.model.KlantcontactContactDetails
import nl.info.client.or.`object`.model.createORObject
import nl.info.client.or.`object`.model.createObjectRecord
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.klant.model.contactdetails.ContactDetails
import nl.info.zac.flowable.ProcessStartData
import nl.info.zac.flowable.ZaakProcessService
import nl.info.zac.productaanvraag.model.createBetrokkene
import nl.info.zac.productaanvraag.model.createProductaanvraagDimpact
import java.util.UUID

class ProductaanvraagCmmnServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zaakProcessService = mockk<ZaakProcessService>()
    val productaanvraagZaakService = mockk<ProductaanvraagZaakService>()
    val productaanvraagEmailService = mockk<ProductaanvraagEmailService>()
    val productaanvraagClaimRepository = mockk<ProductaanvraagClaimRepository>()
    val productaanvraagCmmnService = ProductaanvraagCmmnService(
        ztcClientService = ztcClientService,
        zaakProcessService = zaakProcessService,
        productaanvraagZaakService = productaanvraagZaakService,
        productaanvraagEmailService = productaanvraagEmailService,
        productaanvraagClaimRepository = productaanvraagClaimRepository
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Creating and starting a zaak for a productaanvraag with a CMMN zaaktype") {
        val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
            groupId = "fakeGroupId",
            defaultBehandelaarId = "fakeBehandelaarId"
        )
        val zaaktype = createZaakType()
        val zaak = createZaak()
        val productaanvraagDimpact = createProductaanvraagDimpact()
        val productaanvraagObject = createORObject(
            record = createObjectRecord(
                data = mapOf("aanvraaggegevens" to mapOf("fakeFormStep" to mapOf("fakeKey" to "fakeValue")))
            )
        )
        val productaanvraagInitiator = ProductaanvraagInitiator(
            betrokkene = createBetrokkene(),
            klantcontactContactDetails = null
        )

        given("a zaak can be created, set up from the productaanvraag and have its CMMN case started") {
            clearAllMocks()
            every { ztcClientService.readZaaktype(zaaktypeConfiguration.zaaktypeUuid) } returns zaaktype
            every {
                productaanvraagZaakService.createZaak(zaaktype, productaanvraagDimpact, productaanvraagObject)
            } returns zaak
            every {
                productaanvraagZaakService.findValidDefaultBehandelaarId(zaaktypeConfiguration, zaak)
            } returns "fakeBehandelaarId"
            every {
                productaanvraagZaakService.setUpZaakFromProductaanvraag(
                    zaak = zaak,
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    behandelaarId = "fakeBehandelaarId",
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )
            } returns productaanvraagInitiator
            every {
                zaakProcessService.start(
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    zaak = zaak,
                    zaaktype = zaaktype,
                    processStartData = any()
                )
            } just runs
            every { productaanvraagClaimRepository.markDone(productaanvraagObject.uuid) } just runs
            every {
                productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                    zaak = zaak,
                    betrokkene = productaanvraagInitiator.betrokkene,
                    productaanvraagSpecificEmailAddress = null,
                    zaaktypeConfiguration = zaaktypeConfiguration
                )
            } just runs

            `when`("the zaak is created and started") {
                productaanvraagCmmnService.createAndStartZaak(
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )

                then(
                    """
                    the zaak is created, the zaak is set up from the productaanvraag before the CMMN case is started,
                    the productaanvraag is marked as done once the case is started, and only then the confirmation
                    of receipt is sent
                    """
                ) {
                    verifyOrder {
                        productaanvraagZaakService.createZaak(zaaktype, productaanvraagDimpact, productaanvraagObject)
                        productaanvraagZaakService.setUpZaakFromProductaanvraag(
                            zaak = zaak,
                            zaaktypeConfiguration = zaaktypeConfiguration,
                            behandelaarId = "fakeBehandelaarId",
                            productaanvraagDimpact = productaanvraagDimpact,
                            productaanvraagObject = productaanvraagObject
                        )
                        zaakProcessService.start(
                            zaaktypeConfiguration = zaaktypeConfiguration,
                            zaak = zaak,
                            zaaktype = zaaktype,
                            processStartData = ProcessStartData(zaakData = mapOf("fakeKey" to "fakeValue"))
                        )
                        productaanvraagClaimRepository.markDone(productaanvraagObject.uuid)
                        productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                            zaak = zaak,
                            betrokkene = productaanvraagInitiator.betrokkene,
                            productaanvraagSpecificEmailAddress = null,
                            zaaktypeConfiguration = zaaktypeConfiguration
                        )
                    }
                }
            }
        }

        given("a productaanvraag with a klantcontact with an email address of its own") {
            clearAllMocks()
            val productaanvraagInitiatorWithContactDetails = ProductaanvraagInitiator(
                betrokkene = createBetrokkene(),
                klantcontactContactDetails = KlantcontactContactDetails(
                    klantcontactUuid = UUID.randomUUID(),
                    contactDetails = ContactDetails(
                        emailAddress = "fake@example.com",
                        telephoneNumber = "fakeTelephoneNumber"
                    )
                )
            )
            every { ztcClientService.readZaaktype(zaaktypeConfiguration.zaaktypeUuid) } returns zaaktype
            every {
                productaanvraagZaakService.createZaak(zaaktype, productaanvraagDimpact, productaanvraagObject)
            } returns zaak
            every {
                productaanvraagZaakService.findValidDefaultBehandelaarId(zaaktypeConfiguration, zaak)
            } returns "fakeBehandelaarId"
            every {
                productaanvraagZaakService.setUpZaakFromProductaanvraag(
                    zaak = zaak,
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    behandelaarId = "fakeBehandelaarId",
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )
            } returns productaanvraagInitiatorWithContactDetails
            every {
                zaakProcessService.start(
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    zaak = zaak,
                    zaaktype = zaaktype,
                    processStartData = any()
                )
            } just runs
            every { productaanvraagClaimRepository.markDone(productaanvraagObject.uuid) } just runs
            every {
                productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                    zaak = zaak,
                    betrokkene = productaanvraagInitiatorWithContactDetails.betrokkene,
                    productaanvraagSpecificEmailAddress = "fake@example.com",
                    zaaktypeConfiguration = zaaktypeConfiguration
                )
            } just runs

            `when`("the zaak is created and started") {
                productaanvraagCmmnService.createAndStartZaak(
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )

                then("the confirmation of receipt is sent to the email address of the klantcontact") {
                    verify(exactly = 1) {
                        productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                            zaak = zaak,
                            betrokkene = productaanvraagInitiatorWithContactDetails.betrokkene,
                            productaanvraagSpecificEmailAddress = "fake@example.com",
                            zaaktypeConfiguration = zaaktypeConfiguration
                        )
                    }
                }
            }
        }

        given("a zaak whose CMMN case cannot be started") {
            clearAllMocks()
            every { ztcClientService.readZaaktype(zaaktypeConfiguration.zaaktypeUuid) } returns zaaktype
            every {
                productaanvraagZaakService.createZaak(zaaktype, productaanvraagDimpact, productaanvraagObject)
            } returns zaak
            every {
                productaanvraagZaakService.findValidDefaultBehandelaarId(zaaktypeConfiguration, zaak)
            } returns "fakeBehandelaarId"
            every {
                productaanvraagZaakService.setUpZaakFromProductaanvraag(
                    zaak = zaak,
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    behandelaarId = "fakeBehandelaarId",
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )
            } returns productaanvraagInitiator
            every {
                zaakProcessService.start(
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    zaak = zaak,
                    zaaktype = zaaktype,
                    processStartData = any()
                )
            } throws IllegalStateException("fakeCaseStartFailure")

            `when`("the zaak is created and started") {
                val illegalStateException = shouldThrow<IllegalStateException> {
                    productaanvraagCmmnService.createAndStartZaak(
                        zaaktypeConfiguration = zaaktypeConfiguration,
                        productaanvraagDimpact = productaanvraagDimpact,
                        productaanvraagObject = productaanvraagObject
                    )
                }

                then(
                    """
                    the zaak is already set up from the productaanvraag, but the productaanvraag is not marked as done
                    and no confirmation of receipt is sent
                    """
                ) {
                    illegalStateException.message shouldBe "fakeCaseStartFailure"
                    verify(exactly = 1) {
                        productaanvraagZaakService.setUpZaakFromProductaanvraag(
                            zaak = zaak,
                            zaaktypeConfiguration = zaaktypeConfiguration,
                            behandelaarId = "fakeBehandelaarId",
                            productaanvraagDimpact = productaanvraagDimpact,
                            productaanvraagObject = productaanvraagObject
                        )
                    }
                    verify(exactly = 0) {
                        productaanvraagClaimRepository.markDone(any())
                        productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
                            zaak = any(),
                            betrokkene = any(),
                            productaanvraagSpecificEmailAddress = any(),
                            zaaktypeConfiguration = any()
                        )
                    }
                }
            }
        }
    }
})
