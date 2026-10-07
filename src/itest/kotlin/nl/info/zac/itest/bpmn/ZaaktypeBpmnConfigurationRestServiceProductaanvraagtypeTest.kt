/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_PERMISSION_CHECK_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_3
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_RESULTAATTYPE_AFGEBROKEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_UUID
import nl.info.zac.itest.util.queryZacDatabase
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_OK

class ZaaktypeBpmnConfigurationRestServiceProductaanvraagtypeTest : BehaviorSpec({
    val zacClient = ZacClient(ItestHttpClient())

    fun storeBpmnTest5Configuration(productaanvraagType: String) = zacClient.createZaaktypeBpmnConfiguration(
        zaakTypeUuid = ZAAKTYPE_BPMN_TEST_5_UUID,
        zaakTypeDescription = ZAAKTYPE_BPMN_TEST_5_DESCRIPTION,
        bpmnProcessDefinitionKey = BPMN_PERMISSION_CHECK_PROCESS_DEFINITION_KEY,
        productaanvraagType = productaanvraagType,
        defaultGroupName = GROUP_BEHANDELAARS_TEST_1.name,
        defaultBehandelaarId = BEHANDELAAR_1.username,
        nietOntvankelijkResultaattype = ZAAKTYPE_BPMN_TEST_5_RESULTAATTYPE_AFGEBROKEN_UUID,
        testUser = BEHEERDER_1
    )

    given("a CMMN zaaktype whose current configuration uses a productaanvraagtype") {
        `when`("a beheerder stores a BPMN configuration of another zaaktype with that productaanvraagtype") {
            val response = storeBpmnTest5Configuration(productaanvraagType = PRODUCTAANVRAAG_TYPE_3)

            then("it is rejected as already in use, and the BPMN configuration keeps its own productaanvraagtype") {
                response.code shouldBe HTTP_BAD_REQUEST
                response.bodyAsString shouldContain "msg.error.productaanvraagtype.already.in.use"
                queryZacDatabase(
                    "SELECT productaanvraagtype FROM zaakafhandelcomponent.zaaktype_configuration " +
                        "WHERE zaaktype_uuid = '$ZAAKTYPE_BPMN_TEST_5_UUID'"
                ) shouldBe listOf(ZAAKTYPE_BPMN_TEST_5_PRODUCTAANVRAAG_TYPE)
            }
        }
    }

    given("a BPMN zaaktype whose current configuration uses a productaanvraagtype") {
        `when`("a beheerder stores the configuration of that zaaktype again with the same productaanvraagtype") {
            val response = storeBpmnTest5Configuration(productaanvraagType = ZAAKTYPE_BPMN_TEST_5_PRODUCTAANVRAAG_TYPE)

            then("it is accepted") {
                response.code shouldBe HTTP_OK
            }
        }
    }
})
