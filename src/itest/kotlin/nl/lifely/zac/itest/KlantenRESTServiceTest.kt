/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.ROLTYPE_COUNT
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BIRTHDATE
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BSN
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_EMAIL
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_FULLNAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_GENDER
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_PHONE_NUMBER
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_PLACE_OF_RESIDENCE
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONArray

/**
 * This test assumes a roltype has been created in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
class KlantenRESTServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val logger = KotlinLogging.logger {}

    Given("ZAC Docker container is running") {
        When("the list roltypen endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/klanten/roltype"
            )
            Then("the response should be a 200 HTTP response with the correct amount of roltypen") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                with(responseBody) {
                    shouldBeJsonArray()
                    JSONArray(responseBody).length() shouldBe ROLTYPE_COUNT
                    with(JSONArray(responseBody)[0].toString()) {
                        shouldContainJsonKeyValue("naam", "Behandelaar")
                        shouldContainJsonKeyValue("omschrijvingGeneriekEnum", "behandelaar")
                    }
                }
            }
        }
        When("a person is retrieved using a BSN which is present in both the BRP and Klanten API databases") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/klanten/persoon/$TEST_PERSON_HENDRIKA_JANSE_BSN"
            )
            Then(
                """
                    the response should be a 200 HTTP response with personal data from both the BRP and Klanten databases
                    """
            ) {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                with(responseBody) {
                    shouldContainJsonKeyValue("identificatie", TEST_PERSON_HENDRIKA_JANSE_BSN)
                    shouldContainJsonKeyValue("naam", TEST_PERSON_HENDRIKA_JANSE_FULLNAME)
                    shouldContainJsonKeyValue("emailadres", TEST_PERSON_HENDRIKA_JANSE_EMAIL)
                    shouldContainJsonKeyValue("telefoonnummer", TEST_PERSON_HENDRIKA_JANSE_PHONE_NUMBER)
                    shouldContainJsonKeyValue("geboortedatum", TEST_PERSON_HENDRIKA_JANSE_BIRTHDATE)
                    shouldContainJsonKeyValue("geslacht", TEST_PERSON_HENDRIKA_JANSE_GENDER)
                    shouldContainJsonKeyValue("verblijfplaats", TEST_PERSON_HENDRIKA_JANSE_PLACE_OF_RESIDENCE)
                }
            }
        }
    }
})
