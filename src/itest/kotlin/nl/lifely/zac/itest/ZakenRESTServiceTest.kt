/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.createZaakWithGroupNameThatIsTooLong
import org.apache.http.HttpStatus

class ZakenRESTServiceTest : BehaviorSpec({
    given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the create zaak endpoint is called with a group name that is longer than 24 characters") {
            then("the response should be a 400 invalid request with an expected error message") {
                val response = createZaakWithGroupNameThatIsTooLong()
                response.statusCode shouldBe HttpStatus.SC_BAD_REQUEST
                response.text shouldEqualJson """
                        {
                            "classViolations": [],
                            "parameterViolations": [
                                {
                                    "constraintType": "PARAMETER",
                                    "message": "size must be between 0 and 24",
                                    "path": "createZaak.arg0.zaak.groep.id",
                                    "value": "test-group-functioneel-beheerders"
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
