/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class BronTest : BehaviorSpec({
    given("a Bron enum value") {
        `when`("toValue is called") {
            then("it returns the ZGW API code") {
                Bron.ZAKEN_API.toValue() shouldBe "ZRC"
            }
        }
    }

    given("a ZGW API code") {
        `when`("fromValue is called with a known code") {
            then("it returns the matching Bron") {
                Bron.fromValue("ZRC") shouldBe Bron.ZAKEN_API
            }
        }

        `when`("fromValue is called with an unknown code") {
            val exception = shouldThrow<NoSuchElementException> { Bron.fromValue("unknown") }

            then("it throws a NoSuchElementException") {
                exception.message shouldBe "Array contains no element matching the predicate."
            }
        }
    }

    given("the JSON-B adapter for Bron") {
        val adapter = Bron.Adapter()

        `when`("adaptToJson is called") {
            val json = adapter.adaptToJson(Bron.DOCUMENTEN_API)

            then("it returns the ZGW API code") {
                json shouldBe "DRC"
            }
        }

        `when`("adaptFromJson is called with a known code") {
            val bron = adapter.adaptFromJson("BRC")

            then("it returns the matching Bron") {
                bron shouldBe Bron.BESLUITEN_API
            }
        }

        `when`("adaptFromJson is called with an unknown code") {
            val bron = adapter.adaptFromJson("unknown")

            then("it returns null") {
                bron.shouldBeNull()
            }
        }
    }
})
