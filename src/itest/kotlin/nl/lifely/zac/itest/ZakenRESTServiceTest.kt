/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.lifely.zac.itest.client.assignZaakToGroup
import nl.lifely.zac.itest.client.createZaak
import nl.lifely.zac.itest.config.ItestConfiguration
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_2_IDENTIFICATION
import org.apache.http.HttpStatus
import org.json.JSONObject

const val GROUP_ID_A= "test-group-a"
const val GROUP_ID_THAT_IS_TOO_LONG = "test-group-that-is-way-too-long"
const val GROUP_NAME = "test-group-a-name"

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(1)
class ZakenRESTServiceTest : BehaviorSpec({
    given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the create zaak endpoint is called and the user has permissions for the zaaktype used") {
            then("the response should be a 200") {
                val response = createZaak(GROUP_ID_A, GROUP_NAME)
                response.statusCode shouldBe HttpStatus.SC_OK
                JSONObject(response.text).apply {
                    getJSONObject("zaaktype").getString("identificatie") shouldBe ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE

                    getJSONObject("zaakdata").apply {
                        getString("zaakUUID") shouldNotBe null
                        getString("zaakIdentificatie") shouldBe ZAAK_2_IDENTIFICATION
                    }
                }
            }
        }
    }
    given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the create zaak endpoint is called with a group name that is longer than 24 characters") {
            then("the response should be a 400 invalid request with an expected error message") {
                val response = createZaak(GROUP_ID_THAT_IS_TOO_LONG, GROUP_NAME)
                response.statusCode shouldBe HttpStatus.SC_BAD_REQUEST
                response.text shouldEqualJson """
                        {
                            "classViolations": [],
                            "parameterViolations": [
                                {
                                    "constraintType": "PARAMETER",
                                    "message": "size must be between 0 and 24",
                                    "path": "createZaak.arg0.zaak.groep.id",
                                    "value": "$GROUP_ID_THAT_IS_TOO_LONG"
                                }
                            ],
                            "propertyViolations": [],
                            "returnValueViolations": []
                        }
                """.trimIndent()
            }
        }
    }
    given("ZAC Docker container is running and a zaak has been created") {
        When("the zaak toekennen endpoint is called with a group name that is longer than 24 characters") {
            then("the response should be a 400 invalid request with an expected error message") {
                val response = assignZaakToGroup(GROUP_ID_THAT_IS_TOO_LONG)
                response.statusCode shouldBe HttpStatus.SC_BAD_REQUEST
                response.text shouldEqualJson """
                        {
                            "classViolations": [],
                            "parameterViolations": [
                                {
                                    "constraintType": "PARAMETER",
                                    "message": "size must be between 0 and 24",
                                    "path": "toekennen.arg0.groepId",
                                    "value": "$GROUP_ID_THAT_IS_TOO_LONG"
                                }
                            ],
                            "propertyViolations": [],
                            "returnValueViolations": []
                        }
                """.trimIndent()
            }
        }
    }
})
