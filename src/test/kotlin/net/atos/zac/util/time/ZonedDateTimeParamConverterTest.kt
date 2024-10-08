package net.atos.zac.util.time

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.ZoneId
import java.time.ZonedDateTime

class ZonedDateTimeParamConverterTest : BehaviorSpec({
    val zoneId = ZoneId.of("+02:00")

    Given("ZonedDateTime date") {
        val zonedDateTime = ZonedDateTime.of(2024, 10, 7, 0, 0, 0, 0, zoneId)

        When("converted to string") {
            val zonedDateTimeString = ZonedDateTimeParamConverter().toString(zonedDateTime)

            Then("it should convert correctly") {
                zonedDateTimeString shouldBe "2024-10-07T00:00:00+02:00"
            }
        }
    }

    Given("Date as string in ISO_OFFSET_DATE_TIME format") {
        val zonedDateTimeString = "2024-10-07T00:00:00+02:00"

        When("converted to ZonedDateTime") {
            val zonedDateTime = ZonedDateTimeParamConverter().fromString(zonedDateTimeString)

            Then("it should convert correctly") {
                zonedDateTime shouldBe ZonedDateTime.of(2024, 10, 7, 0, 0, 0, 0, zoneId)
            }
        }
    }

})
