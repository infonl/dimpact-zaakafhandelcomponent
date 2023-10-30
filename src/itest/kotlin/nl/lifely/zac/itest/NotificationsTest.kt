/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.provided.OBJECTS_API_HOSTNAME_URL
import io.kotest.provided.ProjectConfig
import nl.lifely.zac.itest.config.ZACContainer
import org.json.JSONObject
import java.time.ZoneId
import java.time.ZonedDateTime

private val logger = KotlinLogging.logger {}

const val OBJECTTYPE_UUID_PRODUCTAANVRAAG_DENHAAG = "021f685e-9482-4620-b157-34cd4003da6b"
const val OBJECT_UUID_PRODUCTAANVRAAG = "9dbed186-89ca-48d7-8c6c-f9995ceb8e27"

class NotificationsTest : BehaviorSpec({
    given("ZAC and all related Docker containers are running") {
        When("the notificaties endpoint is called with dummy payload without authentication header") {
            then("the response should be forbidden") {
                khttp.post(
                    url = "${ProjectConfig.zacContainer.apiUrl}/notificaties",
                    headers = mapOf("Content-Type" to "application/json"),
                    data = JSONObject(
                        mapOf(
                            "dummy" to "dummy"
                        )
                    )
                ).apply {
                    statusCode shouldBe HttpStatus.SC_FORBIDDEN
                }
            }
        }
    }
    given(
        "ZAC and all related Docker containers are running, productaanvraag object exists in Objecten API " +
            "and productaanvraag PDF exists in Open Zaak"
    ) {
        When("the notificaties endpoint is called with a 'create productaanvraag' payload with authentication header") {
            then(
                "the response should be 'no content', a zaak should be created in OpenZaak " +
                    "and a zaak productaanvraag proces of type 'Productaanvraag-Denhaag' should be started in ZAC"
            ) {
                khttp.post(
                    url = "${ProjectConfig.zacContainer.apiUrl}/notificaties",
                    headers = mapOf(
                        "Content-Type" to "application/json",
                        "Authorization" to ZACContainer.OPEN_NOTIFICATIONS_API_SECRET_KEY
                    ),
                    data = JSONObject(
                        mapOf(
                            "resource" to "object",
                            "resourceUrl" to "$OBJECTS_API_HOSTNAME_URL/$OBJECT_UUID_PRODUCTAANVRAAG",
                            "actie" to "create",
                            "kenmerken" to mapOf(
                                "objectType" to "$OBJECTS_API_HOSTNAME_URL/$OBJECTTYPE_UUID_PRODUCTAANVRAAG_DENHAAG"
                            ),
                            "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                        )
                    )
                ).apply {
                    // check if zaak was created in OpenZaak and if CMMN zaak proces was started in ZAC (how?)

                    // GET /zaken/zaak/<uuid> - how do we know the zaak uuid?

                    // GET "zaak/id/{identificatie} - how do we know the zaak identificatie?

                    // Solr index updaten en dan de eerste (enige) zaak ophalen?

                    // Note that the 'notificaties' endpoint always returns 'no content' even if things go wrong
                    // since it is a fire-and-forget kind of endpoint.
                    statusCode shouldBe HttpStatus.SC_NO_CONTENT
                }
            }
        }
    }
})
