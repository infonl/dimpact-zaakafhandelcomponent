/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification.log

import nl.info.zac.log.LogEvent

sealed class UnknownNotificationCode(description: String, val code: String) : LogEvent(description) {
    class UnknownActionCode(code: String) : UnknownNotificationCode("Unknown action", code)

    class UnknownChannelCode(code: String) : UnknownNotificationCode("Unknown channel", code)

    class UnknownResourceCode(code: String) : UnknownNotificationCode("Unknown resource", code)
}
