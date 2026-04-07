/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.DocumentHelper
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.OpenZaakClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class InboxDocumentRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val openZaakClient = OpenZaakClient(itestHttpClient)
    val documentHelper = DocumentHelper(zacClient)

    Given("a document is created directly in Open Zaak without being linked to a zaak") {
        val uniqueTitle = "inbox-doc-itest-${UUID.randomUUID()}"
        val createResponse = openZaakClient.createEnkelvoudigInformatieobject(
            fileName = TEST_PDF_FILE_NAME,
            title = uniqueTitle
        )
        logger.info { "createEnkelvoudigInformatieobject response: ${createResponse.bodyAsString}" }
        createResponse.code shouldBe HTTP_CREATED
        val documentUuid = JSONObject(createResponse.bodyAsString).getString("uuid").run(UUID::fromString)

        When("a create notification is sent to ZAC for that document") {
            documentHelper.sendEnkelvoudigInformatieobjectCreateNotification(documentUuid)

            Then("the document should appear in the inbox documents list") {
                eventually(10.seconds) {
                    val listResponse = itestHttpClient.performPutRequest(
                        url = "$ZAC_API_URI/inboxdocumenten",
                        requestBodyAsString = JSONObject(
                            mapOf(
                                "page" to 0,
                                "maxResults" to 10,
                                "titel" to uniqueTitle
                            )
                        ).toString(),
                        testUser = BEHANDELAAR_DOMAIN_TEST_1
                    )
                    listResponse.code shouldBe HTTP_OK
                    with(JSONObject(listResponse.bodyAsString)) {
                        getInt("totaal") shouldBe 1
                        getJSONArray("resultaten").getJSONObject(0).apply {
                            getString("enkelvoudiginformatieobjectUUID") shouldBe documentUuid.toString()
                            getString("titel") shouldBe uniqueTitle
                        }
                    }
                }
            }
        }
    }
})
