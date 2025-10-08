/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.or.`object`.model.createORObject
import net.atos.client.or.`object`.model.createObjectRecord
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.productaanvraag.InboxProductaanvraagService
import net.atos.zac.productaanvraag.model.InboxProductaanvraag
import nl.info.client.kvk.model.createRandomKvkNumber
import nl.info.client.kvk.model.createRandomVestigingsNumber
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobjectForCreatesAndUpdates
import nl.info.client.zgw.model.createZaakobjectProductaanvraag
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.GeometryTypeEnum
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.admin.ZaaktypeCmmnConfigurationBeheerService
import nl.info.zac.admin.model.createBetrokkeneKoppelingen
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createGroup
import nl.info.zac.productaanvraag.model.generated.Geometry
import nl.info.zac.test.util.createRandomStringWithAlphanumericCharacters
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@Suppress("LargeClass")
class ProductaanvraagServiceTest : BehaviorSpec({
    val objectsClientService = mockk<ObjectsClientService>()
    val zgwApiService = mockk<ZGWApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val drcClientService = mockk<DrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val identityService = mockk<IdentityService>()
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val zaaktypeCmmnConfigurationBeheerService = mockk<ZaaktypeCmmnConfigurationBeheerService>()
    val inboxDocumentenService = mockk<InboxDocumentenService>()
    val inboxProductaanvraagService = mockk<InboxProductaanvraagService>()
    val productaanvraagEmailService = mockk<ProductaanvraagEmailService>()
    val cmmnService = mockk<CMMNService>()
    val bpmnService = mockk<BpmnService>()
    val zaaktypeBpmnConfigurationService = mockk<ZaaktypeBpmnConfigurationService>()
    val configuratieService = mockk<ConfiguratieService>()
    val productaanvraagService = ProductaanvraagService(
        objectsClientService = objectsClientService,
        zgwApiService = zgwApiService,
        zrcClientService = zrcClientService,
        drcClientService = drcClientService,
        ztcClientService = ztcClientService,
        identityService = identityService,
        zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
        zaaktypeCmmnConfigurationBeheerService = zaaktypeCmmnConfigurationBeheerService,
        inboxDocumentenService = inboxDocumentenService,
        inboxProductaanvraagService = inboxProductaanvraagService,
        productaanvraagEmailService = productaanvraagEmailService,
        cmmnService = cmmnService,
        bpmnService = bpmnService,
        zaaktypeBpmnConfigurationService = zaaktypeBpmnConfigurationService,
        configuratieService = configuratieService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Get aanvraaggegevens") {
        Given("a productaanvraag-dimpact object with aanvraaggegevens containing form steps with key-value pairs") {
            val type = "productaanvraag"
            val bron = createBron()
            val orObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to bron,
                        "type" to type,
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
            When("the form data is requested from the productaanvraag") {
                val formData = productaanvraagService.getAanvraaggegevens(orObject)

                Then("all key-value pairs in the aanvraaggegevens are returned") {
                    with(formData) {
                        this["fakeKey1"] shouldBe "fakeValue1"
                        this["fakeKey2"] shouldBe "fakeValue2"
                        this["fakeKey3"] shouldBe "fakeValue3"
                    }
                }
            }
        }
    }

    Context("Get productaanvraag") {
        Given("a productaanvraag-dimpact object registration object with zaakgegevens") {
            val type = "productaanvraag"
            val bron = createBron()
            val zaakIdentificatie = "fakeZaakIdentificatie"
            val zaakOmschrijving = "fakeOmschrijving"
            val zaakToelichting = "fakeToelichting"
            val coordinates = listOf(52.08968250760225, 5.114358701512936)
            val orObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to bron,
                        "type" to type,
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
            When("the productaanvraag is requested from the product aanvraag service") {
                val productAanVraagDimpact = productaanvraagService.getProductaanvraag(orObject)

                Then(
                    "the productaanvraag of type 'productaanvraag Dimpact' is returned and contains the expected data"
                ) {
                    with(productAanVraagDimpact) {
                        with(this.bron) {
                            naam shouldBe bron.naam
                            kenmerk shouldBe bron.kenmerk
                        }
                        taal shouldBe "nld"
                        type shouldBe type
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

        Given("a productaanvraag-dimpact object registration object without zaakgegevens") {
            val type = "productaanvraag"
            val bron = createBron()
            val orObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to bron,
                        "type" to type
                    )
                )
            )
            When("the productaanvraag is requested from the product aanvraag service") {
                val productAanVraagDimpact = productaanvraagService.getProductaanvraag(orObject)

                Then(
                    "the productaanvraag of type 'productaanvraag Dimpact' is returned and contains the expected data"
                ) {
                    with(productAanVraagDimpact) {
                        with(this.bron) {
                            naam shouldBe bron.naam
                            kenmerk shouldBe bron.kenmerk
                        }
                        taal shouldBe "nld"
                        type shouldBe type
                        zaakgegevens shouldBe null
                    }
                }
            }
        }
    }

    Context("Handle productaanvraag") {
        Given(
            """
        a productaanvraag-dimpact object registration object for which zaaktypeCmmnConfiguration exist 
        containing zaakgegevens with a point geometry and a betrokkene with role initiator and type BSN 
        as well as a betrokkene with role initiator and type vestiging
        and a zaak description with has the maximum length allowed
        """
        ) {
            val productAanvraagObjectUUID = UUID.randomUUID()
            val zaakTypeUUID = UUID.randomUUID()
            val productAanvraagType = "productaanvraag"
            val zaakDescription = "fakeDescription"
            val zaakExplanation = createRandomStringWithAlphanumericCharacters(1000)
            val zaakType = createZaakType()
            val createdZaak = createZaak()
            val createdZaakobjectProductAanvraag = createZaakobjectProductaanvraag()
            val createdZaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates()
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaakTypeUUID,
            )
            zaaktypeCmmnConfiguration.apply {
                zaaktypeCmmnBetrokkeneParameters = createBetrokkeneKoppelingen(
                    zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration,
                    brpKoppelen = true,
                    kvkKoppelen = true
                )
            }
            val formulierBron = createBron()
            val coordinates = listOf(52.08968250760225, 5.114358701512936)
            val bsnNumber = "fakeBsnNumber"
            val today = LocalDate.now()
            val productAanvraagORObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to formulierBron,
                        "type" to productAanvraagType,
                        // aanvraaggegevens must contain at least one key with a map value
                        "aanvraaggegevens" to mapOf("fakeKey" to mapOf("fakeSubKey" to "fakeValue")),
                        "zaakgegevens" to mapOf(
                            "geometry" to mapOf(
                                "type" to "Point",
                                "coordinates" to coordinates
                            ),
                            "omschrijving" to zaakDescription,
                            "toelichting" to zaakExplanation
                        ),
                        "betrokkenen" to listOf(
                            mapOf(
                                "inpBsn" to bsnNumber,
                                "roltypeOmschrijving" to "Initiator"
                            ),
                            mapOf(
                                "kvkNummer" to "fakeKvkNumber",
                                "rolOmschrijvingGeneriek" to "initiator"
                            )
                        )
                    ),
                    startAt = today
                )
            )
            val rolTypeInitiator = createRolType(
                zaakTypeUri = zaakType.url,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR
            )
            val zaakToBeCreated = slot<Zaak>()
            val roleToBeCreated = slot<Rol<*>>()
            every { objectsClientService.readObject(productAanvraagObjectUUID) } returns productAanvraagORObject
            every {
                zaaktypeCmmnConfigurationBeheerService.findActiveZaaktypeCmmnConfigurationByProductaanvraagtype(
                    productAanvraagType
                )
            } returns listOf(zaaktypeCmmnConfiguration)
            every {
                zaaktypeBpmnConfigurationService.findZaaktypeBpmnConfigurationByProductAanvraagType(productAanvraagType)
            } returns null
            every { ztcClientService.readZaaktype(zaakTypeUUID) } returns zaakType
            every { zgwApiService.createZaak(capture(zaakToBeCreated)) } returns createdZaak
            every { zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID) } returns zaaktypeCmmnConfiguration
            every { zrcClientService.createZaakobject(any()) } returns createdZaakobjectProductAanvraag
            every {
                zrcClientService.createZaakInformatieobject(
                    any(),
                    "Document toegevoegd tijdens het starten van de van de zaak vanuit een product aanvraag"
                )
            } returns createdZaakInformatieobject
            every { cmmnService.startCase(createdZaak, zaakType, zaaktypeCmmnConfiguration, any()) } just Runs
            every { ztcClientService.findRoltypen(any(), "Initiator") } returns listOf(rolTypeInitiator)
            every { zrcClientService.createRol(capture(roleToBeCreated)) } returns mockk()
            every { configuratieService.readBronOrganisatie() } returns "123443210"

            When("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

                Then(
                    """
                    a zaak should be created, an initiator role of type 'natuurlijk persoon' should be created for the zaak
                    and a CMMN case process should be started
                    """
                ) {
                    verify(exactly = 1) {
                        zgwApiService.createZaak(any())
                        zrcClientService.createZaakobject(any())
                        cmmnService.startCase(createdZaak, zaakType, zaaktypeCmmnConfiguration, any())
                    }
                    verify(exactly = 0) {
                        bpmnService.startProcess(any(), any(), any())
                    }
                    with(zaakToBeCreated.captured) {
                        zaaktype shouldBe zaakType.url
                        startdatum shouldBe today
                        communicatiekanaalNaam shouldBe "E-formulier"
                        bronorganisatie shouldBe "123443210"
                        omschrijving shouldBe zaakDescription
                        // the provided zaak explanation should be appended to the default explanation but truncated
                        // to the maximum length allowed
                        toelichting.length shouldBe 1000
                        toelichting shouldBe
                            (
                                "Aangemaakt vanuit ${formulierBron.naam} met kenmerk '${formulierBron.kenmerk}'. " +
                                    zaakExplanation
                                )
                                .take(1000)
                        with(zaakgeometrie) {
                            type shouldBe GeometryTypeEnum.POINT
                            // productaanvraag coordinates have the order [latitude, longitude]
                            // but the ZGW API expects them in the order [longitude, latitude]
                            this.coordinates[0].toDouble() shouldBe coordinates[1]
                            this.coordinates[1].toDouble() shouldBe coordinates[0]
                        }
                    }
                    with(roleToBeCreated.captured) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NATUURLIJK_PERSOON
                        identificatienummer shouldBe bsnNumber
                        roltype shouldBe rolTypeInitiator.url
                        zaak shouldBe createdZaak.url
                    }
                }
            }
        }
        Given(
            """
        a productaanvraag-dimpact object registration object for which zaaktypeCmmnConfiguration exist
        containing a betrokkene with role initiator and type vestiging and zaaktypeCmmnConfiguration
        that have the KVK koppeling disabled
        """
        ) {
            clearAllMocks()
            val productAanvraagObjectUUID = UUID.randomUUID()
            val zaakTypeUUID = UUID.randomUUID()
            val productAanvraagType = "productaanvraag"
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaakTypeUUID,
                zaaktypeCmmnBetrokkeneParameters = createBetrokkeneKoppelingen(
                    brpKoppelen = false,
                    kvkKoppelen = false
                )
            )
            val formulierBron = createBron()
            val vestigingsNummer = createRandomVestigingsNumber()
            val kvkNummer = createRandomKvkNumber()
            val productAanvraagORObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to formulierBron,
                        "type" to productAanvraagType,
                        // aanvraaggegevens must contain at least one key with a map value
                        "aanvraaggegevens" to mapOf("fakeKey" to mapOf("fakeSubKey" to "fakeValue")),
                        "betrokkenen" to listOf(
                            mapOf(
                                "vestigingsNummer" to vestigingsNummer,
                                "kvkNummer" to kvkNummer,
                                "rolOmschrijvingGeneriek" to "initiator"
                            )
                        )
                    )
                )
            )
            every { objectsClientService.readObject(productAanvraagObjectUUID) } returns productAanvraagORObject
            every {
                zaaktypeCmmnConfigurationBeheerService.findActiveZaaktypeCmmnConfigurationByProductaanvraagtype(
                    productAanvraagType
                )
            } returns listOf(zaaktypeCmmnConfiguration)
            every {
                zaaktypeBpmnConfigurationService.findZaaktypeBpmnConfigurationByProductAanvraagType(productAanvraagType)
            } returns null

            When("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

                Then(
                    """
                    no zaak should be created, and a CMMN case process should not be started and a warning should be logged
                    """
                ) {
                    verify(exactly = 0) {
                        zgwApiService.createZaak(any())
                        zrcClientService.createZaakobject(any())
                        cmmnService.startCase(any(), any(), any(), any())
                        bpmnService.startProcess(any(), any(), any())
                    }
                }
            }
        }

        Given(
            """
        a productaanvraag-dimpact object registration object for which zaaktypeCmmnConfiguration exist
        containing a betrokkene with role initiator and type vestiging with an invalid kvk nummer
        and zaaktypeCmmnConfiguration that have the KVK koppeling enabled 
        """
        ) {
            clearAllMocks()
            val productAanvraagObjectUUID = UUID.randomUUID()
            val zaakTypeUUID = UUID.randomUUID()
            val productAanvraagType = "productaanvraag"
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaakTypeUUID,
                zaaktypeCmmnBetrokkeneParameters = createBetrokkeneKoppelingen(
                    brpKoppelen = false,
                    kvkKoppelen = true
                )
            )
            val formulierBron = createBron()
            val invalidKvkNummer = "123456"
            val productAanvraagORObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to formulierBron,
                        "type" to productAanvraagType,
                        // aanvraaggegevens must contain at least one key with a map value
                        "aanvraaggegevens" to mapOf("fakeKey" to mapOf("fakeSubKey" to "fakeValue")),
                        "betrokkenen" to listOf(
                            mapOf(
                                "kvkNummer" to invalidKvkNummer,
                                "rolOmschrijvingGeneriek" to "initiator"
                            )
                        )
                    )
                )
            )
            every { objectsClientService.readObject(productAanvraagObjectUUID) } returns productAanvraagORObject
            every {
                zaaktypeCmmnConfigurationBeheerService.findActiveZaaktypeCmmnConfigurationByProductaanvraagtype(
                    productAanvraagType
                )
            } returns listOf(zaaktypeCmmnConfiguration)
            every {
                zaaktypeBpmnConfigurationService.findZaaktypeBpmnConfigurationByProductAanvraagType(productAanvraagType)
            } returns null

            When("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

                Then(
                    """
                    no zaak should be created and no CMMN case process should be started
                    """
                ) {
                    verify(exactly = 0) {
                        zgwApiService.createZaak(any())
                        zrcClientService.createZaakobject(any())
                        cmmnService.startCase(any(), any(), any(), any())
                        bpmnService.startProcess(any(), any(), any())
                    }
                }
            }
        }

        Given(
            """
        a productaanvraag-dimpact object registration object for which zaaktypeCmmnConfiguration exist
        containing a betrokkene with role initiator and type vestiging and zaaktypeCmmnConfiguration
        that have the BRP koppeling enabled
        """
        ) {
            clearAllMocks()
            val productAanvraagObjectUUID = UUID.randomUUID()
            val zaakTypeUUID = UUID.randomUUID()
            val productAanvraagType = "productaanvraag"
            val zaakType = createZaakType()
            val createdZaak = createZaak()
            val createdZaakobjectProductAanvraag = createZaakobjectProductaanvraag()
            val createdZaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates()
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaakTypeUUID,
            )
            val formulierBron = createBron()
            val vestigingsNummer = createRandomVestigingsNumber()
            val kvkNummer = createRandomKvkNumber()
            val productAanvraagORObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to formulierBron,
                        "type" to productAanvraagType,
                        // aanvraaggegevens must contain at least one key with a map value
                        "aanvraaggegevens" to mapOf("fakeKey" to mapOf("fakeSubKey" to "fakeValue")),
                        "betrokkenen" to listOf(
                            mapOf(
                                "vestigingsNummer" to vestigingsNummer,
                                "kvkNummer" to kvkNummer,
                                "rolOmschrijvingGeneriek" to "initiator"
                            )
                        )
                    )
                )
            )
            val rolType = createRolType(
                zaakTypeUri = zaakType.url,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR
            )
            val zaakToBeCreated = slot<Zaak>()
            val roleToBeCreated = slot<Rol<*>>()
            every { objectsClientService.readObject(productAanvraagObjectUUID) } returns productAanvraagORObject
            every {
                zaaktypeCmmnConfigurationBeheerService.findActiveZaaktypeCmmnConfigurationByProductaanvraagtype(
                    productAanvraagType
                )
            } returns listOf(zaaktypeCmmnConfiguration)
            every {
                zaaktypeBpmnConfigurationService.findZaaktypeBpmnConfigurationByProductAanvraagType(productAanvraagType)
            } returns null
            every { ztcClientService.readZaaktype(zaakTypeUUID) } returns zaakType
            every { zgwApiService.createZaak(capture(zaakToBeCreated)) } returns createdZaak
            every { zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID) } returns zaaktypeCmmnConfiguration
            every { zrcClientService.createZaakobject(any()) } returns createdZaakobjectProductAanvraag
            every {
                zrcClientService.createZaakInformatieobject(
                    any(),
                    "Document toegevoegd tijdens het starten van de van de zaak vanuit een product aanvraag"
                )
            } returns createdZaakInformatieobject
            every { cmmnService.startCase(createdZaak, zaakType, zaaktypeCmmnConfiguration, any()) } just Runs
            every { ztcClientService.findRoltypen(any(), OmschrijvingGeneriekEnum.INITIATOR) } returns listOf(rolType)
            every { zrcClientService.createRol(capture(roleToBeCreated)) } returns mockk()
            every { configuratieService.readBronOrganisatie() } returns "123443210"

            When("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

                Then(
                    """
                    a zaak and a zaak object should be created
                    """
                ) {
                    verify(exactly = 1) {
                        zgwApiService.createZaak(any())
                        zrcClientService.createZaakobject(any())
                    }
                    verify(exactly = 0) {
                        bpmnService.startProcess(any(), any(), any())
                    }
                    with(zaakToBeCreated.captured) {
                        zaaktype shouldBe zaakType.url
                        communicatiekanaalNaam shouldBe "E-formulier"
                        bronorganisatie shouldBe "123443210"
                        omschrijving shouldBe null
                        toelichting shouldBe "Aangemaakt vanuit ${formulierBron.naam} met kenmerk '${formulierBron.kenmerk}'."
                    }
                }
                And("a CMMN process should be started for the zaak") {
                    verify(exactly = 1) {
                        cmmnService.startCase(createdZaak, zaakType, zaaktypeCmmnConfiguration, any())
                    }
                }
                And("an initiator role of type 'niet-natuurlijk persoon' should be created and linked to the zaak") {
                    with(roleToBeCreated.captured) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON
                        identificatienummer shouldBe vestigingsNummer
                        roltype shouldBe rolType.url
                        zaak shouldBe createdZaak.url
                    }
                }
            }
        }

        Given(
            """
        a productaanvraag-dimpact object registration object for which zaaktypeCmmnConfiguration exist
        containing a betrokkene with role initiator but no supported initiator identification
        """
        ) {
            clearAllMocks()
            val productAanvraagObjectUUID = UUID.randomUUID()
            val zaakTypeUUID = UUID.randomUUID()
            val productAanvraagType = "productaanvraag"
            val zaakType = createZaakType()
            val createdZaak = createZaak()
            val createdZaakobjectProductAanvraag = createZaakobjectProductaanvraag()
            val createdZaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates()
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaakTypeUUID
            )
            val formulierBron = createBron()
            val productAanvraagORObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to formulierBron,
                        "type" to productAanvraagType,
                        // aanvraaggegevens must contain at least one key with a map value
                        "aanvraaggegevens" to mapOf("fakeKey" to mapOf("fakeSubKey" to "fakeValue")),
                        "betrokkenen" to listOf(
                            mapOf(
                                "unsupportedIdentification" to "1234",
                                "rolOmschrijvingGeneriek" to "initiator"
                            )
                        )
                    )
                )
            )
            val zaakToBeCreated = slot<Zaak>()
            every { objectsClientService.readObject(productAanvraagObjectUUID) } returns productAanvraagORObject
            every {
                zaaktypeCmmnConfigurationBeheerService.findActiveZaaktypeCmmnConfigurationByProductaanvraagtype(
                    productAanvraagType
                )
            } returns listOf(zaaktypeCmmnConfiguration)
            every {
                zaaktypeBpmnConfigurationService.findZaaktypeBpmnConfigurationByProductAanvraagType(productAanvraagType)
            } returns null
            every { ztcClientService.readZaaktype(zaakTypeUUID) } returns zaakType
            every { zgwApiService.createZaak(capture(zaakToBeCreated)) } returns createdZaak
            every { zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID) } returns zaaktypeCmmnConfiguration
            every { zrcClientService.createZaakobject(any()) } returns createdZaakobjectProductAanvraag
            every {
                zrcClientService.createZaakInformatieobject(
                    any(),
                    "Document toegevoegd tijdens het starten van de van de zaak vanuit een product aanvraag"
                )
            } returns createdZaakInformatieobject
            every { cmmnService.startCase(createdZaak, zaakType, zaaktypeCmmnConfiguration, any()) } just Runs
            every { ztcClientService.findRoltypen(any(), OmschrijvingGeneriekEnum.INITIATOR) } returns emptyList()
            every { configuratieService.readBronOrganisatie() } returns "123443210"

            When("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

                Then(
                    """
                    a zaak should be created, no initiator role should be created for the zaak
                    and a CMMN case process should be started
                    """
                ) {
                    verify(exactly = 1) {
                        zgwApiService.createZaak(any())
                        zrcClientService.createZaakobject(any())
                        cmmnService.startCase(createdZaak, zaakType, zaaktypeCmmnConfiguration, any())
                    }
                    verify(exactly = 0) {
                        zrcClientService.createRol(any())
                        bpmnService.startProcess(any(), any(), any())
                    }
                    with(zaakToBeCreated.captured) {
                        zaaktype shouldBe zaakType.url
                        communicatiekanaalNaam shouldBe "E-formulier"
                        bronorganisatie shouldBe "123443210"
                        omschrijving shouldBe null
                        toelichting shouldBe "Aangemaakt vanuit ${formulierBron.naam} met kenmerk '${formulierBron.kenmerk}'."
                    }
                }
            }
        }

        Given(
            """
        a productaanvraag-dimpact object registration object for which zaaktypeCmmnConfiguration exist 
        not containing any betrokkenen
        """
        ) {
            clearAllMocks()
            val productAanvraagObjectUUID = UUID.randomUUID()
            val zaakTypeUUID = UUID.randomUUID()
            val productAanvraagType = "productaanvraag"
            val zaakType = createZaakType()
            val createdZaak = createZaak()
            val createdZaakobjectProductAanvraag = createZaakobjectProductaanvraag()
            val createdZaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates()
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaakTypeUUID
            )
            val formulierBron = createBron()
            val productAanvraagORObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to formulierBron,
                        "type" to productAanvraagType,
                        // aanvraaggegevens must contain at least one key with a map value
                        "aanvraaggegevens" to mapOf("fakeKey" to mapOf("fakeSubKey" to "fakeValue"))
                    )
                )
            )
            val zaakToBeCreated = slot<Zaak>()
            every { objectsClientService.readObject(productAanvraagObjectUUID) } returns productAanvraagORObject
            every {
                zaaktypeCmmnConfigurationBeheerService.findActiveZaaktypeCmmnConfigurationByProductaanvraagtype(
                    productAanvraagType
                )
            } returns listOf(zaaktypeCmmnConfiguration)
            every {
                zaaktypeBpmnConfigurationService.findZaaktypeBpmnConfigurationByProductAanvraagType(productAanvraagType)
            } returns null
            every { ztcClientService.readZaaktype(zaakTypeUUID) } returns zaakType
            every { zgwApiService.createZaak(capture(zaakToBeCreated)) } returns createdZaak
            every { zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID) } returns zaaktypeCmmnConfiguration
            every { zrcClientService.createZaakobject(any()) } returns createdZaakobjectProductAanvraag
            every {
                zrcClientService.createZaakInformatieobject(
                    any(),
                    "Document toegevoegd tijdens het starten van de van de zaak vanuit een product aanvraag"
                )
            } returns createdZaakInformatieobject
            every { cmmnService.startCase(createdZaak, zaakType, zaaktypeCmmnConfiguration, any()) } just Runs
            every { configuratieService.readBronOrganisatie() } returns "123443210"

            When("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

                Then(
                    """
                    a zaak should be created and a CMMN case process should be started
                    """
                ) {
                    verify(exactly = 1) {
                        zgwApiService.createZaak(any())
                        zrcClientService.createZaakobject(any())
                        cmmnService.startCase(createdZaak, zaakType, zaaktypeCmmnConfiguration, any())
                    }
                    verify(exactly = 0) {
                        zrcClientService.createRol(any())
                        bpmnService.startProcess(any(), any(), any())
                    }
                    with(zaakToBeCreated.captured) {
                        zaaktype shouldBe zaakType.url
                        communicatiekanaalNaam shouldBe "E-formulier"
                        bronorganisatie shouldBe "123443210"
                        omschrijving shouldBe null
                        toelichting shouldBe "Aangemaakt vanuit ${formulierBron.naam} met kenmerk '${formulierBron.kenmerk}'."
                    }
                }
            }
        }

        Given(
            """
        a productaanvraag-dimpact object registration object for which zaaktypeCmmnConfiguration exist
        containing a list of supported betrokkenen including behandelaar but no initiator
        """
        ) {
            clearAllMocks()
            val productAanvraagObjectUUID = UUID.randomUUID()
            val zaakTypeUUID = UUID.randomUUID()
            val productAanvraagType = "productaanvraag"
            val zaakType = createZaakType()
            val createdZaak = createZaak()
            val createdZaakobjectProductAanvraag = createZaakobjectProductaanvraag()
            val createdZaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates()
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaakTypeUUID
            )
            val formulierBron = createBron()
            val adviseurBsn1 = "fakeBsn1"
            val behandelaarBsn = "fakeBsn3"
            val beslisserBsn = "fakeBsn4"
            val klantcontacterBsn = "fakeBsn5"
            val medeInitiatorBsn = "fakeBsn6"
            val belanghebbendeKvkNummer1 = createRandomKvkNumber()
            val belanghebbendeKvkNummer2 = createRandomKvkNumber()
            val beslisserKvkNummer = createRandomKvkNumber()
            val zaakcoordinatorKvkNummer = createRandomKvkNumber()
            val rolTypeBelanghebbende = createRolType(
                zaakTypeUri = zaakType.url,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BELANGHEBBENDE
            )
            val rolTypeBeslisser = createRolType(
                zaakTypeUri = zaakType.url,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BESLISSER
            )
            val rolTypeKlantcontacter1 = createRolType(
                zaakTypeUri = zaakType.url,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.KLANTCONTACTER
            )
            val rolTypeKlantcontacter2 = createRolType(
                zaakTypeUri = zaakType.url,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.KLANTCONTACTER
            )
            val rolTypeMedeInitiator = createRolType(
                zaakTypeUri = zaakType.url,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.MEDE_INITIATOR
            )
            val rolTypeZaakcoordinator = createRolType(
                zaakTypeUri = zaakType.url,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.ZAAKCOORDINATOR
            )
            val productAanvraagORObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to formulierBron,
                        "type" to productAanvraagType,
                        // aanvraaggegevens must contain at least one key with a map value
                        "aanvraaggegevens" to mapOf("fakeKey" to mapOf("fakeSubKey" to "fakeValue")),
                        "betrokkenen" to listOf(
                            mapOf(
                                "inpBsn" to adviseurBsn1,
                                "rolOmschrijvingGeneriek" to "adviseur"
                            ),
                            mapOf(
                                "inpBsn" to behandelaarBsn,
                                "rolOmschrijvingGeneriek" to "behandelaar"
                            ),
                            mapOf(
                                "inpBsn" to behandelaarBsn,
                                "roltypeOmschrijving" to "Behandelaar"
                            ),
                            mapOf(
                                "kvkNummer" to belanghebbendeKvkNummer1,
                                "roltypeOmschrijving" to "Belanghebbende"
                            ),
                            mapOf(
                                "kvkNummer" to belanghebbendeKvkNummer2,
                                "roltypeOmschrijving" to "Belanghebbende"
                            ),
                            mapOf(
                                "inpBsn" to beslisserBsn,
                                "rolOmschrijvingGeneriek" to "beslisser"
                            ),
                            mapOf(
                                "kvkNummer" to beslisserKvkNummer,
                                "rolOmschrijvingGeneriek" to "beslisser"
                            ),
                            mapOf(
                                "inpBsn" to klantcontacterBsn,
                                "rolOmschrijvingGeneriek" to "klantcontacter"
                            ),
                            mapOf(
                                "inpBsn" to medeInitiatorBsn,
                                "roltypeOmschrijving" to "Medeaanvrager",
                                "rolOmschrijvingGeneriek" to "mede_initiator"
                            ),
                            mapOf(
                                "kvkNummer" to zaakcoordinatorKvkNummer,
                                "rolOmschrijvingGeneriek" to "zaakcoordinator"
                            )
                        )
                    )
                )
            )
            val rolesToBeCreated = mutableListOf<Rol<*>>()
            val zaakToBeCreated = slot<Zaak>()
            every { objectsClientService.readObject(productAanvraagObjectUUID) } returns productAanvraagORObject
            every {
                zaaktypeCmmnConfigurationBeheerService.findActiveZaaktypeCmmnConfigurationByProductaanvraagtype(
                    productAanvraagType
                )
            } returns listOf(zaaktypeCmmnConfiguration)
            every {
                zaaktypeBpmnConfigurationService.findZaaktypeBpmnConfigurationByProductAanvraagType(productAanvraagType)
            } returns null

            When("it is not allowed to add a betrokkene") {
                val invalidZaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
                val zaaktypeCmmnConfigurationNonAllowedBetrokkene = invalidZaaktypeCmmnConfiguration.apply {
                    zaaktypeCmmnBetrokkeneParameters = createBetrokkeneKoppelingen(
                        brpKoppelen = false,
                        zaaktypeCmmnConfiguration = invalidZaaktypeCmmnConfiguration
                    )
                }

                every {
                    zaaktypeCmmnConfigurationBeheerService.findActiveZaaktypeCmmnConfigurationByProductaanvraagtype(
                        productAanvraagType
                    )
                } returns listOf(zaaktypeCmmnConfigurationNonAllowedBetrokkene)

                productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

                Then("not create the zaak not related objects and neither start the zaak") {
                    verify(exactly = 0) {
                        zgwApiService.createZaak(any())
                        zrcClientService.createZaakobject(any())
                        zrcClientService.createRol(any())
                        cmmnService.startCase(createdZaak, zaakType, zaaktypeCmmnConfiguration, any())
                        bpmnService.startProcess(any(), any(), any())
                    }
                }
            }

            When("the productaanvraag is handled") {
                every { ztcClientService.readZaaktype(zaakTypeUUID) } returns zaakType
                // here we simulate the case that no role types have been defined for the adviseur role
                every { ztcClientService.findRoltypen(any(), OmschrijvingGeneriekEnum.ADVISEUR) } returns emptyList()
                every {
                    ztcClientService.findRoltypen(any(), "Belanghebbende")
                } returns listOf(rolTypeBelanghebbende)
                every {
                    ztcClientService.findRoltypen(any(), OmschrijvingGeneriekEnum.BESLISSER)
                } returns listOf(rolTypeBeslisser)
                // here we simulate the case that multiple role types have been defined for the klantcontacter role
                every {
                    ztcClientService.findRoltypen(any(), OmschrijvingGeneriekEnum.KLANTCONTACTER)
                } returns listOf(rolTypeKlantcontacter1, rolTypeKlantcontacter2)
                every {
                    ztcClientService.findRoltypen(any(), "Medeaanvrager")
                } returns listOf(rolTypeMedeInitiator)
                every {
                    ztcClientService.findRoltypen(any(), OmschrijvingGeneriekEnum.ZAAKCOORDINATOR)
                } returns listOf(rolTypeZaakcoordinator)
                every { zrcClientService.createRol(capture(rolesToBeCreated)) } returns mockk()
                every { zgwApiService.createZaak(capture(zaakToBeCreated)) } returns createdZaak
                every {
                    zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID)
                } returns zaaktypeCmmnConfiguration
                every { zrcClientService.createZaakobject(any()) } returns createdZaakobjectProductAanvraag
                every {
                    zrcClientService.createZaakInformatieobject(
                        any(),
                        "Document toegevoegd tijdens het starten van de van de zaak vanuit een product aanvraag"
                    )
                } returns createdZaakInformatieobject
                every { cmmnService.startCase(createdZaak, zaakType, zaaktypeCmmnConfiguration, any()) } just Runs
                every { configuratieService.readBronOrganisatie() } returns "123443210"
                every {
                    zaaktypeCmmnConfigurationBeheerService.findActiveZaaktypeCmmnConfigurationByProductaanvraagtype(
                        productAanvraagType
                    )
                } returns listOf(zaaktypeCmmnConfiguration)

                productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

                Then("a zaak should be created") {
                    verify(exactly = 1) {
                        zgwApiService.createZaak(any())
                        zrcClientService.createZaakobject(any())
                    }
                    verify(exactly = 0) {
                        bpmnService.startProcess(any(), any(), any())
                    }
                    with(zaakToBeCreated.captured) {
                        zaaktype shouldBe zaakType.url
                        communicatiekanaalNaam shouldBe "E-formulier"
                        bronorganisatie shouldBe "123443210"
                        omschrijving shouldBe null
                        toelichting shouldBe "Aangemaakt vanuit ${formulierBron.naam} met kenmerk '${formulierBron.kenmerk}'."
                    }
                }
                And("and a CMMN process should be started for the zaak") {
                    verify(exactly = 1) {
                        cmmnService.startCase(createdZaak, zaakType, zaaktypeCmmnConfiguration, any())
                    }
                }
                And(
                    """
                    roles should be created and linked to the zaak for all supported betrokkenen types for which
                    there are role types defined in the ZTC client service, except for the behandelaar betrokkene
                """
                ) {
                    verify(exactly = 7) {
                        zrcClientService.createRol(any())
                    }
                    rolesToBeCreated.forEach {
                        it.roltoelichting shouldBe "Overgenomen vanuit de product aanvraag"
                        it.zaak shouldBe createdZaak.url
                    }
                    with(rolesToBeCreated[0]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON
                        identificatienummer shouldBe belanghebbendeKvkNummer1
                        roltype shouldBe rolTypeBelanghebbende.url
                    }
                    with(rolesToBeCreated[1]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON
                        identificatienummer shouldBe belanghebbendeKvkNummer2
                        roltype shouldBe rolTypeBelanghebbende.url
                    }
                    with(rolesToBeCreated[2]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NATUURLIJK_PERSOON
                        identificatienummer shouldBe beslisserBsn
                        roltype shouldBe rolTypeBeslisser.url
                    }
                    with(rolesToBeCreated[3]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON
                        identificatienummer shouldBe beslisserKvkNummer
                        roltype shouldBe rolTypeBeslisser.url
                    }
                    with(rolesToBeCreated[4]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NATUURLIJK_PERSOON
                        identificatienummer shouldBe klantcontacterBsn
                        roltype shouldBe rolTypeKlantcontacter1.url
                    }
                    with(rolesToBeCreated[5]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NATUURLIJK_PERSOON
                        identificatienummer shouldBe medeInitiatorBsn
                        roltype shouldBe rolTypeMedeInitiator.url
                    }
                    with(rolesToBeCreated[6]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON
                        identificatienummer shouldBe zaakcoordinatorKvkNummer
                        roltype shouldBe rolTypeZaakcoordinator.url
                    }
                }
            }
        }

        Given(
            """
        A productaanvraag-dimpact object registration object for which zaaktypeCmmnConfiguration exist
        with a default group, but an exception occurs when adding a zaakinformatieobject
        """
        ) {
            clearAllMocks()
            val productAanvraagObjectUUID = UUID.randomUUID()
            val zaakTypeUUID = UUID.randomUUID()
            val productAanvraagType = "productaanvraag"
            val zaakType = createZaakType()
            val createdZaak = createZaak()
            val createdZaakobjectProductAanvraag = createZaakobjectProductaanvraag()
            val groupId = "fakeGroupId"
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeUUID = zaakTypeUUID,
                groupId = groupId
            )
            val group = createGroup(
                id = groupId,
                name = "Fake Group",
            )
            val behandelaarRolType = createRolType(
                zaakTypeUri = zaakType.url,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR
            )
            val formulierBron = createBron()
            val productAanvraagORObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to formulierBron,
                        "type" to productAanvraagType,
                        // aanvraaggegevens must contain at least one key with a map value
                        "aanvraaggegevens" to mapOf("fakeKey" to mapOf("fakeSubKey" to "fakeValue"))
                    )
                )
            )
            every { objectsClientService.readObject(productAanvraagObjectUUID) } returns productAanvraagORObject
            every {
                zaaktypeCmmnConfigurationBeheerService.findActiveZaaktypeCmmnConfigurationByProductaanvraagtype(
                    productAanvraagType
                )
            } returns listOf(zaaktypeCmmnConfiguration)
            every {
                zaaktypeBpmnConfigurationService.findZaaktypeBpmnConfigurationByProductAanvraagType(productAanvraagType)
            } returns null
            every { ztcClientService.readZaaktype(zaakTypeUUID) } returns zaakType
            every { zgwApiService.createZaak(any()) } returns createdZaak
            every { zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID) } returns zaaktypeCmmnConfiguration
            every { zrcClientService.createZaakobject(any()) } returns createdZaakobjectProductAanvraag
            every {
                zrcClientService.createZaakInformatieobject(any(), any())
            } throws RuntimeException("Failed to create zaakinformatieobject!")
            every { cmmnService.startCase(createdZaak, zaakType, zaaktypeCmmnConfiguration, any()) } just Runs
            every { configuratieService.readBronOrganisatie() } returns "123443210"
            every { identityService.readGroup(groupId) } returns group
            every {
                ztcClientService.readRoltype(createdZaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR)
            } returns behandelaarRolType
            every { zrcClientService.createRol(any<RolOrganisatorischeEenheid>()) } returns createRolOrganisatorischeEenheid()

            When("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

                Then("a zaak should be created") {
                    verify(exactly = 1) {
                        zgwApiService.createZaak(any())
                    }
                }
                And("a CMMN process should be started for the zaak") {
                    verify(exactly = 1) {
                        cmmnService.startCase(createdZaak, zaakType, zaaktypeCmmnConfiguration, any())
                    }
                }
                And("a zaak object should be created") {
                    verify(exactly = 1) {
                        zrcClientService.createZaakobject(any())
                    }
                }
                And("the zaak should be assigned to the group") {
                    verify(exactly = 1) {
                        zrcClientService.createRol(any<RolOrganisatorischeEenheid>())
                    }
                }
                And("no automatic reply email should be sent (this behaviour may change in the future)") {
                    verify(exactly = 0) {
                        productaanvraagEmailService.sendEmailForZaakFromProductaanvraag(
                            any(),
                            any(),
                            any()
                        )
                    }
                }
                And("BPMN process should not be started") {
                    verify(exactly = 0) { bpmnService.startProcess(any(), any(), any()) }
                }
            }
        }

        Given("a productaanvraag-dimpact object registration object missing required aanvraaggegevens") {
            clearAllMocks()
            val productAanvraagObjectUUID = UUID.randomUUID()
            val productAanvraagType = "productaanvraag"
            val formulierBron = createBron()
            val productAanvraagORObjectWithMissingAanvraaggegevens = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to formulierBron,
                        "type" to productAanvraagType
                    )
                )
            )
            every {
                objectsClientService.readObject(productAanvraagObjectUUID)
            } returns productAanvraagORObjectWithMissingAanvraaggegevens

            When("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

                Then(
                    """
                an inbox productaanvraag should be created and a zaak should not be created, 
                and no CMMN should be started
                """
                ) {
                    verify(exactly = 0) {
                        zgwApiService.createZaak(any())
                        zrcClientService.createZaakobject(any())
                        cmmnService.startCase(any(), any(), any(), any())
                        bpmnService.startProcess(any(), any(), any())
                    }
                }
            }
        }

        Given(
            """
        a productaanvraag-dimpact object registration object containing required data but
        no betrokkenen and which contains a zaaktype for which no zaaktypeCmmnConfiguration are configured 
        and for which no zaaktype exists in the ZTC catalogus
        """
        ) {
            clearAllMocks()
            val productAanvraagObjectUUID = UUID.randomUUID()
            val productAanvraagType = "productaanvraag"
            val zaakType = createZaakType()
            val createdZaak = createZaak()
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
            val formulierBron = createBron()
            val productAanvraagORObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to formulierBron,
                        "type" to productAanvraagType,
                        // aanvraaggegevens must contain at least one key with a map value
                        "aanvraaggegevens" to mapOf("fakeKey" to mapOf("fakeSubKey" to "fakeValue"))
                    )
                ),
                uuid = productAanvraagObjectUUID
            )
            val inboxProductaanvraagSlot = slot<InboxProductaanvraag>()
            every { objectsClientService.readObject(productAanvraagObjectUUID) } returns productAanvraagORObject
            // no zaaktypeCmmnConfiguration are configured for the zaaktype
            every {
                zaaktypeCmmnConfigurationBeheerService.findActiveZaaktypeCmmnConfigurationByProductaanvraagtype(
                    productAanvraagType
                )
            } returns emptyList()
            // BPMN not configured
            every {
                zaaktypeBpmnConfigurationService.findZaaktypeBpmnConfigurationByProductAanvraagType(productAanvraagType)
            } returns null
            every { inboxProductaanvraagService.create(capture(inboxProductaanvraagSlot)) } just runs

            When("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

                Then(
                    """
                an inbox productaanvraag should be created, 
                no zaak should be created and no CMMN process should be started
                """
                ) {
                    verify(exactly = 1) {
                        inboxProductaanvraagService.create(any())
                    }
                    verify(exactly = 0) {
                        zgwApiService.createZaak(any())
                        zrcClientService.createZaakobject(any())
                        cmmnService.startCase(createdZaak, zaakType, zaaktypeCmmnConfiguration, any())
                        bpmnService.startProcess(any(), any(), any())
                    }
                    inboxProductaanvraagSlot.captured.run {
                        productaanvraagObjectUUID shouldBe productAanvraagObjectUUID
                        aanvraagdocumentUUID shouldBe null
                        ontvangstdatum shouldBe null
                        type shouldBe productAanvraagType
                        initiatorID shouldBe null
                        aantalBijlagen shouldBe 0
                    }
                }
            }
        }

        Given(
            """
        A productaanvraag-dimpact object that cannot be read from the objects client service
        """
        ) {
            clearAllMocks()
            val productAanvraagObjectUUID = UUID.randomUUID()
            every { objectsClientService.readObject(productAanvraagObjectUUID) } throws RuntimeException("Failed")

            When("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

                Then(
                    """
                no exception is thrown, and no further actions are taken
                """
                ) {
                    verify(exactly = 0) {
                        inboxProductaanvraagService.create(any())
                        zgwApiService.createZaak(any())
                        zrcClientService.createZaakobject(any())
                        cmmnService.startCase(any(), any(), any(), any())
                        bpmnService.startProcess(any(), any(), any())
                    }
                }
            }
        }

        Given(
            """
        A productaanvraag-dimpact object registration object with an initiator betrokkene,
        no matching zaaktypeCmmnConfiguration,
        but a BPMN definition for the productaanvraagtype
        """
        ) {
            clearAllMocks()
            val productAanvraagObjectUUID = UUID.randomUUID()
            val productAanvraagType = "productaanvraag"
            val zaakTypeUUID = UUID.randomUUID()
            val zaakType = createZaakType()
            val createdZaak = createZaak()
            val createdZaakobjectProductAanvraag = createZaakobjectProductaanvraag()
            val createdZaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates()
            val formulierBron = createBron()
            val groupName = "fakeGroup"
            val group = createGroup(
                id = groupName,
                name = "Fake Group",
            )
            val behandelaarRolType = createRolType(
                zaakTypeUri = zaakType.url,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR
            )
            val bsnNumber = "123456789"
            val productAanvraagORObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to formulierBron,
                        "type" to productAanvraagType,
                        "aanvraaggegevens" to mapOf("fakeKey" to mapOf("fakeSubKey" to "fakeValue")),
                        "betrokkenen" to listOf(
                            mapOf(
                                "inpBsn" to bsnNumber,
                                "roltypeOmschrijving" to "Initiator"
                            ),
                        )
                    )
                )
            )
            val bpmnDefinition = createZaaktypeBpmnConfiguration(
                zaaktypeUuid = zaakTypeUUID,
                groupName = groupName,
                bpmnProcessDefinitionKey = "fakeBpmnProcessKey"

            )
            val zaakDataSlot = slot<Map<String, Any>>()
            val rolTypeInitiator = createRolType(
                zaakTypeUri = zaakType.url,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR
            )
            every { objectsClientService.readObject(productAanvraagObjectUUID) } returns productAanvraagORObject
            every {
                zaaktypeCmmnConfigurationBeheerService.findActiveZaaktypeCmmnConfigurationByProductaanvraagtype(
                    productAanvraagType
                )
            } returns emptyList()
            every {
                zaaktypeBpmnConfigurationService.findZaaktypeBpmnConfigurationByProductAanvraagType(productAanvraagType)
            } returns bpmnDefinition
            every { ztcClientService.readZaaktype(zaakTypeUUID) } returns zaakType
            every { zgwApiService.createZaak(any()) } returns createdZaak
            every { zrcClientService.createZaakobject(any()) } returns createdZaakobjectProductAanvraag
            every {
                zrcClientService.createZaakInformatieobject(
                    any(),
                    "Document toegevoegd tijdens het starten van de van de zaak vanuit een product aanvraag"
                )
            } returns createdZaakInformatieobject
            every { bpmnService.startProcess(createdZaak, zaakType, "fakeBpmnProcessKey", capture(zaakDataSlot)) } just Runs
            every { configuratieService.readBronOrganisatie() } returns "123443210"
            every { identityService.readGroup(groupName) } returns group
            every {
                ztcClientService.readRoltype(createdZaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR)
            } returns behandelaarRolType
            every { zrcClientService.createRol(any<RolOrganisatorischeEenheid>()) } returns createRolOrganisatorischeEenheid()

            When("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

                Then(" A zaak should be created and a BPMN process should be started") {
                    verify(exactly = 1) {
                        zgwApiService.createZaak(any())
                        bpmnService.startProcess(createdZaak, zaakType, "fakeBpmnProcessKey", any())
                    }
                    with(zaakDataSlot.captured) {
                        size shouldBe 1
                        values.first() shouldBe groupName
                    }
                }
                And("and the productaanvraag and documents should be paired") {
                    verify(exactly = 1) {
                        zrcClientService.createZaakobject(any())
                        zrcClientService.createZaakInformatieobject(any(), any())
                    }
                }
                And("the group role should be created for the assigned group") {
                    verify(exactly = 1) {
                        zrcClientService.createRol(any())
                    }
                }
                And("no CMMN process be started") {
                    verify(exactly = 0) {
                        cmmnService.startCase(any(), any(), any(), any())
                    }
                }
                And("no email notification should be sent") {
                    verify(exactly = 0) {
                        productaanvraagEmailService.sendEmailForZaakFromProductaanvraag(any(), any(), any())
                    }
                }
            }
        }

        Given(
            """
        A productaanvraag-dimpact object where both a CMMN mapping and a BPMN mapping exist for the productaanvraagtype
        """
        ) {
            clearAllMocks()
            val productAanvraagObjectUUID = UUID.randomUUID()
            val productAanvraagType = "productaanvraag"
            val zaakTypeUUID = UUID.randomUUID()
            val zaakType = createZaakType()
            val createdZaak = createZaak()
            val createdZaakobjectProductAanvraag = createZaakobjectProductaanvraag()
            val createdZaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates()
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(zaaktypeUUID = zaakTypeUUID)
            val bpmnDefinition = createZaaktypeBpmnConfiguration()
            val formulierBron = createBron()
            val productAanvraagORObject = createORObject(
                record = createObjectRecord(
                    data = mapOf(
                        "bron" to formulierBron,
                        "type" to productAanvraagType,
                        "aanvraaggegevens" to mapOf("fakeKey" to mapOf("fakeSubKey" to "fakeValue"))
                    )
                )
            )

            every { objectsClientService.readObject(productAanvraagObjectUUID) } returns productAanvraagORObject
            every {
                zaaktypeCmmnConfigurationBeheerService.findActiveZaaktypeCmmnConfigurationByProductaanvraagtype(
                    productAanvraagType
                )
            } returns listOf(zaaktypeCmmnConfiguration)
            every {
                zaaktypeBpmnConfigurationService.findZaaktypeBpmnConfigurationByProductAanvraagType(productAanvraagType)
            } returns bpmnDefinition
            every { ztcClientService.readZaaktype(zaakTypeUUID) } returns zaakType
            every { zgwApiService.createZaak(any()) } returns createdZaak
            every { zrcClientService.createZaakobject(any()) } returns createdZaakobjectProductAanvraag
            every { zrcClientService.createZaakInformatieobject(any(), any()) } returns createdZaakInformatieobject
            every { zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID) } returns zaaktypeCmmnConfiguration
            every { cmmnService.startCase(createdZaak, zaakType, zaaktypeCmmnConfiguration, any()) } just Runs
            every { configuratieService.readBronOrganisatie() } returns "123443210"

            When("the productaanvraag is handled") {
                productaanvraagService.handleProductaanvraag(productAanvraagObjectUUID)

                Then("CMMN should win: a CMMN process is started and BPMN is not started") {
                    verify(exactly = 1) {
                        zgwApiService.createZaak(any())
                        zrcClientService.createZaakobject(any())
                        cmmnService.startCase(createdZaak, zaakType, zaaktypeCmmnConfiguration, any())
                    }
                    verify(exactly = 0) {
                        bpmnService.startProcess(any(), any(), any())
                    }
                }
            }
        }
    }

    Context("Pair bijlagen with zaak") {
        Given("a list of bijlage URIs and a zaak URI") {
            val bijlageURIs = listOf(URI("fakeURI1"), URI("fakeURI2"))
            val enkelvoudigInformatieobjecten = listOf(
                createEnkelvoudigInformatieObject(),
                createEnkelvoudigInformatieObject()
            )
            val zaakInformatieobjecten = listOf(
                createZaakInformatieobjectForCreatesAndUpdates(),
                createZaakInformatieobjectForCreatesAndUpdates()
            )
            val zaakUrl = URI("fakeZaakUrl")
            val createdZaakInformatieobjectSlot = slot<ZaakInformatieobject>()
            val beschrijving = "Document toegevoegd tijdens het starten van de van de zaak vanuit een product aanvraag"
            bijlageURIs.forEachIndexed { index, uri ->
                every { drcClientService.readEnkelvoudigInformatieobject(uri) } returns enkelvoudigInformatieobjecten[index]
                every { drcClientService.readEnkelvoudigInformatieobject(uri) } returns enkelvoudigInformatieobjecten[index]
            }
            every {
                zrcClientService.createZaakInformatieobject(
                    capture(createdZaakInformatieobjectSlot),
                    beschrijving
                )
            } returns zaakInformatieobjecten[0] andThenAnswer { zaakInformatieobjecten[1] }

            When("the bijlagen are paired with the zaak") {
                productaanvraagService.pairBijlagenWithZaak(bijlageURIs, zaakUrl)

                Then("for every bijlage a zaakInformatieobject should be created") {
                    verify(exactly = 2) {
                        zrcClientService.createZaakInformatieobject(any(), any())
                    }
                    createdZaakInformatieobjectSlot.captured.run {
                        zaak shouldBe zaakUrl
                        beschrijving shouldBe beschrijving
                        informatieobject shouldBe enkelvoudigInformatieobjecten[1].url
                        titel shouldBe enkelvoudigInformatieobjecten[1].titel
                    }
                }
            }
        }
    }
})
