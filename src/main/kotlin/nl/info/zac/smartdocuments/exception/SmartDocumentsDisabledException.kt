/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.smartdocuments.exception

import nl.info.zac.exception.ErrorCode.ERROR_CODE_SMARTDOCUMENTS_DISABLED
import nl.info.zac.exception.InputValidationFailedException

class SmartDocumentsDisabledException : InputValidationFailedException(ERROR_CODE_SMARTDOCUMENTS_DISABLED)
