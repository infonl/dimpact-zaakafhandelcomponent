/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.KeycloakClient
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration
import okhttp3.Request
import org.json.JSONArray

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(1)
class PlanItemsRESTServiceTest : BehaviorSpec() {
    private val logger = KotlinLogging.logger {}
    private val zacClient: ZacClient = ZacClient()

    init {
        given("ZAC Docker container is running and a zaak has been created") {
            When("the list human task plan items endpoint is called") {
                then("the list of human task plan items for this zaak is returned") {
                    val endpointUrl = "${ItestConfiguration.ZAC_API_URI}/planitems/zaak/$zaak1UUID/humanTaskPlanItems"
                    logger.info { "Calling $endpointUrl endpoint" }

                    val request = Request.Builder()
                        .header("Authorization", "Bearer ${KeycloakClient.requestAccessToken()}")
                        .url(endpointUrl)
                        .get()
                        .build()

                    zacClient.okHttpClient.newCall(request).execute().use { response ->
                        val responseBody = response.body!!.string()
                        logger.info { "$endpointUrl response: $responseBody" }

                        response.isSuccessful shouldBe true
                        responseBody.shouldBeJsonArray()
                        // the zaak is in the intake phase, so there should be only be one human task plan item: 'aanvullende informatie'
                        JSONArray(responseBody).length() shouldBe 1

                        with(JSONArray(responseBody)[0].toString()) {
                            shouldContainJsonKeyValue("actief", "true")
                            shouldContainJsonKeyValue("formulierDefinitie", "AANVULLENDE_INFORMATIE")
                            shouldContainJsonKeyValue("naam", "Aanvullende informatie")
                            shouldContainJsonKeyValue("type", "HUMAN_TASK")
                            shouldContainJsonKeyValue("zaakUuid", zaak1UUID.toString())
                            shouldContainJsonKey("id")
                        }
                    }
                }
            }
        }
        //    given("ZAC Docker container is running and a zaak has been created") {
        //        When("the get human task plan item endpoint is called for the task 'aanvullende informatie'") {
        //            then("the human task plan item data is returned") {
        //                val endpointUrl = "${ItestConfiguration.ZAC_API_URI}/planitems/humanTaskPlanItem/262509"
        //                logger.info { "Calling $endpointUrl endpoint" }
        //
        //                val request = Request.Builder()
        //                    .header("Authorization", "Bearer ${KeycloakClient.requestAccessToken()}")
        //                    .url(endpointUrl)
        //                    .get()
        //                    .build()
        //
        //                zacClient.okHttpClient.newCall(request).execute().use { response ->
        //                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
        //
        //                    logger.info { "$endpointUrl response: ${(response.body!!.string())}" }
        //                }
        //
        //            }
        //        }
        //    }
        //    given("ZAC Docker container is running and a zaak has been created") {
        //        When("the human task plan items endpoint is called") {
        //            then("a task is started for this zaak") {
        //
        // planitems/doHumanTaskPlanItem
        // {"planItemInstanceId":"262509",
        // "fataledatum":"2024-03-11",
        // "taakStuurGegevens":{"sendMail":true,"mail":"TAAK_AANVULLENDE_INFORMATIE"},
        // "medewerker":null,"groep":{"id":"test-group-a","naam":"Test groep A"},
        // "taakdata":{"verzender":"gemeente-adorp-test@team-dimpact.info.nl",
        // "emailadres":"edgar@info.nl",
        // "body":"<p>Beste {ZAAK_INITIATOR},</p><p></p><p>Voor het behandelen van de zaak over
        // {ZAAK_OMSCHRIJVING} hebben wij de volgende informatie van u nodig:</p><ul><li><p>Omschrijf informatie 1</p></li>
        // <li><p>Omschrijf informatie 2</p></li></ul><p>We ontvangen de informatie graag uiterlijk op datum [vul datum in].
        // U kunt dit aanleveren door deze per e-mail te sturen naar [Vul email in]. Vermeld op de informatie ook het
        // zaaknummer van uw zaak,
        // dit is:{ZAAK_NUMMER}</p><p></p><p>Met vriendelijke groet,</p><p></p><p>Gemeente Dommeldam</p>",
        // "datumGevraagd":"2024-02-26T16:29:30+01:00",
        // "bijlagen":null}}

        //
        //            }
        //        }
        //    }
    }
}
