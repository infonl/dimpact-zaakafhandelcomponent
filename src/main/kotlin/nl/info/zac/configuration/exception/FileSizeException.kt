/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration.exception

import nl.info.zac.exception.ErrorCode

/**
 * Thrown when a document is larger than ZAC is configured to handle.
 * These exceptions result in a 413 Payload Too Large response.
 */
sealed class FileSizeException(
    val errorCode: ErrorCode,
    message: String
) : RuntimeException(message)
