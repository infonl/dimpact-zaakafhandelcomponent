/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.event

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.model.createZaak
import nl.info.test.org.flowable.task.api.createTestTask
import nl.info.zac.event.Opcode
import nl.info.zac.identity.model.createUser
import nl.info.zac.notification.Action
import nl.info.zac.notification.Channel
import nl.info.zac.notification.Notification
import nl.info.zac.notification.Resource
import nl.info.zac.signalering.model.SignaleringType
import java.net.URI

class SignaleringEventUtilTest : BehaviorSpec({
    val zaakUri = URI("https://example.com/zaken/api/v1/zaken/fakeZaakUuid")
    val zaakInformatieobjectUri = URI("https://example.com/zaken/api/v1/zaakinformatieobjecten/fakeUuid")
    val rolUri = URI("https://example.com/zaken/api/v1/rollen/fakeRolUuid")
    val zaakResourceInfo = Notification.ResourceInfo(Resource.ZAAK, zaakUri, Action.UPDATE)

    context("Creating a signalering event") {
        given("a zaak and an actor") {
            val zaak = createZaak()
            val user = createUser(id = "fakeActorId")

            `when`("a signalering event is created for the zaak") {
                val signaleringEvent = signaleringEvent(SignaleringType.Type.ZAAK_OP_NAAM, zaak, user)

                then("the event references the zaak URL and the actor id") {
                    with(signaleringEvent) {
                        objectType shouldBe SignaleringType.Type.ZAAK_OP_NAAM
                        objectId shouldBe SignaleringEventId(zaak.url, null)
                        opcode shouldBe Opcode.UPDATED
                        actor shouldBe "fakeActorId"
                    }
                }
            }
        }

        given("a task and no actor") {
            val task = createTestTask(id = "fakeTaskId")

            `when`("a signalering event is created for the task") {
                val signaleringEvent = signaleringEvent(SignaleringType.Type.TAAK_OP_NAAM, task, null)

                then("the event references the task id and has no actor") {
                    signaleringEvent.objectId shouldBe SignaleringEventId("fakeTaskId", null)
                    signaleringEvent.actor shouldBe null
                }
            }
        }
    }

    context("Mapping a notification to signalering events") {
        given("a notification about a created zaakinformatieobject on the zaken channel") {
            val resourceInfo = Notification.ResourceInfo(Resource.ZAAKINFORMATIEOBJECT, zaakInformatieobjectUri, Action.CREATE)

            `when`("the events are requested") {
                val signaleringEvents = getSignaleringEvents(Channel.ZAKEN, zaakResourceInfo, resourceInfo)

                then("a zaak document toegevoegd event for the zaak with the zaakinformatieobject as detail is returned") {
                    signaleringEvents.map { it.objectType to it.objectId } shouldContainExactly listOf(
                        SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD to SignaleringEventId(zaakUri, zaakInformatieobjectUri)
                    )
                }
            }
        }

        given("a notification about a created rol on the zaken channel") {
            val resourceInfo = Notification.ResourceInfo(Resource.ROL, rolUri, Action.CREATE)

            `when`("the events are requested") {
                val signaleringEvents = getSignaleringEvents(Channel.ZAKEN, zaakResourceInfo, resourceInfo)

                then("a zaak op naam event for the rol without detail is returned") {
                    signaleringEvents.map { it.objectType to it.objectId } shouldContainExactly listOf(
                        SignaleringType.Type.ZAAK_OP_NAAM to SignaleringEventId(rolUri, null)
                    )
                }
            }
        }

        given("a notification about a deleted rol on the zaken channel") {
            val resourceInfo = Notification.ResourceInfo(Resource.ROL, rolUri, Action.DELETE)

            `when`("the events are requested") {
                val signaleringEvents = getSignaleringEvents(Channel.ZAKEN, zaakResourceInfo, resourceInfo)

                then("no events are returned") {
                    signaleringEvents.shouldBeEmpty()
                }
            }
        }

        given("a notification about a created zaak on the zaken channel") {
            val resourceInfo = Notification.ResourceInfo(Resource.ZAAK, zaakUri, Action.CREATE)

            `when`("the events are requested") {
                val signaleringEvents = getSignaleringEvents(Channel.ZAKEN, zaakResourceInfo, resourceInfo)

                then("no events are returned") {
                    signaleringEvents.shouldBeEmpty()
                }
            }
        }

        given("a notification about a created rol on another channel than zaken") {
            val resourceInfo = Notification.ResourceInfo(Resource.ROL, rolUri, Action.CREATE)

            `when`("the events are requested") {
                val signaleringEvents = getSignaleringEvents(Channel.OBJECTEN, zaakResourceInfo, resourceInfo)

                then("no events are returned") {
                    signaleringEvents.shouldBeEmpty()
                }
            }
        }
    }

    context("Formatting a signalering event id") {
        given("an event id without detail") {
            `when`("it is formatted") {
                then("only the resource is shown") {
                    SignaleringEventId(zaakUri, null).toString() shouldBe zaakUri.toString()
                }
            }
        }

        given("an event id with detail") {
            `when`("it is formatted") {
                then("the resource and the detail are shown, separated by a semicolon") {
                    SignaleringEventId(zaakUri, zaakInformatieobjectUri).toString() shouldBe
                        "$zaakUri;$zaakInformatieobjectUri"
                }
            }
        }
    }
})
