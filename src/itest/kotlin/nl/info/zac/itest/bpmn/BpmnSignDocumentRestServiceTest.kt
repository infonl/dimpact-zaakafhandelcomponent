/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_DOCUMENT_SIGN_SELECT_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.BPMN_DOCUMENT_SIGN_SUMMARY_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.PDF_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Integration test for the sign document BPMN process.
 * Creates a zaak, uploads a document, runs through the sign document process,
 * and verifies that the document is signed after the service task executes.
 */
class BpmnSignDocumentRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()

    val afterThirtySeconds = eventuallyConfig {
        duration = 30.seconds
        interval = 500.milliseconds
    }

    Given("A BPMN sign document zaak exists with an uploaded document") {
        var zaakUuid: UUID
        var zaakIdentificatie: String
        lateinit var documentUuid: UUID

        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_BPMN_TEST_3_UUID,
            groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
            groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_DOMAIN_TEST_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
            JSONObject(responseBody).run {
                getJSONObject("zaakdata").run {
                    zaakUuid = getString("zaakUUID").run(UUID::fromString)
                    zaakIdentificatie = getString("zaakIdentificatie")
                }
            }
        }

        zacClient.createEnkelvoudigInformatieobjectForZaak(
            zaakUUID = zaakUuid,
            fileName = TEST_PDF_FILE_NAME,
            fileMediaType = PDF_MIME_TYPE,
            vertrouwelijkheidaanduiding = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR,
            testUser = BEHANDELAAR_DOMAIN_TEST_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
            documentUuid = UUID.fromString(JSONObject(responseBody).getString("uuid"))
        }

        When("the select documents form is submitted with the document UUID") {
            val takenPatchResponse = zacClient.submitFormData(
                bpmnZaakUuid = zaakUuid,
                taakData = """{ "ZAAK_Documenten_Ondertekenen_Selectie": ["$documentUuid"] }""",
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("the select documents task is completed") {
                JSONObject(takenPatchResponse).getString("status") shouldBe "AFGEROND"
            }

            And("the select documents task is removed from the task list") {
                eventually(10.seconds) {
                    val searchResponseBody = zacClient.searchForTasks(
                        zaakIdentificatie = zaakIdentificatie,
                        taskName = BPMN_DOCUMENT_SIGN_SELECT_TASK_NAME,
                        testUser = BEHANDELAAR_DOMAIN_TEST_1
                    )
                    JSONObject(searchResponseBody).getInt("totaal") shouldBe 0
                }
            }

            And("the summary task becomes available") {
                eventually(afterThirtySeconds) {
                    val searchResponseBody = zacClient.searchForTasks(
                        zaakIdentificatie = zaakIdentificatie,
                        taskName = BPMN_DOCUMENT_SIGN_SUMMARY_TASK_NAME,
                        testUser = BEHANDELAAR_DOMAIN_TEST_1
                    )
                    JSONObject(searchResponseBody).getInt("totaal") shouldBe 1
                }
            }
        }

        When("the summary form is submitted") {
            val takenPatchResponse = zacClient.submitFormData(
                bpmnZaakUuid = zaakUuid,
                taakData = """{ "ZAAK_Documenten_Ondertekenen_Selectie": ["$documentUuid"] }""",
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("the summary task is completed") {
                JSONObject(takenPatchResponse).getString("status") shouldBe "AFGEROND"
            }

            And("the document should be signed") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/informatieobjecten/informatieobject/$documentUuid",
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                JSONObject(responseBody).getJSONArray("indicaties")[0] shouldBe "ONDERTEKEND"
            }
        }
    }
})
