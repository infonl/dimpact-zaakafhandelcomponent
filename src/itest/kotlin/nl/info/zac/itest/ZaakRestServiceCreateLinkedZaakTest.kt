/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ResponseContent
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2024_01_01
import nl.info.zac.itest.config.ItestConfiguration.VERTROUWELIJKHEIDAANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID

class ZaakRestServiceCreateLinkedZaakTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)

    fun createLinkedZaak(zaaktypeUuid: UUID, zaakToLinkToUuid: UUID, relatieType: String): ResponseContent =
        itestHttpClient.performJSONPostRequest(
            url = "$ZAC_API_URI/zaken/zaak",
            requestBodyAsString = """
                {
                    "zaak": {
                        "zaaktype": { "uuid": "$zaaktypeUuid" },
                        "startdatum": "$DATE_TIME_2024_01_01",
                        "groep": {
                            "id": "${GROUP_BEHANDELAARS_TEST_1.name}",
                            "naam": "${GROUP_BEHANDELAARS_TEST_1.description}"
                        },
                        "communicatiekanaal": "$COMMUNICATIEKANAAL_TEST_1",
                        "vertrouwelijkheidaanduiding": "$VERTROUWELIJKHEIDAANDUIDING_OPENBAAR",
                        "omschrijving": "${ZaakRestServiceCreateLinkedZaakTest::class.simpleName}-$relatieType"
                    },
                    "bagObjecten": [],
                    "koppeling": {
                        "zaakUuid": "$zaakToLinkToUuid",
                        "relatieType": "$relatieType"
                    }
                }
            """.trimIndent(),
            testUser = BEHANDELAAR_1
        )

    fun readGerelateerdeZaken(zaakIdentificatie: String) =
        zacClient.retrieveZaak(zaakIdentificatie, BEHANDELAAR_1).run {
            code shouldBe HTTP_OK
            JSONObject(bodyAsString).getJSONArray("gerelateerdeZaken")
                .run { (0 until length()).map(::getJSONObject) }
                .map { it.getString("identificatie") to it.getString("relatieType") }
        }

    fun readHistory(zaakUuid: UUID) =
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/historie",
            testUser = BEHANDELAAR_1
        ).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
            JSONArray(bodyAsString).run { (0 until length()).map(::getJSONObject) }
        }

    fun JSONObject.toHistoryLine() = listOf(getString("actie"), getString("attribuutLabel"), optString("nieuweWaarde"))

    given("An open zaak and a behandelaar who may link zaken and start zaken") {
        val (zaakIdentificatie, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            testUser = BEHANDELAAR_1
        )
        lateinit var gerelateerdeZaakIdentificatie: String

        `when`("a zaak is created linked to that zaak as gerelateerde zaak") {
            val response = createLinkedZaak(
                zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
                zaakToLinkToUuid = zaakUuid,
                relatieType = "GERELATEERD"
            )

            then("the zaak is created") {
                logger.info { "Response: ${response.bodyAsString}" }
                response.code shouldBe HTTP_OK
                gerelateerdeZaakIdentificatie = JSONObject(response.bodyAsString).getString("identificatie")
            }

            and("both zaken list each other as gerelateerde zaak") {
                readGerelateerdeZaken(gerelateerdeZaakIdentificatie) shouldBe
                    listOf(zaakIdentificatie to "GERELATEERD")
                readGerelateerdeZaken(zaakIdentificatie) shouldBe
                    listOf(gerelateerdeZaakIdentificatie to "GERELATEERD")
            }

            and("the history of the existing zaak shows that the new zaak was added to its gerelateerde zaken") {
                val gerelateerdeZakenHistoryLine = readHistory(zaakUuid)
                    .first { it.getString("attribuutLabel") == "gerelateerdeZaken" }
                gerelateerdeZakenHistoryLine.toHistoryLine() shouldBe
                    listOf("GEWIJZIGD", "gerelateerdeZaken", gerelateerdeZaakIdentificatie)
                gerelateerdeZakenHistoryLine.getString("toelichting") shouldBe "Aangemaakt als gerelateerde zaak"
            }

            and("the history of the new zaak shows its creation and its gerelateerde zaak") {
                val gerelateerdeZaakUuid = zacClient.retrieveZaak(gerelateerdeZaakIdentificatie, BEHANDELAAR_1)
                    .run { JSONObject(bodyAsString).getString("uuid").run(UUID::fromString) }
                readHistory(gerelateerdeZaakUuid).map { it.toHistoryLine() } shouldContainAll listOf(
                    listOf("AANGEMAAKT", "zaak", gerelateerdeZaakIdentificatie),
                    listOf("GEKOPPELD", "gerelateerdeZaken", zaakIdentificatie)
                )
            }
        }

        `when`("a zaak of a deelzaaktype of its zaaktype is created linked to that zaak as deelzaak") {
            val response = createLinkedZaak(
                zaaktypeUuid = ZAAKTYPE_CMMN_TEST_3_UUID,
                zaakToLinkToUuid = zaakUuid,
                relatieType = "DEELZAAK"
            )
            lateinit var deelzaakIdentificatie: String

            then("the zaak is created") {
                logger.info { "Response: ${response.bodyAsString}" }
                response.code shouldBe HTTP_OK
                deelzaakIdentificatie = JSONObject(response.bodyAsString).getString("identificatie")
            }

            and("the existing zaak lists the new zaak as deelzaak and the new zaak lists it as hoofdzaak") {
                readGerelateerdeZaken(zaakIdentificatie) shouldContain (deelzaakIdentificatie to "DEELZAAK")
                readGerelateerdeZaken(deelzaakIdentificatie) shouldBe listOf(zaakIdentificatie to "HOOFDZAAK")
            }

            and("the history of the new zaak shows its creation and its hoofdzaak") {
                val deelzaakUuid = zacClient.retrieveZaak(deelzaakIdentificatie, BEHANDELAAR_1)
                    .run { JSONObject(bodyAsString).getString("uuid").run(UUID::fromString) }
                readHistory(deelzaakUuid).map { it.toHistoryLine() } shouldContainAll listOf(
                    listOf("AANGEMAAKT", "zaak", deelzaakIdentificatie),
                    listOf("GEKOPPELD", "hoofdzaak", zaakIdentificatie)
                )
            }
        }

        `when`("a zaak is created linked to that zaak as hoofdzaak") {
            val response = createLinkedZaak(
                zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
                zaakToLinkToUuid = zaakUuid,
                relatieType = "HOOFDZAAK"
            )

            then("the request is refused with a validation error") {
                logger.info { "Response: ${response.bodyAsString}" }
                response.code shouldBe HTTP_BAD_REQUEST
            }
        }
    }
})
