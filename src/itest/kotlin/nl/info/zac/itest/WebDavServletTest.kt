/*
 * SPDX-FileCopyrightText: 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.MediaType
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.createZaakAndRetrieve
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2024_01_31
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_IN_BEWERKING
import nl.info.zac.itest.config.ItestConfiguration.VERTROUWELIJKHEIDAANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.FAKE_AUTHOR_NAME
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.TEST_WORD_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.WORD_DOCUMENT_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_BASE_URI
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.net.URLDecoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val HTTP_MULTI_STATUS = 207
private const val HTTP_LOCKED = 423
private const val VERSION_AFTER_FIRST_WEBDAV_PUT = 2
private const val VERSION_AFTER_LOCKED_WEBDAV_PUT = 3
private const val XML_MEDIA_TYPE = "application/xml; charset=utf-8"
private const val PROPFIND_ALLPROP_BODY = """<?xml version="1.0" encoding="utf-8"?>
<D:propfind xmlns:D="DAV:"><D:allprop/></D:propfind>"""
private const val LOCK_EXCLUSIVE_WRITE_BODY = """<?xml version="1.0" encoding="utf-8"?>
<D:lockinfo xmlns:D="DAV:">
  <D:lockscope><D:exclusive/></D:lockscope>
  <D:locktype><D:write/></D:locktype>
  <D:owner><D:href>itest</D:href></D:owner>
</D:lockinfo>"""

/**
 * Integration test that creates a zaak, uploads a document, and verifies access via the WebDAV servlet,
 * including the OPTIONS / PROPFIND / LOCK / PUT / UNLOCK sequence that Microsoft Office clients use to edit a document.
 *
 * Some assertions pin the current behaviour of the `nl.info.webdav:webdav-servlet` library where it deviates
 * from RFC 4918. These are marked with a comment and will need to be updated when the library is fixed.
 */
class WebDavServletTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val zacClient = ZacClient()
    val itestHttpClient = ItestHttpClient()
    val wordFile = Thread.currentThread().contextClassLoader.getResource(TEST_WORD_FILE_NAME).let {
        File(URLDecoder.decode(it!!.path, Charsets.UTF_8))
    }
    lateinit var enkelvoudigInformatieObjectUUID: String
    lateinit var wordDocumentWebDAVToken: UUID

    fun webdavDocumentUrl() = "$ZAC_BASE_URI/webdav/folder/$wordDocumentWebDAVToken.docx"

    fun readDocumentVersion(): Int =
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/informatieobjecten/informatieobject/$enkelvoudigInformatieObjectUUID",
            testUser = BEHANDELAAR_1
        ).run {
            code shouldBe HTTP_OK
            JSONObject(bodyAsString).getInt("versie")
        }

    fun putDocument(headers: Headers = Headers.headersOf("Content-Type", MediaType.DOCX.value)) =
        itestHttpClient.performPutRequest(
            url = webdavDocumentUrl(),
            headers = headers,
            requestBody = wordFile.asRequestBody(MediaType.DOCX.toMediaType())
        )

    given("A zaak that was created and a logged-in behandelaar") {
        lateinit var zaakUUID: UUID
        zacClient.createZaakAndRetrieve(
            zaakTypeUUID = ZAAKTYPE_CMMN_TEST_2_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            behandelaarId = BEHANDELAAR_1.username,
            startDate = DATE_TIME_2024_01_31,
            testUser = BEHANDELAAR_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            this.code shouldBe HTTP_OK
            JSONObject(responseBody).run {
                zaakUUID = getString("uuid").run(UUID::fromString)
            }
        }
        `when`(
            """
                the create enkelvoudig informatie object with file upload endpoint is called for the zaak with a DOCX file
                """
        ) {
            val endpointUrl =
                "$ZAC_API_URI/informatieobjecten/informatieobject/$zaakUUID/$zaakUUID"
            logger.info { "Calling $endpointUrl endpoint" }
            val requestBody =
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("bestandsnaam", TEST_WORD_FILE_NAME)
                    .addFormDataPart("titel", WORD_DOCUMENT_FILE_TITLE)
                    .addFormDataPart("bestandsomvang", wordFile.length().toString())
                    .addFormDataPart("formaat", MediaType.DOCX.toMediaType().toString())
                    .addFormDataPart(
                        "file",
                        TEST_WORD_FILE_NAME,
                        wordFile.asRequestBody(MediaType.DOCX.toMediaType())
                    )
                    .addFormDataPart("informatieobjectTypeUUID", INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID)
                    .addFormDataPart(
                        "vertrouwelijkheidaanduiding",
                        VERTROUWELIJKHEIDAANDUIDING_OPENBAAR
                    )
                    .addFormDataPart("status", DOCUMENT_STATUS_IN_BEWERKING)
                    .addFormDataPart(
                        "creatiedatum",
                        DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd'T'HH:mm+01:00"
                        ).format(ZonedDateTime.now())
                    )
                    .addFormDataPart("auteur", FAKE_AUTHOR_NAME)
                    .addFormDataPart("taal", "dut")
                    .build()
            val response = itestHttpClient.performPostRequest(
                url = endpointUrl,
                headers = Headers.headersOf(
                    "Accept",
                    "application/json",
                    "Content-Type",
                    "multipart/form-data"
                ),
                requestBody = requestBody,
                testUser = BEHANDELAAR_1
            )
            then(
                "the response should be OK and contain information for the created Word document and uploaded file"
            ) {
                val responseBody = response.bodyAsString
                logger.info { "$endpointUrl response: $responseBody" }
                response.code shouldBe HTTP_OK
                enkelvoudigInformatieObjectUUID = JSONObject(responseBody).getString("uuid")
            }
        }

        `when`("the edit endpoint is called on the enkelvoudig informatie object") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/informatieobjecten/informatieobject/$enkelvoudigInformatieObjectUUID/edit" +
                    "?zaak=$zaakUUID",
                testUser = BEHANDELAAR_1
            )
            then(
                """
                the response should be ok and should contain an MS Word WebDAV redirect URL with a
                token for the uploaded Word document
                """
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldStartWith "\"ms-word:http://localhost:8080/webdav/folder/"
                responseBody shouldEndWith ".docx\""
                wordDocumentWebDAVToken = UUID.fromString(responseBody.substringAfterLast("/").substringBefore(".docx"))
            }
        }

        `when`("an OPTIONS request is performed using the WebDAV token for the uploaded file") {
            // the WebDAV servlet does not require any authorization
            val response = itestHttpClient.performRequest(method = "OPTIONS", url = webdavDocumentUrl())

            then("the response should be ok and advertise WebDAV class 2 (locking) support to Office clients") {
                response.code shouldBe HTTP_OK
                response.headers["DAV"] shouldBe "1, 2"
                response.headers["MS-Author-Via"] shouldBe "DAV"
                // the library currently does not list PUT for an existing resource, although PUT is supported
                response.headers["Allow"].shouldNotBeNull().run {
                    shouldContain("GET")
                    shouldContain("PROPFIND")
                    shouldContain("LOCK")
                    shouldContain("UNLOCK")
                }
            }
        }

        `when`("a PROPFIND request with depth 0 is performed using the WebDAV token for the uploaded file") {
            val response = itestHttpClient.performRequest(
                method = "PROPFIND",
                url = webdavDocumentUrl(),
                headers = Headers.headersOf("Depth", "0"),
                requestBody = PROPFIND_ALLPROP_BODY.toRequestBody(XML_MEDIA_TYPE.toMediaType())
            )

            then("the response should be a multistatus with the properties of the document") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_MULTI_STATUS
                responseBody shouldContain "<D:href>/webdav/folder/$wordDocumentWebDAVToken.docx</D:href>"
                responseBody shouldContain "<D:getcontentlength>${wordFile.length()}</D:getcontentlength>"
                responseBody shouldContain "<D:getlastmodified>"
            }
        }

        `when`("the DOCX Word document is requested using the WebDAV token for the uploaded file") {
            val response = itestHttpClient.performGetRequest(url = webdavDocumentUrl())

            then("the response should be ok and contain the uploaded DOCX Word document") {
                response.code shouldBe HTTP_OK
                response.bodyAsBytes shouldBe wordFile.readBytes()
            }
        }

        `when`("a HEAD request is performed using the WebDAV token for the uploaded file") {
            val responseCode = itestHttpClient.performHeadRequest(url = webdavDocumentUrl())

            then("the response should be ok") {
                responseCode shouldBe HTTP_OK
            }
        }

        `when`("the DOCX Word document is updated using a WebDAV PUT request with updated file content") {
            // the logged-in user is retrieved from the WebDAV token
            val response = putDocument()

            then("the response should be created and a new version of the document should be stored in the DRC") {
                logger.info { "Response: ${response.bodyAsString}" }
                // RFC 4918 prescribes 204 No Content when an existing resource is overwritten,
                // but the library currently returns 201 Created (see the webdav-servlet bug register, #23)
                response.code shouldBe HTTP_CREATED
                readDocumentVersion() shouldBe VERSION_AFTER_FIRST_WEBDAV_PUT
            }
        }

        `when`("an exclusive write LOCK is requested using the WebDAV token for the uploaded file") {
            val response = itestHttpClient.performRequest(
                method = "LOCK",
                url = webdavDocumentUrl(),
                headers = Headers.headersOf("Timeout", "Second-60"),
                requestBody = LOCK_EXCLUSIVE_WRITE_BODY.toRequestBody(XML_MEDIA_TYPE.toMediaType())
            )
            val lockToken = response.headers["Lock-Token"].orEmpty()

            then("the response should be ok and contain the lock token") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                lockToken shouldStartWith "<opaquelocktoken:"
                responseBody shouldContain lockToken.removePrefix("<").removeSuffix(">")
                responseBody shouldContain "<D:exclusive/>"
            }

            and("the document is updated with a PUT request without the lock token") {
                val putResponse = putDocument()

                then("the response should be locked and no new version should be stored in the DRC") {
                    putResponse.code shouldBe HTTP_LOCKED
                    readDocumentVersion() shouldBe VERSION_AFTER_FIRST_WEBDAV_PUT
                }
            }

            and("the document is updated with a PUT request with the lock token in the If header") {
                val putResponse = putDocument(
                    Headers.headersOf(
                        "Content-Type",
                        MediaType.DOCX.value,
                        "If",
                        "($lockToken)"
                    )
                )

                then("the response should be created and a new version should be stored in the DRC") {
                    // RFC 4918 prescribes 204 No Content, see the PUT test above
                    putResponse.code shouldBe HTTP_CREATED
                    readDocumentVersion() shouldBe VERSION_AFTER_LOCKED_WEBDAV_PUT
                }
            }

            and("the lock is released with an UNLOCK request with the lock token") {
                val unlockResponse = itestHttpClient.performRequest(
                    method = "UNLOCK",
                    url = webdavDocumentUrl(),
                    headers = Headers.headersOf("Lock-Token", lockToken)
                )

                then("the response should be no content") {
                    unlockResponse.code shouldBe HTTP_NO_CONTENT
                }
            }

            and("the lock is released again with the same lock token") {
                val unlockResponse = itestHttpClient.performRequest(
                    method = "UNLOCK",
                    url = webdavDocumentUrl(),
                    headers = Headers.headersOf("Lock-Token", lockToken)
                )

                then("the response should be bad request because the lock no longer exists") {
                    // RFC 4918 prescribes 409 Conflict for a lock token that does not match a lock,
                    // but the library currently returns 400 Bad Request
                    unlockResponse.code shouldBe HTTP_BAD_REQUEST
                }
            }
        }

        `when`("a COPY or MOVE request is performed with a Destination header that traverses outside the root") {
            val destinations = listOf(
                "/webdav/folder/../../../etc/passwd",
                // URL-encoded backslashes: rejected since webdav-servlet PR #406
                "/webdav/folder/a%5C..%5C..%5C..%5Cetc%5Cpasswd"
            )
            val responses = listOf("COPY", "MOVE").flatMap { method ->
                destinations.map { destination ->
                    "$method $destination" to itestHttpClient.performRequest(
                        method = method,
                        url = webdavDocumentUrl(),
                        headers = Headers.headersOf("Destination", destination)
                    ).code
                }
            }

            then("every request should be rejected with bad request before it reaches the WebDAV store") {
                responses.forEach { (request, code) ->
                    logger.info { "$request: $code" }
                }
                responses.map { it.second }.toSet() shouldBe setOf(HTTP_BAD_REQUEST)
            }
        }
    }

    given("A WebDAV request is performed with a non-existent token") {
        val nonExistentToken = UUID.randomUUID()

        // ZAC's WebdavStore throws a WebdavException for an unknown token instead of returning null,
        // which the library maps to 500 Internal Server Error instead of 404 Not Found
        `when`("a GET request is performed using the non-existent WebDAV token") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_BASE_URI/webdav/folder/$nonExistentToken.docx"
            )

            then("the response should be an internal server error") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_INTERNAL_ERROR
            }
        }

        `when`("a HEAD request is performed using the non-existent WebDAV token") {
            val responseCode = itestHttpClient.performHeadRequest(
                url = "$ZAC_BASE_URI/webdav/folder/$nonExistentToken.docx"
            )

            then("the response should be an internal server error") {
                responseCode shouldBe HTTP_INTERNAL_ERROR
            }
        }
    }
})
