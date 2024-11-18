/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI

class HealthCheckRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("Default communicatiekanalen referentietabel data is provisioned on startup") {
        When("the check on the existence of the e-formulier communicatiekanaal is performed") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/health-check/bestaat-communicatiekanaal-eformulier"
            )
            val responseBody = response.body!!.string()
            logger.info { "Response: $responseBody" }

            Then("the response should be a 200 OK with a response body 'true'") {
                response.isSuccessful shouldBe true
                responseBody shouldBe "true"
            }
        }
    }

    Given("Zaak types are configured correctly") {
        When("the check for zaak types validity is performed") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/health-check/zaaktypes"
            )
            val responseBody = response.body!!.string()
            logger.info { "Response: $responseBody" }

            Then("the response should be a 200 OK") {
                response.isSuccessful shouldBe true
            }

            And("the body contains all the performed checks") {
                responseBody shouldEqualJson """
                    [
                      {
                        "besluittypeAanwezig": true,
                        "informatieobjecttypeEmailAanwezig": true,
                        "resultaattypeAanwezig": true,
                        "resultaattypesMetVerplichtBesluit": [],
                        "rolBehandelaarAanwezig": true,
                        "rolInitiatorAanwezig": true,
                        "rolOverigeAanwezig": true,
                        "statustypeAanvullendeInformatieVereist": true,
                        "statustypeAfgerondAanwezig": true,
                        "statustypeAfgerondLaatsteVolgnummer": true,
                        "statustypeHeropendAanwezig": true,
                        "statustypeInBehandelingAanwezig": true,
                        "statustypeIntakeAanwezig": true,
                        "valide": true,
                        "zaakafhandelParametersValide": true,
                        "zaaktype": {
                          "beginGeldigheid": "2023-10-01",
                          "doel": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                          "identificatie": "indienen-aansprakelijkstelling-behandelen",
                          "nuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                          "servicenorm": false,
                          "uuid": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID",
                          "versiedatum": "2023-10-01",
                          "vertrouwelijkheidaanduiding": "openbaar"
                        }
                      },
                      {
                        "besluittypeAanwezig": false,
                        "informatieobjecttypeEmailAanwezig": true,
                        "resultaattypeAanwezig": true,
                        "resultaattypesMetVerplichtBesluit": [],
                        "rolBehandelaarAanwezig": true,
                        "rolInitiatorAanwezig": true,
                        "rolOverigeAanwezig": true,
                        "statustypeAanvullendeInformatieVereist": true,
                        "statustypeAfgerondAanwezig": true,
                        "statustypeAfgerondLaatsteVolgnummer": true,
                        "statustypeHeropendAanwezig": true,
                        "statustypeInBehandelingAanwezig": true,
                        "statustypeIntakeAanwezig": true,
                        "valide": true,
                        "zaakafhandelParametersValide": true,
                        "zaaktype": {
                          "beginGeldigheid": "2023-09-21",
                          "doel": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION",
                          "identificatie": "melding-evenement-organiseren-behandelen",
                          "nuGeldig": true,
                          "omschrijving": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION",
                          "servicenorm": false,
                          "uuid": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID",
                          "versiedatum": "2023-09-21",
                          "vertrouwelijkheidaanduiding": "openbaar"
                        }
                      }
                    ]
                """.trimIndent()
            }
        }
    }
})
