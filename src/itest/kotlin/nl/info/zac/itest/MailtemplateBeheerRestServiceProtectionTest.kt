/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_USER_MANAGEMENT_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_SIGNALERING_TAAK_OP_NAAM_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_ALGEMEEN_MAIL
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_RESULTAATTYPE_AFGEBROKEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK

@Isolate
class MailtemplateBeheerRestServiceProtectionTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    context("Protecting a system mail template") {
        given("the system mail template of the mail type 'Signalering taak op naam'") {
            val systemMailTemplate = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/beheer/mailtemplates",
                testUser = BEHEERDER_1
            ).let { response ->
                response.code shouldBe HTTP_OK
                JSONArray(response.bodyAsString).let { mailTemplates ->
                    (0 until mailTemplates.length()).map(mailTemplates::getJSONObject).single {
                        it.getString("mail") == MAIL_TEMPLATE_SIGNALERING_TAAK_OP_NAAM_MAIL &&
                            it.getBoolean("defaultMailtemplate")
                    }
                }
            }
            val systemMailTemplateId = systemMailTemplate.getLong("id")
            val systemMailTemplateUrl = "$ZAC_API_URI/beheer/mailtemplates/$systemMailTemplateId"

            fun systemMailTemplateRequest(
                mail: String = MAIL_TEMPLATE_SIGNALERING_TAAK_OP_NAAM_MAIL,
                isDefaultMailtemplate: Boolean = true,
                mailTemplateNaam: String = systemMailTemplate.getString("mailTemplateNaam"),
                onderwerp: String = systemMailTemplate.getString("onderwerp"),
                body: String = systemMailTemplate.getString("body")
            ) = JSONObject()
                .put("mailTemplateNaam", mailTemplateNaam)
                .put("onderwerp", onderwerp)
                .put("body", body)
                .put("mail", mail)
                .put("defaultMailtemplate", isDefaultMailtemplate)
                .toString()

            `when`("a beheerder deletes it through the API") {
                val response = itestHttpClient.performDeleteRequest(
                    url = systemMailTemplateUrl,
                    testUser = BEHEERDER_1
                )

                then("it is refused with the error code for a system mail template, and the template still exists") {
                    response.code shouldBe HTTP_BAD_REQUEST
                    JSONObject(response.bodyAsString).getString("message") shouldBe
                        "msg.error.system.mailtemplate.cannot.be.deleted"
                    itestHttpClient.performGetRequest(
                        url = systemMailTemplateUrl,
                        testUser = BEHEERDER_1
                    ).code shouldBe HTTP_OK
                }
            }

            mapOf(
                "the system flag off" to systemMailTemplateRequest(
                    isDefaultMailtemplate = false,
                    onderwerp = "fakeChangedOnderwerp"
                ),
                "another mail type" to systemMailTemplateRequest(
                    mail = MAIL_TEMPLATE_ZAAK_ALGEMEEN_MAIL,
                    onderwerp = "fakeChangedOnderwerp"
                )
            ).forEach { (change, requestBody) ->
                `when`("a beheerder stores it with $change and a changed subject") {
                    val response = itestHttpClient.performPutRequest(
                        url = systemMailTemplateUrl,
                        requestBodyAsString = requestBody,
                        testUser = BEHEERDER_1
                    )

                    then(
                        "it is refused with the error code for converting a system mail template, " +
                            "and the template is unchanged"
                    ) {
                        response.code shouldBe HTTP_BAD_REQUEST
                        JSONObject(response.bodyAsString).getString("message") shouldBe
                            "msg.error.system.mailtemplate.cannot.be.converted"
                        itestHttpClient.performGetRequest(
                            url = systemMailTemplateUrl,
                            testUser = BEHEERDER_1
                        ).let {
                            it.code shouldBe HTTP_OK
                            it.bodyAsString shouldEqualJsonIgnoringExtraneousFields systemMailTemplateRequest()
                        }
                    }
                }
            }

            `when`("a beheerder stores it with a changed name, subject and body") {
                val changedRequestBody = systemMailTemplateRequest(
                    mailTemplateNaam = "fakeChangedMailTemplateNaam",
                    onderwerp = "fakeChangedOnderwerp",
                    body = "<p>fakeChangedBody</p>"
                )
                val response = itestHttpClient.performPutRequest(
                    url = systemMailTemplateUrl,
                    requestBodyAsString = changedRequestBody,
                    testUser = BEHEERDER_1
                )
                val restoreResponse = itestHttpClient.performPutRequest(
                    url = systemMailTemplateUrl,
                    requestBodyAsString = systemMailTemplateRequest(),
                    testUser = BEHEERDER_1
                )

                then("the changes are stored, and the template is still the system mail template of its mail type") {
                    response.code shouldBe HTTP_OK
                    response.bodyAsString shouldEqualJsonIgnoringExtraneousFields changedRequestBody
                    restoreResponse.code shouldBe HTTP_OK
                }
            }
        }

        given("a beheerder who wants a mail template of their own to be a system mail template") {
            `when`("a beheerder creates a mail template with the system flag on") {
                val response = itestHttpClient.performJSONPostRequest(
                    url = "$ZAC_API_URI/beheer/mailtemplates",
                    requestBodyAsString = JSONObject()
                        .put("mailTemplateNaam", "fakeSystemMailTemplateNaam")
                        .put("onderwerp", "fakeOnderwerp")
                        .put("body", "<p>fakeBody</p>")
                        .put("mail", MAIL_TEMPLATE_ZAAK_ALGEMEEN_MAIL)
                        .put("defaultMailtemplate", true)
                        .toString(),
                    testUser = BEHEERDER_1
                )

                then("it is refused with the error code for converting a system mail template") {
                    response.code shouldBe HTTP_BAD_REQUEST
                    JSONObject(response.bodyAsString).getString("message") shouldBe
                        "msg.error.system.mailtemplate.cannot.be.converted"
                }
            }
        }
    }

    context("Deleting a mail template that a zaaktype configuration links") {
        given("a normal mail template that the configuration of a zaaktype links") {
            val zacClient = ZacClient(itestHttpClient)
            fun storeBpmnTest2Configuration(mailtemplateKoppelingenJson: String) =
                zacClient.createZaaktypeBpmnConfiguration(
                    zaakTypeUuid = ZAAKTYPE_BPMN_TEST_2_UUID,
                    zaakTypeDescription = ZAAKTYPE_BPMN_TEST_2_DESCRIPTION,
                    bpmnProcessDefinitionKey = BPMN_TEST_USER_MANAGEMENT_PROCESS_DEFINITION_KEY,
                    productaanvraagType = ZAAKTYPE_BPMN_TEST_2_PRODUCTAANVRAAG_TYPE,
                    defaultGroupName = GROUP_BEHANDELAARS_TEST_1.name,
                    defaultBehandelaarId = BEHANDELAAR_1.username,
                    nietOntvankelijkResultaattype = ZAAKTYPE_BPMN_TEST_2_RESULTAATTYPE_AFGEBROKEN_UUID,
                    mailtemplateKoppelingenJson = mailtemplateKoppelingenJson,
                    testUser = BEHEERDER_1
                )

            val linkedMailTemplate = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/beheer/mailtemplates",
                requestBodyAsString = JSONObject()
                    .put("mailTemplateNaam", "fakeLinkedMailTemplateNaam")
                    .put("onderwerp", "fakeOnderwerp")
                    .put("body", "<p>fakeBody</p>")
                    .put("mail", MAIL_TEMPLATE_ZAAK_ALGEMEEN_MAIL)
                    .put("defaultMailtemplate", false)
                    .toString(),
                testUser = BEHEERDER_1
            ).let {
                it.code shouldBe HTTP_CREATED
                JSONObject(it.bodyAsString)
            }
            val linkedMailTemplateId = linkedMailTemplate.getLong("id")
            val linkedMailTemplateUrl = "$ZAC_API_URI/beheer/mailtemplates/$linkedMailTemplateId"
            fun isLinked() = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/beheer/mailtemplatekoppeling",
                testUser = BEHEERDER_1
            ).let { response ->
                response.code shouldBe HTTP_OK
                JSONArray(response.bodyAsString).let { koppelingen ->
                    (0 until koppelingen.length()).map(koppelingen::getJSONObject).any {
                        it.getJSONObject("mailtemplate").getLong("id") == linkedMailTemplateId
                    }
                }
            }

            storeBpmnTest2Configuration(
                mailtemplateKoppelingenJson = JSONArray()
                    .put(JSONObject().put("mailtemplate", linkedMailTemplate))
                    .toString()
            ).code shouldBe HTTP_OK
            isLinked() shouldBe true

            `when`("a beheerder deletes the template through the API") {
                val response = itestHttpClient.performDeleteRequest(
                    url = linkedMailTemplateUrl,
                    testUser = BEHEERDER_1
                )

                then(
                    "it is refused with the error code for a linked mail template instead of the database error, " +
                        "and the template and the link still exist"
                ) {
                    response.code shouldBe HTTP_BAD_REQUEST
                    JSONObject(response.bodyAsString).getString("message") shouldBe
                        "msg.error.mailtemplate.is.in.use.by.zaaktype-configuration"
                    itestHttpClient.performGetRequest(
                        url = linkedMailTemplateUrl,
                        testUser = BEHEERDER_1
                    ).code shouldBe HTTP_OK
                    isLinked() shouldBe true
                }
            }

            `when`("the configuration is stored again with its original links, and the beheerder deletes the template") {
                storeBpmnTest2Configuration(mailtemplateKoppelingenJson = "[]").code shouldBe HTTP_OK
                val response = itestHttpClient.performDeleteRequest(
                    url = linkedMailTemplateUrl,
                    testUser = BEHEERDER_1
                )

                then("the template is deleted") {
                    response.code shouldBe HTTP_NO_CONTENT
                    itestHttpClient.performGetRequest(
                        url = linkedMailTemplateUrl,
                        testUser = BEHEERDER_1
                    ).code shouldBe HTTP_NOT_FOUND
                }
            }
        }
    }
})
