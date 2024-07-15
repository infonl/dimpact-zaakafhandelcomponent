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
import io.kotest.matchers.string.shouldContain
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.BETROKKENE_IDENTIFACTION_TYPE_VESTIGING
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.ROLTYPE_COUNT
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_KVK_ADRES_1
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_KVK_EERSTE_HANDELSNAAM_1
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_KVK_NAAM_1
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_KVK_NUMMER_1
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_KVK_PLAATS_1
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGING1_HOOFDACTIVITEIT
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGING1_NEVENACTIVITEIT1
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGING1_NEVENACTIVITEIT2
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGING1_TOTAAL_WERKZAME_PERSONEN
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGING1_VOLTIJD_WERKZAME_PERSONEN
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGINGSNUMMER_1
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGINGSTYPE_HOOFDVESTIGING
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BIRTHDATE
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BSN
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_EMAIL
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_FULLNAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_GENDER
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_PHONE_NUMBER
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_PLACE_OF_RESIDENCE
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.lifely.zac.itest.config.ItestConfiguration.VESTIGINGTYPE_NEVENVESTIGING
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONArray
import org.json.JSONObject

/**
 * This test assumes a roltype has been created in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
class KlantRestServiceTest : BehaviorSpec({
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
        When("a vestiging is requested which is present in the KVK test environment") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/klanten/vestiging/$TEST_KVK_VESTIGINGSNUMMER_1"
            )
            Then("the vestiging is returned with the expected data") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                with(responseBody) {
                    shouldContainJsonKeyValue("adres", "$TEST_KVK_ADRES_1, $TEST_KVK_PLAATS_1")
                    shouldContainJsonKeyValue("identificatie", TEST_KVK_VESTIGINGSNUMMER_1)
                    shouldContainJsonKeyValue("identificatieType", BETROKKENE_IDENTIFACTION_TYPE_VESTIGING)
                    shouldContainJsonKeyValue("kvkNummer", TEST_KVK_NUMMER_1)
                    shouldContainJsonKeyValue("naam", TEST_KVK_NAAM_1)
                    shouldContainJsonKeyValue("type", VESTIGINGTYPE_NEVENVESTIGING)
                    shouldContainJsonKeyValue("vestigingsnummer", TEST_KVK_VESTIGINGSNUMMER_1)
                    // since there is customer contact data linked to this vestiging in our Open Klant container
                    // the response should contain an email address and telephone number
                    shouldContainJsonKeyValue("emailadres", TEST_PERSON_HENDRIKA_JANSE_EMAIL)
                    shouldContainJsonKeyValue("telefoonnummer", TEST_PERSON_HENDRIKA_JANSE_PHONE_NUMBER)
                }
            }
        }
        When("a vestigingsprofiel is requested which is present in the KVK test environment") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/klanten/vestigingsprofiel/$TEST_KVK_VESTIGINGSNUMMER_1"
            )
            Then("the vestigingsprofiel is returned with the expected data") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                with(responseBody) {
                    shouldContainJsonKey("adressen")
                    val adressen = JSONObject(responseBody).getJSONArray("adressen")
                    adressen.length() shouldBe 1
                    with(JSONArray(adressen).get(0).toString()) {
                        shouldContainJsonKeyValue("type", "bezoekadres")
                        shouldContainJsonKeyValue("afgeschermd", false)
                        shouldContainJsonKeyValue("volledigAdres", "$TEST_KVK_ADRES_1, $TEST_KVK_PLAATS_1")
                    }
                    shouldContainJsonKeyValue("commercieleVestiging", true)
                    shouldContainJsonKeyValue("deeltijdWerkzamePersonen", 1)
                    shouldContainJsonKeyValue("eersteHandelsnaam", TEST_KVK_EERSTE_HANDELSNAAM_1)
                    shouldContainJsonKeyValue("kvkNummer", TEST_KVK_NUMMER_1)
                    shouldContainJsonKey("sbiActiviteiten")
                    val sbiActiviteiten = JSONObject(responseBody).getJSONArray("sbiActiviteiten")
                    sbiActiviteiten.length() shouldBe 2
                    with(JSONArray(sbiActiviteiten).get(0).toString()) {
                        shouldContain(TEST_KVK_VESTIGING1_NEVENACTIVITEIT1)
                    }
                    with(JSONArray(sbiActiviteiten).get(1).toString()) {
                        shouldContain(TEST_KVK_VESTIGING1_NEVENACTIVITEIT2)
                    }
                    shouldContainJsonKeyValue("sbiHoofdActiviteit", TEST_KVK_VESTIGING1_HOOFDACTIVITEIT)
                    shouldContainJsonKeyValue("totaalWerkzamePersonen", TEST_KVK_VESTIGING1_TOTAAL_WERKZAME_PERSONEN)
                    shouldContainJsonKeyValue("type", TEST_KVK_VESTIGINGSTYPE_HOOFDVESTIGING)
                    shouldContainJsonKeyValue("vestigingsnummer", TEST_KVK_VESTIGINGSNUMMER_1)
                    shouldContainJsonKeyValue("voltijdWerkzamePersonen", TEST_KVK_VESTIGING1_VOLTIJD_WERKZAME_PERSONEN)
                }
            }
        }
    }
})
