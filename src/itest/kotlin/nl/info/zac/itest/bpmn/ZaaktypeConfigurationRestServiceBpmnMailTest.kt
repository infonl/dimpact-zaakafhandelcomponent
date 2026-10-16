/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.createZaakAndRetrieve
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_USER_MANAGEMENT_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_ALGEMEEN_MAIL
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_RESULTAATTYPE_AFGEBROKEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_OK

private const val BPMN_MAILTEMPLATE_NAME = "fakeBpmnZaakAlgemeenMailtemplate"
private const val BPMN_ZAAKAFZENDER = "fakeBpmnAfzender@example.com"

class ZaaktypeConfigurationRestServiceBpmnMailTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)

    fun storeBpmnTest2Configuration(mailtemplateKoppelingenJson: String, zaakAfzendersJson: String) =
        zacClient.createZaaktypeBpmnConfiguration(
            zaakTypeUuid = ZAAKTYPE_BPMN_TEST_2_UUID,
            zaakTypeDescription = ZAAKTYPE_BPMN_TEST_2_DESCRIPTION,
            bpmnProcessDefinitionKey = BPMN_TEST_USER_MANAGEMENT_PROCESS_DEFINITION_KEY,
            productaanvraagType = ZAAKTYPE_BPMN_TEST_2_PRODUCTAANVRAAG_TYPE,
            defaultGroupName = GROUP_BEHANDELAARS_TEST_1.name,
            defaultBehandelaarId = BEHANDELAAR_1.username,
            nietOntvankelijkResultaattype = ZAAKTYPE_BPMN_TEST_2_RESULTAATTYPE_AFGEBROKEN_UUID,
            mailtemplateKoppelingenJson = mailtemplateKoppelingenJson,
            zaakAfzendersJson = zaakAfzendersJson,
            testUser = BEHEERDER_1
        )

    val mailtemplate = itestHttpClient.performJSONPostRequest(
        url = "$ZAC_API_URI/beheer/mailtemplates",
        requestBodyAsString = JSONObject(
            mapOf(
                "mailTemplateNaam" to BPMN_MAILTEMPLATE_NAME,
                "onderwerp" to "fakeOnderwerp",
                "body" to "<p>fakeBody</p>",
                "mail" to MAIL_TEMPLATE_ZAAK_ALGEMEEN_MAIL,
                "defaultMailtemplate" to false
            )
        ).toString(),
        testUser = BEHEERDER_1
    ).let {
        it.code shouldBe HTTP_CREATED
        JSONObject(it.bodyAsString)
    }

    afterSpec {
        storeBpmnTest2Configuration(mailtemplateKoppelingenJson = "[]", zaakAfzendersJson = "[]").code shouldBe HTTP_OK
        itestHttpClient.performDeleteRequest(
            url = "$ZAC_API_URI/beheer/mailtemplates/${mailtemplate.getLong("id")}",
            testUser = BEHEERDER_1
        )
    }

    given(
        """
        a BPMN zaaktype configuration that links the ZAAK_ALGEMEEN mail to a mail template of its own,
        and that has a default zaakafzender
        """
    ) {
        val storeResponse = storeBpmnTest2Configuration(
            mailtemplateKoppelingenJson = JSONArray().put(JSONObject().put("mailtemplate", mailtemplate)).toString(),
            zaakAfzendersJson = JSONArray()
                .put(
                    JSONObject()
                        .put("mail", BPMN_ZAAKAFZENDER)
                        .put("isSpeciaal", false)
                        .put("isDefaultMail", true)
                ).toString()
        )
        logger.info { "Response: ${storeResponse.bodyAsString}" }
        storeResponse.code shouldBe HTTP_OK
        val zaakUuid = zacClient.createZaakAndRetrieve(
            zaakTypeUUID = ZAAKTYPE_BPMN_TEST_2_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_1
        ).let { JSONObject(it.bodyAsString).getJSONObject("zaakdata").getString("zaakUUID") }

        `when`("a behandelaar asks for the ZAAK_ALGEMEEN mail template of a zaak of that zaaktype") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/mailtemplates/$MAIL_TEMPLATE_ZAAK_ALGEMEEN_MAIL/$zaakUuid",
                testUser = BEHANDELAAR_1
            )

            then("the mail template of the koppeling is proposed and not the default one") {
                response.code shouldBe HTTP_OK
                JSONObject(response.bodyAsString).getString("mailTemplateNaam") shouldBe BPMN_MAILTEMPLATE_NAME
            }
        }

        `when`("a behandelaar asks for the default afzender of a zaak of that zaaktype") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/afzender/default",
                testUser = BEHANDELAAR_1
            )

            then("it is the default zaakafzender of the configuration") {
                response.code shouldBe HTTP_OK
                JSONObject(response.bodyAsString).getString("mail") shouldBe BPMN_ZAAKAFZENDER
            }
        }
    }
})
