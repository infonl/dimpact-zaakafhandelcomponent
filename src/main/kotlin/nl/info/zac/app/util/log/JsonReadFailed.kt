/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.util.log

import nl.info.zac.log.LogEvent

data object JsonReadFailed : LogEvent("JSON exception while reading from reader")
