/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.exception

import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.ServerErrorException

class ZaaktypeConfigurationNotFoundException(override val message: String) : ServerErrorException(
    ErrorCode.ERROR_CODE_ZAAKTYPE_CONFIGURATION_NOT_FOUND,
    message
)
