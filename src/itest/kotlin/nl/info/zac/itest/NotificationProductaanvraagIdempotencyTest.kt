/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.COORDINATOR_1
import nl.info.zac.itest.config.ItestConfiguration.OBJECTS_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_CONCURRENT_UUID
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_CONCURRENT_NOTIFICATIONS
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_DATABASE_CONTAINER_SERVICE_NAME
import nl.info.zac.itest.config.dockerComposeContainer
import okhttp3.Headers
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds

private const val CLAIM_TABLE = "zaakafhandelcomponent.verwerkte_productaanvraag"
private const val NUMBER_OF_SIMULTANEOUS_NOTIFICATIONS = 5
private const val INBOX_LIST_MAX_RESULTS = 10

private val logger = KotlinLogging.logger {}

private fun executeSqlInZacDatabase(sql: String): String =
    dockerComposeContainer.getContainerByServiceName(ZAC_DATABASE_CONTAINER_SERVICE_NAME).get()
        .execInContainer("psql", "-U", "zac", "-d", "zac", "-t", "-A", "-c", sql)
        .let { execResult ->
            check(execResult.exitCode == 0) { "psql failed: ${execResult.stderr}" }
            execResult.stdout.trim()
        }

private fun selectFromClaim(productaanvraagObjectUUID: Any, columns: String) = executeSqlInZacDatabase(
    "SELECT $columns FROM $CLAIM_TABLE WHERE uuid_productaanvraag_object = '$productaanvraagObjectUUID'"
)

private fun readClaim(productaanvraagObjectUUID: Any) =
    selectFromClaim(productaanvraagObjectUUID, columns = "status, gestart_op")

private fun readClaimStatus(productaanvraagObjectUUID: Any) =
    selectFromClaim(productaanvraagObjectUUID, columns = "status")

private fun insertClaim(productaanvraagObjectUUID: UUID, status: String, age: String) {
    executeSqlInZacDatabase(
        "INSERT INTO $CLAIM_TABLE (uuid_productaanvraag_object, status, gestart_op) " +
            "VALUES ('$productaanvraagObjectUUID', '$status', now() - interval '$age')"
    )
}

private fun ItestHttpClient.sendProductaanvraagCreateNotification(productaanvraagObjectUUID: Any) =
    performJSONPostRequest(
        url = "$ZAC_API_URI/notificaties",
        headers = Headers.headersOf(
            "Content-Type",
            "application/json",
            "Authorization",
            OPEN_NOTIFICATIONS_API_SECRET_KEY
        ),
        requestBodyAsString = JSONObject(
            mapOf(
                "kanaal" to "objecten",
                "resource" to "object",
                "resourceUrl" to "$OBJECTS_BASE_URI/$productaanvraagObjectUUID",
                "hoofdObject" to "$OBJECTS_BASE_URI/$productaanvraagObjectUUID",
                "actie" to "create",
                "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString(),
                "kenmerken" to mapOf(
                    "objectType" to "$OBJECTS_BASE_URI/$OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT"
                )
            )
        ).toString()
    )

class NotificationProductaanvraagIdempotencyTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    given(
        """
            A productaanvraag object of a type that is not mapped to any zaaktype configuration, for which ZAC has
            not yet recorded a claim
        """.trimIndent()
    ) {
        readClaimStatus(OBJECT_PRODUCTAANVRAAG_CONCURRENT_UUID) shouldBe ""

        `when`("$NUMBER_OF_SIMULTANEOUS_NOTIFICATIONS identical create notifications arrive simultaneously") {
            val startSignal = CountDownLatch(1)
            val responseCodes = ConcurrentLinkedQueue<Int>()
            List(NUMBER_OF_SIMULTANEOUS_NOTIFICATIONS) {
                thread {
                    startSignal.await()
                    responseCodes.add(
                        itestHttpClient.sendProductaanvraagCreateNotification(
                            OBJECT_PRODUCTAANVRAAG_CONCURRENT_UUID
                        ).code
                    )
                }
            }.also { threads ->
                startSignal.countDown()
                threads.forEach { it.join() }
            }

            then(
                """
                    every notification is answered successfully, so that Open Notificaties stops redelivering,
                    and the productaanvraag ends up in the inbox exactly once
                """.trimIndent()
            ) {
                responseCodes.toSet() shouldBe setOf(HTTP_NO_CONTENT)

                eventually(10.seconds) {
                    val listResponse = itestHttpClient.performPutRequest(
                        url = "$ZAC_API_URI/inbox-productaanvragen",
                        requestBodyAsString = JSONObject(
                            mapOf(
                                "page" to 0,
                                "maxResults" to INBOX_LIST_MAX_RESULTS,
                                "type" to PRODUCTAANVRAAG_TYPE_CONCURRENT_NOTIFICATIONS
                            )
                        ).toString(),
                        testUser = COORDINATOR_1
                    )
                    logger.info { "List inbox productaanvragen response: ${listResponse.bodyAsString}" }
                    listResponse.code shouldBe HTTP_OK
                    JSONObject(listResponse.bodyAsString).getInt("totaal") shouldBe 1
                }
            }

            And(
                """
                    the claim is released, so that a productaanvraag without a zaaktype mapping is never retried
                    against the unique constraint of the inbox
                """.trimIndent()
            ) {
                eventually(10.seconds) {
                    readClaimStatus(OBJECT_PRODUCTAANVRAAG_CONCURRENT_UUID) shouldBe "DONE"
                }
            }
        }
    }

    given(
        """
            A productaanvraag claim that was taken but never released, for example because ZAC was restarted while
            handling it, and which is older than the configured staleness period
        """.trimIndent()
    ) {
        val productaanvraagObjectUUID = UUID.randomUUID()
        insertClaim(productaanvraagObjectUUID, status = "IN_PROGRESS", age = "1 day")

        `when`("a notification for that productaanvraag arrives") {
            itestHttpClient.sendProductaanvraagCreateNotification(productaanvraagObjectUUID)
                .code shouldBe HTTP_NO_CONTENT

            then("the claim is taken over, so that handling starts again without manual intervention") {
                eventually(10.seconds) {
                    selectFromClaim(
                        productaanvraagObjectUUID,
                        columns = "gestart_op > now() - interval '1 minute'"
                    ) shouldBe "t"
                }
            }

            And(
                """
                    the claim is left unreleased because this productaanvraag object does not exist in Objecten,
                    so that it can be taken over once more after the staleness period
                """.trimIndent()
            ) {
                readClaimStatus(productaanvraagObjectUUID) shouldBe "IN_PROGRESS"
            }
        }
    }

    given("A productaanvraag claim that another ZAC instance took within the configured staleness period") {
        val productaanvraagObjectUUID = UUID.randomUUID()
        insertClaim(productaanvraagObjectUUID, status = "IN_PROGRESS", age = "1 second")
        val claimBeforeNotification = readClaim(productaanvraagObjectUUID)

        `when`("a notification for that productaanvraag arrives") {
            itestHttpClient.sendProductaanvraagCreateNotification(productaanvraagObjectUUID)
                .code shouldBe HTTP_NO_CONTENT

            then("the claim is left untouched, so that the instance already handling it is not interrupted") {
                continually(5.seconds) {
                    readClaim(productaanvraagObjectUUID) shouldBe claimBeforeNotification
                }
            }
        }
    }

    given("A productaanvraag claim that was already released long before the configured staleness period") {
        val productaanvraagObjectUUID = UUID.randomUUID()
        insertClaim(productaanvraagObjectUUID, status = "DONE", age = "1 day")
        val claimBeforeNotification = readClaim(productaanvraagObjectUUID)

        `when`("the notification for that productaanvraag is redelivered") {
            itestHttpClient.sendProductaanvraagCreateNotification(productaanvraagObjectUUID)
                .code shouldBe HTTP_NO_CONTENT

            then("the released claim is not taken again, so that no second zaak is ever created for it") {
                continually(5.seconds) {
                    readClaim(productaanvraagObjectUUID) shouldBe claimBeforeNotification
                }
            }
        }
    }
})
