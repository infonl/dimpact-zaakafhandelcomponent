/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration.exception

import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.ServerErrorException

class BrpProtocolleringConfigurationException : ServerErrorException {
    constructor(message: String) : super(ErrorCode.ERROR_CODE_BAD_BRP_PROTOCOLLERING_CONFIGURATION, message)
}
