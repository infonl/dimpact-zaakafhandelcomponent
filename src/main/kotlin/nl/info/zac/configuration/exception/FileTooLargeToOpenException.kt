/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_FILE_TOO_LARGE_TO_OPEN

class FileTooLargeToOpenException(message: String) : FileSizeException(ERROR_CODE_FILE_TOO_LARGE_TO_OPEN, message)
