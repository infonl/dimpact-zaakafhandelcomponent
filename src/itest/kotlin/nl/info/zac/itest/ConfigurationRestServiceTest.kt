/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.CONFIG_GEMEENTE_CODE
import nl.info.zac.itest.config.ItestConfiguration.CONFIG_GEMEENTE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.CONFIG_MAX_FILE_SIZE_IN_MB
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RAADPLEGER_1
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrder
import java.net.HttpURLConnection.HTTP_OK

class ConfigurationRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val logger = KotlinLogging.logger {}

    given("Configuration items are available in ZAC and a user with at least one ZAC role is logged in") {
        `when`("the languages are retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/talen",
                testUser = RAADPLEGER_1
            )

            then("the available languages are returned") {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJsonIgnoringOrder """
                [ 
                    {
                      "code" : "dut",
                      "id" : "1",
                      "local" : "Nederlands",
                      "naam" : "Nederlands",
                      "name" : "Dutch"
                    }, 
                    {
                      "code" : "fre",
                      "id" : "2",
                      "local" : "français",
                      "naam" : "Frans",
                      "name" : "French"
                    },
                    {
                      "code" : "eng",
                      "id" : "3",
                      "local" : "English",
                      "naam" : "Engels",
                      "name" : "English"
                    },
                    {
                      "code" : "ger",
                      "id" : "4",
                      "local" : "Deutsch",
                      "naam" : "Duits",
                      "name" : "German"
                    },
                    {
                      "code" : "fry",
                      "id" : "5",
                      "local" : "Frysk",
                      "naam" : "Fries",
                      "name" : "Frisian"
                    }
                ]
                """.trimIndent()
            }
        }
        `when`("the default language is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/talen/default",
                testUser = RAADPLEGER_1
            )

            then("the default language is returned") {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJson """
                {
                  "code" : "dut",
                  "id" : "1",
                  "local" : "Nederlands",
                  "naam" : "Nederlands",
                  "name" : "Dutch"
                }
                """.trimIndent()
            }
        }
        `when`("the max upload file size is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/max-file-size-mb",
                testUser = RAADPLEGER_1
            )

            then("the max upload file size is returned") {
                response.code shouldBe HTTP_OK
                response.bodyAsString.toLong() shouldBe CONFIG_MAX_FILE_SIZE_IN_MB
            }
        }
        `when`("the allowed file types are retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/file-types",
                testUser = RAADPLEGER_1
            )

            then("the canonical allowlist of extension/media type pairs is returned") {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJsonIgnoringOrder """
                    [
                      { "extension": ".avi",  "mediaType": "video/x-msvideo" },
                      { "extension": ".bmp",  "mediaType": "image/bmp" },
                      { "extension": ".doc",  "mediaType": "application/msword" },
                      { "extension": ".docx", "mediaType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document" },
                      { "extension": ".eml",  "mediaType": "message/rfc822" },
                      { "extension": ".flv",  "mediaType": "video/x-flv" },
                      { "extension": ".gif",  "mediaType": "image/gif" },
                      { "extension": ".jpeg", "mediaType": "image/jpeg" },
                      { "extension": ".jpg",  "mediaType": "image/jpeg" },
                      { "extension": ".mkv",  "mediaType": "video/x-matroska" },
                      { "extension": ".mov",  "mediaType": "video/quicktime" },
                      { "extension": ".mp4",  "mediaType": "video/mp4" },
                      { "extension": ".mpeg", "mediaType": "video/mpeg" },
                      { "extension": ".msg",  "mediaType": "application/vnd.ms-outlook" },
                      { "extension": ".ods",  "mediaType": "application/vnd.oasis.opendocument.spreadsheet" },
                      { "extension": ".odt",  "mediaType": "application/vnd.oasis.opendocument.text" },
                      { "extension": ".pdf",  "mediaType": "application/pdf" },
                      { "extension": ".png",  "mediaType": "image/png" },
                      { "extension": ".ppt",  "mediaType": "application/vnd.ms-powerpoint" },
                      { "extension": ".pptx", "mediaType": "application/vnd.openxmlformats-officedocument.presentationml.presentation" },
                      { "extension": ".rtf",  "mediaType": "application/rtf" },
                      { "extension": ".txt",  "mediaType": "text/plain" },
                      { "extension": ".vsd",  "mediaType": "application/vnd.visio" },
                      { "extension": ".wmv",  "mediaType": "video/x-ms-wmv" },
                      { "extension": ".xls",  "mediaType": "application/vnd.ms-excel" },
                      { "extension": ".xlsx", "mediaType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" }
                    ]
                """.trimIndent()
            }
        }
        `when`("the council name is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/gemeente",
                testUser = RAADPLEGER_1
            )

            then("the council name is returned") {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJson "\"$CONFIG_GEMEENTE_NAAM\""
            }
        }
        `when`("the council code is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/gemeente/code",
                testUser = RAADPLEGER_1
            )

            then("the council code is returned") {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                responseBody shouldEqualJson "\"$CONFIG_GEMEENTE_CODE\""
            }
        }

        `when`("the 'is BRP doelbinding setup enabled' endpoint is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/configuratie/brp/doelbinding-setup-enabled",
                testUser = RAADPLEGER_1
            )

            then(
                "'true' is returned because BRP_DOELBINDING_PER_ZAAKTYPE is set to true in the itest configuration"
            ) {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                responseBody shouldBe "true"
            }
        }
    }
})
