/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_USER_MANAGEMENT_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_VERSION_2_RESULTAATTYPE_VERLEEND_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_VERSION_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.queryZacDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_OK
import java.time.ZonedDateTime
import java.util.UUID

private const val ERROR_CODE_PROCESS_DEFINITION_CANNOT_CHANGE = "\"msg.error.process.definition.cannot.change\""
private const val ERROR_CODE_PROCESS_ENGINE_CANNOT_CHANGE = "\"msg.error.process.engine.cannot.change\""

@Isolate
class ZaaktypeConfigurationRestServiceBpmnProcessDefinitionTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zaakHelper = ZaakHelper(ZacClient(itestHttpClient))
    val zaaktypeConfigurationUri = "$ZAC_API_URI/zaakafhandelparameters"

    fun readConfiguration(zaaktypeUuid: UUID) =
        itestHttpClient.performGetRequest(url = "$zaaktypeConfigurationUri/$zaaktypeUuid", testUser = BEHEERDER_1).let {
            it.code shouldBe HTTP_OK
            JSONObject(it.bodyAsString)
        }

    fun storeConfiguration(configuration: JSONObject) =
        itestHttpClient.performPutRequest(
            url = "$zaaktypeConfigurationUri/${configuration.getJSONObject("zaaktype").getString("uuid")}",
            requestBodyAsString = configuration.toString(),
            testUser = BEHEERDER_1
        )

    fun JSONObject.withProcessDefinition(processEngine: String, key: String) = apply {
        put("processDefinition", JSONObject().put("processEngine", processEngine).put("key", key))
    }

    fun readStoredDefinitionKey(zaaktypeUuid: UUID) =
        queryZacDatabase(
            """
            SELECT binding.definition_key
            FROM zaakafhandelcomponent.zaaktype_process_binding binding
            JOIN zaakafhandelcomponent.zaaktype_configuration configuration
                ON configuration.id = binding.zaaktype_configuration_id
            WHERE configuration.zaaktype_uuid = '$zaaktypeUuid'
            """.trimIndent()
        )

    afterSpec {
        val secondVersionConfiguration = "SELECT id FROM zaakafhandelcomponent.zaaktype_configuration " +
            "WHERE zaaktype_uuid = '$ZAAKTYPE_BPMN_TEST_1_VERSION_2_UUID'"
        queryZacDatabase(
            "DELETE FROM zaakafhandelcomponent.zaaktype_zaakafzender_parameters " +
                "WHERE zaaktype_configuration_id IN ($secondVersionConfiguration)"
        )
        queryZacDatabase("DELETE FROM zaakafhandelcomponent.zaaktype_configuration WHERE id IN ($secondVersionConfiguration)")
    }

    given("a second version of a BPMN zaaktype that has no zaken, configured with the process of the first version") {
        val storeResponse = storeConfiguration(
            readConfiguration(ZAAKTYPE_BPMN_TEST_1_UUID).apply {
                remove("id")
                optJSONObject("betrokkeneKoppelingen")?.remove("id")
                optJSONObject("brpDoelbindingen")?.remove("id")
                getJSONObject("zaaktype").put("uuid", ZAAKTYPE_BPMN_TEST_1_VERSION_2_UUID.toString())
                put("mailtemplateKoppelingen", JSONArray())
                put("zaakAfzenders", JSONArray())
                put("zaakNietOntvankelijkResultaattype", JSONObject().put("id", ZAAKTYPE_BPMN_TEST_1_VERSION_2_RESULTAATTYPE_VERLEEND_UUID))
                put("zaakbeeindigParameters", JSONArray())
            }
        )
        storeResponse.code shouldBe HTTP_OK
        val secondVersionConfiguration = JSONObject(storeResponse.bodyAsString)

        `when`("a beheerder reads its configuration") {
            val configuration = readConfiguration(ZAAKTYPE_BPMN_TEST_1_VERSION_2_UUID)

            then("it may change its process definition") {
                configuration.getBoolean("isProcessDefinitionChangeable") shouldBe true
            }
        }

        `when`("a beheerder stores it with a CMMN case definition") {
            val caseDefinitionKey = readConfiguration(ZAAKTYPE_CMMN_TEST_1_UUID).getJSONObject("processDefinition").getString("key")
            val response = storeConfiguration(
                JSONObject(secondVersionConfiguration.toString()).withProcessDefinition("CMMN", caseDefinitionKey)
            )

            then("it is rejected, because a configuration never changes engine") {
                response.code shouldBe HTTP_BAD_REQUEST
                response.bodyAsString shouldContain ERROR_CODE_PROCESS_ENGINE_CANNOT_CHANGE
                readStoredDefinitionKey(ZAAKTYPE_BPMN_TEST_1_VERSION_2_UUID) shouldBe listOf(BPMN_TEST_PROCESS_DEFINITION_KEY)
            }
        }

        `when`("a beheerder stores it with another BPMN process definition") {
            val response = storeConfiguration(
                secondVersionConfiguration.withProcessDefinition("BPMN", BPMN_TEST_USER_MANAGEMENT_PROCESS_DEFINITION_KEY)
            )

            then("it is bound to the other process definition") {
                response.code shouldBe HTTP_OK
                JSONObject(response.bodyAsString).getJSONObject("processDefinition").getString("key") shouldBe
                    BPMN_TEST_USER_MANAGEMENT_PROCESS_DEFINITION_KEY
                readStoredDefinitionKey(ZAAKTYPE_BPMN_TEST_1_VERSION_2_UUID) shouldBe
                    listOf(BPMN_TEST_USER_MANAGEMENT_PROCESS_DEFINITION_KEY)
            }
        }
    }

    given("a BPMN zaaktype version with a zaak") {
        zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_BPMN_TEST_1_UUID,
            startDate = ZonedDateTime.now().minusDays(1),
            testUser = BEHANDELAAR_1
        )

        `when`("a beheerder reads its configuration") {
            val configuration = readConfiguration(ZAAKTYPE_BPMN_TEST_1_UUID)

            then("it may no longer change its process definition") {
                configuration.getBoolean("isProcessDefinitionChangeable") shouldBe false
            }
        }

        `when`("a beheerder stores it with another BPMN process definition") {
            val response = storeConfiguration(
                readConfiguration(ZAAKTYPE_BPMN_TEST_1_UUID)
                    .withProcessDefinition("BPMN", BPMN_TEST_USER_MANAGEMENT_PROCESS_DEFINITION_KEY)
            )

            then("it is rejected, and the configuration keeps its process definition") {
                response.code shouldBe HTTP_BAD_REQUEST
                response.bodyAsString shouldContain ERROR_CODE_PROCESS_DEFINITION_CANNOT_CHANGE
                readStoredDefinitionKey(ZAAKTYPE_BPMN_TEST_1_UUID) shouldBe listOf(BPMN_TEST_PROCESS_DEFINITION_KEY)
            }
        }
    }

    given("a CMMN zaaktype configuration") {
        val configuration = readConfiguration(ZAAKTYPE_CMMN_TEST_1_UUID)
        val caseDefinitionKey = configuration.getJSONObject("processDefinition").getString("key")

        `when`("a beheerder stores it with a BPMN process definition") {
            val response = storeConfiguration(configuration.withProcessDefinition("BPMN", BPMN_TEST_PROCESS_DEFINITION_KEY))

            then("it is rejected, because a configuration never changes engine, and it keeps its case definition") {
                response.code shouldBe HTTP_BAD_REQUEST
                response.bodyAsString shouldContain ERROR_CODE_PROCESS_ENGINE_CANNOT_CHANGE
                readStoredDefinitionKey(ZAAKTYPE_CMMN_TEST_1_UUID) shouldBe listOf(caseDefinitionKey)
            }
        }
    }
})
