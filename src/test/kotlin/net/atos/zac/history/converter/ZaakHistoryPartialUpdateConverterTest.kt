/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.history.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.mockk
import net.atos.client.zgw.shared.model.Bron
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.generated.Wijzigingen
import net.atos.zac.history.model.HistoryAction
import nl.info.client.zgw.shared.model.audit.createZRCAuditTrailRegel
import java.math.BigDecimal
import java.net.URI
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

class ZaakHistoryPartialUpdateConverterTest : BehaviorSpec({
    val zrcClientService = mockk<ZrcClientService>()
    val userName = "dummyUserName"
    val description = "description"
    val zaakHistoryPartialUpdateConverter = ZaakHistoryPartialUpdateConverter(zrcClientService)

    beforeEach {
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
            "dummyKey" to "dummyValue"
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
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "30-10-2024"
                }
                with(history[1]) {
                    datumTijd shouldBe creationDate
                    door shouldBe userName
                    toelichting shouldBe description
                    attribuutLabel shouldBe "uiterlijkeEinddatumAfdoening"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "30-11-2024"
                }
                with(history[2]) {
                    datumTijd shouldBe creationDate
                    door shouldBe userName
                    toelichting shouldBe description
                    attribuutLabel shouldBe "einddatumGepland"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "30-12-2024"
                }
                with(history[3]) {
                    datumTijd shouldBe creationDate
                    door shouldBe userName
                    toelichting shouldBe description
                    attribuutLabel shouldBe "zaakgeometrie"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "POINT(53.602182801494195 5.363728969647492)"
                }
                with(history[4]) {
                    datumTijd shouldBe creationDate
                    door shouldBe userName
                    toelichting shouldBe description
                    attribuutLabel shouldBe "dummyKey"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "dummyValue"
                }
            }
        }
    }
})
