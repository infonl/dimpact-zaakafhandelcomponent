/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.history

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.shared.model.Bron
import nl.info.client.zgw.shared.model.audit.createZRCAuditTrailRegel
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Wijzigingen
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.zac.history.converter.ZaakHistoryPartialUpdateConverter
import nl.info.zac.history.model.HistoryAction
import java.net.URI
import java.util.UUID

class ZaakHistoryServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val zaakHistoryPartialUpdateConverter = mockk<ZaakHistoryPartialUpdateConverter>()
    val zaakHistoryService = ZaakHistoryService(
        zrcClientService,
        ztcClientService,
        zaakHistoryPartialUpdateConverter
    )

    afterEach {
        checkUnnecessaryStub()
    }

    Given("Audit trail has resource zaak with action created") {
        val zaakUUID = UUID.randomUUID()
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
        every { zrcClientService.listAuditTrail(zaakUUID) } returns listOf(zrcAuditTrailRegel)

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryService.getZaakHistory(zaakUUID)

            Then("it should return correct data") {
                historyLines.size shouldBe 1
                with(historyLines.first()) {
                    attribuutLabel shouldBe "zaak"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe zaakIdentificatie
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "zaak created"
                    actie shouldBe HistoryAction.AANGEMAAKT
                }
            }
        }
    }

    Given("Audit trail has resource rol with action updated") {
        val zaakUUID = UUID.randomUUID()
        val rolTypeUri = "https://example.com/roltype/$zaakUUID"
        val rolType = createRolType()
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
        every { zrcClientService.listAuditTrail(zaakUUID) } returns listOf(zrcAuditTrailRegel)
        every { ztcClientService.readRoltype(zaakUUID) } returns rolType

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryService.getZaakHistory(zaakUUID)

            Then("it should return correct data") {
                historyLines.size shouldBe 1
                with(historyLines.first()) {
                    attribuutLabel shouldBe rolType.omschrijving
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "fakeVoorletters fakeAchternaam"
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "rol updated"
                    actie shouldBe HistoryAction.GEWIJZIGD
                }
            }
        }
    }

    Given("Audit trail has resource zaakinformatieobject with action destroy") {
        val zaakUUID = UUID.randomUUID()
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
        every { zrcClientService.listAuditTrail(zaakUUID) } returns listOf(zrcAuditTrailRegel)

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryService.getZaakHistory(zaakUUID)

            Then("it should return correct data") {
                historyLines.size shouldBe 1
                with(historyLines.first()) {
                    attribuutLabel shouldBe "zaakinformatieobject"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "title"
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "file dropped"
                    actie shouldBe HistoryAction.ONTKOPPELD
                }
            }
        }
    }

    Given("Audit trail has resource klantcontact with action create") {
        val zaakUUID = UUID.randomUUID()
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
        every { zrcClientService.listAuditTrail(zaakUUID) } returns listOf(zrcAuditTrailRegel)

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryService.getZaakHistory(zaakUUID)

            Then("it should return correct data") {
                historyLines.size shouldBe 1
                with(historyLines.first()) {
                    attribuutLabel shouldBe "klantcontact"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe zrcAuditTrailRegel.resourceWeergave
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "n/a"
                    actie shouldBe HistoryAction.GEKOPPELD
                }
            }
        }
    }

    Given("Audit trail has resource resultaat with action update") {
        val zaakUUID = UUID.randomUUID()
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
        every { zrcClientService.listAuditTrail(zaakUUID) } returns listOf(zrcAuditTrailRegel)
        every { ztcClientService.readResultaattype(any<URI>()) } returns createResultaatType().apply {
            omschrijving = "description"
        }

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryService.getZaakHistory(zaakUUID)

            Then("it should return correct data") {
                historyLines.size shouldBe 1
                with(historyLines.first()) {
                    attribuutLabel shouldBe "resultaat"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "description"
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "n/a"
                    actie shouldBe HistoryAction.GEWIJZIGD
                }
            }
        }
    }

    Given("Audit trail has resource status with action update") {
        val zaakUUID = UUID.randomUUID()
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
        every { zrcClientService.listAuditTrail(zaakUUID) } returns listOf(zrcAuditTrailRegel)
        every { ztcClientService.readStatustype(any<URI>()) } returns createStatusType().apply {
            omschrijving = "description"
        }

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryService.getZaakHistory(zaakUUID)

            Then("it should return correct data") {
                historyLines.size shouldBe 1
                with(historyLines.first()) {
                    attribuutLabel shouldBe "status"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "description"
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "n/a"
                    actie shouldBe HistoryAction.GEWIJZIGD
                }
            }
        }
    }

    Given("Audit trail has resource zaakobject with action destroy") {
        val zaakUUID = UUID.randomUUID()
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
                    "objectTypeOverige" to "fakeObjectTypeOverige",
                    "relatieomschrijving" to "relation description",
                    "objectIdentificatie" to mapOf(
                        "identificatie" to "identity"
                    )
                )
            }
        )
        every { zrcClientService.listAuditTrail(zaakUUID) } returns listOf(zrcAuditTrailRegel)

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryService.getZaakHistory(zaakUUID)

            Then("it should return correct data") {
                historyLines.size shouldBe 1
                with(historyLines.first()) {
                    attribuutLabel shouldBe "objecttype.ADRES"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "identity"
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "n/a"
                    actie shouldBe HistoryAction.ONTKOPPELD
                }
            }
        }
    }

    Given("A retrieve action") {
        val zaakUUID = UUID.randomUUID()
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
        every { zrcClientService.listAuditTrail(zaakUUID) } returns listOf(zrcAuditTrailRegel)

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryService.getZaakHistory(zaakUUID)

            Then("it should contain no lines") {
                historyLines.size shouldBe 0
            }
        }
    }

    Given("An unknown resource") {
        val zaakUUID = UUID.randomUUID()
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
        every { zrcClientService.listAuditTrail(zaakUUID) } returns listOf(zrcAuditTrailRegel)

        When("converted to REST historie regel") {
            val historyLines = zaakHistoryService.getZaakHistory(zaakUUID)

            Then("it should return correct data, with null values for oudeWaarde and nieuweWaarde") {
                historyLines.size shouldBe 1
                with(historyLines.first()) {
                    attribuutLabel shouldBe "some_unknown_value"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe null
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe zrcAuditTrailRegel.gebruikersWeergave
                    applicatie shouldBe null
                    toelichting shouldBe "hologram"
                    actie shouldBe HistoryAction.GEWIJZIGD
                }
            }
        }
    }
})
