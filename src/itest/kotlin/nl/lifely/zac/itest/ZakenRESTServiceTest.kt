/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration.GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.GROUP_A_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_2_IDENTIFICATION
import org.apache.http.HttpStatus
import org.json.JSONObject

private val zacClient = ZacClient()

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(1)
class ZakenRESTServiceTest : BehaviorSpec({
    given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the create zaak endpoint is called and the user has permissions for the zaaktype used") {
            then("the response should be a 200 HTTP response with the created zaak") {
                val response = zacClient.createZaak(ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID, GROUP_A_ID, GROUP_A_NAME)
                response.statusCode shouldBe HttpStatus.SC_OK
                JSONObject(response.text).apply {
                    getJSONObject("zaaktype").getString("identificatie") shouldBe ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE

                    getJSONObject("zaakdata").apply {
                        getString("zaakUUID") shouldNotBe null
                        getString("zaakIdentificatie") shouldBe ZAAK_2_IDENTIFICATION
                    }
                }
            }
        }
    }

    // test for Intake afronden
    // POST on planitems/doUserEventListenerPlanItem met payload:
    // {"zaakUuid":"626a8987-14be-4b1f-b63e-17375b382eb3","planItemInstanceId":"2546",
    // "actie":"INTAKE_AFRONDEN","zaakOntvankelijk":true,"resultaatToelichting":""}

    //  test for Besluit toevoegen
    // POST on zaken/besluit met payload:
    // {"zaakUuid":"626a8987-14be-4b1f-b63e-17375b382eb3","resultaattypeUuid":"b0ed0590-a1fe-4448-9f9a-9e8e848be727",
    // "besluittypeUuid":"1a282535-09cc-480c-a5cf-cef0a76a1c5b","toelichting":"Dummy toelichting",
    // "ingangsdatum":"2024-01-30T14:54:53+01:00","vervaldatum":null,"informatieobjecten":[]}
})
