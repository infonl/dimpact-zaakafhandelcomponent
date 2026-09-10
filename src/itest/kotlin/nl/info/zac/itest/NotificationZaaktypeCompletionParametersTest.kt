/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.RESULTAAT_TYPE_GEWEIGERD_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_RESULTAATTYPE_AFGEBROKEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_RESULTAATTYPE_VERLEEND_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

private const val ZAAKTYPE_TEST_1_RESULTAATTYPE_GEWEIGERD_UUID = "f940861c-f8f8-4e45-8317-a6175561af0a"
private const val ZAAKTYPE_TEST_1_RESULTAATTYPE_AFGEBROKEN_UUID = "31f56eaa-4515-437e-a3ab-9f7f71e8ee6f"
private const val ZAAKTYPE_TEST_3_RESULTAATTYPE_AFGEBROKEN_UUID = "060b1651-4795-4982-bf66-584391bf0421"
private const val ZAAKTYPE_TEST_3_RESULTAATTYPE_VERLEEND_UUID = "2b774ae4-68b0-462c-b6a0-e48b861ee148"

class NotificationZaaktypeCompletionParametersTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zaaktypeCmmnConfigurationUri = "$ZAC_API_URI/zaakafhandelparameters"
    val zaaktypeBpmnConfigurationUri = "$ZAC_API_URI/zaaktype-bpmn-configuration"

    data class ZaaktypeConfigurationUnderTest(
        val configurationType: String,
        val zaaktypeUuid: UUID,
        val readConfiguration: () -> String,
        val storeConfiguration: (String) -> Unit,
        val previousNietOntvankelijkResultaattypeUuid: String,
        val previousZaakbeeindigResultaattypeUuid: String,
        val expectedNietOntvankelijkResultaattypeUuid: String,
        val expectedZaakbeeindigResultaattypeUuid: String
    )

    fun read(url: String) = itestHttpClient.performGetRequest(url = url, testUser = BEHEERDER_1).let {
        it.code shouldBe HTTP_OK
        it.bodyAsString
    }

    listOf(
        ZaaktypeConfigurationUnderTest(
            configurationType = "CMMN",
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_1_UUID,
            readConfiguration = { read("$zaaktypeCmmnConfigurationUri/$ZAAKTYPE_CMMN_TEST_1_UUID") },
            storeConfiguration = {
                itestHttpClient.performPutRequest(
                    url = zaaktypeCmmnConfigurationUri,
                    requestBodyAsString = it,
                    testUser = BEHEERDER_1
                ).code shouldBe HTTP_OK
            },
            previousNietOntvankelijkResultaattypeUuid = RESULTAAT_TYPE_GEWEIGERD_UUID,
            previousZaakbeeindigResultaattypeUuid = ZAAKTYPE_TEST_3_RESULTAATTYPE_AFGEBROKEN_UUID,
            expectedNietOntvankelijkResultaattypeUuid = ZAAKTYPE_TEST_1_RESULTAATTYPE_GEWEIGERD_UUID,
            expectedZaakbeeindigResultaattypeUuid = ZAAKTYPE_TEST_1_RESULTAATTYPE_AFGEBROKEN_UUID
        ),
        ZaaktypeConfigurationUnderTest(
            configurationType = "BPMN",
            zaaktypeUuid = ZAAKTYPE_BPMN_TEST_1_UUID,
            readConfiguration = { read("$zaaktypeBpmnConfigurationUri/$BPMN_TEST_PROCESS_DEFINITION_KEY") },
            storeConfiguration = {
                itestHttpClient.performJSONPostRequest(
                    url = zaaktypeBpmnConfigurationUri,
                    requestBodyAsString = it,
                    testUser = BEHEERDER_1
                ).code shouldBe HTTP_OK
            },
            previousNietOntvankelijkResultaattypeUuid = ZAAKTYPE_TEST_3_RESULTAATTYPE_VERLEEND_UUID,
            previousZaakbeeindigResultaattypeUuid = ZAAKTYPE_TEST_3_RESULTAATTYPE_AFGEBROKEN_UUID,
            expectedNietOntvankelijkResultaattypeUuid = ZAAKTYPE_BPMN_TEST_1_RESULTAATTYPE_VERLEEND_UUID.toString(),
            expectedZaakbeeindigResultaattypeUuid = ZAAKTYPE_BPMN_TEST_1_RESULTAATTYPE_AFGEBROKEN_UUID.toString()
        )
    ).forEach { zaaktypeConfigurationUnderTest ->
        var originalZaaktypeConfiguration: String? = null
        afterSpec {
            originalZaaktypeConfiguration?.let(zaaktypeConfigurationUnderTest.storeConfiguration)
        }

        given(
            """a ${zaaktypeConfigurationUnderTest.configurationType} zaaktype configuration whose zaak beeindigen
                gegevens point at resultaattypen of another zaaktype"""
        ) {
            val zaakbeeindigReden = JSONArray(read("$zaaktypeCmmnConfigurationUri/zaakbeeindigredenen"))
                .getJSONObject(0)

            originalZaaktypeConfiguration = zaaktypeConfigurationUnderTest.readConfiguration()
            zaaktypeConfigurationUnderTest.storeConfiguration(
                JSONObject(originalZaaktypeConfiguration).apply {
                    put(
                        "zaakNietOntvankelijkResultaattype",
                        JSONObject().put(
                            "id",
                            zaaktypeConfigurationUnderTest.previousNietOntvankelijkResultaattypeUuid
                        )
                    )
                    put(
                        "zaakbeeindigParameters",
                        JSONArray().put(
                            JSONObject()
                                .put("zaakbeeindigReden", zaakbeeindigReden)
                                .put(
                                    "resultaattype",
                                    JSONObject().put(
                                        "id",
                                        zaaktypeConfigurationUnderTest.previousZaakbeeindigResultaattypeUuid
                                    )
                                )
                        )
                    )
                }.toString()
            )

            `when`("a zaaktype notification for this zaaktype is received") {
                val zaaktypeUri = "$OPEN_ZAAK_BASE_URI/catalogi/api/v1/zaaktypen/" +
                    "${zaaktypeConfigurationUnderTest.zaaktypeUuid}"
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
                            "resourceUrl" to zaaktypeUri,
                            "hoofdObject" to zaaktypeUri,
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
                        val zaaktypeConfiguration = JSONObject(zaaktypeConfigurationUnderTest.readConfiguration())

                        zaaktypeConfiguration
                            .getJSONObject("zaakNietOntvankelijkResultaattype")
                            .getString("id") shouldBe
                            zaaktypeConfigurationUnderTest.expectedNietOntvankelijkResultaattypeUuid

                        val zaakbeeindigParameters = zaaktypeConfiguration.getJSONArray("zaakbeeindigParameters")
                        zaakbeeindigParameters.length() shouldBe 1
                        zaakbeeindigParameters
                            .getJSONObject(0)
                            .getJSONObject("resultaattype")
                            .getString("id") shouldBe
                            zaaktypeConfigurationUnderTest.expectedZaakbeeindigResultaattypeUuid
                    }
                }
            }
        }
    }
})
