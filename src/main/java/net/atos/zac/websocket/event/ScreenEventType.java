/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.websocket.event;

import static net.atos.client.zgw.util.UriUtilsKt.extractUuid;
import static net.atos.zac.event.Opcode.DELETED;
import static net.atos.zac.event.Opcode.SKIPPED;
import static net.atos.zac.event.Opcode.UPDATED;

import java.net.URI;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import nl.info.zac.notification.Notification;
import org.flowable.task.api.TaskInfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

import net.atos.client.zgw.brc.model.generated.Besluit;
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.zac.app.zaak.model.RestZaakOverzicht;
import net.atos.zac.event.Opcode;
import nl.info.zac.notification.Channel;
import net.atos.zac.signalering.model.Signalering;

/**
 * Enumeration of the type of objects that can be referenced by a {@link ScreenEvent} event.
 * <p>
 * Maps to object-type.ts
 */
public enum ScreenEventType {

    BESLUIT {
        @Override
        public ScreenEvent event(final Opcode opcode, final Besluit besluit) {
            return instance(opcode, this, besluit);
        }
    },

    BESLUIT_INFORMATIEOBJECTEN {
        @Override
        public ScreenEvent event(final Opcode opcode, final Besluit besluit) {
            return instance(opcode, this, besluit);
        }
    },

    ENKELVOUDIG_INFORMATIEOBJECT {
        @Override
        public ScreenEvent event(
                final Opcode opcode,
                final EnkelvoudigInformatieObject enkelvoudigInformatieobject
        ) {
            return instance(opcode, this, enkelvoudigInformatieobject);
        }
    },

    SIGNALERINGEN {
        @Override
        public ScreenEvent event(
                final Opcode opcode,
                final Signalering signalering
        ) {
            return instance(opcode, this, signalering);
        }
    },

    TAAK {
        @Override
        public ScreenEvent event(final Opcode opcode, final TaskInfo taskInfo) {
            return instance(opcode, this, taskInfo);
        }
    },

    TAKEN_VERDELEN,

    TAKEN_VRIJGEVEN,

    ZAAK {
        @Override
        public ScreenEvent event(final Opcode opcode, final Zaak zaak) {
            return instance(opcode, this, zaak);
        }
    },

    ZAAK_BESLUITEN {
        @Override
        public ScreenEvent event(final Opcode opcode, final Zaak zaak) {
            return instance(opcode, this, zaak);
        }
    },

    ZAAK_INFORMATIEOBJECTEN {
        @Override
        public ScreenEvent event(final Opcode opcode, final Zaak zaak) {
            return instance(opcode, this, zaak);
        }
    },

    ZAAK_ROLLEN {
        @Override
        public ScreenEvent event(final Opcode opcode, final Zaak zaak) {
            return instance(opcode, this, zaak);
        }
    },

    ZAAK_TAKEN {
        @Override
        public ScreenEvent event(final Opcode opcode, final Zaak zaak) {
            return instance(opcode, this, zaak);
        }
    },

    ZAKEN_VERDELEN,

    ZAKEN_VRIJGEVEN,

    ANY;

    /**
     * Returns a set of all screen event types defined in this enum except for {@link #ANY}.
     */
    public static Set<ScreenEventType> any() {
        return EnumSet.complementOf(EnumSet.of(ANY));
    }

    // This is the factory method.
    private static ScreenEvent instance(
            final Opcode opcode,
            final ScreenEventType type,
            final String id,
            final String detail
    ) {
        return new ScreenEvent(opcode, type, new ScreenEventId(id, detail));
    }

    // In these methods you determine what is used as an id, make sure that this is consistent with the other methods
    private static ScreenEvent instance(
            final Opcode opcode,
            final ScreenEventType type,
            final UUID uuid,
            final UUID detail
    ) {
        return instance(opcode, type,
                uuid.toString(),
                detail != null ? detail.toString() : null);
    }

    private static ScreenEvent instance(
            final Opcode opcode,
            final ScreenEventType type,
            final URI url,
            final URI detail
    ) {
        return instance(opcode, type, extractUuid(url), detail != null ? extractUuid(detail) : null);
    }

    // These methods determine what is used as an id, so that it is the same everywhere

    private static ScreenEvent instance(final Opcode opcode, final ScreenEventType type, final Zaak zaak) {
        return instance(opcode, type, zaak.getUuid(), null);
    }

    private static ScreenEvent instance(final Opcode opcode, final ScreenEventType type, final TaskInfo taskinfo) {
        return instance(opcode, type, taskinfo.getId(), (String) null);
    }

    private static ScreenEvent instance(final Opcode opcode, final ScreenEventType type, final Besluit besluit) {
        return instance(opcode, type, besluit.getUrl(), null);
    }

    private static ScreenEvent instance(
            final Opcode opcode,
            final ScreenEventType type,
            final EnkelvoudigInformatieObject enkelvoudigInformatieobject
    ) {
        return instance(opcode, type, enkelvoudigInformatieobject.getUrl(), null);
    }

    private static ScreenEvent instance(
            final Opcode opcode,
            final ScreenEventType type,
            final Signalering signalering
    ) {
        return instance(opcode, type, signalering.getTarget(), signalering.getType().getType().name());
    }

    private final static KotlinModule KOTLIN_MODULE = (new KotlinModule.Builder()).build();
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(KOTLIN_MODULE)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static ScreenEvent instance(
            final Opcode opcode,
            final ScreenEventType type,
            final String eventResourceId,
            final List<RestZaakOverzicht> restZaakOverzichtList
    ) throws JsonProcessingException {
        String details = OBJECT_MAPPER.writeValueAsString(restZaakOverzichtList);
        return instance(opcode, type, eventResourceId, details);
    }

    // These methods determine on which object types the different arguments are allowed
    private ScreenEvent event(final Opcode opcode, final UUID uuid) {
        return instance(opcode, this, uuid, null); // Allowed with all object types
    }

    private ScreenEvent event(final Opcode opcode, final URI url) {
        return instance(opcode, this, url, null); // Allowed with all object types
    }

    private ScreenEvent event(final Opcode opcode, final String eventResourceId) {
        return instance(opcode, this, eventResourceId, (String) null);
    }

    private ScreenEvent event(
            final Opcode opcode,
            final Notification.ResourceInfo resource,
            final Notification.ResourceInfo detail
    ) {
        return instance(opcode, this,
                resource.getUrl(),
                detail != null ? detail.getUrl() : null); // Allowed with all object types
    }

    public ScreenEvent event(final Opcode opcode, final Zaak zaak) {
        throw new IllegalArgumentException(); // Not allowed except for object types where this method has an override
    }

    public ScreenEvent event(final Opcode opcode, final TaskInfo taskInfo) {
        throw new IllegalArgumentException(); // Not allowed except for object types where this method has an override
    }

    public ScreenEvent event(final Opcode opcode, final Besluit besluit) {
        throw new IllegalArgumentException(); // Not allowed except for object types where this method has an override
    }

    public ScreenEvent event(
            final Opcode opcode,
            final EnkelvoudigInformatieObject enkelvoudigInformatieobject
    ) {
        throw new IllegalArgumentException(); // Not allowed except for object types where this method has an override
    }

    public ScreenEvent event(final Opcode opcode, final Signalering signalering) {
        throw new IllegalArgumentException(); // Not allowed except for object types where this method has an override
    }

    public ScreenEvent event(
            final Opcode opcode,
            final String signaleringResourceId,
            final List<RestZaakOverzicht> restZaakOverzichtList
    ) throws JsonProcessingException {
        throw new IllegalArgumentException(); // Not allowed except for object types where this method has an override
    }

    // These are factory methods to create handy and unambiguous ScreenEvents for an object type
    // Note that there are no "created" factory methods as there will never be a listener for those (the new objectId is unknown client side).

    /**
     * Pay attention! If you use this method, you are responsible for providing the correct UUID.
     * Preferably use the other factory methods.
     *
     * @param uuid identification of the modified object.
     * @return instance of the event
     */
    public final ScreenEvent updated(final UUID uuid) {
        return event(UPDATED, uuid);
    }

    /**
     * Pay attention! If you use this method, you are responsible for providing the correct URI.
     * Preferably use the other factory methods.
     *
     * @param url identification of the modified object.
     * @return instance of the event
     */
    public final ScreenEvent updated(final URI url) {
        return event(UPDATED, url);
    }

    /**
     * Factory method for ScreenEvent (with case identification).
     *
     * @param zaak modified zaak.
     * @return instance of the event
     */
    public final ScreenEvent updated(final Zaak zaak) {
        return event(UPDATED, zaak);
    }

    /**
     * Factory method for ScreenEvent (with identification of a task).
     *
     * @param taskInfo modified task
     * @return instance of the event
     */
    public final ScreenEvent updated(final TaskInfo taskInfo) {
        return event(UPDATED, taskInfo);
    }

    /**
     * Factory method for ScreenEvent (with besluit identification).
     *
     * @param besluit modified besluit.
     * @return instance of the event
     */
    public final ScreenEvent updated(final Besluit besluit) {
        return event(UPDATED, besluit);
    }

    /**
     * Factory method for ScreenEvent (identifying a single Information object).
     *
     * @param enkelvoudigInformatieobject modified enkelvoudigInformatieobject.
     * @return instance of the event
     */
    public final ScreenEvent updated(final EnkelvoudigInformatieObject enkelvoudigInformatieobject) {
        return event(UPDATED, enkelvoudigInformatieobject);
    }

    /**
     * Factory method for ScreenEvent (with identification of a signalering target).
     *
     * @param signalering a created or deleted signalering
     * @return instance of the event
     */
    public final ScreenEvent updated(final Signalering signalering) {
        return event(UPDATED, signalering);
    }


    /**
     * Factory method for ScreenEvent (with string identification of a custom resource).
     *
     * @param eventResourceId identification of the custom resource.
     * @return instance of the event
     */
    public final ScreenEvent updated(final String eventResourceId) {
        return event(UPDATED, eventResourceId);
    }

    public final ScreenEvent updated(
            final String eventResourceId,
            final List<RestZaakOverzicht> restZaakOverzichtList
    ) throws JsonProcessingException {
        return event(UPDATED, eventResourceId, restZaakOverzichtList);
    }

    /**
     * Pay attention! If you use this method, you are responsible for providing the correct UUID. Preferably use the other deletion methods.
     *
     * @param uuid identification of the deleted object.
     * @return instance of the event
     */
    public final ScreenEvent deleted(final UUID uuid) {
        return event(DELETED, uuid);
    }

    /**
     * Pay attention! If you use this method, you are responsible for providing the correct URI. Preferably use the other creation methods.
     *
     * @param url identificatioon of the deleted object.
     * @return instance of the event
     */
    public final ScreenEvent deleted(final URI url) {
        return event(DELETED, url);
    }

    /**
     * Factory method for ScreenEvent (with case identification).
     *
     * @param zaak deleted zaak.
     * @return instance of the event
     */
    public final ScreenEvent deleted(final Zaak zaak) {
        return event(DELETED, zaak);
    }

    /**
     * Factory method for ScreenEvent (with identification of a task).
     *
     * @param taskinfo deleted task.
     * @return instance of the event
     */
    public final ScreenEvent deleted(final TaskInfo taskinfo) {
        return event(DELETED, taskinfo);
    }

    /**
     * Factory method for ScreenEvent (with identification of a besluit).
     *
     * @param besluit deleted besluit.
     * @return instance of the event
     */
    public final ScreenEvent deleted(final Besluit besluit) {
        return event(DELETED, besluit);
    }

    /**
     * Factory method for ScreenEvent (with identification of a zaak) when a zaak was skipped during handling.
     *
     * @param zaak the skipped zaak.
     * @return instance of the event
     */
    public final ScreenEvent skipped(final Zaak zaak) {
        return event(SKIPPED, zaak);
    }

    /**
     * Factory method for ScreenEvent (with case identification).
     *
     * @param enkelvoudigInformatieobject deleted enkelvoudigInformatieobject.
     * @return instance of the event
     */
    public final ScreenEvent deleted(final EnkelvoudigInformatieObject enkelvoudigInformatieobject) {
        return event(DELETED, enkelvoudigInformatieobject);
    }

    private void addEvent(
            final Set<ScreenEvent> events,
            final Notification.ResourceInfo resource,
            final Notification.ResourceInfo detail
    ) {
        switch (resource.getAction()) {
            case CREATE:
                // There cannot be any websockets listeners for Opcode.CREATED, so don't send the event.
                // (The new objectId would have to be known client side before it exists to subscribe to it. ;-)
                break;
            case UPDATE:
                events.add(event(UPDATED, resource, detail));
                break;
            case DELETE:
                events.add(event(DELETED, resource, detail));
                break;
            default:
                break;
        }
    }

    /**
     * Maps incoming notifications to specific screen events.
     *
     * @param channel      the channel the notification came in on
     * @param mainResource the involved main resource (may be equal to the resource)
     * @param resource     the actually modified resource
     * @return the set of events that the parameters map to
     */
    public static Set<ScreenEvent> getEvents(
            final Channel channel,
            final Notification.ResourceInfo mainResource,
            final Notification.ResourceInfo resource
    ) {
        final Set<ScreenEvent> events = new HashSet<>();
        switch (channel) {
            case BESLUITEN:
                switch (resource.getType()) {
                    case BESLUIT:
                        ScreenEventType.BESLUIT.addEvent(events, resource, null);
                        break;
                    case BESLUITINFORMATIEOBJECT:
                        ScreenEventType.BESLUIT_INFORMATIEOBJECTEN.addEvent(events, mainResource, resource);
                        break;
                }
                break;
            case INFORMATIEOBJECTEN:
                switch (resource.getType()) {
                    case INFORMATIEOBJECT:
                        ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT.addEvent(events, resource, null);
                        break;
                    case GEBRUIKSRECHTEN:
                        ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT.addEvent(events, mainResource, resource);
                        break;
                }
                break;
            case ZAKEN:
                switch (resource.getType()) {
                    case ZAAK:
                        ScreenEventType.ZAAK.addEvent(events, resource, null);
                        break;
                    case STATUS, RESULTAAT, ZAAKEIGENSCHAP, KLANTCONTACT, ZAAKOBJECT:
                        ScreenEventType.ZAAK.addEvent(events, mainResource, resource);
                        break;
                    case ZAAKINFORMATIEOBJECT:
                        ScreenEventType.ZAAK_INFORMATIEOBJECTEN.addEvent(events, mainResource, resource);
                        break;
                    case ROL:
                        ScreenEventType.ZAAK_ROLLEN.addEvent(events, mainResource, resource);
                        break;
                    case ZAAKBESLUIT:
                        ScreenEventType.ZAAK_BESLUITEN.addEvent(events, mainResource, resource);
                        break;
                }
                break;
        }
        return events;
    }
}
