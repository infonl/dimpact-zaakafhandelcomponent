/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.DocumentHelper
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.COORDINATOR_1
import nl.info.zac.itest.config.ItestConfiguration.FAKE_AUTHOR_NAME
import nl.info.zac.itest.config.ItestConfiguration.PDF_MEDIA_TYPE
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RECORDMANAGER_1
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID

class DetachedDocumentRestServiceBulkTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)
    val documentHelper = DocumentHelper(zacClient)
    val unknownDetachedDocumentId = 987_654_321L

    fun detachNewDocument(zaakUuid: UUID, titlePrefix: String): UUID {
        val (documentUuid, _) = documentHelper.uploadDocumentToZaak(
            zaakUuid = zaakUuid,
            fileName = TEST_PDF_FILE_NAME,
            documentTitle = "$titlePrefix-${System.currentTimeMillis()}",
            authorName = FAKE_AUTHOR_NAME,
            mediaType = PDF_MEDIA_TYPE,
            testUser = BEHANDELAAR_1
        )
        itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/zaken/zaakinformatieobjecten/ontkoppel",
            requestBodyAsString = JSONObject(
                mapOf(
                    "zaakUUID" to zaakUuid.toString(),
                    "documentUUID" to documentUuid.toString(),
                    "reden" to "fakeBulkDetachReason"
                )
            ).toString(),
            testUser = BEHANDELAAR_1
        )
        return documentUuid
    }

    fun findDetachedDocumentId(documentUuid: UUID): Long? {
        val listResponse = itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/ontkoppeldedocumenten",
            requestBodyAsString = """{"page": 0, "maxResults": 100, "sort": "", "order": ""}""",
            testUser = COORDINATOR_1
        )
        val resultaten = JSONObject(listResponse.bodyAsString).getJSONArray("resultaten")
        return (0 until resultaten.length())
            .map(resultaten::getJSONObject)
            .firstOrNull { it.getString("documentUUID") == documentUuid.toString() }
            ?.getLong("id")
    }

    fun JSONArray.toLongList() = (0 until length()).map(::getLong)

    given("Two detached documents and an id that does not exist") {
        val (_, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            testUser = BEHANDELAAR_1
        )
        val firstId = findDetachedDocumentId(detachNewDocument(zaakUuid, "bulkDeleteItest1"))!!
        val secondId = findDetachedDocumentId(detachNewDocument(zaakUuid, "bulkDeleteItest2"))!!

        `when`("the bulk delete endpoint is called by a recordmanager with the three ids") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/ontkoppeldedocumenten/lijst/verwijderen",
                requestBodyAsString = JSONObject(
                    mapOf("ids" to listOf(firstId, unknownDetachedDocumentId, secondId))
                ).toString(),
                testUser = RECORDMANAGER_1
            )

            then("the two documents are processed and the unknown id is reported as failed") {
                response.code shouldBe HTTP_OK
                val responseJson = JSONObject(response.bodyAsString)
                responseJson.getJSONArray("verwerkt").toLongList() shouldBe listOf(firstId, secondId)
                val failure = responseJson.getJSONArray("mislukt").getJSONObject(0)
                failure.getLong("id") shouldBe unknownDetachedDocumentId
                failure.getString("foutcode") shouldBe "NIET_GEVONDEN"
            }
        }

        `when`("the bulk delete endpoint is called with 101 ids") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/ontkoppeldedocumenten/lijst/verwijderen",
                requestBodyAsString = JSONObject(mapOf("ids" to (1L..101L).toList())).toString(),
                testUser = RECORDMANAGER_1
            )

            then("the request is refused") {
                response.code shouldBe HTTP_BAD_REQUEST
            }
        }
    }

    given("Two detached documents, an id that does not exist and a zaak to link them to") {
        val (zaakIdentificatie, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            testUser = BEHANDELAAR_1
        )
        val firstId = findDetachedDocumentId(detachNewDocument(zaakUuid, "bulkLinkItest1"))!!
        val secondId = findDetachedDocumentId(detachNewDocument(zaakUuid, "bulkLinkItest2"))!!

        `when`("the bulk link endpoint is called with the three ids") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/ontkoppeldedocumenten/lijst/koppelen",
                requestBodyAsString = JSONObject(
                    mapOf(
                        "ids" to listOf(firstId, unknownDetachedDocumentId, secondId),
                        "nieuweZaakID" to zaakIdentificatie
                    )
                ).toString(),
                testUser = BEHANDELAAR_1
            )

            then("the two documents are linked and the unknown id is reported as failed") {
                response.code shouldBe HTTP_OK
                val responseJson = JSONObject(response.bodyAsString)
                responseJson.getJSONArray("verwerkt").toLongList() shouldBe listOf(firstId, secondId)
                responseJson.getJSONArray("mislukt").getJSONObject(0).getLong("id") shouldBe unknownDetachedDocumentId
            }
        }
    }
})
