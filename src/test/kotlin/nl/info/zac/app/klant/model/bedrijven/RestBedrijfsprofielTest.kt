/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.bedrijven

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.kvk.model.createVestiging

class RestBedrijfsprofielTest : BehaviorSpec({

    given("a vestiging with websites") {
        val vestiging = createVestiging().apply {
            websites = listOf("fakeWebsite1", "fakeWebsite2")
        }

        `when`("converted to RestBedrijfsprofiel") {
            val restBedrijfsprofiel = vestiging.toRestBedrijfsprofiel()

            then("the website is the first website of the vestiging") {
                restBedrijfsprofiel.website shouldBe "fakeWebsite1"
            }
        }
    }

    given("a vestiging with an empty list of websites") {
        val vestiging = createVestiging().apply {
            websites = emptyList()
        }

        `when`("converted to RestBedrijfsprofiel") {
            val restBedrijfsprofiel = vestiging.toRestBedrijfsprofiel()

            then("the profiel has no website") {
                restBedrijfsprofiel.website shouldBe null
            }
        }
    }
})
