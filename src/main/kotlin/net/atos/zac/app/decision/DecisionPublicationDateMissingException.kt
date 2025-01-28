/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.decision

import nl.info.zac.exception.ErrorCode.ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE
import nl.info.zac.exception.InputValidationFailedException

class DecisionPublicationDateMissingException : InputValidationFailedException(
    ERROR_CODE_BESLUIT_PUBLICATION_DATE_MISSING_TYPE
)
