/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_FILE_TOO_LARGE_TO_OPEN

/**
 * Thrown when a document can be stored and downloaded but is too large for an operation that has to
 * hold it in memory, such as converting it to PDF, sending it as a mail attachment or editing it
 * through WebDAV.
 */
class FileTooLargeToOpenException(message: String) : FileSizeException(ERROR_CODE_FILE_TOO_LARGE_TO_OPEN, message)
