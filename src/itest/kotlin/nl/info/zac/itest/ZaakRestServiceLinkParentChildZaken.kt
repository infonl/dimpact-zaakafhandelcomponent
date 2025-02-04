/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.ItestConfiguration.DATE_2020_01_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_NO_CONTENT
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields
import org.json.JSONObject
import java.util.UUID
import java.util.UUID.fromString

/**
 * Integration test to test the functionality of linking parent and child zaken (hoofd- en deelzaken).
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
class ZaakRestServiceLinkParentChildZaken : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    Given(
        """
            Two zaken each of a different zaaktype and these two zaaktypes have been configured so
            that these zaken are allowed to have a parent-child relationship
        """
    ) {
        lateinit var zaak1UUID: UUID
        lateinit var zaak2UUID: UUID
        lateinit var zaak2Identificatie: String
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID,
            groupId = TEST_GROUP_A_ID,
            groupName = TEST_GROUP_A_DESCRIPTION,
            startDate = DATE_TIME_2000_01_01
        ).run {
            val responseBody = body!!.string()
            logger.info { "Response: $responseBody" }
            JSONObject(responseBody).run {
                zaak1UUID = getString("uuid").run(UUID::fromString)
            }
        }
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID,
            groupId = TEST_GROUP_A_ID,
            groupName = TEST_GROUP_A_DESCRIPTION,
            startDate = DATE_TIME_2000_01_01
        ).run {
            val responseBody = body!!.string()
            logger.info { "Response: $responseBody" }
            JSONObject(responseBody).run {
                zaak2Identificatie = getString("identificatie")
                zaak2UUID = getString("uuid").run(UUID::fromString)
            }
        }
        When("a request is done to link the parent zaak to the child zaak") {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/zaak/koppel",
                requestBodyAsString = """
                    {
                        "zaakUuid": "$zaak1UUID",
                        "teKoppelenZaakUuid": "$zaak2UUID",
                        "relatieType": "DEELZAAK"
                    }
                """.trimIndent()
            )
            Then("the parent-child relationship between the two zaken should be established") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_NO_CONTENT

                // retrieve the first zaak and check if the parent-child relationship has been established
                val response = zacClient.retrieveZaak(zaak1UUID)
                with(response) {
                    code shouldBe HTTP_STATUS_OK
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    JSONObject(responseBody).getJSONArray("gerelateerdeZaken").run {
                        length() shouldBe 1
                        this[0].toString() shouldEqualJsonIgnoringOrderAndExtraneousFields """
                            {
                                "identificatie" : "$zaak2Identificatie",
                                "relatieType" : "DEELZAAK",
                                "startdatum" : "$DATE_2020_01_01",
                                "statustypeOmschrijving" : "Intake",
                                "zaaktypeOmschrijving" : "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION"
                            }
                        """.trimIndent()
                    }
                }
            }
        }
    }
})
