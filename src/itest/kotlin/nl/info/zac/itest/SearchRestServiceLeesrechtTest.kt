/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.BEHANDELAAR_1_EN_BRP_ZOEKER_2
import nl.info.zac.itest.config.BEHANDELAAR_2
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_2
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.TestUser
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID

class SearchRestServiceLeesrechtTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zaakHelper = ZaakHelper(ZacClient(itestHttpClient))

    fun searchZaak(zaakIdentificatie: String, testUser: TestUser) =
        itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/zoeken/list",
            requestBodyAsString = """
                {
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": false,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": { "ZAAK_IDENTIFICATIE": "$zaakIdentificatie" },
                    "filters": {},
                    "datums": {},
                    "rows": 10,
                    "page": 0,
                    "type": "ZAAK"
                }
            """.trimIndent(),
            testUser = testUser
        )

    fun searchGerelateerdLinkableZaak(zaakUuid: UUID, zaakIdentificatie: String, testUser: TestUser) =
        itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/zaken/gekoppelde-zaken/$zaakUuid/zoek-koppelbare-zaken",
            requestBodyAsString = """
                {
                    "zoekZaakIdentifier": "$zaakIdentificatie",
                    "relationType": "GERELATEERD",
                    "rows": 10,
                    "page": 0
                }
            """.trimIndent(),
            testUser = testUser
        )

    given(
        """
        An indexed zaak of zaaktype test 1 (domein test 2) and an indexed zaak of zaaktype test 2
        (domein test 1), and a user who is a behandelaar in domein test 1 but holds only the brp_zoeken
        application role in domein test 2
        """
    ) {
        val (zaakWithoutReadRoleIdentificatie, _) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_1_UUID,
            group = GROUP_BEHANDELAARS_TEST_2,
            indexZaak = true,
            testUser = BEHANDELAAR_2
        )
        val (zaakWithReadRoleIdentificatie, zaakWithReadRoleUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            group = GROUP_BEHANDELAARS_TEST_1,
            indexZaak = true,
            testUser = BEHANDELAAR_1
        )

        `when`("a behandelaar in domein test 2 searches for the zaak of zaaktype test 1") {
            val response = searchZaak(zaakWithoutReadRoleIdentificatie, BEHANDELAAR_2)

            then("the zaak is found, so it is present in the search index") {
                logger.info { "Response: ${response.bodyAsString}" }
                response.code shouldBe HTTP_OK
                JSONObject(response.bodyAsString).getInt("totaal") shouldBe 1
            }
        }

        `when`("the user searches for the zaak of zaaktype test 2") {
            val response = searchZaak(zaakWithReadRoleIdentificatie, BEHANDELAAR_1_EN_BRP_ZOEKER_2)

            then("the zaak is found because the user holds a read role for its zaaktype") {
                logger.info { "Response: ${response.bodyAsString}" }
                response.code shouldBe HTTP_OK
                JSONObject(response.bodyAsString).getInt("totaal") shouldBe 1
            }
        }

        `when`("the user searches for the zaak of zaaktype test 1") {
            val response = searchZaak(zaakWithoutReadRoleIdentificatie, BEHANDELAAR_1_EN_BRP_ZOEKER_2)

            then("the zaak is not found because the user holds no read role for its zaaktype") {
                logger.info { "Response: ${response.bodyAsString}" }
                response.code shouldBe HTTP_OK
                JSONObject(response.bodyAsString).getInt("totaal") shouldBe 0
            }
        }

        `when`(
            "the user searches for the zaak of zaaktype test 1 to relate it to the zaak of zaaktype test 2"
        ) {
            val response = searchGerelateerdLinkableZaak(
                zaakUuid = zaakWithReadRoleUuid,
                zaakIdentificatie = zaakWithoutReadRoleIdentificatie,
                testUser = BEHANDELAAR_1_EN_BRP_ZOEKER_2
            )

            then("the zaak is not offered because the user holds no read role for its zaaktype") {
                logger.info { "Response: ${response.bodyAsString}" }
                response.code shouldBe HTTP_OK
                JSONObject(response.bodyAsString).getInt("totaal") shouldBe 0
            }
        }
    }
})
