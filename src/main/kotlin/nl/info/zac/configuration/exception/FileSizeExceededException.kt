/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_FILE_SIZE_EXCEEDED

class FileSizeExceededException(message: String) : FileSizeException(ERROR_CODE_FILE_SIZE_EXCEEDED, message)
