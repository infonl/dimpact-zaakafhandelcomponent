/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_DEFINITIEF
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_IN_BEWERKING
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_UPDATED_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
import nl.info.zac.itest.config.ItestConfiguration.FAKE_AUTHOR_NAME
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_FACTUUR_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_FACTUUR_UUID
import nl.info.zac.itest.config.ItestConfiguration.PDF_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_TXT_CONVERTED_TO_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_TXT_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_TXT_FILE_SIZE
import nl.info.zac.itest.config.ItestConfiguration.TEXT_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection.HTTP_OK
import java.net.URLDecoder
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class EnkelvoudigInformatieObjectRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)
    val taskHelper = TaskHelper(zacClient)
    val today = LocalDate.now()

    Given(
        """
            A zaak exists and a behandelaar authorised for the zaaktype of the zaak is logged in
        """
    ) {
        // log in as a beheerder authorised in all domains
        // and create the zaken, tasks and documents and index them
        authenticate(BEHEERDER_ELK_ZAAKTYPE)
        val (_, zaakUuid) = zaakHelper.createAndIndexZaak(zaaktypeUuid = ZAAKTYPE_TEST_2_UUID)
        lateinit var enkelvoudigInformatieObjectUuid: String
        lateinit var enkelvoudigInformatieObject2Uuid: String
        authenticate(BEHANDELAAR_DOMAIN_TEST_1)

        When(
            """
            the create enkelvoudig informatie object with file upload endpoint is called for the zaak with a PDF file
            """
        ) {
            val file = Thread.currentThread().contextClassLoader.getResource(TEST_PDF_FILE_NAME).let {
                File(URLDecoder.decode(it!!.path, Charsets.UTF_8))
            }
            val response = zacClient.createEnkelvoudigInformatieobjectForZaak(
                zaakUUID = zaakUuid,
                fileName = TEST_PDF_FILE_NAME,
                fileMediaType = PDF_MIME_TYPE,
                vertrouwelijkheidaanduiding = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
            )

            Then(
                """
                   the response should be OK and contain information for the created document and uploaded file
                   and the permissions for the document should be those for a behandelaar
                   """
            ) {
                val responseBody = response.bodyAsString
                logger.info { "response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                         {
                          "bestandsnaam" : "$TEST_PDF_FILE_NAME",
                          "auteur" : "$FAKE_AUTHOR_NAME",
                          "beschrijving" : "",
                          "bestandsomvang" : ${file.length()},
                          "creatiedatum" : "${LocalDate.now()}",
                          "formaat" : "$PDF_MIME_TYPE",
                          "indicatieGebruiksrecht" : false,
                          "indicaties" : [ ],
                          "informatieobjectTypeOmschrijving" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING",
                          "informatieobjectTypeUUID" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID",
                          "isBesluitDocument" : false,
                          "rechten" : {
                            "lezen" : true,
                            "ondertekenen" : true,
                            "ontgrendelen" : false,
                            "toevoegenNieuweVersie" : true,
                            "vergrendelen" : true,
                            "verwijderen" : true,
                            "wijzigen" : true
                          },
                          "status" : "$DOCUMENT_STATUS_IN_BEWERKING",
                          "taal" : "Nederlands",
                          "titel" : "$DOCUMENT_FILE_TITLE",
                          "versie" : 1,
                          "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK"
                        }
                """.trimIndent()
                enkelvoudigInformatieObjectUuid = JSONObject(responseBody).getString("uuid")
                JSONObject(responseBody).getString("identificatie") shouldNotBe null
            }
        }

        When("update of enkelvoudig informatie object with file upload endpoint is called with a TXT file") {
            val endpointUrl =
                "$ZAC_API_URI/informatieobjecten/informatieobject/update"
            logger.info { "Calling $endpointUrl endpoint" }
            val file = Thread.currentThread().contextClassLoader.getResource(TEST_TXT_FILE_NAME).let {
                File(URLDecoder.decode(it!!.path, Charsets.UTF_8))
            }

            val requestBody =
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("uuid", enkelvoudigInformatieObjectUuid)
                    .addFormDataPart("zaakUuid", zaakUuid.toString())
                    .addFormDataPart("informatieobjectTypeUUID", INFORMATIE_OBJECT_TYPE_FACTUUR_UUID)
                    .addFormDataPart("bestandsnaam", TEST_TXT_FILE_NAME)
                    .addFormDataPart("titel", DOCUMENT_UPDATED_FILE_TITLE)
                    .addFormDataPart("bestandsomvang", TEST_TXT_FILE_SIZE.toString())
                    .addFormDataPart("formaat", TEXT_MIME_TYPE)
                    .addFormDataPart(
                        "vertrouwelijkheidaanduiding",
                        DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
                    )
                    .addFormDataPart(
                        "file",
                        TEST_TXT_FILE_NAME,
                        file.asRequestBody(TEXT_MIME_TYPE.toMediaType())
                    )
                    .build()
            val response = itestHttpClient.performPostRequest(
                url = endpointUrl,
                headers = Headers.headersOf(
                    "Accept",
                    "application/json",
                    "Content-Type",
                    "multipart/form-data"
                ),
                requestBody = requestBody
            )

            Then(
                "the response should be OK and should contain information about the updates"
            ) {
                val responseBody = response.bodyAsString
                logger.info { "$endpointUrl response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    {
                      "bestandsnaam" : "$TEST_TXT_FILE_NAME",
                      "auteur" : "$FAKE_AUTHOR_NAME",
                      "beschrijving" : "",
                      "bestandsomvang" : $TEST_TXT_FILE_SIZE,
                      "creatiedatum" : "$today",
                      "formaat" : "$TEXT_MIME_TYPE",
                      "indicatieGebruiksrecht" : false,
                      "indicaties" : [ ],
                      "informatieobjectTypeOmschrijving" : "$INFORMATIE_OBJECT_TYPE_FACTUUR_OMSCHRIJVING",
                      "informatieobjectTypeUUID" : "$INFORMATIE_OBJECT_TYPE_FACTUUR_UUID",
                      "isBesluitDocument" : false,
                      "rechten" : {
                        "lezen" : true,
                        "ondertekenen" : false,
                        "ontgrendelen" : false,
                        "toevoegenNieuweVersie" : false,
                        "vergrendelen" : false,
                        "verwijderen" : false,
                        "wijzigen" : false
                      },
                      "status" : "$DOCUMENT_STATUS_IN_BEWERKING",
                      "taal" : "Nederlands",
                      "titel" : "$DOCUMENT_UPDATED_FILE_TITLE",
                      "versie" : 2,
                      "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK"
                    }
                """.trimIndent()
                responseBody shouldContainJsonKey("registratiedatumTijd")
                responseBody shouldContainJsonKey("identificatie")
                responseBody shouldContainJsonKey("uuid")
            }
        }

        When("ondertekenInformatieObject endpoint is called") {
            val endpointUrl =
                "$ZAC_API_URI/informatieobjecten/informatieobject" +
                    "/$enkelvoudigInformatieObjectUuid/onderteken?zaak=$zaakUuid"
            logger.info { "Calling $endpointUrl endpoint" }

            val response = itestHttpClient.performPostRequest(
                url = endpointUrl,
                requestBody = "".toRequestBody()
            )
            Then(
                "the response should be OK"
            ) {
                logger.info { "$endpointUrl status code: ${response.code}" }
                response.code shouldBe HTTP_OK
            }
        }

        When("the get enkelvoudiginformatieobject endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/informatieobjecten/informatieobject/$enkelvoudigInformatieObjectUuid/"
            )
            Then(
                """
                the response should be OK and should contain an `ONDERTEKEND` indicatie
                and data about the ondertekening and the status should be 'definitief'
                """
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    {
                      "bestandsnaam" : "$TEST_TXT_FILE_NAME",
                      "auteur" : "$FAKE_AUTHOR_NAME",
                      "beschrijving" : "",
                      "bestandsomvang" : $TEST_TXT_FILE_SIZE,
                      "creatiedatum" : "$today",
                      "formaat" : "$TEXT_MIME_TYPE",
                      "indicatieGebruiksrecht" : false,
                      "indicaties" : [ "ONDERTEKEND" ],
                      "informatieobjectTypeOmschrijving" : "factuur",
                      "informatieobjectTypeUUID" : "$INFORMATIE_OBJECT_TYPE_FACTUUR_UUID",
                      "isBesluitDocument" : false,
                      "ondertekening" : {
                        "datum" : "$today",
                        "soort" : "digitaal"
                      },
                      "rechten" : {
                        "converteren" : true,
                        "lezen" : true,
                        "ondertekenen" : false,
                        "ontgrendelen" : false,
                        "toevoegenNieuweVersie" : false,
                        "vergrendelen" : false,
                        "verwijderen" : false,
                        "wijzigen" : false
                      },
                      "status" : "$DOCUMENT_STATUS_DEFINITIEF",
                      "taal" : "Nederlands",
                      "titel" : "$DOCUMENT_UPDATED_FILE_TITLE",
                      "versie" : 3,
                      "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK"
                    }
                """.trimIndent()
            }
        }

        When("the current version endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/informatieobjecten/informatieobject/$enkelvoudigInformatieObjectUuid/huidigeversie"
            )
            Then("the response should be OK and the informatieobject should be returned") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    {
                      "auteur" : "$FAKE_AUTHOR_NAME",
                      "beschrijving" : "",
                      "informatieobjectTypeUUID" : "$INFORMATIE_OBJECT_TYPE_FACTUUR_UUID",
                      "status" : "$DOCUMENT_STATUS_DEFINITIEF",
                      "taal" : {
                        "code" : "dut",
                        "id" : "1",
                        "local" : "Nederlands",
                        "naam" : "Nederlands",
                        "name" : "Dutch"
                      },
                      "titel" : "$DOCUMENT_UPDATED_FILE_TITLE",
                      "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK"
                    }
                """.trimIndent()
                responseBody shouldContainJsonKey("uuid")
                responseBody shouldContainJsonKey("bestandsnaam")
            }
        }

        When(
            """
                the create enkelvoudig informatie object with file upload endpoint is called for the zaak with a TXT file
                """
        ) {
            val endpointUrl =
                "$ZAC_API_URI/informatieobjecten/informatieobject/$zaakUuid/$zaakUuid"
            logger.info { "Calling $endpointUrl endpoint" }
            val file = Thread.currentThread().contextClassLoader.getResource(TEST_TXT_FILE_NAME).let {
                File(URLDecoder.decode(it!!.path, Charsets.UTF_8))
            }
            val requestBody =
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("bestandsnaam", TEST_TXT_FILE_NAME)
                    .addFormDataPart("titel", DOCUMENT_FILE_TITLE)
                    .addFormDataPart("bestandsomvang", file.length().toString())
                    .addFormDataPart("formaat", TEXT_MIME_TYPE)
                    .addFormDataPart(
                        "file",
                        TEST_TXT_FILE_NAME,
                        file.asRequestBody(TEXT_MIME_TYPE.toMediaType())
                    )
                    .addFormDataPart("informatieobjectTypeUUID", INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID)
                    .addFormDataPart(
                        "vertrouwelijkheidaanduiding",
                        DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
                    )
                    .addFormDataPart("status", DOCUMENT_STATUS_DEFINITIEF)
                    .addFormDataPart(
                        "creatiedatum",
                        DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd'T'HH:mm+01:00"
                        ).format(ZonedDateTime.now())
                    )
                    .addFormDataPart("auteur", FAKE_AUTHOR_NAME)
                    .addFormDataPart("taal", "eng")
                    .build()
            val response = itestHttpClient.performPostRequest(
                url = endpointUrl,
                headers = Headers.headersOf(
                    "Accept",
                    "application/json",
                    "Content-Type",
                    "multipart/form-data"
                ),
                requestBody = requestBody
            )

            Then(
                """
                the response should be OK and contain information for the created document and uploaded file
                and the document permissions should be as expected for a document with status 'definitief'
                and a behandelaar user (e.g. no 'wijzigen' permission)
                """
            ) {
                val responseBody = response.bodyAsString
                logger.info { "$endpointUrl response: $responseBody" }
                response.code shouldBe HTTP_OK
                // note: there is an issue where the update endpoint does not take into account the zaak
                // to which this document belongs, resulting in incorrect permissions being returned
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    {
                      "bestandsnaam" : "$TEST_TXT_FILE_NAME",
                      "auteur" : "$FAKE_AUTHOR_NAME",
                      "beschrijving" : "",
                      "bestandsomvang" : ${file.length()},
                      "creatiedatum" : "${LocalDate.now()}",
                      "formaat" : "$TEXT_MIME_TYPE",
                      "indicatieGebruiksrecht" : false,
                      "indicaties" : [ ],
                      "informatieobjectTypeOmschrijving" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING",
                      "informatieobjectTypeUUID" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID",
                      "isBesluitDocument" : false,
                      "rechten" : {
                        "lezen" : true,
                        "ondertekenen" : true,
                        "ontgrendelen" : false,
                        "toevoegenNieuweVersie" : false,
                        "vergrendelen" : true,
                        "verwijderen" : false,
                        "wijzigen" : false
                      },
                      "status" : "$DOCUMENT_STATUS_DEFINITIEF",
                      "taal" : "Engels",
                      "titel" : "$DOCUMENT_FILE_TITLE",
                      "versie" : 1,
                      "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR"
                    }
                """.trimIndent()
                enkelvoudigInformatieObject2Uuid = JSONObject(responseBody).getString("uuid")
            }
        }

        When("the convert endpoint is called") {
            val response = itestHttpClient.performPostRequest(
                url = "$ZAC_API_URI/informatieobjecten/informatieobject/$enkelvoudigInformatieObject2Uuid/" +
                    "convert?zaak=$zaakUuid",
                requestBody = "".toRequestBody()
            )
            Then(
                """
                    the response should be OK and the informatieobject should be converted from TXT to PDF
                    because a recordmanager is allowed to convert documents even if they have the status 'definitief'
                    """
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
            }
        }

        When("the get enkelvoudiginformatieobject endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/informatieobjecten/informatieobject/$enkelvoudigInformatieObject2Uuid/"
            )
            Then(
                """
                the response should be OK and should contain information about the document converted to PDF
                and the permissions should be those for a document with status 'definitief' and a behandelaar user
                """
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                {
                  "bestandsnaam" : "$TEST_TXT_CONVERTED_TO_PDF_FILE_NAME",
                  "auteur" : "$FAKE_AUTHOR_NAME",
                  "beschrijving" : "",
                  "creatiedatum" : "${LocalDate.now()}",
                  "formaat" : "$PDF_MIME_TYPE",
                  "indicatieGebruiksrecht" : false,
                  "indicaties" : [ ],
                  "informatieobjectTypeOmschrijving" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING",
                  "informatieobjectTypeUUID" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID",
                  "isBesluitDocument" : false,
                  "rechten": {
                    "converteren": true,
                    "lezen": true,
                    "ondertekenen": false,
                    "ontgrendelen": false,
                    "toevoegenNieuweVersie": false,
                    "vergrendelen": false,
                    "verwijderen": false,
                    "wijzigen": false
                  },
                  "status" : "$DOCUMENT_STATUS_DEFINITIEF",
                  "taal" : "Engels",
                  "titel" : "$DOCUMENT_FILE_TITLE",
                  "versie" : 2,
                  "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR"
                }
                """.trimIndent()
                JSONObject(responseBody).getString("identificatie") shouldNotBe null
            }
        }
    }

    Given("""A zaak exist and a task has been started and a behandelaar is logged in""") {
        authenticate(BEHEERDER_ELK_ZAAKTYPE)
        val (zaakIdentification, zaakUuid) = zaakHelper.createAndIndexZaak(zaaktypeUuid = ZAAKTYPE_TEST_2_UUID)
        val taskId = taskHelper.startAanvullendeInformatieTaskForZaak(
            zaakUuid = zaakUuid,
            zaakIdentificatie = zaakIdentification,
            fatalDate = LocalDate.now().plusDays(1),
            group = BEHANDELAARS_DOMAIN_TEST_1
        )
        authenticate(BEHANDELAAR_DOMAIN_TEST_1)

        When("the create enkelvoudig informatie object with file upload endpoint is called for the task") {
            val endpointUrl = "$ZAC_API_URI/informatieobjecten/informatieobject/" +
                "$zaakUuid/$taskId?taakObject=true"
            logger.info { "Calling $endpointUrl endpoint" }
            val file = Thread.currentThread().contextClassLoader.getResource(TEST_PDF_FILE_NAME).let {
                File(URLDecoder.decode(it!!.path, Charsets.UTF_8))
            }
            val requestBody =
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("bestandsnaam", TEST_PDF_FILE_NAME)
                    .addFormDataPart("titel", DOCUMENT_FILE_TITLE)
                    .addFormDataPart("bestandsomvang", file.length().toString())
                    .addFormDataPart("formaat", PDF_MIME_TYPE)
                    .addFormDataPart(
                        "file",
                        TEST_PDF_FILE_NAME,
                        file.asRequestBody(PDF_MIME_TYPE.toMediaType())
                    )
                    .addFormDataPart("informatieobjectTypeUUID", INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID)
                    .addFormDataPart(
                        "vertrouwelijkheidaanduiding",
                        DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
                    )
                    .addFormDataPart("status", DOCUMENT_STATUS_DEFINITIEF)
                    .addFormDataPart(
                        "creatiedatum",
                        DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd'T'HH:mm+01:00"
                        ).format(ZonedDateTime.now())
                    )
                    .addFormDataPart("auteur", FAKE_AUTHOR_NAME)
                    .addFormDataPart("taal", "eng")
                    .build()
            val response = itestHttpClient.performPostRequest(
                url = endpointUrl,
                headers = Headers.headersOf(
                    "Accept",
                    "application/json",
                    "Content-Type",
                    "multipart/form-data"
                ),
                requestBody = requestBody
            )

            Then(
                "the response should be OK and contain information for the created document and uploaded file"
            ) {
                val responseBody = response.bodyAsString
                logger.info { "$endpointUrl response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                {
                  "bestandsnaam" : "$TEST_PDF_FILE_NAME",
                  "auteur" : "$FAKE_AUTHOR_NAME",
                  "beschrijving" : "",
                  "creatiedatum" : "${LocalDate.now()}",
                  "formaat" : "$PDF_MIME_TYPE",
                  "indicatieGebruiksrecht" : false,
                  "indicaties" : [ ],
                  "informatieobjectTypeOmschrijving" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING",
                  "informatieobjectTypeUUID" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID",
                  "isBesluitDocument" : false,
                  "rechten" : {
                    "lezen" : true,
                    "ondertekenen" : true,
                    "ontgrendelen" : false,
                    "toevoegenNieuweVersie" : false,
                    "vergrendelen" : true,
                    "verwijderen" : false,
                    "wijzigen" : false
                  },
                  "status" : "$DOCUMENT_STATUS_DEFINITIEF",
                  "taal" : "Engels",
                  "titel" : "$DOCUMENT_FILE_TITLE",
                  "versie" : 1,
                  "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR"
                }
                """.trimIndent()
            }
        }
    }
})
