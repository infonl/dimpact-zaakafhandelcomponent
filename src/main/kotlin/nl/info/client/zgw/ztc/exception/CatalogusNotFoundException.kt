/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_CATALOGUS_NOT_CONFIGURED
import nl.info.zac.exception.ServerErrorException

class CatalogusNotFoundException(message: String) : ServerErrorException(
    errorCode = ERROR_CODE_CATALOGUS_NOT_CONFIGURED,
    message = message
)
