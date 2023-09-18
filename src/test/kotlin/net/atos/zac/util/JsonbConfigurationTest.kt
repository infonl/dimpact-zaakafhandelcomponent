package net.atos.zac.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.zac.LocalDateTest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Datums worden NIET aangepast naar Locale tijdzone (Europe/Amsterdam), dus 23-06-2020 blijft 23-06-2020,
 * ongeacht de tijdzone.
 * Wanneer je deze test aanpast moet je ook angular test datum.pipe.spec.ts aanpassen
 */
class JsonbConfigurationTest : BehaviorSpec({
    val contextResolver = JsonbConfiguration().getContext(LocalDateTest::class.java)

    given("a JSON date-string with an ISO Z timezone") {
        When("it is parsed with our context resolver") {
            then("the date-time string is correctly formatted") {
                val datum = contextResolver.fromJson("\"2021-06-23T00:00:00Z\"", LocalDate::class.java)

                datum.format(DateTimeFormatter.ISO_DATE) shouldBe "2021-06-23"
            }
        }
    }

    given("a JSON date-string with a +02:00 timezone") {
        When("it is parsed with our context resolver") {
            then("the date-time string is correctly formatted") {
                val datum = contextResolver.fromJson("\"2021-06-23T00:00:00+02:00\"", LocalDate::class.java)

                datum.format(DateTimeFormatter.ISO_DATE) shouldBe "2021-06-23"
            }
        }
    }

    given("a JSON date-string with a -02:00 timezone") {
        When("it is parsed with our context resolver") {
            then("the date-time string is correctly formatted") {
                val datum = contextResolver.fromJson("\"2021-06-23T00:00:00-02:00\"", LocalDate::class.java)

                datum.format(DateTimeFormatter.ISO_DATE) shouldBe "2021-06-23"
            }
        }
    }

    given("a JSON date-string without a timezone") {
        When("it is parsed with our context resolver") {
            then("the date-time string is correctly formatted") {
                val datum = contextResolver.fromJson("\"2021-06-23\"", LocalDate::class.java)

                datum.format(DateTimeFormatter.ISO_DATE) shouldBe "2021-06-23"
            }
        }
    }
})
