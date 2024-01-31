/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.assignZaakToGroup
import nl.lifely.zac.itest.client.createZaak
import org.apache.http.HttpStatus

const val GROUP_ID_THAT_IS_TOO_LONG = "test-group-that-is-way-too-long"
const val GROUP_NAME = "test-group-name"

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(1)
class ZakenRESTServiceTest : BehaviorSpec({
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
    // test for Intake afronden
    // POST on planitems/doUserEventListenerPlanItem met payload:
    // {"zaakUuid":"626a8987-14be-4b1f-b63e-17375b382eb3","planItemInstanceId":"2546",
    // "actie":"INTAKE_AFRONDEN","zaakOntvankelijk":true,"resultaatToelichting":""}

    //  test for Besluit toevoegen
    // POST on zaken/besluit met payload:
    // {"zaakUuid":"626a8987-14be-4b1f-b63e-17375b382eb3","resultaattypeUuid":"b0ed0590-a1fe-4448-9f9a-9e8e848be727",
    // "besluittypeUuid":"1a282535-09cc-480c-a5cf-cef0a76a1c5b","toelichting":"Dummy toelichting",
    // "ingangsdatum":"2024-01-30T14:54:53+01:00","vervaldatum":null,"informatieobjecten":[]}
})
