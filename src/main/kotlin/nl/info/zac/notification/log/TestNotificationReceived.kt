/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification.log

import nl.info.zac.log.LogEvent

data object TestNotificationReceived : LogEvent("Received test callback URL status notification")
