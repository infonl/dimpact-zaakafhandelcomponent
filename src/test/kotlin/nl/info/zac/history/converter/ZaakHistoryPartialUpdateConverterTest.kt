/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.history.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.shared.model.Bron
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.model.audit.createZRCAuditTrailRegel
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Wijzigingen
import nl.info.zac.history.model.HistoryAction
import java.math.BigDecimal
import java.net.URI
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

class ZaakHistoryPartialUpdateConverterTest : BehaviorSpec({
    val zrcClientService = mockk<ZrcClientService>()
    val userName = "fakeUserName"
    val description = "description"
    val zaakHistoryPartialUpdateConverter = ZaakHistoryPartialUpdateConverter(zrcClientService)

    afterEach {
        checkUnnecessaryStub()
    }

    Given(
        """
           An audit trail with action 'create' and new values for various resources
        """.trimIndent()
    ) {
        val rolTypeUri = "https://example.com/roltype/${UUID.randomUUID()}"
        val creationDate = ZonedDateTime.of(2024, 10, 30, 0, 0, 0, 0, ZoneOffset.UTC)
        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            aanmaakdatum = creationDate,
            gebruikersWeergave = userName,
            bron = Bron.ZAKEN_API,
            actie = "create",
            actieWeergave = "Object aangemaakt",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "zaak",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = description,
            wijzigingen = Wijzigingen().apply {
                nieuw = mapOf(
                    "roltype" to rolTypeUri,
                    "roltoelichting" to "",
                    "omschrijving" to "fakeOmschrijving",
                    "betrokkeneIdentificatie" to mapOf(
                        "achternaam" to "fakeAchternaam",
                        "identificatie" to "fakeIdentificatie",
                        "voorletters" to "fakeVoorletters"
                    ),
                    "betrokkeneType" to "medewerker",
                    "zaak" to "https://example.com/zaak",
                    "identificatienummer" to "fakeIdentificatie",
                    "naam" to "fakeVoorletters fakeAchternaam",
                    "omschrijvingGeneriek" to "initiator"
                )
            }
        )
        val newValues = mapOf(
            "startdatum" to "2024-10-30",
            "uiterlijkeEinddatumAfdoening" to "2024-11-30",
            "einddatumGepland" to "2024-12-30",
            "zaakgeometrie" to mapOf(
                "type" to "Point",
                "coordinates" to listOf(
                    BigDecimal("53.602182801494195"),
                    BigDecimal("5.363728969647492")
                )
            ),
            "fakeKey" to "fakeValue"
        )

        When("history is requested") {
            val history = zaakHistoryPartialUpdateConverter.convertPartialUpdate(
                zrcAuditTrailRegel,
                HistoryAction.AANGEMAAKT,
                emptyMap<String, String>(),
                newValues
            )

            Then("it is converted correctly") {
                history.size shouldBe 5
                with(history[0]) {
                    datumTijd shouldBe creationDate
                    door shouldBe userName
                    toelichting shouldBe description
                    attribuutLabel shouldBe "startdatum"
                    oldValue shouldBe null
                    newValue shouldBe "30-10-2024"
                }
                with(history[1]) {
                    datumTijd shouldBe creationDate
                    door shouldBe userName
                    toelichting shouldBe description
                    attribuutLabel shouldBe "uiterlijkeEinddatumAfdoening"
                    oldValue shouldBe null
                    newValue shouldBe "30-11-2024"
                }
                with(history[2]) {
                    datumTijd shouldBe creationDate
                    door shouldBe userName
                    toelichting shouldBe description
                    attribuutLabel shouldBe "einddatumGepland"
                    oldValue shouldBe null
                    newValue shouldBe "30-12-2024"
                }
                with(history[3]) {
                    datumTijd shouldBe creationDate
                    door shouldBe userName
                    toelichting shouldBe description
                    attribuutLabel shouldBe "zaakgeometrie"
                    oldValue shouldBe null
                    newValue shouldBe "POINT(53.602182801494195 5.363728969647492)"
                }
                with(history[4]) {
                    datumTijd shouldBe creationDate
                    door shouldBe userName
                    toelichting shouldBe description
                    attribuutLabel shouldBe "fakeKey"
                    oldValue shouldBe null
                    newValue shouldBe "fakeValue"
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
            wijzigingen = Wijzigingen()
        )
        val oldValues = mapOf(
            "zaakgeometrie" to mapOf(
                "type" to "Point",
                "coordinates" to listOf(
                    BigDecimal("52.602182801494195"),
                    BigDecimal("4.363728969647492")
                )
            )
        )
        val newValues = mapOf(
            "zaakgeometrie" to mapOf(
                "type" to "Point",
                "coordinates" to listOf(
                    BigDecimal("53.602182801494195"),
                    BigDecimal("5.363728969647492")
                )
            )
        )

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryPartialUpdateConverter.convertPartialUpdate(
                zrcAuditTrailRegel,
                HistoryAction.GEWIJZIGD,
                oldValues,
                newValues
            )

            Then("it should return correct data") {
                historyLines.size shouldBe 1
                with(historyLines.first()) {
                    attribuutLabel shouldBe "zaakgeometrie"
                    oldValue shouldBe "POINT(52.602182801494195 4.363728969647492)"
                    newValue shouldBe "POINT(53.602182801494195 5.363728969647492)"
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe "Test User"
                    toelichting shouldBe "xyz"
                    actie shouldBe HistoryAction.GEWIJZIGD
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
            wijzigingen = Wijzigingen()
        )
        val oldValues = mapOf("communicatiekanaal" to "old")
        val newValues = mapOf("communicatiekanaal" to "new")

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryPartialUpdateConverter.convertPartialUpdate(
                zrcAuditTrailRegel,
                HistoryAction.GEWIJZIGD,
                oldValues,
                newValues
            )

            Then("it should return correct data") {
                historyLines.size shouldBe 1
                with(historyLines.first()) {
                    attribuutLabel shouldBe "communicatiekanaal"
                    oldValue shouldBe "old"
                    newValue shouldBe "new"
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    toelichting shouldBe "hologram"
                    actie shouldBe HistoryAction.GEWIJZIGD
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
            wijzigingen = Wijzigingen()
        )
        val oldValues = mapOf("my_list" to listOf(1, 2))
        val newValues = mapOf("my_list" to listOf(1))

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryPartialUpdateConverter.convertPartialUpdate(
                zrcAuditTrailRegel,
                HistoryAction.GEWIJZIGD,
                oldValues,
                newValues
            )

            Then("it should not throw an exception") {
                historyLines.size shouldBe 1
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
            wijzigingen = Wijzigingen()
        )
        val oldValues = mapOf("my_list" to listOf(1, 2))
        val newValues = mapOf("my_list" to listOf(1, 3))

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryPartialUpdateConverter.convertPartialUpdate(
                zrcAuditTrailRegel,
                HistoryAction.GEWIJZIGD,
                oldValues,
                newValues
            )

            Then("it should not throw an exception") {
                historyLines.size shouldBe 1
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
            wijzigingen = Wijzigingen()
        )
        val oldValues = mapOf("my_list" to listOf(1, 2))
        val newValues = mapOf("my_list" to listOf(1, 2))

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryPartialUpdateConverter.convertPartialUpdate(
                zrcAuditTrailRegel,
                HistoryAction.GEWIJZIGD,
                oldValues,
                newValues
            )

            Then("it should not contain lines") {
                historyLines.size shouldBe 0
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
            wijzigingen = Wijzigingen()
        )
        val oldValues = mapOf("my_map" to mapOf("my_key" to "my_value"))
        val newValues = mapOf("my_list" to mapOf("my_key" to "my_changed_value"))

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryPartialUpdateConverter.convertPartialUpdate(
                zrcAuditTrailRegel,
                HistoryAction.GEWIJZIGD,
                oldValues,
                newValues
            )

            Then("it should contain a line") {
                historyLines.size shouldBe 1
            }
        }
    }

    Given("Audit trail has a partial_update where gerelateerdeZaken was linked for the first time") {
        val gerelateerdeZaak = createZaak(identificatie = "fakeGerelateerdeZaakIdentificatie1")
        every { zrcClientService.readZaak(gerelateerdeZaak.url) } returns gerelateerdeZaak
        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.ZAKEN_API,
            actie = "partial_update",
            actieWeergave = "Almost updated",
            resultaat = 200,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "zaak",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "",
            wijzigingen = Wijzigingen()
        )
        val oldValues = mapOf("gerelateerdeZaken" to emptyList<Any>())
        val newValues = mapOf("gerelateerdeZaken" to listOf(mapOf("url" to gerelateerdeZaak.url.toString())))

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryPartialUpdateConverter.convertPartialUpdate(
                zrcAuditTrailRegel,
                HistoryAction.GEWIJZIGD,
                oldValues,
                newValues
            )

            Then("it should return null for oldValue and the zaak identificatie for newValue") {
                historyLines.size shouldBe 1
                with(historyLines.first()) {
                    attribuutLabel shouldBe "gerelateerdeZaken"
                    oldValue shouldBe null
                    newValue shouldBe gerelateerdeZaak.identificatie
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    toelichting shouldBe ""
                    actie shouldBe HistoryAction.GEWIJZIGD
                }
            }
        }
    }

    Given("Audit trail has a partial_update where multiple gerelateerde zaken were linked") {
        val gerelateerdeZaak1 = createZaak(identificatie = "fakeGerelateerdeZaakIdentificatie1")
        val gerelateerdeZaak2 = createZaak(identificatie = "fakeGerelateerdeZaakIdentificatie2")
        every { zrcClientService.readZaak(gerelateerdeZaak1.url) } returns gerelateerdeZaak1
        every { zrcClientService.readZaak(gerelateerdeZaak2.url) } returns gerelateerdeZaak2
        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.ZAKEN_API,
            actie = "partial_update",
            actieWeergave = "Almost updated",
            resultaat = 200,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "zaak",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "",
            wijzigingen = Wijzigingen()
        )
        val oldValues = mapOf("gerelateerdeZaken" to emptyList<Any>())
        val newValues = mapOf(
            "gerelateerdeZaken" to listOf(
                mapOf("url" to gerelateerdeZaak1.url.toString()),
                mapOf("url" to gerelateerdeZaak2.url.toString())
            )
        )

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryPartialUpdateConverter.convertPartialUpdate(
                zrcAuditTrailRegel,
                HistoryAction.GEWIJZIGD,
                oldValues,
                newValues
            )

            Then("it should return the zaak identificaties joined by a comma") {
                historyLines.size shouldBe 1
                with(historyLines.first()) {
                    attribuutLabel shouldBe "gerelateerdeZaken"
                    oldValue shouldBe null
                    newValue shouldBe "${gerelateerdeZaak1.identificatie}, ${gerelateerdeZaak2.identificatie}"
                }
            }
        }
    }

    Given("Audit trail has a partial_update where gerelateerdeZaken contains a duplicate url") {
        val gerelateerdeZaak = createZaak(identificatie = "fakeGerelateerdeZaakIdentificatie1")
        every { zrcClientService.readZaak(gerelateerdeZaak.url) } returns gerelateerdeZaak
        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.ZAKEN_API,
            actie = "partial_update",
            actieWeergave = "Almost updated",
            resultaat = 200,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "zaak",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "",
            wijzigingen = Wijzigingen()
        )
        val oldValues = mapOf("gerelateerdeZaken" to emptyList<Any>())
        val newValues = mapOf(
            "gerelateerdeZaken" to listOf(
                mapOf("url" to gerelateerdeZaak.url.toString()),
                mapOf("url" to gerelateerdeZaak.url.toString())
            )
        )

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryPartialUpdateConverter.convertPartialUpdate(
                zrcAuditTrailRegel,
                HistoryAction.GEWIJZIGD,
                oldValues,
                newValues
            )

            Then("it should return the zaak identificatie only once") {
                historyLines.size shouldBe 1
                with(historyLines.first()) {
                    attribuutLabel shouldBe "gerelateerdeZaken"
                    newValue shouldBe gerelateerdeZaak.identificatie
                }
            }
        }
    }

    Given("Audit trail has a partial_update where a gerelateerde zaak can no longer be resolved") {
        val gerelateerdeZaakUri = URI("https://example.com/zaken/${UUID.randomUUID()}")
        every { zrcClientService.readZaak(gerelateerdeZaakUri) } throws RuntimeException("fakeException")
        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.ZAKEN_API,
            actie = "partial_update",
            actieWeergave = "Almost updated",
            resultaat = 200,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "zaak",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "",
            wijzigingen = Wijzigingen()
        )
        val oldValues = mapOf("gerelateerdeZaken" to emptyList<Any>())
        val newValues = mapOf("gerelateerdeZaken" to listOf(mapOf("url" to gerelateerdeZaakUri.toString())))

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryPartialUpdateConverter.convertPartialUpdate(
                zrcAuditTrailRegel,
                HistoryAction.GEWIJZIGD,
                oldValues,
                newValues
            )

            Then("it should fall back to the zaak url") {
                historyLines.size shouldBe 1
                with(historyLines.first()) {
                    attribuutLabel shouldBe "gerelateerdeZaken"
                    newValue shouldBe gerelateerdeZaakUri.toString()
                }
            }
        }
    }

    Given(
        """
           Audit trail has a partial_update where gerelateerdeZaken contains an invalid url,
           a non-map entry and a map without a url
        """.trimIndent()
    ) {
        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.ZAKEN_API,
            actie = "partial_update",
            actieWeergave = "Almost updated",
            resultaat = 200,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "zaak",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "",
            wijzigingen = Wijzigingen()
        )
        val oldValues = mapOf("gerelateerdeZaken" to emptyList<Any>())
        val newValues = mapOf(
            "gerelateerdeZaken" to listOf(
                mapOf("url" to "https://exam ple.com/invalidUrl"),
                "notAMap",
                mapOf("noUrlKey" to "fakeValue")
            )
        )

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryPartialUpdateConverter.convertPartialUpdate(
                zrcAuditTrailRegel,
                HistoryAction.GEWIJZIGD,
                oldValues,
                newValues
            )

            Then("it should return null for newValue") {
                historyLines.size shouldBe 1
                with(historyLines.first()) {
                    attribuutLabel shouldBe "gerelateerdeZaken"
                    newValue shouldBe null
                }
            }
        }
    }

    Given("Audit trail has resource hoofdzaak with action partial_update") {
        val zaak1 = createZaak(identificatie = "identificatie1")
        val zaak2 = createZaak(identificatie = "identificatie2")
        every { zrcClientService.readZaak(zaak1.url) } returns zaak1
        every { zrcClientService.readZaak(zaak2.url) } returns zaak2
        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "partial_update",
            actieWeergave = "Almost updated",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "zaak",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "xyz",
            wijzigingen = Wijzigingen()
        )
        val oldValues = mapOf("hoofdzaak" to zaak1.url.toString())
        val newValues = mapOf("hoofdzaak" to zaak2.url.toString())

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryPartialUpdateConverter.convertPartialUpdate(
                zrcAuditTrailRegel,
                HistoryAction.GEWIJZIGD,
                oldValues,
                newValues
            )

            Then("it should return correct data") {
                historyLines.size shouldBe 1
                with(historyLines.first()) {
                    attribuutLabel shouldBe "hoofdzaak"
                    oldValue shouldBe zaak1.identificatie
                    newValue shouldBe zaak2.identificatie
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe "Test User"
                    toelichting shouldBe "xyz"
                    actie shouldBe HistoryAction.GEWIJZIGD
                }
            }
        }
    }
})
