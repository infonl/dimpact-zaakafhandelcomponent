/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import net.atos.zac.event.Opcode
import net.atos.zac.websocket.event.ScreenEventType

class WebSocketSubscriptionMessageDecoderTest : BehaviorSpec({
    val webSocketSubscriptionMessageDecoder = WebSocketSubscriptionMessageDecoder()

    context("Decoding a subscription message") {
        given("a JSON message that subscribes to an event") {
            val jsonMessage = """
                {
                    "subscriptionType": "CREATE",
                    "event": {
                        "opcode": "UPDATED",
                        "objectType": "ZAAK",
                        "objectId": { "resource": "fakeResource" }
                    }
                }
            """.trimIndent()

            `when`("the message is decoded") {
                val subscriptionMessage = webSocketSubscriptionMessageDecoder.decode(jsonMessage)

                then("it is a create message for the event in the JSON") {
                    subscriptionMessage.subscriptionType shouldBe SubscriptionType.CREATE
                    with(subscriptionMessage.event.shouldNotBeNull()) {
                        opcode shouldBe Opcode.UPDATED
                        objectType shouldBe ScreenEventType.ZAAK
                        objectId.resource shouldBe "fakeResource"
                        objectId.detail shouldBe null
                    }
                }
            }
        }

        given("a JSON message that cancels all subscriptions") {
            val jsonMessage = """{ "subscriptionType": "DELETE_ALL" }"""

            `when`("the message is decoded") {
                val subscriptionMessage = webSocketSubscriptionMessageDecoder.decode(jsonMessage)

                then("it is the delete all message") {
                    subscriptionMessage shouldBeSameInstanceAs SubscriptionType.DELETE_ALL.message()
                }
            }
        }
    }

    context("Checking whether a message can be decoded") {
        given("a JSON object") {
            then("it can be decoded") {
                webSocketSubscriptionMessageDecoder.willDecode("""{ "subscriptionType": "DELETE_ALL" }""") shouldBe true
            }
        }

        given("a message that is not a JSON object") {
            then("it cannot be decoded") {
                webSocketSubscriptionMessageDecoder.willDecode("not json") shouldBe false
            }
        }
    }
})
