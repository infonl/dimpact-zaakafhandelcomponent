/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.OpenZaakClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.createZaakAndRetrieve
import nl.info.zac.itest.config.BEHANDELAAR_1_EN_BRP_ZOEKER_2
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2024_01_31
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_EXTERNAL_URI
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_NAME_BEHANDELAAR
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.Headers
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Isolate
class SignaleringRestServiceRolDestroyTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val openZaakClient = OpenZaakClient(itestHttpClient)
    val afterThirtySeconds = eventuallyConfig {
        duration = 30.seconds
        interval = 500.milliseconds
    }
    val behandelaar = BEHANDELAAR_1_EN_BRP_ZOEKER_2

    fun sendRolNotification(action: String, zaakUUID: UUID, rolUrl: String) =
        itestHttpClient.performJSONPostRequest(
            url = "$ZAC_API_URI/notificaties",
            headers = Headers.headersOf(
                "Content-Type",
                "application/json",
                "Authorization",
                ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
            ),
            requestBodyAsString = JSONObject(
                mapOf(
                    "actie" to action,
                    "kanaal" to "zaken",
                    "resource" to "rol",
                    "hoofdObject" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakUUID",
                    "resourceUrl" to rolUrl,
                    "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                )
            ).toString()
        )

    fun readLatestSignalering() =
        itestHttpClient.performGetRequest(url = "$ZAC_API_URI/signaleringen/latest", testUser = behandelaar)

    given("a behandelaar with a ZAAK_OP_NAAM signalering for a zaak assigned to them") {
        itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/signaleringen/instellingen",
            headers = Headers.headersOf("Content-Type", "application/json"),
            requestBodyAsString =
                """{"isDashboardEnabled":true,"isMailEnabled":false,"subjecttype":"ZAAK","type":"ZAAK_OP_NAAM"}""",
            testUser = behandelaar
        ).code shouldBe HTTP_OK
        val zaakUUID = zacClient.createZaakAndRetrieve(
            zaakTypeUUID = ZAAKTYPE_CMMN_TEST_2_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            behandelaarId = behandelaar.username,
            startDate = DATE_TIME_2024_01_31,
            testUser = behandelaar
        ).run {
            code shouldBe HTTP_OK
            bodyAsString.let(::JSONObject).getString("uuid").let(UUID::fromString)
        }
        val behandelaarRolUrl = openZaakClient.getRolesForZaak(zaakUUID).bodyAsString
            .let(::JSONObject)
            .getJSONArray("results")
            .let { rollen -> (0 until rollen.length()).map(rollen::getJSONObject) }
            .single { it.getString("omschrijving") == ROLTYPE_NAME_BEHANDELAAR && it.getString("betrokkeneType") == "medewerker" }
            .getString("url")
            .replace(OPEN_ZAAK_EXTERNAL_URI, OPEN_ZAAK_BASE_URI)
        sendRolNotification(action = "create", zaakUUID = zaakUUID, rolUrl = behandelaarRolUrl).code shouldBe HTTP_NO_CONTENT
        eventually(afterThirtySeconds) {
            readLatestSignalering().code shouldBe HTTP_OK
        }

        `when`("the behandelaar is removed from the zaak and ZAC receives the notification that the rol was deleted") {
            itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/toekennen",
                requestBodyAsString = """
                    {
                        "zaakUUID": "$zaakUUID",
                        "groepId": "${GROUP_BEHANDELAARS_TEST_1.name}",
                        "behandelaarGebruikersnaam": "",
                        "reden": "fakeReason"
                    }
                """.trimIndent(),
                testUser = behandelaar
            ).code shouldBe HTTP_OK
            val response = sendRolNotification(action = "destroy", zaakUUID = zaakUUID, rolUrl = behandelaarRolUrl)

            then("the signalering of the previous behandelaar is deleted without listing the dashboard card") {
                response.code shouldBe HTTP_NO_CONTENT
                readLatestSignalering().code shouldBe HTTP_NO_CONTENT
            }
            and("the dashboard card of the previous behandelaar no longer lists the zaak") {
                val listResponse = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/signaleringen/zaken/ZAAK_OP_NAAM",
                    requestBodyAsString = """
                        { "page": 0, "rows": 5, "sortField": "SIGNALERING_TIJDSTIP", "sortOrder": "DESC" }
                    """.trimIndent(),
                    testUser = behandelaar
                )
                val responseBody = listResponse.bodyAsString
                logger.info { "Response: $responseBody" }
                listResponse.code shouldBe HTTP_OK
                responseBody.shouldContainJsonKeyValue("totaal", 0)
            }
        }
    }
})
