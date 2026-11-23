/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.exception.log

import nl.info.zac.log.LogEvent

class ExceptionMappedToResponse(
    val httpStatus: Int,
    val errorCode: String?
) : LogEvent("Exception was thrown; returning an error response")
