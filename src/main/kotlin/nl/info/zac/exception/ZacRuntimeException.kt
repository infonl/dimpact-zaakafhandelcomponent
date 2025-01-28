/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.exception

/**
 * Exception thrown when an error occurs in the ZAC application.
 * Contains an error code which can be translated to a user-friendly message.
 */
open class ZacRuntimeException(
    val errorCode: ErrorCode,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
