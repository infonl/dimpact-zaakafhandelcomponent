/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.exception.log

import nl.info.zac.log.LogEvent

data object ExceptionResponseConversionFailed : LogEvent("Failed to convert exception to JSON")
