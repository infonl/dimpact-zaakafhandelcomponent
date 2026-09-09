/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration.exception

import nl.info.zac.exception.ErrorCode

sealed class FileSizeException(
    val errorCode: ErrorCode,
    message: String
) : RuntimeException(message)
