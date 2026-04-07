/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm.HMAC256
import nl.info.zac.itest.config.ItestConfiguration.BRON_ORGANISATIE
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_IN_BEWERKING
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
import nl.info.zac.itest.config.ItestConfiguration.FAKE_AUTHOR_NAME
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_CLIENT_ID
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_CLIENT_SECRET
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_EXTERNAL_URI
import org.json.JSONObject
import java.io.File
import java.net.URLDecoder
import java.time.LocalDate
import java.util.Base64
import java.util.Date
import java.util.UUID

class OpenZaakClient(
    val itestHttpClient: ItestHttpClient
) {
    fun getRolesForZaak(zaakUUID: UUID): ResponseContent =
        itestHttpClient.performZgwApiGetRequest(
            url = "$OPEN_ZAAK_EXTERNAL_URI/zaken/api/v1/rollen?zaak=$OPEN_ZAAK_EXTERNAL_URI/zaken/api/v1/zaken/$zaakUUID"
        )

    /**
     * Creates an enkelvoudig informatieobject directly in Open Zaak's DRC API,
     * bypassing ZAC. Use this to simulate externally created documents in integration tests.
     *
     * The file is loaded from test resources and base64-encoded for the [inhoud] field.
     *
     * @param fileName Name of the file in test resources (e.g. "fäkeTestDocument.pdf")
     * @param title Document title; defaults to [DOCUMENT_FILE_TITLE]
     * @param informatieobjectTypeUUID UUID of the informatieobjecttype in Open Zaak;
     *   defaults to the "bijlage" type ([INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID])
     * @param vertrouwelijkheidaanduiding Confidentiality level;
     *   defaults to [DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK]
     * @return [ResponseContent] with the Open Zaak API response (HTTP 201 on success)
     */
    fun createEnkelvoudigInformatieobject(
        fileName: String,
        title: String = DOCUMENT_FILE_TITLE,
        informatieobjectTypeUUID: UUID = UUID.fromString(INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID),
        vertrouwelijkheidaanduiding: String = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
    ): ResponseContent {
        val resource = Thread.currentThread().contextClassLoader.getResource(fileName)
            ?: error("Test resource not found on classpath: '$fileName'")
        val file = File(URLDecoder.decode(resource.path, Charsets.UTF_8))
        val encodedContent = Base64.getEncoder().encodeToString(file.readBytes())
        val requestBody = JSONObject(
            mapOf(
                "bronorganisatie" to BRON_ORGANISATIE,
                "creatiedatum" to LocalDate.now().toString(),
                "titel" to title,
                "auteur" to FAKE_AUTHOR_NAME,
                "taal" to "dut",
                "informatieobjecttype" to
                    "$OPEN_ZAAK_BASE_URI/catalogi/api/v1/informatieobjecttypen/$informatieobjectTypeUUID",
                "inhoud" to encodedContent,
                "bestandsnaam" to fileName,
                "bestandsomvang" to file.length(),
                "vertrouwelijkheidaanduiding" to vertrouwelijkheidaanduiding,
                "status" to DOCUMENT_STATUS_IN_BEWERKING
            )
        ).toString()
        return itestHttpClient.performZgwApiPostRequest(
            url = "$OPEN_ZAAK_EXTERNAL_URI/documenten/api/v1/enkelvoudiginformatieobjecten",
            requestBodyAsString = requestBody
        )
    }
}

/**
 * Generates a JWT token for OpenZaak client authentication from our integration tests.
 * Note that no user claims are added, as this is not required for these requests from
 * our integration tests.
 */
fun generateOpenZaakJwtToken(): String =
    JWT.create().withIssuer(OPEN_ZAAK_CLIENT_ID)
        .withIssuedAt(Date())
        .withHeader(mapOf("client_identifier" to OPEN_ZAAK_CLIENT_ID))
        .withClaim("client_id", OPEN_ZAAK_CLIENT_ID)
        .sign(HMAC256(OPEN_ZAAK_CLIENT_SECRET))
