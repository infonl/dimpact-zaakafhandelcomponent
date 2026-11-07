/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.exception

/**
 * Custom exception for input validation failures.
 * These exceptions typically result in a 400 Bad Request response.
 */
open class InputValidationFailedException(
    val errorCode: ErrorCode? = null,
    message: String? = null
) : RuntimeException(message)
