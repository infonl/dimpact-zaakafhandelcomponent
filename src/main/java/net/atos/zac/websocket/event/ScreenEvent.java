/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.websocket.event;

import java.io.Serial;

import net.atos.zac.event.AbstractEvent;
import net.atos.zac.event.Opcode;

public class ScreenEvent extends AbstractEvent<ScreenEventType, ScreenEventId> {

    @Serial
    private static final long serialVersionUID = -740125186878024703L;

    private ScreenEventType objectType;

    /**
     * Identifies the user whose action caused this event, so that a screen can tell a change it made
     * itself from one made by somebody else. Holds the functionele gebruiker for changes that
     * originate outside a user session, such as notifications and cron jobs.
     */
    private String actorUserId;

    public ScreenEvent() {
        super();
    }

    public ScreenEvent(final Opcode opcode, final ScreenEventType objectType, final ScreenEventId objectId) {
        super(opcode, objectId);
        this.objectType = objectType;
    }

    @Override
    public ScreenEventType getObjectType() {
        return objectType;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(final String actorUserId) {
        this.actorUserId = actorUserId;
    }
}
