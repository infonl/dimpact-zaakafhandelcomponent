/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import java.util.UUID

sealed class RolEvent(description: String, val rolUuid: UUID?) : LogEvent(description) {
    class InitiatorRoleWithoutIdentificatie(rolUuid: UUID?) :
        RolEvent("Initiator role has no identificatie; cannot resolve initiator variables", rolUuid)

    class InitiatorWithoutName(rolUuid: UUID?) :
        RolEvent("Initiator persoon does not have a name; using a replacement for the full name", rolUuid)
}
