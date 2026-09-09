/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.addEnkelvoudigInformatieobjectVersion
import nl.info.zac.itest.client.createEnkelvoudigInformatieobjectForZaak
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.RECORDMANAGER_1
import nl.info.zac.itest.config.ItestConfiguration.CONFIG_MAX_IN_MEMORY_FILE_SIZE_IN_MB
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_DEFINITIEF
import nl.info.zac.itest.config.ItestConfiguration.TEXT_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.VERTROUWELIJKHEIDAANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection.HTTP_ENTITY_TOO_LARGE
import java.net.HttpURLConnection.HTTP_OK
import java.security.MessageDigest
import java.util.UUID

private const val BYTES_PER_MB = 1024 * 1024
private const val LETTERS_IN_THE_ALPHABET = 26

private fun createTemporaryDocument(sizeInBytes: Int) =
    ByteArray(sizeInBytes) { ('a' + it % LETTERS_IN_THE_ALPHABET).code.toByte() }.let { content ->
        content to File.createTempFile("zac-itest-document", ".txt")
            .apply { writeBytes(content) }
            .also { it.deleteOnExit() }
    }

class LargeDocumentUploadTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)

    given("a zaak and a document larger than the maximum in-memory file size") {
        val (_, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            testUser = BEHANDELAAR_1
        )
        val fileSizeInBytes = (CONFIG_MAX_IN_MEMORY_FILE_SIZE_IN_MB.toInt() + 2) * BYTES_PER_MB
        val (fileContent, file) = createTemporaryDocument(fileSizeInBytes)

        `when`("the document is uploaded to the zaak") {
            val response = zacClient.createEnkelvoudigInformatieobjectForZaak(
                zaakUUID = zaakUuid,
                file = file,
                fileName = "large-document.txt",
                fileMediaType = TEXT_MIME_TYPE,
                vertrouwelijkheidaanduiding = VERTROUWELIJKHEIDAANDUIDING_OPENBAAR,
                status = DOCUMENT_STATUS_DEFINITIEF,
                testUser = BEHANDELAAR_1
            )
            withClue("upload response: ${response.bodyAsString}") {
                response.code shouldBe HTTP_OK
            }
            val documentUuid = JSONObject(response.bodyAsString).getString("uuid")

            then("the document is stored with its full size, so it was uploaded in parts") {
                JSONObject(response.bodyAsString).getInt("bestandsomvang") shouldBe fileSizeInBytes
            }

            and("downloading it returns exactly the bytes that were uploaded") {
                val downloadResponse = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/informatieobjecten/informatieobject/$documentUuid/download",
                    testUser = BEHANDELAAR_1
                )

                downloadResponse.code shouldBe HTTP_OK
                downloadResponse.bodyAsBytes.size shouldBe fileSizeInBytes
                MessageDigest.getInstance("SHA-256").digest(downloadResponse.bodyAsBytes) shouldBe
                    MessageDigest.getInstance("SHA-256").digest(fileContent)
            }

            and("converting it to PDF is refused because that cannot be done without holding it in memory") {
                val convertResponse = itestHttpClient.performPostRequest(
                    url = "$ZAC_API_URI/informatieobjecten/informatieobject/$documentUuid/" +
                        "convert?zaak=$zaakUuid",
                    requestBody = "".toRequestBody("application/json".toMediaType()),
                    testUser = RECORDMANAGER_1
                )

                convertResponse.code shouldBe HTTP_ENTITY_TOO_LARGE
                JSONObject(convertResponse.bodyAsString).getString("message") shouldBe
                    "msg.error.file.too-large-to-open"
            }

            and("a new version larger than the maximum in-memory file size is added to it") {
                val newVersionSizeInBytes = fileSizeInBytes + BYTES_PER_MB
                val (newVersionContent, newVersionFile) = createTemporaryDocument(newVersionSizeInBytes)

                val newVersionResponse = zacClient.addEnkelvoudigInformatieobjectVersion(
                    enkelvoudigInformatieobjectUuid = UUID.fromString(documentUuid),
                    zaakUuid = zaakUuid,
                    file = newVersionFile,
                    fileName = "large-document-v2.txt",
                    fileMediaType = TEXT_MIME_TYPE,
                    vertrouwelijkheidaanduiding = VERTROUWELIJKHEIDAANDUIDING_OPENBAAR,
                    testUser = BEHANDELAAR_1
                )

                withClue("new version response: ${newVersionResponse.bodyAsString}") {
                    newVersionResponse.code shouldBe HTTP_OK
                }
                JSONObject(newVersionResponse.bodyAsString).run {
                    getInt("bestandsomvang") shouldBe newVersionSizeInBytes
                    getInt("versie") shouldBe 2
                }

                val downloadResponse = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/informatieobjecten/informatieobject/$documentUuid/download",
                    testUser = BEHANDELAAR_1
                )
                downloadResponse.code shouldBe HTTP_OK
                downloadResponse.bodyAsBytes.size shouldBe newVersionSizeInBytes
                MessageDigest.getInstance("SHA-256").digest(downloadResponse.bodyAsBytes) shouldBe
                    MessageDigest.getInstance("SHA-256").digest(newVersionContent)
            }
        }
    }
})
