/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.besluit

import nl.info.zac.exception.ErrorCode.ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE
import nl.info.zac.exception.InputValidationFailedException

class BesluitPublicationDateMissingException : InputValidationFailedException(
    ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE
)
