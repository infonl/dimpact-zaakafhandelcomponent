/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_RESULTAATTYPE_AFGEBROKEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_RESULTAATTYPE_VERLEEND_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_VERSION_2_RESULTAATTYPE_AFGEBROKEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_VERSION_2_RESULTAATTYPE_VERLEEND_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_VERSION_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_VERSION_2_RESULTAATTYPE_AFGEBROKEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_VERSION_2_RESULTAATTYPE_GEWEIGERD_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_VERSION_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ZaaktypeConfigurationType.BPMN
import nl.info.zac.itest.config.ZaaktypeConfigurationType.CMMN
import nl.info.zac.itest.config.ZaaktypeConfigurationUnderTest
import nl.info.zac.itest.util.queryZacDatabase
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

/** Resultaattype of zaaktype test 3 whose omschrijving does not occur in the second versions. */
private const val ZAAKTYPE_TEST_3_RESULTAATTYPE_EIGENSCHAP_UUID = "ce19f9dc-efd7-4f6a-a95f-7b22f5ab9a09"

@Isolate
class NotificationZaaktypeCompletionParametersTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zaaktypeCmmnConfigurationUri = "$ZAC_API_URI/zaakafhandelparameters"
    val zaaktypeBpmnConfigurationUri = "$ZAC_API_URI/zaaktype-bpmn-configuration"

    fun read(url: String) = itestHttpClient.performGetRequest(url = url, testUser = BEHEERDER_1).let {
        it.code shouldBe HTTP_OK
        it.bodyAsString
    }

    fun storeBpmnConfiguration(configuration: String) {
        itestHttpClient.performJSONPostRequest(
            url = zaaktypeBpmnConfigurationUri,
            requestBodyAsString = configuration,
            testUser = BEHEERDER_1
        ).code shouldBe HTTP_OK
    }

    fun sendZaaktypeCreatedNotification(zaaktypeUuid: UUID) {
        val zaaktypeUri = "$OPEN_ZAAK_BASE_URI/catalogi/api/v1/zaaktypen/$zaaktypeUuid"
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
                    "kanaal" to "zaaktypen",
                    "resource" to "zaaktype",
                    "resourceUrl" to zaaktypeUri,
                    "hoofdObject" to zaaktypeUri,
                    "actie" to "create",
                    "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                )
            ).toString()
        ).code shouldBe HTTP_NO_CONTENT
    }

    fun readStoredConfiguration(zaaktypeUuid: UUID, columns: String) =
        queryZacDatabase(
            "SELECT $columns FROM zaakafhandelcomponent.zaaktype_configuration WHERE zaaktype_uuid = '$zaaktypeUuid'"
        )

    fun readStoredZaakbeeindigResultaattypen(zaaktypeUuid: UUID) =
        queryZacDatabase(
            """
            SELECT parameters.resultaattype_uuid || ' ' || parameters.resultaattype_omschrijving
            FROM zaakafhandelcomponent.zaaktype_completion_parameters parameters
            JOIN zaakafhandelcomponent.zaaktype_configuration configuration
                ON configuration.id = parameters.zaaktype_configuration_id
            WHERE configuration.zaaktype_uuid = '$zaaktypeUuid'
            """.trimIndent()
        )

    listOf(
        ZaaktypeConfigurationUnderTest(
            configurationType = CMMN,
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_1_UUID,
            secondVersionZaaktypeUuid = ZAAKTYPE_CMMN_TEST_1_VERSION_2_UUID,
            readConfiguration = { read("$zaaktypeCmmnConfigurationUri/$ZAAKTYPE_CMMN_TEST_1_UUID") },
            storeConfiguration = {
                itestHttpClient.performPutRequest(
                    url = zaaktypeCmmnConfigurationUri,
                    requestBodyAsString = it,
                    testUser = BEHEERDER_1
                ).code shouldBe HTTP_OK
            },
            nietOntvankelijkResultaattypeUuid = ZAAKTYPE_TEST_1_RESULTAATTYPE_GEWEIGERD_UUID,
            nietOntvankelijkResultaattypeOmschrijving = "Geweigerd",
            zaakbeeindigResultaattypeUuid = ZAAKTYPE_TEST_1_RESULTAATTYPE_AFGEBROKEN_UUID,
            secondVersionNietOntvankelijkResultaattypeUuid = ZAAKTYPE_CMMN_TEST_1_VERSION_2_RESULTAATTYPE_GEWEIGERD_UUID,
            secondVersionZaakbeeindigResultaattypeUuid = ZAAKTYPE_CMMN_TEST_1_VERSION_2_RESULTAATTYPE_AFGEBROKEN_UUID
        ),
        ZaaktypeConfigurationUnderTest(
            configurationType = BPMN,
            zaaktypeUuid = ZAAKTYPE_BPMN_TEST_1_UUID,
            secondVersionZaaktypeUuid = ZAAKTYPE_BPMN_TEST_1_VERSION_2_UUID,
            readConfiguration = { read("$zaaktypeBpmnConfigurationUri/$BPMN_TEST_PROCESS_DEFINITION_KEY") },
            storeConfiguration = ::storeBpmnConfiguration,
            nietOntvankelijkResultaattypeUuid = ZAAKTYPE_BPMN_TEST_1_RESULTAATTYPE_VERLEEND_UUID.toString(),
            nietOntvankelijkResultaattypeOmschrijving = "Verleend",
            zaakbeeindigResultaattypeUuid = ZAAKTYPE_BPMN_TEST_1_RESULTAATTYPE_AFGEBROKEN_UUID.toString(),
            secondVersionNietOntvankelijkResultaattypeUuid = ZAAKTYPE_BPMN_TEST_1_VERSION_2_RESULTAATTYPE_VERLEEND_UUID,
            secondVersionZaakbeeindigResultaattypeUuid = ZAAKTYPE_BPMN_TEST_1_VERSION_2_RESULTAATTYPE_AFGEBROKEN_UUID
        )
    ).forEach { zaaktypeConfigurationUnderTest ->
        var originalZaaktypeConfiguration: String? = null
        afterSpec {
            val secondVersionConfiguration = "SELECT id FROM zaakafhandelcomponent.zaaktype_configuration " +
                "WHERE zaaktype_uuid = '${zaaktypeConfigurationUnderTest.secondVersionZaaktypeUuid}'"
            queryZacDatabase(
                "DELETE FROM zaakafhandelcomponent.zaaktype_zaakafzender_parameters " +
                    "WHERE zaaktype_configuration_id IN ($secondVersionConfiguration)"
            )
            queryZacDatabase("DELETE FROM zaakafhandelcomponent.zaaktype_configuration WHERE id IN ($secondVersionConfiguration)")
            originalZaaktypeConfiguration?.let(zaaktypeConfigurationUnderTest.storeConfiguration)
        }

        given(
            """a ${zaaktypeConfigurationUnderTest.configurationType} zaaktype configuration whose zaak beeindigen
                gegevens point at resultaattypen of its zaaktype version and at a resultaattype whose omschrijving
                the next version does not have"""
        ) {
            val zaakbeeindigRedenen = JSONArray(read("$zaaktypeCmmnConfigurationUri/zaakbeeindigredenen"))
            originalZaaktypeConfiguration = zaaktypeConfigurationUnderTest.readConfiguration()
            zaaktypeConfigurationUnderTest.storeConfiguration(
                JSONObject(originalZaaktypeConfiguration).apply {
                    put(
                        "zaakNietOntvankelijkResultaattype",
                        JSONObject().put("id", zaaktypeConfigurationUnderTest.nietOntvankelijkResultaattypeUuid)
                    )
                    put(
                        "zaakbeeindigParameters",
                        JSONArray()
                            .put(
                                JSONObject()
                                    .put("zaakbeeindigReden", zaakbeeindigRedenen.getJSONObject(0))
                                    .put(
                                        "resultaattype",
                                        JSONObject().put("id", zaaktypeConfigurationUnderTest.zaakbeeindigResultaattypeUuid)
                                    )
                            )
                            .put(
                                JSONObject()
                                    .put("zaakbeeindigReden", zaakbeeindigRedenen.getJSONObject(1))
                                    .put(
                                        "resultaattype",
                                        JSONObject().put("id", ZAAKTYPE_TEST_3_RESULTAATTYPE_EIGENSCHAP_UUID)
                                    )
                            )
                    )
                }.toString()
            )
            val productaanvraagtype = JSONObject(originalZaaktypeConfiguration).optString("productaanvraagtype")

            `when`("a notification is received that a second version of the zaaktype was published") {
                sendZaaktypeCreatedNotification(zaaktypeConfigurationUnderTest.secondVersionZaaktypeUuid)

                then(
                    """the second version gets a copy of the configuration that references the resultaattypen of the
                        second version with the same omschrijving, and drops the reference whose omschrijving the
                        second version does not have"""
                ) {
                    eventually(30.seconds) {
                        readStoredConfiguration(
                            zaaktypeConfigurationUnderTest.secondVersionZaaktypeUuid,
                            "niet_ontvankelijk_resultaattype_uuid || ' ' || niet_ontvankelijk_resultaattype_omschrijving"
                        ) shouldBe listOf(
                            "${zaaktypeConfigurationUnderTest.secondVersionNietOntvankelijkResultaattypeUuid} " +
                                zaaktypeConfigurationUnderTest.nietOntvankelijkResultaattypeOmschrijving
                        )
                        readStoredZaakbeeindigResultaattypen(
                            zaaktypeConfigurationUnderTest.secondVersionZaaktypeUuid
                        ) shouldBe listOf("${zaaktypeConfigurationUnderTest.secondVersionZaakbeeindigResultaattypeUuid} Afgebroken")
                    }
                }

                then("the configuration of the first version keeps referencing the resultaattypen of the first version") {
                    readStoredConfiguration(
                        zaaktypeConfigurationUnderTest.zaaktypeUuid,
                        "niet_ontvankelijk_resultaattype_uuid"
                    ) shouldBe listOf(zaaktypeConfigurationUnderTest.nietOntvankelijkResultaattypeUuid)
                }

                then("the second version keeps the productaanvraagtype of the first version") {
                    readStoredConfiguration(
                        zaaktypeConfigurationUnderTest.secondVersionZaaktypeUuid,
                        "COALESCE(productaanvraagtype, '')"
                    ) shouldBe listOf(productaanvraagtype)
                }
            }

            if (zaaktypeConfigurationUnderTest.configurationType == BPMN) {
                `when`("a beheerder stores the configuration of the second version with the productaanvraagtype of the first") {
                    storeBpmnConfiguration(
                        JSONObject(originalZaaktypeConfiguration).apply {
                            remove("id")
                            optJSONObject("betrokkeneKoppelingen")?.remove("id")
                            optJSONObject("brpDoelbindingen")?.remove("id")
                            put("zaaktypeUuid", zaaktypeConfigurationUnderTest.secondVersionZaaktypeUuid.toString())
                            put(
                                "zaakNietOntvankelijkResultaattype",
                                JSONObject().put("id", zaaktypeConfigurationUnderTest.secondVersionNietOntvankelijkResultaattypeUuid)
                            )
                            put("zaakbeeindigParameters", JSONArray())
                        }.toString()
                    )

                    then("it is accepted, because both configurations belong to versions of the same zaaktype") {
                        readStoredConfiguration(
                            zaaktypeConfigurationUnderTest.secondVersionZaaktypeUuid,
                            "productaanvraagtype"
                        ) shouldBe listOf(productaanvraagtype)
                    }
                }
            }
        }
    }
})
