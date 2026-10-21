/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrder
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID

private val ROUND_TRIPPED_FIELDS = listOf(
    "processDefinition",
    "intakeMail",
    "afrondenMail",
    "humanTaskParameters",
    "userEventListenerParameters",
    "automaticEmailConfirmation",
    "defaultGroepId",
    "defaultBehandelaarId",
    "productaanvraagtype",
    "einddatumGeplandWaarschuwing",
    "uiterlijkeEinddatumAfdoeningWaarschuwing",
    "smartDocuments",
    "betrokkeneKoppelingen",
    "brpDoelbindingen",
    "zaakNietOntvankelijkResultaattype",
    "zaakbeeindigParameters",
    "mailtemplateKoppelingen",
    "zaakAfzenders"
)

/**
 * Leaves out the database ids, which a store may give to new rows of the same settings.
 */
private fun Any?.withoutDatabaseIds(): Any? = when (this) {
    is JSONObject -> JSONObject().also { copy ->
        keys().forEach { key ->
            val value = get(key)
            if (!(key == "id" && value is Number)) copy.put(key, value.withoutDatabaseIds())
        }
    }
    is JSONArray -> JSONArray().also { copy -> forEach { copy.put(it.withoutDatabaseIds()) } }
    else -> this
}

private fun JSONObject.roundTrippedFields() = JSONObject().also { fields ->
    ROUND_TRIPPED_FIELDS.forEach { field -> fields.put(field, opt(field).withoutDatabaseIds() ?: JSONObject.NULL) }
}.toString()

class ZaaktypeConfigurationRestServiceRoundTripTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    fun read(zaaktypeUuid: UUID) = itestHttpClient.performGetRequest(
        url = "$ZAC_API_URI/zaakafhandelparameters/$zaaktypeUuid",
        testUser = BEHEERDER_1
    ).let {
        it.code shouldBe HTTP_OK
        JSONObject(it.bodyAsString)
    }

    listOf(
        "CMMN" to ZAAKTYPE_CMMN_TEST_3_UUID,
        "BPMN" to ZAAKTYPE_BPMN_TEST_1_UUID
    ).forEach { (engine, zaaktypeUuid) ->
        given("the stored configuration of a $engine zaaktype") {
            val storedConfiguration = read(zaaktypeUuid)

            `when`("a beheerder stores it again as it was read, and reads it back") {
                val storeResponse = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/zaakafhandelparameters/$zaaktypeUuid",
                    requestBodyAsString = storedConfiguration.toString(),
                    testUser = BEHEERDER_1
                )
                val readBackConfiguration = read(zaaktypeUuid)

                then("the configuration is stored") {
                    storeResponse.code shouldBe HTTP_OK
                }

                and("the engine-agnostic settings and the engine block have the same values as before") {
                    readBackConfiguration.roundTrippedFields() shouldEqualJsonIgnoringOrder
                        storedConfiguration.roundTrippedFields()
                }
            }
        }
    }
})
