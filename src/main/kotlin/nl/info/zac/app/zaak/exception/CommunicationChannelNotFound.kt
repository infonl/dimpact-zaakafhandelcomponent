/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_CASE_HAS_NO_COMMUNICATION_CHANNEL
import nl.info.zac.exception.InputValidationFailedException

class CommunicationChannelNotFound : InputValidationFailedException(
    errorCode = ERROR_CODE_CASE_HAS_NO_COMMUNICATION_CHANNEL,
)
