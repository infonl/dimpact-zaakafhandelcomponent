/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.ItestConfiguration.DOMEIN_TEST_2
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_3
import nl.info.zac.itest.config.ItestConfiguration.TEST_BEHANDELAAR_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.TEST_BEHANDELAAR_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_FUNCTIONAL_ADMIN_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.TEST_FUNCTIONAL_ADMIN_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BEHANDELEN_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_1_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields
import java.net.HttpURLConnection.HTTP_OK

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
@Suppress("LargeClass")
@Tags("pabc")
class ZaakRestServicePabcTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    afterSpec {
        // re-authenticate using testuser1 since currently subsequent integration tests rely on this user being logged in
        authenticate(username = TEST_USER_1_USERNAME, password = TEST_USER_1_PASSWORD)
    }

    Given("ZAC Docker container is running and a functioneelbeheerder1 is logged-in") {
        authenticate(username = TEST_FUNCTIONAL_ADMIN_1_USERNAME, password = TEST_FUNCTIONAL_ADMIN_1_PASSWORD)

        When("zaaktype_test_1 is created") {
            val response = zacClient.createZaakAfhandelParameters(
                zaakTypeIdentificatie = ZAAKTYPE_TEST_1_IDENTIFICATIE,
                zaakTypeUuid = ZAAKTYPE_TEST_1_UUID,
                zaakTypeDescription = ZAAKTYPE_TEST_1_DESCRIPTION,
                productaanvraagType = PRODUCTAANVRAAG_TYPE_3,
                domein = DOMEIN_TEST_2
            )
            Then("the response should be ok") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
            }
        }
    }

    Given(
        """
        ZAC Docker container is running, zaakafhandleparameters is created and a testuser1 is logged-in
        """.trimIndent()
    ) {
        authenticate(username = TEST_USER_1_USERNAME, password = TEST_USER_1_PASSWORD)

        When("zaak types are listed") {
            val response = itestHttpClient.performGetRequest("$ZAC_API_URI/zaken/zaaktypes-for-creation")
            lateinit var responseBody: String

            Then("the response should be a 200 HTTP response") {
                responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
            }

            And("the response body should contain the zaaktypes in all domains") {
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                [
                  {
                    "doel": "$ZAAKTYPE_TEST_1_DESCRIPTION",
                    "identificatie": "$ZAAKTYPE_TEST_1_IDENTIFICATIE",
                    "omschrijving": "$ZAAKTYPE_TEST_1_DESCRIPTION"
                  },
                  {
                    "doel": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                    "identificatie": "$ZAAKTYPE_BPMN_TEST_IDENTIFICATIE",
                    "omschrijving": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION"
                  },
                  {
                    "doel": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                    "identificatie": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BEHANDELEN_IDENTIFICATIE",
                    "omschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION"
                  },
                  {
                    "doel": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION",
                    "identificatie": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE",
                    "omschrijving": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION"
                  }
                ]
                """.trimIndent()
            }
        }
    }

    Given(
        """
        ZAC Docker container is running and zaakafhandelparameters have been created and a behandelaar is logged-in
        """.trimIndent()
    ) {
        authenticate(username = TEST_BEHANDELAAR_1_USERNAME, password = TEST_BEHANDELAAR_1_PASSWORD)
        lateinit var responseBody: String

        When("zaak types are listed") {
            val response = itestHttpClient.performGetRequest("$ZAC_API_URI/zaken/zaaktypes-for-creation")
            Then("the response should be a 200 HTTP response") {
                responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
            }
            And("the response body should contain only the zaaktypes for which the user is authorized") {
                responseBody shouldEqualJsonIgnoringOrderAndExtraneousFields """
                [
                  {
                    "doel": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                    "identificatie": "$ZAAKTYPE_BPMN_TEST_IDENTIFICATIE",
                    "omschrijving": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION"
                  },
                  {
                    "doel": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                    "identificatie": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BEHANDELEN_IDENTIFICATIE",
                    "omschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION"
                  },
                  {
                    "doel": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION",
                    "identificatie": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE",
                    "omschrijving": "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION"
                  }
                ]
                """.trimIndent()
            }
        }
    }
})
