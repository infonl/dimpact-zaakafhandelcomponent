/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm.HMAC256
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.config.ItestConfiguration.BRON_ORGANISATIE
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_IN_BEWERKING
import nl.info.zac.itest.config.ItestConfiguration.FAKE_AUTHOR_NAME
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_CLIENT_ID
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_CLIENT_SECRET
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_EXTERNAL_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.Headers
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.URLDecoder
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
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
     * Creates a zaakeigenschap directly in Open Zaak's ZRC API, bypassing ZAC. Use this to mark a
     * zaak as zaakspecifiek geautoriseerd in integration tests, by creating a zaakeigenschap with
     * naam [eigenschapNaam] (e.g. "ZAAK_GEAUTORISEERD") and value [waarde] (e.g. "true"). The
     * zaaktype of [zaakUUID] must define an eigenschap with that naam in Open Zaak's catalogi API.
     *
     * @return the UUID of the created zaakeigenschap, for use with [sendZaakeigenschapCreateNotification]
     */
    fun createZaakeigenschap(
        zaakUUID: UUID,
        zaaktypeUUID: UUID,
        eigenschapNaam: String,
        waarde: String
    ): UUID {
        val eigenschapUrl = getEigenschapUrl(zaaktypeUUID, eigenschapNaam)
        val requestBody = JSONObject(
            mapOf(
                "zaak" to "$OPEN_ZAAK_EXTERNAL_URI/zaken/api/v1/zaken/$zaakUUID",
                "eigenschap" to eigenschapUrl,
                "waarde" to waarde
            )
        ).toString()
        return itestHttpClient.performZgwApiPostRequest(
            url = "$OPEN_ZAAK_EXTERNAL_URI/zaken/api/v1/zaken/$zaakUUID/zaakeigenschappen",
            requestBodyAsString = requestBody
        ).let { response ->
            JSONObject(response.bodyAsString).getString("uuid").run(UUID::fromString)
        }
    }

    /**
     * Sends a request to the ZAC notification endpoint to notify ZAC about the creation of the
     * zaakeigenschap identified by [zaakeigenschapUUID], so that ZAC will reindex the zaak (and its
     * taken and documenten) in Solr. Use this after [createZaakeigenschap], which bypasses ZAC and
     * therefore triggers no real notification, passing the UUID it returned: ZAC reads the
     * zaakeigenschap back from Open Zaak by this UUID to decide whether to reindex, so a UUID that
     * does not resolve to a real zaakeigenschap silently skips the reindex.
     */
    fun sendZaakeigenschapCreateNotification(zaakUUID: UUID, zaakeigenschapUUID: UUID) {
        val zaakUrl = "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakUUID"
        itestHttpClient.performJSONPostRequest(
            url = "$ZAC_API_URI/notificaties",
            headers = Headers.headersOf(
                "Content-Type",
                "application/json",
                "Authorization",
                OPEN_NOTIFICATIONS_API_SECRET_KEY
            ),
            requestBodyAsString = JSONObject(
                mapOf(
                    "kanaal" to "zaken",
                    "resource" to "zaakeigenschap",
                    "hoofdObject" to zaakUrl,
                    "resourceUrl" to "$zaakUrl/zaakeigenschappen/$zaakeigenschapUUID",
                    "actie" to "create",
                    "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                )
            ).toString()
        ).run {
            code shouldBe HTTP_NO_CONTENT
        }
    }

    /**
     * Fetches the URL of an eigenschap with the given naam, defined for the given zaaktype, from
     * Open Zaak's catalogi API. This returns the URL as Open Zaak itself serves it, which is the
     * URL format that Open Zaak accepts in the ZRC API when creating a zaakeigenschap.
     */
    private fun getEigenschapUrl(zaaktypeUUID: UUID, eigenschapNaam: String): String {
        val zaaktypeUrl = "$OPEN_ZAAK_EXTERNAL_URI/catalogi/api/v1/zaaktypen/$zaaktypeUUID"
        val response = itestHttpClient.performZgwApiGetRequest(
            url = "$OPEN_ZAAK_EXTERNAL_URI/catalogi/api/v1/eigenschappen?zaaktype=$zaaktypeUrl&status=alles"
        )
        val results = JSONObject(response.bodyAsString).getJSONArray("results")
        for (index in 0 until results.length()) {
            val eigenschap = results.getJSONObject(index)
            if (eigenschap.getString("naam") == eigenschapNaam) {
                return eigenschap.getString("url")
            }
        }
        error("No eigenschap with naam '$eigenschapNaam' found for zaaktype '$zaaktypeUrl'")
    }

    /**
     * Creates an enkelvoudig informatieobject directly in Open Zaak's DRC API,
     * bypassing ZAC. Use this to simulate externally created documents in integration tests.
     *
     * The file is loaded from test resources and base64-encoded for the [inhoud] field.
     * The [informatieobjecttype] URL is fetched from Open Zaak's catalogi API to ensure it
     * matches the URL format that Open Zaak itself uses and will accept in the DRC API.
     *
     * @param fileName Name of the file in test resources (e.g. "fäkeTestDocument.pdf")
     * @param title Document title; defaults to [DOCUMENT_FILE_TITLE]
     * @param informatieobjectTypeUUID UUID of the informatieobjecttype in Open Zaak;
     *   defaults to the "bijlage" type ([INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID])
     * @param vertrouwelijkheidaanduiding Confidentiality level as a lowercase DRC API enum value
     *   (e.g. "zaakvertrouwelijk", "openbaar"). Note: ZAC API constants like
     *   [DOCUMENT_VERTROUWELIJKHEIDAANDUIDING_VERTROUWELIJK] use uppercase and cannot be passed
     *   directly here.
     * @return [ResponseContent] with the Open Zaak API response (HTTP 201 on success)
     */
    fun createEnkelvoudigInformatieobject(
        fileName: String,
        title: String = DOCUMENT_FILE_TITLE,
        informatieobjectTypeUUID: UUID = UUID.fromString(INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID),
        vertrouwelijkheidaanduiding: String = "zaakvertrouwelijk"
    ): ResponseContent {
        val informatieobjecttypeUrl = getInformatieobjecttypeUrl(informatieobjectTypeUUID)
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
                "informatieobjecttype" to informatieobjecttypeUrl,
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

    /**
     * Fetches the URL of an informatieobjecttype from Open Zaak's catalogi API.
     * This returns the URL as Open Zaak itself serves it, which is the URL format
     * that Open Zaak accepts in the DRC API when creating enkelvoudiginformatieobjecten.
     */
    private fun getInformatieobjecttypeUrl(informatieobjectTypeUUID: UUID): String =
        itestHttpClient.performZgwApiGetRequest(
            url = "$OPEN_ZAAK_EXTERNAL_URI/catalogi/api/v1/informatieobjecttypen/$informatieobjectTypeUUID"
        ).let { response ->
            JSONObject(response.bodyAsString).getString("url")
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
