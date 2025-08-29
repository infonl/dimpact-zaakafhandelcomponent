/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2024_01_01
import nl.info.zac.itest.config.ItestConfiguration.TEST_BEHANDELAAR_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_BEHANDELAAR_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.TEST_BEHANDELAAR_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_BEHANDELAARS_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_BEHANDELAARS_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_SEARCH
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_DESCRIPTION_1
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import org.json.JSONObject
import java.util.UUID

/**
 * This test creates a zaak and uploads a document and because we do not want this test
 * to impact e.g. [SearchRestServiceTest] we run it afterward.
 */
@Order(TEST_SPEC_ORDER_AFTER_SEARCH)
class ZaakRestServiceHistoryTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()

    beforeSpec {
        authenticate(username = TEST_BEHANDELAAR_1_USERNAME, password = TEST_BEHANDELAAR_1_PASSWORD)
    }

    afterSpec {
        // re-authenticate using testuser1 since currently subsequent integration tests rely on this user being logged in
        authenticate(username = TEST_USER_1_USERNAME, password = TEST_USER_1_PASSWORD)
    }

    Given("A behandelaar is logged in and a zaak exists that has been assigned to the currently logged-in user") {
        lateinit var zaakUuid: UUID
        lateinit var zaakIdentificatie: String
        zacClient.createZaak(
            description = ZAAK_DESCRIPTION_1,
            groupId = TEST_GROUP_A_ID,
            groupName = TEST_GROUP_A_DESCRIPTION,
            startDate = DATE_TIME_2024_01_01,
            zaakTypeUUID = ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
        ).run {
            JSONObject(body.string()).run {
                logger.info { "Response: $this" }
                zaakUuid = getString("uuid").run(UUID::fromString)
                zaakIdentificatie = getString("identificatie")
            }
        }
        // assign the zaak to the current user and a different group
        val zaakAssignToMeFromListReason = "fakeAssignToMeFromListReason"
        itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/zaken/lijst/toekennen/mij",
            requestBodyAsString = """{
                    "zaakUUID" : "$zaakUuid",
                    "groepId" : "$TEST_GROUP_BEHANDELAARS_ID",
                    "reden" : "$zaakAssignToMeFromListReason"
                }
            """.trimIndent()
        ).run {
            logger.info { "Response: ${body.string()}" }
        }

        When("zaak history is requested") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/historie"
            )

            Then("the response should be ok") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    [                    
                       {
                          "actie" : "GEKOPPELD",
                          "attribuutLabel" : "Behandelaar",
                          "door" : "$TEST_BEHANDELAAR_1_NAME",
                          "nieuweWaarde" : "$TEST_GROUP_BEHANDELAARS_DESCRIPTION",
                          "toelichting" : "$zaakAssignToMeFromListReason"
                        }, {
                          "actie" : "ONTKOPPELD",
                          "attribuutLabel" : "Behandelaar",
                          "door" : "$TEST_BEHANDELAAR_1_NAME",
                          "oudeWaarde" : "$TEST_GROUP_A_DESCRIPTION",
                          "toelichting" : "$zaakAssignToMeFromListReason"
                        }, {
                          "actie" : "GEKOPPELD",
                          "attribuutLabel" : "Behandelaar",
                          "door" : "$TEST_BEHANDELAAR_1_NAME",
                          "nieuweWaarde" : "$TEST_BEHANDELAAR_1_NAME",
                          "toelichting" : "$zaakAssignToMeFromListReason"
                        }, {
                          "actie" : "GEWIJZIGD",
                          "attribuutLabel" : "status",
                          "door" : "$TEST_BEHANDELAAR_1_NAME",
                          "nieuweWaarde" : "Intake",
                          "toelichting" : "Status gewijzigd"
                        }, {
                          "actie" : "GEKOPPELD",
                          "attribuutLabel" : "Behandelaar",
                          "door" : "$TEST_BEHANDELAAR_1_NAME",
                          "nieuweWaarde" : "$TEST_GROUP_A_DESCRIPTION",
                          "toelichting" : "Aanmaken zaak"
                        }, {
                          "actie" : "AANGEMAAKT",
                          "attribuutLabel" : "zaak",
                          "door" : "$TEST_BEHANDELAAR_1_NAME",
                          "nieuweWaarde" : "$zaakIdentificatie",
                          "toelichting" : "null"
                        }              
                  ]
                """.trimIndent()
                responseBody shouldContainJsonKey("$[0].datumTijd")
            }
        }
    }
})
