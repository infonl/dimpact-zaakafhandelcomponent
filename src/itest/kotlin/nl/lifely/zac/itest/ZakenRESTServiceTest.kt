/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration.BETROKKENE_IDENTIFICATIE_TYPE_BSN
import nl.lifely.zac.itest.config.ItestConfiguration.BETROKKENE_TYPE_NATUURLIJK_PERSOON
import nl.lifely.zac.itest.config.ItestConfiguration.ROLTYPE_NAME_BETROKKENE
import nl.lifely.zac.itest.config.ItestConfiguration.ROLTYPE_UUID_BELANGHEBBENDE
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_BETROKKENE_BSN_HENDRIKA_JANSE
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_2_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_2_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_2_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONArray
import org.json.JSONObject
import org.mockserver.model.HttpStatusCode
import java.util.*

private val itestHttpClient = ItestHttpClient()
private val zacClient = ZacClient()
private val logger = KotlinLogging.logger {}

lateinit var zaak2UUID: UUID

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
class ZakenRESTServiceTest : BehaviorSpec({
    Given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the create zaak endpoint is called and the user has permissions for the zaaktype used") {
            val response = zacClient.createZaak(
                ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID,
                TEST_GROUP_A_ID,
                TEST_GROUP_A_DESCRIPTION
            )
            Then("the response should be a 200 HTTP response with the created zaak") {
                response.code shouldBe HttpStatusCode.OK_200.code()
                JSONObject(response.body!!.string()).apply {
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
    }
    Given("A zaak has been created") {
        When("the add betrokkene to zaak endpoint is called") {
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
            Then("the response should be a 200 HTTP response") {
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
    Given("A zaak has been created") {
        When("the assign to zaak endpoint is called with a group") {
            val response = itestHttpClient.performPatchRequest(
                url = "${ZAC_API_URI}/zaken/toekennen",
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
    }
    Given("Two zaken have been created") {
        When("the 'lijst verdelen' endpoint is called to assign the two zaken to a group and a user") {
            val response = itestHttpClient.performPutRequest(
                url = "${ZAC_API_URI}/zaken/lijst/verdelen",
                requestBodyAsString = "{\n" +
                    "\"uuids\":[\"$zaak1UUID\", \"$zaak2UUID\"],\n" +
                    "\"groepId\":\"$TEST_GROUP_A_ID\",\n" +
                    "\"behandelaarGebruikersnaam\":\"$TEST_USER_2_ID\",\n" +
                    "\"reden\":\"dummyLijstVerdelenReason\"\n" +
                    "}"
            )
            Then("the response should be a 204 HTTP response and the zaken should be assigned correctly") {
                response.code shouldBe HttpStatusCode.NO_CONTENT_204.code()
                with(zacClient.retrieveZaak(zaak1UUID)) {
                    code shouldBe HttpStatusCode.OK_200.code()
                    JSONObject(body!!.string()).apply {
                        getJSONObject("groep").apply {
                            getString("id") shouldBe TEST_GROUP_A_ID
                            getString("naam") shouldBe TEST_GROUP_A_DESCRIPTION
                        }
                        getJSONObject("behandelaar").apply {
                            getString("id") shouldBe TEST_USER_2_ID
                            getString("naam") shouldBe TEST_USER_2_NAME
                        }
                    }
                }
                with(zacClient.retrieveZaak(zaak2UUID)) {
                    code shouldBe HttpStatusCode.OK_200.code()
                    JSONObject(body!!.string()).apply {
                        getJSONObject("groep").apply {
                            getString("id") shouldBe TEST_GROUP_A_ID
                            getString("naam") shouldBe TEST_GROUP_A_DESCRIPTION
                        }
                        getJSONObject("behandelaar").apply {
                            getString("id") shouldBe TEST_USER_2_ID
                            getString("naam") shouldBe TEST_USER_2_NAME
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
                    "\"behandelaarGebruikersnaam\":\"$TEST_USER_1_ID\",\n" +
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
                        getString("id") shouldBe TEST_USER_1_ID
                        getString("naam") shouldBe TEST_USER_1_NAME
                    }
                }
                with(zacClient.retrieveZaak(zaak1UUID)) {
                    code shouldBe HttpStatusCode.OK_200.code()
                    JSONObject(body!!.string()).apply {
                        getJSONObject("behandelaar").apply {
                            getString("id") shouldBe TEST_USER_1_ID
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
                url = "$ZAC_API_URI/zaken/lijst/vrijgeven\n",
                requestBodyAsString = "{\n" +
                    "\"uuids\":[\"$zaak1UUID\", \"$zaak2UUID\"],\n" +
                    "\"reden\":\"dummyLijstVrijgevenReason\"\n" +
                    "}"
            )
            Then(
                "the response should be a 204 HTTP response and the zaak should be unassigned from the user " +
                    "but should still be assigned to the group"
            ) {
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
})
