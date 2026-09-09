/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_IN_BEWERKING
import nl.info.zac.itest.config.ItestConfiguration.FAKE_AUTHOR_NAME
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.TestUser
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Suppress("LongParameterList")
fun ZacClient.createEnkelvoudigInformatieobjectForZaak(
    zaakUUID: UUID,
    file: File,
    fileName: String,
    title: String = DOCUMENT_FILE_TITLE,
    authorName: String = FAKE_AUTHOR_NAME,
    fileMediaType: String,
    vertrouwelijkheidaanduiding: String,
    status: String = DOCUMENT_STATUS_IN_BEWERKING,
    testUser: TestUser
): ResponseContent {
    val createEnkelvoudigInformatieobjectEndpointURI =
        "$ZAC_API_URI/informatieobjecten/informatieobject/$zaakUUID/$zaakUUID"
    val requestBody =
        MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("bestandsnaam", fileName)
            .addFormDataPart("titel", title)
            .addFormDataPart("bestandsomvang", file.length().toString())
            .addFormDataPart("formaat", fileMediaType)
            .addFormDataPart(
                "file",
                fileName,
                file.asRequestBody(fileMediaType.toMediaType())
            )
            .addFormDataPart("informatieobjectTypeUUID", INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID)
            .addFormDataPart(
                "vertrouwelijkheidaanduiding",
                vertrouwelijkheidaanduiding
            )
            .addFormDataPart("status", status)
            .addFormDataPart(
                "creatiedatum",
                DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd'T'HH:mm+01:00"
                ).format(ZonedDateTime.now())
            )
            .addFormDataPart("auteur", authorName)
            .addFormDataPart("taal", "dut")
            .build()
    return itestHttpClient.performPostRequest(
        url = createEnkelvoudigInformatieobjectEndpointURI,
        headers = Headers.headersOf(
            "Accept",
            "application/json",
            "Content-Type",
            "multipart/form-data"
        ),
        requestBody = requestBody,
        testUser = testUser
    )
}

@Suppress("LongParameterList")
fun ZacClient.addEnkelvoudigInformatieobjectVersion(
    enkelvoudigInformatieobjectUuid: UUID,
    zaakUuid: UUID,
    file: File,
    fileName: String,
    title: String = DOCUMENT_FILE_TITLE,
    fileMediaType: String,
    vertrouwelijkheidaanduiding: String,
    testUser: TestUser
): ResponseContent {
    val requestBody =
        MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("informatieobjectTypeUUID", INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID)
            .addFormDataPart("bestandsnaam", fileName)
            .addFormDataPart("titel", title)
            .addFormDataPart("formaat", fileMediaType)
            .addFormDataPart("vertrouwelijkheidaanduiding", vertrouwelijkheidaanduiding)
            .addFormDataPart(
                "file",
                fileName,
                file.asRequestBody(fileMediaType.toMediaType())
            )
            .build()
    return itestHttpClient.performPutRequest(
        url = "$ZAC_API_URI/informatieobjecten/informatieobject/$enkelvoudigInformatieobjectUuid?zaak=$zaakUuid",
        headers = Headers.headersOf(
            "Accept",
            "application/json",
            "Content-Type",
            "multipart/form-data"
        ),
        requestBody = requestBody,
        testUser = testUser
    )
}
