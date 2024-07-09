package net.atos.zac.app.zaken.converter.historie

import io.kotest.assertions.any
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import net.atos.client.vrl.VrlClientService
import net.atos.client.vrl.model.generated.CommunicatieKanaal
import net.atos.client.zgw.shared.model.Bron
import net.atos.client.zgw.shared.model.audit.createZRCAuditTrailRegel
import net.atos.client.zgw.zrc.model.generated.Wijzigingen
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createResultaatType
import net.atos.client.zgw.ztc.model.createRolType
import net.atos.client.zgw.ztc.model.createStatusType
import net.atos.zac.app.audit.model.RESTHistorieActie
import java.net.URI
import java.util.Optional
import java.util.UUID

class RESTZaakHistorieRegelConverterTest : BehaviorSpec({

    beforeEach {
        checkUnnecessaryStub()
    }

    beforeSpec {
        clearAllMocks()
    }

    Given("Audit trail has resource zaak with action created") {
        val ztcClientService = mockk<ZtcClientService>()
        val vrlClientService = mockk<VrlClientService>()

        val zaakIdentificatie = "ZAAK-2024-0000000003"
        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.ZAKEN_API,
            actie = "create",
            actieWeergave = "Object aangemaakt",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "zaak",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "zaak created",
            wijzigingen = Wijzigingen().apply {
                nieuw = mapOf("identificatie" to zaakIdentificatie)
            }
        )
        val restZaakHistorieRegelConverter = RESTZaakHistorieRegelConverter(
            ztcClientService,
            RESTZaakHistoriePartialUpdateConverter(vrlClientService)
        )

        When("converted to REST historie regel") {
            val listRestRegel = restZaakHistorieRegelConverter.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should return correct data") {
                listRestRegel.size shouldBe 1
                with(listRestRegel.first()) {
                    attribuutLabel shouldBe "zaak"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe zaakIdentificatie
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "zaak created"
                    actie shouldBe RESTHistorieActie.AANGEMAAKT
                }
            }
        }
    }

    Given("Audit trail has resource rol with action updated") {
        val uuid = UUID.randomUUID()
        val rolTypeUri = "https://example.com/roltype/$uuid"
        val rolType = createRolType()
        val ztcClientService = mockk<ZtcClientService>()
        val vrlClientService = mockk<VrlClientService>()

        every { ztcClientService.readRoltype(uuid) } returns rolType

        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "update",
            actieWeergave = "Update",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "rol",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "rol updated",
            wijzigingen = Wijzigingen().apply {
                nieuw = mapOf(
                    "roltype" to rolTypeUri,
                    "roltoelichting" to "",
                    "omschrijving" to "dummyOmschrijving",
                    "betrokkeneIdentificatie" to mapOf(
                        "achternaam" to "dummyAchternaam",
                        "identificatie" to "dummyIdentificatie",
                        "voorletters" to "dummyVoorletters"
                    ),
                    "betrokkeneType" to "medewerker",
                    "zaak" to "https://example.com/zaak",
                    "identificatienummer" to "dummyIdentificatie",
                    "naam" to "dummyVoorletters dummyAchternaam",
                    "omschrijvingGeneriek" to "initiator"
                )
            }
        )
        val restZaakHistorieRegelConverter = RESTZaakHistorieRegelConverter(
            ztcClientService,
            RESTZaakHistoriePartialUpdateConverter(vrlClientService)
        )

        When("converted to REST historie regel") {
            val listRestRegel = restZaakHistorieRegelConverter.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should return correct data") {
                listRestRegel.size shouldBe 1
                with(listRestRegel.first()) {
                    attribuutLabel shouldBe rolType.omschrijving
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "dummyVoorletters dummyAchternaam"
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "rol updated"
                    actie shouldBe RESTHistorieActie.GEWIJZIGD
                }
            }
        }
    }

    Given("Audit trail has resource zaakinformatieobject with action destroy") {
        val ztcClientService = mockk<ZtcClientService>()
        val vrlClientService = mockk<VrlClientService>()

        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "destroy",
            actieWeergave = "Destroy",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "zaakinformatieobject",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "file dropped",
            wijzigingen = Wijzigingen().apply {
                nieuw = mapOf("titel" to "title")
            }
        )
        val restZaakHistorieRegelConverter = RESTZaakHistorieRegelConverter(
            ztcClientService,
            RESTZaakHistoriePartialUpdateConverter(vrlClientService)
        )

        When("converted to REST historie regel") {
            val listRestRegel = restZaakHistorieRegelConverter.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should return correct data") {
                listRestRegel.size shouldBe 1
                with(listRestRegel.first()) {
                    attribuutLabel shouldBe "zaakinformatieobject"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "title"
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "file dropped"
                    actie shouldBe RESTHistorieActie.ONTKOPPELD
                }
            }
        }
    }

    Given("Audit trail has resource klantcontact with action create") {
        val ztcClientService = mockk<ZtcClientService>()
        val vrlClientService = mockk<VrlClientService>()

        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "create",
            actieWeergave = "Create",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "klantcontact",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "n/a",
            wijzigingen = Wijzigingen().apply {
                nieuw = mapOf("titel" to "title")
            }
        )
        val restZaakHistorieRegelConverter = RESTZaakHistorieRegelConverter(
            ztcClientService,
            RESTZaakHistoriePartialUpdateConverter(vrlClientService)
        )

        When("converted to REST historie regel") {
            val listRestRegel = restZaakHistorieRegelConverter.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should return correct data") {
                listRestRegel.size shouldBe 1
                with(listRestRegel.first()) {
                    attribuutLabel shouldBe "klantcontact"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe zrcAuditTrailRegel.resourceWeergave
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "n/a"
                    actie shouldBe RESTHistorieActie.GEKOPPELD
                }
            }
        }
    }

    Given("Audit trail has resource resultaat with action update") {
        val ztcClientService = mockk<ZtcClientService>()
        val vrlClientService = mockk<VrlClientService>()

        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "update",
            actieWeergave = "Update",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "resultaat",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "n/a",
            wijzigingen = Wijzigingen().apply {
                nieuw = mapOf("resultaattype" to "https://example.com/resultaattype")
            }
        )

        every { ztcClientService.readResultaattype(any<URI>()) } returns createResultaatType().apply {
            omschrijving = "description"
        }

        val restZaakHistorieRegelConverter = RESTZaakHistorieRegelConverter(
            ztcClientService,
            RESTZaakHistoriePartialUpdateConverter(vrlClientService)
        )

        When("converted to REST historie regel") {
            val listRestRegel = restZaakHistorieRegelConverter.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should return correct data") {
                listRestRegel.size shouldBe 1
                with(listRestRegel.first()) {
                    attribuutLabel shouldBe "resultaat"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "description"
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "n/a"
                    actie shouldBe RESTHistorieActie.GEWIJZIGD
                }
            }
        }
    }

    Given("Audit trail has resource status with action update") {
        val ztcClientService = mockk<ZtcClientService>()
        val vrlClientService = mockk<VrlClientService>()

        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "update",
            actieWeergave = "Update",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "status",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "n/a",
            wijzigingen = Wijzigingen().apply {
                nieuw = mapOf("statustype" to "https://example.com/statustype")
            }
        )

        every { ztcClientService.readStatustype(any<URI>()) } returns createStatusType().apply {
            omschrijving = "description"
        }

        val restZaakHistorieRegelConverter = RESTZaakHistorieRegelConverter(
            ztcClientService,
            RESTZaakHistoriePartialUpdateConverter(vrlClientService)
        )

        When("converted to REST historie regel") {
            val listRestRegel = restZaakHistorieRegelConverter.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should return correct data") {
                listRestRegel.size shouldBe 1
                with(listRestRegel.first()) {
                    attribuutLabel shouldBe "status"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "description"
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "n/a"
                    actie shouldBe RESTHistorieActie.GEWIJZIGD
                }
            }
        }
    }

    Given("Audit trail has resource zaakobject with action destroy") {
        val ztcClientService = mockk<ZtcClientService>()
        val vrlClientService = mockk<VrlClientService>()

        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "destroy",
            actieWeergave = "Destroy",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "zaakobject",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "n/a",
            wijzigingen = Wijzigingen().apply {
                nieuw = mapOf(
                    "url" to "https://example.com/zaakobject",
                    "uuid" to UUID.randomUUID().toString(),
                    "zaak" to "https://example.com/zaak",
                    "object" to "https://example.com/object",
                    "objectType" to "adres",
                    "objectTypeOverige" to "dummyObjectTypeOverige",
                    "relatieomschrijving" to "relation description",
                    "objectIdentificatie" to mapOf(
                        "identificatie" to "identity"
                    )
                )
            }
        )
        val restZaakHistorieRegelConverter = RESTZaakHistorieRegelConverter(
            ztcClientService,
            RESTZaakHistoriePartialUpdateConverter(vrlClientService)
        )

        When("converted to REST historie regel") {
            val listRestRegel = restZaakHistorieRegelConverter.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should return correct data") {
                listRestRegel.size shouldBe 1
                with(listRestRegel.first()) {
                    attribuutLabel shouldBe "objecttype.ADRES"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "identity"
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "n/a"
                    actie shouldBe RESTHistorieActie.ONTKOPPELD
                }
            }
        }
    }

    Given("Audit trail has resource zaakgeometrie with action partial_update") {
        val ztcClientService = mockk<ZtcClientService>()
        val vrlClientService = mockk<VrlClientService>()

        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "partial_update",
            actieWeergave = "Almost updated",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "zaakgeometrie",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "xyz",
            wijzigingen = Wijzigingen().apply {
                oud = mapOf("zaakgeometrie" to "Point")
                nieuw = mapOf("zaakgeometrie" to "Polygon")
            }
        )
        val restZaakHistorieRegelConverter = RESTZaakHistorieRegelConverter(
            ztcClientService,
            RESTZaakHistoriePartialUpdateConverter(vrlClientService)
        )

        When("converted to REST historie regel") {
            val listRestRegel = restZaakHistorieRegelConverter.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should return correct data") {
                listRestRegel.size shouldBe 1
                with(listRestRegel.first()) {
                    attribuutLabel shouldBe "zaakgeometrie"
                    oudeWaarde shouldBe "Point"
                    nieuweWaarde shouldBe "Polygon"
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe "Test User"
                    applicatie shouldBe null
                    toelichting shouldBe "xyz"
                    actie shouldBe RESTHistorieActie.GEWIJZIGD
                }
            }
        }
    }

    Given("Audit trail has resource communicatiekanaal with action partial_update") {
        val ztcClientService = mockk<ZtcClientService>()
        val vrlClientService = mockk<VrlClientService>()

        val oldUUID = UUID.randomUUID()
        val newUUID = UUID.randomUUID()
        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "partial_update",
            actieWeergave = "Almost updated",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "communicatiekanaal",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "hologram",
            wijzigingen = Wijzigingen().apply {
                oud = mapOf("communicatiekanaal" to "https://example.com/comminicationChannel/$oldUUID")
                nieuw = mapOf("communicatiekanaal" to "https://example.com/comminicationChannel/$newUUID")
            }
        )

        every {
            vrlClientService.findCommunicatiekanaal(oldUUID)
        } returns Optional.of(CommunicatieKanaal().apply { naam = "old" })
        every {
            vrlClientService.findCommunicatiekanaal(newUUID)
        } returns Optional.of(CommunicatieKanaal().apply { naam = "new" })

        val restZaakHistorieRegelConverter = RESTZaakHistorieRegelConverter(
            ztcClientService,
            RESTZaakHistoriePartialUpdateConverter(vrlClientService)
        )

        When("converted to REST historie regel") {
            val listRestRegel = restZaakHistorieRegelConverter.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should return correct data") {
                listRestRegel.size shouldBe 1
                with(listRestRegel.first()) {
                    attribuutLabel shouldBe "communicatiekanaal"
                    oudeWaarde shouldBe "old"
                    nieuweWaarde shouldBe "new"
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "hologram"
                    actie shouldBe RESTHistorieActie.GEWIJZIGD
                }
            }
        }
    }

    Given("A partial update with a list that has gotten smaller") {
        val ztcClientService = mockk<ZtcClientService>()
        val vrlClientService = mockk<VrlClientService>()

        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "partial_update",
            actieWeergave = "Almost updated",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "communicatiekanaal",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "hologram",
            wijzigingen = Wijzigingen().apply {
                this.oud = mapOf("my_list" to listOf(1, 2))
                this.nieuw = mapOf("my_list" to listOf(1))
            }
        )

        val restZaakHistorieRegelConverter = RESTZaakHistorieRegelConverter(
            ztcClientService,
            RESTZaakHistoriePartialUpdateConverter(vrlClientService)
        )

        When("converted to REST historie regel") {
            val listRestRegel = restZaakHistorieRegelConverter.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should not throw an exception") {
                listRestRegel.size shouldBe 1
            }
        }
    }

    Given("A partial update with a list that stays the same size but values change") {
        val ztcClientService = mockk<ZtcClientService>()
        val vrlClientService = mockk<VrlClientService>()

        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "partial_update",
            actieWeergave = "Almost updated",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "communicatiekanaal",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "hologram",
            wijzigingen = Wijzigingen().apply {
                this.oud = mapOf("my_list" to listOf(1, 2))
                this.nieuw = mapOf("my_list" to listOf(1, 3))
            }
        )

        val restZaakHistorieRegelConverter = RESTZaakHistorieRegelConverter(
            ztcClientService,
            RESTZaakHistoriePartialUpdateConverter(vrlClientService)
        )

        When("converted to REST historie regel") {
            val listRestRegel = restZaakHistorieRegelConverter.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should not throw an exception") {
                listRestRegel.size shouldBe 1
            }
        }
    }

    Given("A partial update with a list that stays exactly the same") {
        val ztcClientService = mockk<ZtcClientService>()
        val vrlClientService = mockk<VrlClientService>()

        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "partial_update",
            actieWeergave = "Almost updated",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "communicatiekanaal",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "hologram",
            wijzigingen = Wijzigingen().apply {
                this.oud = mapOf("my_list" to listOf(1, 2))
                this.nieuw = mapOf("my_list" to listOf(1, 2))
            }
        )

        val restZaakHistorieRegelConverter = RESTZaakHistorieRegelConverter(
            ztcClientService,
            RESTZaakHistoriePartialUpdateConverter(vrlClientService)
        )

        When("converted to REST historie regel") {
            val listRestRegel = restZaakHistorieRegelConverter.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should not contain lines") {
                listRestRegel.size shouldBe 0
            }
        }
    }

    Given("A partial update with a map with a value that changes") {
        val ztcClientService = mockk<ZtcClientService>()
        val vrlClientService = mockk<VrlClientService>()

        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "partial_update",
            actieWeergave = "Almost updated",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "communicatiekanaal",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "hologram",
            wijzigingen = Wijzigingen().apply {
                this.oud = mapOf("my_map" to mapOf("my_key" to "my_value"))
                this.nieuw = mapOf("my_list" to mapOf("my_key" to "my_changed_value"))
            }
        )

        val restZaakHistorieRegelConverter = RESTZaakHistorieRegelConverter(
            ztcClientService,
            RESTZaakHistoriePartialUpdateConverter(vrlClientService)
        )

        When("converted to REST historie regel") {
            val listRestRegel = restZaakHistorieRegelConverter.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should contain a line") {
                listRestRegel.size shouldBe 1
            }
        }
    }
})
