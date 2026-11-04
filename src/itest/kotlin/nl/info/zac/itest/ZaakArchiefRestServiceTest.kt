/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.createZaakAndRetrieve
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RECORDMANAGER_1
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID

private const val ARCHIEFNOMINATIE_BLIJVEND_BEWAREN = "BLIJVEND_BEWAREN"
private const val ARCHIEFNOMINATIE_VERNIETIGEN = "VERNIETIGEN"
private const val REDEN = "fakeRedenWijzigenArchiefnominatie"

class ZaakArchiefRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()

    given("an afgehandelde zaak") {
        lateinit var zaakUuid: UUID
        lateinit var currentArchiefnominatie: String
        zacClient.createZaakAndRetrieve(
            zaakTypeUUID = ZAAKTYPE_CMMN_TEST_2_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_1
        ).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
            zaakUuid = JSONObject(bodyAsString).getString("uuid").let(UUID::fromString)
        }
        itestHttpClient.performPatchRequest(
            url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/afbreken",
            requestBodyAsString = """{ "zaakbeeindigRedenId": "ZAAK_NIET_ONTVANKELIJK" }""",
            testUser = BEHANDELAAR_1
        ).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
            currentArchiefnominatie = JSONObject(bodyAsString).getString("archiefNominatie")
        }
        val newArchiefnominatie = if (currentArchiefnominatie == ARCHIEFNOMINATIE_VERNIETIGEN) {
            ARCHIEFNOMINATIE_BLIJVEND_BEWAREN
        } else {
            ARCHIEFNOMINATIE_VERNIETIGEN
        }

        `when`("a behandelaar changes the archiefnominatie") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/archiefnominatie",
                requestBodyAsString = """{ "archiefnominatie": "$newArchiefnominatie", "reden": "$REDEN" }""",
                testUser = BEHANDELAAR_1
            )

            then("the request is refused") {
                logger.info { "Response: ${response.bodyAsString}" }
                response.code shouldBe HTTP_FORBIDDEN
            }
        }

        `when`("a recordmanager changes the archiefnominatie without a reden") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/archiefnominatie",
                requestBodyAsString = """{ "archiefnominatie": "$newArchiefnominatie", "reden": "" }""",
                testUser = RECORDMANAGER_1
            )

            then("the request is refused as invalid") {
                logger.info { "Response: ${response.bodyAsString}" }
                response.code shouldBe HTTP_BAD_REQUEST
            }
        }

        `when`("a recordmanager changes the archiefnominatie with a reden") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/archiefnominatie",
                requestBodyAsString = """{ "archiefnominatie": "$newArchiefnominatie", "reden": "$REDEN" }""",
                testUser = RECORDMANAGER_1
            )

            then("the returned zaak has the new archiefnominatie and the recordmanager may still change it") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody.shouldContainJsonKeyValue("archiefNominatie", newArchiefnominatie)
                responseBody.shouldContainJsonKeyValue("$.rechten.canWijzigenArchief", true)
            }

            and("the zaakhistorie shows the change with the reden") {
                itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/historie",
                    testUser = RECORDMANAGER_1
                ).run {
                    logger.info { "Response: $bodyAsString" }
                    code shouldBe HTTP_OK
                    JSONArray(bodyAsString)
                        .run { (0 until length()).map(::getJSONObject) }
                        .map { it.optString("toelichting") } shouldContain REDEN
                }
            }
        }
    }
})
