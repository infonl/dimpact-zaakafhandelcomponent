/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket.event

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import net.atos.zac.event.Opcode
import nl.info.zac.notification.Action
import nl.info.zac.notification.Channel
import nl.info.zac.notification.Notification
import nl.info.zac.notification.Resource
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID

class ScreenEventTypeTest : BehaviorSpec({
    val zaakUuid = UUID.randomUUID()
    val zaakUrl = URI("https://example.com/zaken/api/v1/zaken/$zaakUuid")

    fun notification(resource: Resource, resourceUuid: UUID, action: Action) = Notification(
        channel = Channel.ZAKEN,
        mainResourceUrl = zaakUrl,
        resource = resource,
        resourceUrl = URI("https://example.com/zaken/api/v1/${resource.name.lowercase()}/$resourceUuid"),
        action = action,
        creationDateTime = ZonedDateTime.now()
    )

    context("Mapping notifications about a resource that belongs to a zaak") {
        listOf(
            Resource.ROL to ScreenEventType.ZAAK_ROLLEN,
            Resource.ZAAKOBJECT to ScreenEventType.ZAAK,
            Resource.STATUS to ScreenEventType.ZAAK,
            Resource.RESULTAAT to ScreenEventType.ZAAK,
            Resource.ZAAKEIGENSCHAP to ScreenEventType.ZAAK,
            Resource.KLANTCONTACT to ScreenEventType.ZAAK,
            Resource.ZAAKINFORMATIEOBJECT to ScreenEventType.ZAAK_INFORMATIEOBJECTEN,
            Resource.ZAAKBESLUIT to ScreenEventType.ZAAK_BESLUITEN
        ).forEach { (resource, expectedScreenEventType) ->
            listOf(Action.CREATE, Action.UPDATE, Action.DELETE).forEach { action ->
                given("a $resource of a zaak on which a $action was performed") {
                    val notification = notification(resource, UUID.randomUUID(), action)

                    `when`("the notification is mapped to screen events") {
                        val events = ScreenEventType.getEvents(
                            notification.channel,
                            notification.getMainResourceInfo(),
                            notification.getResourceInfo()
                        )

                        then("the zaak it belongs to is announced as updated, whatever happened to the resource") {
                            events.size shouldBe 1
                            events.first().opcode shouldBe Opcode.UPDATED
                            events.first().objectType shouldBe expectedScreenEventType
                            events.first().objectId.resource shouldBe zaakUuid.toString()
                        }
                    }
                }
            }
        }
    }

    context("Mapping notifications about a zaak itself") {
        listOf(
            Action.UPDATE to Opcode.UPDATED,
            Action.DELETE to Opcode.DELETED
        ).forEach { (action, expectedOpcode) ->
            given("a zaak on which a $action was performed") {
                val notification = notification(Resource.ZAAK, zaakUuid, action).apply {
                    resourceUrl = zaakUrl
                }

                `when`("the notification is mapped to screen events") {
                    val events = ScreenEventType.getEvents(
                        notification.channel,
                        notification.getMainResourceInfo(),
                        notification.getResourceInfo()
                    )

                    then("the zaak is announced with that same action") {
                        events.size shouldBe 1
                        events.first().opcode shouldBe expectedOpcode
                        events.first().objectType shouldBe ScreenEventType.ZAAK
                    }
                }
            }
        }

        given("a zaak that was created") {
            val notification = notification(Resource.ZAAK, zaakUuid, Action.CREATE).apply {
                resourceUrl = zaakUrl
            }

            `when`("the notification is mapped to screen events") {
                val events = ScreenEventType.getEvents(
                    notification.channel,
                    notification.getMainResourceInfo(),
                    notification.getResourceInfo()
                )

                then("no event is sent, as no client can be subscribed to a zaak that did not exist yet") {
                    events.shouldBeEmpty()
                }
            }
        }
    }
})
