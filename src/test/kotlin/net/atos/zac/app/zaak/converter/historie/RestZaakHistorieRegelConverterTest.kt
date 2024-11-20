package net.atos.zac.app.zaak.converter.historie

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.shared.model.Bron
import net.atos.client.zgw.shared.model.audit.createZRCAuditTrailRegel
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.generated.Wijzigingen
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createResultaatType
import net.atos.client.zgw.ztc.model.createRolType
import net.atos.client.zgw.ztc.model.createStatusType
import net.atos.zac.app.audit.model.RESTHistorieActie
import java.math.BigDecimal
import java.net.URI
import java.util.UUID

@Suppress("LargeClass")
class RestZaakHistorieRegelConverterTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val zaakHistoryService = ZaakHistoryService(
        zrcClientService,
        ztcClientService,
        RestZaakHistoriePartialUpdateConverter(zrcClientService)
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Audit trail has resource zaak with action created") {
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

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

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

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

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

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

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

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

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

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

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

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

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

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

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
                oud = mapOf(
                    "zaakgeometrie" to mapOf(
                        "type" to "Point",
                        "coordinates" to listOf(
                            BigDecimal("52.602182801494195"),
                            BigDecimal("4.363728969647492")
                        )
                    )
                )
                nieuw = mapOf(
                    "zaakgeometrie" to mapOf(
                        "type" to "Point",
                        "coordinates" to listOf(
                            BigDecimal("53.602182801494195"),
                            BigDecimal("5.363728969647492")
                        )
                    )
                )
            }
        )

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should return correct data") {
                listRestRegel.size shouldBe 1
                with(listRestRegel.first()) {
                    attribuutLabel shouldBe "zaakgeometrie"
                    oudeWaarde shouldBe "POINT(52.602182801494195 4.363728969647492)"
                    nieuweWaarde shouldBe "POINT(53.602182801494195 5.363728969647492)"
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
                oud = mapOf("communicatiekanaal" to "old")
                nieuw = mapOf("communicatiekanaal" to "new")
            }
        )

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

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

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should not throw an exception") {
                listRestRegel.size shouldBe 1
            }
        }
    }

    Given("A partial update with a list that stays the same size but values change") {
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

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should not throw an exception") {
                listRestRegel.size shouldBe 1
            }
        }
    }

    Given("A partial update with a list that stays exactly the same") {
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

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should not contain lines") {
                listRestRegel.size shouldBe 0
            }
        }
    }

    Given("A partial update with a map with a value that changes") {
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

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should contain a line") {
                listRestRegel.size shouldBe 1
            }
        }
    }

    Given("A retrieve action") {
        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "retrieve",
            actieWeergave = "retrieved some data",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "communicatiekanaal",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "hologram",
            wijzigingen = Wijzigingen()
        )

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should contain no lines") {
                listRestRegel.size shouldBe 0
            }
        }
    }

    Given("An unknown resource") {
        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "update",
            actieWeergave = "updated some data",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "some_unknown_value",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "hologram",
            wijzigingen = Wijzigingen().apply {
                nieuw = mapOf("statustype" to "https://example.com/statustype")
            }
        )

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should return correct data, with null values for oudeWaarde and nieuweWaarde") {
                listRestRegel.size shouldBe 1
                with(listRestRegel.first()) {
                    attribuutLabel shouldBe "some_unknown_value"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe null
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "hologram"
                    actie shouldBe RESTHistorieActie.GEWIJZIGD
                }
            }
        }
    }

    Given("Audit trail has resource hoofdzaak with action partial_update") {
        val zaak1 = createZaak(identificatie = "identificatie1")
        val zaak2 = createZaak(identificatie = "identificatie2")
        every {
            zrcClientService.readZaak(zaak1.url)
        } returns zaak1
        every {
            zrcClientService.readZaak(zaak2.url)
        } returns zaak2

        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "partial_update",
            actieWeergave = "Almost updated",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "zaak",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "xyz",
            wijzigingen = Wijzigingen().apply {
                oud = mapOf("hoofdzaak" to zaak1.url.toString())
                nieuw = mapOf("hoofdzaak" to zaak2.url.toString())
            }
        )

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should return correct data") {
                listRestRegel.size shouldBe 1
                with(listRestRegel.first()) {
                    attribuutLabel shouldBe "hoofdzaak"
                    oudeWaarde shouldBe zaak1.identificatie
                    nieuweWaarde shouldBe zaak2.identificatie
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe "Test User"
                    applicatie shouldBe null
                    toelichting shouldBe "xyz"
                    actie shouldBe RESTHistorieActie.GEWIJZIGD
                }
            }
        }
    }

    Given("Audit trail has resource relevanteAndereZaken with action partial_update") {
        val zaak1 = createZaak(identificatie = "identificatie1")
        val zaak2 = createZaak(identificatie = "identificatie2")
        val zaak3 = createZaak(identificatie = "identificatie3")
        val zaak4 = createZaak(identificatie = "identificatie4")

        every {
            zrcClientService.readZaak(zaak1.url)
        } returns zaak1
        every {
            zrcClientService.readZaak(zaak2.url)
        } returns zaak2
        every {
            zrcClientService.readZaak(zaak3.url)
        } returns zaak3
        every {
            zrcClientService.readZaak(zaak4.url)
        } returns zaak4

        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "partial_update",
            actieWeergave = "Almost updated",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "zaak",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "xyz",
            wijzigingen = Wijzigingen().apply {
                oud = mapOf(
                    "relevanteAndereZaken" to listOf(
                        mapOf("url" to zaak1.url.toString()),
                        mapOf("url" to zaak2.url.toString()),
                    )
                )
                nieuw = mapOf(
                    "relevanteAndereZaken" to listOf(
                        mapOf("url" to zaak3.url.toString()),
                        mapOf("url" to zaak4.url.toString()),
                    )
                )
            }
        )

        When("converted to REST historie regel") {
            val listRestRegel = zaakHistoryService.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should return correct data") {
                listRestRegel.size shouldBe 1
                with(listRestRegel.first()) {
                    attribuutLabel shouldBe "relevanteAndereZaken"
                    oudeWaarde shouldBe "${zaak1.identificatie}, ${zaak2.identificatie}"
                    nieuweWaarde shouldBe "${zaak3.identificatie}, ${zaak4.identificatie}"
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe "Test User"
                    applicatie shouldBe null
                    toelichting shouldBe "xyz"
                    actie shouldBe RESTHistorieActie.GEWIJZIGD
                }
            }
        }
    }
})
