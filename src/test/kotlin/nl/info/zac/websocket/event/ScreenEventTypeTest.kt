/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.websocket.event

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import nl.info.zac.event.Opcode
import net.atos.zac.signalering.model.SignaleringType
import nl.info.client.zgw.brc.model.createBesluit
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.test.org.flowable.task.service.impl.persistence.entity.createHistoricTaskInstanceEntityImpl
import nl.info.zac.identity.model.createUser
import nl.info.zac.signalering.model.createSignalering
import nl.info.zac.signalering.model.createSignaleringType
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

    context("Mapping notifications about besluiten and informatieobjecten") {
        data class MappedNotification(
            val channel: Channel,
            val mainResource: Resource,
            val resource: Resource,
            val expectedScreenEventType: ScreenEventType,
            val isSubResource: Boolean
        )
        listOf(
            MappedNotification(
                channel = Channel.BESLUITEN,
                mainResource = Resource.BESLUIT,
                resource = Resource.BESLUIT,
                expectedScreenEventType = ScreenEventType.BESLUIT,
                isSubResource = false
            ),
            MappedNotification(
                channel = Channel.BESLUITEN,
                mainResource = Resource.BESLUIT,
                resource = Resource.BESLUITINFORMATIEOBJECT,
                expectedScreenEventType = ScreenEventType.BESLUIT_INFORMATIEOBJECTEN,
                isSubResource = true
            ),
            MappedNotification(
                channel = Channel.INFORMATIEOBJECTEN,
                mainResource = Resource.INFORMATIEOBJECT,
                resource = Resource.INFORMATIEOBJECT,
                expectedScreenEventType = ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT,
                isSubResource = false
            ),
            MappedNotification(
                channel = Channel.INFORMATIEOBJECTEN,
                mainResource = Resource.INFORMATIEOBJECT,
                resource = Resource.GEBRUIKSRECHTEN,
                expectedScreenEventType = ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT,
                isSubResource = true
            )
        ).forEach { mappedNotification ->
            given("a ${mappedNotification.resource} on the ${mappedNotification.channel} channel that was updated") {
                val mainResourceUuid = UUID.randomUUID()
                val resourceUuid = if (mappedNotification.isSubResource) UUID.randomUUID() else mainResourceUuid
                val mainResourceInfo = Notification.ResourceInfo(
                    mappedNotification.mainResource,
                    URI("https://example.com/api/v1/main/$mainResourceUuid"),
                    Action.UPDATE
                )
                val resourceInfo = Notification.ResourceInfo(
                    mappedNotification.resource,
                    URI("https://example.com/api/v1/resource/$resourceUuid"),
                    Action.UPDATE
                )

                `when`("the notification is mapped to screen events") {
                    val events = ScreenEventType.getEvents(mappedNotification.channel, mainResourceInfo, resourceInfo)

                    then("the main resource is announced as updated, with the modified resource as detail if it differs") {
                        events.size shouldBe 1
                        with(events.first()) {
                            opcode shouldBe Opcode.UPDATED
                            objectType shouldBe mappedNotification.expectedScreenEventType
                            objectId.resource shouldBe mainResourceUuid.toString()
                            objectId.detail shouldBe
                                resourceUuid.toString().takeIf { mappedNotification.isSubResource }
                        }
                    }
                }
            }
        }
    }

    context("Mapping notifications that do not concern any screen") {
        listOf(
            Triple(Channel.ZAAKTYPEN, Resource.ZAAKTYPE, Action.UPDATE),
            Triple(Channel.ZAKEN, Resource.ZAAKTYPE, Action.UPDATE),
            Triple(Channel.BESLUITEN, Resource.BESLUITTYPE, Action.UPDATE),
            Triple(Channel.INFORMATIEOBJECTEN, Resource.INFORMATIEOBJECTTYPE, Action.UPDATE),
            Triple(Channel.ZAKEN, Resource.ZAAK, Action.READ)
        ).forEach { (channel, resource, action) ->
            given("a $action of a $resource on the $channel channel") {
                val resourceInfo = Notification.ResourceInfo(resource, zaakUrl, action)

                `when`("the notification is mapped to screen events") {
                    val events = ScreenEventType.getEvents(channel, resourceInfo, resourceInfo)

                    then("no event is sent") {
                        events.shouldBeEmpty()
                    }
                }
            }
        }
    }

    context("Creating screen events with the factory functions") {
        val uuid = UUID.randomUUID()
        val zaak = createZaak(uuid = uuid)
        val taskInfo = createHistoricTaskInstanceEntityImpl(id = "fakeTaskId")
        val besluit = createBesluit(url = URI("https://example.com/besluiten/$uuid"))
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(
            url = URI("https://example.com/enkelvoudiginformatieobjecten/$uuid")
        )
        val signalering = createSignalering(
            type = createSignaleringType(type = SignaleringType.Type.ZAAK_OP_NAAM),
            targetUser = createUser(id = "fakeUserId")
        )
        listOf(
            "updated with a UUID" to { ScreenEventType.ZAAK.updated(uuid) } to
                Triple(Opcode.UPDATED, ScreenEventType.ZAAK, ScreenEventId(uuid.toString(), null)),
            "updated with a URI" to { ScreenEventType.ZAAK.updated(zaak.url) } to
                Triple(Opcode.UPDATED, ScreenEventType.ZAAK, ScreenEventId(uuid.toString(), null)),
            "updated with a zaak" to { ScreenEventType.ZAAK_ROLLEN.updated(zaak) } to
                Triple(Opcode.UPDATED, ScreenEventType.ZAAK_ROLLEN, ScreenEventId(uuid.toString(), null)),
            "updated with a task" to { ScreenEventType.TAAK.updated(taskInfo) } to
                Triple(Opcode.UPDATED, ScreenEventType.TAAK, ScreenEventId("fakeTaskId", null)),
            "updated with a besluit" to { ScreenEventType.BESLUIT.updated(besluit) } to
                Triple(Opcode.UPDATED, ScreenEventType.BESLUIT, ScreenEventId(uuid.toString(), null)),
            "updated with an enkelvoudig informatieobject" to {
                ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT.updated(enkelvoudigInformatieObject)
            } to Triple(Opcode.UPDATED, ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT, ScreenEventId(uuid.toString(), null)),
            "updated with a signalering" to { ScreenEventType.SIGNALERINGEN.updated(signalering) } to
                Triple(Opcode.UPDATED, ScreenEventType.SIGNALERINGEN, ScreenEventId("fakeUserId", "ZAAK_OP_NAAM")),
            "updated with a custom resource id" to { ScreenEventType.ZAKEN_VERDELEN.updated("fakeResourceId") } to
                Triple(Opcode.UPDATED, ScreenEventType.ZAKEN_VERDELEN, ScreenEventId("fakeResourceId", null)),
            "deleted with a UUID" to { ScreenEventType.ZAAK.deleted(uuid) } to
                Triple(Opcode.DELETED, ScreenEventType.ZAAK, ScreenEventId(uuid.toString(), null)),
            "deleted with a URI" to { ScreenEventType.ZAAK.deleted(zaak.url) } to
                Triple(Opcode.DELETED, ScreenEventType.ZAAK, ScreenEventId(uuid.toString(), null)),
            "deleted with a zaak" to { ScreenEventType.ZAAK.deleted(zaak) } to
                Triple(Opcode.DELETED, ScreenEventType.ZAAK, ScreenEventId(uuid.toString(), null)),
            "deleted with a task" to { ScreenEventType.TAAK.deleted(taskInfo) } to
                Triple(Opcode.DELETED, ScreenEventType.TAAK, ScreenEventId("fakeTaskId", null)),
            "deleted with a besluit" to { ScreenEventType.BESLUIT_INFORMATIEOBJECTEN.deleted(besluit) } to
                Triple(Opcode.DELETED, ScreenEventType.BESLUIT_INFORMATIEOBJECTEN, ScreenEventId(uuid.toString(), null)),
            "deleted with an enkelvoudig informatieobject" to {
                ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT.deleted(enkelvoudigInformatieObject)
            } to Triple(Opcode.DELETED, ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT, ScreenEventId(uuid.toString(), null)),
            "skipped with a zaak" to { ScreenEventType.ZAAK.skipped(zaak) } to
                Triple(Opcode.SKIPPED, ScreenEventType.ZAAK, ScreenEventId(uuid.toString(), null)),
            "skipped with a custom resource id" to { ScreenEventType.ZAKEN_VRIJGEVEN.skipped("fakeResourceId") } to
                Triple(Opcode.SKIPPED, ScreenEventType.ZAKEN_VRIJGEVEN, ScreenEventId("fakeResourceId", null))
        ).forEach { (descriptionAndFactory, expected) ->
            val (description, factory) = descriptionAndFactory
            val (expectedOpcode, expectedScreenEventType, expectedScreenEventId) = expected
            given("a screen event that is created as $description") {
                `when`("the screen event is created") {
                    val screenEvent = factory()

                    then("it holds the opcode, the type and the id of the object, including its detail") {
                        screenEvent.opcode shouldBe expectedOpcode
                        screenEvent.objectType shouldBe expectedScreenEventType
                        screenEvent.objectId shouldBe expectedScreenEventId
                        screenEvent.objectId.detail shouldBe expectedScreenEventId.detail
                    }
                }
            }
        }
    }

    context("Creating screen events for an object that does not belong to the screen event type") {
        val zaak = createZaak()
        listOf(
            "a zaak" to { ScreenEventType.TAAK.updated(zaak) },
            "a task" to { ScreenEventType.ZAAK.updated(createHistoricTaskInstanceEntityImpl()) },
            "a besluit" to { ScreenEventType.ZAAK.updated(createBesluit()) },
            "an enkelvoudig informatieobject" to { ScreenEventType.ZAAK.updated(createEnkelvoudigInformatieObject()) },
            "a signalering" to { ScreenEventType.ZAAK.updated(createSignalering()) }
        ).forEach { (description, factory) ->
            given("a screen event type that does not accept $description") {
                `when`("a screen event is created for $description") {
                    val illegalArgumentException = shouldThrow<IllegalArgumentException> { factory() }

                    then("it fails") {
                        illegalArgumentException.message shouldStartWith
                            "This argument is not allowed for screen event type"
                    }
                }
            }
        }
    }

    context("Listing all screen event types") {
        given("the screen event types") {
            `when`("all screen event types are requested") {
                val screenEventTypes = ScreenEventType.any()

                then("every screen event type is returned, except for the 'any' type itself") {
                    screenEventTypes shouldContainExactlyInAnyOrder ScreenEventType.entries - ScreenEventType.ANY
                }
            }
        }
    }
})
