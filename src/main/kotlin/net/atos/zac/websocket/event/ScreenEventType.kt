/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.websocket.event

import net.atos.zac.event.Opcode
import net.atos.zac.event.Opcode.DELETED
import net.atos.zac.event.Opcode.SKIPPED
import net.atos.zac.event.Opcode.UPDATED
import net.atos.zac.signalering.model.Signalering
import nl.info.client.zgw.brc.model.generated.Besluit
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.notification.Action
import nl.info.zac.notification.Channel
import nl.info.zac.notification.Notification
import nl.info.zac.notification.Resource
import org.flowable.task.api.TaskInfo
import java.net.URI
import java.util.EnumSet
import java.util.UUID

/**
 * Enumeration of the type of objects that can be referenced by a [ScreenEvent] event.
 *
 * Maps to object-type.ts
 */
@Suppress("TooManyFunctions")
enum class ScreenEventType {
    BESLUIT {
        override fun event(opcode: Opcode, besluit: Besluit) = instance(opcode, this, besluit)
    },

    BESLUIT_INFORMATIEOBJECTEN {
        override fun event(opcode: Opcode, besluit: Besluit) = instance(opcode, this, besluit)
    },

    ENKELVOUDIG_INFORMATIEOBJECT {
        override fun event(opcode: Opcode, enkelvoudigInformatieobject: EnkelvoudigInformatieObject) =
            instance(opcode, this, enkelvoudigInformatieobject)
    },

    SIGNALERINGEN {
        override fun event(opcode: Opcode, signalering: Signalering) = instance(opcode, this, signalering)
    },

    TAAK {
        override fun event(opcode: Opcode, taskInfo: TaskInfo) = instance(opcode, this, taskInfo)
    },

    TAKEN_VERDELEN,

    TAKEN_VRIJGEVEN,

    ZAAK {
        override fun event(opcode: Opcode, zaak: Zaak) = instance(opcode, this, zaak)
    },

    ZAAK_BESLUITEN {
        override fun event(opcode: Opcode, zaak: Zaak) = instance(opcode, this, zaak)
    },

    ZAAK_INFORMATIEOBJECTEN {
        override fun event(opcode: Opcode, zaak: Zaak) = instance(opcode, this, zaak)
    },

    ZAAK_NOTITIES,

    ZAAK_ROLLEN {
        override fun event(opcode: Opcode, zaak: Zaak) = instance(opcode, this, zaak)
    },

    ZAAK_TAKEN {
        override fun event(opcode: Opcode, zaak: Zaak) = instance(opcode, this, zaak)
    },

    ZAKEN_VERDELEN,

    ZAKEN_VRIJGEVEN,

    ANY;

    companion object {
        /**
         * Returns a set of all screen event types defined in this enum except for [ANY].
         */
        fun any(): Set<ScreenEventType> = EnumSet.complementOf(EnumSet.of(ANY))

        /**
         * Maps incoming notifications to specific screen events.
         *
         * @param channel      the channel the notification came in on
         * @param mainResource the involved main resource (may be equal to the resource)
         * @param resource     the actually modified resource
         * @return the set of events that the parameters map to
         */
        fun getEvents(
            channel: Channel,
            mainResource: Notification.ResourceInfo,
            resource: Notification.ResourceInfo
        ): Set<ScreenEvent> {
            val events = HashSet<ScreenEvent>()
            when (channel) {
                Channel.BESLUITEN -> addBesluitenEvents(events, mainResource, resource)
                Channel.INFORMATIEOBJECTEN -> addInformatieobjectenEvents(events, mainResource, resource)
                Channel.ZAKEN -> addZakenEvents(events, mainResource, resource)
                else -> {}
            }
            return events
        }

        private fun addBesluitenEvents(
            events: MutableSet<ScreenEvent>,
            mainResource: Notification.ResourceInfo,
            resource: Notification.ResourceInfo
        ) {
            when (resource.type) {
                Resource.BESLUIT -> BESLUIT.addEvent(events, resource, null)
                Resource.BESLUITINFORMATIEOBJECT -> BESLUIT_INFORMATIEOBJECTEN.addEvent(events, mainResource, resource)
                else -> {}
            }
        }

        private fun addInformatieobjectenEvents(
            events: MutableSet<ScreenEvent>,
            mainResource: Notification.ResourceInfo,
            resource: Notification.ResourceInfo
        ) {
            when (resource.type) {
                Resource.INFORMATIEOBJECT -> ENKELVOUDIG_INFORMATIEOBJECT.addEvent(events, resource, null)
                Resource.GEBRUIKSRECHTEN -> ENKELVOUDIG_INFORMATIEOBJECT.addEvent(events, mainResource, resource)
                else -> {}
            }
        }

        private fun addZakenEvents(
            events: MutableSet<ScreenEvent>,
            mainResource: Notification.ResourceInfo,
            resource: Notification.ResourceInfo
        ) {
            when (resource.type) {
                Resource.ZAAK -> ZAAK.addEvent(events, resource, null)
                Resource.STATUS,
                Resource.RESULTAAT,
                Resource.ZAAKEIGENSCHAP,
                Resource.KLANTCONTACT,
                Resource.ZAAKOBJECT -> ZAAK.addEvent(events, mainResource, resource)
                Resource.ZAAKINFORMATIEOBJECT -> ZAAK_INFORMATIEOBJECTEN.addEvent(events, mainResource, resource)
                Resource.ROL -> ZAAK_ROLLEN.addEvent(events, mainResource, resource)
                Resource.ZAAKBESLUIT -> ZAAK_BESLUITEN.addEvent(events, mainResource, resource)
                else -> {}
            }
        }

        private fun instance(opcode: Opcode, type: ScreenEventType, id: String, detail: String? = null) =
            ScreenEvent(opcode, type, ScreenEventId(id, detail))

        // In these methods you determine what is used as an id, make sure that this is consistent with the other methods
        private fun instance(opcode: Opcode, type: ScreenEventType, uuid: UUID, detail: UUID? = null) =
            instance(opcode = opcode, type = type, id = uuid.toString(), detail = detail?.toString())

        private fun instance(opcode: Opcode, type: ScreenEventType, url: URI, detail: URI? = null) =
            instance(opcode = opcode, type = type, uuid = url.extractUuid(), detail = detail?.extractUuid())

        // These methods determine what is used as an id, so that it is the same everywhere
        private fun instance(opcode: Opcode, type: ScreenEventType, zaak: Zaak) = instance(opcode, type, zaak.uuid)

        private fun instance(opcode: Opcode, type: ScreenEventType, taskInfo: TaskInfo) =
            instance(opcode, type, taskInfo.id)

        private fun instance(opcode: Opcode, type: ScreenEventType, besluit: Besluit) =
            instance(opcode, type, besluit.url)

        private fun instance(
            opcode: Opcode,
            type: ScreenEventType,
            enkelvoudigInformatieobject: EnkelvoudigInformatieObject
        ) = instance(opcode, type, enkelvoudigInformatieobject.url)

        private fun instance(opcode: Opcode, type: ScreenEventType, signalering: Signalering) =
            instance(opcode = opcode, type = type, id = signalering.target, detail = signalering.type.type.name)

        private fun notAllowed(type: ScreenEventType): Nothing =
            throw IllegalArgumentException("This argument is not allowed for screen event type $type")
    }

    // These methods determine on which object types the different arguments are allowed

    // Allowed with all object types
    private fun event(opcode: Opcode, uuid: UUID) = instance(opcode, this, uuid)

    // Allowed with all object types
    private fun event(opcode: Opcode, url: URI) = instance(opcode, this, url)

    private fun event(opcode: Opcode, eventResourceId: String) = instance(opcode, this, eventResourceId)

    // Allowed with all object types
    private fun event(opcode: Opcode, resource: Notification.ResourceInfo, detail: Notification.ResourceInfo?) =
        instance(opcode = opcode, type = this, url = resource.url, detail = detail?.url)

    // The following methods are not allowed, except for the object types that override them

    open fun event(opcode: Opcode, zaak: Zaak): ScreenEvent = notAllowed(this)

    open fun event(opcode: Opcode, taskInfo: TaskInfo): ScreenEvent = notAllowed(this)

    open fun event(opcode: Opcode, besluit: Besluit): ScreenEvent = notAllowed(this)

    open fun event(opcode: Opcode, enkelvoudigInformatieobject: EnkelvoudigInformatieObject): ScreenEvent =
        notAllowed(this)

    open fun event(opcode: Opcode, signalering: Signalering): ScreenEvent = notAllowed(this)

    // These are factory methods to create handy and unambiguous ScreenEvents for an object type.
    // Note that there are no "created" factory methods as there will never be a listener for those
    // (the new objectId is unknown client side).

    /**
     * Pay attention! If you use this method, you are responsible for providing the correct UUID.
     * Preferably use the other factory methods.
     *
     * @param uuid identification of the modified object.
     * @return instance of the event
     */
    fun updated(uuid: UUID) = event(UPDATED, uuid)

    /**
     * Pay attention! If you use this method, you are responsible for providing the correct URI.
     * Preferably use the other factory methods.
     *
     * @param url identification of the modified object.
     * @return instance of the event
     */
    fun updated(url: URI) = event(UPDATED, url)

    /**
     * Factory method for ScreenEvent (with case identification).
     *
     * @param zaak modified zaak.
     * @return instance of the event
     */
    fun updated(zaak: Zaak) = event(UPDATED, zaak)

    /**
     * Factory method for ScreenEvent (with identification of a task).
     *
     * @param taskInfo modified task
     * @return instance of the event
     */
    fun updated(taskInfo: TaskInfo) = event(UPDATED, taskInfo)

    /**
     * Factory method for ScreenEvent (with besluit identification).
     *
     * @param besluit modified besluit.
     * @return instance of the event
     */
    fun updated(besluit: Besluit) = event(UPDATED, besluit)

    /**
     * Factory method for ScreenEvent (identifying a single Information object).
     *
     * @param enkelvoudigInformatieobject modified enkelvoudigInformatieobject.
     * @return instance of the event
     */
    fun updated(enkelvoudigInformatieobject: EnkelvoudigInformatieObject) = event(UPDATED, enkelvoudigInformatieobject)

    /**
     * Factory method for ScreenEvent (with identification of a signalering target).
     *
     * @param signalering a created or deleted signalering
     * @return instance of the event
     */
    fun updated(signalering: Signalering) = event(UPDATED, signalering)

    /**
     * Factory method for ScreenEvent (with string identification of a custom resource).
     *
     * @param eventResourceId identification of the custom resource.
     * @return instance of the event
     */
    fun updated(eventResourceId: String) = event(UPDATED, eventResourceId)

    /**
     * Pay attention! If you use this method, you are responsible for providing the correct UUID. Preferably use the other deletion methods.
     *
     * @param uuid identification of the deleted object.
     * @return instance of the event
     */
    fun deleted(uuid: UUID) = event(DELETED, uuid)

    /**
     * Pay attention! If you use this method, you are responsible for providing the correct URI. Preferably use the other creation methods.
     *
     * @param url identification of the deleted object.
     * @return instance of the event
     */
    fun deleted(url: URI) = event(DELETED, url)

    /**
     * Factory method for ScreenEvent (with case identification).
     *
     * @param zaak deleted zaak.
     * @return instance of the event
     */
    fun deleted(zaak: Zaak) = event(DELETED, zaak)

    /**
     * Factory method for ScreenEvent (with identification of a task).
     *
     * @param taskInfo deleted task.
     * @return instance of the event
     */
    fun deleted(taskInfo: TaskInfo) = event(DELETED, taskInfo)

    /**
     * Factory method for ScreenEvent (with identification of a besluit).
     *
     * @param besluit deleted besluit.
     * @return instance of the event
     */
    fun deleted(besluit: Besluit) = event(DELETED, besluit)

    /**
     * Factory method for ScreenEvent (with case identification).
     *
     * @param enkelvoudigInformatieobject deleted enkelvoudigInformatieobject.
     * @return instance of the event
     */
    fun deleted(enkelvoudigInformatieobject: EnkelvoudigInformatieObject) = event(DELETED, enkelvoudigInformatieobject)

    /**
     * Factory method for ScreenEvent (with identification of a zaak) when a zaak was skipped during handling.
     *
     * @param zaak the skipped zaak.
     * @return instance of the event
     */
    fun skipped(zaak: Zaak) = event(SKIPPED, zaak)

    /**
     * Factory method for ScreenEvent to skip any `eventResourceId`.
     *
     * @param eventResourceId the skipped event.
     * @return instance of the event
     */
    fun skipped(eventResourceId: String) = event(SKIPPED, eventResourceId)

    private fun addEvent(
        events: MutableSet<ScreenEvent>,
        resource: Notification.ResourceInfo,
        detail: Notification.ResourceInfo?
    ) {
        when (resource.action) {
            // There cannot be any websockets listeners for Opcode.CREATED, so don't send the event.
            // (The new objectId would have to be known client side before it exists to subscribe to it. ;-)
            Action.CREATE -> {}
            Action.UPDATE -> events.add(event(UPDATED, resource, detail))
            Action.DELETE -> events.add(event(DELETED, resource, detail))
            else -> {}
        }
    }
}
