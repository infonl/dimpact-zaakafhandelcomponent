/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.websocket.event

import nl.info.zac.event.AbstractEvent
import nl.info.zac.event.Opcode
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.io.Serial

@Schema(requiredProperties = ["timestamp", "opcode", "objectType", "objectId"])
class ScreenEvent(
    opcode: Opcode,
    override val objectType: ScreenEventType,
    objectId: ScreenEventId
) : AbstractEvent<ScreenEventType, ScreenEventId>(opcode, objectId) {
    companion object {
        @Serial
        private const val serialVersionUID = -740125186878024703L
    }

    /**
     * The username of the user whose action caused this event, so that a screen can tell a change it made
     * itself from one made by somebody else. Holds the functionele gebruiker for changes that
     * originate outside a user session, such as notifications and cron jobs.
     */
    var actorUsername: String? = null
}
