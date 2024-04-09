/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration
import nl.lifely.zac.itest.config.ItestConfiguration.BETROKKENE_IDENTIFICATIE_TYPE_BSN
import nl.lifely.zac.itest.config.ItestConfiguration.BETROKKENE_TYPE_NATUURLIJK_PERSOON
import nl.lifely.zac.itest.config.ItestConfiguration.ROLTYPE_NAME_BETROKKENE
import nl.lifely.zac.itest.config.ItestConfiguration.ROLTYPE_UUID_BELANGHEBBENDE
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_BETROKKENE_BSN_HENDRIKA_JANSE
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_2_ID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_2_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.zaak1UUID
import nl.lifely.zac.itest.util.WebSocketTestListener
import org.json.JSONArray
import org.json.JSONObject
import org.mockserver.model.HttpStatusCode
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

// private const val EXPECTED_STATUS_CODE_FOR_HEROPENEN = 204

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
class ZakenRESTServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    lateinit var zaak2UUID: UUID

    Given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the create zaak endpoint is called and the user has permissions for the zaaktype used") {
            val response = zacClient.createZaak(
                ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID,
                TEST_GROUP_A_ID,
                TEST_GROUP_A_DESCRIPTION
            )
            Then("the response should be a 200 HTTP response with the created zaak") {
                response.code shouldBe HttpStatusCode.OK_200.code()
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                JSONObject(responseBody).apply {
                    getJSONObject("zaaktype").getString("identificatie") shouldBe ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
                    getJSONObject("zaakdata").apply {
                        getString("zaakUUID") shouldNotBe null
                        getString("zaakIdentificatie") shouldBe ZAAK_2_IDENTIFICATION
                        zaak2UUID = getString("zaakUUID").let(UUID::fromString)
                    }
                }
            }
        }
    }
    Given("A zaak has been created") {
        When("the get zaak endpoint is called") {
            val response = zacClient.retrieveZaak(zaak2UUID)
            Then("the response should be a 200 HTTP response and contain the created zaak") {
                with(response) {
                    code shouldBe HttpStatusCode.OK_200.code()
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    with(JSONObject(responseBody)) {
                        getString("identificatie") shouldBe ZAAK_2_IDENTIFICATION
                        getJSONObject("zaaktype").getString("identificatie") shouldBe ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
                    }
                }
            }
        }
        When("the add betrokkene to zaak endpoint is called with an empty 'rol toelichting'") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/zaken/betrokkene",
                requestBodyAsString = "{\n" +
                    "  \"zaakUUID\": \"$zaak2UUID\",\n" +
                    "  \"roltypeUUID\": \"$ROLTYPE_UUID_BELANGHEBBENDE\",\n" +
                    "  \"roltoelichting\": \"\",\n" +
                    "  \"betrokkeneIdentificatieType\": \"$BETROKKENE_IDENTIFICATIE_TYPE_BSN\",\n" +
                    "  \"betrokkeneIdentificatie\": \"$TEST_BETROKKENE_BSN_HENDRIKA_JANSE\"\n" +
                    "}"
            )
            Then("the response should be a 400 bad request HTTP response") {
                response.code shouldBe HttpStatusCode.BAD_REQUEST_400.code()
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                with(responseBody) {
                    with(JSONObject(this).getJSONArray("parameterViolations")) {
                        length() shouldBe 1
                        getJSONObject(0).apply {
                            getString("message") shouldBe "must not be blank"
                            getString("path") shouldBe "createBetrokken.arg0.roltoelichting"
                        }
                    }
                }
            }
        }
        When("the 'update zaak' endpoint is called where the start and fatal dates are changed") {
            val startDateNew = LocalDate.now()
            val fatalDateNew = startDateNew.plusDays(1)
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaak2UUID",
                requestBodyAsString = "{\n" +
                    "\"zaak\":{\n" +
                    "  \"startdatum\":\"$startDateNew\",\n" +
                    "  \"uiterlijkeEinddatumAfdoening\":\"$fatalDateNew\"\n" +
                    "  },\n" +
                    "  \"reden\":\"dummyReason\"\n" +
                    "}\n"
            )
            Then("the response should be a 200 HTTP response with the changed zaak data") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HttpStatusCode.OK_200.code()
                with(responseBody) {
                    shouldContainJsonKeyValue("uuid", zaak2UUID.toString())
                    shouldContainJsonKeyValue("startdatum", startDateNew.toString())
                    shouldContainJsonKeyValue("uiterlijkeEinddatumAfdoening", fatalDateNew.toString())
                }
            }
        }
        When("the 'assign to zaak' endpoint is called with a group") {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/toekennen",
                requestBodyAsString = "{\n" +
                    "  \"zaakUUID\": \"$zaak1UUID\",\n" +
                    "  \"groepId\": \"$TEST_GROUP_A_ID\",\n" +
                    "  \"reden\": \"dummyReason\"\n" +
                    "}"
            )
            Then("the group should be assigned to the zaak") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true

                with(responseBody) {
                    shouldContainJsonKeyValue("uuid", zaak1UUID.toString())
                    shouldContainJsonKey("groep")
                    JSONObject(this).getJSONObject("groep").apply {
                        getString("id") shouldBe TEST_GROUP_A_ID
                        getString("naam") shouldBe TEST_GROUP_A_DESCRIPTION
                    }
                }
            }
        }
        When("the add betrokkene to zaak endpoint is called with valid data") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/zaken/betrokkene",
                requestBodyAsString = "{\n" +
                    "  \"zaakUUID\": \"$zaak2UUID\",\n" +
                    "  \"roltypeUUID\": \"$ROLTYPE_UUID_BELANGHEBBENDE\",\n" +
                    "  \"roltoelichting\": \"dummyToelichting\",\n" +
                    "  \"betrokkeneIdentificatieType\": \"$BETROKKENE_IDENTIFICATIE_TYPE_BSN\",\n" +
                    "  \"betrokkeneIdentificatie\": \"$TEST_BETROKKENE_BSN_HENDRIKA_JANSE\"\n" +
                    "}"
            )
            Then("the response should be a 200 OK HTTP response") {
                response.code shouldBe HttpStatusCode.OK_200.code()
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                with(responseBody) {
                    shouldContainJsonKeyValue("uuid", zaak2UUID.toString())
                }
            }
        }
    }
    Given("A betrokkene has been added to a zaak") {
        When("the get betrokkene endpoint is called for a zaak") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaak2UUID/betrokkene",
            )
            Then("the response should be a 200 HTTP response with a list consisting of the betrokkene") {
                response.code shouldBe HttpStatusCode.OK_200.code()
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                with(JSONArray(responseBody)) {
                    length() shouldBe 1
                    getJSONObject(0).apply {
                        getString("rolid") shouldNotBe null
                        getString("roltype") shouldBe ROLTYPE_NAME_BETROKKENE
                        getString("roltoelichting") shouldBe "dummyToelichting"
                        getString("type") shouldBe BETROKKENE_TYPE_NATUURLIJK_PERSOON
                        getString("identificatie") shouldBe TEST_BETROKKENE_BSN_HENDRIKA_JANSE
                    }
                }
            }
        }
    }
    Given(
        "Two zaken have been created and a websocket subscription has been created to listen" +
            " for a 'zaken verdelen' screen event which will be sent by the asynchronous 'assign zaken from list' job"
    ) {
        val uniqueResourceId = UUID.randomUUID()
        val websocketListener = WebSocketTestListener(
            textToBeSentOnOpen = "{" +
                "\"subscriptionType\":\"CREATE\"," +
                "\"event\":{" +
                "  \"opcode\":\"UPDATED\"," +
                "  \"objectType\":\"ZAKEN_VERDELEN\"," +
                "  \"objectId\":{" +
                "    \"resource\":\"$uniqueResourceId\"" +
                "  }," +
                "\"_key\":\"ANY;ZAKEN_VERDELEN;$uniqueResourceId\"" +
                "}" +
                "}"
        )
        itestHttpClient.connectNewWebSocket(
            url = ItestConfiguration.ZAC_WEBSOCKET_BASE_URI,
            webSocketListener = websocketListener
        )
        When(
            "the 'assign zaken from list' endpoint is called to start an asynchronous process to assign the two zaken " +
                "to a group and a user using the unique resource ID that was used to create the websocket subscription"
        ) {
            val lijstVerdelenResponse = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zaken/lijst/verdelen",
                requestBodyAsString = "{\n" +
                    "\"uuids\":[\"$zaak1UUID\", \"$zaak2UUID\"],\n" +
                    "\"groepId\":\"$TEST_GROUP_A_ID\",\n" +
                    "\"behandelaarGebruikersnaam\":\"$TEST_USER_2_ID\",\n" +
                    "\"reden\":\"dummyLijstVerdelenReason\",\n" +
                    "\"screenEventResourceId\":\"$uniqueResourceId\"\n" +
                    "}"
            )
            Then(
                "the response should be a 204 HTTP response and eventually a screen event of type 'zaken verdelen' " +
                    "should be received by the websocker listener and the two zaken should be assigned correctly"
            ) {
                val lijstVerdelenResponseBody = lijstVerdelenResponse.body!!.string()
                logger.info { "Response: $lijstVerdelenResponseBody" }
                lijstVerdelenResponse.code shouldBe HttpStatusCode.NO_CONTENT_204.code()
                // the backend process is asynchronous, so we need to wait a bit until the zaken are assigned
                eventually(10.seconds) {
                    websocketListener.messagesReceived.size shouldBe 1
                    with(JSONObject(websocketListener.messagesReceived[0])) {
                        getString("opcode") shouldBe "UPDATED"
                        getString("objectType") shouldBe "ZAKEN_VERDELEN"
                        getJSONObject("objectId").getString("resource") shouldBe uniqueResourceId.toString()
                    }
                    zacClient.retrieveZaak(zaak1UUID).use { response ->
                        response.code shouldBe HttpStatusCode.OK_200.code()
                        with(JSONObject(response.body!!.string())) {
                            getJSONObject("groep").getString("id") shouldBe TEST_GROUP_A_ID
                            getJSONObject("behandelaar").getString("id") shouldBe TEST_USER_2_ID
                        }
                    }
                    zacClient.retrieveZaak(zaak2UUID).use { response ->
                        response.code shouldBe HttpStatusCode.OK_200.code()
                        with(JSONObject(response.body!!.string())) {
                            getJSONObject("groep").getString("id") shouldBe TEST_GROUP_A_ID
                            getJSONObject("behandelaar").getString("id") shouldBe TEST_USER_2_ID
                        }
                    }
                }
            }
        }
    }
    Given("A zaak has not been assigned to the currently logged in user") {
        When("the 'assign to logged-in user from list' endpoint is called for the zaak") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zaken/lijst/toekennen/mij",
                requestBodyAsString = "{\n" +
                    "\"zaakUUID\":\"$zaak1UUID\",\n" +
                    "\"behandelaarGebruikersnaam\":\"$TEST_USER_1_USERNAME\",\n" +
                    "\"reden\":\"dummyAssignToMeFromListReason\"\n" +
                    "}"
            )
            Then(
                "the response should be a 200 HTTP response with zaak data and the zaak should be assigned to the user"
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HttpStatusCode.OK_200.code()
                with(responseBody) {
                    shouldContainJsonKeyValue("uuid", zaak1UUID.toString())
                    JSONObject(this).getJSONObject("behandelaar").apply {
                        getString("id") shouldBe TEST_USER_1_USERNAME
                        getString("naam") shouldBe TEST_USER_1_NAME
                    }
                }
                with(zacClient.retrieveZaak(zaak1UUID)) {
                    code shouldBe HttpStatusCode.OK_200.code()
                    JSONObject(body!!.string()).apply {
                        getJSONObject("behandelaar").apply {
                            getString("id") shouldBe TEST_USER_1_USERNAME
                            getString("naam") shouldBe TEST_USER_1_NAME
                        }
                    }
                }
            }
        }
    }
    Given("Zaken have been assigned to a user") {
        When("the 'lijst vrijgeven' endpoint is called for the zaken") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zaken/lijst/vrijgeven",
                requestBodyAsString = "{\n" +
                    "\"uuids\":[\"$zaak1UUID\", \"$zaak2UUID\"],\n" +
                    "\"reden\":\"dummyLijstVrijgevenReason\"\n" +
                    "}"
            )
            Then(
                "the response should be a 204 HTTP response and the zaak should be unassigned from the user " +
                    "but should still be assigned to the group"
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HttpStatusCode.NO_CONTENT_204.code()
                with(zacClient.retrieveZaak(zaak1UUID)) {
                    code shouldBe HttpStatusCode.OK_200.code()
                    JSONObject(body!!.string()).apply {
                        getJSONObject("groep").apply {
                            getString("id") shouldBe TEST_GROUP_A_ID
                            getString("naam") shouldBe TEST_GROUP_A_DESCRIPTION
                        }
                        has("behandelaar") shouldBe false
                    }
                }
                with(zacClient.retrieveZaak(zaak2UUID)) {
                    code shouldBe HttpStatusCode.OK_200.code()
                    JSONObject(body!!.string()).apply {
                        getJSONObject("groep").apply {
                            getString("id") shouldBe TEST_GROUP_A_ID
                            getString("naam") shouldBe TEST_GROUP_A_DESCRIPTION
                        }
                        has("behandelaar") shouldBe false
                    }
                }
            }
        }
    }
//    Given("A zaak that is ontvankelijk") {
//        lateinit var uuid: UUID
//        var intakeId: Int
//        lateinit var resultaatUuid: UUID
//
//        with(
//            zacClient.createZaak(
//                ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID,
//                TEST_GROUP_A_ID,
//                TEST_GROUP_A_DESCRIPTION
//            )
//        ) {
//            with(JSONObject(body!!.string())) {
//                getJSONObject("zaakdata").apply {
//                    uuid = getString("zaakUUID").let(UUID::fromString)
//                }
//            }
//        }
//
//        with(itestHttpClient.performGetRequest("$ZAC_API_URI/planitems/zaak/$uuid/userEventListenerPlanItems")) {
//            with(JSONArray(body!!.string()).getJSONObject(0)) {
//                intakeId = getString("id").toInt()
//            }
//        }
//
//        with(
//            itestHttpClient.performJSONPostRequest(
//                "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
//                requestBodyAsString = """
//            {
//                "zaakUuid":"$uuid",
//                "planItemInstanceId":"$intakeId",
//                "actie":"INTAKE_AFRONDEN",
//                "zaakOntvankelijk":true,
//                "resultaatToelichting":"intake"
//            }
//                """.trimIndent()
//            )
//        ) {
//            logger.info { "--- intake afronden status code: $code ---" }
//        }
//
//        with(
//            itestHttpClient.performGetRequest(
//                "$ZAC_API_URI/zaken/resultaattypes/$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID"
//            )
//        ) {
//            with(JSONArray(body!!.string()).getJSONObject(0)) {
//                resultaatUuid = getString("id").let(UUID::fromString)
//            }
//        }
//
//        When("The zaak is closed") {
//            var afhandelenId: Int
//
//            with(itestHttpClient.performGetRequest("$ZAC_API_URI/planitems/zaak/$uuid/userEventListenerPlanItems")) {
//                with(JSONArray(body!!.string()).getJSONObject(0)) {
//                    afhandelenId = getString("id").toInt()
//                }
//            }
//
//            with(
//                itestHttpClient.performJSONPostRequest(
//                    "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
//                    requestBodyAsString = """
//            {
//                "zaakUuid":"$uuid",
//                "planItemInstanceId":"$afhandelenId",
//                "actie":"ZAAK_AFHANDELEN",
//                "zaakOntvankelijk":true,
//                "resultaattypeUuid": "$resultaatUuid",
//                "resultaatToelichting":"afronden"
//            }
//                    """.trimIndent()
//                )
//            ) {
//                logger.info { "--- afronden planItem status code: $code ---" }
//            }
//
//            with(
//                itestHttpClient.performPatchRequest(
//                    "$ZAC_API_URI/zaken/zaak/$uuid/afsluiten",
//                    requestBodyAsString = """
//                {"reden":"dummyReason","resultaattypeUuid":"$resultaatUuid"}
//                    """.trimIndent()
//                )
//            ) {
//                logger.info { "--- afsluiten zaak status code: $code ---" }
//            }
//
//            Then("The zaak should have a resultaat") {
//                with(zacClient.retrieveZaak(uuid)) {
//                    code shouldBe HttpStatusCode.OK_200.code()
//                    val bodyStr = body!!.string()
//                    logger.info { "--- zaak body after afronden: $bodyStr ---" }
//                    JSONObject(bodyStr).apply {
//                        has("resultaat") shouldBe true
//                    }
//                }
//            }
//        }
//
//        When("The zaak is re-opened") {
//            KeycloakClient.authenticate(TEST_RECORD_MANAGER_1_USERNAME, TEST_RECORD_MANAGER_1_PASSWORD)
//
//            with(
//                itestHttpClient.performPatchRequest(
//                    "$ZAC_API_URI/zaken/zaak/$uuid/heropenen",
//                    requestBodyAsString = """
//            {"reden":"dummyReason"}
//                    """.trimIndent()
//                )
//            ) {
//                logger.info { "--- Heropenen status code: $code ---" }
//                if (code > EXPECTED_STATUS_CODE_FOR_HEROPENEN) {
//                    val bodyStr = body!!.string()
//                    logger.info { "--- Heropenen error body: $bodyStr ---" }
//                }
//            }
//
//            // re-authenticate with the default user
//            KeycloakClient.authenticate()
//
//            Then("The zaak should not have a resultaat") {
//                with(zacClient.retrieveZaak(uuid)) {
//                    code shouldBe HttpStatusCode.OK_200.code()
//                    JSONObject(body!!.string()).apply {
//                        has("resultaat") shouldBe false
//                    }
//                }
//            }
//        }
//    }
})
