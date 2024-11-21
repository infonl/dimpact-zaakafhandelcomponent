package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration.ACTIE_INTAKE_AFRONDEN
import nl.lifely.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_NO_CONTENT
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import nl.lifely.zac.itest.util.sleep
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
@Suppress("MagicNumber")
class ZaakRestServiceExtensionTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val logger = KotlinLogging.logger {}

    Given(
        """A zaak exists which has not been extended yet"""
    ) {

        When("the zaak is extended") {
            val daysExtended = 3
            val reason = "dummyReason"
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakProductaanvraag1Uuid/verlenging",
                requestBodyAsString = """
                    {
                        "redenVerlenging": "$reason",
                        "duurDagen": $daysExtended,
                        "takenVerlengen": false
                    }
                    """.trimIndent()
            )
            Then(
                """the response should be OK and the returned zaak should be extended"""
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("redenVerlenging", reason)
                    shouldContainJsonKeyValue("duurVerlenging", "$daysExtended dagen")
                }
            }
        }
    }
})
