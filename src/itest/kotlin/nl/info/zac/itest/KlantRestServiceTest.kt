/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.BETROKKENE_IDENTIFACTION_TYPE_VESTIGING
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_COUNT
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_ADRES_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_EERSTE_HANDELSNAAM_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_NAAM_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_NUMMER_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_PLAATS_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_RSIN_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGING1_HOOFDACTIVITEIT
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGING1_NEVENACTIVITEIT1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGING1_NEVENACTIVITEIT2
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGING1_TOTAAL_WERKZAME_PERSONEN
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGING1_VOLTIJD_WERKZAME_PERSONEN
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGINGSNUMMER_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_KVK_VESTIGINGSTYPE_HOOFDVESTIGING
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BIRTHDATE
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_BSN
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_EMAIL
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_FULLNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_GENDER
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_PHONE_NUMBER
import nl.info.zac.itest.config.ItestConfiguration.TEST_PERSON_HENDRIKA_JANSE_PLACE_OF_RESIDENCE
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.info.zac.itest.config.ItestConfiguration.VESTIGINGTYPE_NEVENVESTIGING
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_BELANGHEBBENDE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_BEWINDVOERDER
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_CONTACTPERSOON
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_GEMACHTIGDE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_MEDEAANVRAGER
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_PLAATSVERVANGER
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONArray
import org.json.JSONObject

/**
 * This test assumes a roltype has been created in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
class KlantRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val logger = KotlinLogging.logger {}

    Given("Klanten en bedrijven data is present in the BRP Personen Mock and the Klanten API database") {
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
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                responseBody shouldEqualJson """
                    {
                      "bsn": "$TEST_PERSON_HENDRIKA_JANSE_BSN",
                      "emailadres": "$TEST_PERSON_HENDRIKA_JANSE_EMAIL",
                      "geboortedatum": "$TEST_PERSON_HENDRIKA_JANSE_BIRTHDATE",
                      "geslacht": "$TEST_PERSON_HENDRIKA_JANSE_GENDER",
                      "identificatie": "$TEST_PERSON_HENDRIKA_JANSE_BSN",
                      "identificatieType": "BSN",
                      "indicaties": [ "OPSCHORTING_BIJHOUDING" ],
                      "naam": "$TEST_PERSON_HENDRIKA_JANSE_FULLNAME",
                      "telefoonnummer": "$TEST_PERSON_HENDRIKA_JANSE_PHONE_NUMBER",
                      "verblijfplaats": "$TEST_PERSON_HENDRIKA_JANSE_PLACE_OF_RESIDENCE"
                    }
                """.trimIndent()
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
                // since there is customer contact data linked to this vestiging in our Open Klant container
                // the response should contain an email address and telephone number
                responseBody shouldEqualJson """
                    {
                      "adres": "$TEST_KVK_ADRES_1, $TEST_KVK_PLAATS_1",
                      "emailadres": "$TEST_PERSON_HENDRIKA_JANSE_EMAIL",
                      "identificatie": "$TEST_KVK_VESTIGINGSNUMMER_1",
                      "identificatieType": "$BETROKKENE_IDENTIFACTION_TYPE_VESTIGING",
                      "kvkNummer": "$TEST_KVK_NUMMER_1",
                      "naam": "$TEST_KVK_NAAM_1",
                      "type": "$VESTIGINGTYPE_NEVENVESTIGING",
                      "telefoonnummer": "$TEST_PERSON_HENDRIKA_JANSE_PHONE_NUMBER",
                      "vestigingsnummer": "$TEST_KVK_VESTIGINGSNUMMER_1"
                    }
                """.trimIndent()
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
        When("a search on companies by name is performed") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/klanten/bedrijven",
                requestBodyAsString = JSONObject(
                    mapOf("naam" to TEST_KVK_NAAM_1)
                ).toString()
            )
            Then("the expected companies as defined in the KVK mock are returned") {
                response.code shouldBe HTTP_STATUS_OK
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                with(responseBody) {
                    shouldContainJsonKeyValue("totaal", 1)
                    shouldContainJsonKey("resultaten")
                    val resultaten = JSONObject(responseBody).getJSONArray("resultaten")
                    resultaten.length() shouldBe 1
                    with(JSONArray(resultaten).get(0).toString()) {
                        shouldContainJsonKeyValue("adres", "$TEST_KVK_ADRES_1, $TEST_KVK_PLAATS_1")
                        shouldContainJsonKeyValue("identificatie", TEST_KVK_VESTIGINGSNUMMER_1)
                        shouldContainJsonKeyValue("identificatieType", BETROKKENE_IDENTIFACTION_TYPE_VESTIGING)
                        shouldContainJsonKeyValue("kvkNummer", TEST_KVK_NUMMER_1)
                        shouldContainJsonKeyValue("naam", TEST_KVK_NAAM_1)
                        shouldContainJsonKeyValue("type", VESTIGINGTYPE_NEVENVESTIGING)
                        shouldContainJsonKeyValue("vestigingsnummer", TEST_KVK_VESTIGINGSNUMMER_1)
                    }
                }
            }
        }
        When("the list contactmomenten endpoint is called with the BSN of this test customer") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/klanten/contactmomenten",
                requestBodyAsString = """
                    {
                        "bsn": "$TEST_PERSON_HENDRIKA_JANSE_BSN",
                        "page": 0
                    }
                """.trimIndent()
            )
            Then("the response should be a 200 HTTP response with the customer contactmomenten") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                responseBody shouldEqualJson """
                    {
                      "foutmelding": "",
                      "resultaten": [
                        {
                          "initiatiefnemer": "First of Last",
                          "kanaal": "email",
                          "medewerker": "Actor Name",
                          "registratiedatum": "2000-01-01T12:00:00Z",
                          "tekst": "email contact"
                        },
                        {
                          "initiatiefnemer": "Name in Family",
                          "kanaal": "telefoon",
                          "registratiedatum": "2010-01-01T12:00:00Z",
                          "tekst": "phone contact"
                        }
                      ],
                      "totaal": 2
                    }
                """.trimIndent()
            }
        }
        When(
            """
                the read rechtspersoon endpoint is called with the RSIN of a test company available in the KVK mock
                """
        ) {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/klanten/rechtspersoon/$TEST_KVK_RSIN_1",
            )
            Then("the response should be ok and the test company should be returned without contact data") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                responseBody shouldEqualJson """
                    {
                      "adres": "$TEST_KVK_ADRES_1, $TEST_KVK_PLAATS_1",
                      "identificatie": "$TEST_KVK_VESTIGINGSNUMMER_1",
                      "identificatieType": "$BETROKKENE_IDENTIFACTION_TYPE_VESTIGING",
                      "kvkNummer": "$TEST_KVK_NUMMER_1",
                      "naam": "$TEST_KVK_NAAM_1",
                      "type": "$VESTIGINGTYPE_NEVENVESTIGING",
                      "vestigingsnummer": "$TEST_KVK_VESTIGINGSNUMMER_1"
                    }
                """.trimIndent()
            }
        }
        When(
            """
                the personen parameters endpoint is called
                """
        ) {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/klanten/personen/parameters",
            )
            Then("the response should be ok and the test company should be returned without contact data") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                responseBody shouldEqualJson """
                  [ 
                    {
                      "bsn" : "REQ",
                      "geboortedatum" : "NON",
                      "gemeenteVanInschrijving" : "NON",
                      "geslachtsnaam" : "NON",
                      "huisnummer" : "NON",
                      "postcode" : "NON",
                      "straat" : "NON",
                      "voornamen" : "NON",
                      "voorvoegsel" : "NON"
                    }, 
                    {
                      "bsn" : "NON",
                      "geboortedatum" : "REQ",
                      "gemeenteVanInschrijving" : "NON",
                      "geslachtsnaam" : "REQ",
                      "huisnummer" : "NON",
                      "postcode" : "NON",
                      "straat" : "NON",
                      "voornamen" : "OPT",
                      "voorvoegsel" : "OPT"
                    }, 
                    {
                      "bsn" : "NON",
                      "geboortedatum" : "NON",
                      "gemeenteVanInschrijving" : "REQ",
                      "geslachtsnaam" : "REQ",
                      "huisnummer" : "NON",
                      "postcode" : "NON",
                      "straat" : "NON",
                      "voornamen" : "REQ",
                      "voorvoegsel" : "OPT"
                    }, 
                    {
                      "bsn" : "NON",
                      "geboortedatum" : "NON",
                      "gemeenteVanInschrijving" : "NON",
                      "geslachtsnaam" : "NON",
                      "huisnummer" : "REQ",
                      "postcode" : "REQ",
                      "straat" : "NON",
                      "voornamen" : "NON",
                      "voorvoegsel" : "NON"
                    }, 
                    {
                      "bsn" : "NON",
                      "geboortedatum" : "NON",
                      "gemeenteVanInschrijving" : "REQ",
                      "geslachtsnaam" : "NON",
                      "huisnummer" : "REQ",
                      "postcode" : "NON",
                      "straat" : "REQ",
                      "voornamen" : "NON",
                      "voorvoegsel" : "NON"
                    } 
                  ]
                """.trimIndent()
            }
        }
        When(
            """
                the betrokkenen are retrieved for the zaaktype 'indienen aansprakelijkstelling door derden behandelen'
                """
        ) {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/klanten/roltype/$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID/betrokkene",
            )
            Then("the response should be ok and the test company should be returned without contact data") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                responseBody shouldEqualJson
                    """
                    [
                      {
                        "naam": "Belanghebbende",
                        "omschrijvingGeneriekEnum": "belanghebbende",
                        "uuid": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_BELANGHEBBENDE"
                      },
                      {
                        "naam": "Bewindvoerder",
                        "omschrijvingGeneriekEnum": "belanghebbende",
                        "uuid": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_BEWINDVOERDER"
                      },
                      {
                        "naam": "Contactpersoon",
                        "omschrijvingGeneriekEnum": "belanghebbende",
                        "uuid": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_CONTACTPERSOON"
                      },
                      {
                        "naam": "Gemachtigde",
                        "omschrijvingGeneriekEnum": "belanghebbende",
                        "uuid": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_GEMACHTIGDE"
                      },
                      {
                        "naam": "Medeaanvrager",
                        "omschrijvingGeneriekEnum": "mede_initiator",
                        "uuid": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_MEDEAANVRAGER"
                      },
                      {
                        "naam": "Plaatsvervanger",
                        "omschrijvingGeneriekEnum": "belanghebbende",
                        "uuid": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_BETROKKENE_PLAATSVERVANGER"
                      }
                    ]
                    """.trimIndent()
            }
        }
    }
})
