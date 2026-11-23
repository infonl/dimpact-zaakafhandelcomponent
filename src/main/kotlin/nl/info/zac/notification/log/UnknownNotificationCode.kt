/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification.log

import nl.info.zac.log.LogEvent

class UnknownNotificationCode(
    val type: Type,
    val code: String
) : LogEvent(type.description) {
    enum class Type(val description: String) {
        UNKNOWN_ACTION_CODE("Unknown action"),
        UNKNOWN_CHANNEL_CODE("Unknown channel"),
        UNKNOWN_RESOURCE_CODE("Unknown resource")
    }
}
