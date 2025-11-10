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
import nl.info.zac.itest.config.ItestConfiguration.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2024_01_01
import nl.info.zac.itest.config.ItestConfiguration.RAADPLEGERS_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_SEARCH
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_DESCRIPTION_1
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
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
        authenticate(BEHANDELAAR_DOMAIN_TEST_1)
    }

    afterSpec {
        // re-authenticate using beheerder since currently subsequent integration tests rely on this user being logged in
        authenticate(BEHEERDER_ELK_ZAAKTYPE)
    }

    Given("A behandelaar is logged in and a zaak exists that has not been assigned yet") {
        lateinit var zaakUuid: UUID
        lateinit var zaakIdentificatie: String
        zacClient.createZaak(
            description = ZAAK_DESCRIPTION_1,
            groupId = RAADPLEGERS_DOMAIN_TEST_1.name,
            groupName = RAADPLEGERS_DOMAIN_TEST_1.description,
            startDate = DATE_TIME_2024_01_01,
            zaakTypeUUID = ZAAKTYPE_TEST_2_UUID
        ).run {
            JSONObject(bodyAsString).run {
                logger.info { "Response: $this" }
                zaakUuid = getString("uuid").run(UUID::fromString)
                zaakIdentificatie = getString("identificatie")
            }
        }
        // assign the zaak to the current user but to a different group
        val zaakAssignToMeFromListReason = "fakeAssignToMeFromListReason"
        itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/zaken/lijst/toekennen/mij",
            requestBodyAsString = """{
                    "zaakUUID" : "$zaakUuid",
                    "groepId" : "${BEHANDELAARS_DOMAIN_TEST_1.name}",
                    "reden" : "$zaakAssignToMeFromListReason"
                }
            """.trimIndent()
        ).run {
            logger.info { "Response: $bodyAsString" }
        }

        When("zaak history is requested") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/historie"
            )

            Then(
                """
                the response should be successful and contain the expected history items of the zaak 
                in reverse chronological order (most recent change first): 
                1. zaak created
                2. zaak assigned to raadplegers group
                3. zaak status changed to 'Intake'
                4. zaak assigned to behandelaar (= currently logged-in user)
                5. zaak unassigned from raadplegers group
                6. zaak assigned to behandelaars group
                """
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    [                    
                       {
                          "actie" : "GEKOPPELD",
                          "attribuutLabel" : "Behandelaar",
                          "door" : "${BEHANDELAAR_DOMAIN_TEST_1.displayName}",
                          "nieuweWaarde" : "${BEHANDELAARS_DOMAIN_TEST_1.description}",
                          "toelichting" : "$zaakAssignToMeFromListReason"
                        }, {
                          "actie" : "ONTKOPPELD",
                          "attribuutLabel" : "Behandelaar",
                          "door" : "${BEHANDELAAR_DOMAIN_TEST_1.displayName}",
                          "oudeWaarde" : "${RAADPLEGERS_DOMAIN_TEST_1.description}",
                          "toelichting" : "$zaakAssignToMeFromListReason"
                        }, {
                          "actie" : "GEKOPPELD",
                          "attribuutLabel" : "Behandelaar",
                          "door" : "${BEHANDELAAR_DOMAIN_TEST_1.displayName}",
                          "nieuweWaarde" : "${BEHANDELAAR_DOMAIN_TEST_1.displayName}",
                          "toelichting" : "$zaakAssignToMeFromListReason"
                        }, {
                          "actie" : "GEWIJZIGD",
                          "attribuutLabel" : "status",
                          "door" : "${BEHANDELAAR_DOMAIN_TEST_1.displayName}",
                          "nieuweWaarde" : "Intake",
                          "toelichting" : "Status gewijzigd"
                        }, {
                          "actie" : "GEKOPPELD",
                          "attribuutLabel" : "Behandelaar",
                          "door" : "${BEHANDELAAR_DOMAIN_TEST_1.displayName}",
                          "nieuweWaarde" : "${RAADPLEGERS_DOMAIN_TEST_1.description}",
                          "toelichting" : "Aanmaken zaak"
                        }, {
                          "actie" : "AANGEMAAKT",
                          "attribuutLabel" : "zaak",
                          "door" : "${BEHANDELAAR_DOMAIN_TEST_1.displayName}",
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
