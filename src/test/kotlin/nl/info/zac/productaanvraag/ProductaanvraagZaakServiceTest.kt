/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import jakarta.ws.rs.ProcessingException
import nl.info.client.klant.KlantClientService
import nl.info.client.klant.model.KlantcontactContactDetails
import nl.info.client.or.`object`.model.createORObject
import nl.info.client.or.`object`.model.createObjectRecord
import nl.info.client.zgw.drc.exception.DrcRuntimeException
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.model.generated.GeometryTypeEnum
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.model.createBetrokkeneKoppelingen
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.klant.model.contactdetails.ContactDetails
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.identity.IdentityService
import nl.info.zac.productaanvraag.model.createBetrokkene
import nl.info.zac.productaanvraag.model.createBron
import nl.info.zac.productaanvraag.model.createProductaanvraagDimpact
import nl.info.zac.productaanvraag.model.generated.Geometry
import nl.info.zac.productaanvraag.model.generated.Zaakgegevens
import nl.info.zac.test.util.createRandomStringWithAlphanumericCharacters
import nl.info.zac.zaak.ZaakService
import java.time.LocalDate
import java.util.UUID

@Suppress("LargeClass")
class ProductaanvraagZaakServiceTest : BehaviorSpec({
    val zgwApiService = mockk<ZgwApiService>()
    val zaakService = mockk<ZaakService>()
    val identityService = mockk<IdentityService>()
    val configurationService = mockk<ConfigurationService>()
    val klantClientService = mockk<KlantClientService>()
    val productaanvraagBetrokkeneService = mockk<ProductaanvraagBetrokkeneService>()
    val productaanvraagDocumentService = mockk<ProductaanvraagDocumentService>()
    val productaanvraagZaakService = ProductaanvraagZaakService(
        zgwApiService = zgwApiService,
        zaakService = zaakService,
        identityService = identityService,
        configurationService = configurationService,
        klantClientService = klantClientService,
        productaanvraagBetrokkeneService = productaanvraagBetrokkeneService,
        productaanvraagDocumentService = productaanvraagDocumentService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Creating a zaak for a productaanvraag") {
        val zaakType = createZaakType()
        val bron = createBron()
        val startDate = LocalDate.of(2026, 1, 2)
        val productaanvraagObject = createORObject(record = createObjectRecord(startAt = startDate))

        given(
            """
            a productaanvraag with zaakgegevens that contain a point geometry, an omschrijving and a toelichting
            that is too long to fit in the toelichting of the zaak
            """
        ) {
            clearAllMocks()
            val coordinates = listOf(52.08968250760225, 5.114358701512936)
            val zaakgegevensToelichting = createRandomStringWithAlphanumericCharacters(1000)
            val productaanvraagDimpact = createProductaanvraagDimpact().apply {
                this.bron = bron
                zaakgegevens = Zaakgegevens().apply {
                    geometry = Geometry().apply {
                        type = Geometry.Type.POINT
                        this.coordinates = coordinates
                    }
                    omschrijving = "fakeOmschrijving"
                    toelichting = zaakgegevensToelichting
                }
            }
            val createdZaak = createZaak()
            val zaakToBeCreated = slot<Zaak>()
            every { configurationService.readBronOrganisatie() } returns "fakeBronOrganisatie"
            every { zgwApiService.createZaak(capture(zaakToBeCreated)) } returns createdZaak

            `when`("the zaak is created") {
                val zaak = productaanvraagZaakService.createZaak(
                    zaaktype = zaakType,
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )

                then(
                    """
                    a zaak is created with the zaaktype, the start date of the productaanvraag object,
                    the bronorganisatie as bronorganisatie and verantwoordelijke organisatie, the 'E-formulier'
                    communicatiekanaal and the omschrijving of the zaakgegevens
                    """
                ) {
                    with(zaakToBeCreated.captured) {
                        zaaktype shouldBe zaakType.url
                        startdatum shouldBe startDate
                        bronorganisatie shouldBe "fakeBronOrganisatie"
                        verantwoordelijkeOrganisatie shouldBe "fakeBronOrganisatie"
                        communicatiekanaalNaam shouldBe "E-formulier"
                        omschrijving shouldBe "fakeOmschrijving"
                    }
                }

                and(
                    """
                    the zaakgeometrie is the point geometry with its coordinates swapped from
                    [latitude, longitude] to the [longitude, latitude] order of the ZGW API
                    """
                ) {
                    with(zaakToBeCreated.captured.zaakgeometrie) {
                        type shouldBe GeometryTypeEnum.POINT
                        this.coordinates[0].toDouble() shouldBe coordinates[1]
                        this.coordinates[1].toDouble() shouldBe coordinates[0]
                    }
                }

                and(
                    """
                    the toelichting names the bron of the productaanvraag followed by the toelichting of the
                    zaakgegevens, truncated to the maximum length of 1000 characters
                    """
                ) {
                    with(zaakToBeCreated.captured.toelichting) {
                        length shouldBe 1000
                        this shouldBe "Aangemaakt vanuit ${bron.naam} met kenmerk '${bron.kenmerk}'. $zaakgegevensToelichting"
                            .take(1000)
                    }
                }

                and("the created zaak is returned") {
                    zaak shouldBeSameInstanceAs createdZaak
                }
            }
        }

        given("a productaanvraag with zaakgegevens that contain a geometry without a supported type and a short toelichting") {
            clearAllMocks()
            val productaanvraagDimpact = createProductaanvraagDimpact().apply {
                this.bron = bron
                zaakgegevens = Zaakgegevens().apply {
                    geometry = Geometry().apply { coordinates = listOf(52.0, 5.0) }
                    toelichting = "fakeToelichting"
                }
            }
            val zaakToBeCreated = slot<Zaak>()
            every { configurationService.readBronOrganisatie() } returns "fakeBronOrganisatie"
            every { zgwApiService.createZaak(capture(zaakToBeCreated)) } returns createZaak()

            `when`("the zaak is created") {
                productaanvraagZaakService.createZaak(
                    zaaktype = zaakType,
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )

                then("the geometry is ignored and the toelichting of the zaakgegevens is appended to the toelichting") {
                    with(zaakToBeCreated.captured) {
                        zaakgeometrie shouldBe null
                        toelichting shouldBe "Aangemaakt vanuit ${bron.naam} met kenmerk '${bron.kenmerk}'. fakeToelichting"
                    }
                }
            }
        }

        given("a productaanvraag without zaakgegevens") {
            clearAllMocks()
            val productaanvraagDimpact = createProductaanvraagDimpact().apply { this.bron = bron }
            val zaakToBeCreated = slot<Zaak>()
            every { configurationService.readBronOrganisatie() } returns "fakeBronOrganisatie"
            every { zgwApiService.createZaak(capture(zaakToBeCreated)) } returns createZaak()

            `when`("the zaak is created") {
                productaanvraagZaakService.createZaak(
                    zaaktype = zaakType,
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )

                then("the zaak has no omschrijving and no zaakgeometrie, and its toelichting only names the bron") {
                    with(zaakToBeCreated.captured) {
                        omschrijving shouldBe null
                        zaakgeometrie shouldBe null
                        toelichting shouldBe "Aangemaakt vanuit ${bron.naam} met kenmerk '${bron.kenmerk}'."
                    }
                }
            }
        }
    }

    context("Finding the valid default behandelaar of a zaaktype configuration") {
        val zaak = createZaak()

        given("a default behandelaar that is a member of the default group") {
            clearAllMocks()
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                groupId = "fakeGroupId",
                defaultBehandelaarId = "fakeBehandelaarId"
            )
            every { identityService.isUserInGroup("fakeBehandelaarId", "fakeGroupId") } returns true

            `when`("the valid default behandelaar is requested") {
                val behandelaarId = productaanvraagZaakService.findValidDefaultBehandelaarId(zaaktypeConfiguration, zaak)

                then("the default behandelaar is returned") {
                    behandelaarId shouldBe "fakeBehandelaarId"
                }
            }
        }

        given("a default behandelaar that is not a member of the default group") {
            clearAllMocks()
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                groupId = "fakeGroupId",
                defaultBehandelaarId = "fakeBehandelaarId"
            )
            every { identityService.isUserInGroup("fakeBehandelaarId", "fakeGroupId") } returns false

            `when`("the valid default behandelaar is requested") {
                val behandelaarId = productaanvraagZaakService.findValidDefaultBehandelaarId(zaaktypeConfiguration, zaak)

                then("no behandelaar is returned, so that the zaak is assigned to the group only") {
                    behandelaarId shouldBe null
                }
            }
        }

        given("a default behandelaar without a default group") {
            clearAllMocks()
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                groupId = null,
                defaultBehandelaarId = "fakeBehandelaarId"
            )

            `when`("the valid default behandelaar is requested") {
                val behandelaarId = productaanvraagZaakService.findValidDefaultBehandelaarId(zaaktypeConfiguration, zaak)

                then("the default behandelaar is returned without checking a group membership") {
                    behandelaarId shouldBe "fakeBehandelaarId"
                    verify(exactly = 0) { identityService.isUserInGroup(any(), any()) }
                }
            }
        }

        given("a default group without a default behandelaar") {
            clearAllMocks()
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                groupId = "fakeGroupId",
                defaultBehandelaarId = null
            )

            `when`("the valid default behandelaar is requested") {
                val behandelaarId = productaanvraagZaakService.findValidDefaultBehandelaarId(zaaktypeConfiguration, zaak)

                then("no behandelaar is returned without checking a group membership") {
                    behandelaarId shouldBe null
                    verify(exactly = 0) { identityService.isUserInGroup(any(), any()) }
                }
            }
        }
    }

    context("Setting up a zaak from a productaanvraag") {
        val zaak = createZaak()
        val bron = createBron()
        val productaanvraagObject = createORObject()
        val initiatorBetrokkene = createBetrokkene()
        val klantcontactContactDetails = KlantcontactContactDetails(
            klantcontactUuid = UUID.randomUUID(),
            contactDetails = ContactDetails(emailAddress = "fake@example.com", telephoneNumber = null)
        )

        given(
            """
            a zaaktype configuration with a default group and BRP enabled but KVK disabled, a behandelaar,
            a productaanvraag with bijlagen and productaanvraag-specific contact details
            """
        ) {
            clearAllMocks()
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
                groupId = "fakeGroupId",
                zaaktypeBetrokkeneParameters = createBetrokkeneKoppelingen(brpKoppelen = true, kvkKoppelen = false)
            )
            val productaanvraagDimpact = createProductaanvraagDimpact().apply { this.bron = bron }
            every { productaanvraagDocumentService.pairProductaanvraagWithZaak(productaanvraagObject, zaak.url) } just runs
            every {
                zaakService.assignZaak(zaak = zaak, groupId = "fakeGroupId", userName = "fakeBehandelaarId", reason = null)
            } just runs
            every { productaanvraagDocumentService.pairAanvraagPDFWithZaak(productaanvraagDimpact, zaak.url) } just runs
            every {
                productaanvraagDocumentService.pairBijlagenWithZaakIgnoringExceptions(
                    bijlageURIs = productaanvraagDimpact.bijlagen,
                    zaakUrl = zaak.url
                )
            } just runs
            every {
                productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                    productaanvraag = productaanvraagDimpact,
                    zaak = zaak,
                    brpEnabled = true,
                    kvkEnabled = false
                )
            } returns initiatorBetrokkene
            every {
                klantClientService.findKlantcontactContactDetailsForFormulier(bron.kenmerk)
            } returns klantcontactContactDetails
            every {
                klantClientService.linkKlantcontactToZaak(
                    klantcontactUuid = klantcontactContactDetails.klantcontactUuid,
                    zaakUuid = zaak.uuid
                )
            } just runs

            `when`("the zaak is set up from the productaanvraag") {
                val productaanvraagInitiator = productaanvraagZaakService.setUpZaakFromProductaanvraag(
                    zaak = zaak,
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    behandelaarId = "fakeBehandelaarId",
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )

                then(
                    """
                    the productaanvraag is paired and the zaak is assigned to the group and the behandelaar first,
                    so that these are done even when a later step fails, followed by the documents, the betrokkenen
                    with the BRP and KVK settings of the zaaktype and the productaanvraag-specific contact details
                    """
                ) {
                    verifyOrder {
                        productaanvraagDocumentService.pairProductaanvraagWithZaak(productaanvraagObject, zaak.url)
                        zaakService.assignZaak(
                            zaak = zaak,
                            groupId = "fakeGroupId",
                            userName = "fakeBehandelaarId",
                            reason = null
                        )
                        productaanvraagDocumentService.pairAanvraagPDFWithZaak(productaanvraagDimpact, zaak.url)
                        productaanvraagDocumentService.pairBijlagenWithZaakIgnoringExceptions(
                            bijlageURIs = productaanvraagDimpact.bijlagen,
                            zaakUrl = zaak.url
                        )
                        productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                            productaanvraag = productaanvraagDimpact,
                            zaak = zaak,
                            brpEnabled = true,
                            kvkEnabled = false
                        )
                        klantClientService.findKlantcontactContactDetailsForFormulier(bron.kenmerk)
                        klantClientService.linkKlantcontactToZaak(
                            klantcontactUuid = klantcontactContactDetails.klantcontactUuid,
                            zaakUuid = zaak.uuid
                        )
                    }
                }

                and("the added initiator and the productaanvraag-specific contact details are returned") {
                    productaanvraagInitiator.betrokkene shouldBeSameInstanceAs initiatorBetrokkene
                    productaanvraagInitiator.klantcontactContactDetails shouldBe klantcontactContactDetails
                }
            }
        }

        given("a zaaktype configuration with a default group and no behandelaar") {
            clearAllMocks()
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(groupId = "fakeGroupId")
            val productaanvraagDimpact = createProductaanvraagDimpact().apply { this.bron = bron }
            every { productaanvraagDocumentService.pairProductaanvraagWithZaak(productaanvraagObject, zaak.url) } just runs
            every {
                zaakService.assignZaak(zaak = zaak, groupId = "fakeGroupId", userName = null, reason = null)
            } just runs
            every { productaanvraagDocumentService.pairAanvraagPDFWithZaak(productaanvraagDimpact, zaak.url) } just runs
            every {
                productaanvraagDocumentService.pairBijlagenWithZaakIgnoringExceptions(
                    bijlageURIs = productaanvraagDimpact.bijlagen,
                    zaakUrl = zaak.url
                )
            } just runs
            every {
                productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                    productaanvraag = productaanvraagDimpact,
                    zaak = zaak,
                    brpEnabled = true,
                    kvkEnabled = true
                )
            } returns initiatorBetrokkene
            every { klantClientService.findKlantcontactContactDetailsForFormulier(bron.kenmerk) } returns null

            `when`("the zaak is set up from the productaanvraag") {
                productaanvraagZaakService.setUpZaakFromProductaanvraag(
                    zaak = zaak,
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    behandelaarId = null,
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )

                then("the zaak is assigned to the default group only") {
                    verify(exactly = 1) {
                        zaakService.assignZaak(zaak = zaak, groupId = "fakeGroupId", userName = null, reason = null)
                    }
                }
            }
        }

        given("a zaaktype configuration without a default group and a behandelaar") {
            clearAllMocks()
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(groupId = null)
            val productaanvraagDimpact = createProductaanvraagDimpact().apply { this.bron = bron }
            every { productaanvraagDocumentService.pairProductaanvraagWithZaak(productaanvraagObject, zaak.url) } just runs
            every {
                zaakService.assignZaak(zaak = zaak, groupId = null, userName = "fakeBehandelaarId", reason = null)
            } just runs
            every { productaanvraagDocumentService.pairAanvraagPDFWithZaak(productaanvraagDimpact, zaak.url) } just runs
            every {
                productaanvraagDocumentService.pairBijlagenWithZaakIgnoringExceptions(
                    bijlageURIs = productaanvraagDimpact.bijlagen,
                    zaakUrl = zaak.url
                )
            } just runs
            every {
                productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                    productaanvraag = productaanvraagDimpact,
                    zaak = zaak,
                    brpEnabled = true,
                    kvkEnabled = true
                )
            } returns initiatorBetrokkene
            every { klantClientService.findKlantcontactContactDetailsForFormulier(bron.kenmerk) } returns null

            `when`("the zaak is set up from the productaanvraag") {
                productaanvraagZaakService.setUpZaakFromProductaanvraag(
                    zaak = zaak,
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    behandelaarId = "fakeBehandelaarId",
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )

                then("the zaak is assigned to the behandelaar only") {
                    verify(exactly = 1) {
                        zaakService.assignZaak(zaak = zaak, groupId = null, userName = "fakeBehandelaarId", reason = null)
                    }
                }
            }
        }

        given(
            """
            a zaaktype configuration without a default group and without betrokkene parameters, no behandelaar,
            a productaanvraag without bijlagen and no productaanvraag-specific contact details
            """
        ) {
            clearAllMocks()
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(groupId = null).apply {
                zaaktypeBetrokkeneParameters = null
            }
            val productaanvraagDimpact = createProductaanvraagDimpact().apply {
                this.bron = bron
                bijlagen = null
            }
            every { productaanvraagDocumentService.pairProductaanvraagWithZaak(productaanvraagObject, zaak.url) } just runs
            every { productaanvraagDocumentService.pairAanvraagPDFWithZaak(productaanvraagDimpact, zaak.url) } just runs
            every {
                productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                    productaanvraag = productaanvraagDimpact,
                    zaak = zaak,
                    brpEnabled = false,
                    kvkEnabled = false
                )
            } returns null
            every { klantClientService.findKlantcontactContactDetailsForFormulier(bron.kenmerk) } returns null

            `when`("the zaak is set up from the productaanvraag") {
                val productaanvraagInitiator = productaanvraagZaakService.setUpZaakFromProductaanvraag(
                    zaak = zaak,
                    zaaktypeConfiguration = zaaktypeConfiguration,
                    behandelaarId = null,
                    productaanvraagDimpact = productaanvraagDimpact,
                    productaanvraagObject = productaanvraagObject
                )

                then("the zaak is not assigned") {
                    verify(exactly = 0) {
                        zaakService.assignZaak(zaak = any(), groupId = any(), userName = any(), reason = any())
                    }
                }

                and("no bijlagen are paired with the zaak") {
                    verify(exactly = 0) {
                        productaanvraagDocumentService.pairBijlagenWithZaakIgnoringExceptions(any(), any())
                    }
                }

                and("the betrokkenen are added with BRP and KVK disabled") {
                    verify(exactly = 1) {
                        productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                            productaanvraag = productaanvraagDimpact,
                            zaak = zaak,
                            brpEnabled = false,
                            kvkEnabled = false
                        )
                    }
                }

                and("no klantcontact is linked to the zaak and neither an initiator nor contact details are returned") {
                    verify(exactly = 0) {
                        klantClientService.linkKlantcontactToZaak(any(), any())
                    }
                    productaanvraagInitiator shouldBe ProductaanvraagInitiator(
                        betrokkene = null,
                        klantcontactContactDetails = null
                    )
                }
            }
        }

        listOf(
            DrcRuntimeException("fakeDrcRuntimeExceptionMessage"),
            ProcessingException("fakeProcessingExceptionMessage")
        ).forEach { exception ->
            given("pairing the aanvraag PDF with the zaak fails with a ${exception::class.simpleName}") {
                clearAllMocks()
                val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(groupId = "fakeGroupId")
                val productaanvraagDimpact = createProductaanvraagDimpact().apply { this.bron = bron }
                every { productaanvraagDocumentService.pairProductaanvraagWithZaak(productaanvraagObject, zaak.url) } just runs
                every {
                    zaakService.assignZaak(zaak = zaak, groupId = "fakeGroupId", userName = null, reason = null)
                } just runs
                every {
                    productaanvraagDocumentService.pairAanvraagPDFWithZaak(productaanvraagDimpact, zaak.url)
                } throws exception
                every {
                    productaanvraagDocumentService.pairBijlagenWithZaakIgnoringExceptions(
                        bijlageURIs = productaanvraagDimpact.bijlagen,
                        zaakUrl = zaak.url
                    )
                } just runs
                every {
                    productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                        productaanvraag = productaanvraagDimpact,
                        zaak = zaak,
                        brpEnabled = true,
                        kvkEnabled = true
                    )
                } returns initiatorBetrokkene
                every { klantClientService.findKlantcontactContactDetailsForFormulier(bron.kenmerk) } returns null

                `when`("the zaak is set up from the productaanvraag") {
                    val productaanvraagInitiator = productaanvraagZaakService.setUpZaakFromProductaanvraag(
                        zaak = zaak,
                        zaaktypeConfiguration = zaaktypeConfiguration,
                        behandelaarId = null,
                        productaanvraagDimpact = productaanvraagDimpact,
                        productaanvraagObject = productaanvraagObject
                    )

                    then("the failure is logged and the bijlagen and betrokkenen are still added to the zaak") {
                        verify(exactly = 1) {
                            productaanvraagDocumentService.pairBijlagenWithZaakIgnoringExceptions(
                                bijlageURIs = productaanvraagDimpact.bijlagen,
                                zaakUrl = zaak.url
                            )
                            productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                                productaanvraag = productaanvraagDimpact,
                                zaak = zaak,
                                brpEnabled = true,
                                kvkEnabled = true
                            )
                        }
                        productaanvraagInitiator.betrokkene shouldBeSameInstanceAs initiatorBetrokkene
                    }
                }
            }
        }

        given("pairing the productaanvraag with the zaak fails") {
            clearAllMocks()
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(groupId = "fakeGroupId")
            val productaanvraagDimpact = createProductaanvraagDimpact().apply { this.bron = bron }
            every {
                productaanvraagDocumentService.pairProductaanvraagWithZaak(productaanvraagObject, zaak.url)
            } throws RuntimeException("fakeExceptionMessage")

            `when`("the zaak is set up from the productaanvraag") {
                val runtimeException = shouldThrow<RuntimeException> {
                    productaanvraagZaakService.setUpZaakFromProductaanvraag(
                        zaak = zaak,
                        zaaktypeConfiguration = zaaktypeConfiguration,
                        behandelaarId = null,
                        productaanvraagDimpact = productaanvraagDimpact,
                        productaanvraagObject = productaanvraagObject
                    )
                }

                then("the exception is rethrown and the zaak is not set up any further") {
                    runtimeException.message shouldBe "fakeExceptionMessage"
                    verify(exactly = 0) {
                        zaakService.assignZaak(zaak = any(), groupId = any(), userName = any(), reason = any())
                        productaanvraagDocumentService.pairAanvraagPDFWithZaak(any(), any())
                        productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                            productaanvraag = any(),
                            zaak = any(),
                            brpEnabled = any(),
                            kvkEnabled = any()
                        )
                    }
                }
            }
        }
    }
})
