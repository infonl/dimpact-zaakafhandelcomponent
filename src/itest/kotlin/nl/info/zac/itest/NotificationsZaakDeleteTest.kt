/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_NO_CONTENT
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_REINDEXING
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import okhttp3.Headers
import org.json.JSONObject

/**
 * This test creates a zaak and a document (the form data PDF) which we use in other tests, and therefore we run this test first.
 */
@Order(TEST_SPEC_ORDER_AFTER_REINDEXING)
class NotificationsZaakDeleteTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    // TODO: ideally we want the situation where the zaak no longer exists in OpenZaak..
    Given("Existing zaak process data and Solr index data for a specific zaak in ZAC") {
        When("the notificaties endpoint is called with a 'zaak destroy' payload") {
//            val response = itestHttpClient.performJSONPostRequest(
//                url = "$ZAC_API_URI/notificaties",
//                headers = Headers.headersOf(
//                    "Content-Type",
//                    "application/json",
//                    // this test simulates that Open Notificaties sends the request to ZAC
//                    // using the secret API key that is configured in ZAC
//                    "Authorization",
//                    OPEN_NOTIFICATIONS_API_SECRET_KEY
//                ),
//                requestBodyAsString = JSONObject(
//                    mapOf(
//                        "kanaal" to "zaken",
//                        "resource" to "zaak",
//                        "resourceUrl" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakProductaanvraag1Uuid",
//                        "actie" to "destroy"
//                    )
//                ).toString(),
//                addAuthorizationHeader = false
 //           )
            Then(
                """the response should be 'no content', a zaak should be created in OpenZaak
                        and a zaak productaanvraag proces of type 'Productaanvraag-Dimpact' should be started in ZAC"""
            ) {
                //response.code shouldBe HTTP_STATUS_NO_CONTENT
            }
        }
    }
})
