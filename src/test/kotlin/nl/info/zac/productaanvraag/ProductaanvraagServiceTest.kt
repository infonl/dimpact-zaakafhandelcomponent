/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import jakarta.enterprise.inject.Instance
import jakarta.servlet.http.HttpSession
import nl.info.client.or.`object`.ObjectsClientService
import nl.info.client.or.`object`.model.createORObject
import nl.info.client.or.`object`.model.createObjectRecord
import nl.info.client.or.shared.exception.ORRuntimeException
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.LoggedInUserProvider
import nl.info.zac.document.inboxdocument.InboxDocumentService
import nl.info.zac.document.inboxdocument.repository.model.createInboxDocument
import nl.info.zac.productaanvraag.model.InboxProductaanvraag
import nl.info.zac.productaanvraag.model.createBron
import nl.info.zac.productaanvraag.model.generated.Geometry
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class ProductaanvraagServiceTest : BehaviorSpec({
    val objectsClientService = mockk<ObjectsClientService>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val inboxDocumentService = mockk<InboxDocumentService>()
    val inboxProductaanvraagService = mockk<InboxProductaanvraagService>()
    val productaanvraagCmmnService = mockk<ProductaanvraagCmmnService>()
    val productaanvraagBpmnService = mockk<ProductaanvraagBpmnService>()
    val productaanvraagClaimRepository = mockk<ProductaanvraagClaimRepository>()
    val productaanvraagService = ProductaanvraagService(
        objectsClientService = objectsClientService,
        zaaktypeConfigurationService = zaaktypeConfigurationService,
        inboxDocumentService = inboxDocumentService,
        inboxProductaanvraagService = inboxProductaanvraagService,
        productaanvraagCmmnService = productaanvraagCmmnService,
        productaanvraagBpmnService = productaanvraagBpmnService,
        productaanvraagClaimRepository = productaanvraagClaimRepository
    )
    val productaanvraagType = "fakeProductaanvraagType"
    val aanvraaggegevens = mapOf("fakeFormStep" to mapOf("fakeKey" to "fakeValue"))

    fun createProductaanvraagObject(
        productaanvraagObjectUuid: UUID,
        additionalData: Map<String, Any> = emptyMap()
    ) = createORObject(
        uuid = productaanvraagObjectUuid,
        record = createObjectRecord(
            data = mapOf(
                "bron" to createBron(),
                "type" to productaanvraagType,
                "aanvraaggegevens" to aanvraaggegevens
            ) + additionalData,
            registrationAt = LocalDate.of(2021, 1, 1)
        )
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Get aanvraaggegevens") {
        given("a productaanvraag-dimpact object with aanvraaggegevens containing form steps with key-value pairs") {
            val orObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to createBron(),
                        "type" to productaanvraagType,
                        "aanvraaggegevens" to mapOf(
                            "formStep1" to mapOf(
                                "fakeKey1" to "fakeValue1",
                                "fakeKey2" to "fakeValue2"
                            ),
                            "formStep2" to mapOf(
                                "fakeKey3" to "fakeValue3"
                            )
                        )
                    )
                )
            )

            `when`("the form data is requested from the productaanvraag") {
                val formData = productaanvraagService.getAanvraaggegevens(orObject)

                then("all key-value pairs in the aanvraaggegevens are returned") {
                    with(formData) {
                        this["fakeKey1"] shouldBe "fakeValue1"
                        this["fakeKey2"] shouldBe "fakeValue2"
                        this["fakeKey3"] shouldBe "fakeValue3"
                    }
                }
            }
        }
    }

    context("Get productaanvraag") {
        given("a productaanvraag-dimpact object registration object with zaakgegevens") {
            val bron = createBron()
            val zaakIdentificatie = "fakeZaakIdentificatie"
            val zaakOmschrijving = "fakeOmschrijving"
            val zaakToelichting = "fakeToelichting"
            val coordinates = listOf(52.08968250760225, 5.114358701512936)
            val orObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to bron,
                        "type" to productaanvraagType,
                        "zaakgegevens" to mapOf(
                            "identificatie" to zaakIdentificatie,
                            "geometry" to mapOf(
                                "type" to "Point",
                                "coordinates" to coordinates
                            ),
                            "omschrijving" to zaakOmschrijving,
                            "toelichting" to zaakToelichting
                        )
                    )
                )
            )

            `when`("the productaanvraag is requested from the product aanvraag service") {
                val productaanvraagDimpact = productaanvraagService.getProductaanvraag(orObject)

                then(
                    "the productaanvraag of type 'productaanvraag Dimpact' is returned and contains the expected data"
                ) {
                    with(productaanvraagDimpact) {
                        with(this.bron) {
                            naam shouldBe bron.naam
                            kenmerk shouldBe bron.kenmerk
                        }
                        taal shouldBe "nld"
                        type shouldBe productaanvraagType
                        with(zaakgegevens) {
                            identificatie shouldBe zaakIdentificatie
                            with(geometry) {
                                this.type shouldBe Geometry.Type.POINT
                                this.coordinates shouldBe coordinates
                            }
                            omschrijving shouldBe zaakOmschrijving
                            toelichting shouldBe zaakToelichting
                        }
                    }
                }
            }
        }

        given("a productaanvraag-dimpact object registration object without zaakgegevens") {
            val bron = createBron()
            val orObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to bron,
                        "type" to productaanvraagType
                    )
                )
            )

            `when`("the productaanvraag is requested from the product aanvraag service") {
                val productaanvraagDimpact = productaanvraagService.getProductaanvraag(orObject)

                then(
                    "the productaanvraag of type 'productaanvraag Dimpact' is returned and contains the expected data"
                ) {
                    with(productaanvraagDimpact) {
                        with(this.bron) {
                            naam shouldBe bron.naam
                            kenmerk shouldBe bron.kenmerk
                        }
                        taal shouldBe "nld"
                        type shouldBe productaanvraagType
                        zaakgegevens shouldBe null
                    }
                }
            }
        }
    }

    context("Handle productaanvraag") {
        given("a productaanvraag whose productaanvraagtype is configured on a CMMN-bound zaaktype configuration") {
            clearAllMocks()
            val productaanvraagObjectUuid = UUID.randomUUID()
            val productaanvraagObject = createProductaanvraagObject(productaanvraagObjectUuid)
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
            every { productaanvraagClaimRepository.claim(productaanvraagObjectUuid) } returns true
            every { objectsClientService.readObject(productaanvraagObjectUuid) } returns productaanvraagObject
            every {
                zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype(productaanvraagType)
            } returns listOf(zaaktypeCmmnConfiguration)
            every {
                productaanvraagCmmnService.createAndStartZaak(
                    zaaktypeConfiguration = zaaktypeCmmnConfiguration,
                    productaanvraagDimpact = match { it.type == productaanvraagType },
                    productaanvraagObject = productaanvraagObject
                )
            } just runs

            `when`("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productaanvraagObjectUuid)

                then("a zaak is created and started by the CMMN service, and no inbox productaanvraag is created") {
                    verify(exactly = 1) {
                        productaanvraagCmmnService.createAndStartZaak(
                            zaaktypeConfiguration = zaaktypeCmmnConfiguration,
                            productaanvraagDimpact = any(),
                            productaanvraagObject = productaanvraagObject
                        )
                    }
                    verify(exactly = 0) {
                        productaanvraagBpmnService.createAndStartZaak(any(), any(), any())
                        inboxProductaanvraagService.create(any())
                    }
                }
            }
        }

        given("a productaanvraag whose productaanvraagtype is configured on a BPMN-bound zaaktype configuration") {
            clearAllMocks()
            val productaanvraagObjectUuid = UUID.randomUUID()
            val productaanvraagObject = createProductaanvraagObject(productaanvraagObjectUuid)
            val zaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration()
            every { productaanvraagClaimRepository.claim(productaanvraagObjectUuid) } returns true
            every { objectsClientService.readObject(productaanvraagObjectUuid) } returns productaanvraagObject
            every {
                zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype(productaanvraagType)
            } returns listOf(zaaktypeBpmnConfiguration)
            every {
                productaanvraagBpmnService.createAndStartZaak(
                    zaaktypeConfiguration = zaaktypeBpmnConfiguration,
                    productaanvraagDimpact = match { it.type == productaanvraagType },
                    productaanvraagObject = productaanvraagObject
                )
            } just runs

            `when`("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productaanvraagObjectUuid)

                then("a zaak is created and started by the BPMN service, and no inbox productaanvraag is created") {
                    verify(exactly = 1) {
                        productaanvraagBpmnService.createAndStartZaak(
                            zaaktypeConfiguration = zaaktypeBpmnConfiguration,
                            productaanvraagDimpact = any(),
                            productaanvraagObject = productaanvraagObject
                        )
                    }
                    verify(exactly = 0) {
                        productaanvraagCmmnService.createAndStartZaak(any(), any(), any())
                        inboxProductaanvraagService.create(any())
                    }
                }
            }
        }

        given(
            """
            a productaanvraag whose productaanvraagtype is configured on a CMMN-bound zaaktype configuration and on
            a less recently created BPMN-bound zaaktype configuration
            """
        ) {
            clearAllMocks()
            val productaanvraagObjectUuid = UUID.randomUUID()
            val productaanvraagObject = createProductaanvraagObject(productaanvraagObjectUuid)
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(creationDate = ZonedDateTime.now())
            val zaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration(
                creationDate = ZonedDateTime.now().minusDays(1)
            )
            every { productaanvraagClaimRepository.claim(productaanvraagObjectUuid) } returns true
            every { objectsClientService.readObject(productaanvraagObjectUuid) } returns productaanvraagObject
            every {
                zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype(productaanvraagType)
            } returns listOf(zaaktypeCmmnConfiguration, zaaktypeBpmnConfiguration)
            every {
                productaanvraagCmmnService.createAndStartZaak(
                    zaaktypeConfiguration = zaaktypeCmmnConfiguration,
                    productaanvraagDimpact = any(),
                    productaanvraagObject = productaanvraagObject
                )
            } just runs

            `when`("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productaanvraagObjectUuid)

                then("only the most recently created configuration is used to create and start a zaak") {
                    verify(exactly = 1) {
                        productaanvraagCmmnService.createAndStartZaak(
                            zaaktypeConfiguration = zaaktypeCmmnConfiguration,
                            productaanvraagDimpact = any(),
                            productaanvraagObject = productaanvraagObject
                        )
                    }
                    verify(exactly = 0) {
                        productaanvraagBpmnService.createAndStartZaak(any(), any(), any())
                    }
                }
            }
        }

        given("a productaanvraag for which creating and starting the zaak fails") {
            clearAllMocks()
            val productaanvraagObjectUuid = UUID.randomUUID()
            val productaanvraagObject = createProductaanvraagObject(productaanvraagObjectUuid)
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
            every { productaanvraagClaimRepository.claim(productaanvraagObjectUuid) } returns true
            every { objectsClientService.readObject(productaanvraagObjectUuid) } returns productaanvraagObject
            every {
                zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype(productaanvraagType)
            } returns listOf(zaaktypeCmmnConfiguration)
            every {
                productaanvraagCmmnService.createAndStartZaak(
                    zaaktypeConfiguration = zaaktypeCmmnConfiguration,
                    productaanvraagDimpact = any(),
                    productaanvraagObject = productaanvraagObject
                )
            } throws IllegalStateException("fakeZaakCreationFailure")

            `when`("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productaanvraagObjectUuid)

                then(
                    """
                    the failure does not reach the caller, no inbox productaanvraag is created, and the
                    productaanvraag is not marked as done
                    """
                ) {
                    verify(exactly = 0) {
                        inboxProductaanvraagService.create(any())
                        productaanvraagClaimRepository.markDone(any())
                    }
                }
            }
        }

        given("a productaanvraag for whose productaanvraagtype no zaaktype configuration exists") {
            clearAllMocks()
            val productaanvraagObjectUuid = UUID.randomUUID()
            val pdfUuid = UUID.randomUUID()
            val bijlageUuid = UUID.randomUUID()
            val productaanvraagObject = createProductaanvraagObject(
                productaanvraagObjectUuid = productaanvraagObjectUuid,
                additionalData = mapOf(
                    "betrokkenen" to listOf(
                        mapOf("inpBsn" to "fakeBsn", "rolOmschrijvingGeneriek" to "initiator")
                    ),
                    "pdf" to URI("https://example.com/documenten/$pdfUuid"),
                    "bijlagen" to listOf(URI("https://example.com/documenten/$bijlageUuid"))
                )
            )
            val inboxProductaanvraagSlot = slot<InboxProductaanvraag>()
            val pdfInboxDocument = createInboxDocument(id = 1L)
            val bijlageInboxDocument = createInboxDocument(id = 2L)
            every { productaanvraagClaimRepository.claim(productaanvraagObjectUuid) } returns true
            every { objectsClientService.readObject(productaanvraagObjectUuid) } returns productaanvraagObject
            every {
                zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype(productaanvraagType)
            } returns emptyList()
            every { inboxProductaanvraagService.create(capture(inboxProductaanvraagSlot)) } just runs
            every { inboxDocumentService.find(pdfUuid) } returns pdfInboxDocument
            every { inboxDocumentService.find(bijlageUuid) } returns bijlageInboxDocument
            every { inboxDocumentService.deleteIfExists(any<Long>()) } just runs
            every { productaanvraagClaimRepository.markDone(productaanvraagObjectUuid) } just runs

            `when`("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productaanvraagObjectUuid)

                then("an inbox productaanvraag is created with the data of the productaanvraag") {
                    with(inboxProductaanvraagSlot.captured) {
                        productaanvraagObjectUUID shouldBe productaanvraagObjectUuid
                        type shouldBe productaanvraagType
                        ontvangstdatum shouldBe LocalDate.of(2021, 1, 1)
                        initiatorID shouldBe "fakeBsn"
                        aanvraagdocumentUUID shouldBe pdfUuid
                        aantalBijlagen shouldBe 1
                    }
                }

                and(
                    """
                    the inbox productaanvraag is created before its documents are removed from the inbox, so that a
                    failure to create it never leaves those documents deleted without an inbox productaanvraag
                    referring to them, and the productaanvraag is marked as done after that
                    """
                ) {
                    verifyOrder {
                        inboxProductaanvraagService.create(any())
                        inboxDocumentService.deleteIfExists(1L)
                        inboxDocumentService.deleteIfExists(2L)
                        productaanvraagClaimRepository.markDone(productaanvraagObjectUuid)
                    }
                }

                and("no zaak is created") {
                    verify(exactly = 0) {
                        productaanvraagCmmnService.createAndStartZaak(any(), any(), any())
                        productaanvraagBpmnService.createAndStartZaak(any(), any(), any())
                    }
                }
            }
        }

        given(
            """
            a productaanvraag without betrokkenen, PDF and bijlagen, for whose productaanvraagtype no zaaktype
            configuration exists
            """
        ) {
            clearAllMocks()
            val productaanvraagObjectUuid = UUID.randomUUID()
            val productaanvraagObject = createProductaanvraagObject(productaanvraagObjectUuid)
            val inboxProductaanvraagSlot = slot<InboxProductaanvraag>()
            every { productaanvraagClaimRepository.claim(productaanvraagObjectUuid) } returns true
            every { objectsClientService.readObject(productaanvraagObjectUuid) } returns productaanvraagObject
            every {
                zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype(productaanvraagType)
            } returns emptyList()
            every { inboxProductaanvraagService.create(capture(inboxProductaanvraagSlot)) } just runs
            every { productaanvraagClaimRepository.markDone(productaanvraagObjectUuid) } just runs

            `when`("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productaanvraagObjectUuid)

                then("an inbox productaanvraag without initiator, aanvraagdocument and bijlagen is created") {
                    with(inboxProductaanvraagSlot.captured) {
                        initiatorID shouldBe null
                        aanvraagdocumentUUID shouldBe null
                        aantalBijlagen shouldBe 0
                    }
                    verify(exactly = 0) {
                        inboxDocumentService.find(any<UUID>())
                    }
                }
            }
        }

        given(
            """
            a productaanvraag with an aanvraag PDF that is no longer an inbox document, for whose productaanvraagtype
            no zaaktype configuration exists
            """
        ) {
            clearAllMocks()
            val productaanvraagObjectUuid = UUID.randomUUID()
            val pdfUuid = UUID.randomUUID()
            val productaanvraagObject = createProductaanvraagObject(
                productaanvraagObjectUuid = productaanvraagObjectUuid,
                additionalData = mapOf("pdf" to URI("https://example.com/documenten/$pdfUuid"))
            )
            every { productaanvraagClaimRepository.claim(productaanvraagObjectUuid) } returns true
            every { objectsClientService.readObject(productaanvraagObjectUuid) } returns productaanvraagObject
            every {
                zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype(productaanvraagType)
            } returns emptyList()
            every { inboxProductaanvraagService.create(any()) } just runs
            every { inboxDocumentService.find(pdfUuid) } returns null
            every { productaanvraagClaimRepository.markDone(productaanvraagObjectUuid) } just runs

            `when`("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productaanvraagObjectUuid)

                then("the inbox productaanvraag is created and no inbox document is deleted") {
                    verify(exactly = 1) {
                        inboxProductaanvraagService.create(any())
                        productaanvraagClaimRepository.markDone(productaanvraagObjectUuid)
                    }
                    verify(exactly = 0) {
                        inboxDocumentService.deleteIfExists(any<Long>())
                    }
                }
            }
        }

        given(
            """
            a productaanvraag whose productaanvraagtype is only configured on a zaaktype configuration that is not
            bound to a process engine
            """
        ) {
            clearAllMocks()
            val productaanvraagObjectUuid = UUID.randomUUID()
            val productaanvraagObject = createProductaanvraagObject(productaanvraagObjectUuid)
            val unboundZaaktypeConfiguration = createZaaktypeCmmnConfiguration().apply { processBinding = null }
            every { productaanvraagClaimRepository.claim(productaanvraagObjectUuid) } returns true
            every { objectsClientService.readObject(productaanvraagObjectUuid) } returns productaanvraagObject
            every {
                zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype(productaanvraagType)
            } returns listOf(unboundZaaktypeConfiguration)
            every { inboxProductaanvraagService.create(any()) } just runs
            every { productaanvraagClaimRepository.markDone(productaanvraagObjectUuid) } just runs

            `when`("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productaanvraagObjectUuid)

                then(
                    """
                    an inbox productaanvraag is created and the productaanvraag is marked as done, so that a
                    configuration that cannot start a zaak does not lose the productaanvraag
                    """
                ) {
                    verify(exactly = 1) {
                        inboxProductaanvraagService.create(any())
                        productaanvraagClaimRepository.markDone(productaanvraagObjectUuid)
                    }
                    verify(exactly = 0) {
                        productaanvraagCmmnService.createAndStartZaak(any(), any(), any())
                        productaanvraagBpmnService.createAndStartZaak(any(), any(), any())
                    }
                }
            }
        }

        given("an object that misses the aanvraaggegevens required for a productaanvraag-dimpact") {
            clearAllMocks()
            val productaanvraagObjectUuid = UUID.randomUUID()
            val productaanvraagObject = createORObject(
                uuid = productaanvraagObjectUuid,
                record = createObjectRecord(
                    data = mapOf("bron" to createBron(), "type" to productaanvraagType)
                )
            )
            every { productaanvraagClaimRepository.claim(productaanvraagObjectUuid) } returns true
            every { objectsClientService.readObject(productaanvraagObjectUuid) } returns productaanvraagObject

            `when`("the object is handled") {
                productaanvraagService.handleProductaanvraag(productaanvraagObjectUuid)

                then("the object is not handled as a productaanvraag, so no zaak and no inbox productaanvraag are created") {
                    verify(exactly = 0) {
                        zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype(any())
                        productaanvraagCmmnService.createAndStartZaak(any(), any(), any())
                        productaanvraagBpmnService.createAndStartZaak(any(), any(), any())
                        inboxProductaanvraagService.create(any())
                    }
                }
            }
        }

        given("a productaanvraag object that cannot be read from the objects client service") {
            clearAllMocks()
            val productaanvraagObjectUuid = UUID.randomUUID()
            every { productaanvraagClaimRepository.claim(productaanvraagObjectUuid) } returns true
            every { objectsClientService.readObject(productaanvraagObjectUuid) } throws ORRuntimeException("Failed")

            `when`("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productaanvraagObjectUuid)

                then(
                    """
                    no exception is thrown, no zaak and no inbox productaanvraag are created, and the claim is not
                    marked as done, so that the claim timeout reclaims it and the productaanvraag is retried
                    """
                ) {
                    verify(exactly = 0) {
                        productaanvraagCmmnService.createAndStartZaak(any(), any(), any())
                        productaanvraagBpmnService.createAndStartZaak(any(), any(), any())
                        inboxProductaanvraagService.create(any())
                        productaanvraagClaimRepository.markDone(any())
                    }
                }
            }
        }
    }

    context("Productaanvraag that was already claimed for processing") {
        given("a productaanvraag object UUID which is already claimed") {
            clearAllMocks()
            val productaanvraagObjectUuid = UUID.randomUUID()
            every { productaanvraagClaimRepository.claim(productaanvraagObjectUuid) } returns false

            `when`("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productaanvraagObjectUuid)

                then("the productaanvraag object is not even read and no zaak is created") {
                    verify(exactly = 0) {
                        objectsClientService.readObject(any())
                        productaanvraagCmmnService.createAndStartZaak(any(), any(), any())
                        productaanvraagBpmnService.createAndStartZaak(any(), any(), any())
                        inboxProductaanvraagService.create(any())
                    }
                }
            }
        }
    }

    context("Handling a productaanvraag") {
        given(
            """
            a productaanvraag that has not been handled yet, received by the notification endpoint that leaves the
            functionele gebruiker on the session
            """
        ) {
            clearAllMocks()
            val productaanvraagObjectUuid = UUID.randomUUID()
            val productaanvraagObject = createProductaanvraagObject(productaanvraagObjectUuid)
            val httpSession = mockk<HttpSession>()
            val httpSessionInstance = mockk<Instance<HttpSession>>()
            val loggedInUserProvider = LoggedInUserProvider(httpSessionInstance)
            var userWhileHandlingProductaanvraag: LoggedInUser? = null
            every { httpSessionInstance.get() } returns httpSession
            every {
                httpSession.getAttribute(LoggedInUserProvider.LOGGED_IN_USER_SESSION_ATTRIBUTE)
            } returns LoggedInUserProvider.FUNCTIONEEL_GEBRUIKER
            every { productaanvraagClaimRepository.claim(productaanvraagObjectUuid) } returns true
            every { objectsClientService.readObject(productaanvraagObjectUuid) } returns productaanvraagObject
            every {
                zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype(productaanvraagType)
            } answers {
                userWhileHandlingProductaanvraag = loggedInUserProvider.getLoggedInUser()
                error("fakeProductaanvraagHandlingFailure")
            }

            `when`("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productaanvraagObjectUuid)
                val userAfterHandlingProductaanvraag = loggedInUserProvider.getLoggedInUser()

                then("the zaaktype lookup that decides how to create the zaak runs as the productaanvraag user") {
                    userWhileHandlingProductaanvraag shouldBe LoggedInUserProvider.PRODUCTAANVRAAG_GEBRUIKER
                }

                and("a failure while handling does not reach the notification endpoint that triggered it") {
                    verify(exactly = 1) {
                        zaaktypeConfigurationService.listCurrentConfigurationsByProductaanvraagtype(productaanvraagType)
                    }
                }

                and("the session is left as the notification endpoint set it") {
                    userAfterHandlingProductaanvraag shouldBe LoggedInUserProvider.FUNCTIONEEL_GEBRUIKER
                }
            }
        }
    }
})
