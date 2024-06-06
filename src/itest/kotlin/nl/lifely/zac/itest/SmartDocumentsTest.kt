/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.compareJsonOptions
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI

class SmartDocumentsTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    val myOptions = compareJsonOptions {
        arrayOrder = ArrayOrder.Lenient
    }

    infix fun String.lenientShouldEqualJson(other: String) = this.shouldEqualJson(other, myOptions)

    Given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the list SmartDocuments templates endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/smartdocuments/templates"
            )

            Then("the response should be ok") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                with(responseBody) {
                    shouldContainJsonKeyValue("$[0].id", "D5037631FF6748269059B353699EFA0C")
                    shouldContainJsonKeyValue("$[0].name", "Dimpact")

                    shouldContainJsonKeyValue("$[0].groups[0].id", "0E18B04EDF9646C0A2D04E651DC4C6FF")
                    shouldContainJsonKeyValue("$[0].groups[0].name", "Intern zaaktype voor test volledig gebruik ZAC")
                    shouldContainJsonKeyValue("$[0].groups[0].templates[0].id", "7B7857BB9959470C82974037304E433D")
                    shouldContainJsonKeyValue("$[0].groups[0].templates[0].name", "Data Test")
                    shouldContainJsonKeyValue("$[0].groups[0].templates[1].id", "273C2707E5A844699B653C87ACFD618E")
                    shouldContainJsonKeyValue("$[0].groups[0].templates[1].name", "OpenZaakTest")

                    shouldContainJsonKeyValue("$[0].groups[1].id", "348097107FA346DC9AEBBE33A5500B79")
                    shouldContainJsonKeyValue("$[0].groups[1].name", "OpenZaak")
                    shouldContainJsonKeyValue("$[0].groups[1].templates[0].id", "7B7857BB9959470C82974037304E433D")
                    shouldContainJsonKeyValue("$[0].groups[1].templates[0].name", "Data Test")
                    shouldContainJsonKeyValue("$[0].groups[1].templates[1].id", "273C2707E5A844699B653C87ACFD618E")
                    shouldContainJsonKeyValue("$[0].groups[1].templates[1].name", "OpenZaakTest")

                    shouldContainJsonKeyValue("$[0].groups[2].id", "CC2696396E1D4899A74AC025B19C8FDC")
                    shouldContainJsonKeyValue(
                        "$[0].groups[2].name",
                        "Indienen aansprakelijkstelling door derden behandelen"
                    )
                    shouldContainJsonKeyValue("$[0].groups[2].templates[0].id", "7B7857BB9959470C82974037304E433D")
                    shouldContainJsonKeyValue("$[0].groups[2].templates[0].name", "Data Test")
                    shouldContainJsonKeyValue("$[0].groups[2].templates[1].id", "273C2707E5A844699B653C87ACFD618E")
                    shouldContainJsonKeyValue("$[0].groups[2].templates[1].name", "OpenZaakTest")

                    shouldContainJsonKeyValue("$[0].templates[0].id", "2F426D9B06034FDB93FDC2C6427640DD")
                    shouldContainJsonKeyValue("$[0].templates[0].name", "Aanvullende informatie nieuw")
                    shouldContainJsonKeyValue("$[0].templates[1].id", "CDC0FD0F08B545B59E36D610AFFA0B58")
                    shouldContainJsonKeyValue("$[0].templates[1].name", "Aanvullende informatie oud")
                    shouldContainJsonKeyValue("$[0].templates[2].id", "40A8482A34F941618B95359BF8246FB8")
                    shouldContainJsonKeyValue("$[0].templates[2].name", "Advies nieuw")
                    shouldContainJsonKeyValue("$[0].templates[3].id", "16B8B5A8E18A4BA484302029B51C32B6")
                    shouldContainJsonKeyValue("$[0].templates[3].name", "Advies oud")
                    shouldContainJsonKeyValue("$[0].templates[4].id", "53AFB00EE74E4B8AB1EC73CCB3251F8B")
                    shouldContainJsonKeyValue("$[0].templates[4].name", "Besluit nieuw")
                    shouldContainJsonKeyValue("$[0].templates[5].id", "60D711D768184F7F94A8BC1553A689A1")
                    shouldContainJsonKeyValue("$[0].templates[5].name", "Besluit oud")
                    shouldContainJsonKeyValue("$[0].templates[6].id", "7B7857BB9959470C82974037304E433D")
                    shouldContainJsonKeyValue("$[0].templates[6].name", "Data Test")
                    shouldContainJsonKeyValue("$[0].templates[7].id", "6BF6F7B751554C24964C7B39DC62F2BA")
                    shouldContainJsonKeyValue("$[0].templates[7].name", "Memo nieuw")
                    shouldContainJsonKeyValue("$[0].templates[8].id", "4A615E0E9F754539A7BB969B55CC814A")
                    shouldContainJsonKeyValue("$[0].templates[8].name", "Memo oud")
                    shouldContainJsonKeyValue("$[0].templates[9].id", "C775E87B0DA04FFFA3355320C2E905A9")
                    shouldContainJsonKeyValue("$[0].templates[9].name", "Ontvangstbevestiging nieuw")
                    shouldContainJsonKeyValue("$[0].templates[10].id", "237A278B4CC9455EA9D7AF9BC41610FC")
                    shouldContainJsonKeyValue("$[0].templates[10].name", "Ontvangstbevestiging oud")
                    shouldContainJsonKeyValue("$[0].templates[10].id", "237A278B4CC9455EA9D7AF9BC41610FC")
                    shouldContainJsonKeyValue("$[0].templates[10].name", "Ontvangstbevestiging oud")
                    shouldContainJsonKeyValue("$[0].templates[11].id", "B1052A0E3A7A46FDA694A239B93E68AD")
                    shouldContainJsonKeyValue("$[0].templates[11].name", "Samenwerking loont")
                    shouldContainJsonKeyValue("$[0].templates[12].id", "719AB39150074AF88E02CFE55EBAE479")
                    shouldContainJsonKeyValue("$[0].templates[12].name", "Signature Template")
                    shouldContainJsonKeyValue("$[0].templates[13].id", "445E1A2C5D964A33961CA46679AB51CF")
                    shouldContainJsonKeyValue("$[0].templates[13].name", "Toets nieuw")
                    shouldContainJsonKeyValue("$[0].templates[14].id", "8CCCF38A7757473CB5F5F2B8E5D7484D")
                    shouldContainJsonKeyValue("$[0].templates[14].name", "Toets oud")
                    shouldContainJsonKeyValue("$[0].templates[15].id", "E919C3C9444F4EA7B4A53C896FBC8ABC")
                    shouldContainJsonKeyValue("$[0].templates[15].name", "Zaakafhandelcomponent Test")
                }
            }
        }

        When("the create mapping endpoint is called") {
            val smartDocumentsZaakafhandelParametersUrl =
                "$ZAC_API_URI/smartdocuments/templates/zaakafhandelParamaters/$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID"
            val restTemplateGroups = """
                [
                  {
                    "id": "6e0658a4-3039-4a7c-84d2-4ef47285ea52",
                    "name": "root",
                    "groups": [
                      {
                        "id": "ef1abdd4-2182-4600-85ab-b9e7bab4e96a",
                        "name": "group 1",
                        "templates": [
                          {
                            "id": "4496b307-2980-4fe3-ac7f-53219683770b",
                            "name": "group 1 template 1"
                          },
                          {
                            "id": "606db3b5-bff1-4664-bfab-d7c4f9647f19",
                            "name": "group 1 template 2"
                          }
                        ]
                      },
                      {
                        "id": "949e5a09-0361-43cb-a82f-93263b7fc4b4",
                        "name": "group 2",
                        "templates": [
                          {
                            "id": "e147850c-4492-446d-a37e-0f593c6061fd",
                            "name": "group 2 template 1"
                          },
                          {
                            "id": "d5da1fd2-dd72-4c3c-af43-3555dacaf59a",
                            "name": "group 2 template 2"
                          }
                        ]
                      }
                    ],
                    "templates": [
                      {
                        "id": "93556271-a1ec-43a2-95f5-a5570ce927a4",
                        "name": "root template 1"
                      },
                      {
                        "id": "2fe1ab77-57c8-4e78-88ed-e25358ceff89",
                        "name": "root template 2"
                      }
                    ]
                  }
                ]
            """.trimIndent()
            val storeResponse = itestHttpClient.performJSONPostRequest(
                url = smartDocumentsZaakafhandelParametersUrl,
                requestBodyAsString = restTemplateGroups
            )
            storeResponse.isSuccessful shouldBe true

            And("then the mapping is fetched back") {
                val fetchResponse = itestHttpClient.performGetRequest(url = smartDocumentsZaakafhandelParametersUrl)

                Then("the data is fetched correctly") {
                    fetchResponse.isSuccessful shouldBe true
                    val fetchResponseBody = fetchResponse.body!!.string()
                    logger.info { "Response: $fetchResponseBody" }

                    fetchResponseBody lenientShouldEqualJson restTemplateGroups
                }
            }
        }
    }
})
