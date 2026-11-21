/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.exception

/**
 * Exception thrown when a server error occurs in the ZAC application.
 * Contains an error code which can be translated to a user-friendly message.
 * These exceptions typically result in a 500 Internal Server Error response.
 */
open class ServerErrorException(
    val errorCode: ErrorCode,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
