/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.RESULTAAT_TYPE_GEWEIGERD_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.BEHEERDER_1
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.seconds

private const val ZAAKTYPE_TEST_1_RESULTAATTYPE_GEWEIGERD_UUID = "f940861c-f8f8-4e45-8317-a6175561af0a"
private const val ZAAKTYPE_TEST_1_RESULTAATTYPE_AFGEBROKEN_UUID = "31f56eaa-4515-437e-a3ab-9f7f71e8ee6f"
private const val ZAAKTYPE_TEST_3_RESULTAATTYPE_AFGEBROKEN_UUID = "060b1651-4795-4982-bf66-584391bf0421"

class NotificationZaaktypeCompletionParametersTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zaaktypeCmmnConfigurationUri = "$ZAC_API_URI/zaakafhandelparameters"

    fun readZaaktypeCmmnConfiguration() = itestHttpClient.performGetRequest(
        url = "$zaaktypeCmmnConfigurationUri/$ZAAKTYPE_CMMN_TEST_1_UUID",
        testUser = BEHEERDER_1
    ).let {
        it.code shouldBe HTTP_OK
        it.bodyAsString
    }

    fun storeZaaktypeCmmnConfiguration(zaaktypeCmmnConfiguration: String) = itestHttpClient.performPutRequest(
        url = zaaktypeCmmnConfigurationUri,
        requestBodyAsString = zaaktypeCmmnConfiguration,
        testUser = BEHEERDER_1
    ).code shouldBe HTTP_OK

    // this test changes the configuration of a zaaktype that other tests rely on, so restore it afterwards
    var originalZaaktypeCmmnConfiguration: String? = null
    afterSpec {
        originalZaaktypeCmmnConfiguration?.let(::storeZaaktypeCmmnConfiguration)
    }

    given("a zaaktype configuration whose zaak beeindigen gegevens point at resultaattypen of another zaaktype") {
        val zaakbeeindigReden = itestHttpClient.performGetRequest(
            url = "$zaaktypeCmmnConfigurationUri/zaakbeeindigredenen",
            testUser = BEHEERDER_1
        ).let {
            it.code shouldBe HTTP_OK
            JSONArray(it.bodyAsString).getJSONObject(0)
        }

        originalZaaktypeCmmnConfiguration = readZaaktypeCmmnConfiguration()
        storeZaaktypeCmmnConfiguration(
            JSONObject(originalZaaktypeCmmnConfiguration).apply {
                put(
                    "zaakNietOntvankelijkResultaattype",
                    JSONObject().put("id", RESULTAAT_TYPE_GEWEIGERD_UUID)
                )
                put(
                    "zaakbeeindigParameters",
                    JSONArray().put(
                        JSONObject()
                            .put("zaakbeeindigReden", zaakbeeindigReden)
                            .put(
                                "resultaattype",
                                JSONObject().put("id", ZAAKTYPE_TEST_3_RESULTAATTYPE_AFGEBROKEN_UUID)
                            )
                    )
                )
            }.toString()
        )

        `when`("a zaaktype notification for this zaaktype is received") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/notificaties",
                headers = Headers.headersOf(
                    "Content-Type",
                    "application/json",
                    "Authorization",
                    OPEN_NOTIFICATIONS_API_SECRET_KEY
                ),
                requestBodyAsString = JSONObject(
                    mapOf(
                        "kanaal" to "zaaktypen",
                        "resource" to "zaaktype",
                        "resourceUrl" to "$OPEN_ZAAK_BASE_URI/catalogi/api/v1/zaaktypen/$ZAAKTYPE_CMMN_TEST_1_UUID",
                        "hoofdObject" to "$OPEN_ZAAK_BASE_URI/catalogi/api/v1/zaaktypen/$ZAAKTYPE_CMMN_TEST_1_UUID",
                        "actie" to "update",
                        "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                    )
                ).toString()
            )
            response.code shouldBe HTTP_NO_CONTENT

            then(
                """the resultaattypen are remapped to the resultaattypen of the zaaktype with the same omschrijving
                    and not to the first resultaattype of the zaaktype"""
            ) {
                eventually(30.seconds) {
                    val zaaktypeCmmnConfiguration = JSONObject(readZaaktypeCmmnConfiguration())

                    zaaktypeCmmnConfiguration
                        .getJSONObject("zaakNietOntvankelijkResultaattype")
                        .getString("id") shouldBe ZAAKTYPE_TEST_1_RESULTAATTYPE_GEWEIGERD_UUID

                    val zaakbeeindigParameters = zaaktypeCmmnConfiguration.getJSONArray("zaakbeeindigParameters")
                    zaakbeeindigParameters.length() shouldBe 1
                    zaakbeeindigParameters
                        .getJSONObject(0)
                        .getJSONObject("resultaattype")
                        .getString("id") shouldBe ZAAKTYPE_TEST_1_RESULTAATTYPE_AFGEBROKEN_UUID
                }
            }
        }
    }
})
