/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_OK

class BpmnProcessDefinitionRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    given(
        """BPMN process definitions have been created in ZAC in the integration test setup phase
            and a beheerder is logged in"""
    ) {
        `when`("the process definitions are retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/bpmn-process-definitions",
                testUser = BEHEERDER_1
            )
            then("the response contains the BPMN process definitions that were just created") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                 [
                  {
                    "key": "itProcessDefinition",
                    "name": "Integration Tests BPMN Process Definition",
                    "version": 1
                  },
                  {
                    "key": "permissionCheckProcess",
                    "name": "Permission Check Process",
                    "version": 1
                  },
                  {
                    "key": "sendConfirmationEmailAndSignDocumentsProcess",
                    "name": "Send Confirmation Email And Sign Documents Process",
                    "version": 1
                  },
                  {
                    "key": "suspendResume",
                    "name": "Suspend & Resume",
                    "version": 1
                  },
                  {
                    "key": "userManagement",
                    "name": "User Management",
                    "version": 1
                  }
                ]
                """.trimIndent()
            }
        }
    }

    given(
        """BPMN process definitions have been created in ZAC in the integration test setup phase
            and a beheerder is logged in"""
    ) {
        `when`("the process definitions are retrieved with details") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/bpmn-process-definitions?details=true",
                testUser = BEHEERDER_1
            )
            then("the response contains the BPMN process definitions that were just created") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                 [
                  {
                    "key": "itProcessDefinition",
                    "name": "Integration Tests BPMN Process Definition",
                    "version": 1,
                    "details": {
                      "inUse": true,
                      "documentation": "Simple BPMN process definition to test various functionalities. Used in ZAC integration tests.",
                      "forms": [
                        {
                          "formKey": "summaryForm",
                          "title": "Summary form",
                          "uploaded": true
                        },
                        {
                          "formKey": "testForm",
                          "title": "Test form",
                          "uploaded": true
                        }
                      ],
                      "orphanedForms": []
                    }
                  },
                  {
                    "key": "permissionCheckProcess",
                    "name": "Permission Check Process",
                    "version": 1,
                    "details": {
                      "inUse": true,
                      "documentation": "Integration Test Process To Check Permissions",
                      "forms": [
                        {
                          "formKey": "chooseTestProcess",
                          "title": "Choose Test Process",
                          "uploaded": true
                        }
                      ],
                      "orphanedForms": []
                    }
                  },
                  {
                    "key": "sendConfirmationEmailAndSignDocumentsProcess",
                    "name": "Send Confirmation Email And Sign Documents Process",
                    "version": 1,
                    "details": {
                      "inUse": true,
                      "forms": [
                        {
                          "formKey": "selectDocumentsForm",
                          "title": "SelectDocumentsForm",
                          "uploaded": true
                        },
                        {
                          "formKey": "signDocumentForm",
                          "title": "signDocumentForm",
                          "uploaded": true
                        }
                      ],
                      "orphanedForms": []
                    }
                  },
                  {
                    "key": "suspendResume",
                    "name": "Suspend & Resume",
                    "version": 1,
                    "details": {
                      "inUse": true,
                      "forms": [
                        {
                          "formKey": "suspendForm",
                          "title": "Suspend form",
                          "uploaded": true
                        },
                        {
                          "formKey": "resumeForm",
                          "title": "Resume form",
                          "uploaded": true
                        },
                        {
                          "formKey": "extendForm",
                          "title": "Extend form",
                          "uploaded": true
                        }
                      ],
                      "orphanedForms": []
                    }
                  },
                  {
                    "key": "userManagement",
                    "name": "User Management",
                    "version": 1,
                    "details": {
                      "inUse": true,
                      "forms": [
                        {
                          "formKey": "zaakDefaults",
                          "title": "Zaak defaults",
                          "uploaded": true
                        },
                        {
                          "formKey": "hardCoded",
                          "title": "Hard-coded",
                          "uploaded": true
                        },
                        {
                          "formKey": "userGroupSelection",
                          "title": "User and group selection",
                          "uploaded": true
                        },
                        {
                          "formKey": "newZaakDefaults",
                          "title": "New Zaak Defaults",
                          "uploaded": true
                        },
                        {
                          "formKey": "copyUserGroup",
                          "title": "Copy user and group",
                          "uploaded": true
                        }
                      ],
                      "orphanedForms": []
                    }
                  }
                ]
                """.trimIndent()
            }
        }
    }

    given(
        "The in-use process definition 'itProcessDefinition' exists and a beheerder is logged in"
    ) {
        `when`("the process definition 'itProcessDefinition' is attempted to be deleted") {
            val response = itestHttpClient.performDeleteRequest(
                url = "$ZAC_API_URI/bpmn-process-definitions/itProcessDefinition",
                testUser = BEHEERDER_1
            )
            then("the response contains Bad Request") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_BAD_REQUEST
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    {
                      "message": "BPMN process definition 'itProcessDefinition' cannot be deleted as it is in use"
                    }
                """.trimIndent()
            }
        }
    }
})
